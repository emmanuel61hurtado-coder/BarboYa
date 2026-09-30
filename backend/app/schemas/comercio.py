from uuid import UUID
from datetime import datetime
from decimal import Decimal
from pydantic import BaseModel, Field
from app.schemas.producto import ProductoRead


class ComercioBase(BaseModel):
    nombre: str
    descripcion: str | None = None
    logo_url: str | None = None
    banner_url: str | None = None
    direccion: str
    lat: Decimal = Field(..., ge=-90, le=90)
    lng: Decimal = Field(..., ge=-180, le=180)
    abierto: bool = True


class ComercioCreate(ComercioBase):
    pass


class ComercioUpdate(BaseModel):
    nombre: str | None = None
    descripcion: str | None = None
    logo_url: str | None = None
    banner_url: str | None = None
    direccion: str | None = None
    lat: Decimal | None = None
    lng: Decimal | None = None
    abierto: bool | None = None


class ComercioRead(ComercioBase):
    id: UUID
    user_id: UUID
    calificacion_promedio: Decimal
    created_at: datetime
    updated_at: datetime
    productos: list[ProductoRead] = []

    class Config:
        from_attributes = True
