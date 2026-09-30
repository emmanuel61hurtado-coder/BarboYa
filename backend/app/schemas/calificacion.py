from uuid import UUID
from datetime import datetime
from pydantic import BaseModel, Field


class CalificacionCreate(BaseModel):
    pedido_id: UUID
    comercio_id: UUID | None = None
    repartidor_id: UUID | None = None
    puntuacion: int = Field(..., ge=1, le=5)
    comentario: str | None = None


class CalificacionRead(BaseModel):
    id: UUID
    pedido_id: UUID
    cliente_id: UUID
    comercio_id: UUID | None = None
    repartidor_id: UUID | None = None
    puntuacion: int
    comentario: str | None = None
    created_at: datetime

    class Config:
        from_attributes = True
