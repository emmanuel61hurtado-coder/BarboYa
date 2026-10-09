from uuid import UUID
from datetime import datetime
from decimal import Decimal
from pydantic import BaseModel, Field


class CuponBase(BaseModel):
    codigo: str
    descuento_porcentaje: Decimal | None = Field(None, ge=0, le=100)
    descuento_monto: Decimal | None = Field(None, ge=0)
    monto_minimo: Decimal = Field(0, ge=0)
    usos_maximos: int = Field(100, gt=0)
    activo: bool = True
    expiracion: datetime


class CuponCreate(CuponBase):
    pass


class CuponUpdate(BaseModel):
    codigo: str | None = None
    descuento_porcentaje: Decimal | None = None
    descuento_monto: Decimal | None = None
    monto_minimo: Decimal | None = None
    usos_maximos: int | None = None
    activo: bool | None = None
    expiracion: datetime | None = None


class CuponRead(CuponBase):
    id: UUID
    usos_actuales: int
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class CuponValidarRequest(BaseModel):
    codigo: str
    subtotal: Decimal
