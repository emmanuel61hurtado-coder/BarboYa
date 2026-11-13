from datetime import datetime, timezone
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_session
from app.core.security import CurrentUser, require_role
from app.models.billing import WalletTransaction
from app.models.enums import WalletTxStatus
from app.models.user import DriverProfile
from app.schemas.schemas import (
    BillingConfigIn,
    EligibilityOut,
    SubscriptionApproveIn,
    TopupDecisionIn,
    TopupIn,
    WalletOut,
)
from app.services import billing

router = APIRouter(tags=["wallet"])


@router.get("/wallet/me", response_model=WalletOut)
async def my_wallet(
    user: CurrentUser = Depends(require_role("driver")), session: AsyncSession = Depends(get_session)
):
    d = await session.get(DriverProfile, user.id)
    fee = await billing.resolve_fee(session, d, None)
    days_left = None
    if d.has_valid_subscription:
        days_left = max(0, (d.subscription_expires_at - datetime.now(timezone.utc)).days)
    return WalletOut(
        scheme=d.billing_scheme,
        balance_cop=d.wallet_balance_cop,
        subscription_active=d.has_valid_subscription,
        subscription_expires_at=d.subscription_expires_at,
        days_left=days_left,
        fee_cop=fee,
    )


@router.get("/wallet/eligibility", response_model=EligibilityOut)
async def my_eligibility(
    user: CurrentUser = Depends(require_role("driver")), session: AsyncSession = Depends(get_session)
):
    d = await session.get(DriverProfile, user.id)
    r = await billing.check_eligibility(session, d)
    return EligibilityOut(allowed=r.allowed, scheme=r.scheme, fee_cop=r.fee_cop, reason=r.reason, code=r.code)


@router.post("/wallet/topups", status_code=201)
async def request_topup(
    data: TopupIn,
    user: CurrentUser = Depends(require_role("driver")),
    session: AsyncSession = Depends(get_session),
):
    tx = await billing.request_topup(
        session, user.id, data.amount_cop, data.payment_method, data.payment_reference, data.proof_url
    )
    await session.commit()
    return {"id": tx.id, "status": tx.status.value}


# --------------------------------------------------------------------- admin
admin = APIRouter(prefix="/admin", tags=["admin"], dependencies=[Depends(require_role("admin"))])


@admin.get("/topups/pending")
async def pending_topups(session: AsyncSession = Depends(get_session)):
    rows = (
        await session.execute(
            select(WalletTransaction)
            .where(WalletTransaction.status == WalletTxStatus.pending)
            .order_by(WalletTransaction.created_at)
        )
    ).scalars()
    return [
        {
            "id": t.id,
            "driver_id": t.driver_id,
            "amount_cop": t.amount_cop,
            "method": t.payment_method,
            "reference": t.payment_reference,
            "proof_url": t.proof_url,
            "created_at": t.created_at,
        }
        for t in rows
    ]


@admin.post("/topups/{tx_id}/decision")
async def decide_topup(
    tx_id: UUID,
    data: TopupDecisionIn,
    admin_user: CurrentUser = Depends(require_role("admin")),
    session: AsyncSession = Depends(get_session),
):
    try:
        tx = await billing.approve_topup(session, tx_id, admin_user.id, data.approve)
    except billing.BillingError as e:
        raise HTTPException(status.HTTP_409_CONFLICT, {"code": e.code, "message": e.message})
    await session.commit()
    return {"id": tx.id, "status": tx.status.value, "balance_after_cop": tx.balance_after_cop}


@admin.post("/subscriptions", status_code=201)
async def grant_subscription(
    data: SubscriptionApproveIn,
    admin_user: CurrentUser = Depends(require_role("admin")),
    session: AsyncSession = Depends(get_session),
):
    sub = await billing.approve_subscription(
        session, data.driver_id, data.plan, admin_user.id, data.payment_method, data.payment_reference
    )
    await session.commit()
    return {"id": sub.id, "expires_at": sub.expires_at}


@admin.put("/drivers/billing")
async def configure_driver_billing(data: BillingConfigIn, session: AsyncSession = Depends(get_session)):
    """El admin cambia dinámicamente el esquema de cobro de un conductor."""
    d = await session.get(DriverProfile, data.driver_id)
    if d is None:
        raise HTTPException(404, "Conductor no encontrado")
    d.billing_scheme = data.scheme
    d.per_trip_fee_override_cop = data.per_trip_fee_override_cop
    await session.commit()
    return {"driver_id": d.user_id, "scheme": d.billing_scheme.value, "fee_override": d.per_trip_fee_override_cop}


@admin.post("/drivers/{driver_id}/verify")
async def verify_driver(driver_id: UUID, session: AsyncSession = Depends(get_session)):
    d = await session.get(DriverProfile, driver_id)
    if d is None:
        raise HTTPException(404, "Conductor no encontrado")
    d.is_verified = True
    await session.commit()
    return {"driver_id": d.user_id, "is_verified": True}
