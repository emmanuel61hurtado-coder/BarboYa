from pydantic import BaseModel, Field
from typing import Optional
import uuid
from datetime import datetime
from app.models.envio import TipoPaquete, EstadoEnvio

class EnvioCreate(BaseModel):
    tipo_paquete: TipoPaquete = TipoPaquete.PEQUENO
    descripcion: str = Field(..., min_length=3)
    peso_kg: Optional[float] = Field(None, gt=0)
    origen_direccion: str = Field(..., min_length=3, max_length=255)
    origen_lat: float
    origen_lng: float
    destino_direccion: str = Field(..., min_length=3, max_length=255)
    destino_lat: float
    destino_lng: float
    nombre_destinatario: str = Field(..., min_length=2, max_length=100)
    telefono_destinatario: str = Field(..., min_length=5, max_length=30)
    instrucciones: Optional[str] = None
    costo: float = Field(..., gt=0)

class EnvioUpdate(BaseModel):
    estado: Optional[EstadoEnvio] = None
    repartidor_id: Optional[uuid.UUID] = None

class EnvioResponse(BaseModel):
    id: uuid.UUID
    remitente_id: uuid.UUID
    repartidor_id: Optional[uuid.UUID] = None
    tipo_paquete: TipoPaquete
    descripcion: str
    peso_kg: Optional[float] = None
    origen_direccion: str
    origen_lat: float
    origen_lng: float
    destino_direccion: str
    destino_lat: float
    destino_lng: float
    nombre_destinatario: str
    telefono_destinatario: str
    instrucciones: Optional[str] = None
    costo: float
    codigo_entrega: str
    estado: EstadoEnvio
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True
