from uuid import UUID
from datetime import datetime
from decimal import Decimal
from pydantic import BaseModel
from app.models.pedido import EstadoPedido
from app.schemas.detalle_pedido import DetallePedidoRead


class PedidoCreate(BaseModel):
    comercio_id: UUID
    direccion_id: UUID
    metodo_pago: str
    cupon_codigo: str | None = None
    detalles: list[DetallePedidoCreateSchema] = [] # wait, let's reference DetallePedidoCreate


class DetallePedidoCreateSchema(BaseModel):
    producto_id: UUID
    cantidad: int


class PedidoEstadoUpdate(BaseModel):
    estado: EstadoPedido


class PedidoRead(BaseModel):
    id: UUID
    cliente_id: UUID
    comercio_id: UUID
    repartidor_id: UUID | None = None
    direccion_id: UUID
    estado: EstadoPedido
    subtotal: Decimal
    costo_envio: Decimal
    descuento: Decimal
    total: Decimal
    metodo_pago: str
    cupon_codigo: str | None = None
    created_at: datetime
    updated_at: datetime
    detalles: list[DetallePedidoRead] = []

    class Config:
        from_attributes = True
