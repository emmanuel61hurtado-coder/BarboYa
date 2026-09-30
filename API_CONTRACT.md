# BarboYa API Contract

Base URL: `/api/v1`
Authentication: Bearer token (`Authorization: Bearer <access_token>`) or HttpOnly cookie for refresh token.

## Roles
- `CLIENTE`
- `REPARTIDOR` (Pendiente de aprobación por admin)
- `COMERCIO` (Pendiente de aprobación por admin)
- `ADMIN`

---

## 1. Auth & Users (`/auth`, `/users`, `/admin`)
- `POST /api/v1/auth/register`: Registro de usuario (`email`, `password`, `nombre`, `telefono`, `rol`).
- `POST /api/v1/auth/login`: Login (`email`, `password`) -> Access token + Refresh cookie.
- `POST /api/v1/auth/refresh`: Renueva access token con cookie HttpOnly.
- `POST /api/v1/auth/logout`: Revoca sesión.
- `POST /api/v1/auth/forgot-password`: Solicita recuperación de contraseña.
- `POST /api/v1/auth/reset-password`: Restablece contraseña con token.
- `GET /api/v1/users/me`: Perfil del usuario actual.
- `PATCH /api/v1/users/me`: Actualiza perfil del usuario actual.

## 2. Comercios & Categorías & Productos (`/comercios`, `/categorias`, `/productos`, `/direcciones`)
- `GET /api/v1/categorias`: Lista categorías.
- `GET /api/v1/comercios`: Lista comercios activos (con filtros).
- `GET /api/v1/comercios/{id}`: Detalle de comercio con menú y productos.
- `POST /api/v1/comercios`: Crear comercio (Rol COMERCIO).
- `GET /api/v1/comercios/me/productos`: Productos del comercio actual.
- `POST /api/v1/comercios/me/productos`: Crear producto.
- `PATCH /api/v1/comercios/me/productos/{id}`: Actualizar producto.
- `GET /api/v1/direcciones`: Direcciones del cliente.
- `POST /api/v1/direcciones`: Crear dirección.

## 3. Pedidos, Pagos & Cupones (`/pedidos`, `/pagos`, `/cupones`)
- `POST /api/v1/pedidos`: Crear pedido (Rol CLIENTE). Servidor calcula total, aplica cupón, valida stock y horario.
- `GET /api/v1/pedidos`: Listar pedidos del usuario (filtrado por rol).
- `GET /api/v1/pedidos/{id}`: Detalle de pedido.
- `PATCH /api/v1/pedidos/{id}/estado`: Cambiar estado (Transición controlada por máquina de estados).
- `POST /api/v1/pagos`: Procesar pago con Idempotency-Key.
- `POST /api/v1/cupones/validar`: Validar cupón.

## 4. Repartidores & Entregas (`/repartidores`, `/entregas`)
- `GET /api/v1/entregas/disponibles`: Pedidos en estado `LISTO` para repartidores en línea.
- `POST /api/v1/entregas/{pedido_id}/aceptar`: Aceptar pedido (SELECT FOR UPDATE SKIP LOCKED).
- `PATCH /api/v1/entregas/{id}/estado`: Actualizar estado de entrega.

## 5. Calificaciones & Notificaciones (`/calificaciones`, `/notificaciones`)
- `POST /api/v1/calificaciones`: Calificar pedido entregado (1-5).
- `GET /api/v1/notificaciones`: Notificaciones del usuario.

## 6. Admin & Reportes (`/admin`, `/reportes`)
- `GET /api/v1/admin/usuarios`: Listar usuarios.
- `PATCH /api/v1/admin/usuarios/{id}/aprobar`: Aprobar comercio/repartidor.
- `PATCH /api/v1/admin/usuarios/{id}/bloquear`: Bloquear usuario.
- `GET /api/v1/admin/reportes/ventas`: Reporte de ventas.

---

## WebSockets
- `/api/v1/ws/pedidos/{id}`: Cambios de estado del pedido (Cliente y Comercio).
- `/api/v1/ws/tracking/{pedido_id}`: Ubicación en tiempo real del repartidor (Repartidor emite, Cliente recibe).
