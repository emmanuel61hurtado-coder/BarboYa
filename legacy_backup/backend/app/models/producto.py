import uuid
from datetime import datetime
from sqlalchemy import Column, String, Boolean, DateTime, ForeignKey, Numeric, CheckConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from app.db.base import Base


class Producto(Base):
    __tablename__ = "productos"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    comercio_id = Column(UUID(as_uuid=True), ForeignKey("comercios.id", ondelete="CASCADE"), nullable=False, index=True)
    categoria_id = Column(UUID(as_uuid=True), ForeignKey("categorias.id", ondelete="SET NULL"), nullable=True)
    nombre = Column(String, nullable=False, index=True)
    descripcion = Column(String)
    precio = Column(Numeric(12, 2), nullable=False)
    imagen_url = Column(String)
    disponible = Column(Boolean, default=True, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    __table_args__ = (
        CheckConstraint("precio >= 0", name="check_producto_precio_positivo"),
    )

    comercio = relationship("Comercio", backref="productos")
    categoria = relationship("Categoria")
