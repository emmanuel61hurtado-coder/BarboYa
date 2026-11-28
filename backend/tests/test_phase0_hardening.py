"""Fase 0 (contención): secretos, webhook de pagos, ofertas de viaje y SOS."""
import hashlib
import hmac
import uuid
from datetime import datetime, timedelta, timezone

import pytest
from fastapi import HTTPException
from fastapi.testclient import TestClient
from pydantic import ValidationError
from sqlalchemy import select
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

import app.models  # noqa: F401  (registra todas las tablas)
from app.core.config import Settings
from app.core.database import Base
from app.core.security import CurrentUser, create_token
from app.main import app as api
from app.models import DriverProfile, SecurityLog, Trip, User
from app.models.enums import ServiceType, TripStatus, UserRole, VehicleType
from app.routers import trips, webhooks
from app.schemas.schemas import AcceptOfferIn, DriverOfferIn

STRONG = "x7Qm2" * 10


# ───────────────────────────── SECRETO JWT ─────────────────────────────

@pytest.fixture
def clean_env(monkeypatch):
    for name in ("JWT_SECRET", "SECRET_KEY", "ENVIRONMENT"):
        monkeypatch.delenv(name, raising=False)


@pytest.mark.parametrize("secret", [
    "",
    "CAMBIAR-EN-PRODUCCION",
    "cambia-esto-por-un-secreto-largo-y-aleatorio",
    "demasiado-corto",
])
def test_production_refuses_weak_jwt_secret(clean_env, secret):
    with pytest.raises(ValidationError, match="JWT_SECRET"):
        Settings(_env_file=None, environment="production", jwt_secret=secret)


def test_production_accepts_strong_jwt_secret(clean_env):
    assert Settings(_env_file=None, environment="production", jwt_secret=STRONG).jwt_secret == STRONG


def test_development_without_secret_gets_a_random_one(clean_env):
    first = Settings(_env_file=None, environment="development").jwt_secret
    second = Settings(_env_file=None, environment="development").jwt_secret
    assert first != second
    assert len(first) >= 32 and first != "CAMBIAR-EN-PRODUCCION"


# ───────────────────────────── WEBHOOK DE PAGOS ─────────────────────────────

BODY = b'{"event":"payment.approved"}'


def _post_webhook(signature: str | None, body: bytes = BODY):
    headers = {"Content-Type": "application/json"}
    if signature is not None:
        headers["X-Signature"] = signature
    return TestClient(api).post("/webhooks/payments", content=body, headers=headers)


def _sign(secret: str, body: bytes = BODY) -> str:
    return hmac.new(secret.encode(), body, hashlib.sha256).hexdigest()


def test_webhook_is_closed_without_configured_secret(monkeypatch):
    monkeypatch.setattr(webhooks.settings, "webhook_secret", "")
    assert _post_webhook(None).status_code == 503


@pytest.mark.parametrize("signature", [None, "", "0" * 64])
def test_webhook_rejects_missing_or_wrong_signature(monkeypatch, signature):
    monkeypatch.setattr(webhooks.settings, "webhook_secret", STRONG)
    assert _post_webhook(signature).status_code == 401


def test_webhook_accepts_valid_signature(monkeypatch):
    monkeypatch.setattr(webhooks.settings, "webhook_secret", STRONG)
    res = _post_webhook(_sign(STRONG))
    assert res.status_code == 200
    assert res.json() == {"received": True, "event": "payment.approved"}


def test_webhook_rejects_signed_non_json(monkeypatch):
    monkeypatch.setattr(webhooks.settings, "webhook_secret", STRONG)
    assert _post_webhook(_sign(STRONG, b"no-json"), b"no-json").status_code == 400


# ───────────────────────────── BASE EN MEMORIA ─────────────────────────────

