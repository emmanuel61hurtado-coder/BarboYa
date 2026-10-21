# Despliegue de BarboYa en Coolify

Este documento describe el procedimiento para desplegar la plataforma **BarboYa** en un servidor propio utilizando **Coolify**.

## Prerrequisitos
- Servidor VPS (Ubuntu 22.04+) con Coolify instalado y configurado.
- Repositorio conectado a GitHub.
- Dominio configurado (ej. `api.barboya.co` y `app.barboya.co`).

## Pasos para el Despliegue

1. **Crear Nuevo Proyecto en Coolify:**
   - Seleccionar tipo **Docker Compose**.
   - Conectar con el repositorio Git de BarboYa y la rama `main`.
   - En la configuración del archivo Compose, indicar `docker-compose.backend.yml`.

2. **Configurar Variables de Entorno (.env):**
   - Definir las credenciales seguras de producción:
     - `DATABASE_URL=postgresql+asyncpg://barboya:<password-segura>@db:5432/barboya`
     - `REDIS_URL=redis://redis:6379/0`
     - `JWT_SECRET=<secreto-largo-y-aleatorio>`
     - `JWT_ISSUER=barboya-auth`
     - `JWT_AUDIENCE=barboya-api`

3. **Ejecutar Despliegue:**
   - Coolify clonará el repositorio, construirá la imagen Docker del backend y levantará los servicios (`db`, `redis`, `api`) utilizando `docker-compose.backend.yml`.

4. **Verificación (Health Check):**
   - Comprobar que el endpoint `/health` responda correctamente (`{"status": "ok", "app": "Barboya"}`).

## Solución de errores

### `Error: no service selected`

El archivo Compose de este repositorio define los servicios `db`, `redis` y `api`.
Este error también se ha reportado en Coolify cuando está activada la opción
**Consistent Container Names** en recursos conectados a Git. Desactiva esa opción
en la configuración del recurso y vuelve a desplegar. Si tu instalación de Coolify
es antigua, actualízala a una versión que incluya la corrección (el problema fue
resuelto en `v4.0.0-beta.240`).

Referencia: [coollabsio/coolify#1852](https://github.com/coollabsio/coolify/issues/1852).
