# BarboYa: diagnóstico técnico y plan por fases

Fecha: 26 de noviembre de 2026. Base revisada: commit `f1d19c5`. Incluye las correcciones de la Fase 0, subidas a `main` en los commits `8ddfbee` y `edb64f4`. El despliegue en el servidor no se ha verificado.

## 1. Resumen

- BarboYa **no está listo para operar con usuarios ni dinero reales**.
- El backend en uso es FastAPI (Python). En Spring Boot, el exigido, ya funcionan las cuentas, la administración, los comercios, el catálogo, los pedidos en efectivo con PIN, la verificación de domiciliarios, las entregas con mapa y las comisiones con libro contable, con interfaz en el modo `v1` y pruebas. Faltan viajes, pagos en línea y blockchain.
- En el panel de comercios, el panel de administración, comida, viajes y envíos, los componentes de React no llaman a la API.
- No hay contratos inteligentes: no existe código Solidity, Hardhat ni Web3j.

## 2. Qué existe

| Carpeta | Contenido | Decisión propuesta |
|---|---|---|
| `backend/` | FastAPI, SQLAlchemy, pagos cripto con JSON-RPC | Mantener como referencia hasta que Spring Boot lo iguale |
| `backend-springboot/` | Spring Boot 3.3.5 y Java 21: módulos nuevos de identidad y administración, más el esqueleto anterior desconectado | Reescribir cada módulo y retirar el esqueleto |
| `frontend/` | React 18 y TypeScript: la app actual (FastAPI) y la interfaz `v1` (Spring Boot) en el mismo proyecto | Migrar cada módulo a `v1` y retirar la app actual |
| `database/` | Esquema PostgreSQL con PostGIS | Convertir en migraciones Flyway |
| `mobile/` | App Flutter | Fuera del stack pedido; decidir si se conserva |

## 3. Verificaciones ejecutadas

| Verificación | Resultado |
|---|---|
| Pruebas Python (suite completa) | 76 aprobadas en 4 corridas seguidas |
| Prueba de punta a punta con la API real (SQLite) | Registro, ingreso, SOS, webhook y recuperación responden como se espera; en producción sin `JWT_SECRET` no arranca |
| TypeScript y build de Vite (ambos modos, Node 20) | Correctos |
| Pruebas de Spring Boot (PostgreSQL 16 real) | 200 aprobadas |
| Entorno de pruebas reproducido sin Docker (perfil `prod`, API sirviendo la interfaz) | Comprobador de despliegue y 71 pasos en Chrome aprobados (antes de fotos y horarios). La imagen Docker la construye el CI; falta ver ese resultado |
| Pruebas del frontend (`npm test`) | 34 aprobadas |
| Flujos de la interfaz `v1` en Chrome (API, PostgreSQL y correo de prueba) | 76 pasos (cuentas, pedido, verificación, entrega, comisiones, soporte, calificaciones, fotos y horarios), sin errores de consola; teselas del mapa simuladas |
| App actual en Chrome tras actualizar herramientas | Carga, cambia de pestaña y abre el SOS sin errores |
| `.jar` de Spring Boot con perfil `prod` | Arranca, aplica migraciones y responde por HTTP como se espera; sin `JWT_SECRET` no arranca |
| `docker compose` | No ejecutado: Docker no está instalado |
| `ruff check` (paso del CI) | No ejecutado: Ruff no está instalado ni figura en `requirements.txt`, así que ese paso del CI debería fallar |
| `npm audit` | 0 vulnerabilidades tras actualizar Vite, Vitest, Zod y React Router |
| Herramientas locales | Hay Java 21, Maven, Node 24 y PostgreSQL 16. Faltan Docker y Redis |

Las pruebas de Python se ejecutaron en una copia temporal del código, sin el archivo `.env`. El cuelgue intermitente de `test_blockchain_api.py`, que ya ocurría en el último commit, se resolvió evitando el arranque completo de la app dentro de la prueba.

