from uuid import UUID

from jose import JWTError, jwt
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.core.config import settings
from app.models.comercio import Comercio
from app.models.pedido import Pedido
from app.models.user import User, UserRole, UserStatus


async def authenticate_ws(token: str, db: AsyncSession) -> User | None:
    """Devuelve el usuario dueño de un access token valido, o None si no es valido."""
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=["HS256"])
        if payload.get("type") != "access":
            return None
        user_id = UUID(payload.get("sub"))
    except (JWTError, ValueError, TypeError, AttributeError):
        return None
    user = (await db.execute(select(User).where(User.id == user_id))).scalar_one_or_none()
    if user is None or user.estado == UserStatus.BLOQUEADO:
        return None
    return user


async def can_access_pedido(user: User, pedido_id: str, db: AsyncSession) -> tuple[bool, Pedido | None]:
    """El pedido es visible para su cliente, su repartidor, el dueño del comercio y los admin."""
    try:
        pid = UUID(pedido_id)
    except ValueError:
        return False, None
    pedido = (await db.execute(select(Pedido).where(Pedido.id == pid))).scalar_one_or_none()
    if pedido is None:
        return False, None
    if user.rol == UserRole.ADMIN or user.id in (pedido.cliente_id, pedido.repartidor_id):
        return True, pedido
    comercio = (await db.execute(select(Comercio).where(Comercio.id == pedido.comercio_id))).scalar_one_or_none()
    return (comercio is not None and comercio.user_id == user.id), pedido
