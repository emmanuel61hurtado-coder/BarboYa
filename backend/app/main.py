from contextlib import asynccontextmanager

import redis.asyncio as aioredis
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import get_settings
from app.routers import auth, trips, wallet, ws
from app.websockets.manager import manager

settings = get_settings()


@asynccontextmanager
async def lifespan(app: FastAPI):
    client = aioredis.from_url(settings.redis_url)
    await manager.attach_redis(client)
    yield
    await client.aclose()


app = FastAPI(
    title=settings.app_name,
    description="Movilidad, mensajería y domicilios locales de Barbosa, Santander.",
    version="0.1.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=False,  # con "*" no se permiten credenciales; en prod listar orígenes
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(trips.router)
app.include_router(trips.public_router)
app.include_router(wallet.router)
app.include_router(wallet.admin)
app.include_router(ws.router)


@app.get("/health", tags=["meta"])
async def health():
    return {"status": "ok", "app": "Barboya"}
