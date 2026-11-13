from datetime import datetime, timezone
from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select, func, desc
from sqlalchemy.ext.asyncio import AsyncSession
from pydantic import BaseModel, Field, ConfigDict

from app.core.database import get_session
from app.core.security import CurrentUser, get_current_user, require_role
from app.models.enums import MerchantStatus, SecurityEventType, VerificationStatus
from app.models.user import DriverProfile, User
from app.models.trip import SecurityLog, Trip
from app.models.merchant import Merchant, Order

router = APIRouter(
    prefix="/admin",
    tags=["admin-management"],
    dependencies=[Depends(require_role("admin", "superadmin"))],
)


class UserAdminOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: UUID
    full_name: str
    phone: str
    email: str | None
    role: str
    is_active: bool


class UserStatusIn(BaseModel):
    is_active: bool


class DriverVerificationDecisionIn(BaseModel):
    action: str = Field(description="approve | reject | suspend")
    reason: str | None = None
    is_female_driver: bool = False


class MerchantApprovalDecisionIn(BaseModel):
    action: str = Field(description="approve | reject | suspend")
    reason: str | None = None


class DriverAdminItem(BaseModel):
    user_id: UUID
    full_name: str
    phone: str
    vehicle_type: str
    vehicle_plate: str
    document_id: str
    license_doc_url: str | None = None
    soat_doc_url: str | None = None
    soat_expires_at: datetime | None = None
    tech_doc_url: str | None = None
    tech_expires_at: datetime | None = None
    selfie_reference_url: str | None = None
    verification_status: str
    is_verified: bool
    is_female_driver: bool
    rejection_reason: str | None = None
    rating_avg: float
    wallet_balance_cop: int


class MerchantAdminItem(BaseModel):
    id: UUID
    name: str
    category: str
    owner_name: str
    phone: str | None = None
    address_note: str
    approval_status: str
    is_open: bool
    is_active: bool


class AuditLogItem(BaseModel):
    id: UUID
    event_type: str
    user_id: UUID
    trip_id: UUID | None = None
    resolved: bool
    created_at: datetime
    details: dict


# ─────────────────────────────────────────────────────────────────────────────
# USUARIOS Y DASHBOARD STATS
# ─────────────────────────────────────────────────────────────────────────────

@router.get("/users", response_model=list[UserAdminOut])
async def admin_list_users(session: AsyncSession = Depends(get_session)):
    try:
        rows = (await session.execute(select(User).order_by(User.created_at.desc()).limit(100))).scalars().all()
        return [UserAdminOut.model_validate(u, from_attributes=True) for u in rows]
    except Exception:
        return []


@router.post("/users/{user_id}/status", response_model=UserAdminOut)
async def admin_set_user_status(user_id: UUID, data: UserStatusIn, session: AsyncSession = Depends(get_session)):
    user = await session.get(User, user_id)
    if user is None:
        raise HTTPException(404, "Usuario no encontrado")
    user.is_active = data.is_active
    await session.commit()
    return UserAdminOut.model_validate(user, from_attributes=True)


@router.get("/dashboard/stats")
async def admin_dashboard_stats(session: AsyncSession = Depends(get_session)):
    try:
        users_count = (await session.execute(select(func.count(User.id)))).scalar() or 0
        trips_count = (await session.execute(select(func.count(Trip.id)))).scalar() or 0
        orders_count = (await session.execute(select(func.count(Order.id)))).scalar() or 0
        drivers_verified = (
            await session.execute(select(func.count(DriverProfile.user_id)).where(DriverProfile.is_verified.is_(True)))
        ).scalar() or 0
        pending_verifications = (
            await session.execute(
                select(func.count(DriverProfile.user_id)).where(
                    DriverProfile.verification_status.in_([VerificationStatus.pending, VerificationStatus.under_review])
                )
            )
        ).scalar() or 0

        return {
            "total_users": users_count,
            "total_trips": trips_count,
            "total_orders": orders_count,
            "drivers_verified": drivers_verified,
            "pending_verifications": pending_verifications,
        }
    except Exception:
        return {
            "total_users": 0,
            "total_trips": 0,
            "total_orders": 0,
            "drivers_verified": 0,
            "pending_verifications": 0,
        }


# ─────────────────────────────────────────────────────────────────────────────
# GESTIÓN Y VERIFICACIÓN CONSTANTE DE CONDUCTORES (Modulo 6 y Modulo 9)
# ─────────────────────────────────────────────────────────────────────────────

@router.get("/drivers/verification", response_model=list[DriverAdminItem])
async def list_drivers_for_verification(session: AsyncSession = Depends(get_session)):
    stmt = (
        select(DriverProfile, User)
        .join(User, DriverProfile.user_id == User.id)
        .order_by(desc(User.created_at))
        .limit(100)
    )
    rows = (await session.execute(stmt)).all()

    result = []
    for drv, usr in rows:
        result.append(
            DriverAdminItem(
                user_id=drv.user_id,
                full_name=usr.full_name,
                phone=usr.phone,
                vehicle_type=drv.vehicle_type.value,
                vehicle_plate=drv.vehicle_plate,
                document_id=drv.document_id,
                license_doc_url=drv.license_doc_url,
                soat_doc_url=drv.soat_doc_url,
                soat_expires_at=drv.soat_expires_at,
                tech_doc_url=drv.tech_doc_url,
                tech_expires_at=drv.tech_expires_at,
                selfie_reference_url=drv.selfie_reference_url,
                verification_status=drv.verification_status.value if drv.verification_status else "pending",
                is_verified=drv.is_verified,
                is_female_driver=drv.is_female_driver,
                rejection_reason=drv.rejection_reason,
                rating_avg=float(drv.rating_avg),
                wallet_balance_cop=drv.wallet_balance_cop,
            )
        )
    return result


