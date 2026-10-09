from pydantic import BaseModel, Field
from typing import Optional
import uuid
from datetime import datetime
from app.models.viaje import TipoServicioMove, EstadoViaje

class ViajeCreate(BaseModel):
    tipo_servicio: TipoServicioMove = TipoServicioMove.CARRO
    origen_direccion: str = Field(..., min_length=3, max_length=255)
    origen_lat: float
    origen_lng: float
    destino_direccion: str = Field(..., min_length=3, max_length=255)
    destino_lat: float
    destino_lng: float
    precio_estimado: float = Field(..., gt=0)
    precio_propuesto: Optional[float] = Field(None, gt=0)

class ViajeUpdate(BaseModel):
    estado: Optional[EstadoViaje] = None
    conductor_id: Optional[uuid.UUID] = None
    precio_propuesto: Optional[float] = Field(None, gt=0)
    precio_final: Optional[float] = Field(None, gt=0)

class ViajeResponse(BaseModel):
    id: uuid.UUID
    cliente_id: uuid.UUID
    conductor_id: Optional[uuid.UUID] = None
    tipo_servicio: TipoServicioMove
    origen_direccion: str
    origen_lat: float
    origen_lng: float
    destino_direccion: str
    destino_lat: float
    destino_lng: float
    precio_estimado: float
    precio_propuesto: Optional[float] = None
    precio_final: Optional[float] = None
    estado: EstadoViaje
    codigo_confirmacion: str
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True
