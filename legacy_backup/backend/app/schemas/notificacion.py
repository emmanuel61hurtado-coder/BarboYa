from uuid import UUID
from datetime import datetime
from pydantic import BaseModel


class NotificacionRead(BaseModel):
    id: UUID
    user_id: UUID
    titulo: str
    mensaje: str
    leida: bool
    created_at: datetime

    class Config:
        from_attributes = True
