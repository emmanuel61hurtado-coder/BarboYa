from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from sqlalchemy.orm import selectinload
from uuid import UUID
from app.db.session import get_db
from app.models.comercio import Comercio
from app.models.user import User, UserRole
from app.schemas.comercio import ComercioRead, ComercioCreate, ComercioUpdate
from app.deps import get_current_user, require_role
from app.core.exceptions import DomainException

router = APIRouter(prefix="/comercios", tags=["Comercios"])


@router.get("", response_model=list[ComercioRead])
async def list_comercios(db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Comercio).options(selectinload(Comercio.productos)).where(Comercio.abierto == True))
    return result.scalars().all()


@router.get("/{id}", response_model=ComercioRead)
async def get_comercio(id: UUID, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Comercio).options(selectinload(Comercio.productos)).where(Comercio.id == id))
    comercio = result.scalar_one_or_none()
    if not comercio:
        raise DomainException("NOT_FOUND", "Comercio no encontrado", status.HTTP_404_NOT_FOUND)
    return comercio


@router.post("", response_model=ComercioRead, status_code=status.HTTP_201_CREATED)
async def create_comercio(payload: ComercioCreate, current_user: User = Depends(require_role(UserRole.COMERCIO)), db: AsyncSession = Depends(get_db)):
    existing = await db.execute(select(Comercio).where(Comercio.user_id == current_user.id))
    if existing.scalar_one_or_none():
        raise DomainException("ALREADY_EXISTS", "El usuario ya tiene un comercio registrado", status.HTTP_409_CONFLICT)
        
    comercio = Comercio(
        user_id=current_user.id,
        **payload.model_dump()
    )
    db.add(comercio)
    await db.commit()
    await db.refresh(comercio)
    return comercio
