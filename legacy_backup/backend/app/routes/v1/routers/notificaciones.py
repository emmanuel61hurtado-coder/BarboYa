from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from app.db.session import get_db
from app.models.notificacion import Notificacion
from app.models.user import User
from app.schemas.notificacion import NotificacionRead
from app.deps import get_current_user

router = APIRouter(prefix="/notificaciones", tags=["Notificaciones"])


@router.get("", response_model=list[NotificacionRead])
async def list_notificaciones(current_user: User = Depends(get_current_user), db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Notificacion).where(Notificacion.user_id == current_user.id))
    return result.scalars().all()
