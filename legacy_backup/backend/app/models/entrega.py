import uuid
from datetime import datetime
from sqlalchemy import Column, DateTime, ForeignKey, Numeric, Enum as SQLEnum
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from app.db.base import Base
import enum


class EstadoEntrega(str, enum.Enum):
    PENDIENTE = "PENDIENTE"
    ASIGNADA = "ASIGNADA"
    EN_CAMINO = "EN_CAMINO"
    ENTREGADA = "ENTREGADA"


class Entrega(Base):
    __tablename__ = "entregas"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    pedido_id = Column(UUID(as_uuid=True), ForeignKey("pedidos.id", ondelete="CASCADE"), nullable=False, unique=True, index=True)
    repartidor_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=True, index=True)
    estado = Column(SQLEnum(EstadoEntrega), nullable=False, default=EstadoEntrega.PENDIENTE)
    costo_domicilio = Column(Numeric(12, 2), nullable=False)
    lat_actual = Column(Numeric(10, 8), nullable=True)
    lng_actual = Column(Numeric(11, 8), nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    pedido = relationship("Pedido", backref="entrega")
    repartidor = relationship("User", foreign_keys=[repartidor_id], backref="entregas")
