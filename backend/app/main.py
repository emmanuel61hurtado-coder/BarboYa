import asyncio
from contextlib import asynccontextmanager
from pathlib import Path

import redis.asyncio as aioredis
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse
from fastapi.staticfiles import StaticFiles
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


async def _bootstrap_database(attempts: int = 30, delay: float = 10.0) -> None:
    """Sincroniza el esquema con la base de datos reintentando.

    En producción el contenedor de PostgreSQL puede tardar en estar listo
    (o reiniciarse); la API arranca igual y reintenta en segundo plano.
    """
    from app.core.database import init_db
    from app.core.demo_data import seed_demo_customer

    for attempt in range(1, attempts + 1):
        try:
            await init_db()
            await seed_demo_customer()
            print("[BarboYa] Base de datos lista y esquema sincronizado")
            return
        except asyncio.CancelledError:
            raise
        except Exception as err:
            print(f"[BarboYa] Base de datos aun no disponible (intento {attempt}/{attempts}): {err}")
            await asyncio.sleep(delay)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Create tables / synchronize the schema with the SQLAlchemy models
    schema_task = asyncio.create_task(_bootstrap_database())

    client = None
    try:
        client = aioredis.from_url(settings.redis_url)
        await manager.attach_redis(client)
    except Exception as err:
        print(f"[BarboYa] Redis no disponible ({err}). Continuando servidor FastAPI en memoria...")
    yield
    schema_task.cancel()
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
FRONTEND_DIST = Path(__file__).resolve().parent.parent.parent / "frontend" / "dist"

if FRONTEND_DIST.exists():
    assets_dir = FRONTEND_DIST / "assets"
    if assets_dir.exists():
        app.mount("/assets", StaticFiles(directory=str(assets_dir)), name="assets")

    @app.get("/{full_path:path}", include_in_schema=False)
    async def serve_spa(full_path: str, request: Request):
        # Serve files from the build directory only; never allow paths outside it.
        dist_dir = FRONTEND_DIST.resolve()
        file_path = (dist_dir / full_path).resolve()
        if file_path.is_relative_to(dist_dir) and file_path.is_file():
            return FileResponse(file_path)

        # Only browser document requests should receive the SPA fallback.
        if "text/html" not in request.headers.get("accept", ""):
            return JSONResponse({"detail": "Not Found"}, status_code=404)

        index_file = FRONTEND_DIST / "index.html"
        if index_file.is_file():
            return FileResponse(index_file)
        return JSONResponse({"detail": "Not Found"}, status_code=404)
