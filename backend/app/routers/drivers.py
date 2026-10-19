from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import func

from app.core.database import get_session
from app.core.security import CurrentUser, require_role
from app.models.user import DriverProfile
from pydantic import BaseModel, Field

router = APIRouter(prefix="/drivers", tags=["drivers"])


class DriverLocationIn(BaseModel):
    lat: float = Field(ge=-90, le=90)
    lng: float = Field(ge=-180, le=180)
    is_online: bool = True


@router.post("/location", status_code=200)
async def update_driver_location(
    data: DriverLocationIn,
    user: CurrentUser = Depends(require_role("driver")),
    session: AsyncSession = Depends(get_session),
):
    driver = await session.get(DriverProfile, user.id)
    if driver is None:
        raise HTTPException(404, "Perfil de conductor no encontrado")

    driver.current_location = func.ST_SetSRID(func.ST_MakePoint(data.lng, data.lat), 4326)
    driver.is_online = data.is_online
    driver.location_updated_at = func.now()
    await session.commit()
    return {"updated": True, "is_online": driver.is_online}
