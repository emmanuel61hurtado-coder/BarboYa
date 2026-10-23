#!/usr/bin/env sh
# =====================================================================
# BARBOYA - inicializacion tolerante del esquema PostgreSQL
#
# Docker ejecuta este script durante el initdb del contenedor "db".
# A diferencia de un .sql montado directamente (que aborta el arranque
# ante cualquier error), aqui los errores de schema.sql NO impiden que
# la base de datos levante: la API sincroniza el esquema con los
# modelos SQLAlchemy al arrancar (app/core/schema_sync.py).
# =====================================================================

echo "[BarboYa] Aplicando /opt/barboya/schema.sql (modo tolerante)..."

if psql -v ON_ERROR_STOP=1 -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -f /opt/barboya/schema.sql; then
    echo "[BarboYa] schema.sql aplicado sin errores."
else
    echo "[BarboYa] AVISO: schema.sql devolvio errores. Revisa los logs del contenedor 'db'."
    echo "[BarboYa] La API sincronizara el esquema con los modelos al arrancar."
fi

true
