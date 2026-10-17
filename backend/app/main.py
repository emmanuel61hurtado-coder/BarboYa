from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.auth import router as auth_router
from app.api.notifications import router as notifications_router
from app.api.payments import router as payments_router
from app.api.services import router as services_router
from app.api.transport import router as transport_router
from app.api.users import router as users_router
from app.core.config import configure_logging, settings

configure_logging()

docs_enabled = settings.ENVIRONMENT != "production" or settings.ENABLE_DOCS

app = FastAPI(
    title=settings.PROJECT_NAME,
    version="2.0.0",
    docs_url="/docs" if docs_enabled else None,
    redoc_url="/redoc" if docs_enabled else None,
    openapi_url=f"{settings.API_V1_STR}/openapi.json" if docs_enabled else None,
)

allow_credentials = "*" not in settings.ALLOWED_ORIGINS
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS,
    allow_credentials=allow_credentials,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth_router, prefix=settings.API_V1_STR)
app.include_router(users_router, prefix=settings.API_V1_STR)
app.include_router(services_router, prefix=settings.API_V1_STR)
app.include_router(transport_router, prefix=settings.API_V1_STR)
app.include_router(notifications_router, prefix=settings.API_V1_STR)
app.include_router(payments_router, prefix=settings.API_V1_STR)

@app.get("/")
async def root():
    return {
        "mensaje": "BarboYa Súper App API funcionando",
        "docs": "/docs" if docs_enabled else "disabled in production",
        "environment": settings.ENVIRONMENT,
    }

@app.get("/health")
async def health_check():
    return {"status": "ok", "service": "BarboYa Backend"}

