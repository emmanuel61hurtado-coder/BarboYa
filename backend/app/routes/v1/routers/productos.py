from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from app.db.session import get_db
from app.models.producto import Producto
from app.models.comercio import Comercio
from app.models.user import User, UserRole
from app.schemas.producto import ProductoRead, ProductoCreate
from app.deps import require_role
from app.core.exceptions import DomainException

router = APIRouter(prefix="", tags=["Productos"])


@router.get("/comercios/me/productos", response_model=list[ProductoRead])
async def list_my_products(current_user: User = Depends(require_role(UserRole.COMERCIO)), db: AsyncSession = Depends(get_db)):
    comercio_res = await db.execute(select(Comercio).where(Comercio.user_id == current_user.id))
    comercio = comercio_res.scalar_one_or_none()
    if not comercio:
        raise DomainException("NOT_FOUND", "Comercio no encontrado", status.HTTP_404_NOT_FOUND)
        
    result = await db.execute(select(Producto).where(Producto.comercio_id == comercio.id))
    return result.scalars().all()


@router.post("/comercios/me/productos", response_model=ProductoRead, status_code=status.HTTP_201_CREATED)
async def create_product(payload: ProductoCreate, current_user: User = Depends(require_role(UserRole.COMERCIO)), db: AsyncSession = Depends(get_db)):
    comercio_res = await db.execute(select(Comercio).where(Comercio.user_id == current_user.id))
    comercio = comercio_res.scalar_one_or_none()
    if not comercio:
        raise DomainException("NOT_FOUND", "Comercio no encontrado", status.HTTP_404_NOT_FOUND)
        
    producto = Producto(
        comercio_id=comercio.id,
        **payload.model_dump()
    )
    db.add(producto)
    await db.commit()
    await db.refresh(producto)
    return producto
