from uuid import UUID

from fastapi import Depends, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from jose import jwt, JWTError
from app.db.session import get_db
from app.core.config import settings
from app.models.user import User, UserRole, UserStatus
from app.core.exceptions import DomainException

security = HTTPBearer()


async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: AsyncSession = Depends(get_db),
) -> User:
    token = credentials.credentials
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=["HS256"])
        if payload.get("type") != "access":
            raise DomainException("INVALID_TOKEN", "Tipo de token inválido", status.HTTP_401_UNAUTHORIZED)
        user_id = UUID(payload.get("sub"))
    except (JWTError, ValueError, TypeError, AttributeError):
        raise DomainException("INVALID_TOKEN", "Token inválido o expirado", status.HTTP_401_UNAUTHORIZED)

    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()
    if user is None:
        raise DomainException("USER_NOT_FOUND", "Usuario no encontrado", status.HTTP_404_NOT_FOUND)
    
    if user.estado == UserStatus.BLOQUEADO:
        raise DomainException("USER_BLOCKED", "Usuario bloqueado", status.HTTP_403_FORBIDDEN)

    return user


def require_role(*roles: UserRole):
    async def dependency(current_user: User = Depends(get_current_user)) -> User:
        if current_user.rol not in roles:
            raise DomainException("FORBIDDEN", "No tienes permisos para realizar esta acción", status.HTTP_403_FORBIDDEN)
        
        if current_user.rol in [UserRole.REPARTIDOR, UserRole.COMERCIO] and current_user.estado == UserStatus.PENDIENTE:
            raise DomainException("PENDING_APPROVAL", "Tu cuenta está pendiente de aprobación por el administrador", status.HTTP_403_FORBIDDEN)
            
        return current_user
    return dependency
