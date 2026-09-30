from uuid import UUID
from datetime import datetime
from decimal import Decimal
from pydantic import BaseModel
from app.models.entrega import EstadoEntrega


class EntregaRead(BaseModel):
    id: UUID
    pedido_id: UUID
    repartidor_id: UUID | None = None
    estado: EstadoEntrega
    costo_domicilio: Decimal
    lat_actual: Decimal | None = None
    lng_actual: Decimal | None = None
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class UbicacionUpdate(BaseModel):
    lat: Decimal
    lng: Decimal
