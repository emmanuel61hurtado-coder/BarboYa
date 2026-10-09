from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from app.db.session import get_db
from app.models.vehiculo import Vehiculo
from app.models.user import User, UserRole
from app.schemas.vehiculo import VehiculoRead, VehiculoCreate
from app.deps import require_role
from app.core.exceptions import DomainException

router = APIRouter(prefix="/vehiculos", tags=["Vehiculos"])


@router.get("/me", response_model=VehiculoRead)
async def get_my_vehiculo(current_user: User = Depends(require_role(UserRole.REPARTIDOR)), db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Vehiculo).where(Vehiculo.repartidor_id == current_user.id))
    vehiculo = result.scalar_one_or_none()
    if not vehiculo:
        raise DomainException("NOT_FOUND", "Vehículo no registrado", status.HTTP_404_NOT_FOUND)
    return vehiculo


@router.post("", response_model=VehiculoRead, status_code=status.HTTP_201_CREATED)
async def create_vehiculo(payload: VehiculoCreate, current_user: User = Depends(require_role(UserRole.REPARTIDOR)), db: AsyncSession = Depends(get_db)):
    existing = await db.execute(select(Vehiculo).where(Vehiculo.repartidor_id == current_user.id))
    if existing.scalar_one_or_none():
        raise DomainException("ALREADY_EXISTS", "Vehículo ya registrado", status.HTTP_409_CONFLICT)
        
    vehiculo = Vehiculo(
        repartidor_id=current_user.id,
        **payload.model_dump()
    )
    db.add(vehiculo)
    await db.commit()
    await db.refresh(vehiculo)
    return vehiculo
