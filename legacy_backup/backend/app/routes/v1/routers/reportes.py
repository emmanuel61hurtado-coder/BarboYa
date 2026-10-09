from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from app.db.session import get_db
from app.models.user import User, UserRole
from app.deps import require_role

router = APIRouter(prefix="/reportes", tags=["Reportes"])


@router.get("/ventas")
async def ventas_reporte(current_user: User = Depends(require_role(UserRole.ADMIN, UserRole.COMERCIO)), db: AsyncSession = Depends(get_db)):
    return {
        "total_ventas": 1500000.00,
        "pedidos_completados": 45,
        "periodo": "ultimo_mes"
    }
