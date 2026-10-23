import secrets
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from geoalchemy2.shape import to_shape
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_session
from app.core.security import CurrentUser, require_role
from app.models.enums import TripStatus
from app.models.trip import Trip
from app.models.user import DriverProfile, User
from app.schemas.schemas import (
    AcceptOfferIn,
    CustomerCounterIn,
    DriverOfferIn,
    FareQuoteIn,
    FareQuoteOut,
    PinVerifyIn,
    TripCreatedOut,
    TripCreateIn,
    TripOut,
)
from app.services import billing, fares
from app.websockets.manager import manager

router = APIRouter(prefix="/trips", tags=["trips"])

NEARBY_RADIUS_M = 3_000


def _point(lat: float, lng: float):
    return func.ST_SetSRID(func.ST_MakePoint(lng, lat), 4326)


@router.post("/quote", response_model=FareQuoteOut)
async def quote_fare(data: FareQuoteIn, session: AsyncSession = Depends(get_session)):
    q = await fares.quote(session, (data.pickup.lat, data.pickup.lng), (data.dropoff.lat, data.dropoff.lng))
    return FareQuoteOut(**{k: v for k, v in q.items() if k != "zone_id"})


@router.post("", response_model=TripCreatedOut, status_code=201)
async def create_trip(
    data: TripCreateIn,
    user: CurrentUser = Depends(require_role("customer")),
    session: AsyncSession = Depends(get_session),
):
    q = await fares.quote(session, (data.pickup.lat, data.pickup.lng), (data.dropoff.lat, data.dropoff.lng))
    trip = Trip(
        customer_id=user.id,
        zone_id=q["zone_id"],
        service_type=data.service_type,
        status=TripStatus.requested,
        pickup_point=_point(data.pickup.lat, data.pickup.lng),
        pickup_note=data.pickup_note,
        dropoff_point=_point(data.dropoff.lat, data.dropoff.lng),
        dropoff_note=data.dropoff_note,
        distance_m=q["distance_m"],
        suggested_fare_cop=q["suggested_fare_cop"],
        offered_fare_cop=data.offered_fare_cop,
        payment_method=data.payment_method,
        prefer_female_driver=data.prefer_female_driver,
        confirmation_pin=f"{secrets.randbelow(10_000):04d}",
        share_token=secrets.token_urlsafe(16),
    )
    session.add(trip)
    await session.flush()

    # Conductores online cercanos (PostGIS) -> filtro de seguridad por género si la clienta lo solicitó
    stmt = select(DriverProfile).where(
        DriverProfile.is_online.is_(True),
        DriverProfile.is_verified.is_(True),
        func.ST_DWithin(DriverProfile.current_location, _point(data.pickup.lat, data.pickup.lng), NEARBY_RADIUS_M),
    )
    if data.prefer_female_driver:
        # Validación de conductora verificada en backend
        stmt = stmt.where(DriverProfile.is_female_driver.is_(True))

    drivers = (await session.execute(stmt)).scalars().all()
    eligible: list[UUID] = []
    for d in drivers:
        if (await billing.check_eligibility(session, d, trip.zone_id)).allowed:
            eligible.append(d.user_id)

    await session.commit()
    await manager.join_trip(trip.id, user.id)
    await manager.broadcast_new_trip(
        trip.id,
        eligible,
        {
            "service_type": data.service_type.value,
            "pickup": data.pickup.model_dump(),
            "pickup_note": data.pickup_note,
            "dropoff": data.dropoff.model_dump(),
            "dropoff_note": data.dropoff_note,
            "distance_m": q["distance_m"],
            "offered_fare_cop": data.offered_fare_cop,
            "suggested_fare_cop": q["suggested_fare_cop"],
            "payment_method": data.payment_method.value,
            "prefer_female_driver": data.prefer_female_driver,
        },
    )
    out = TripCreatedOut.model_validate(trip, from_attributes=True)
    out.confirmation_pin = trip.confirmation_pin
    out.share_url = f"/track/{trip.share_token}"
    out.female_drivers_nearby = len(eligible) if data.prefer_female_driver else 0
    return out


@router.post("/{trip_id}/offers", status_code=202)
async def driver_offer(
    trip_id: UUID,
    data: DriverOfferIn,
    user: CurrentUser = Depends(require_role("driver")),
    session: AsyncSession = Depends(get_session),
):
    """El conductor ofrece su precio (o contraoferta). Ya aquí se valida el cobro."""
    trip = await session.get(Trip, trip_id)
    if trip is None or trip.status not in (TripStatus.requested, TripStatus.negotiating):
        raise HTTPException(404, "Viaje no disponible")
    driver = await session.get(DriverProfile, user.id)
    elig = await billing.check_eligibility(session, driver, trip.zone_id)
    if not elig.allowed:
        raise HTTPException(status.HTTP_402_PAYMENT_REQUIRED, {"code": elig.code, "message": elig.reason})

    trip.status = TripStatus.negotiating
    profile_user = await session.get(User, user.id)
    await session.commit()

    await manager.driver_offer(
        trip.id,
        trip.customer_id,
        user.id,
        {
            "amount_cop": data.amount_cop,
            "eta_minutes": data.eta_minutes,
            "driver": {
                "name": profile_user.full_name,
                "avatar_url": profile_user.avatar_url,
                "vehicle": driver.vehicle_description or driver.vehicle_type.value,
                "plate": driver.vehicle_plate,
                "rating": float(driver.rating_avg),
                "rating_count": driver.rating_count,
            },
        },
    )
    return {"sent": True}


