import json
import logging
from typing import List, Union
from pydantic import field_validator, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

logger = logging.getLogger("barboya.security")


class Settings(BaseSettings):
    PROJECT_NAME: str = "BarboYa API"
    API_V1_STR: str = "/api/v1"
    DATABASE_URL: str = "postgresql+asyncpg://barboya:changeme@localhost:5432/barboya_db"
    SECRET_KEY: str = "insecure_dev_key_only_must_be_overridden_in_production"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 15
    REFRESH_TOKEN_EXPIRE_DAYS: int = 7
    ENVIRONMENT: str = "development"
    ALLOWED_ORIGINS: List[str] = ["*"]
    ADMIN_EMAIL: str = "admin@barboya.com"
    ADMIN_PASSWORD: str = ""

    @field_validator("ALLOWED_ORIGINS", mode="before")
    @classmethod
    def assemble_cors_origins(cls, v: Union[str, List[str]]) -> List[str]:
        if isinstance(v, str):
            if v.startswith("[") and v.endswith("]"):
                try:
                    return json.loads(v)
                except Exception:
                    pass
            return [i.strip() for i in v.split(",") if i.strip()]
        return v

    @model_validator(mode="after")
    def validate_production_security(self):
        insecure_keys = [
            "supersecretkeychangemeinproduction",
            "insecure_dev_key_only_must_be_overridden_in_production",
            "changeme",
            "secret",
        ]
        if self.ENVIRONMENT == "production":
            key_lower = self.SECRET_KEY.lower()
            if any(bad in key_lower for bad in insecure_keys) or len(self.SECRET_KEY) < 32:
                raise ValueError(
                    "CRITICAL SECURITY ALERT: In production, SECRET_KEY must be a secure, random string "
                    "of at least 32 characters (e.g. openssl rand -hex 32). "
                    "Please define SECRET_KEY in your environment variables."
                )
            if self.ADMIN_PASSWORD in ["Admin123*", "password", "admin", "123456", "root"]:
                raise ValueError(
                    "CRITICAL SECURITY ALERT: In production, ADMIN_PASSWORD cannot be a known default. "
                    "Please set a strong ADMIN_PASSWORD in your environment variables."
                )
        return self

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        # In Docker, env vars come from the container environment, not .env file.
        # This setting prevents crashes when .env is absent.
        env_ignore_empty=True,
    )


settings = Settings()