@router.post("/drivers/{driver_id}/verify", status_code=200)
async def verify_driver(
    driver_id: UUID,
    data: DriverVerificationDecisionIn,
    current_admin: CurrentUser = Depends(require_role("admin", "superadmin")),
    session: AsyncSession = Depends(get_session),
):
    driver = await session.get(DriverProfile, driver_id)
    if not driver:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Conductor no encontrado")

    now = datetime.now(timezone.utc)
    driver.verified_by = current_admin.id

    if data.action == "approve":
        driver.is_verified = True
        driver.verification_status = VerificationStatus.verified
        driver.verified_at = now
        driver.rejection_reason = None
        driver.is_female_driver = data.is_female_driver
        msg = "Conductor verificado y habilitado para operar"
    elif data.action == "reject":
        driver.is_verified = False
        driver.is_online = False
        driver.verification_status = VerificationStatus.rejected
        driver.rejection_reason = data.reason or "Documentación no cumple con los requisitos mínimos"
        msg = f"Conductor rechazado: {driver.rejection_reason}"
    elif data.action == "suspend":
        driver.is_verified = False
        driver.is_online = False
        driver.verification_status = VerificationStatus.suspended
        driver.rejection_reason = data.reason or "Suspendido administrativamente"
        msg = f"Conductor suspendido: {driver.rejection_reason}"
    else:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Acción inválida. Usa approve, reject o suspend")

    await session.commit()
    return {
        "driver_id": str(driver.user_id),
        "status": driver.verification_status.value,
        "is_verified": driver.is_verified,
        "is_female_driver": driver.is_female_driver,
        "message": msg,
    }


# ─────────────────────────────────────────────────────────────────────────────
# GESTIÓN Y APROBACIÓN DE COMERCIOS (Modulo 9)
# ─────────────────────────────────────────────────────────────────────────────

@router.get("/merchants/approval", response_model=list[MerchantAdminItem])
async def list_merchants_for_approval(session: AsyncSession = Depends(get_session)):
    stmt = (
        select(Merchant, User)
        .join(User, Merchant.owner_id == User.id)
        .order_by(desc(Merchant.created_at))
        .limit(100)
    )
    rows = (await session.execute(stmt)).all()

    result = []
    for m, u in rows:
        result.append(
            MerchantAdminItem(
                id=m.id,
                name=m.name,
                category=m.category.value,
                owner_name=u.full_name,
                phone=m.phone or u.phone,
                address_note=m.address_note,
                approval_status=m.approval_status.value if m.approval_status else "approved",
                is_open=m.is_open,
                is_active=m.is_active,
            )
        )
    return result


@router.post("/merchants/{merchant_id}/approval", status_code=200)
async def process_merchant_approval(
    merchant_id: UUID,
    data: MerchantApprovalDecisionIn,
    session: AsyncSession = Depends(get_session),
):
    merchant = await session.get(Merchant, merchant_id)
    if not merchant:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Comercio no encontrado")

    if data.action == "approve":
        merchant.approval_status = MerchantStatus.approved
        merchant.is_active = True
        msg = "Comercio aprobado y visible al público"
    elif data.action == "reject":
        merchant.approval_status = MerchantStatus.rejected
        merchant.is_active = False
        merchant.is_open = False
        msg = f"Comercio rechazado: {data.reason or 'No cumple con requisitos'}"
    elif data.action == "suspend":
        merchant.approval_status = MerchantStatus.suspended
        merchant.is_active = False
        merchant.is_open = False
        msg = f"Comercio suspendido: {data.reason or 'Violación de políticas'}"
    else:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Acción inválida. Usa approve, reject o suspend")

    await session.commit()
    return {"merchant_id": str(merchant.id), "status": merchant.approval_status.value, "message": msg}


# ─────────────────────────────────────────────────────────────────────────────
# REGISTROS DE AUDITORÍA Y SEGURIDAD (Modulo 11)
# ─────────────────────────────────────────────────────────────────────────────

@router.get("/audit-logs", response_model=list[AuditLogItem])
async def list_audit_logs(session: AsyncSession = Depends(get_session)):
    stmt = select(SecurityLog).order_by(desc(SecurityLog.created_at)).limit(100)
    rows = (await session.execute(stmt)).scalars().all()
    return [
        AuditLogItem(
            id=l.id,
            event_type=l.event_type.value,
            user_id=l.user_id,
            trip_id=l.trip_id,
            resolved=l.resolved,
            created_at=l.created_at,
            details=l.details or {},
        )
        for l in rows
    ]
