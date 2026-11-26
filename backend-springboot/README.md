# BarboYa API (Spring Boot)

Backend en Java 21 y Spring Boot 3.3 que reemplazará al de FastAPI. Hoy cubre **identidad y administración de cuentas**; los demás módulos siguen en `../backend`.

**No reemplaza todavía al backend publicado.** El frontend lo usa solo en el modo `v1` (ver [Interfaz](#interfaz)).

## Arrancar en desarrollo

Requisitos: JDK 21, Maven y PostgreSQL 16.

```bash
# Una sola vez: usuario y base propios (no compartas la base de FastAPI)
psql -U postgres -c "CREATE ROLE barboya LOGIN PASSWORD 'elige-una-clave'"
psql -U postgres -c "CREATE DATABASE barboya_core OWNER barboya"

export SPRING_PROFILES_ACTIVE=dev
export DB_PASSWORD='elige-una-clave'
mvn spring-boot:run
```

- API: `http://localhost:8081`
- Documentación interactiva (solo perfil `dev`): `http://localhost:8081/swagger-ui.html`
- Salud: `http://localhost:8081/actuator/health`

Flyway crea las tablas al arrancar. En PowerShell las variables se definen con `$env:DB_PASSWORD = '...'`.

## Interfaz

Con la API en marcha (perfil `dev`), en otra terminal (requiere Node 20 o superior):

```bash
cd ../frontend
npm ci
npm run dev:v1        # http://localhost:3000
```

Vite reenvía `/api/v1` a `http://localhost:8081` (otro destino: variable `BARBOYA_API_V1`). Incluye registro, ingreso, recuperación de contraseña, seguridad de la cuenta y administración.

- Para entrar a la administración, arranca la API la primera vez con las variables `BOOTSTRAP_ADMIN_*`.
- Para los códigos por correo, configura las variables SMTP.
- `npm run dev` y `npm run build` siguen generando la aplicación actual, conectada a FastAPI.
- `npm run build:v1` compila esta interfaz en `dist-v1/`. El servidor que la publique debe devolver `index.html` en cualquier ruta y enviar `/api/v1` a esta API desde el mismo sitio.

## Pruebas

```bash
mvn test
```

En `frontend/`: `npm test`.

Levantan un PostgreSQL 16 propio y temporal; no necesitan Docker ni tocan tu base. La primera ejecución descarga sus binarios.

## Variables de entorno

| Variable | Obligatoria | Uso |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Sí | `dev` o `prod` |
| `DB_URL` | En `prod` | Por defecto `jdbc:postgresql://localhost:5432/barboya_core` |
| `DB_USER` / `DB_PASSWORD` | Sí (`DB_USER` por defecto `barboya` fuera de `prod`) | Credenciales de PostgreSQL |
| `JWT_SECRET` | Sí, salvo en `dev` | 32 caracteres o más. En `dev`, si falta, se genera uno temporal |
| `CORS_ORIGINS` | Si el frontend usa otro dominio | Lista separada por comas; no admite `*` |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `MAIL_FROM` | Para correo | Sin ellas, verificación y recuperación responden `503` |
| `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PHONE`, `BOOTSTRAP_ADMIN_PASSWORD` | Primer arranque | Crea el primer superadministrador (clave de 12+ caracteres). Elimina la clave después |
| `PORT` | No | `8081` |

## Rutas

Base: `/api/v1`. Los errores usan `application/problem+json` con un campo `code`.

| Método y ruta | Acceso |
|---|---|
| `POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout` | Público |
| `POST /auth/password/forgot`, `/auth/password/reset` | Público |
| `POST /auth/email/verification`, `/auth/email/verify` | Sesión |
| `GET /auth/sessions`, `DELETE /auth/sessions`, `DELETE /auth/sessions/{id}` | Sesión propia |
| `GET /auth/history` | Sesión propia |
| `GET /users/me` | Sesión |
| `GET /admin/users`, `GET /admin/audit`, `PATCH /admin/users/{id}/status` | `ADMIN`, `SUPERADMIN` |
| `POST /admin/staff` | `SUPERADMIN` |

Toda ruta no declarada queda cerrada.

## Sesiones

- El **token de acceso** (JWT, 15 minutos) va en `Authorization: Bearer`. Guárdalo en memoria, no en `localStorage`.
- El **token de refresco** viaja en una cookie `HttpOnly`, `Secure` y `SameSite=Strict`, por lo que frontend y API deben compartir sitio. Dura como máximo 30 días desde el ingreso.
- Cada token de refresco **sirve una sola vez**. Reutilizarlo cierra la sesión, así que el cliente debe evitar dos renovaciones simultáneas (por ejemplo, entre pestañas).
- Excepción: si la respuesta de una renovación se pierde (recarga o corte de red), el mismo token puede repetirse durante 20 segundos mientras su reemplazo no se haya usado.
- Cerrar una sesión o suspender una cuenta surte efecto de inmediato.

## Roles

`CUSTOMER`, `COURIER`, `MOTO_DRIVER`, `CAR_DRIVER`, `RESTAURANT`, `STORE`, `ADMIN`, `SUPERADMIN`. Quien presta un servicio queda en `PENDING_VERIFICATION` hasta que un administrador lo active. Los módulos de operación deberán exigir el estado `ACTIVE`.

## Esqueleto anterior

`src/main/java/com/barboya/barboya` se compila pero **no se carga**: sus rutas no comprobaban permisos. Sirve de referencia mientras se reescribe cada módulo.

## Pendiente

- Autenticación de múltiples factores.
- Verificación del celular por SMS o WhatsApp (requiere proveedor).
- Verificación documental y facial de quienes prestan servicios.
- Límite de peticiones compartido entre instancias (hoy es en memoria).
- Migración de las cuentas existentes en FastAPI.
- Imagen Docker y despliegue.
