"""
BarboYa Pydantic Schemas Package.
"""
from app.schemas.auth import (
    Token,
    TokenData,
    LoginRequest,
    RegisterRequest,
    TokenResponse,
    ForgotPasswordRequest,
    ResetPasswordRequest,
)
from app.schemas.user import UserCreate, UserRead, UserUpdate
from app.schemas.comercio import ComercioCreate, ComercioRead, ComercioUpdate
from app.schemas.categoria import CategoriaCreate, CategoriaRead
from app.schemas.producto import ProductoCreate, ProductoRead, ProductoUpdate
from app.schemas.direccion import DireccionCreate, DireccionRead
from app.schemas.pedido import PedidoCreate, PedidoRead, PedidoEstadoUpdate
from app.schemas.detalle_pedido import DetallePedidoCreate, DetallePedidoRead
from app.schemas.pago import PagoCreate, PagoRead
from app.schemas.entrega import EntregaRead, UbicacionUpdate
from app.schemas.calificacion import CalificacionCreate, CalificacionRead
from app.schemas.vehiculo import VehiculoCreate, VehiculoRead
from app.schemas.cupon import CuponCreate, CuponRead, CuponValidarRequest
from app.schemas.notificacion import NotificacionRead

__all__ = [
    "Token",
    "TokenData",
    "LoginRequest",
    "RegisterRequest",
    "TokenResponse",
    "ForgotPasswordRequest",
    "ResetPasswordRequest",
    "UserCreate",
    "UserRead",
    "UserUpdate",
    "ComercioCreate",
    "ComercioRead",
    "ComercioUpdate",
    "CategoriaCreate",
    "CategoriaRead",
    "ProductoCreate",
    "ProductoRead",
    "ProductoUpdate",
    "DireccionCreate",
    "DireccionRead",
    "PedidoCreate",
    "PedidoRead",
    "PedidoEstadoUpdate",
    "DetallePedidoCreate",
    "DetallePedidoRead",
    "PagoCreate",
    "PagoRead",
    "EntregaRead",
    "UbicacionUpdate",
    "CalificacionCreate",
    "CalificacionRead",
    "VehiculoCreate",
    "VehiculoRead",
    "CuponCreate",
    "CuponRead",
    "NotificacionRead",
]
