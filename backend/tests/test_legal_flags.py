"""Servicios suspendidos por la revision legal: apagados salvo que se enciendan a proposito."""
import uuid

import pytest
from fastapi.testclient import TestClient

from app.core.config import Settings, get_settings
from app.core.security import create_token
from app.main import app

TRIP = str(uuid.uuid4())


@pytest.fixture
def suspended(monkeypatch):
    monkeypatch.setattr(get_settings(), "enable_trips", False)
    monkeypatch.setattr(get_settings(), "enable_crypto_payments", False)
    # Sin "with": no arranca las tareas de fondo
    return TestClient(app)


def auth(role):
    return {"Authorization": f"Bearer {create_token(uuid.uuid4(), role)}"}


def test_they_are_off_unless_configured(monkeypatch):
    for name in ("ENABLE_TRIPS", "ENABLE_CRYPTO_PAYMENTS"):
        monkeypatch.delenv(name, raising=False)
    settings = Settings(_env_file=None, environment="development")
    assert settings.enable_trips is False
    assert settings.enable_crypto_payments is False


def test_the_interface_can_ask_what_is_on(suspended):
    assert suspended.get("/features").json() == {"trips": False, "crypto": False}


@pytest.mark.parametrize("path,role", [
    ("/trips/quote", "customer"),
    ("/trips", "customer"),
    (f"/trips/{TRIP}/offers", "driver"),
    (f"/trips/{TRIP}/counter", "customer"),
    (f"/trips/{TRIP}/accept", "customer"),
    (f"/trips/{TRIP}/driver-accept", "driver"),
])
def test_no_new_trip_can_start(suspended, path, role):
    response = suspended.post(path, json={}, headers=auth(role))
    assert response.status_code == 503, response.text
    assert response.json()["detail"]["code"] == "TRIPS_DISABLED"


def test_no_new_crypto_payment(suspended):
    response = suspended.get("/payments/crypto/config", headers=auth("driver"))
    assert response.status_code == 503
    assert response.json()["detail"]["code"] == "CRYPTO_DISABLED"
    topup = suspended.post("/wallet/topups", headers=auth("driver"),
                           json={"amount_cop": 5000, "payment_method": "crypto"})
    assert topup.status_code == 503
    assert topup.json()["detail"]["code"] == "CRYPTO_DISABLED"
