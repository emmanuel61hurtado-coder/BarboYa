from datetime import datetime
from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import func, select, desc
from pydantic import BaseModel, Field, ConfigDict

from app.core.database import get_session
from app.core.security import CurrentUser, require_role
from app.models.enums import TripStatus, VerificationStatus
from app.models.trip import Trip
from app.models.user import DriverProfile, User

router = APIRouter(prefix="/drivers", tags=["drivers"])


class DriverLocationIn(BaseModel):
    lat: float = Field(ge=-90, le=90)
    lng: float = Field(ge=-180, le=180)
    is_online: bool = True


class DriverProfileOut(BaseModel):
    user_id: UUID
    full_name: str
    phone: str
    avatar_url: str | None = None
    vehicle_type: str
    vehicle_plate: str
    vehicle_brand: str | None = None
    vehicle_model: str | None = None
    vehicle_color: str | None = None
    vehicle_description: str | None = None
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
    verified_at: datetime | None = None
    billing_scheme: str
    wallet_balance_cop: int
    subscription_active: bool
    subscription_expires_at: datetime | None = None
    is_online: bool
    rating_avg: float
    rating_count: int


class DriverProfileUpdateIn(BaseModel):
    phone: str | None = Field(default=None, pattern=r"^\+?[0-9]{10,15}$")
    avatar_url: str | None = None
    vehicle_description: str | None = None
    vehicle_brand: str | None = None
    vehicle_model: str | None = None
    vehicle_color: str | None = None


class DriverDocumentsIn(BaseModel):
    license_doc_url: str = Field(min_length=5, examples=["https://storage.barboya.co/docs/lic_123.jpg"])
    soat_doc_url: str = Field(min_length=5, examples=["https://storage.barboya.co/docs/soat_123.jpg"])
    soat_expires_at: datetime | None = None
    tech_doc_url: str | None = None
    tech_expires_at: datetime | None = None
    selfie_reference_url: str = Field(min_length=5, examples=["https://storage.barboya.co/docs/selfie_123.jpg"])


class DriverTripHistoryItem(BaseModel):
    trip_id: UUID
    service_type: str
    status: str
    agreed_fare_cop: int
    platform_fee_cop: int
    net_earnings_cop: int
    pickup_note: str | None = None
    dropoff_note: str | None = None
    customer_rating: int | None = None
    created_at: datetime


@router.get("/me", response_model=DriverProfileOut)
async def get_driver_profile(
    user: CurrentUser = Depends(require_role("driver", "courier")),
    session: AsyncSession = Depends(get_session),
):
    driver = await session.get(DriverProfile, user.id)
    if driver is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Perfil de conductor no encontrado")
    user_row = await session.get(User, user.id)

    return DriverProfileOut(
        user_id=driver.user_id,
        full_name=user_row.full_name if user_row else "Conductor",
        phone=user_row.phone if user_row else "",
        avatar_url=user_row.avatar_url if user_row else None,
        vehicle_type=driver.vehicle_type.value,
        vehicle_plate=driver.vehicle_plate,
        vehicle_brand=driver.vehicle_brand,
        vehicle_model=driver.vehicle_model,
        vehicle_color=driver.vehicle_color,
        vehicle_description=driver.vehicle_description,
        document_id=driver.document_id,
        license_doc_url=driver.license_doc_url,
        soat_doc_url=driver.soat_doc_url,
        soat_expires_at=driver.soat_expires_at,
        tech_doc_url=driver.tech_doc_url,
        tech_expires_at=driver.tech_expires_at,
        selfie_reference_url=driver.selfie_reference_url,
        verification_status=driver.verification_status.value if driver.verification_status else "pending",
        is_verified=driver.is_verified,
        is_female_driver=driver.is_female_driver,
        rejection_reason=driver.rejection_reason,
        verified_at=driver.verified_at,
        billing_scheme=driver.billing_scheme.value,
        wallet_balance_cop=driver.wallet_balance_cop,
        subscription_active=driver.subscription_active,
        subscription_expires_at=driver.subscription_expires_at,
        is_online=driver.is_online,
        rating_avg=float(driver.rating_avg),
        rating_count=driver.rating_count,
    )


