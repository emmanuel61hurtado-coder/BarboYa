from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from app.db.session import get_db
from app.models.direccion import Direccion
from app.models.user import User, UserRole
from app.schemas.direccion import DireccionRead, DireccionCreate
from app.deps import require_role

router = APIRouter(prefix="/direcciones", tags=["Direcciones"])


@router.get("", response_model=list[DireccionRead])
async def list_direcciones(current_user: User = Depends(require_role(UserRole.CLIENTE)), db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Direccion).where(Direccion.user_id == current_user.id))
    return result.scalars().all()


@router.post("", response_model=DireccionRead, status_code=status.HTTP_201_CREATED)
async def create_direccion(payload: DireccionCreate, current_user: User = Depends(require_role(UserRole.CLIENTE)), db: AsyncSession = Depends(get_db)):
    direccion = Direccion(
        user_id=current_user.id,
        **payload.model_dump()
    )
    db.add(direccion)
    await db.commit()
    await db.refresh(direccion)
    return direccion
