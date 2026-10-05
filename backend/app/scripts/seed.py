import asyncio
import logging
from sqlalchemy.future import select
from app.db.session import async_session_maker
from app.models.user import User, UserRole, UserStatus
from app.models.comercio import Comercio
from app.models.categoria import Categoria
from app.models.producto import Producto
from app.models.cupon import Cupon
from app.core.security import get_password_hash
from app.core.config import settings
from datetime import datetime, timedelta
from decimal import Decimal

logger = logging.getLogger("barboya.seed")


async def seed():
    async with async_session_maker() as session:
        async with session.begin():
            # Seed Admin User if ADMIN_PASSWORD is provided and secure
            if settings.ADMIN_PASSWORD and len(settings.ADMIN_PASSWORD) >= 8:
                result = await session.execute(select(User).where(User.email == settings.ADMIN_EMAIL))
                admin = result.scalar_one_or_none()
                if not admin:
                    admin = User(
                        email=settings.ADMIN_EMAIL,
                        password_hash=get_password_hash(settings.ADMIN_PASSWORD),
                        nombre="Administrador BarboYa",
                        telefono="3001234567",
                        rol=UserRole.ADMIN,
                        estado=UserStatus.ACTIVO
                    )
                    session.add(admin)
                    logger.info(f"Admin user seeded ({settings.ADMIN_EMAIL}).")
            else:
                logger.warning("ADMIN_PASSWORD not configured or too short. Skipping admin seeding.")

            # Base system categories (essential catalog data)
            cats = ["Hamburguesas", "Pizza", "Sushi", "Mexicana", "Bebidas", "Postres", "Café"]
            cat_objs = {}
            for c_name in cats:
                res = await session.execute(select(Categoria).where(Categoria.nombre == c_name))
                cat = res.scalar_one_or_none()
                if not cat:
                    cat = Categoria(nombre=c_name, imagen_url="https://images.unsplash.com/photo-1546069901-ba9599a7e63c")
                    session.add(cat)
                    await session.flush()
                cat_objs[c_name] = cat

            # ONLY seed mock demo commerce and coupons in development / staging
            if settings.ENVIRONMENT.lower() != "production":
                res = await session.execute(select(User).where(User.email == "burger@barboya.com"))
                c1_user = res.scalar_one_or_none()
                if not c1_user:
                    c1_user = User(
                        email="burger@barboya.com",
                        password_hash=get_password_hash("DevPassword123*"),
                        nombre="Burger Master Dev",
                        telefono="3101112233",
                        rol=UserRole.COMERCIO,
                        estado=UserStatus.ACTIVO
                    )
                    session.add(c1_user)
                    await session.flush()

                    comercio1 = Comercio(
                        user_id=c1_user.id,
                        nombre="Burger Master Gourmet",
                        descripcion="Las mejores hamburguesas artesanales de la ciudad",
                        logo_url="https://images.unsplash.com/photo-1568901346375-23c9450c58cd",
                        banner_url="https://images.unsplash.com/photo-1550547660-d9450f859349",
                        direccion="Calle 100 # 15-20",
                        lat=Decimal("4.67123400"),
                        lng=Decimal("-74.05312300"),
                        abierto=True,
                        calificacion_promedio=Decimal("4.8")
                    )
                    session.add(comercio1)
                    await session.flush()

                    p1 = Producto(
                        comercio_id=comercio1.id,
                        categoria_id=cat_objs["Hamburguesas"].id,
                        nombre="Hamburguesa Doble Carne",
                        descripcion="Carne angus, queso cheddar fundido y tocino",
                        precio=Decimal("28000.00"),
                        imagen_url="https://images.unsplash.com/photo-1568901346375-23c9450c58cd",
                        disponible=True
                    )
                    session.add(p1)

                res = await session.execute(select(Cupon).where(Cupon.codigo == "BARBOYA20"))
                cupon = res.scalar_one_or_none()
                if not cupon:
                    cupon = Cupon(
                        codigo="BARBOYA20",
                        descuento_porcentaje=Decimal("20.00"),
                        monto_minimo=Decimal("20000.00"),
                        usos_maximos=500,
                        usos_actuales=0,
                        activo=True,
                        expiracion=datetime.utcnow() + timedelta(days=30)
                    )
                    session.add(cupon)
                    logger.info("Dev Coupon BARBOYA20 seeded.")

        # session.begin() auto-commits on successful exit of the context manager
        logger.info("Database initialization / seed completed successfully.")


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    asyncio.run(seed())

