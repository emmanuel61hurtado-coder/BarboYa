import uuid
from sqlalchemy import Column, String, Float, ForeignKey, Enum as SQLEnum, DateTime, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
import enum
from app.db.base import Base

class TipoServicioMove(str, enum.Enum):
    CARRO = "carro"
    MOTO = "moto"

class EstadoViaje(str, enum.Enum):
    SOLICITADO = "SOLICITADO"
    OFERTADO = "OFERTADO"
    ACEPTADO = "ACEPTADO"
    EN_CURSO = "EN_CURSO"
    FINALIZADO = "FINALIZADO"
    CANCELADO = "CANCELADO"

class ViajePasajero(Base):
    __tablename__ = "viajes_pasajeros"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    cliente_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    conductor_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=True)
    
    tipo_servicio = Column(SQLEnum(TipoServicioMove), nullable=False, default=TipoServicioMove.CARRO)
    
    origen_direccion = Column(String(255), nullable=False)
    origen_lat = Column(Float, nullable=False)
    origen_lng = Column(Float, nullable=False)
    
    destino_direccion = Column(String(255), nullable=False)
    destino_lat = Column(Float, nullable=False)
    destino_lng = Column(Float, nullable=False)
    
    precio_estimado = Column(Float, nullable=False)
    precio_propuesto = Column(Float, nullable=True)
    precio_final = Column(Float, nullable=True)
    
    estado = Column(SQLEnum(EstadoViaje), nullable=False, default=EstadoViaje.SOLICITADO)
    codigo_confirmacion = Column(String(6), nullable=False)
    
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    cliente = relationship("User", foreign_keys=[cliente_id], backref="viajes_como_cliente")
    conductor = relationship("User", foreign_keys=[conductor_id], backref="viajes_como_conductor")
