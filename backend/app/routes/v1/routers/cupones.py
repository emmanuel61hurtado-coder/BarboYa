from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from app.db.session import get_db
from app.models.cupon import Cupon
from app.schemas.cupon import CuponRead, CuponCreate, CuponValidarRequest
from app.deps import require_role, get_current_user
from app.models.user import User, UserRole
from app.core.exceptions import DomainException

router = APIRouter(prefix="/cupones", tags=["Cupones"])


@router.post("/validar", response_model=CuponRead)
async def validar_cupon(payload: CuponValidarRequest, current_user: User = Depends(get_current_user), db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Cupon).where(Cupon.codigo == payload.codigo, Cupon.activo == True))
    cupon = result.scalar_one_or_none()
    if not cupon or payload.subtotal < cupon.monto_minimo or cupon.usos_actuales >= cupon.usos_maximos:
        raise DomainException("INVALID_COUPON", "Cupón inválido o expirado", status.HTTP_400_BAD_REQUEST)
    return cupon


@router.post("", response_model=CuponRead, status_code=status.HTTP_201_CREATED)
async def create_cupon(payload: CuponCreate, current_user: User = Depends(require_role(UserRole.ADMIN)), db: AsyncSession = Depends(get_db)):
    cupon = Cupon(**payload.model_dump())
    db.add(cupon)
    await db.commit()
    await db.refresh(cupon)
    return cupon
