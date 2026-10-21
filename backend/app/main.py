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
        response.headers["Content-Security-Policy"] = (
            "default-src 'self' 'unsafe-inline' 'unsafe-eval' data: blob: https:; "
            "connect-src 'self' ws: wss: http: https:; "
            "img-src 'self' data: blob: https:; "
            "font-src 'self' data: https:;"
        )
        return response


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Create tables if using SQLite dev mode
    from app.core.database import init_db
    await init_db()

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


# ───────────────────── SERVIR FRONTEND SPA CON FASTAPI ─────────────────────
from pathlib import Path
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse

FRONTEND_DIST = Path(__file__).resolve().parent.parent.parent / "frontend" / "dist"

if FRONTEND_DIST.exists():
    assets_dir = FRONTEND_DIST / "assets"
    if assets_dir.exists():
        app.mount("/assets", StaticFiles(directory=str(assets_dir)), name="assets")

    @app.get("/{full_path:path}", include_in_schema=False)
    async def serve_spa(full_path: str):
        # Permitir rutas de archivos estáticos directos (como favicon, vite.svg, etc)
        file_path = FRONTEND_DIST / full_path
        if file_path.is_file():
            return FileResponse(file_path)
        # Fallback a index.html para soportar navegación SPA
        index_file = FRONTEND_DIST / "index.html"
        if index_file.is_file():
            return FileResponse(index_file)
        return {"status": "ok", "app": "Barboya"}

