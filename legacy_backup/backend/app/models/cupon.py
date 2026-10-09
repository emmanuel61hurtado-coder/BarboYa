import uuid
from datetime import datetime
from sqlalchemy import Column, String, Integer, Boolean, DateTime, Numeric, CheckConstraint
from sqlalchemy.dialects.postgresql import UUID
from app.db.base import Base


class Cupon(Base):
    __tablename__ = "cupones"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    codigo = Column(String, unique=True, index=True, nullable=False)
    descuento_porcentaje = Column(Numeric(5, 2), nullable=True)
    descuento_monto = Column(Numeric(12, 2), nullable=True)
    monto_minimo = Column(Numeric(12, 2), default=0.0, nullable=False)
    usos_maximos = Column(Integer, default=100, nullable=False)
    usos_actuales = Column(Integer, default=0, nullable=False)
    activo = Column(Boolean, default=True, nullable=False)
    expiracion = Column(DateTime, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    __table_args__ = (
        CheckConstraint("usos_actuales <= usos_maximos", name="check_cupon_usos"),
    )
