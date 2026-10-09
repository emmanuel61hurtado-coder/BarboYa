from uuid import UUID
from datetime import datetime
from decimal import Decimal
from pydantic import BaseModel
from app.models.pago import MetodoPago, EstadoPago


class PagoCreate(BaseModel):
    pedido_id: UUID
    metodo: MetodoPago
    monto: Decimal


class PagoRead(BaseModel):
    id: UUID
    pedido_id: UUID
    metodo: MetodoPago
    estado: EstadoPago
    monto: Decimal
    transaccion_id: str | None = None
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True