## 4. Hallazgos de seguridad

### 4.1. FastAPI y frontend

| Gravedad | Hallazgo | Estado |
|---|---|---|
| Crítica | Cualquiera podía cambiar la contraseña de cualquier cuenta; el PIN era fijo y la API lo devolvía | **Corregido:** recuperación desactivada |
| Crítica | El SOS afirmaba transmitir coordenadas a la policía y no enviaba nada | **Corregido:** registra la alerta y dice que no avisa a nadie |
| Crítica | Secreto JWT con valor público en el código y en `docker-compose.yml` (el compose de Coolify ya lo exigía) | **Corregido:** en producción exige 32 caracteres o más |
| Alta | El SOS mostraba teléfonos locales que no se pudieron verificar | **Corregido:** quedan las líneas 123 y 119 |
| Alta | El cliente podía asignar un viaje a cualquier conductor y con cualquier valor | **Corregido:** exige una oferta vigente |
| Media | El webhook de pagos aceptaba peticiones sin firma y usaba el secreto de sesiones | **Corregido:** `WEBHOOK_SECRET` y firma obligatoria |
| Media | La interfaz publicada incluía accesos de demostración, también de administrador | **Corregido:** solo en desarrollo |
| Media | CORS permite cualquier origen por defecto | Pendiente |
| Baja | `docker-compose.yml` (local) usa `barboya` como contraseña por defecto de PostgreSQL; la base no publica puerto | Pendiente |

La aplicación Flutter permite contraofertar y aceptar de inmediato el nuevo valor. Con la corrección, el conductor debe enviar primero una oferta por ese valor.

### 4.2. Esqueleto anterior de Spring Boot

Estos fallos pertenecen al código de `com.barboya.barboya`, que desde la Fase 1 **ya no se carga**: sus rutas responden `401` o `403` (probado).

| Gravedad | Hallazgo | Evidencia original |
|---|---|---|
| Crítica | El registro público permitía elegir el rol `admin` | Probado en servidor local |
| Crítica | Un cliente accedía a rutas `/admin/**` | Probado en servidor local |
| Crítica | Un pago cripto inventado quedaba `confirmed` sin verificar la cadena | Probado en servidor local |
| Crítica | La respuesta del registro exponía el hash de la contraseña | Probado en servidor local |
| Crítica | Las rutas para aprobar recargas no comprobaban el rol | Lectura de código |
| Crítica | Cualquier usuario podía consultar, aceptar, completar o cancelar viajes ajenos | Lectura de código |
| Alta | El enlace público de seguimiento devolvía el viaje completo, con PIN y usuarios | Lectura de código |
| Alta | WebSocket sin autenticación, CORS abierto y secreto JWT por defecto | Lectura de código |

Cada módulo reescrito debe cubrir su fallo con una prueba antes de activarse.

## 5. Brechas funcionales

- **Interfaz sin conexión:** la recarga Nequi/Daviplata solo cambia el saldo en pantalla.
- **SOS:** la alerta queda guardada, pero nadie la atiende: falta notificar a soporte y contactos, y mostrarla en el panel de administración.
- **Incompatibilidad:** el frontend espera campos como `access_token` y rutas que Spring Boot no tiene (usa `accessToken`). Hoy no pueden funcionar juntos.
- **Spring Boot:** faltan repartidores, viajes, pagos, comisiones y Web3j.
- **Comercios:** faltan promociones, reportes, estadísticas y búsqueda por cercanía o precio.
- **Frontend `v1`:** usa React Router, TanStack Query, React Hook Form, Zod, Axios y componentes al estilo shadcn/ui escritos a mano. Faltan los módulos de viajes y finanzas.
- **Sin implementar:** MFA, verificación por SMS, proveedor de pagos, reembolsos, liquidaciones, impuestos y contratos inteligentes.

## 6. Repositorio

