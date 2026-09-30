from decimal import Decimal
from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from app.db.session import get_db
from app.models.calificacion import Calificacion
from app.models.pedido import Pedido, EstadoPedido
from app.models.comercio import Comercio
from app.models.user import User, UserRole
from app.schemas.calificacion import CalificacionRead, CalificacionCreate
from app.deps import require_role
from app.core.exceptions import DomainException

router = APIRouter(prefix="/calificaciones", tags=["Calificaciones"])


@router.post("", response_model=CalificacionRead, status_code=status.HTTP_201_CREATED)
async def create_calificacion(payload: CalificacionCreate, current_user: User = Depends(require_role(UserRole.CLIENTE)), db: AsyncSession = Depends(get_db)):
    pedido_res = await db.execute(select(Pedido).where(Pedido.id == payload.pedido_id, Pedido.cliente_id == current_user.id))
    pedido = pedido_res.scalar_one_or_none()
    if not pedido or pedido.estado != EstadoPedido.ENTREGADO:
        raise DomainException("INVALID_ORDER", "El pedido no existe o no ha sido entregado", status.HTTP_400_BAD_REQUEST)
        
    calif = Calificacion(
        cliente_id=current_user.id,
        **payload.model_dump()
    )
    db.add(calif)
    
    if payload.comercio_id:
        com_res = await db.execute(select(Comercio).where(Comercio.id == payload.comercio_id))
        comercio = com_res.scalar_one_or_none()
        if comercio:
            # Recalculate average (simplified)
            comercio.calificacion_promedio = (comercio.calificacion_promedio + Decimal(payload.puntuacion)) / 2
            
    await db.commit()
    await db.refresh(calif)
    return calif
