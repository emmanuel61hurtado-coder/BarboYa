import uuid
from datetime import datetime

from geoalchemy2 import Geography
from sqlalchemy import Boolean, DateTime, Enum, ForeignKey, Integer, String, Text, func
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base
from app.models.enums import PaymentMethod, SecurityEventType, ServiceType, TripStatus


class Trip(Base):
    __tablename__ = "trips"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    customer_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id"))
    driver_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), ForeignKey("driver_profiles.user_id")
    )
    zone_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("zones.id"))
    service_type: Mapped[ServiceType] = mapped_column(Enum(ServiceType, name="service_type"))
    status: Mapped[TripStatus] = mapped_column(
        Enum(TripStatus, name="trip_status"), default=TripStatus.requested
    )
    pickup_point = mapped_column(Geography("POINT", srid=4326))
    pickup_note: Mapped[str | None] = mapped_column(Text)
    dropoff_point = mapped_column(Geography("POINT", srid=4326))
    dropoff_note: Mapped[str | None] = mapped_column(Text)
    distance_m: Mapped[int | None] = mapped_column(Integer)
    suggested_fare_cop: Mapped[int] = mapped_column(Integer)
    offered_fare_cop: Mapped[int] = mapped_column(Integer)
    agreed_fare_cop: Mapped[int | None] = mapped_column(Integer)
    platform_fee_cop: Mapped[int] = mapped_column(Integer, default=0)
    payment_method: Mapped[PaymentMethod] = mapped_column(
        Enum(PaymentMethod, name="payment_method"), default=PaymentMethod.cash
    )
    confirmation_pin: Mapped[str] = mapped_column(String(4))
    pin_verified_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    delivery_proof_url: Mapped[str | None] = mapped_column(Text)
    share_token: Mapped[str | None] = mapped_column(String, unique=True)
    requested_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    accepted_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class SecurityLog(Base):
    __tablename__ = "security_logs"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id"))
    trip_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("trips.id"))
    event_type: Mapped[SecurityEventType] = mapped_column(Enum(SecurityEventType, name="security_event_type"))
    location = mapped_column(Geography("POINT", srid=4326), nullable=True)
    details: Mapped[dict] = mapped_column(JSONB, server_default="{}")
    resolved: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
