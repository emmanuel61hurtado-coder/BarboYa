"""
BarboYa v1 API Routes.
"""
from app.routes.v1.routers import (
    admin,
    auth,
    calificaciones,
    categorias,
    comercios,
    cupones,
    direcciones,
    entregas,
    notificaciones,
    pagos,
    pedidos,
    productos,
    reportes,
    users,
    vehiculos,
)

__all__ = [
    "admin",
    "auth",
    "calificaciones",
    "categorias",
    "comercios",
    "cupones",
    "direcciones",
    "entregas",
    "notificaciones",
    "pagos",
    "pedidos",
    "productos",
    "reportes",
    "users",
    "vehiculos",
]
