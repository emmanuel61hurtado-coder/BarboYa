from contextlib import asynccontextmanager

import redis.asyncio as aioredis
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response

from app.core.config import get_settings
from app.routers import admin, auth, drivers, merchants, security, trips, wallet, webhooks, ws
from app.websockets.manager import manager

settings = get_settings()


class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        response: Response = await call_next(request)
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["X-Frame-Options"] = "DENY"
        response.headers["X-XSS-Protection"] = "1; mode=block"
        response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"
        response.headers["Content-Security-Policy"] = "default-src 'self'"
        return response


@asynccontextmanager
async def lifespan(app: FastAPI):
    client = None
    try:
        client = aioredis.from_url(settings.redis_url)
        await manager.attach_redis(client)
    except Exception as err:
        print(f"[BarboYa] Redis no disponible ({err}). Continuando servidor FastAPI en memoria...")
    yield
    if client:
        try:
            await client.aclose()
        except Exception:
            pass


app = FastAPI(
    title=settings.app_name,
    description="Movilidad, mensajería y domicilios locales de Barbosa, Santander.",
    version="0.1.0",
    lifespan=lifespan,
)

app.add_middleware(SecurityHeadersMiddleware)

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
app.include_router(admin.router)
app.include_router(merchants.router)
app.include_router(merchants.orders_router)
app.include_router(drivers.router)
app.include_router(security.router)
app.include_router(webhooks.router)
app.include_router(ws.router)


@app.get("/health", tags=["meta"])
async def health():
    return {"status": "ok", "app": "Barboya"}
