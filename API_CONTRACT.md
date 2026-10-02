# BarboYa API Contract & Reference

**Base URL:** `/api/v1`  
**Authentication:** Bearer token (`Authorization: Bearer <access_token>`) or HttpOnly Cookie (`refresh_token`).

---

## 👥 Roles y Permisos (RBAC)
- `CLIENTE`: Realiza pedidos, administra direcciones, paga y califica.
- `REPARTIDOR`: Requiere aprobación del admin. Acepta entregas (`LISTO`), actualiza ubicación y estado de entrega.
- `COMERCIO`: Requiere aprobación del admin. Gestiona su perfil, horarios, productos y pedidos entrantes.
- `ADMIN`: Control total (aprobar/bloquear usuarios, cupones, tarifas, reportes).

---

## 🔐 1. Auth & Usuarios (`/auth`, `/users`)

### `POST /api/v1/auth/register`
- **Descripción:** Registro de nuevo usuario.
- **Roles permitidos:** Público (`CLIENTE`, `REPARTIDOR`, `COMERCIO`).
- **Body Request:**
  ```json
  {
    "email": "cliente@barboya.com",
    "password": "Password123*",
    "nombre": "Juan Pérez",
    "telefono": "3001234567",
    "rol": "CLIENTE"
  }
  ```
- **Respuesta (201 Created):** Objeto `UserRead`.

### `POST /api/v1/auth/login`
- **Descripción:** Iniciar sesión.
- **Body Request:**
  ```json
  {
    "email": "cliente@barboya.com",
    "password": "Password123*"
  }
  ```
- **Respuesta (200 OK):**
  ```json
  {
    "access_token": "eyJhbGci...",
    "token_type": "bearer"
  }
  ```
  *(Establece cookie HttpOnly `refresh_token`)*

### `POST /api/v1/auth/logout`
- **Respuesta (200 OK):** `{ "message": "Sesión cerrada exitosamente" }`

### `GET /api/v1/users/me`
- **Autenticación:** Requerida (Cualquier rol).
- **Respuesta (200 OK):** Objeto `UserRead`.

---

## 🍔 2. Comercios, Categorías & Productos (`/comercios`, `/categorias`, `/productos`, `/direcciones`)

### `GET /api/v1/comercios`
- **Respuesta (200 OK):** Lista de comercios activos.

### `GET /api/v1/comercios/{id}`
- **Respuesta (200 OK):** Detalle del comercio con su lista de productos.

### `GET /api/v1/categorias`
- **Respuesta (200 OK):** Lista de categorías de comida.

### `POST /api/v1/comercios/me/productos`
- **Autenticación:** Rol `COMERCIO`.
- **Body Request:**
  ```json
  {
    "nombre": "Hamburguesa Doble",
    "descripcion": "Doble carne angus y queso",
    "precio": 28000.00,
    "disponible": true,
    "categoria_id": "uuid-cat"
  }
  ```

---

## 📦 3. Pedidos, Pagos & Cupones (`/pedidos`, `/pagos`, `/cupones`)

### `POST /api/v1/pedidos`
- **Autenticación:** Rol `CLIENTE`.
- **Descripción:** El servidor calcula el subtotal, costo de envío y total (nunca confía en cliente).
- **Body Request:**
  ```json
  {
    "comercio_id": "uuid-comercio",
    "direccion_id": "uuid-direccion",
    "metodo_pago": "TARJETA",
    "cupon_codigo": "BARBOYA20",
    "detalles": [
      {
        "producto_id": "uuid-prod",
        "cantidad": 2
      }
    ]
  }
  ```

### `GET /api/v1/pedidos`
- **Descripción:** Lista pedidos según rol (cliente ve los suyos, comercio los de su local, repartidor sus entregas).

### `PATCH /api/v1/pedidos/{id}/estado`
- **Descripción:** Transición de estado sujeta a la máquina de estados estricta (`CREADO` → `ACEPTADO` → `PREPARANDO` → `LISTO` → `EN_CAMINO` → `ENTREGADO`).

---

## 🛵 4. Entregas & Repartidores (`/entregas`, `/vehiculos`)

### `GET /api/v1/entregas/disponibles`
- **Autenticación:** Rol `REPARTIDOR` (activo y aprobado).
- **Respuesta:** Lista de pedidos con estado `LISTO` sin repartidor asignado.

### `POST /api/v1/entregas/{pedido_id}/aceptar`
- **Descripción:** Asigna pedido usando transacción con bloqueo (`SELECT FOR UPDATE SKIP LOCKED`).

---

## ⚡ 5. WebSockets

- **`/api/v1/ws/pedidos/{id}?token=...`**: Emite eventos en tiempo real cuando cambia el estado del pedido.
- **`/api/v1/ws/tracking/{pedido_id}?token=...`**: Canal de ubicación GPS en tiempo real entre Repartidor y Cliente.
