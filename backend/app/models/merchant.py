import uuid
from datetime import datetime
from geoalchemy2 import Geography
from sqlalchemy import Boolean, DateTime, Enum, ForeignKey, Integer, Numeric, String, Text, func
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base
from app.models.enums import MerchantCategory, MerchantStatus, OrderStatus, PaymentMethod


class Merchant(Base):
    __tablename__ = "merchants"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    owner_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id"))
    name: Mapped[str] = mapped_column(Text, nullable=False)
    category: Mapped[MerchantCategory] = mapped_column(Enum(MerchantCategory, name="merchant_category"))
    description: Mapped[str | None] = mapped_column(Text)
    logo_url: Mapped[str | None] = mapped_column(Text)
    cover_image_url: Mapped[str | None] = mapped_column(Text)
    address_note: Mapped[str] = mapped_column(Text, nullable=False)
    location = mapped_column(Geography("POINT", srid=4326), nullable=False)
    phone: Mapped[str | None] = mapped_column(String)
    whatsapp: Mapped[str | None] = mapped_column(String)
    opening_hours: Mapped[str | None] = mapped_column(Text)
    commission_pct: Mapped[float] = mapped_column(Numeric(4, 2), default=10.00)
    approval_status: Mapped[MerchantStatus] = mapped_column(
        Enum(MerchantStatus, name="merchant_status"), default=MerchantStatus.approved
    )
    is_open: Mapped[bool] = mapped_column(Boolean, default=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class Product(Base):
    __tablename__ = "products"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    merchant_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("merchants.id", ondelete="CASCADE"))
    name: Mapped[str] = mapped_column(Text, nullable=False)
    description: Mapped[str | None] = mapped_column(Text)
    price_cop: Mapped[int] = mapped_column(Integer, nullable=False)
    image_url: Mapped[str | None] = mapped_column(Text)
    is_available: Mapped[bool] = mapped_column(Boolean, default=True)
    stock: Mapped[int | None] = mapped_column(Integer)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class Order(Base):
    __tablename__ = "orders"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    customer_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id"))
    merchant_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("merchants.id"))
    courier_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("driver_profiles.user_id"), nullable=True)
    trip_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("trips.id"))
    status: Mapped[OrderStatus] = mapped_column(Enum(OrderStatus, name="order_status"), default=OrderStatus.placed)
    status_history: Mapped[list] = mapped_column(JSONB, server_default="[]")
    delivery_pin: Mapped[str | None] = mapped_column(String(4), nullable=True)
    items: Mapped[dict] = mapped_column(JSONB, nullable=False)
    subtotal_cop: Mapped[int] = mapped_column(Integer, nullable=False)
    delivery_fee_cop: Mapped[int] = mapped_column(Integer, default=0)
    commission_cop: Mapped[int] = mapped_column(Integer, default=0)
    total_cop: Mapped[int] = mapped_column(Integer, nullable=False)
    payment_method: Mapped[PaymentMethod] = mapped_column(Enum(PaymentMethod, name="payment_method"), default=PaymentMethod.cash)
    delivery_address_note: Mapped[str | None] = mapped_column(Text)
    delivery_point = mapped_column(Geography("POINT", srid=4326))
    started_via_whatsapp: Mapped[bool] = mapped_column(Boolean, default=False)
    prep_minutes: Mapped[int | None] = mapped_column(Integer)
    rejection_reason: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
