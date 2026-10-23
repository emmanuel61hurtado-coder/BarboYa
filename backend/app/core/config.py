from functools import lru_cache
from urllib.parse import quote_plus

from pydantic import model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    app_name: str = "Barboya API"
    environment: str = "development"

    database_url: str = ""

    # Partes de la URL de PostgreSQL (se usan si DATABASE_URL no viene definida)
    postgres_user: str = "barboya"
    postgres_password: str = "barboya"
    postgres_host: str = "localhost"
    postgres_port: int = 5432
    postgres_db: str = "barboya"

    redis_url: str = "redis://localhost:6379/0"

    # SQLite fallback for local development without Docker/PostgreSQL
    use_sqlite: bool = True  # auto-fallback when PostgreSQL is unreachable
    sqlite_url: str = "sqlite+aiosqlite:///./barboya_dev.db"

    jwt_secret: str = "CAMBIAR-EN-PRODUCCION"
    jwt_algorithm: str = "HS256"
    jwt_expire_minutes: int = 60 * 24
    jwt_issuer: str = "barboya-auth"
    jwt_audience: str = "barboya-api"

    cors_origins: list[str] = ["*"]

    # Tarifas de suscripción (COP)
    price_daily_cop: int = 3_000
    price_weekly_cop: int = 15_000
    price_monthly_cop: int = 50_000

    # Fee por carrera por defecto (COP)
    default_trip_fee_cop: int = 300

    # Seguridad / operación
    stopped_alert_seconds: int = 180
    face_check_interval_minutes: int = 240
    offer_ttl_seconds: int = 120

    # Storage (S3 o Supabase compatible con S3)
    s3_bucket: str = "barboya-media"
    s3_endpoint_url: str | None = None
    s3_region: str = "us-east-1"

    @model_validator(mode="after")
    def _build_database_url(self) -> "Settings":
        """Si no viene DATABASE_URL, se arma con las variables POSTGRES_*."""
        import os
        import json

        if self.jwt_secret == "CAMBIAR-EN-PRODUCCION":
            sec = os.getenv("SECRET_KEY") or os.getenv("JWT_SECRET")
            if sec:
                self.jwt_secret = sec

        if self.cors_origins == ["*"]:
            allowed = os.getenv("ALLOWED_ORIGINS") or os.getenv("CORS_ORIGINS")
            if allowed:
                try:
                    parsed = json.loads(allowed)
                    if isinstance(parsed, list):
                        self.cors_origins = parsed
                except Exception:
                    self.cors_origins = [o.strip() for o in allowed.split(",")]

        if not self.database_url:
            from urllib.parse import urlparse

            if self.postgres_host:
                host_str = str(self.postgres_host).strip()
                if "://" in host_str:
                    parsed_url = urlparse(host_str)
                    if parsed_url.hostname:
                        self.postgres_host = parsed_url.hostname
                    if parsed_url.port:
                        self.postgres_port = parsed_url.port
                    if parsed_url.username and not self.postgres_user:
                        self.postgres_user = parsed_url.username
                    if parsed_url.password and not self.postgres_password:
                        self.postgres_password = parsed_url.password
                    if parsed_url.path and len(parsed_url.path) > 1 and not self.postgres_db:
                        self.postgres_db = parsed_url.path.lstrip("/")
                elif ":" in host_str and not host_str.startswith("["):
                    parts = host_str.split(":")
                    self.postgres_host = parts[0]
                    if len(parts) > 1 and parts[1].isdigit():
                        self.postgres_port = int(parts[1])

            user = quote_plus(self.postgres_user)
            password = quote_plus(self.postgres_password)
            self.database_url = (
                f"postgresql+asyncpg://{user}:{password}"
                f"@{self.postgres_host}:{self.postgres_port}/{self.postgres_db}"
            )
        return self


@lru_cache
def get_settings() -> Settings:
    return Settings()
