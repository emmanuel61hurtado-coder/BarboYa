from fastapi import FastAPI, Depends, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from app.core.config import settings
from app.core.exceptions import register_exception_handlers
from app.db.session import get_db
from app.routes.v1.routers import (
    auth, users, comercios, categorias, productos, direcciones,
    pedidos, pagos, entregas, calificaciones, vehiculos, cupones,
    notificaciones, admin, reportes
)
from app.routes.v1.ws import pedidos_ws, tracking_ws

app = FastAPI(
    title=settings.PROJECT_NAME,
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    docs_url=f"{settings.API_V1_STR}/docs",
)

register_exception_handlers(app)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include v1 routers
api_prefix = settings.API_V1_STR
app.include_router(auth.router, prefix=api_prefix)
app.include_router(users.router, prefix=api_prefix)
app.include_router(comercios.router, prefix=api_prefix)
app.include_router(categorias.router, prefix=api_prefix)
app.include_router(productos.router, prefix=api_prefix)
app.include_router(direcciones.router, prefix=api_prefix)
app.include_router(pedidos.router, prefix=api_prefix)
app.include_router(pagos.router, prefix=api_prefix)
app.include_router(entregas.router, prefix=api_prefix)
app.include_router(calificaciones.router, prefix=api_prefix)
app.include_router(vehiculos.router, prefix=api_prefix)
app.include_router(cupones.router, prefix=api_prefix)
app.include_router(notificaciones.router, prefix=api_prefix)
app.include_router(admin.router, prefix=api_prefix)
app.include_router(reportes.router, prefix=api_prefix)

# WebSockets
app.include_router(pedidos_ws.router, prefix=api_prefix + "/ws")
app.include_router(tracking_ws.router, prefix=api_prefix + "/ws")


@app.get("/health")
async def health_check():
    return {"status": "ok"}


@app.get("/ready")
async def readiness_check(db: AsyncSession = Depends(get_db)):
    try:
        await db.execute(select(1))
        return {"status": "ready", "database": "connected"}
    except Exception as e:
        return JSONResponse(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            content={"status": "not_ready", "error": str(e)},
        )
