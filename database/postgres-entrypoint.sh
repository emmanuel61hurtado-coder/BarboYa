#!/usr/bin/env sh
set -e

# Iniciar PostgreSQL oficial en segundo plano
/usr/local/bin/docker-entrypoint.sh postgres &
_pid=$!

echo "[BarboYa DB] Esperando a que PostgreSQL esté listo..."
until pg_isready -U "postgres" >/dev/null 2>&1; do
    sleep 1
done

echo "[BarboYa DB] Sincronizando usuario y contraseña para '${POSTGRES_USER:-barboya}'..."
psql -U postgres <<-EOSQL
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${POSTGRES_USER:-barboya}') THEN
            CREATE ROLE "${POSTGRES_USER:-barboya}" LOGIN PASSWORD '${POSTGRES_PASSWORD:-barboya}';
        ELSE
            ALTER USER "${POSTGRES_USER:-barboya}" WITH PASSWORD '${POSTGRES_PASSWORD:-barboya}';
        END IF;
    END
    \$\$;

    ALTER DATABASE "${POSTGRES_DB:-barboya}" OWNER TO "${POSTGRES_USER:-barboya}";
    GRANT ALL PRIVILEGES ON DATABASE "${POSTGRES_DB:-barboya}" TO "${POSTGRES_USER:-barboya}";
EOSQL

echo "[BarboYa DB] Contraseña y usuario sincronizados exitosamente."

# Mantener el proceso principal en primer plano
wait "$_pid"
