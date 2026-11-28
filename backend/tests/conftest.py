import pytest

from app.core.config import get_settings


@pytest.fixture(autouse=True)
def suspended_services_on(monkeypatch):
    """Las pruebas ejercitan los servicios suspendidos; test_legal_flags comprueba el corte."""
    monkeypatch.setattr(get_settings(), "enable_trips", True)
    monkeypatch.setattr(get_settings(), "enable_crypto_payments", True)
