"""Sincronización automática del esquema PostgreSQL con los modelos SQLAlchemy.

Se ejecuta al arrancar la API (ver `app.core.database.init_db`) y es idempotente:

1. Activa las extensiones necesarias (postgis, pgcrypto).
2. Crea los tipos ENUM que falten y agrega los valores que falten.
3. Crea las tablas que falten (`Base.metadata.create_all`).
4. Agrega las columnas que falten en tablas existentes.
5. Siembra las zonas base de Barbosa cuando la tabla `zones` está vacía.

De esta forma el despliegue en Docker/Coolify no depende de que
`database/schema.sql` se haya aplicado sin errores: la base siempre termina
coincidiendo con los modelos.
"""

from __future__ import annotations

import logging
from enum import Enum

from sqlalchemy import Enum as SAEnum
from sqlalchemy import Text, text
from sqlalchemy.dialects import postgresql
from sqlalchemy.ext.asyncio import AsyncConnection, AsyncEngine

from app.core.database import Base

log = logging.getLogger("barboya.schema")

_DIALECT = postgresql.dialect()

# Zonas de ejemplo para Barbosa (Santander). Mismo origen que database/schema.sql.
_ZONES = (
    (
        "Barbosa Casco Urbano",
        True,
        "POLYGON((-73.630 5.925, -73.610 5.925, -73.610 5.942, -73.630 5.942, -73.630 5.925))",
        3000,
        800,
        20,
        300,
    ),
    (
        "Barbosa Rural",
        False,
        "POLYGON((-73.680 5.880, -73.560 5.880, -73.560 6.000, -73.680 6.000, -73.680 5.880))",
        5000,
        1200,
        25,
        500,
    ),
)


def _quote(value: str) -> str:
    return "'" + str(value).replace("'", "''") + "'"


def _column_ddl(col) -> str:
    """DDL del tipo de una columna según el dialecto PostgreSQL."""
    col_type = col.type
    if isinstance(col_type, SAEnum):
        return col_type.name
    if isinstance(col_type, Text):
        return "TEXT"
    impl = col_type.dialect_impl(_DIALECT)
    return str(impl.compile(_DIALECT))


def _default_literal(col) -> str | None:
    """Valor por defecto escalar del modelo (booleano, número o texto)."""
    default = col.default
    if default is None or not default.is_scalar:
        return None
    arg = default.arg
    if isinstance(arg, Enum):
        arg = arg.value
    if isinstance(arg, bool):
        return "TRUE" if arg else "FALSE"
    if isinstance(arg, (int, float)):
        return str(arg)
    if isinstance(arg, str):
        return _quote(arg)
    return None


def _model_enums() -> dict[str, list[str]]:
    """Tipos ENUM usados por los modelos: {nombre_pg: [valores]}."""
    enums: dict[str, list[str]] = {}
    for table in Base.metadata.tables.values():
        for col in table.columns:
            col_type = col.type
            if not isinstance(col_type, SAEnum):
                continue
            enum_cls = col_type.enum_class
            if enum_cls is None:
                continue
            enums.setdefault(col_type.name, [item.value for item in enum_cls])
    return enums


async def _ensure_extensions(conn: AsyncConnection) -> None:
    """PostGIS y pgcrypto son necesarios para las columnas geoespaciales."""
    for extension in ("postgis", "pgcrypto"):
        try:
            await conn.execute(text(f"CREATE EXTENSION IF NOT EXISTS {extension}"))
        except Exception as err:  # sin permisos o imagen sin la extension
            log.warning("No se pudo activar la extension %s: %s", extension, err)


async def _existing_enum_values(conn: AsyncConnection) -> dict[str, set[str]]:
    rows = (
        await conn.execute(
            text(
                "SELECT t.typname AS name, e.enumlabel AS value "
                "FROM pg_type t JOIN pg_enum e ON e.enumtypid = t.oid "
                "WHERE t.typtype = 'e'"
            )
        )
    ).all()
    existing: dict[str, set[str]] = {}
    for name, value in rows:
        existing.setdefault(name, set()).add(value)
    return existing


async def _create_missing_enum_types(conn: AsyncConnection, existing: dict[str, set[str]]) -> None:
    for name, values in _model_enums().items():
        if name in existing or not values:
            continue
        literal = ", ".join(_quote(v) for v in values)
        await conn.execute(text(f'CREATE TYPE "{name}" AS ENUM ({literal})'))
        log.info("Tipo ENUM creado: %s%s", name, values)


async def _add_missing_enum_values(engine: AsyncEngine, existing: dict[str, set[str]]) -> None:
    """Agrega valores a los ENUM existentes.

    Se ejecuta en autocommit: PostgreSQL no permite usar un valor nuevo
    dentro de la misma transacción en la que se agregó.
    """
    statements = []
    for name, values in _model_enums().items():
        have = existing.get(name)
        if not have:
            continue
        for value in values:
            if value not in have:
                statements.append(f'ALTER TYPE "{name}" ADD VALUE IF NOT EXISTS {_quote(value)}')
                log.info("Valor ENUM agregado: %s.%s", name, value)

    if not statements:
        return

    async with engine.connect().execution_options(isolation_level="AUTOCOMMIT") as conn:
        for statement in statements:
            await conn.execute(text(statement))


async def _sync_columns(conn: AsyncConnection) -> None:
    rows = (
        await conn.execute(
            text(
                "SELECT table_name, column_name FROM information_schema.columns "
                "WHERE table_schema = current_schema()"
            )
        )
    ).all()
    existing = {(table, column) for table, column in rows}
    tables = {table for table, _ in existing}

    for table in Base.metadata.tables.values():
        if table.name not in tables:
            continue
        for col in table.columns:
            if (table.name, col.name) in existing:
                continue
            ddl = _column_ddl(col)
            statement = f'ALTER TABLE "{table.name}" ADD COLUMN IF NOT EXISTS "{col.name}" {ddl}'
            default = _default_literal(col)
            if default:
                statement += f" DEFAULT {default}"
            await conn.execute(text(statement))
            log.warning("Columna agregada: %s.%s %s", table.name, col.name, ddl)


async def _seed_zones(conn: AsyncConnection) -> None:
    count = (await conn.execute(text("SELECT count(*) FROM zones"))).scalar_one()
    if count:
        return
    for name, is_urban, wkt, base_fare, per_km, night_pct, fee in _ZONES:
        await conn.execute(
            text(
                "INSERT INTO zones (name, is_urban, boundary, base_fare_cop, per_km_cop, "
                "night_surcharge_pct, billing_scheme, per_trip_fee_cop) "
                "VALUES (:name, :urban, ST_Multi(ST_GeomFromText(:wkt, 4326)), :base, :per_km, "
                ":night, 'per_trip_fee', :fee) ON CONFLICT (name) DO NOTHING"
            ),
            {
                "name": name,
                "urban": is_urban,
                "wkt": wkt,
                "base": base_fare,
                "per_km": per_km,
                "night": night_pct,
                "fee": fee,
            },
        )
    log.info("Zonas base de Barbosa sembradas")


async def sync_schema(engine: AsyncEngine) -> None:
    """Alinea la base de datos PostgreSQL con los modelos SQLAlchemy."""
    import app.models  # noqa: F401  (registra todas las tablas en Base.metadata)

    async with engine.begin() as conn:
        await _ensure_extensions(conn)
        existing = await _existing_enum_values(conn)
        await _create_missing_enum_types(conn, existing)

    await _add_missing_enum_values(engine, existing)

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
        await _sync_columns(conn)
        await _seed_zones(conn)

    log.info("[BarboYa DB] Esquema sincronizado con los modelos")
