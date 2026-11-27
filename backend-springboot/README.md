# BarboYa API (Spring Boot)

Backend en Java 21 y Spring Boot 3.3 que reemplazará al de FastAPI. Hoy cubre **identidad, administración de cuentas, comercios, catálogo, pedidos, verificación de domiciliarios, entregas, comisiones y libro contable**; los viajes y los pagos en línea siguen en `../backend` o están pendientes.

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
- `npm run build:v1` compila esta interfaz en `dist-v1/`. La propia API la sirve si `SPRING_WEB_RESOURCES_STATIC_LOCATIONS=file:/ruta/dist-v1/`.
- Una pantalla nueva debe añadirse también en `web/SpaRoutes.java`, y un recurso externo nuevo, en la política de contenido de `SecurityConfig`.

## Publicar

`Dockerfile` y `../docker-compose.pruebas.yml` empaquetan API e interfaz. Guía: [docs/despliegue-pruebas.md](../docs/despliegue-pruebas.md).

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
| `FILES_KEY` | Sí, salvo en `dev` | 32 bytes en base64 (`openssl rand -base64 32`). Cifra los documentos |
| `FILES_DIRECTORY` | Recomendada en `prod` | Carpeta privada y persistente. Por defecto `./data/files` |
| `DELIVERY_BASE_FEE_COP`, `DELIVERY_PER_KM_COP`, `DELIVERY_MAX_KM` | No | Tarifa del domiciliario: `3000`, `1000` y `8` |
| `MERCHANT_COMMISSION_BPS`, `COURIER_COMMISSION_BPS` | No | Comisiones en puntos básicos (`1000` = 10 %) |
| `DEBT_LIMIT_COP` | No | Tope de comisiones sin pagar: `50000` |
| `ORDER_ACCEPT_TIMEOUT` | No | Plazo del comercio para responder un pedido: `15m` |
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
| `GET /admin/users/{id}/business` | `ADMIN`, `SUPERADMIN` |
| `POST /admin/staff` | `SUPERADMIN` |
| `GET`, `PUT /courier/profile` | `COURIER` (propio) |
| `POST /courier/documents/{tipo}`, `DELETE /courier/documents/{id}`, `GET …/{id}/file` | `COURIER` (propios) |
| `GET /admin/users/{id}/courier`, `GET /admin/documents/{id}/file`, `POST /admin/documents/{id}/review` | `ADMIN`, `SUPERADMIN` |
| `GET /catalog/businesses`, `GET /catalog/businesses/{id}` | Sesión |
| `GET`, `PUT /merchant/business`, `PATCH /merchant/business/accepting` | `RESTAURANT`, `STORE` (propio) |
| `GET`, `POST /merchant/products`, `PUT`, `DELETE /merchant/products/{id}` | `RESTAURANT`, `STORE` (propios) |
| `GET /merchant/orders`, `POST /merchant/orders/{id}/{accept,reject,ready,dispatch,deliver,cancel}` | `RESTAURANT`, `STORE` (propios) |
| `POST`, `GET /orders`, `GET /orders/{id}`, `POST /orders/{id}/{cancel,new-pin}` | `CUSTOMER` (propios) |
| `GET /catalog/businesses/{id}/delivery-quote?lat&lng` | Sesión |
| `GET`, `PUT /courier/presence`, `GET /courier/offers`, `POST /courier/offers/{id}/accept` | `COURIER` |
| `GET /courier/delivery`, `POST /courier/delivery/{id}/{release,pickup,evidence,deliver,fail}` | `COURIER` (propia) |
| `GET /courier/history`, `GET /courier/earnings` | `COURIER` (propios) |
| `POST /merchant/orders/{id}/unassign` | `RESTAURANT`, `STORE` (propios) |
| `GET /finance/statement` | `RESTAURANT`, `STORE`, `COURIER` (propio) |
| `GET /admin/finance/summary`, `GET /admin/finance/accounts/{id}`, `POST …/payments` | `ADMIN`, `SUPERADMIN` |
| `POST /admin/finance/accounts/{id}/adjustments`, `POST /admin/finance/entries/{id}/reversal` | `SUPERADMIN` |
| `GET …/orders/{id}/evidence` (también bajo `/merchant` y `/courier/delivery`) | Participantes del pedido |

| `GET`, `POST /support/tickets`, `GET …/{id}`, `POST …/{id}/messages` | Sesión (casos propios) |
| `GET /admin/support/tickets`, `GET …/{id}`, `POST …/{id}/{messages,resolve}` | `ADMIN`, `SUPERADMIN` |
| `GET /admin/orders`, `GET /admin/orders/{id}`, `POST /admin/orders/{id}/close` | `ADMIN`, `SUPERADMIN` |

Toda ruta no declarada queda cerrada.

## Pedidos

```
PLACED ──aceptar──▶ PREPARING ──▶ READY ──enviar──▶ OUT_FOR_DELIVERY ──PIN──▶ DELIVERED
  │ rechazar → REJECTED                └──────── PIN (recoge en el local) ──▶ DELIVERED
  │ cancela el cliente → CANCELLED     El comercio puede cancelar desde PREPARING, con motivo
```

- Los precios los calcula el servidor. El cliente envía `expectedTotalCop`; si no coincide responde `PRICE_CHANGED`.
- `POST /orders` exige el encabezado `Idempotency-Key`: repetirlo devuelve el mismo pedido.
- Las existencias se reservan al pedir y se devuelven al rechazar o cancelar.
- Si el comercio no responde en el plazo (`respondBy`), el pedido se **cancela solo** y vuelven las existencias. Una tarea lo revisa cada 30 segundos.
- El **PIN de 4 dígitos** solo lo ve el cliente y es la única forma de marcar la entrega. Tras 5 fallos se bloquea hasta que el cliente genere otro.
- Pago: **solo en efectivo**. La plataforma todavía no cobra comisión.
- Entrega: con personal del comercio o recogiendo en el local.

