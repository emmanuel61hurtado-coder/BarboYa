import uuid
from datetime import datetime

from sqlalchemy import DateTime, Enum, ForeignKey, Integer, String, Text, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base
from app.models.enums import (
    PaymentMethod,
    SubscriptionPlan,
    SubscriptionStatus,
    WalletTxStatus,
    WalletTxType,
)


class Subscription(Base):
    __tablename__ = "subscriptions"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    driver_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("driver_profiles.user_id"))
    plan: Mapped[SubscriptionPlan] = mapped_column(Enum(SubscriptionPlan, name="subscription_plan"))
    price_cop: Mapped[int] = mapped_column(Integer)
    status: Mapped[SubscriptionStatus] = mapped_column(
        Enum(SubscriptionStatus, name="subscription_status"), default=SubscriptionStatus.pending
    )
    payment_method: Mapped[PaymentMethod] = mapped_column(Enum(PaymentMethod, name="payment_method"))
    payment_reference: Mapped[str | None] = mapped_column(String)
    proof_url: Mapped[str | None] = mapped_column(Text)
    starts_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    approved_by: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id"))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class WalletTransaction(Base):
    """Libro mayor de la bolsa prepago. amount_cop > 0 recarga, < 0 descuento."""

    __tablename__ = "wallet_transactions"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    driver_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("driver_profiles.user_id"))
    type: Mapped[WalletTxType] = mapped_column(Enum(WalletTxType, name="wallet_tx_type"))
    status: Mapped[WalletTxStatus] = mapped_column(
        Enum(WalletTxStatus, name="wallet_tx_status"), default=WalletTxStatus.approved
    )
    amount_cop: Mapped[int] = mapped_column(Integer)
    balance_after_cop: Mapped[int | None] = mapped_column(Integer)
    payment_method: Mapped[PaymentMethod | None] = mapped_column(Enum(PaymentMethod, name="payment_method"))
    payment_reference: Mapped[str | None] = mapped_column(String)
    proof_url: Mapped[str | None] = mapped_column(Text)
    trip_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("trips.id"))
    note: Mapped[str | None] = mapped_column(Text)
    approved_by: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id"))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
