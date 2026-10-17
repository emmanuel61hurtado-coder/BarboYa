from fastapi import FastAPI
from app.core.config import settings, configure_logging

configure_logging()

app = FastAPI(
    title=settings.PROJECT_NAME,
    version="2.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url=f"{settings.API_V1_STR}/openapi.json"
)

@app.get("/")
async def root():
    return {"mensaje": "BarboYa Súper App API funcionando", "docs": "/docs"}

@app.get("/health")
async def health_check():
    return {"status": "ok", "service": "BarboYa Backend"}
