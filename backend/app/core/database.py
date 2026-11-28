"""
BarboYa Database Engine
- Primary: PostgreSQL + asyncpg (production/docker)
- Fallback: SQLite + aiosqlite (local dev without Docker)

Automatically detects which engine to use based on config and connectivity.
"""

from __future__ import annotations

import uuid
from collections.abc import AsyncIterator

from sqlalchemy import String, Text, event
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.orm import DeclarativeBase

from app.core.config import get_settings

settings = get_settings()

# ──────────────────────── RESOLVE DATABASE ENGINE ────────────────────────

_using_sqlite = False


def _resolve_database_url() -> str:
    """Pick the right DB url. If use_sqlite is True, go straight to SQLite."""
    global _using_sqlite

    if settings.use_sqlite:
        _using_sqlite = True
        print("[BarboYa DB] [SQLite] Modo SQLite activado (desarrollo local sin PostgreSQL)")
        return settings.sqlite_url

    # Otherwise, try PostgreSQL (it might still fail at connect time)
    _using_sqlite = False
    print("[BarboYa DB] [PostgreSQL] Usando PostgreSQL:", settings.database_url[:50] + "...")
    return settings.database_url


_db_url = _resolve_database_url()

# SQLite-specific engine args
_engine_kwargs: dict = {"pool_pre_ping": True}
if _using_sqlite:
    _engine_kwargs = {
        "connect_args": {"check_same_thread": False},
    }

engine = create_async_engine(_db_url, **_engine_kwargs)
SessionLocal = async_sessionmaker(engine, expire_on_commit=False)


# ──────────────────────── SQLITE UUID SUPPORT ────────────────────────

if _using_sqlite:
    @event.listens_for(engine.sync_engine, "connect")
    def _sqlite_pragmas(dbapi_conn, _):
        """Enable WAL mode and foreign keys for SQLite."""
        cursor = dbapi_conn.cursor()
        cursor.execute("PRAGMA journal_mode=WAL;")
        cursor.execute("PRAGMA foreign_keys=ON;")
        cursor.close()


# ──────────────────────── BASE ────────────────────────

class Base(DeclarativeBase):
    pass


# ──────────────────────── SESSION DEPENDENCY ────────────────────────

async def get_session() -> AsyncIterator[AsyncSession]:
    async with SessionLocal() as session:
        yield session


# ──────────────────────── TABLE CREATION (SQLite Dev) ────────────────────────

async def init_db():
    """Crea/verifica el esquema.

    - SQLite (dev): `create_all` sobre las tablas definidas en los modelos.
    - PostgreSQL (producción): sincroniza tipos ENUM, tablas y columnas con los
      modelos, de modo que la base siempre quede alineada aunque
      `database/schema.sql` haya fallado en el initdb del contenedor.
    """
    # Import all models so Base.metadata knows about them
    import app.models  # noqa: F401

    if not _using_sqlite:
        from app.core.schema_sync import sync_schema

        await sync_schema(engine)
        return

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    print("[BarboYa DB] [OK] Tablas SQLite creadas/verificadas correctamente")


def is_using_sqlite() -> bool:
    return _using_sqlite
