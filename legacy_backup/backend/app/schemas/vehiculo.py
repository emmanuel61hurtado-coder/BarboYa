from uuid import UUID
from datetime import datetime
from pydantic import BaseModel
from app.models.vehiculo import TipoVehiculo


class VehiculoBase(BaseModel):
    tipo: TipoVehiculo
    placa: str | None = None
    modelo: str | None = None


class VehiculoCreate(VehiculoBase):
    pass


class VehiculoUpdate(BaseModel):
    tipo: TipoVehiculo | None = None
    placa: str | None = None
    modelo: str | None = None


class VehiculoRead(VehiculoBase):
    id: UUID
    repartidor_id: UUID
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True