## Verificación de domiciliarios

| Vehículo | Documentos obligatorios |
|---|---|
| Bicicleta | Identidad y foto del rostro |
| Motocicleta o automóvil | Además, licencia de conducción y SOAT, con vencimiento |

La revisión técnico-mecánica se puede adjuntar y la valora quien revisa. **Un abogado debe confirmar esta lista y el texto de autorización antes de operar.**

- La cuenta solo se activa cuando cada documento obligatorio está aprobado y vigente.
- Cambiar los datos o un documento obligatorio devuelve la cuenta a revisión. Un documento que vence después de la aprobación **no** suspende la cuenta por sí solo: la asignación de pedidos deberá comprobarlo.
- La revisión es **humana**. No hay verificación facial automática ni prueba de vida (requieren un proveedor).
- Archivos: JPG, PNG o PDF de hasta 5 MB, validados por contenido. Las imágenes conservan sus metadatos originales.
- Se guardan cifrados (AES-256-GCM). Cada consulta del equipo queda auditada. La persona puede eliminarlos.
- **Si pierdes `FILES_KEY`, los archivos no se pueden recuperar.** Respáldala aparte de la carpeta y de la base.
- Falta definir el plazo de conservación y un almacén compatible con S3.

## Entregas con domiciliarios

```
El comercio acepta ──▶ un domiciliario lo toma ──▶ READY ──código de recogida──▶ OUT_FOR_DELIVERY ──PIN──▶ DELIVERED
                          (puede soltarlo antes de recoger)                          └─ no se pudo entregar → FAILED
```

- **Dinero:** el domiciliario paga los productos al comercio al recogerlos y cobra el total al cliente. Su ganancia es la tarifa. La plataforma no registra esos pagos ni cobra comisión todavía.
- **Tarifa:** base más valor por kilómetro, redondeado a $100. La distancia es **en línea recta**.
- **Quién ve pedidos:** cuenta activa, documentos vigentes, conectado, posición de menos de 2 minutos y comercio a 5 km o menos. En bicicleta, el recorrido total no pasa de 4 km. Un pedido a la vez.
- **Privacidad:** antes de tomar el pedido no se muestran datos del cliente; al terminar dejan de mostrarse. Solo se guarda la última posición y se borra al desconectarse.
- La web no comparte la ubicación en segundo plano: la pantalla debe seguir abierta.
- Una entrega `FAILED` abre sola un caso de soporte. El equipo puede cerrar un pedido atascado (ver Soporte).
- El dinero que el domiciliario ya pagó al comercio **se resuelve por fuera de la plataforma**.
- Los mapas usan las teselas públicas de OpenStreetMap, aptas para desarrollo y poco tráfico.

## Comisiones y libro contable

| Quién | Base | Por defecto |
|---|---|---|
| Comercio | Valor de los productos (no su domicilio propio) | 10 % |
| Domiciliario | Tarifa del domicilio | 10 % |

- La comisión se **fija al crear el pedido** y se **causa al entregarlo**. El cliente no paga nada adicional.
- Libro de **doble partida**: la base rechaza asientos descuadrados y no permite modificarlos ni borrarlos. Los errores se corrigen con una reversión.
- Cuentas: `RECEIVABLE` (deuda de cada participante), `COMMISSION_REVENUE`, `PLATFORM_FUNDS` y `WAIVED`.
- Los pagos son **conciliación manual**: un administrador registra un dinero ya recibido, con su comprobante, que no se puede repetir. No hay pasarela ni confirmación automática.
- Una deuda **superior al tope** cierra el comercio a pedidos nuevos o impide al domiciliario tomarlos. Lo que está en curso continúa.
- No se calculan impuestos ni se emiten facturas: debe definirlo un contador antes de operar.
- Quien registra pagos puede ser cualquier administrador; perdonar y reversar, solo el superadministrador.

## Soporte

- Cualquier persona con sesión abre casos; sobre un pedido, solo quien participa en él (un caso abierto por persona y pedido, y máximo 10 abiertos).
- Estados: `OPEN` (espera al equipo), `ANSWERED` y `RESOLVED`. Responder un caso resuelto lo reabre.
- Los mensajes no se pueden editar ni borrar. Resolver un caso queda auditado.
- El correo de aviso no incluye el contenido del mensaje.
- **Cerrar un pedido** exige motivo y queda auditado: antes de recogerlo queda `CANCELLED` y vuelven las existencias; ya en camino queda `FAILED`. No genera comisiones.
- No hay chat en tiempo real, adjuntos, plazos de atención ni reembolsos. Falta definir quién atiende y en qué horario.

## Avisos en vivo

WebSocket en `/ws/v1/events`. El primer mensaje es `{"type":"auth","token":"<token de acceso>"}`; el servidor responde `ready` y luego envía `{"type":"order.updated","orderId":"…"}` a quienes participan y `{"type":"tickets.updated"}` a quien abrió un caso cuando el equipo responde. Los datos se consultan por la API. Las conexiones viven en memoria (una sola instancia).

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
- Fotografías de productos, horarios, promociones, reportes y búsqueda por cercanía o precio.
- Chat, calificaciones, orden de ofertas por reputación y seguimiento en segundo plano.
- Devolución del dinero entre comercio y domiciliario en entregas fallidas.
- Pasarela de pagos, reembolsos, liquidaciones, impuestos y facturación.
- Límite de peticiones y avisos en vivo compartidos entre instancias (hoy en memoria).
- Migración de las cuentas existentes en FastAPI.
- Endurecimiento y monitoreo para producción.
