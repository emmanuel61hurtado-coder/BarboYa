"""Pruebas del sincronizador de esquema y de los archivos de despliegue.

No requieren PostgreSQL: validan el SQL que el sincronizador genera y que los
archivos de Docker/Coolify se mantengan consistentes con los modelos.
"""

from pathlib import Path

import pytest
import yaml

from app.core.database import Base
from app.core.schema_sync import _column_ddl, _default_literal, _model_enums

ROOT = Path(__file__).resolve().parents[2]
COMPOSE_FILES = [ROOT / "docker-compose.yml", ROOT / "docker-compose.backend.yml"]


@pytest.fixture(scope="module")
def users_table():
    return Base.metadata.tables["users"]


@pytest.fixture(scope="module")
def driver_table():
    return Base.metadata.tables["driver_profiles"]


def test_column_ddl_uses_postgres_types(users_table, driver_table):
    ddl = {col.name: _column_ddl(col) for col in users_table.columns}
    assert ddl["id"] == "UUID"
    assert ddl["phone"] == "VARCHAR"
    assert ddl["password_hash"] == "TEXT"
    assert ddl["created_at"] == "TIMESTAMP WITH TIME ZONE"
    assert ddl["role"] == "user_role"
    assert ddl["gender"] == "user_gender"

    driver_ddl = {col.name: _column_ddl(col) for col in driver_table.columns}
    assert driver_ddl["current_location"] == "geography(POINT,4326)"
    assert driver_ddl["verification_status"] == "verification_status"


def test_default_literal_uses_enum_values(users_table, driver_table):
    ddl = {col.name: _default_literal(col) for col in users_table.columns}
    assert ddl["role"] == "'customer'"
    assert ddl["is_active"] == "TRUE"

    driver_ddl = {col.name: _default_literal(col) for col in driver_table.columns}
    assert driver_ddl["verification_status"] == "'pending'"
    assert driver_ddl["wallet_balance_cop"] == "0"
    assert driver_ddl["rating_avg"] == "5.0"


def test_model_enums_cover_business_types():
    enums = _model_enums()
    assert "user_role" in enums
    assert "user_gender" in enums
    assert "verification_status" in enums
    assert "merchant_status" in enums
    assert "order_status" in enums
    assert "pending_confirmation" in enums["order_status"]
    assert "courier_assigned" in enums["order_status"]
    assert "in_transit" in enums["order_status"]


@pytest.mark.parametrize("compose_file", COMPOSE_FILES, ids=lambda p: p.name)
def test_compose_file_is_utf8_without_bom(compose_file):
    """Coolify parsea mal los archivos con BOM: por eso el control de bytes."""
    raw = compose_file.read_bytes()
    assert not raw.startswith(b"\xef\xbb\xbf")
    assert raw.startswith(b"services:")


@pytest.mark.parametrize("compose_file", COMPOSE_FILES, ids=lambda p: p.name)
def test_compose_declares_required_services(compose_file):
    data = yaml.safe_load(compose_file.read_text(encoding="utf-8"))
    assert set(data["services"]) == {"db", "redis", "api"}
    # Postgres no bloquea el despliegue: la API reintenta la sincronizacion.
    assert data["services"]["api"]["depends_on"]["db"]["condition"] == "service_started"
    assert data["services"]["api"]["depends_on"]["redis"]["condition"] == "service_healthy"
    assert data["services"]["api"]["environment"]["USE_SQLITE"] == "false"
    assert "JWT_SECRET" in data["services"]["api"]["environment"]
    assert "healthcheck" in data["services"]["api"]
    # Margen suficiente para el initdb con PostGIS en un VPS lento.
    assert data["services"]["db"]["healthcheck"]["start_period"] >= "30s"


@pytest.mark.parametrize("compose_file", COMPOSE_FILES, ids=lambda p: p.name)
def test_compose_uses_tolerant_init_script(compose_file):
    data = yaml.safe_load(compose_file.read_text(encoding="utf-8"))
    volumes = data["services"]["db"]["volumes"]
    assert any("/docker-entrypoint-initdb.d" in vol for vol in volumes)
    assert not any(vol.endswith(":01-schema.sql:ro") for vol in volumes)


def test_init_script_is_shell_without_crlf():
    script = ROOT / "database" / "init" / "00-init.sh"
    raw = script.read_bytes()
    assert b"\r\n" not in raw
    assert raw.startswith(b"#!/usr/bin/env sh")
    assert b"ON_ERROR_STOP=1" in raw


def test_schema_sql_declares_model_enums():
    sql = (ROOT / "database" / "schema.sql").read_text(encoding="utf-8")
    for enum_name in ("user_gender", "verification_status", "merchant_status"):
        assert f"CREATE TYPE {enum_name}" in sql
    for column in ("gender", "prefer_female_driver", "status_history", "delivery_pin"):
        assert column in sql


class _FakeResult:
    def __init__(self, rows):
        self._rows = rows

    def all(self):
        return self._rows

    def scalar_one(self):
        return self._rows[0][0]


class _FakeConn:
    """Captura el SQL ejecutado por el sincronizador."""

    def __init__(self, results=None):
        self.executed = []
        self._results = list(results or [])

    async def execute(self, statement, params=None):
        self.executed.append(str(statement))
        if self._results:
            return self._results.pop(0)
        return _FakeResult([])


@pytest.mark.asyncio
async def test_sync_columns_adds_missing_columns():
    from app.core.schema_sync import _sync_columns

    existing = [("users", "id"), ("users", "phone"), ("users", "role")]
    conn = _FakeConn([_FakeResult(existing)])
    await _sync_columns(conn)

    alters = [stmt for stmt in conn.executed if stmt.startswith("ALTER TABLE")]
    assert 'ALTER TABLE "users" ADD COLUMN IF NOT EXISTS "email" VARCHAR' in alters
    assert 'ALTER TABLE "users" ADD COLUMN IF NOT EXISTS "gender" user_gender' in alters
    assert 'ALTER TABLE "users" ADD COLUMN IF NOT EXISTS "is_active" BOOLEAN DEFAULT TRUE' in alters
    assert "ALTER TABLE" not in " ".join(stmt for stmt in conn.executed if "users" in stmt and "id" in stmt)


@pytest.mark.asyncio
async def test_seed_zones_is_idempotent_and_uses_postgis():
    from app.core.schema_sync import _seed_zones

    conn = _FakeConn([_FakeResult([(3,)])])
    await _seed_zones(conn)
    assert conn.executed == ["SELECT count(*) FROM zones"]

    conn = _FakeConn([_FakeResult([(0,)]), _FakeResult([]), _FakeResult([])])
    await _seed_zones(conn)
    assert any("ST_GeomFromText" in stmt for stmt in conn.executed)
    assert any("ON CONFLICT (name) DO NOTHING" in stmt for stmt in conn.executed)

