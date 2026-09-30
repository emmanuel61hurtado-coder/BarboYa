from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from app.db.session import get_db
from app.models.user import User
from app.schemas.user import UserRead, UserUpdate
from app.deps import get_current_user

router = APIRouter(prefix="/users", tags=["Users"])


@router.get("/me", response_model=UserRead)
async def get_me(current_user: User = Depends(get_current_user)):
    return current_user


@router.patch("/me", response_model=UserRead)
async def update_me(payload: UserUpdate, current_user: User = Depends(get_current_user), db: AsyncSession = Depends(get_db)):
    if payload.nombre:
        current_user.nombre = payload.nombre
    if payload.telefono:
        current_user.telefono = payload.telefono
    await db.commit()
    await db.refresh(current_user)
    return current_user
