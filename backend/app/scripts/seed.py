import asyncio
import logging
from sqlalchemy.future import select
from app.db.session import async_session_maker
from app.models.user import User, UserRole, UserStatus
from app.models.barberia_tenant import BarberiaTenant, Sede, StaffBarbero, ServicioBarberia
from app.core.security import get_password_hash
from app.core.config import settings
from decimal import Decimal

logger = logging.getLogger("barboya.seed")


async def seed():
    async with async_session_maker() as session:
        async with session.begin():
            if settings.ADMIN_PASSWORD and len(settings.ADMIN_PASSWORD) >= 8:
                result = await session.execute(select(User).where(User.email == settings.ADMIN_EMAIL))
                admin = result.scalar_one_or_none()
                if not admin:
                    admin = User(
                        email=settings.ADMIN_EMAIL,
                        password_hash=get_password_hash(settings.ADMIN_PASSWORD),
                        nombre="SuperAdministrador BarboYa",
                        telefono="3001234567",
                        rol=UserRole.SUPERADMIN,
                        estado=UserStatus.ACTIVO,
                        is_verified=True
                    )
                    session.add(admin)
                    logger.info(f"SuperAdmin user seeded ({settings.ADMIN_EMAIL}).")
            else:
                logger.warning("ADMIN_PASSWORD not configured or too short. Skipping admin seeding.")

            res = await session.execute(select(User).where(User.email == "barbero.dueno@barboya.com"))
            dueno = res.scalar_one_or_none()
            if not dueno:
                dueno = User(
                    email="barbero.dueno@barboya.com",
                    password_hash=get_password_hash("DevPassword123*"),
                    nombre="Carlos El Barbero",
                    telefono="3102223344",
                    rol=UserRole.DUENO_BARBERIA,
                    estado=UserStatus.ACTIVO,
                    is_verified=True
                )
                session.add(dueno)
                await session.flush()

                tenant = BarberiaTenant(
                    nombre="Barbería Gentleman Bogotá",
                    nit="900123456-1",
                    dueno_id=dueno.id,
                    telefono="3102223344",
                    direccion="Carrera 15 # 85-30",
                    ciudad="Bogotá",
                    logo_url="https://images.unsplash.com/photo-1503951914875-452162b0f3f1"
                )
                session.add(tenant)
                await session.flush()

                sede = Sede(
                    barberia_id=tenant.id,
                    nombre="Sede Zona T",
                    direccion="Carrera 15 # 85-30",
                    ciudad="Bogotá",
                    lat=Decimal("4.66820000"),
                    lng=Decimal("-74.05350000"),
                    telefono="3102223344",
                    abierta=True
                )
                session.add(sede)
                await session.flush()

                staff_user = User(
                    email="barbero.staff@barboya.com",
                    password_hash=get_password_hash("DevPassword123*"),
                    nombre="Mateo El Pro",
                    telefono="3203334455",
                    rol=UserRole.BARBERO,
                    estado=UserStatus.ACTIVO,
                    is_verified=True
                )
                session.add(staff_user)
                await session.flush()

                staff = StaffBarbero(
                    sede_id=sede.id,
                    user_id=staff_user.id,
                    especialidad="Corte Clásico y Barba",
                    calificacion_promedio=Decimal("4.9")
                )
                session.add(staff)
                await session.flush()

                servicio1 = ServicioBarberia(
                    sede_id=sede.id,
                    nombre="Corte Gentleman + Barba Master",
                    descripcion="Corte estilo personalizado con toalla caliente y perfilado de barba",
                    precio=Decimal("45000.00"),
                    duracion_minutos=45,
                    activo=True
                )
                session.add(servicio1)
                logger.info("Sample Barbershop, Sede, Staff and Services seeded successfully.")

        logger.info("Database initialization / seed completed successfully.")


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    asyncio.run(seed())
