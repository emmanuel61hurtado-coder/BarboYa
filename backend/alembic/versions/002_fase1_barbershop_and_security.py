"""fase1_barbershop_and_security

Revision ID: 002_fase1
Revises: 001_initial_schema
Create Date: 2026-10-15 12:00:00.000000

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision: str = '002_fase1'
down_revision: Union[str, None] = '001_initial_schema'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column('users', sa.Column('google_id', sa.String(), nullable=True))
    op.add_column('users', sa.Column('avatar_url', sa.String(), nullable=True))
    op.add_column('users', sa.Column('is_verified', sa.Boolean(), nullable=False, server_default=sa.text('false')))
    op.alter_column('users', 'password_hash', existing_type=sa.String(), nullable=True)
    op.create_index('ix_users_google_id', 'users', ['google_id'], unique=True)

    op.create_table(
        'refresh_tokens',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
        sa.Column('token_hash', sa.String(), nullable=False),
        sa.Column('expires_at', sa.DateTime(), nullable=False),
        sa.Column('revoked', sa.Boolean(), nullable=False, server_default=sa.text('false')),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
    )
    op.create_index('ix_refresh_tokens_id', 'refresh_tokens', ['id'])
    op.create_index('ix_refresh_tokens_token_hash', 'refresh_tokens', ['token_hash'], unique=True)

    op.create_table(
        'auditoria_logs',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='SET NULL'), nullable=True),
        sa.Column('accion', sa.String(), nullable=False),
        sa.Column('detalles', sa.String(), nullable=True),
        sa.Column('ip', sa.String(), nullable=True),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
    )
    op.create_index('ix_auditoria_logs_id', 'auditoria_logs', ['id'])
    op.create_index('ix_auditoria_logs_accion', 'auditoria_logs', ['accion'])

    op.create_table(
        'barberias_tenant',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('nombre', sa.String(), nullable=False),
        sa.Column('nit', sa.String(), nullable=False),
        sa.Column('dueno_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
        sa.Column('telefono', sa.String(), nullable=False),
        sa.Column('direccion', sa.String(), nullable=False),
        sa.Column('ciudad', sa.String(), nullable=False),
        sa.Column('logo_url', sa.String(), nullable=True),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
    )
    op.create_index('ix_barberias_tenant_id', 'barberias_tenant', ['id'])
    op.create_index('ix_barberias_tenant_nombre', 'barberias_tenant', ['nombre'])
    op.create_index('ix_barberias_tenant_nit', 'barberias_tenant', ['nit'], unique=True)
    op.create_index('ix_barberias_tenant_ciudad', 'barberias_tenant', ['ciudad'])

    op.create_table(
        'sedes',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('barberia_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('barberias_tenant.id', ondelete='CASCADE'), nullable=False),
        sa.Column('nombre', sa.String(), nullable=False),
        sa.Column('direccion', sa.String(), nullable=False),
        sa.Column('ciudad', sa.String(), nullable=False),
        sa.Column('lat', sa.Numeric(10, 8), nullable=False),
        sa.Column('lng', sa.Numeric(11, 8), nullable=False),
        sa.Column('telefono', sa.String(), nullable=False),
        sa.Column('abierta', sa.Boolean(), nullable=False, server_default=sa.text('true')),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
    )
    op.create_index('ix_sedes_id', 'sedes', ['id'])
    op.create_index('ix_sedes_ciudad', 'sedes', ['ciudad'])

    op.create_table(
        'staff_barberos',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('sede_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('sedes.id', ondelete='CASCADE'), nullable=False),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
        sa.Column('especialidad', sa.String(), nullable=True),
        sa.Column('calificacion_promedio', sa.Numeric(3, 2), nullable=False, server_default=sa.text('5.0')),
        sa.Column('activo', sa.Boolean(), nullable=False, server_default=sa.text('true')),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
    )
    op.create_index('ix_staff_barberos_id', 'staff_barberos', ['id'])

    op.create_table(
        'servicios_barberia',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('sede_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('sedes.id', ondelete='CASCADE'), nullable=False),
        sa.Column('nombre', sa.String(), nullable=False),
        sa.Column('descripcion', sa.String(), nullable=True),
        sa.Column('precio', sa.Numeric(10, 2), nullable=False),
        sa.Column('duracion_minutos', sa.Integer(), nullable=False, server_default=sa.text('30')),
        sa.Column('activo', sa.Boolean(), nullable=False, server_default=sa.text('true')),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
    )
    op.create_index('ix_servicios_barberia_id', 'servicios_barberia', ['id'])

    op.create_table(
        'horarios_staff',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('staff_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('staff_barberos.id', ondelete='CASCADE'), nullable=False),
        sa.Column('dia_semana', sa.Integer(), nullable=False),
        sa.Column('hora_inicio', sa.Time(), nullable=False),
        sa.Column('hora_fin', sa.Time(), nullable=False),
        sa.Column('activo', sa.Boolean(), nullable=False, server_default=sa.text('true')),
    )
    op.create_index('ix_horarios_staff_id', 'horarios_staff', ['id'])

    op.create_table(
        'citas',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('cliente_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
        sa.Column('staff_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('staff_barberos.id', ondelete='CASCADE'), nullable=False),
        sa.Column('servicio_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('servicios_barberia.id', ondelete='CASCADE'), nullable=False),
        sa.Column('fecha_hora', sa.DateTime(), nullable=False),
        sa.Column('estado', sa.String(), nullable=False, server_default='PENDIENTE'),
        sa.Column('notas', sa.String(), nullable=True),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(), nullable=False, server_default=sa.text('now()')),
    )
    op.create_index('ix_citas_id', 'citas', ['id'])
    op.create_index('ix_citas_fecha_hora', 'citas', ['fecha_hora'])


def downgrade() -> None:
    op.drop_table('citas')
    op.drop_table('horarios_staff')
    op.drop_table('servicios_barberia')
    op.drop_table('staff_barberos')
    op.drop_table('sedes')
    op.drop_table('barberias_tenant')
    op.drop_table('auditoria_logs')
    op.drop_table('refresh_tokens')
    op.drop_index('ix_users_google_id', 'users')
    op.drop_column('users', 'is_verified')
    op.drop_column('users', 'avatar_url')
    op.drop_column('users', 'google_id')