@router.post("/{trip_id}/counter", status_code=202)
async def customer_counter(
    trip_id: UUID,
    data: CustomerCounterIn,
    user: CurrentUser = Depends(require_role("customer")),
    session: AsyncSession = Depends(get_session),
):
    trip = await session.get(Trip, trip_id)
    if trip is None or trip.customer_id != user.id:
        raise HTTPException(404, "Viaje no encontrado")
    delivered = await manager.customer_counter(trip_id, data.driver_id, data.amount_cop, user.id)
    return {"delivered": delivered}


@router.post("/{trip_id}/accept", response_model=TripOut)
async def customer_accepts_offer(
    trip_id: UUID,
    data: AcceptOfferIn,
    user: CurrentUser = Depends(require_role("customer")),
    session: AsyncSession = Depends(get_session),
):
    """El cliente elige a un conductor. Se re-valida el cobro con la fila bloqueada."""
    trip = await session.get(Trip, trip_id)
    if trip is None or trip.customer_id != user.id:
        raise HTTPException(404, "Viaje no encontrado")
    try:
        trip = await billing.accept_trip(session, trip_id, data.driver_id, data.amount_cop)
    except billing.BillingError as e:
        await session.rollback()
        raise HTTPException(status.HTTP_409_CONFLICT, {"code": e.code, "message": e.message})
    await session.commit()
    await manager.notify_accepted(
        trip.id, trip.customer_id, data.driver_id, {"agreed_fare_cop": trip.agreed_fare_cop}
    )
    return TripOut.model_validate(trip, from_attributes=True)


@router.post("/{trip_id}/driver-accept", response_model=TripOut)
async def driver_accepts_at_offered_price(
    trip_id: UUID,
    user: CurrentUser = Depends(require_role("driver")),
    session: AsyncSession = Depends(get_session),
):
    """El conductor toma el viaje al precio que puso el cliente (1 toque)."""
    trip = await session.get(Trip, trip_id)
    if trip is None:
        raise HTTPException(404, "Viaje no encontrado")
    try:
        trip = await billing.accept_trip(session, trip_id, user.id, trip.offered_fare_cop)
    except billing.BillingError as e:
        await session.rollback()
        code = status.HTTP_402_PAYMENT_REQUIRED if e.code in ("INSUFFICIENT_BALANCE", "SUBSCRIPTION_EXPIRED") else 409
        raise HTTPException(code, {"code": e.code, "message": e.message})
    await session.commit()
    await manager.join_trip(trip.id, user.id)
    await manager.notify_accepted(trip.id, trip.customer_id, user.id, {"agreed_fare_cop": trip.agreed_fare_cop})
    return TripOut.model_validate(trip, from_attributes=True)


@router.post("/{trip_id}/verify-pin", response_model=TripOut)
async def verify_pin(
    trip_id: UUID,
    data: PinVerifyIn,
    user: CurrentUser = Depends(require_role("driver")),
    session: AsyncSession = Depends(get_session),
):
    from datetime import datetime, timezone

    trip = await session.get(Trip, trip_id)
    if trip is None or trip.driver_id != user.id:
        raise HTTPException(404, "Viaje no encontrado")
    if not secrets.compare_digest(trip.confirmation_pin, data.pin):
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "PIN incorrecto")
    trip.pin_verified_at = datetime.now(timezone.utc)
    trip.status = TripStatus.in_progress
    if data.proof_url:
        trip.delivery_proof_url = data.proof_url
    await session.commit()
    await manager.send_to(trip.customer_id, "trip.status", {"trip_id": str(trip.id), "status": trip.status.value})
    return TripOut.model_validate(trip, from_attributes=True)


@router.post("/{trip_id}/complete", response_model=TripOut)
async def complete_trip(
    trip_id: UUID,
    user: CurrentUser = Depends(require_role("driver")),
    session: AsyncSession = Depends(get_session),
):
    trip = await session.get(Trip, trip_id)
    if trip is None or trip.driver_id != user.id:
        raise HTTPException(404, "Viaje no encontrado")
    if trip.pin_verified_at is None:
        raise HTTPException(400, "Falta validar el PIN del cliente")
    trip = await billing.settle_completed_trip(session, trip_id)
    await session.commit()
    await manager.send_to(trip.customer_id, "trip.status", {"trip_id": str(trip.id), "status": "completed"})
    return TripOut.model_validate(trip, from_attributes=True)


# ------------------------------------------------------------------ enlace público de seguimiento
public_router = APIRouter(tags=["public"])


@public_router.get("/track/{token}")
async def track_trip(token: str, session: AsyncSession = Depends(get_session)):
    """Familiares abren este enlace sin instalar la app (datos mínimos, sin PIN)."""
    trip = (await session.execute(select(Trip).where(Trip.share_token == token))).scalar_one_or_none()
    if trip is None:
        raise HTTPException(404, "Enlace no válido")
    driver_loc = None
    driver_info = None
    if trip.driver_id:
        d = await session.get(DriverProfile, trip.driver_id)
        if d and d.current_location is not None:
            p = to_shape(d.current_location)
            driver_loc = {"lat": p.y, "lng": p.x}
        if d:
            driver_info = {"vehicle": d.vehicle_description, "plate": d.vehicle_plate}
    return {"status": trip.status.value, "driver_location": driver_loc, "driver": driver_info}
