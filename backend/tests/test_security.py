import pytest

from app.core.config import Settings


def test_production_security_rejects_insecure_key():
    with pytest.raises(ValueError, match="CRITICAL SECURITY ALERT"):
        Settings(
            ENVIRONMENT="production",
            SECRET_KEY="supersecretkeychangemeinproduction",
            DATABASE_URL="postgresql+asyncpg://prod_user:safe_pass@db:5432/prod_db",
            ALLOWED_ORIGINS=["https://barboya.com"],
        )


def test_production_security_rejects_short_key():
    with pytest.raises(ValueError, match="CRITICAL SECURITY ALERT"):
        Settings(
            ENVIRONMENT="production",
            SECRET_KEY="short_key_12345",
            DATABASE_URL="postgresql+asyncpg://prod_user:safe_pass@db:5432/prod_db",
            ALLOWED_ORIGINS=["https://barboya.com"],
        )


def test_production_security_rejects_default_db_password():
    with pytest.raises(ValueError, match="CRITICAL SECURITY ALERT"):
        Settings(
            ENVIRONMENT="production",
            SECRET_KEY="this_is_a_very_secure_random_key_with_at_least_32_characters",
            DATABASE_URL="postgresql+asyncpg://barboya:barboyasecret@db:5432/barboya_db",
            ALLOWED_ORIGINS=["https://barboya.com"],
        )


def test_production_security_rejects_wildcard_cors():
    with pytest.raises(ValueError, match="CRITICAL SECURITY ALERT"):
        Settings(
            ENVIRONMENT="production",
            SECRET_KEY="this_is_a_very_secure_random_key_with_at_least_32_characters",
            DATABASE_URL="postgresql+asyncpg://prod_user:safe_pass@db:5432/prod_db",
            ALLOWED_ORIGINS=["*"],
        )


def test_production_security_accepts_valid_config():
    valid = Settings(
        ENVIRONMENT="production",
        SECRET_KEY="this_is_a_very_secure_random_key_with_at_least_32_characters",
        DATABASE_URL="postgresql+asyncpg://prod_user:safe_pass@db:5432/prod_db",
        ALLOWED_ORIGINS=["https://barboya.com"],
    )
    assert valid.ENVIRONMENT == "production"
    assert len(valid.SECRET_KEY) >= 32
