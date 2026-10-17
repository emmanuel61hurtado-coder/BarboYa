
from fastapi import APIRouter, status
from pydantic import BaseModel

router = APIRouter(prefix="/notifications", tags=["notifications"])

class NotificationOut(BaseModel):
    id: str
    title: str
    message: str
    read: bool
    created_at: str

@router.get("/", response_model=list[NotificationOut])
async def list_notifications():
    return [
        {
            "id": "notif-001",
            "title": "¡Viaje Confirmado!",
            "message": "Tu conductor en moto va en camino.",
            "read": False,
            "created_at": "2026-10-17T12:30:00Z"
        }
    ]

@router.post("/{notification_id}/read", status_code=status.HTTP_200_OK)
async def mark_as_read(notification_id: str):
    return {"status": "success", "notification_id": notification_id, "read": True}
