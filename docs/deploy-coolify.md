# Despliegue de BarboYa en Coolify

Procedimiento para desplegar **BarboYa** (FastAPI + React en un solo contenedor,
PostgreSQL/PostGIS y Redis) en un servidor propio con **Coolify**.

## Arquitectura del despliegue

```
Coolify  ->  docker compose  ->  db      (postgis/postgis:16-3.4, volumen pgdata)
                                  redis   (redis:7-alpine, volumen redisdata)
                                  api     (backend/Dockerfile: React + FastAPI, puerto 8000)
```

- El contenedor `api` compila el frontend React y lo sirve desde FastAPI (`/` = SPA, `/api/*` = endpoints).
- La API sincroniza el esquema con los modelos SQLAlchemy al arrancar
  (`backend/app/core/schema_sync.py`): crea tipos ENUM, tablas y columnas que falten,
  y siembra las zonas de Barbosa. Por eso el despliegue **no depende** de que
  `database/schema.sql` se aplique sin errores.
- `database/init/00-init.sh` aplica `schema.sql` de forma **tolerante**: si un
  statement falla, la base de datos igual levanta y la API termina de alinear el esquema.

## Prerrequisitos
- VPS (Ubuntu 22.04+) con Coolify instalado y actualizado.
- Repositorio conectado a GitHub.
- Dominio configurado (ej. `app.barboya.co`).

## 1. Crear el recurso
1. En Coolify: **+ New** → **Application** → **Docker Compose** (no "Dockerfile").
2. Selecciona el repositorio y la rama `main`.
3. En **General → Docker Compose**:
   - **Docker Compose Location**: `/docker-compose.yml`
     (`docker-compose.backend.yml` es un alias idéntico; usa solo uno).
   - Pulsa **Load Compose File** / **Reload** y verifica que aparezcan los
     servicios **db**, **redis** y **api** con sus toggles activos.
   - **Base Directory**: `/`.
4. No actives **Consistent Container Names** para este recurso.

## 2. Variables de entorno
En **Environment Variables** define:

| Variable | Obligatoria | Ejemplo |
|---|---|---|
| `JWT_SECRET` | **Sí** | cadena aleatoria de 64+ caracteres |
| `POSTGRES_PASSWORD` | Sí (prod) | `UnaClaveLargaYUnica` |
| `DATABASE_URL` | Opcional | `postgresql+asyncpg://barboya:UnaClaveLargaYUnica@db:5432/barboya` |
| `POSTGRES_USER` / `POSTGRES_DB` | Opcional | `barboya` (por defecto) |
| `REDIS_URL` | Opcional | `redis://redis:6379/0` |
| `CORS_ORIGINS` | Sí | `["https://app.barboya.co"]` |
| `ENVIRONMENT` | Opcional | `production` |
| `API_PORT` | Opcional | `8000` |

- Si **no** defines `DATABASE_URL`, la API la arma con `POSTGRES_USER`,
  `POSTGRES_PASSWORD`, `POSTGRES_HOST` y `POSTGRES_DB` (ver `backend/app/core/config.py`),
  así que basta con definir `POSTGRES_PASSWORD`.
- `docker-compose.yml` ya fija `USE_SQLITE=false` (PostgreSQL + PostGIS).
- `JWT_SECRET` no tiene valor por defecto: si falta, el despliegue se detiene con
  un mensaje explícito en lugar de arrancar con un secreto inseguro.

## 3. Desplegar
Coolify clona el repo, construye `backend/Dockerfile` (compila React y copia
`dist/` dentro de la imagen) y levanta `db`, `redis` y `api`.

### Si el despliegue anterior falló dejando un volumen `pgdata` a medias
Borra el volumen para empezar limpio (por consola SSH del servidor):

```bash
docker volume ls | grep pgdata
docker volume rm <proyecto>_pgdata      # ej. zwhptn6phuccflyg7q157eta_pgdata
```

Vuelve a desplegar: la base se inicializa y la API alinea el esquema.

## 4. Verificación
- `https://<dominio>/health` → `{"status":"ok","app":"Barboya"}`
- `https://<dominio>/` → interfaz React.
- Logs del contenedor `api`: debe aparecer `[BarboYa DB] Esquema sincronizado con los modelos`.
- `POST /auth/register` → 201 (si da 500, revisa el esquema: `docker compose logs api`).

## Solución de errores

### `no service selected`
Este mensaje lo emite **Docker Compose** (`runUp`) cuando el proyecto YAML que
leyó tiene **cero servicios**. En Coolify casi siempre significa que el recurso
guardó una copia vacía/desactualizada del compose. Verifica en orden:

1. **Compose Location** correcto (`/docker-compose.yml`) y pulsa
   **Load Compose File**. Deben listarse `db`, `redis`, `api`.
2. Que el recurso sea de tipo **Docker Compose**, no *Application → Dockerfile*.
3. Revisa que el archivo no tenga **BOM UTF-8** (Coolify/YAML lo interpreta mal).
   En Windows: guardar como *UTF-8 sin BOM* (los bytes deben iniciar en
   `73 65 72 76 69 63 65 73 3A` = `services:`).
4. Si el recurso quedó corrupto, bórralo y créalo de nuevo apuntando a
   `/docker-compose.yml`; la copia en la base de datos de Coolify se regenera.
5. No uses *profiles* ni *anchors/alias* YAML: el parser de Coolify no los
   soporta y el servicio queda fuera de la selección.

### `dependency failed to start: container db ... is unhealthy`
El contenedor de PostgreSQL no pasó el healthcheck (`pg_isready`).

1. Revisa `docker compose logs db` (en Coolify: pestaña **Logs** del servicio `db`).
   El script `database/init/00-init.sh` deja el error de `schema.sql` en el log.
2. El script de init es tolerante: aunque `schema.sql` falle, la base levanta y
   la API crea lo que falte. Si aun así el contenedor muere, normalmente es un
   `PGDATA` a medias → borra el volumen `pgdata` y vuelve a desplegar.
3. Verifica que `POSTGRES_PASSWORD` sea el mismo que usaste al crear el volumen
   (si cambias la clave con el volumen ya inicializado, PostgreSQL conserva la
   clave original y la API no puede conectar).

### `Deployment failed ... exit code 1` durante el arranque de la API
- `JWT_SECRET` ausente → el compose falla al interpolar (`JWT_SECRET requerido`).
- `UndefinedColumn` en `/auth/register` → esquema viejo: borra el volumen o
  revisa el log `[BarboYa DB] Esquema sincronizado con los modelos`.
- La API siempre arranca aunque la base no esté lista: revisa los logs y reintenta
  el despliegue cuando la base esté sana.
