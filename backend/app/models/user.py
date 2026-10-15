import uuid
from datetime import datetime
from sqlalchemy import Column, String, DateTime, Enum as SQLEnum
from sqlalchemy.dialects.postgresql import UUID
from app.db.base import Base
import enum


class UserRole(str, enum.Enum):
    CLIENTE = "CLIENTE"
    REPARTIDOR = "REPARTIDOR"
    COMERCIO = "COMERCIO"
    ADMIN = "ADMIN"


class UserStatus(str, enum.Enum):
    PENDIENTE = "PENDIENTE"
    ACTIVO = "ACTIVO"
    BLOQUEADO = "BLOQUEADO"


class User(Base):
    __tablename__ = "users"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    email = Column(String, unique=True, index=True, nullable=False)
    password_hash = Column(String, nullable=False)
    nombre = Column(String, nullable=False)
    telefono = Column(String, nullable=False)
    rol = Column(SQLEnum(UserRole), nullable=False, default=UserRole.CLIENTE)
    estado = Column(SQLEnum(UserStatus), nullable=False, default=UserStatus.ACTIVO)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)
