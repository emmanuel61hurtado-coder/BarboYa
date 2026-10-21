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
        if not self.database_url:
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
