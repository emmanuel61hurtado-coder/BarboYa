import uuid

from geoalchemy2 import Geometry
from sqlalchemy import Boolean, Enum, Integer, Numeric, Text, Time
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base
from app.models.enums import BillingScheme


class Zone(Base):
    __tablename__ = "zones"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    name: Mapped[str] = mapped_column(Text, unique=True)
    is_urban: Mapped[bool] = mapped_column(Boolean, default=True)
    boundary = mapped_column(Geometry("MULTIPOLYGON", srid=4326))
    base_fare_cop: Mapped[int] = mapped_column(Integer)
    per_km_cop: Mapped[int] = mapped_column(Integer, default=0)
    night_surcharge_pct: Mapped[float] = mapped_column(Numeric(5, 2), default=0)
    night_start = mapped_column(Time, default="20:00")
    night_end = mapped_column(Time, default="05:00")
    billing_scheme: Mapped[BillingScheme] = mapped_column(
        Enum(BillingScheme, name="billing_scheme"), default=BillingScheme.per_trip_fee
    )
    per_trip_fee_cop: Mapped[int] = mapped_column(Integer, default=300)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
