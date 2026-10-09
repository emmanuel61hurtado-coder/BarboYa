from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from typing import List
import uuid
import random

from app.db.session import get_db
from app.models.viaje import ViajePasajero, EstadoViaje
from app.schemas.viaje import ViajeCreate, ViajeUpdate, ViajeResponse
from app.deps import get_current_user
from app.models.user import User

router = APIRouter(prefix="/viajes", tags=["BarboYa Move (Transporte)"])

@router.post("/", response_model=ViajeResponse, status_code=status.HTTP_201_CREATED)
async def solicitar_viaje(
    viaje_in: ViajeCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    codigo = f"{random.randint(1000, 9999)}"
    viaje = ViajePasajero(
        cliente_id=current_user.id,
        tipo_servicio=viaje_in.tipo_servicio,
        origen_direccion=viaje_in.origen_direccion,
        origen_lat=viaje_in.origen_lat,
        origen_lng=viaje_in.origen_lng,
        destino_direccion=viaje_in.destino_direccion,
        destino_lat=viaje_in.destino_lat,
        destino_lng=viaje_in.destino_lng,
        precio_estimado=viaje_in.precio_estimado,
        precio_propuesto=viaje_in.precio_propuesto,
        precio_final=viaje_in.precio_propuesto or viaje_in.precio_estimado,
        estado=EstadoViaje.SOLICITADO,
        codigo_confirmacion=codigo
    )
    db.add(viaje)
    await db.commit()
    await db.refresh(viaje)
    return viaje

@router.get("/", response_model=List[ViajeResponse])
async def listar_viajes(
    skip: int = 0,
    limit: int = 100,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    query = select(ViajePasajero)
    if current_user.rol.value not in ["admin", "agente_soporte"]:
        query = query.filter(
            (ViajePasajero.cliente_id == current_user.id) | 
            (ViajePasajero.conductor_id == current_user.id)
        )
    query = query.offset(skip).limit(limit)
    result = await db.execute(query)
    return result.scalars().all()

@router.get("/{viaje_id}", response_model=ViajeResponse)
async def obtener_viaje(
    viaje_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    result = await db.execute(select(ViajePasajero).filter(ViajePasajero.id == viaje_id))
    viaje = result.scalars().first()
    if not viaje:
        raise HTTPException(status_code=404, detail="Viaje no encontrado")
    return viaje

@router.patch("/{viaje_id}", response_model=ViajeResponse)
async def actualizar_viaje(
    viaje_id: uuid.UUID,
    viaje_in: ViajeUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    result = await db.execute(select(ViajePasajero).filter(ViajePasajero.id == viaje_id))
    viaje = result.scalars().first()
    if not viaje:
        raise HTTPException(status_code=404, detail="Viaje no encontrado")

    if viaje_in.estado:
        viaje.estado = viaje_in.estado
    if viaje_in.conductor_id:
        viaje.conductor_id = viaje_in.conductor_id
    if viaje_in.precio_propuesto:
        viaje.precio_propuesto = viaje_in.precio_propuesto
    if viaje_in.precio_final:
        viaje.precio_final = viaje_in.precio_final

    await db.commit()
    await db.refresh(viaje)
    return viaje
