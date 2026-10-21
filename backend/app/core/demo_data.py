from sqlalchemy import select

from app.core.config import get_settings
from app.core.database import SessionLocal, is_using_sqlite
from app.core.security import hash_password
from app.models.enums import UserRole
from app.models.user import User


async def seed_demo_customer() -> None:
    settings = get_settings()
    if settings.environment != "development" or not is_using_sqlite():
        return

    demo_phone = "+573001112233"
    async with SessionLocal() as session:
        existing_user_id = await session.scalar(
            select(User.id).where(User.phone == demo_phone)
        )
        if existing_user_id is not None:
            return

        session.add(
            User(
                full_name="Pasajero Demo",
                phone=demo_phone,
                password_hash=hash_password("Cliente123!"),
                role=UserRole.customer,
            )
        )
        await session.commit()
