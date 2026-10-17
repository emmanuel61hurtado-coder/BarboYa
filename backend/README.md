# BarboYa Backend API (v2.0.0)

API robusta basada en **FastAPI**, **SQLAlchemy (Async)**, **PostgreSQL** y **Redis**.

## Ejecución Local

1. Levantar servicios con Docker Compose:
   ```bash
   docker-compose up -d
   ```

2. Acceder a la documentación interactiva Swagger:
   - [http://localhost:8000/docs](http://localhost:8000/docs)

## Módulos Principales

- `/api/v1/auth`: Autenticación y registro de usuarios con roles (`client`, `provider`, `driver`, `admin`).
- `/api/v1/users`: Gestión de perfiles de usuario.
- `/api/v1/services`: Marketplace y catálogo de servicios profesionales y del hogar.
- `/api/v1/transport`: Cotización y solicitud de viajes (motos, autos, deliverys).
- `/api/v1/payments`: Billetera virtual BarboYa Pay y pasarela de pagos.
- `/api/v1/notifications`: Centro de mensajería y notificaciones push.
