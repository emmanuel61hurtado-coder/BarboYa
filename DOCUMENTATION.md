# Documentación Técnica Completa - BarboYa

BarboYa es una plataforma web de domicilios (tipo DiDi) full-stack de alta disponibilidad, diseñada con una arquitectura moderna mobile-first (PWA), preparada para producción y optimizada para despliegue en Coolify mediante Docker Compose.

---

## 1. Arquitectura del Proyecto (Monorepo)

El proyecto está estructurado como un monorepo limpio y desacoplado:
```text
/BarboYa
├── backend/            # FastAPI, SQLAlchemy 2.0 Async, Pydantic v2, Alembic, WebSockets
├── frontend/           # React + Vite + TypeScript + Tailwind CSS
├── docker-compose.yml  # Orquestador (PostgreSQL 16 + Backend + Frontend)
├── .env.example        # Variables de entorno documentadas para Coolify
├── API_CONTRACT.md     # Contrato oficial de API y WebSockets
├── DOCUMENTATION.md    # Documentación técnica completa
└── README.md           # Guía rápida de inicio
```

---

## 2. Backend (FastAPI + SQLAlchemy Async + PostgreSQL 16)

### 2.1. Stack Tecnológico
- **Python 3.12** con tipado estricto.
- **FastAPI** para la API REST asíncrona.
- **SQLAlchemy 2.0 (Async / asyncpg)** para el ORM de base de datos.
- **Pydantic v2** para validación de datos y esquemas.
- **Alembic** para migraciones de base de datos.
- **Argon2-cffi** para hash seguro de contraseñas.
- **PyJWT** para autenticación basada en tokens JWT (Access token de 15 min + Refresh token en cookie HttpOnly).
- **Pytest + pytest-asyncio + httpx** para pruebas de integración y extremo a extremo.

### 2.2. Arquitectura de Capas (Estricta)
`Router` (HTTP y validación) → `Service` (Reglas de negocio) → `Repository` (Consultas SQLAlchemy).
- Inyección de dependencias mediante `Depends`.
- Excepciones de dominio personalizadas manejadas por un middleware global (`{"error": {"code", "message"}}`).
- Logs estructurados y `request-id` por petición.

### 2.3. Reglas de Negocio Críticas
1. **Máquina de Estados de Pedidos**: Transiciones controladas (`CREADO` → `ACEPTADO` → `PREPARANDO` → `LISTO` → `EN_CAMINO` → `ENTREGADO`, o `CANCELADO`). Las transiciones inválidas devuelven `409 Conflict`.
2. **Cálculo de Totales en Servidor**: El total se calcula siempre en el backend (precios actuales copiados como snapshot a `DetallePedido`, más costo de envío, menos cupón validado). Nunca se confía en montos enviados por el cliente.
3. **Asignación Concurrente de Repartidores**: Utiliza transacciones con bloqueo pesimista `SELECT ... FOR UPDATE SKIP LOCKED` para evitar que dos repartidores tomen el mismo pedido. Un repartidor solo puede tener una entrega activa.
4. **Seguridad RBAC y IDOR**: Decorador `@require_role(*roles)` y validación estricta de propiedad de recursos.

### 2.4. Tiempo Real (WebSockets)
- `/api/v1/ws/pedidos/{id}`: Notifica cambios de estado en tiempo real al cliente y comercio.
- `/api/v1/ws/tracking/{pedido_id}`: Transmite la ubicación GPS del repartidor al cliente.
- Implementado con un `ConnectionManager` en memoria, preparado para migrar a Redis Pub/Sub.

### 2.5. Endpoints de Salud
- `GET /health` — Healthcheck para Docker/Coolify (no requiere DB).
- `GET /ready` — Readiness check con verificación de conexión a base de datos.

---

## 3. Frontend (React + Vite + TypeScript + Tailwind CSS)

### 3.1. Stack Tecnológico
- **React 18** + **Vite** para desarrollo y compilación ultrarrápida.
- **TypeScript** con interfaces sincronizadas exactamente con los schemas Pydantic del backend.
- **Tailwind CSS** para diseño moderno, limpio y vibrante (Color de marca: `#FF6B00`).
- **Axios** con cliente HTTP configurado (`VITE_API_URL` y soporte de mocks para prototipado rápido).

### 3.2. Diseño y UX
- **Mobile-First**: Optimizado para dispositivos móviles con navegación fluida y tarjetas de alta calidad visual.
- **Modo Oscuro**: Con toggle prioritario para el panel de repartidores.
- **Seguimiento en Vivo**: Barra de progreso de estados y mapa integrado.

### 3.3. Nginx (Producción)
- El frontend se sirve mediante Nginx con configuración SPA (`try_files $uri /index.html`).
- Compresión gzip habilitada.
- Cache de assets estáticos con headers `immutable`.

---

## 4. Despliegue en Producción (Coolify & Docker Compose)

El sistema incluye Dockerfiles multi-stage optimizados sin usuario root y healthchecks integrados para PostgreSQL, FastAPI y Nginx.

### 4.1. Variables de Entorno de Producción

Configura estas variables en la pestaña **Environment Variables** de tu recurso en Coolify:

```env
# Base de datos
POSTGRES_USER=barboya
POSTGRES_PASSWORD=<contraseña_segura_aleatoria>
POSTGRES_DB=barboya_db

# Backend
DATABASE_URL=postgresql+asyncpg://barboya:<contraseña_segura>@postgres:5432/barboya_db
SECRET_KEY=<clave_aleatoria_minimo_32_chars>   # openssl rand -hex 32
ENVIRONMENT=production
ALLOWED_ORIGINS=["https://tudominio.com"]
ADMIN_EMAIL=admin@barboya.com
ADMIN_PASSWORD=<contraseña_admin_segura>

# Frontend (build-time)
VITE_API_URL=/api/v1
VITE_USE_MOCKS=false
```

> **⚠ IMPORTANTE**: En producción, `SECRET_KEY` debe ser una cadena segura y aleatoria de al menos 32 caracteres. Claves inseguras causan un error de arranque intencional.

### 4.2. Arranque con Docker Compose
```bash
docker compose up --build -d
```
*Las migraciones de Alembic y el script de seed (`app.scripts.seed`) se ejecutan automáticamente al arrancar el contenedor del backend.*

### 4.3. Configuración en Coolify

1. **Tipo de recurso**: Selecciona "Docker Compose".
2. **Repositorio**: Apunta a tu repositorio Git.
3. **Variables de entorno**: Copia las variables del `.env.example` en la pestaña de configuración.
4. **Dominios**: Configura los dominios para frontend (puerto 80) y backend (puerto 8000).
5. **Health Check**: Coolify usará automáticamente los healthchecks definidos en los Dockerfiles.

### 4.4. Estructura de Dockerfiles

| Servicio | Base | Multi-stage | Healthcheck | Usuario |
|----------|------|-------------|-------------|---------|
| Backend  | python:3.12-slim | ✅ Builder + Runtime | `curl /health` | appuser (UID 1000) |
| Frontend | node:20-alpine + nginx:stable-alpine | ✅ Builder + Serve | `wget /` | nginx (default) |
| PostgreSQL | postgres:16-alpine | N/A | `pg_isready` | postgres (default) |
