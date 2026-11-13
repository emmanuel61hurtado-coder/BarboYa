"""Rutas de pagos con criptomonedas (EVM). Ver docs/blockchain-payments.md.

Conductor (su billetera ya firmó el tx, paso 1):
    POST /payments/crypto            -> registra el txHash y el monto esperado
    GET  /payments/crypto            -> historial de sus pagos on-chain
    GET  /payments/crypto/{id}       -> estado actual (sincroniza con el nodo)

Admin:
    POST /admin/payments/crypto/sync -> fuerza un barrido del indexador (paso 7)
"""
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import get_settings
from app.core.database import get_session
from app.core.security import CurrentUser, require_role
from app.models.blockchain import BlockchainPayment
from app.models.enums import BlockchainTxStatus
from app.models.user import DriverProfile
from app.schemas.schemas import CryptoPaymentIn, CryptoPaymentOut
from app.services import blockchain

settings = get_settings()

router = APIRouter(prefix="/payments/crypto", tags=["blockchain-payments"])
admin = APIRouter(
    prefix="/admin/payments/crypto",
    tags=["blockchain-payments"],
    dependencies=[Depends(require_role("admin"))],
)


def _out(p: BlockchainPayment) -> CryptoPaymentOut:
    return CryptoPaymentOut(
        id=p.id,
        tx_hash=p.tx_hash,
        payer_address=p.payer_address,
        status=p.status.value,
        token_symbol=p.token_symbol,
        token_amount_units=p.token_amount_units,
        amount_cop=p.amount_cop,
        chain_id=p.chain_id,
        block_number=p.block_number,
        gas_used=p.gas_used,
        confirmations=p.confirmations,
        failure_reason=p.failure_reason,
        wallet_tx_id=p.wallet_tx_id,
        created_at=p.created_at,
        confirmed_at=p.confirmed_at,
    )


def _check_enabled() -> None:
    if not settings.payment_token_address:
        raise HTTPException(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            {"code": "CHAIN_NOT_CONFIGURED", "message": "El método de pago cripto no está configurado"},
        )


@router.post("", status_code=201, response_model=CryptoPaymentOut)
async def register_payment(
    data: CryptoPaymentIn,
    user: CurrentUser = Depends(require_role("driver")),
    session: AsyncSession = Depends(get_session),
):
    """Paso 1: el tx ya fue firmado por la billetera y difundido al nodo."""
    _check_enabled()
    driver = await session.get(DriverProfile, user.id)
    if driver is None:
        raise HTTPException(404, "No tienes perfil de conductor")
    try:
        payment = await blockchain.register_payment(session, driver, data.tx_hash, data.payer_address, data.token_amount_units)
    except blockchain.ChainError as e:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, {"code": e.code, "message": e.message})
    await session.commit()
    return _out(payment)


@router.get("", response_model=list[CryptoPaymentOut])
async def my_crypto_payments(
    user: CurrentUser = Depends(require_role("driver")),
    session: AsyncSession = Depends(get_session),
):
    rows = (
        await session.execute(
            select(BlockchainPayment)
            .where(BlockchainPayment.driver_id == user.id)
            .order_by(BlockchainPayment.created_at.desc())
        )
    ).scalars()
    return [_out(p) for p in rows]


@router.get("/{payment_id}", response_model=CryptoPaymentOut)
async def payment_status(
    payment_id: UUID,
    user: CurrentUser = Depends(require_role("driver")),
    session: AsyncSession = Depends(get_session),
):
    """Consulta el estado; de paso corre un ciclo del indexador para refrescarlo."""
    _check_enabled()
    payment = await session.get(BlockchainPayment, payment_id)
    if payment is None or payment.driver_id != user.id:
        raise HTTPException(404, "Pago no encontrado")
    if payment.status in (BlockchainTxStatus.pending, BlockchainTxStatus.confirming):
        await blockchain.sync_payment(session, payment)
        await session.commit()
    return _out(payment)


@admin.post("/sync")
async def force_sync(session: AsyncSession = Depends(get_session)):
    """Barrido manual del indexador (en producción lo hace la tarea de fondo)."""
    _check_enabled()
    synced = await blockchain.sync_pending_payments(session)
    await session.commit()
    return {
        "synced": len(synced),
        "payments": [
            {"id": p.id, "tx_hash": p.tx_hash, "status": p.status.value, "amount_cop": p.amount_cop}
            for p in synced
        ],
    }
