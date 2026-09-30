from uuid import UUID
from datetime import datetime
from decimal import Decimal
from pydantic import BaseModel, Field


class ProductoBase(BaseModel):
    nombre: str
    descripcion: str | None = None
    precio: Decimal = Field(..., ge=0)
    imagen_url: str | None = None
    disponible: bool = True
    categoria_id: UUID | None = None


class ProductoCreate(ProductoBase):
    pass


class ProductoUpdate(BaseModel):
    nombre: str | None = None
    descripcion: str | None = None
    precio: Decimal | None = Field(None, ge=0)
    imagen_url: str | None = None
    disponible: bool | None = None
    categoria_id: UUID | None = None


class ProductoRead(ProductoBase):
    id: UUID
    comercio_id: UUID
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True
