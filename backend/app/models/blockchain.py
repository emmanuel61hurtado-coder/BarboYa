"""Pagos con criptomonedas (EVM). Ver docs/blockchain-payments.md.

Flujo (de la imagen de arquitectura):
    1. Billetera firma con su clave  -> el conductor envía txHash + monto esperado
    2. Nodo RPC valida (JSON-RPC)    -> app/services/blockchain.py consulta eth_getTransactionReceipt
    3. Mempool                       -> status = pending hasta que exista recibo
    4. Bloque / 5. EVM               -> el contrato del token ejecuta el transfer
    6. Recibo y logs                 -> status, blockNumber, gasUsed, logs del evento
    7. Indexador                     -> sincroniza y verifica el evento Transfer
    8. PostgreSQL                    -> esta tabla; consultas en ms para la app
"""
import uuid
from datetime import datetime

from sqlalchemy import BigInteger, DateTime, Enum, ForeignKey, Index, Integer, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column

from app.core.compat_types import GUID, JsonDict
from app.core.database import Base
from app.models.enums import BlockchainTxStatus, PaymentMethod


class BlockchainPayment(Base):
    __tablename__ = "blockchain_payments"
    __table_args__ = (
        Index("ix_blockchain_payments_status", "status"),
        Index("ix_blockchain_payments_driver", "driver_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(GUID(), primary_key=True, default=uuid.uuid4)
    driver_id: Mapped[uuid.UUID] = mapped_column(GUID(), ForeignKey("driver_profiles.user_id"))

    # 1. Billetera: datos del tx firmado fuera de la cadena (off-chain)
    tx_hash: Mapped[str] = mapped_column(String(66), unique=True)
    payer_address: Mapped[str] = mapped_column(String(42))

    # 2-3. Nodo RPC / mempool
    chain_id: Mapped[int] = mapped_column(Integer)
    status: Mapped[BlockchainTxStatus] = mapped_column(
        Enum(BlockchainTxStatus, name="blockchain_tx_status"), default=BlockchainTxStatus.pending
    )

    # Token esperado (ej. USDC) en unidades mínimas del token (6 decimales)
    token_address: Mapped[str] = mapped_column(String(42))
    token_symbol: Mapped[str] = mapped_column(String(12))
    token_amount_units: Mapped[int] = mapped_column(BigInteger)
    decimals: Mapped[int] = mapped_column(Integer, default=6)

    # Conversión a la moneda del negocio
    amount_cop: Mapped[int] = mapped_column(Integer)
    cop_per_token: Mapped[int] = mapped_column(Integer)

    # 6. Recibo y logs (crudo, tal como lo devolvió el RPC)
    block_number: Mapped[int | None] = mapped_column(Integer)
    gas_used: Mapped[int | None] = mapped_column(BigInteger)
    confirmations: Mapped[int | None] = mapped_column(Integer)
    receipt: Mapped[dict | None] = mapped_column(JsonDict)
    failure_reason: Mapped[str | None] = mapped_column(Text)

    # 8. PostgreSQL: al confirmar se crea la recarga real en el libro mayor
    wallet_tx_id: Mapped[uuid.UUID | None] = mapped_column(
        GUID(), ForeignKey("wallet_transactions.id"), unique=True
    )
    payment_method: Mapped[PaymentMethod] = mapped_column(
        Enum(PaymentMethod, name="payment_method"), default=PaymentMethod.crypto
    )

    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    confirmed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    indexed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
