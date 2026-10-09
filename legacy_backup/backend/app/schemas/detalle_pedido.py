from uuid import UUID
from decimal import Decimal
from pydantic import BaseModel, Field


class DetallePedidoCreate(BaseModel):
    producto_id: UUID
    cantidad: int = Field(..., gt=0)


class DetallePedidoRead(BaseModel):
    id: UUID
    producto_id: UUID
    nombre_producto: str
    precio_unitario: Decimal
    cantidad: int
    subtotal: Decimal

    class Config:
        from_attributes = True
