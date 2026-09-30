from typing import List
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    PROJECT_NAME: str = "BarboYa API"
    API_V1_STR: str = "/api/v1"
    DATABASE_URL: str = "postgresql+asyncpg://barboyadb:barboyasecret@localhost:5432/barboyadb"
    SECRET_KEY: str = "supersecretkeychangemeinproduction"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 15
    REFRESH_TOKEN_EXPIRE_DAYS: int = 7
    ENVIRONMENT: str = "development"
    ALLOWED_ORIGINS: List[str] = ["*"]
    ADMIN_EMAIL: str = "admin@barboya.com"
    ADMIN_PASSWORD: str = "Admin123*"

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")


settings = Settings()
