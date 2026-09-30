from uuid import UUID
from datetime import datetime
from decimal import Decimal
from pydantic import BaseModel, Field


class DireccionBase(BaseModel):
    nombre: str
    direccion: str
    detalles: str | None = None
    lat: Decimal = Field(..., ge=-90, le=90)
    lng: Decimal = Field(..., ge=-180, le=180)


class DireccionCreate(DireccionBase):
    pass


class DireccionUpdate(BaseModel):
    nombre: str | None = None
    direccion: str | None = None
    detalles: str | None = None
    lat: Decimal | None = None
    lng: Decimal | None = None


class DireccionRead(DireccionBase):
    id: UUID
    user_id: UUID
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True