- `frontend/node_modules` y `backend-springboot/target/` (con un `.jar` de decenas de MB) se retiraron del índice de Git y quedaron en `.gitignore`. El cambio ya está en `main`.
- El historial conserva las copias anteriores.
- Quien actualice otra copia del repositorio debe ejecutar `npm ci` en `frontend/`.

## 7. Plan por fases

Cada fase termina cuando sus pruebas pasan.

| Fase | Alcance | Criterio de salida |
|---|---|---|
| 0. Contención (hecha; despliegue sin verificar) | Recuperación de contraseña, secretos, oferta de viajes, webhook, accesos de demostración, SOS y limpieza de Git | Sin hallazgos críticos abiertos en el backend en uso |
| 1. Base Spring Boot (identidad hecha) | Hecho: módulos, Flyway, roles, sesiones con refresco, códigos por correo, auditoría, OpenAPI, Actuator, CI. Falta: MFA y SMS | Pruebas de autorización por cada rol (cumplido) |
| 2. Base React (hecha, sin desplegar) | Router, TanStack Query, formularios con Zod, shadcn/ui, autenticación, seguridad y administración conectadas | Registro e ingreso funcionan de extremo a extremo (cumplido) |
| 3. Comercio y pedidos (hecha en lo esencial, sin desplegar) | Hecho: comercios, productos, inventario, fotografías, horarios, pedidos, PIN, tiempo real. Falta: promociones y reportes | Pedido completo probado entre cliente y comercio (cumplido) |
| 4A. Verificación de domiciliarios (hecha, sin desplegar) | Datos, autorización, documentos cifrados y revisión humana. Falta: prueba de vida, plazo de conservación y S3 | No opera quien no esté verificado (cumplido para la activación) |
| 4B. Entregas con domiciliarios (hecha, sin desplegar) | Ubicación, tarifa por distancia, toma de pedidos, recogida, evidencia y mapas. Las entregas fallidas pasan a soporte (fase 8) | Entrega completa con evidencia (cumplido) |
| 5A. Comisiones (hecha, sin desplegar) | Libro de doble partida, comisiones, estado de cuenta, pagos registrados a mano y tope de deuda | Las cuentas cuadran en las pruebas (cumplido) |
| 5B. Pagos en línea | Pasarela en sandbox, webhooks firmados, idempotencia, conciliación y reembolsos. Requiere cuenta de un proveedor | Pago de prueba confirmado por el proveedor |
| 6. Blockchain | `PaymentEscrow`, `CommissionManager`, `OrderVerification`, `RefundManager`, `ReputationRegistry`; Hardhat y Web3j en red de pruebas | Pruebas y análisis de contratos aprobados |
| 7. Move | Viajes, negociación, seguridad y SOS real | Revisión jurídica aprobada antes de activar |
| 8. Operación (en curso) | Hecho: vencimiento de pedidos sin respuesta, casos de soporte, cierre de pedidos y calificaciones con moderación. Falta: usar la reputación al asignar, notificaciones con la app cerrada, panel y reportes | Acciones críticas auditadas |
| Entorno de pruebas (preparado, sin publicar) | Imagen, compose, comprobador y [guía](despliegue-pruebas.md) | Entrega probada en celulares reales |
| 9. Lanzamiento | Endurecimiento, despliegue, monitoreo, retiro de FastAPI | Lista de producción completa |

## 8. Pendientes de decisión y de terceros

**Decisiones del proyecto**
1. Conservar o no la app Flutter.
2. Confirmar los teléfonos locales de emergencia antes de volver a mostrarlos.
3. Completar los requisitos: el documento recibido se corta en la sección 16 (Notificaciones).

**Cuentas y credenciales:** pasarela de pagos colombiana en sandbox, correo o SMS, proveedor de mapas, nodo RPC y billetera de red de pruebas, y acceso a la base PostgreSQL de desarrollo.

**Revisión legal (antes de operar):** transporte de pasajeros, en especial en motocicleta; tratamiento de datos personales y biométricos (Ley 1581 de 2012); y manejo de dinero de terceros en billetera y liquidaciones.
