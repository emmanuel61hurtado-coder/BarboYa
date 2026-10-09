import uuid
from datetime import datetime
from sqlalchemy import Column, String, DateTime, ForeignKey, Enum as SQLEnum
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from app.db.base import Base
import enum


class TipoVehiculo(str, enum.Enum):
    MOTO = "MOTO"
    BICI = "BICI"
    CARRO = "CARRO"


class Vehiculo(Base):
    __tablename__ = "vehiculos"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    repartidor_id = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, unique=True, index=True)
    tipo = Column(SQLEnum(TipoVehiculo), nullable=False, default=TipoVehiculo.MOTO)
    placa = Column(String, nullable=True)
    modelo = Column(String, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    repartidor = relationship("User", backref="vehiculo")
