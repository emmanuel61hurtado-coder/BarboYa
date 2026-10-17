import json
import logging
import sys
from typing import List, Union
from pydantic import field_validator, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

def configure_logging():
    logging.basicConfig(
        format="%(message)s",
        stream=sys.stdout,
        level=logging.INFO,
    )

class Settings(BaseSettings):
    PROJECT_NAME: str = "BarboYa Súper App API"
    API_V1_STR: str = "/api/v1"
    DATABASE_URL: str = "postgresql+asyncpg://barboya:barboyasecret@localhost:5432/barboya_db"
    REDIS_URL: str = "redis://localhost:6379/0"
    SECRET_KEY: str = "insecure_dev_key_only_must_be_overridden_in_production_with_at_least_32_chars"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 15
    REFRESH_TOKEN_EXPIRE_DAYS: int = 7
    ENVIRONMENT: str = "development"
    ALLOWED_ORIGINS: List[str] = ["*"]
    ENABLE_TRANSPORT_PEOPLE: bool = False
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
                raise ValueError("CRITICAL SECURITY ALERT: In production, SECRET_KEY must be >= 32 chars.")
        return self

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        env_ignore_empty=True,
    )

settings = Settings()
