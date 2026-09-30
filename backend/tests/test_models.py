import uuid
from decimal import Decimal
from datetime import datetime
from app.models.user import User, UserRole, UserStatus
from app.models.comercio import Comercio
from app.models.producto import Producto
from app.models.pedido import Pedido, EstadoPedido
from app.models.cupon import Cupon


def test_user_model_creation():
    user = User(
        email="modeltest@barboya.com",
        password_hash="hash123",
        nombre="Test User",
        telefono="3000000000",
        rol=UserRole.CLIENTE,
        estado=UserStatus.ACTIVO
    )
    assert user.email == "modeltest@barboya.com"
    assert user.rol == UserRole.CLIENTE
    assert user.estado == UserStatus.ACTIVO


def test_comercio_model_creation():
    comercio = Comercio(
        nombre="Restaurante Gourmet",
        descripcion="Comida deliciosa",
        direccion="Calle 10 # 20-30",
        lat=Decimal("6.2442"),
        lng=Decimal("-75.5812"),
        calificacion_promedio=Decimal("4.8"),
        abierto=True
    )
    assert comercio.nombre == "Restaurante Gourmet"
    assert comercio.calificacion_promedio == Decimal("4.8")
    assert comercio.abierto is True


def test_cupon_model_creation():
    cupon = Cupon(
        codigo="PROMO2026",
        descuento_monto=Decimal("5000.00"),
        monto_minimo=Decimal("20000.00"),
        usos_maximos=50,
        usos_actuales=0,
        activo=True,
        expiracion=datetime.now()
    )
    assert cupon.codigo == "PROMO2026"
    assert cupon.descuento_monto == Decimal("5000.00")
    assert cupon.usos_maximos == 50
