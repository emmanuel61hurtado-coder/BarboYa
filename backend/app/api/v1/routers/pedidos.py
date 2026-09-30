from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from sqlalchemy.orm import selectinload
from uuid import UUID
from decimal import Decimal
from app.db.session import get_db
from app.models.pedido import Pedido, EstadoPedido
from app.models.detalle_pedido import DetallePedido
from app.models.producto import Producto
from app.models.comercio import Comercio
from app.models.cupon import Cupon
from app.models.user import User, UserRole
from app.models.notificacion import Notificacion
from app.schemas.pedido import PedidoRead, PedidoCreate, PedidoEstadoUpdate
from app.deps import get_current_user, require_role
from app.core.exceptions import DomainException
from app.services.state_machine import validate_transition
from app.api.v1.ws.manager import manager

router = APIRouter(prefix="/pedidos", tags=["Pedidos"])


@router.post("", response_model=PedidoRead, status_code=status.HTTP_201_CREATED)
async def create_pedido(payload: PedidoCreate, current_user: User = Depends(require_role(UserRole.CLIENTE)), db: AsyncSession = Depends(get_db)):
    # 1. Validate comercio
    comercio_res = await db.execute(select(Comercio).where(Comercio.id == payload.comercio_id))
    comercio = comercio_res.scalar_one_or_none()
    if not comercio or not comercio.abierto:
        raise DomainException("COMERCIO_CLOSED", "El comercio no está disponible o abierto", status.HTTP_400_BAD_REQUEST)
        
    subtotal = Decimal("0.00")
    detalles_to_create = []

    for item in payload.detalles:
        prod_res = await db.execute(select(Producto).where(Producto.id == item.producto_id))
        producto = prod_res.scalar_one_or_none()
        if not producto or not producto.disponible or producto.comercio_id != comercio.id:
            raise DomainException("INVALID_PRODUCT", f"Producto {item.producto_id} no disponible o no pertenece al comercio", status.HTTP_400_BAD_REQUEST)
            
        item_subtotal = producto.precio * item.cantidad
        subtotal += item_subtotal
        detalles_to_create.append({
            "producto_id": producto.id,
            "nombre_producto": producto.nombre,
            "precio_unitario": producto.precio,
            "cantidad": item.cantidad,
            "subtotal": item_subtotal
        })

    costo_envio = Decimal("5000.00")
    descuento = Decimal("0.00")

    if payload.cupon_codigo:
        cupon_res = await db.execute(select(Cupon).where(Cupon.codigo == payload.cupon_codigo, Cupon.activo == True))
        cupon = cupon_res.scalar_one_or_none()
        if not cupon or subtotal < cupon.monto_minimo or cupon.usos_actuales >= cupon.usos_maximos:
            raise DomainException("INVALID_COUPON", "Cupón inválido o no cumple las condiciones", status.HTTP_400_BAD_REQUEST)
            
        if cupon.descuento_porcentaje:
            descuento = subtotal * (cupon.descuento_porcentaje / Decimal("100.00"))
        elif cupon.descuento_monto:
            descuento = cupon.descuento_monto
        cupon.usos_actuales += 1

    total = subtotal + costo_envio - descuento
    if total < 0:
        total = Decimal("0.00")

    pedido = Pedido(
        cliente_id=current_user.id,
        comercio_id=comercio.id,
        direccion_id=payload.direccion_id,
        estado=EstadoPedido.CREADO,
        subtotal=subtotal,
        costo_envio=costo_envio,
        descuento=descuento,
        total=total,
        metodo_pago=payload.metodo_pago,
        cupon_codigo=payload.cupon_codigo
    )
    db.add(pedido)
    await db.flush()

    for det in detalles_to_create:
        detalle = DetallePedido(pedido_id=pedido.id, **det)
        db.add(detalle)

    # Notification
    notif = Notificacion(
        user_id=current_user.id,
        titulo="Pedido Creado",
        mensaje=f"Tu pedido #{str(pedido.id)[:8]} ha sido creado exitosamente."
    )
    db.add(notif)

    await db.commit()
    
    result = await db.execute(select(Pedido).options(selectinload(Pedido.detalles)).where(Pedido.id == pedido.id))
    return result.scalar_one()


@router.get("", response_model=list[PedidoRead])
async def list_pedidos(current_user: User = Depends(get_current_user), db: AsyncSession = Depends(get_db)):
    query = select(Pedido).options(selectinload(Pedido.detalles))
    if current_user.rol == UserRole.CLIENTE:
        query = query.where(Pedido.cliente_id == current_user.id)
    elif current_user.rol == UserRole.COMERCIO:
        comercio_res = await db.execute(select(Comercio).where(Comercio.user_id == current_user.id))
        comercio = comercio_res.scalar_one_or_none()
        if comercio:
            query = query.where(Pedido.comercio_id == comercio.id)
        else:
            return []
    elif current_user.rol == UserRole.REPARTIDOR:
        query = query.where(Pedido.repartidor_id == current_user.id)
        
    result = await db.execute(query)
    return result.scalars().all()


@router.get("/{id}", response_model=PedidoRead)
async def get_pedido(id: UUID, current_user: User = Depends(get_current_user), db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Pedido).options(selectinload(Pedido.detalles)).where(Pedido.id == id))
    pedido = result.scalar_one_or_none()
    if not pedido:
        raise DomainException("NOT_FOUND", "Pedido no encontrado", status.HTTP_404_NOT_FOUND)
    return pedido


@router.patch("/{id}/estado", response_model=PedidoRead)
async def update_pedido_estado(id: UUID, payload: PedidoEstadoUpdate, current_user: User = Depends(get_current_user), db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Pedido).options(selectinload(Pedido.detalles)).where(Pedido.id == id))
    pedido = result.scalar_one_or_none()
    if not pedido:
        raise DomainException("NOT_FOUND", "Pedido no encontrado", status.HTTP_404_NOT_FOUND)
        
    validate_transition(pedido.estado, payload.estado)
    
    pedido.estado = payload.estado
    
    # Broadcast via websocket
    await manager.broadcast(str(pedido.id), {"estado": payload.estado})
    
    notif = Notificacion(
        user_id=pedido.cliente_id,
        titulo="Actualización de Pedido",
        mensaje=f"Tu pedido #{str(pedido.id)[:8]} ahora está {payload.estado}"
    )
    db.add(notif)
    
    await db.commit()
    await db.refresh(pedido)
    return pedido
