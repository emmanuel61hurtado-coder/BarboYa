import pytest
import os
from httpx import AsyncClient, ASGITransport
from sqlalchemy.pool import StaticPool
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from app.main import app
from app.db.base import Base
from app.db.session import get_db

TEST_DATABASE_URL = os.getenv("TEST_DATABASE_URL", "sqlite+aiosqlite:///:memory:")

_engine_kwargs = {"future": True, "echo": False}
if TEST_DATABASE_URL.startswith("sqlite"):
    # En memoria cada conexion veria una BD distinta: compartimos una sola conexion.
    _engine_kwargs.update(poolclass=StaticPool, connect_args={"check_same_thread": False})

engine = create_async_engine(TEST_DATABASE_URL, **_engine_kwargs)

TestingSessionLocal = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)


async def override_get_db():
    async with TestingSessionLocal() as session:
        yield session


app.dependency_overrides[get_db] = override_get_db


@pytest.fixture(autouse=True)
async def setup_db():
    # create_all es idempotente: asegura las tablas en cada test sin depender del
    # orden/loop de un fixture de sesion. La BD se comparte entre tests (emails unicos).
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield


@pytest.fixture
async def client():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        yield ac


@pytest.fixture
async def db_session():
    async with TestingSessionLocal() as session:
        yield session


@pytest.fixture
def make_user(db_session):
    """Crea un usuario directamente en BD y devuelve (user, headers con JWT valido)."""
    import uuid
    from app.models.user import User, UserRole, UserStatus
    from app.core.security import get_password_hash, create_access_token

    async def _make(rol=UserRole.CLIENTE, estado=UserStatus.ACTIVO, password="Password123*"):
        user = User(
            email=f"{uuid.uuid4().hex[:12]}@barboya.com",
            password_hash=get_password_hash(password),
            nombre="Test",
            telefono="3000000000",
            rol=rol,
            estado=estado,
        )
        db_session.add(user)
        await db_session.commit()
        await db_session.refresh(user)
        return user, {"Authorization": f"Bearer {create_access_token(user.id)}"}

    return _make
