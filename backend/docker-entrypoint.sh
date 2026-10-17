#!/bin/sh
set -e

# Run database migrations if requested (useful for automated Coolify deployments)
if [ "$RUN_MIGRATIONS" = "true" ] || [ "$RUN_MIGRATIONS" = "1" ]; then
    echo "[entrypoint] Applying database migrations via Alembic..."
    alembic upgrade head
    echo "[entrypoint] Migrations applied successfully."
fi

exec "$@"
