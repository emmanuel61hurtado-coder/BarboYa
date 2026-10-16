import uuid
from datetime import datetime
from sqlalchemy import Column, String, DateTime, ForeignKey, Numeric, Boolean, Time, Integer, Enum as SQLEnum
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from app.db.base import Base
import enum


class EstadoCita(str, enum.Enum):
    PENDIENTE = "PENDIENTE"
    CONFIRMADA = "CONFIRMADA"
    COMPLETADA = "COMPLETADA"
    CANCELADA = "CANCELADA"
    NO_SHOW = "NO_SHOW"


class BarberiaTenant(Base):
    __tablename__ = "barberias_tenant"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    nombre = Column(String, nullable=False, index=True)
    nit = Column(String, unique=True, index=True, nullable=False)
    dueno_id = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    telefono = Column(String, nullable=False)
    direccion = Column(String, nullable=False)
    ciudad = Column(String, nullable=False, index=True)
    logo_url = Column(String, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    sedes = relationship("Sede", back_populates="barberia", cascade="all, delete-orphan")


class Sede(Base):
    __tablename__ = "sedes"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    barberia_id = Column(UUID(as_uuid=True), ForeignKey("barberias_tenant.id", ondelete="CASCADE"), nullable=False)
    nombre = Column(String, nullable=False)
    direccion = Column(String, nullable=False)
    ciudad = Column(String, nullable=False, index=True)
    lat = Column(Numeric(10, 8), nullable=False)
    lng = Column(Numeric(11, 8), nullable=False)
    telefono = Column(String, nullable=False)
    abierta = Column(Boolean, default=True, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    barberia = relationship("BarberiaTenant", back_populates="sedes")
    staff = relationship("StaffBarbero", back_populates="sede", cascade="all, delete-orphan")
    servicios = relationship("ServicioBarberia", back_populates="sede", cascade="all, delete-orphan")


class StaffBarbero(Base):
    __tablename__ = "staff_barberos"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    sede_id = Column(UUID(as_uuid=True), ForeignKey("sedes.id", ondelete="CASCADE"), nullable=False)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    especialidad = Column(String, nullable=True)
    calificacion_promedio = Column(Numeric(3, 2), default=5.0, nullable=False)
    activo = Column(Boolean, default=True, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    sede = relationship("Sede", back_populates="staff")
    horarios = relationship("HorarioStaff", back_populates="staff", cascade="all, delete-orphan")
    citas = relationship("Cita", back_populates="staff")


class ServicioBarberia(Base):
    __tablename__ = "servicios_barberia"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    sede_id = Column(UUID(as_uuid=True), ForeignKey("sedes.id", ondelete="CASCADE"), nullable=False)
    nombre = Column(String, nullable=False)
    descripcion = Column(String, nullable=True)
    precio = Column(Numeric(10, 2), nullable=False)
    duracion_minutos = Column(Integer, nullable=False, default=30)
    activo = Column(Boolean, default=True, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    sede = relationship("Sede", back_populates="servicios")
    citas = relationship("Cita", back_populates="servicio")


class HorarioStaff(Base):
    __tablename__ = "horarios_staff"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    staff_id = Column(UUID(as_uuid=True), ForeignKey("staff_barberos.id", ondelete="CASCADE"), nullable=False)
    dia_semana = Column(Integer, nullable=False)  # 0=Lunes, 6=Domingo
    hora_inicio = Column(Time, nullable=False)
    hora_fin = Column(Time, nullable=False)
    activo = Column(Boolean, default=True, nullable=False)

    staff = relationship("StaffBarbero", back_populates="horarios")


class Cita(Base):
    __tablename__ = "citas"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    cliente_id = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    staff_id = Column(UUID(as_uuid=True), ForeignKey("staff_barberos.id", ondelete="CASCADE"), nullable=False)
    servicio_id = Column(UUID(as_uuid=True), ForeignKey("servicios_barberia.id", ondelete="CASCADE"), nullable=False)
    fecha_hora = Column(DateTime, nullable=False, index=True)
    estado = Column(SQLEnum(EstadoCita), nullable=False, default=EstadoCita.PENDIENTE)
    notas = Column(String, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    cliente = relationship("User")
    staff = relationship("StaffBarbero", back_populates="citas")
    servicio = relationship("ServicioBarberia", back_populates="citas")