@router.put("/me", response_model=DriverProfileOut)
async def update_driver_profile(
    data: DriverProfileUpdateIn,
    user: CurrentUser = Depends(require_role("driver", "courier")),
    session: AsyncSession = Depends(get_session),
):
    driver = await session.get(DriverProfile, user.id)
    if driver is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Perfil de conductor no encontrado")
    user_row = await session.get(User, user.id)

    if data.phone and user_row:
        user_row.phone = data.phone
    if data.avatar_url and user_row:
        user_row.avatar_url = data.avatar_url

    if data.vehicle_description is not None:
        driver.vehicle_description = data.vehicle_description
    if data.vehicle_brand is not None:
        driver.vehicle_brand = data.vehicle_brand
    if data.vehicle_model is not None:
        driver.vehicle_model = data.vehicle_model
    if data.vehicle_color is not None:
        driver.vehicle_color = data.vehicle_color

    await session.commit()
    return await get_driver_profile(user, session)


@router.post("/me/documents", status_code=200)
async def submit_driver_documents(
    data: DriverDocumentsIn,
    user: CurrentUser = Depends(require_role("driver", "courier")),
    session: AsyncSession = Depends(get_session),
):
    driver = await session.get(DriverProfile, user.id)
    if driver is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Perfil de conductor no encontrado")

    driver.license_doc_url = data.license_doc_url
    driver.soat_doc_url = data.soat_doc_url
    driver.soat_expires_at = data.soat_expires_at
    driver.tech_doc_url = data.tech_doc_url
    driver.tech_expires_at = data.tech_expires_at
    driver.selfie_reference_url = data.selfie_reference_url
    driver.verification_status = VerificationStatus.under_review
    driver.rejection_reason = None

    await session.commit()
    return {
        "status": "under_review",
        "message": "Documentos subidos exitosamente. Se encuentran en revisión por el equipo de seguridad de BarboYa.",
    }


@router.get("/me/history", response_model=list[DriverTripHistoryItem])
async def get_driver_history(
    user: CurrentUser = Depends(require_role("driver", "courier")),
    session: AsyncSession = Depends(get_session),
):
    try:
        stmt = (
            select(Trip)
            .where(Trip.driver_id == user.id)
            .order_by(desc(Trip.requested_at))
            .limit(50)
        )
        rows = (await session.execute(stmt)).scalars().all()
        result = []
        for t in rows:
            fare = t.agreed_fare_cop or t.offered_fare_cop
            fee = t.platform_fee_cop or 0
            result.append(
                DriverTripHistoryItem(
                    trip_id=t.id,
                    service_type=t.service_type.value,
                    status=t.status.value,
                    agreed_fare_cop=fare,
                    platform_fee_cop=fee,
                    net_earnings_cop=max(0, fare - fee),
                    pickup_note=t.pickup_note,
                    dropoff_note=t.dropoff_note,
                    customer_rating=t.customer_rating,
                    created_at=t.requested_at,
                )
            )
        return result
    except Exception:
        return []


@router.post("/location", status_code=200)
async def update_driver_location(
    data: DriverLocationIn,
    user: CurrentUser = Depends(require_role("driver", "courier")),
    session: AsyncSession = Depends(get_session),
):
    driver = await session.get(DriverProfile, user.id)
    if driver is None:
        raise HTTPException(404, "Perfil de conductor no encontrado")

    # Regla: solo conductores verificados pueden ponerse en línea
    if data.is_online and not driver.is_verified:
        raise HTTPException(
            status.HTTP_403_FORBIDDEN,
            "Tu cuenta aún no está verificada. No puedes conectarte para recibir servicios.",
        )

    driver.current_location = func.ST_SetSRID(func.ST_MakePoint(data.lng, data.lat), 4326)
    driver.is_online = data.is_online
    driver.location_updated_at = func.now()
    await session.commit()
    return {"updated": True, "is_online": driver.is_online}
