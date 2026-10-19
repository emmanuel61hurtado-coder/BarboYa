from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

from app.models.enums import (
    BillingScheme,
    PaymentMethod,
    ServiceType,
    SubscriptionPlan,
    TripStatus,
    UserRole,
    VehicleType,
)


class GeoPoint(BaseModel):
    lat: float = Field(ge=-90, le=90)
    lng: float = Field(ge=-180, le=180)


# ----------------------------------------------------------------- auth
class RegisterIn(BaseModel):
    full_name: str = Field(min_length=2, max_length=120)
    phone: str = Field(pattern=r"^\+?[0-9]{10,15}$")
    password: str = Field(min_length=6, max_length=72)
    role: UserRole = UserRole.customer
    # Solo si role == driver
    vehicle_type: VehicleType | None = None
    vehicle_plate: str | None = None
    document_id: str | None = None


class LoginIn(BaseModel):
    phone: str
    password: str


class TokenOut(BaseModel):
    access_token: str
    token_type: str = "bearer"
    role: UserRole
    user_id: UUID


# ----------------------------------------------------------------- viajes
class FareQuoteIn(BaseModel):
    pickup: GeoPoint
    dropoff: GeoPoint
    service_type: ServiceType = ServiceType.passenger


class FareQuoteOut(BaseModel):
    distance_m: int
    suggested_fare_cop: int
    min_fare_cop: int
    max_fare_cop: int
    night_surcharge_applied: bool
    zone_name: str | None


class TripCreateIn(BaseModel):
    service_type: ServiceType
    pickup: GeoPoint
    pickup_note: str | None = Field(default=None, max_length=200, examples=["frente al Parque Principal"])
    dropoff: GeoPoint
    dropoff_note: str | None = Field(default=None, max_length=200, examples=["al lado del D1"])
    offered_fare_cop: int = Field(gt=0, le=500_000)
    payment_method: PaymentMethod = PaymentMethod.cash
    package_notes: str | None = None
    prefer_female_driver: bool = False
    started_via_whatsapp: bool = False


class TripOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    status: TripStatus
    service_type: ServiceType
    suggested_fare_cop: int
    offered_fare_cop: int
    agreed_fare_cop: int | None = None
    payment_method: PaymentMethod
    driver_id: UUID | None = None
    requested_at: datetime


class TripCreatedOut(TripOut):
    confirmation_pin: str   # solo lo ve el cliente que creó el viaje
    share_url: str
    female_drivers_nearby: int = 0


class DriverOfferIn(BaseModel):
    amount_cop: int = Field(gt=0, le=500_000)
    eta_minutes: int | None = Field(default=None, ge=0, le=120)


class CustomerCounterIn(BaseModel):
    driver_id: UUID
    amount_cop: int = Field(gt=0, le=500_000)


class AcceptOfferIn(BaseModel):
    driver_id: UUID
    amount_cop: int = Field(gt=0)


class PinVerifyIn(BaseModel):
    pin: str = Field(pattern=r"^[0-9]{4}$")
    proof_url: str | None = None


# ----------------------------------------------------------------- billing
class EligibilityOut(BaseModel):
    allowed: bool
    scheme: BillingScheme
    fee_cop: int
    reason: str | None = None
    code: str | None = None


class WalletOut(BaseModel):
    scheme: BillingScheme
    balance_cop: int
    subscription_active: bool
    subscription_expires_at: datetime | None
    days_left: int | None
    fee_cop: int


class TopupIn(BaseModel):
    amount_cop: int = Field(ge=1_000, le=500_000)
    payment_method: PaymentMethod
    payment_reference: str | None = None
    proof_url: str | None = None


class TopupDecisionIn(BaseModel):
    approve: bool


class SubscriptionApproveIn(BaseModel):
    driver_id: UUID
    plan: SubscriptionPlan
    payment_method: PaymentMethod
    payment_reference: str | None = None


class BillingConfigIn(BaseModel):
    driver_id: UUID
    scheme: BillingScheme
    per_trip_fee_override_cop: int | None = Field(default=None, ge=0)
