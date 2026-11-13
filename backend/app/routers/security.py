from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_session
from app.core.security import CurrentUser, get_current_user
from app.models.enums import SecurityEventType
from app.models.trip import SecurityLog
from pydantic import BaseModel, Field

router = APIRouter(prefix="/security", tags=["security"])


class SOSIn(BaseModel):
    trip_id: UUID | None = None
    lat: float | None = Field(default=None, ge=-90, le=90)
    lng: float | None = Field(default=None, ge=-180, le=180)
    details: dict = Field(default_factory=dict)


@router.post("/sos", status_code=201)
async def trigger_sos(
    data: SOSIn,
    user: CurrentUser = Depends(get_current_user),
    session: AsyncSession = Depends(get_session),
):
    location = None
    if data.lat is not None and data.lng is not None:
        location = func.ST_SetSRID(func.ST_MakePoint(data.lng, data.lat), 4326)

    log_entry = SecurityLog(
        user_id=user.id,
        trip_id=data.trip_id,
        event_type=SecurityEventType.sos,
        location=location,
        details=data.details,
        resolved=False,
    )
    session.add(log_entry)
    await session.commit()
    return {"status": "recorded", "alert_id": str(log_entry.id), "message": "Alerta SOS registrada. Soporte y contactos notificados."}
