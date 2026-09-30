from decimal import Decimal
from uuid import UUID
from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.core.exceptions import DomainException
from app.db.session import get_db
from app.deps import require_role
from app.models.entrega import Entrega, EstadoEntrega
from app.models.pedido import EstadoPedido, Pedido
from app.models.user import User, UserRole
from app.schemas.entrega import EntregaRead, UbicacionUpdate

router = APIRouter(prefix="", tags=["Entregas"])


@router.get("/entregas/disponibles", response_model=list[dict])
async def list_available_deliveries(
    current_user: User = Depends(require_role(UserRole.REPARTIDOR)),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Pedido).where(
            Pedido.estado == EstadoPedido.LISTO, Pedido.repartidor_id == None
        )
    )
    return result.scalars().all()


@router.post("/entregas/{pedido_id}/aceptar", response_model=EntregaRead)
async def accept_delivery(
    pedido_id: UUID,
    current_user: User = Depends(require_role(UserRole.REPARTIDOR)),
    db: AsyncSession = Depends(get_db),
):
    # Check if repartidor has active delivery
    active = await db.execute(
        select(Entrega).where(
            Entrega.repartidor_id == current_user.id,
            Entrega.estado.in_([EstadoEntrega.ASIGNADA, EstadoEntrega.EN_CAMINO]),
        )
    )
    if active.scalar_one_or_none():
        raise DomainException(
            "ACTIVE_DELIVERY_EXISTS",
            "Ya tienes una entrega activa en curso",
            status.HTTP_409_CONFLICT,
        )

    # SELECT FOR UPDATE SKIP LOCKED
    result = await db.execute(
        select(Pedido)
        .where(
            Pedido.id == pedido_id,
            Pedido.estado == EstadoPedido.LISTO,
            Pedido.repartidor_id == None,
        )
        .with_for_update(skip_locked=True)
    )
    pedido = result.scalar_one_or_none()
    if not pedido:
        raise DomainException(
            "ORDER_NOT_AVAILABLE",
            "El pedido ya fue tomado por otro repartidor o no está listo",
            status.HTTP_409_CONFLICT,
        )

    pedido.repartidor_id = current_user.id
    pedido.estado = EstadoPedido.EN_CAMINO

    entrega = Entrega(
        pedido_id=pedido.id,
        repartidor_id=current_user.id,
        estado=EstadoEntrega.EN_CAMINO,
        costo_domicilio=Decimal("5000.00"),
    )
    db.add(entrega)
    await db.commit()
    await db.refresh(entrega)
    return entrega


@router.patch("/entregas/{id}/estado", response_model=EntregaRead)
async def update_entrega_estado(
    id: UUID,
    estado: EstadoEntrega,
    current_user: User = Depends(require_role(UserRole.REPARTIDOR)),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Entrega).where(
            Entrega.id == id, Entrega.repartidor_id == current_user.id
        )
    )
    entrega = result.scalar_one_or_none()
    if not entrega:
        raise DomainException(
            "NOT_FOUND", "Entrega no encontrada", status.HTTP_404_NOT_FOUND
        )

    entrega.estado = estado
    if estado == EstadoEntrega.ENTREGADA:
        # Also update order
        order_res = await db.execute(
            select(Pedido).where(Pedido.id == entrega.pedido_id)
        )
        pedido = order_res.scalar_one_or_none()
        if pedido:
            pedido.estado = EstadoPedido.ENTREGADO

    await db.commit()
    await db.refresh(entrega)
    return entrega


@router.patch("/entregas/{id}/ubicacion", response_model=EntregaRead)
async def update_ubicacion(
    id: UUID,
    payload: UbicacionUpdate,
    current_user: User = Depends(require_role(UserRole.REPARTIDOR)),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Entrega).where(
            Entrega.id == id, Entrega.repartidor_id == current_user.id
        )
    )
    entrega = result.scalar_one_or_none()
    if not entrega:
        raise DomainException(
            "NOT_FOUND", "Entrega no encontrada", status.HTTP_404_NOT_FOUND
        )

    entrega.lat_actual = payload.lat
    entrega.lng_actual = payload.lng
    await db.commit()
    await db.refresh(entrega)
    return entrega
