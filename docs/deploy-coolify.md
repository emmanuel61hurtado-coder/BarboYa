# Despliegue de BarboYa en Coolify

Procedimiento para desplegar **BarboYa** (FastAPI + React en un solo contenedor,
PostgreSQL/PostGIS y Redis) en un servidor propio con **Coolify**.

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
En **Environment Variables** define (marcadas como *Build Variable* cuando Coolify lo pida):

| Variable | Obligatoria | Ejemplo |
|---|---|---|
| `JWT_SECRET` | **Sí** | cadena aleatoria de 64+ caracteres |
| `POSTGRES_PASSWORD` | Sí (prod) | `UnaClaveLargaYUnica` |
| `DATABASE_URL` | Recomendada | `postgresql+asyncpg://barboya:UnaClaveLargaYUnica@db:5432/barboya` |
| `REDIS_URL` | No | `redis://redis:6379/0` |
| `CORS_ORIGINS` | Sí | `["https://app.barboya.co"]` |
| `ENVIRONMENT` | No | `production` |

`docker-compose.yml` ya fija `USE_SQLITE=false` y usa PostgreSQL/PostGIS.
`JWT_SECRET` no tiene valor por defecto: si falta, el despliegue se detiene con
un mensaje explícito en lugar de arrancar con un secreto inseguro.

## 3. Desplegar
Coolify clona el repo, construye `backend/Dockerfile` (compila React y copia
`dist/` dentro de la imagen) y levanta `db`, `redis` y `api`.
La API sirve la SPA de React en `/` y los endpoints en `/api/*`, `/auth/*`, etc.

## 4. Verificación
- `https://<dominio>/health` → `{"status":"ok","app":"Barboya"}`
- `https://<dominio>/` → interfaz React.
- Logs del contenedor `api` sin errores de `UndefinedColumn`.

## 5. Volumen de base de datos ya existente
`database/schema.sql` solo se ejecuta cuando el volumen `pgdata` está vacío.
Si la base ya existía con un esquema anterior, aplica la migración idempotente:

```bash
docker compose exec -T db psql -U barboya -d barboya \
  < database/migrations/002_sync_models.sql
```

O desde Coolify: **Terminal** del servicio `db` → pegar el contenido del archivo.

## Solución de errores

### `no service selected`
Este mensaje lo emite **Docker Compose** (`runUp`) cuando el proyecto YAML que
leyó tiene **cero servicios**. En Coolify casi siempre significa que el recurso
guardó una copia vacía/desactualizada del compose. Verifica en orden:

1. **Compose Location** correcto (`/docker-compose.yml`) y pulsa
   **Load Compose File**. Deben listarse `db`, `redis`, `api`.
2. Que el recurso sea de tipo **Docker Compose**, no *Application → Dockerfile*.
3. Revisa que el archivo no tenga **BOM UTF-8** (Coolify/YAML lo interpreta mal).
   En Windows: guarda como *UTF-8 sin BOM* (`Format-Hex` debe iniciar en
   `73 65 72 76 69 63 65 73 3A` = `services:`).
4. Si el recurso quedó corrupto, bórralo y créalo de nuevo apuntando a
   `/docker-compose.yml`; la copia en la base de datos de Coolify se regenera.
5. No uses *profiles* ni *anchors/alias* YAML: el parser de Coolify no los
   soporta y el servicio queda fuera de la selección.

### `Deployment failed ... exit code 1` después de arrancar
- `JWT_SECRET` ausente → el compose falla al interpolar (`JWT_SECRET requerido`).
- `UndefinedColumn` en `/auth/register` o `/auth/login` → esquema viejo; aplica
  la migración del punto 5.
- `db` no saludable → revisa `POSTGRES_PASSWORD`: si lo cambias debes cambiar
  también `DATABASE_URL` (o deja ambos con el valor por defecto del compose).
