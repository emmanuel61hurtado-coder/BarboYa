import uuid
from sqlalchemy import Column, String, Float, ForeignKey, Enum as SQLEnum, DateTime, func, Text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
import enum
from app.db.base import Base

class TipoPaquete(str, enum.Enum):
    DOCUMENTO = "documento"
    PEQUENO = "pequeño"
    MEDIANO = "mediano"
    GRANDE = "grande"

class EstadoEnvio(str, enum.Enum):
    SOLICITADO = "SOLICITADO"
    RECOGIDO = "RECOGIDO"
    EN_CAMINO = "EN_CAMINO"
    ENTREGADO = "ENTREGADO"
    CANCELADO = "CANCELADO"

class EnvioPaquete(Base):
    __tablename__ = "envios_paquetes"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    remitente_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    repartidor_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=True)
    
    tipo_paquete = Column(SQLEnum(TipoPaquete), nullable=False, default=TipoPaquete.PEQUENO)
    descripcion = Column(Text, nullable=False)
    peso_kg = Column(Float, nullable=True)
    
    origen_direccion = Column(String(255), nullable=False)
    origen_lat = Column(Float, nullable=False)
    origen_lng = Column(Float, nullable=False)
    
    destino_direccion = Column(String(255), nullable=False)
    destino_lat = Column(Float, nullable=False)
    destino_lng = Column(Float, nullable=False)
    
    nombre_destinatario = Column(String(100), nullable=False)
    telefono_destinatario = Column(String(30), nullable=False)
    instrucciones = Column(Text, nullable=True)
    
    costo = Column(Float, nullable=False)
    codigo_entrega = Column(String(6), nullable=False)
    
    estado = Column(SQLEnum(EstadoEnvio), nullable=False, default=EstadoEnvio.SOLICITADO)
    
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    remitente = relationship("User", foreign_keys=[remitente_id], backref="envios_como_remitente")
    repartidor = relationship("User", foreign_keys=[repartidor_id], backref="envios_como_repartidor")
