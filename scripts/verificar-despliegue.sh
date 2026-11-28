#!/bin/sh
# Comprueba un despliegue de la version nueva sin crear ni modificar datos.
#
#   sh scripts/verificar-despliegue.sh https://pruebas.tudominio.co
#
# El segundo argumento simula la peticion que reenvia un proxy HTTPS a ese dominio
# (para probar contra el contenedor directamente).
BASE=${1:?Uso: verificar-despliegue.sh URL [dominio-simulado]}
BASE=${BASE%/}
DOMAIN=$2
if [ -n "$DOMAIN" ]; then
    ORIGIN="https://$DOMAIN"
else
    ORIGIN=$BASE
fi
failures=0

request() {
    if [ -n "$DOMAIN" ]; then
        curl -s -m 15 -H "Host: $DOMAIN" -H "X-Forwarded-Proto: https" "$@"
    else
        curl -s -m 15 "$@"
    fi
}

status() { request -o /dev/null -w '%{http_code}' "$@"; }

check() {
    if [ "$2" = "$3" ]; then
        echo "  ok     $1"
    else
        echo "  FALLA  $1 (esperado $2, recibido $3)"
        failures=$((failures + 1))
    fi
}

login() {
    status -X POST -H "Origin: $1" -H 'Content-Type: application/json' \
        -d '{"identifier":"comprobacion@barboya.invalid","password":"no-existe"}' "$BASE/api/v1/auth/login"
}

echo "Comprobando $BASE (origen $ORIGIN)"
check "salud" 200 "$(status "$BASE/actuator/health")"
check "la interfaz carga" 1 "$(request "$BASE/" | grep -c 'id="root"')"
check "una pantalla interna carga al abrirla directamente" 200 "$(status "$BASE/mi-comercio")"
asset=$(request "$BASE/" | grep -o '/assets/[^"]*\.js' | head -1)
check "los archivos de la interfaz se descargan" 200 "$(status "$BASE$asset")"
check "politica de contenido activa" 1 "$(request -D - -o /dev/null "$BASE/" | grep -ci '^content-security-policy')"
check "la API exige sesion" 401 "$(status "$BASE/api/v1/users/me")"
check "la configuracion interna esta cerrada" 401 "$(status "$BASE/actuator/env")"
check "la documentacion no se publica" 404 "$(status "$BASE/v3/api-docs")"
check "el ingreso acepta peticiones de la propia web" 401 "$(login "$ORIGIN")"
check "el ingreso rechaza otras webs" 403 "$(login https://sitio-ajeno.example)"
# La conexion queda abierta: curl termina por tiempo, pero el codigo ya llego
check "los avisos en vivo (WebSocket) conectan" 101 "$(status --http1.1 -m 4 \
    -H 'Connection: Upgrade' -H 'Upgrade: websocket' -H 'Sec-WebSocket-Version: 13' \
    -H 'Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==' -H "Origin: $ORIGIN" "$BASE/ws/v1/events")"

if [ "$failures" -gt 0 ]; then
    echo "$failures comprobaciones fallaron"
    exit 1
fi
echo "Todo en orden"
