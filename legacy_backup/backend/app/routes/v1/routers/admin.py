from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from uuid import UUID
from app.db.session import get_db
from app.models.user import User, UserStatus, UserRole
from app.schemas.user import UserRead
from app.deps import require_role
from app.core.exceptions import DomainException

router = APIRouter(prefix="/admin", tags=["Admin"])


@router.get("/usuarios", response_model=list[UserRead])
async def list_all_users(current_user: User = Depends(require_role(UserRole.ADMIN)), db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User))
    return result.scalars().all()


@router.patch("/usuarios/{id}/aprobar", response_model=UserRead)
async def aprobar_usuario(id: UUID, current_user: User = Depends(require_role(UserRole.ADMIN)), db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).where(User.id == id))
    user = result.scalar_one_or_none()
    if not user:
        raise DomainException("NOT_FOUND", "Usuario no encontrado", status.HTTP_404_NOT_FOUND)
        
    user.estado = UserStatus.ACTIVO
    await db.commit()
    await db.refresh(user)
    return user


@router.patch("/usuarios/{id}/bloquear", response_model=UserRead)
async def bloquear_usuario(id: UUID, current_user: User = Depends(require_role(UserRole.ADMIN)), db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).where(User.id == id))
    user = result.scalar_one_or_none()
    if not user:
        raise DomainException("NOT_FOUND", "Usuario no encontrado", status.HTTP_404_NOT_FOUND)
        
    user.estado = UserStatus.BLOQUEADO
    await db.commit()
    await db.refresh(user)
    return user
