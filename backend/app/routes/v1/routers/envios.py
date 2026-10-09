from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from typing import List
import uuid
import random

from app.db.session import get_db
from app.models.envio import EnvioPaquete, EstadoEnvio
from app.schemas.envio import EnvioCreate, EnvioUpdate, EnvioResponse
from app.deps import get_current_user
from app.models.user import User

router = APIRouter(prefix="/envios", tags=["BarboYa Envíos (Paquetes)"])

@router.post("/", response_model=EnvioResponse, status_code=status.HTTP_201_CREATED)
async def crear_envio(
    envio_in: EnvioCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    codigo = f"{random.randint(1000, 9999)}"
    envio = EnvioPaquete(
        remitente_id=current_user.id,
        tipo_paquete=envio_in.tipo_paquete,
        descripcion=envio_in.descripcion,
        peso_kg=envio_in.peso_kg,
        origen_direccion=envio_in.origen_direccion,
        origen_lat=envio_in.origen_lat,
        origen_lng=envio_in.origen_lng,
        destino_direccion=envio_in.destino_direccion,
        destino_lat=envio_in.destino_lat,
        destino_lng=envio_in.destino_lng,
        nombre_destinatario=envio_in.nombre_destinatario,
        telefono_destinatario=envio_in.telefono_destinatario,
        instrucciones=envio_in.instrucciones,
        costo=envio_in.costo,
        codigo_entrega=codigo,
        estado=EstadoEnvio.SOLICITADO
    )
    db.add(envio)
    await db.commit()
    await db.refresh(envio)
    return envio

@router.get("/", response_model=List[EnvioResponse])
async def listar_envios(
    skip: int = 0,
    limit: int = 100,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    query = select(EnvioPaquete)
    if current_user.rol.value not in ["admin", "agente_soporte"]:
        query = query.filter(
            (EnvioPaquete.remitente_id == current_user.id) | 
            (EnvioPaquete.repartidor_id == current_user.id)
        )
    query = query.offset(skip).limit(limit)
    result = await db.execute(query)
    return result.scalars().all()

@router.get("/{envio_id}", response_model=EnvioResponse)
async def obtener_envio(
    envio_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    result = await db.execute(select(EnvioPaquete).filter(EnvioPaquete.id == envio_id))
    envio = result.scalars().first()
    if not envio:
        raise HTTPException(status_code=404, detail="Envío no encontrado")
    return envio

@router.patch("/{envio_id}", response_model=EnvioResponse)
async def actualizar_envio(
    envio_id: uuid.UUID,
    envio_in: EnvioUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    result = await db.execute(select(EnvioPaquete).filter(EnvioPaquete.id == envio_id))
    envio = result.scalars().first()
    if not envio:
        raise HTTPException(status_code=404, detail="Envío no encontrado")

    if envio_in.estado:
        envio.estado = envio_in.estado
    if envio_in.repartidor_id:
        envio.repartidor_id = envio_in.repartidor_id

    await db.commit()
    await db.refresh(envio)
    return envio
