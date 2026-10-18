from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    app_name: str = "Barboya API"
    environment: str = "development"

    database_url: str = "postgresql+asyncpg://barboya:barboya@localhost:5432/barboya"
    redis_url: str = "redis://localhost:6379/0"

    jwt_secret: str = "CAMBIAR-EN-PRODUCCION"
    jwt_algorithm: str = "HS256"
    jwt_expire_minutes: int = 60 * 24

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


@lru_cache
def get_settings() -> Settings:
    return Settings()
