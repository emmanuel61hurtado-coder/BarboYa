import uuid
from datetime import datetime
from sqlalchemy import Column, String, DateTime, ForeignKey, Numeric, Enum as SQLEnum
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from app.db.base import Base
import enum


class EstadoPedido(str, enum.Enum):
    CREADO = "CREADO"
    ACEPTADO = "ACEPTADO"
    PREPARANDO = "PREPARANDO"
    LISTO = "LISTO"
    EN_CAMINO = "EN_CAMINO"
    ENTREGADO = "ENTREGADO"
    CANCELADO = "CANCELADO"


class Pedido(Base):
    __tablename__ = "pedidos"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    cliente_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False, index=True)
    comercio_id = Column(UUID(as_uuid=True), ForeignKey("comercios.id"), nullable=False, index=True)
    repartidor_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=True, index=True)
    direccion_id = Column(UUID(as_uuid=True), ForeignKey("direcciones.id"), nullable=False)
    
    estado = Column(SQLEnum(EstadoPedido), nullable=False, default=EstadoPedido.CREADO, index=True)
    subtotal = Column(Numeric(12, 2), nullable=False)
    costo_envio = Column(Numeric(12, 2), nullable=False)
    descuento = Column(Numeric(12, 2), default=0.0, nullable=False)
    total = Column(Numeric(12, 2), nullable=False)
    
    metodo_pago = Column(String, nullable=False)
    cupon_codigo = Column(String, nullable=True)
    
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    cliente = relationship("User", foreign_keys=[cliente_id], backref="pedidos_cliente")
    comercio = relationship("Comercio", backref="pedidos")
    repartidor = relationship("User", foreign_keys=[repartidor_id], backref="pedidos_repartidor")
    direccion = relationship("Direccion")
