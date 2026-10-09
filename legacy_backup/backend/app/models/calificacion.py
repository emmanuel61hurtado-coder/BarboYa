import uuid
from datetime import datetime
from sqlalchemy import Column, String, Integer, DateTime, ForeignKey, CheckConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from app.db.base import Base


class Calificacion(Base):
    __tablename__ = "calificaciones"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    pedido_id = Column(UUID(as_uuid=True), ForeignKey("pedidos.id", ondelete="CASCADE"), nullable=False, index=True)
    cliente_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    comercio_id = Column(UUID(as_uuid=True), ForeignKey("comercios.id"), nullable=True)
    repartidor_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=True)
    
    puntuacion = Column(Integer, nullable=False) # 1 to 5
    comentario = Column(String)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)

    __table_args__ = (
        CheckConstraint("puntuacion >= 1 AND puntuacion <= 5", name="check_puntuacion_rango"),
    )

    pedido = relationship("Pedido", backref="calificaciones")
    cliente = relationship("User", foreign_keys=[cliente_id])
    comercio = relationship("Comercio", backref="calificaciones")
    repartidor = relationship("User", foreign_keys=[repartidor_id])
