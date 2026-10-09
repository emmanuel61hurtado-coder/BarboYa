"""Pruebas de persistencia, restricciones y relaciones de los modelos SQLAlchemy."""
import uuid
from datetime import datetime, timedelta
from decimal import Decimal

import pytest
from sqlalchemy.exc import IntegrityError
from sqlalchemy.future import select

from app.models.calificacion import Calificacion
from app.models.categoria import Categoria
from app.models.comercio import Comercio
from app.models.cupon import Cupon
from app.models.detalle_pedido import DetallePedido
from app.models.direccion import Direccion
from app.models.pedido import EstadoPedido, Pedido
from app.models.producto import Producto
from app.models.user import User, UserRole, UserStatus


def _user(**kw):
    data = dict(
        email=f"{uuid.uuid4().hex[:12]}@barboya.com",
        password_hash="hash",
        nombre="N",
        telefono="3000000000",
    )
    data.update(kw)
    return User(**data)


async def _comercio(db, **kw):
    owner = _user(rol=UserRole.COMERCIO)
    db.add(owner)
    await db.flush()
    data = dict(user_id=owner.id, nombre="Comercio", direccion="Calle 1", lat=Decimal("6.2"), lng=Decimal("-75.5"))
    data.update(kw)
    c = Comercio(**data)
    db.add(c)
    await db.flush()
    return c


async def _pedido(db):
    cliente = _user()
    db.add(cliente)
    await db.flush()
    comercio = await _comercio(db)
    dire = Direccion(user_id=cliente.id, nombre="Casa", direccion="Calle 2", lat=Decimal("6.2"), lng=Decimal("-75.5"))
    db.add(dire)
    await db.flush()
    pedido = Pedido(
        cliente_id=cliente.id, comercio_id=comercio.id, direccion_id=dire.id,
        subtotal=Decimal("10"), costo_envio=Decimal("2"), total=Decimal("12"), metodo_pago="EFECTIVO",
    )
    db.add(pedido)
    await db.flush()
    return pedido, comercio


async def test_user_defaults_y_persistencia(db_session):
    u = _user()
    db_session.add(u)
    await db_session.commit()
    row = (await db_session.execute(select(User).where(User.id == u.id))).scalar_one()
    assert row.rol == UserRole.CLIENTE
    assert row.estado == UserStatus.ACTIVO
    assert isinstance(row.id, uuid.UUID)
    assert row.created_at is not None and row.updated_at is not None


async def test_user_email_unico(db_session):
    email = f"{uuid.uuid4().hex[:8]}@barboya.com"
    db_session.add(_user(email=email))
    await db_session.commit()
    db_session.add(_user(email=email))
    with pytest.raises(IntegrityError):
        await db_session.commit()
    await db_session.rollback()


async def test_user_campos_obligatorios(db_session):
    db_session.add(User(email=f"{uuid.uuid4().hex[:8]}@barboya.com", nombre="X", telefono="1"))  # sin password_hash
    with pytest.raises(IntegrityError):
        await db_session.commit()
    await db_session.rollback()


async def test_categoria_nombre_unico(db_session):
    nombre = f"Cat-{uuid.uuid4().hex[:6]}"
    db_session.add(Categoria(nombre=nombre))
    await db_session.commit()
    db_session.add(Categoria(nombre=nombre))
    with pytest.raises(IntegrityError):
        await db_session.commit()
    await db_session.rollback()


async def test_comercio_defaults_y_relacion_con_usuario(db_session):
    c = await _comercio(db_session)
    await db_session.commit()
    assert c.abierto is True
    assert c.calificacion_promedio == 0
    owner = (await db_session.execute(select(User).where(User.id == c.user_id))).scalar_one()
    assert owner.rol == UserRole.COMERCIO


