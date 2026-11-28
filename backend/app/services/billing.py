"""Servicio de cobro híbrido de Barboya.

Dos esquemas conviven y el admin elige cuál aplica por conductor (o por zona):

* SUBSCRIPTION  -> pase diario/semanal/mensual. Si está vigente, el conductor
                   acepta viajes ilimitados y conserva el 100 % de la tarifa.
* PER_TRIP_FEE  -> bolsa prepago. Se exige saldo >= fee para ACEPTAR; el fee se
                   descuenta al COMPLETAR el viaje (idempotente por trip_id).
"""
from __future__ import annotations

import uuid
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import get_settings
from app.models.billing import Subscription, WalletTransaction
from app.models.enums import (
    BillingScheme,
    PaymentMethod,
    SubscriptionPlan,
    SubscriptionStatus,
    TripStatus,
    WalletTxStatus,
    WalletTxType,
)
from app.models.trip import Trip
from app.models.user import DriverProfile
from app.models.zone import Zone

settings = get_settings()

PLAN_DURATION = {
    SubscriptionPlan.daily: timedelta(days=1),
    SubscriptionPlan.weekly: timedelta(days=7),
    SubscriptionPlan.monthly: timedelta(days=30),
}


class BillingError(Exception):
    """Error de negocio con un código estable para que la app pueda reaccionar."""

    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code
        self.message = message


@dataclass(frozen=True)
class EligibilityResult:
    allowed: bool
    scheme: BillingScheme
    fee_cop: int           # 0 si es suscripción
    reason: str | None = None
    code: str | None = None


def plan_price(plan: SubscriptionPlan) -> int:
    return {
        SubscriptionPlan.daily: settings.price_daily_cop,
        SubscriptionPlan.weekly: settings.price_weekly_cop,
        SubscriptionPlan.monthly: settings.price_monthly_cop,
    }[plan]


async def resolve_fee(session: AsyncSession, driver: DriverProfile, zone_id: uuid.UUID | None) -> int:
    """Prioridad: override del conductor > fee de la zona > fee global."""
    if driver.per_trip_fee_override_cop is not None:
        return driver.per_trip_fee_override_cop
    if zone_id is not None:
        zone = await session.get(Zone, zone_id)
        if zone is not None:
            return zone.per_trip_fee_cop
    return settings.default_trip_fee_cop


async def check_eligibility(
    session: AsyncSession, driver: DriverProfile, zone_id: uuid.UUID | None = None
) -> EligibilityResult:
    """Decide si el conductor puede aceptar una nueva carrera según su esquema."""
    if not driver.is_verified:
        return EligibilityResult(False, driver.billing_scheme, 0, "Cuenta no verificada", "NOT_VERIFIED")
    if driver.face_check_due:
        return EligibilityResult(
            False, driver.billing_scheme, 0, "Debes completar la verificación facial", "FACE_CHECK_DUE"
        )

    if driver.billing_scheme == BillingScheme.subscription:
        if driver.has_valid_subscription:
            return EligibilityResult(True, BillingScheme.subscription, 0)
        return EligibilityResult(
            False, BillingScheme.subscription, 0, "Tu pase venció. Renuévalo para continuar.", "SUBSCRIPTION_EXPIRED"
        )

    fee = await resolve_fee(session, driver, zone_id)
    if driver.wallet_balance_cop < fee:
        return EligibilityResult(
            False,
            BillingScheme.per_trip_fee,
            fee,
            f"Saldo insuficiente: necesitas al menos ${fee:,} COP. Recarga tu bolsa.".replace(",", "."),
            "INSUFFICIENT_BALANCE",
        )
    return EligibilityResult(True, BillingScheme.per_trip_fee, fee)


async def accept_trip(session: AsyncSession, trip_id: uuid.UUID, driver_id: uuid.UUID, agreed_fare_cop: int) -> Trip:
    """Acepta una carrera validando el cobro. Bloquea filas para evitar doble aceptación."""
    trip = (
        await session.execute(select(Trip).where(Trip.id == trip_id).with_for_update())
    ).scalar_one_or_none()
    if trip is None:
        raise BillingError("TRIP_NOT_FOUND", "Viaje no encontrado")
    if trip.status not in (TripStatus.requested, TripStatus.negotiating):
        raise BillingError("TRIP_UNAVAILABLE", "Otro conductor ya tomó este viaje")

    driver = (
        await session.execute(select(DriverProfile).where(DriverProfile.user_id == driver_id).with_for_update())
    ).scalar_one_or_none()
    if driver is None:
        raise BillingError("NOT_A_DRIVER", "No tienes perfil de conductor")

    result = await check_eligibility(session, driver, trip.zone_id)
    if not result.allowed:
        raise BillingError(result.code or "NOT_ELIGIBLE", result.reason or "No puedes aceptar viajes")

    trip.driver_id = driver_id
    trip.status = TripStatus.accepted
    trip.agreed_fare_cop = agreed_fare_cop
    trip.platform_fee_cop = result.fee_cop
    trip.accepted_at = datetime.now(timezone.utc)
    await session.flush()
    return trip