@pytest.fixture
async def world():
    engine = create_async_engine("sqlite+aiosqlite://", poolclass=StaticPool)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    maker = async_sessionmaker(engine, expire_on_commit=False)

    async with maker() as session:
        customer = User(role=UserRole.customer, full_name="Cliente", phone="+573000000001", password_hash="x")
        driver = User(role=UserRole.driver, full_name="Conductor", phone="+573000000002", password_hash="x")
        session.add_all([customer, driver])
        await session.flush()
        session.add(DriverProfile(
            user_id=driver.id, vehicle_type=VehicleType.moto, vehicle_plate="ABC123",
            document_id="CC-1", is_verified=True, wallet_balance_cop=10_000,
        ))
        trip = Trip(
            customer_id=customer.id, service_type=ServiceType.passenger, status=TripStatus.requested,
            suggested_fare_cop=5_000, offered_fare_cop=5_000, confirmation_pin="1234",
        )
        session.add(trip)
        await session.commit()

    yield {
        "maker": maker,
        "trip_id": trip.id,
        "customer": CurrentUser(customer.id, "customer"),
        "driver": CurrentUser(driver.id, "driver"),
    }
    await engine.dispose()


async def _offer(world, amount):
    async with world["maker"]() as session:
        await trips.driver_offer(
            world["trip_id"], DriverOfferIn(amount_cop=amount, eta_minutes=5), world["driver"], session
        )


async def _accept(world, amount):
    async with world["maker"]() as session:
        data = AcceptOfferIn(driver_id=world["driver"].id, amount_cop=amount)
        return await trips.customer_accepts_offer(world["trip_id"], data, world["customer"], session)


async def _trip(world) -> Trip:
    async with world["maker"]() as session:
        return await session.get(Trip, world["trip_id"])


async def _assert_rejected(world, amount):
    with pytest.raises(HTTPException) as err:
        await _accept(world, amount)
    assert err.value.status_code == 409
    assert err.value.detail["code"] == "OFFER_NOT_FOUND"
    assert (await _trip(world)).driver_id is None


# ───────────────────────────── OFERTAS DE VIAJE ─────────────────────────────

async def test_customer_cannot_assign_driver_who_never_offered(world):
    await _assert_rejected(world, 5_000)


async def test_customer_cannot_change_the_offered_amount(world):
    await _offer(world, 8_000)
    await _assert_rejected(world, 1)


async def test_expired_offer_cannot_be_accepted(world):
    from app.models import TripOffer

    await _offer(world, 8_000)
    async with world["maker"]() as session:
        offer = (await session.execute(select(TripOffer))).scalar_one()
        offer.created_at = datetime.now(timezone.utc) - timedelta(hours=1)
        await session.commit()
    await _assert_rejected(world, 8_000)


async def test_new_offer_replaces_the_previous_one(world):
    await _offer(world, 8_000)
    await _offer(world, 9_000)
    await _assert_rejected(world, 8_000)


async def test_valid_offer_is_accepted_once(world):
    await _offer(world, 8_000)
    await _accept(world, 8_000)
    trip = await _trip(world)
    assert trip.status == TripStatus.accepted
    assert trip.driver_id == world["driver"].id
    assert trip.agreed_fare_cop == 8_000
    with pytest.raises(HTTPException) as err:
        await _accept(world, 8_000)
    assert err.value.status_code == 409


# ───────────────────────────── SOS ─────────────────────────────

async def test_sos_is_recorded_and_does_not_claim_notifications(world):
    from app.core.database import get_session

    async def override():
        async with world["maker"]() as session:
            yield session

    api.dependency_overrides[get_session] = override
    try:
        from httpx import ASGITransport, AsyncClient

        token = create_token(world["customer"].id, "customer")
        async with AsyncClient(transport=ASGITransport(app=api), base_url="http://test") as client:
            res = await client.post(
                "/security/sos",
                json={"lat": 5.9333, "lng": -73.6208},
                headers={"Authorization": f"Bearer {token}"},
            )
    finally:
        api.dependency_overrides.pop(get_session, None)

    assert res.status_code == 201, res.text
    assert "notificados" not in res.json()["message"]
    assert "123" in res.json()["message"]
    async with world["maker"]() as session:
        log = (await session.execute(select(SecurityLog))).scalar_one()
    assert log.user_id == world["customer"].id
