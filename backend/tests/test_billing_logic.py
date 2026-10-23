"""Pruebas de la lógica pura de cobro y tarifas (sin base de datos)."""
from datetime import datetime, time, timedelta, timezone
from types import SimpleNamespace

import pytest

from app.models.enums import BillingScheme
from app.services import billing, fares


def make_driver(**kw):
    base = dict(
        is_verified=True,
        face_check_due=False,
        billing_scheme=BillingScheme.per_trip_fee,
        wallet_balance_cop=10_000,
        per_trip_fee_override_cop=None,
        subscription_active=False,
        subscription_expires_at=None,
    )
    base.update(kw)
    d = SimpleNamespace(**base)
    from app.models.user import DriverProfile

    d.has_valid_subscription = DriverProfile.has_valid_subscription.fget(d)
    return d


@pytest.mark.asyncio
async def test_fee_scheme_allows_with_balance():
    r = await billing.check_eligibility(None, make_driver(), None)
    assert r.allowed and r.fee_cop == 300


@pytest.mark.asyncio
async def test_fee_scheme_blocks_when_balance_below_fee():
    r = await billing.check_eligibility(None, make_driver(wallet_balance_cop=200), None)
    assert not r.allowed and r.code == "INSUFFICIENT_BALANCE"


@pytest.mark.asyncio
async def test_subscription_active_is_unlimited_and_free():
    exp = datetime.now(timezone.utc) + timedelta(days=2)
    d = make_driver(
        billing_scheme=BillingScheme.subscription, wallet_balance_cop=0,
        subscription_active=True, subscription_expires_at=exp,
    )
    r = await billing.check_eligibility(None, d, None)
    assert r.allowed and r.fee_cop == 0


@pytest.mark.asyncio
async def test_subscription_expired_blocks():
    exp = datetime.now(timezone.utc) - timedelta(minutes=1)
    d = make_driver(
        billing_scheme=BillingScheme.subscription, subscription_active=True, subscription_expires_at=exp
    )
    r = await billing.check_eligibility(None, d, None)
    assert not r.allowed and r.code == "SUBSCRIPTION_EXPIRED"


@pytest.mark.asyncio
async def test_unverified_or_face_check_blocks():
    assert (await billing.check_eligibility(None, make_driver(is_verified=False), None)).code == "NOT_VERIFIED"
    assert (await billing.check_eligibility(None, make_driver(face_check_due=True), None)).code == "FACE_CHECK_DUE"


def test_night_window_crosses_midnight():
    tz = fares.BOGOTA
    assert fares.is_night(datetime(2026, 1, 1, 22, 0, tzinfo=tz), time(20), time(5))
    assert fares.is_night(datetime(2026, 1, 1, 3, 0, tzinfo=tz), time(20), time(5))
    assert not fares.is_night(datetime(2026, 1, 1, 12, 0, tzinfo=tz), time(20), time(5))


def test_haversine_and_rounding():
    assert fares.haversine_m(5.9333, -73.6208, 5.9333, -73.6208) == 0
    assert 1000 < fares.haversine_m(5.9333, -73.6208, 5.9333, -73.6108) < 1200
    assert fares.round_to_100(4349) == 4300
