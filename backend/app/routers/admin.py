from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_session
from app.core.security import CurrentUser, require_role
from app.models.user import User
from app.models.trip import Trip
from app.models.merchant import Order
from pydantic import BaseModel, ConfigDict

router = APIRouter(prefix="/admin", tags=["admin-management"], dependencies=[Depends(require_role("admin", "superadmin"))])


class UserAdminOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: UUID
    full_name: str
    phone: str
    email: str | None
    role: str
    is_active: bool


class UserStatusIn(BaseModel):
    is_active: bool


@router.get("/users", response_model=list[UserAdminOut])
async def admin_list_users(session: AsyncSession = Depends(get_session)):
    try:
        rows = (await session.execute(select(User).order_by(User.created_at.desc()).limit(100))).scalars().all()
        return [UserAdminOut.model_validate(u, from_attributes=True) for u in rows]
    except Exception:
        return []


@router.post("/users/{user_id}/status", response_model=UserAdminOut)
async def admin_set_user_status(user_id: UUID, data: UserStatusIn, session: AsyncSession = Depends(get_session)):
    user = await session.get(User, user_id)
    if user is None:
        raise HTTPException(404, "Usuario no encontrado")
    user.is_active = data.is_active
    await session.commit()
    return UserAdminOut.model_validate(user, from_attributes=True)


@router.get("/dashboard/stats")
async def admin_dashboard_stats(session: AsyncSession = Depends(get_session)):
    try:
        users_count = (await session.execute(select(func.count(User.id)))).scalar() or 0
        trips_count = (await session.execute(select(func.count(Trip.id)))).scalar() or 0
        orders_count = (await session.execute(select(func.count(Order.id)))).scalar() or 0
        return {
            "total_users": users_count,
            "total_trips": trips_count,
            "total_orders": orders_count,
        }
    except Exception:
        return {"total_users": 0, "total_trips": 0, "total_orders": 0}
