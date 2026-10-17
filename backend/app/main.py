from app.api.auth import router as auth_router
from app.api.notifications import router as notifications_router
from app.api.payments import router as payments_router
from app.api.services import router as services_router
from app.api.transport import router as transport_router
from app.api.users import router as users_router
from app.core.config import configure_logging, settings
from fastapi import FastAPI

configure_logging()

app = FastAPI(
    title=settings.PROJECT_NAME,
    version="2.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url=f"{settings.API_V1_STR}/openapi.json"
)

app.include_router(auth_router, prefix=settings.API_V1_STR)
app.include_router(users_router, prefix=settings.API_V1_STR)
app.include_router(services_router, prefix=settings.API_V1_STR)
app.include_router(transport_router, prefix=settings.API_V1_STR)
app.include_router(notifications_router, prefix=settings.API_V1_STR)
app.include_router(payments_router, prefix=settings.API_V1_STR)

@app.get("/")
async def root():
    return {"mensaje": "BarboYa Súper App API funcionando", "docs": "/docs"}

@app.get("/health")
async def health_check():
    return {"status": "ok", "service": "BarboYa Backend"}
