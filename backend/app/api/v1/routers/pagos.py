from fastapi import APIRouter, Depends, Header, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from app.db.session import get_db
from app.models.pago import Pago, EstadoPago
from app.models.pedido import Pedido
from app.schemas.pago import PagoRead, PagoCreate
from app.core.exceptions import DomainException
from app.deps import get_current_user

router = APIRouter(prefix="/pagos", tags=["Pagos"])


@router.post("", response_model=PagoRead, status_code=status.HTTP_201_CREATED)
async def process_pago(payload: PagoCreate, idempotency_key: str = Header(None), db: AsyncSession = Depends(get_db)):
    # Check idempotency or process mock payment
    result = await db.execute(select(Pago).where(Pago.pedido_id == payload.pedido_id))
    existing = result.scalar_one_or_none()
    if existing:
        return existing
        
    pago = Pago(
        pedido_id=payload.pedido_id,
        metodo=payload.metodo,
        estado=EstadoPago.COMPLETADO,
        monto=payload.monto,
        transaccion_id=f"tx_mock_{idempotency_key or 'uuid'}"
    )
    db.add(pago)
    await db.commit()
    await db.refresh(pago)
    return pago
