from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from app.db.session import get_db
from app.models.categoria import Categoria
from app.schemas.categoria import CategoriaRead

router = APIRouter(prefix="/categorias", tags=["Categorias"])


@router.get("", response_model=list[CategoriaRead])
async def list_categorias(db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Categoria))
    return result.scalars().all()
