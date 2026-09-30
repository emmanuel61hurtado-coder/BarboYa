import uuid
from datetime import datetime
from sqlalchemy import Column, String, Integer, DateTime, ForeignKey, Numeric, CheckConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from app.db.base import Base


class DetallePedido(Base):
    __tablename__ = "detalle_pedidos"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    pedido_id = Column(UUID(as_uuid=True), ForeignKey("pedidos.id", ondelete="CASCADE"), nullable=False, index=True)
    producto_id = Column(UUID(as_uuid=True), ForeignKey("productos.id"), nullable=False)
    
    nombre_producto = Column(String, nullable=False) # Snapshot
    precio_unitario = Column(Numeric(12, 2), nullable=False) # Snapshot
    cantidad = Column(Integer, nullable=False)
    subtotal = Column(Numeric(12, 2), nullable=False)
    
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)

    __table_args__ = (
        CheckConstraint("cantidad > 0", name="check_detalle_cantidad_positiva"),
        CheckConstraint("precio_unitario >= 0", name="check_detalle_precio_positivo"),
    )

    pedido = relationship("Pedido", backref="detalles")
    producto = relationship("Producto")