async def test_comercio_un_solo_comercio_por_usuario(db_session):
    c = await _comercio(db_session)
    await db_session.commit()
    db_session.add(Comercio(user_id=c.user_id, nombre="Otro", direccion="x", lat=Decimal("1"), lng=Decimal("1")))
    with pytest.raises(IntegrityError):
        await db_session.commit()
    await db_session.rollback()


async def test_producto_precio_negativo_rechazado(db_session):
    c = await _comercio(db_session)
    db_session.add(Producto(comercio_id=c.id, nombre="Malo", precio=Decimal("-1")))
    with pytest.raises(IntegrityError):
        await db_session.commit()
    await db_session.rollback()


async def test_producto_valido_y_disponible_por_defecto(db_session):
    c = await _comercio(db_session)
    p = Producto(comercio_id=c.id, nombre="Hamburguesa", precio=Decimal("15000.50"))
    db_session.add(p)
    await db_session.commit()
    assert p.disponible is True
    assert p.comercio_id == c.id


async def test_pedido_estado_inicial_y_totales(db_session):
    pedido, _ = await _pedido(db_session)
    await db_session.commit()
    assert pedido.estado == EstadoPedido.CREADO
    assert pedido.descuento == 0
    assert pedido.total == Decimal("12")


async def test_detalle_pedido_cantidad_positiva(db_session):
    pedido, comercio = await _pedido(db_session)
    prod = Producto(comercio_id=comercio.id, nombre="P", precio=Decimal("5"))
    db_session.add(prod)
    await db_session.flush()
    db_session.add(DetallePedido(
        pedido_id=pedido.id, producto_id=prod.id, nombre_producto="P",
        precio_unitario=Decimal("5"), cantidad=0, subtotal=Decimal("0"),
    ))
    with pytest.raises(IntegrityError):
        await db_session.commit()
    await db_session.rollback()


@pytest.mark.parametrize("puntuacion", [0, 6, -1])
async def test_calificacion_fuera_de_rango_rechazada(db_session, puntuacion):
    pedido, comercio = await _pedido(db_session)
    db_session.add(Calificacion(
        pedido_id=pedido.id, cliente_id=pedido.cliente_id, comercio_id=comercio.id, puntuacion=puntuacion,
    ))
    with pytest.raises(IntegrityError):
        await db_session.commit()
    await db_session.rollback()


async def test_calificacion_en_rango_aceptada(db_session):
    pedido, comercio = await _pedido(db_session)
    db_session.add(Calificacion(
        pedido_id=pedido.id, cliente_id=pedido.cliente_id, comercio_id=comercio.id, puntuacion=5,
    ))
    await db_session.commit()


async def test_cupon_usos_actuales_no_supera_maximo(db_session):
    db_session.add(Cupon(
        codigo=f"C-{uuid.uuid4().hex[:6]}", descuento_monto=Decimal("1"),
        usos_maximos=1, usos_actuales=2, expiracion=datetime.utcnow() + timedelta(days=1),
    ))
    with pytest.raises(IntegrityError):
        await db_session.commit()
    await db_session.rollback()


async def test_cupon_codigo_unico(db_session):
    codigo = f"U-{uuid.uuid4().hex[:6]}"
    exp = datetime.utcnow() + timedelta(days=1)
    db_session.add(Cupon(codigo=codigo, descuento_monto=Decimal("1"), expiracion=exp))
    await db_session.commit()
    db_session.add(Cupon(codigo=codigo, descuento_monto=Decimal("1"), expiracion=exp))
    with pytest.raises(IntegrityError):
        await db_session.commit()
    await db_session.rollback()


def test_enums_expuestos():
    assert {r.value for r in UserRole} == {"CLIENTE", "REPARTIDOR", "COMERCIO", "ADMIN"}
    assert {s.value for s in UserStatus} == {"PENDIENTE", "ACTIVO", "BLOQUEADO"}
    assert [e.value for e in EstadoPedido] == [
        "CREADO", "ACEPTADO", "PREPARANDO", "LISTO", "EN_CAMINO", "ENTREGADO", "CANCELADO",
    ]
