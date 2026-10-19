import uuid
from datetime import datetime

from geoalchemy2 import Geography
from sqlalchemy import Boolean, CheckConstraint, DateTime, Enum, ForeignKey, Integer, Numeric, String, Text, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base
from app.models.enums import BillingScheme, Gender, UserRole, VehicleType, VerificationStatus


class User(Base):
    __tablename__ = "users"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    role: Mapped[UserRole] = mapped_column(Enum(UserRole, name="user_role"), default=UserRole.customer)
    full_name: Mapped[str] = mapped_column(Text)
    phone: Mapped[str] = mapped_column(String, unique=True)
    email: Mapped[str | None] = mapped_column(String, unique=True)
    password_hash: Mapped[str] = mapped_column(Text)
    avatar_url: Mapped[str | None] = mapped_column(Text)
    gender: Mapped[Gender | None] = mapped_column(Enum(Gender, name="user_gender"), nullable=True)
    default_address_note: Mapped[str | None] = mapped_column(Text)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    driver_profile: Mapped["DriverProfile | None"] = relationship(back_populates="user", uselist=False)


class DriverProfile(Base):
    """Perfil del conductor/repartidor.

    Aquí viven el saldo prepago (bolsa), verificación periódica y suscripción.
    Los importes están en COP enteros para evitar errores de coma flotante.
    """

    __tablename__ = "driver_profiles"
    __table_args__ = (CheckConstraint("wallet_balance_cop >= 0", name="ck_wallet_non_negative"),)

    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), primary_key=True
    )
    vehicle_type: Mapped[VehicleType] = mapped_column(Enum(VehicleType, name="vehicle_type"))
    vehicle_plate: Mapped[str] = mapped_column(String, unique=True)
    vehicle_description: Mapped[str | None] = mapped_column(Text)
    vehicle_brand: Mapped[str | None] = mapped_column(String, nullable=True)
    vehicle_model: Mapped[str | None] = mapped_column(String, nullable=True)
    vehicle_color: Mapped[str | None] = mapped_column(String, nullable=True)

    document_id: Mapped[str] = mapped_column(String, unique=True)
    license_doc_url: Mapped[str | None] = mapped_column(Text, nullable=True)
    soat_doc_url: Mapped[str | None] = mapped_column(Text, nullable=True)
    soat_expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    tech_doc_url: Mapped[str | None] = mapped_column(Text, nullable=True)
    tech_expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    selfie_reference_url: Mapped[str | None] = mapped_column(Text)

    verification_status: Mapped[VerificationStatus] = mapped_column(
        Enum(VerificationStatus, name="verification_status"), default=VerificationStatus.pending
    )
    is_verified: Mapped[bool] = mapped_column(Boolean, default=False)
    is_female_driver: Mapped[bool] = mapped_column(Boolean, default=False)
    rejection_reason: Mapped[str | None] = mapped_column(Text, nullable=True)
    verified_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    verified_by: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), nullable=True)

    billing_scheme: Mapped[BillingScheme] = mapped_column(
        Enum(BillingScheme, name="billing_scheme"), default=BillingScheme.per_trip_fee
    )
    wallet_balance_cop: Mapped[int] = mapped_column(Integer, default=0)
    per_trip_fee_override_cop: Mapped[int | None] = mapped_column(Integer)

    subscription_active: Mapped[bool] = mapped_column(Boolean, default=False)
    subscription_expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))

    is_online: Mapped[bool] = mapped_column(Boolean, default=False)
    face_check_due: Mapped[bool] = mapped_column(Boolean, default=False)
    last_face_check_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))

    rating_avg: Mapped[float] = mapped_column(Numeric(3, 2), default=5.0)
    rating_count: Mapped[int] = mapped_column(Integer, default=0)
    current_location = mapped_column(Geography("POINT", srid=4326), nullable=True)

    user: Mapped[User] = relationship(back_populates="driver_profile")

    @property
    def has_valid_subscription(self) -> bool:
        """True si el pase está activo y no ha vencido (se evalúa al momento de la consulta)."""
        from datetime import timezone

        if not self.subscription_active or self.subscription_expires_at is None:
            return False
        return self.subscription_expires_at > datetime.now(timezone.utc)
