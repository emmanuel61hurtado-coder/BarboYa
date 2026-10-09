"""initial_schema

Revision ID: 001_initial_schema
Revises: 
Create Date: 2026-10-02 16:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = '001_initial_schema'
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. users
    op.create_table(
        'users',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('email', sa.String(), nullable=False),
        sa.Column('password_hash', sa.String(), nullable=False),
        sa.Column('nombre', sa.String(), nullable=False),
        sa.Column('telefono', sa.String(), nullable=False),
        sa.Column('rol', sa.Enum('CLIENTE', 'REPARTIDOR', 'COMERCIO', 'ADMIN', name='userrole'), nullable=False, server_default='CLIENTE'),
        sa.Column('estado', sa.Enum('PENDIENTE', 'ACTIVO', 'BLOQUEADO', name='userstatus'), nullable=False, server_default='ACTIVO'),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
    )
    op.create_index('ix_users_id', 'users', ['id'])
    op.create_index('ix_users_email', 'users', ['email'], unique=True)

    # 2. comercios
    op.create_table(
        'comercios',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False, unique=True),
        sa.Column('nombre', sa.String(), nullable=False),
        sa.Column('descripcion', sa.String(), nullable=True),
        sa.Column('logo_url', sa.String(), nullable=True),
        sa.Column('banner_url', sa.String(), nullable=True),
        sa.Column('direccion', sa.String(), nullable=False),
        sa.Column('lat', sa.Numeric(10, 8), nullable=False),
        sa.Column('lng', sa.Numeric(11, 8), nullable=False),
        sa.Column('abierto', sa.Boolean(), nullable=False, server_default=sa.text('true')),
        sa.Column('calificacion_promedio', sa.Numeric(3, 2), nullable=False, server_default=sa.text('0.0')),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
    )
    op.create_index('ix_comercios_id', 'comercios', ['id'])
    op.create_index('ix_comercios_nombre', 'comercios', ['nombre'])

    # 3. categorias
    op.create_table(
        'categorias',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('nombre', sa.String(), nullable=False),
        sa.Column('imagen_url', sa.String(), nullable=True),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
    )
    op.create_index('ix_categorias_id', 'categorias', ['id'])
    op.create_index('ix_categorias_nombre', 'categorias', ['nombre'], unique=True)

    # 4. productos
    op.create_table(
        'productos',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('comercio_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('comercios.id', ondelete='CASCADE'), nullable=False),
        sa.Column('categoria_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('categorias.id', ondelete='SET NULL'), nullable=True),
        sa.Column('nombre', sa.String(), nullable=False),
        sa.Column('descripcion', sa.String(), nullable=True),
        sa.Column('precio', sa.Numeric(12, 2), nullable=False),
        sa.Column('imagen_url', sa.String(), nullable=True),
        sa.Column('disponible', sa.Boolean(), nullable=False, server_default=sa.text('true')),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.CheckConstraint('precio >= 0', name='check_producto_precio_positivo')
    )
    op.create_index('ix_productos_id', 'productos', ['id'])
    op.create_index('ix_productos_comercio_id', 'productos', ['comercio_id'])
    op.create_index('ix_productos_nombre', 'productos', ['nombre'])

    # 5. direcciones
    op.create_table(
        'direcciones',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
        sa.Column('nombre', sa.String(), nullable=False),
        sa.Column('direccion', sa.String(), nullable=False),
        sa.Column('detalles', sa.String(), nullable=True),
        sa.Column('lat', sa.Numeric(10, 8), nullable=False),
        sa.Column('lng', sa.Numeric(11, 8), nullable=False),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
    )
    op.create_index('ix_direcciones_id', 'direcciones', ['id'])
    op.create_index('ix_direcciones_user_id', 'direcciones', ['user_id'])

    # 6. pedidos
    op.create_table(
        'pedidos',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('cliente_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id'), nullable=False),
        sa.Column('comercio_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('comercios.id'), nullable=False),
        sa.Column('repartidor_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id'), nullable=True),
        sa.Column('direccion_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('direcciones.id'), nullable=False),
        sa.Column('estado', sa.Enum('CREADO', 'ACEPTADO', 'PREPARANDO', 'LISTO', 'EN_CAMINO', 'ENTREGADO', 'CANCELADO', name='estadopedido'), nullable=False, server_default='CREADO'),
        sa.Column('subtotal', sa.Numeric(12, 2), nullable=False),
        sa.Column('costo_envio', sa.Numeric(12, 2), nullable=False),
        sa.Column('descuento', sa.Numeric(12, 2), nullable=False, server_default=sa.text('0.0')),
        sa.Column('total', sa.Numeric(12, 2), nullable=False),
        sa.Column('metodo_pago', sa.String(), nullable=False),
        sa.Column('cupon_codigo', sa.String(), nullable=True),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
    )
    op.create_index('ix_pedidos_id', 'pedidos', ['id'])
    op.create_index('ix_pedidos_cliente_id', 'pedidos', ['cliente_id'])
    op.create_index('ix_pedidos_comercio_id', 'pedidos', ['comercio_id'])
    op.create_index('ix_pedidos_repartidor_id', 'pedidos', ['repartidor_id'])
    op.create_index('ix_pedidos_estado', 'pedidos', ['estado'])

    # 7. detalle_pedidos
    op.create_table(
        'detalle_pedidos',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('pedido_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('pedidos.id', ondelete='CASCADE'), nullable=False),
        sa.Column('producto_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('productos.id'), nullable=False),
        sa.Column('nombre_producto', sa.String(), nullable=False),
        sa.Column('precio_unitario', sa.Numeric(12, 2), nullable=False),
        sa.Column('cantidad', sa.Integer(), nullable=False),
        sa.Column('subtotal', sa.Numeric(12, 2), nullable=False),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.CheckConstraint('cantidad > 0', name='check_detalle_cantidad_positiva'),
        sa.CheckConstraint('precio_unitario >= 0', name='check_detalle_precio_positivo'),
    )
    op.create_index('ix_detalle_pedidos_id', 'detalle_pedidos', ['id'])
    op.create_index('ix_detalle_pedidos_pedido_id', 'detalle_pedidos', ['pedido_id'])

    # 8. pagos
    op.create_table(
        'pagos',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('pedido_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('pedidos.id', ondelete='CASCADE'), nullable=False, unique=True),
        sa.Column('metodo', sa.Enum('EFECTIVO', 'TARJETA', 'NEQUI', name='metodopago'), nullable=False),
        sa.Column('estado', sa.Enum('PENDIENTE', 'COMPLETADO', 'FALLIDO', name='estadopago'), nullable=False, server_default='PENDIENTE'),
        sa.Column('monto', sa.Numeric(12, 2), nullable=False),
        sa.Column('transaccion_id', sa.String(), nullable=True),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
    )
    op.create_index('ix_pagos_id', 'pagos', ['id'])
    op.create_index('ix_pagos_pedido_id', 'pagos', ['pedido_id'])

    # 9. entregas
    op.create_table(
        'entregas',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('pedido_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('pedidos.id', ondelete='CASCADE'), nullable=False, unique=True),
        sa.Column('repartidor_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id'), nullable=True),
        sa.Column('estado', sa.Enum('PENDIENTE', 'ASIGNADA', 'EN_CAMINO', 'ENTREGADA', name='estadoentrega'), nullable=False, server_default='PENDIENTE'),
        sa.Column('costo_domicilio', sa.Numeric(12, 2), nullable=False),
        sa.Column('lat_actual', sa.Numeric(10, 8), nullable=True),
        sa.Column('lng_actual', sa.Numeric(11, 8), nullable=True),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
    )
    op.create_index('ix_entregas_id', 'entregas', ['id'])
    op.create_index('ix_entregas_pedido_id', 'entregas', ['pedido_id'])
    op.create_index('ix_entregas_repartidor_id', 'entregas', ['repartidor_id'])

    # 10. calificaciones
    op.create_table(
        'calificaciones',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('pedido_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('pedidos.id', ondelete='CASCADE'), nullable=False),
        sa.Column('cliente_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id'), nullable=False),
        sa.Column('comercio_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('comercios.id'), nullable=True),
        sa.Column('repartidor_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id'), nullable=True),
        sa.Column('puntuacion', sa.Integer(), nullable=False),
        sa.Column('comentario', sa.String(), nullable=True),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.CheckConstraint('puntuacion >= 1 AND puntuacion <= 5', name='check_puntuacion_rango')
    )
    op.create_index('ix_calificaciones_id', 'calificaciones', ['id'])
    op.create_index('ix_calificaciones_pedido_id', 'calificaciones', ['pedido_id'])

    # 11. vehiculos
    op.create_table(
        'vehiculos',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('repartidor_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False, unique=True),
        sa.Column('tipo', sa.Enum('MOTO', 'BICI', 'CARRO', name='tipovehiculo'), nullable=False, server_default='MOTO'),
        sa.Column('placa', sa.String(), nullable=True),
        sa.Column('modelo', sa.String(), nullable=True),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
    )
    op.create_index('ix_vehiculos_id', 'vehiculos', ['id'])
    op.create_index('ix_vehiculos_repartidor_id', 'vehiculos', ['repartidor_id'])

    # 12. cupones
    op.create_table(
        'cupones',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('codigo', sa.String(), nullable=False),
        sa.Column('descuento_porcentaje', sa.Numeric(5, 2), nullable=True),
        sa.Column('descuento_monto', sa.Numeric(12, 2), nullable=True),
        sa.Column('monto_minimo', sa.Numeric(12, 2), nullable=False, server_default=sa.text('0.0')),
        sa.Column('usos_maximos', sa.Integer(), nullable=False, server_default=sa.text('100')),
        sa.Column('usos_actuales', sa.Integer(), nullable=False, server_default=sa.text('0')),
        sa.Column('activo', sa.Boolean(), nullable=False, server_default=sa.text('true')),
        sa.Column('expiracion', sa.DateTime(), nullable=False),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.CheckConstraint('usos_actuales <= usos_maximos', name='check_cupon_usos')
    )
    op.create_index('ix_cupones_id', 'cupones', ['id'])
    op.create_index('ix_cupones_codigo', 'cupones', ['codigo'], unique=True)

    # 13. notificaciones
    op.create_table(
        'notificaciones',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
        sa.Column('titulo', sa.String(), nullable=False),
        sa.Column('mensaje', sa.String(), nullable=False),
        sa.Column('leida', sa.Boolean(), nullable=False, server_default=sa.text('false')),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
    )
    op.create_index('ix_notificaciones_id', 'notificaciones', ['id'])
    op.create_index('ix_notificaciones_user_id', 'notificaciones', ['user_id'])


def downgrade() -> None:
    op.drop_table('notificaciones')
    op.drop_table('cupones')
    op.drop_table('vehiculos')
    op.drop_table('calificaciones')
    op.drop_table('entregas')
    op.drop_table('pagos')
    op.drop_table('detalle_pedidos')
    op.drop_table('pedidos')
    op.drop_table('direcciones')
    op.drop_table('productos')
    op.drop_table('categorias')
    op.drop_table('comercios')
    op.drop_table('users')

    op.execute('DROP TYPE IF EXISTS tipovehiculo')
    op.execute('DROP TYPE IF EXISTS estadoentrega')
    op.execute('DROP TYPE IF EXISTS estadopago')
    op.execute('DROP TYPE IF EXISTS metodopago')
    op.execute('DROP TYPE IF EXISTS estadopedido')
    op.execute('DROP TYPE IF EXISTS userstatus')
    op.execute('DROP TYPE IF EXISTS userrole')