async def settle_completed_trip(session: AsyncSession, trip_id: uuid.UUID) -> Trip:
    """Descuenta el fee fijo al completar el viaje (solo esquema bolsa). Idempotente."""
    trip = (
        await session.execute(select(Trip).where(Trip.id == trip_id).with_for_update())
    ).scalar_one()
    if trip.driver_id is None:
        raise BillingError("NO_DRIVER", "El viaje no tiene conductor")

    trip.status = TripStatus.completed
    trip.completed_at = datetime.now(timezone.utc)

    if trip.platform_fee_cop > 0:
        already = (
            await session.execute(
                select(WalletTransaction.id).where(
                    WalletTransaction.trip_id == trip.id, WalletTransaction.type == WalletTxType.trip_fee
                )
            )
        ).first()
        if already is None:
            driver = (
                await session.execute(
                    select(DriverProfile).where(DriverProfile.user_id == trip.driver_id).with_for_update()
                )
            ).scalar_one()
            # El saldo nunca baja de cero (CHECK en BD): se descuenta lo que haya.
            charge = min(trip.platform_fee_cop, driver.wallet_balance_cop)
            if charge > 0:
                driver.wallet_balance_cop -= charge
                session.add(
                    WalletTransaction(
                        driver_id=driver.user_id,
                        type=WalletTxType.trip_fee,
                        amount_cop=-charge,
                        balance_after_cop=driver.wallet_balance_cop,
                        trip_id=trip.id,
                        note="Fee por carrera completada",
                    )
                )
    await session.flush()
    return trip


async def request_topup(
    session: AsyncSession,
    driver_id: uuid.UUID,
    amount_cop: int,
    method: PaymentMethod,
    reference: str | None,
    proof_url: str | None,
) -> WalletTransaction:
    """El conductor reporta una recarga; queda PENDIENTE hasta que el admin la apruebe."""
    if amount_cop <= 0:
        raise BillingError("INVALID_AMOUNT", "El monto debe ser positivo")
    tx = WalletTransaction(
        driver_id=driver_id,
        type=WalletTxType.topup,
        status=WalletTxStatus.pending,
        amount_cop=amount_cop,
        payment_method=method,
        payment_reference=reference,
        proof_url=proof_url,
    )
    session.add(tx)
    await session.flush()
    return tx


async def approve_topup(session: AsyncSession, tx_id: uuid.UUID, admin_id: uuid.UUID, approve: bool) -> WalletTransaction:
    tx = (
        await session.execute(select(WalletTransaction).where(WalletTransaction.id == tx_id).with_for_update())
    ).scalar_one_or_none()
    if tx is None or tx.type != WalletTxType.topup:
        raise BillingError("TX_NOT_FOUND", "Recarga no encontrada")
    if tx.status != WalletTxStatus.pending:
        raise BillingError("TX_ALREADY_PROCESSED", "La recarga ya fue procesada")

    tx.approved_by = admin_id
    if not approve:
        tx.status = WalletTxStatus.rejected
        await session.flush()
        return tx

    driver = (
        await session.execute(select(DriverProfile).where(DriverProfile.user_id == tx.driver_id).with_for_update())
    ).scalar_one()
    driver.wallet_balance_cop += tx.amount_cop
    tx.status = WalletTxStatus.approved
    tx.balance_after_cop = driver.wallet_balance_cop
    await session.flush()
    return tx


async def approve_subscription(
    session: AsyncSession, driver_id: uuid.UUID, plan: SubscriptionPlan, admin_id: uuid.UUID,
    method: PaymentMethod, reference: str | None = None,
) -> Subscription:
    """Activa un pase. Si ya hay uno vigente, el nuevo se acumula al final."""
    driver = (
        await session.execute(select(DriverProfile).where(DriverProfile.user_id == driver_id).with_for_update())
    ).scalar_one()
    now = datetime.now(timezone.utc)
    start = driver.subscription_expires_at if driver.has_valid_subscription else now
    expires = start + PLAN_DURATION[plan]

    sub = Subscription(
        driver_id=driver_id,
        plan=plan,
        price_cop=plan_price(plan),
        status=SubscriptionStatus.active,
        payment_method=method,
        payment_reference=reference,
        starts_at=start,
        expires_at=expires,
        approved_by=admin_id,
    )
    session.add(sub)
    driver.subscription_active = True
    driver.subscription_expires_at = expires
    await session.flush()
    return sub
