from uuid import UUID
from datetime import datetime
from pydantic import BaseModel


class CategoriaBase(BaseModel):
    nombre: str
    imagen_url: str | None = None


class CategoriaCreate(CategoriaBase):
    pass


class CategoriaUpdate(BaseModel):
    nombre: str | None = None
    imagen_url: str | None = None


class CategoriaRead(CategoriaBase):
    id: UUID
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True
