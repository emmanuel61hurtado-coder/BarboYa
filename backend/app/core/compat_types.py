"""
BarboYa SQLAlchemy Type Compatibility

Provides type aliases that resolve to PostgreSQL-native types when Postgres is
available, or to SQLite-friendly equivalents for local development.

Usage in models:
    from app.core.compat_types import GUID, GeoPoint, GeoMultiPolygon, JsonDict
"""

from __future__ import annotations

import json
import uuid
from typing import Any

from sqlalchemy import JSON, String, Text, TypeDecorator

from app.core.database import is_using_sqlite


# ──────────────────────── UUID → CHAR(32) / native ────────────────────────

class GUID(TypeDecorator):
    """Platform-agnostic UUID type.

    Uses PostgreSQL's UUID type when available, otherwise stores as CHAR(32).
    """

    impl = String
    cache_ok = True

    def load_dialect_impl(self, dialect):
        if dialect.name == "postgresql":
            from sqlalchemy.dialects.postgresql import UUID as PG_UUID
            return dialect.type_descriptor(PG_UUID(as_uuid=True))
        else:
            return dialect.type_descriptor(String(36))

    def process_bind_param(self, value, dialect):
        if value is None:
            return value
        if dialect.name == "postgresql":
            return value if isinstance(value, uuid.UUID) else uuid.UUID(str(value))
        else:
            return str(value) if isinstance(value, uuid.UUID) else str(uuid.UUID(value))

    def process_result_value(self, value, dialect):
        if value is None:
            return value
        if not isinstance(value, uuid.UUID):
            return uuid.UUID(str(value))
        return value


# ──────────────────────── JSONB → JSON ────────────────────────

class JsonDict(TypeDecorator):
    """Uses JSONB on PostgreSQL, plain JSON on SQLite."""

    impl = JSON
    cache_ok = True

    def load_dialect_impl(self, dialect):
        if dialect.name == "postgresql":
            from sqlalchemy.dialects.postgresql import JSONB
            return dialect.type_descriptor(JSONB())
        else:
            return dialect.type_descriptor(JSON())

    def process_bind_param(self, value, dialect):
        if dialect.name != "postgresql" and value is not None:
            if isinstance(value, (dict, list)):
                return json.dumps(value)
        return value

    def process_result_value(self, value, dialect):
        if dialect.name != "postgresql" and value is not None:
            if isinstance(value, str):
                try:
                    return json.loads(value)
                except (json.JSONDecodeError, TypeError):
                    return value
        return value


# ──────────────────────── Geography/Geometry → Text (SQLite) ────────────────────────

class GeoPoint(TypeDecorator):
    """Geography POINT on PostgreSQL, plain Text storage on SQLite (WKT format)."""

    impl = Text
    cache_ok = True

    def load_dialect_impl(self, dialect):
        if dialect.name == "postgresql":
            from geoalchemy2 import Geography
            return dialect.type_descriptor(Geography("POINT", srid=4326))
        else:
            return dialect.type_descriptor(Text())


class GeoMultiPolygon(TypeDecorator):
    """Geometry MULTIPOLYGON on PostgreSQL, plain Text on SQLite."""

    impl = Text
    cache_ok = True

    def load_dialect_impl(self, dialect):
        if dialect.name == "postgresql":
            from geoalchemy2 import Geometry
            return dialect.type_descriptor(Geometry("MULTIPOLYGON", srid=4326))
        else:
            return dialect.type_descriptor(Text())
