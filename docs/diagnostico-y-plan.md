# BarboYa: diagnóstico técnico y plan por fases

Fecha: 26 de noviembre de 2026. Base revisada: commit `f1d19c5`, más las correcciones de la Fase 0 marcadas como hechas.

## 1. Resumen

- BarboYa **no está listo para operar con usuarios ni dinero reales**.
- El backend en uso es FastAPI (Python). El backend exigido, Spring Boot, existe como esqueleto inicial con fallos graves de acceso.
- En el panel de comercios, el panel de administración, comida, viajes, envíos y SOS, los componentes de React no llaman a la API.
- No hay contratos inteligentes: no existe código Solidity, Hardhat ni Web3j.

## 2. Qué existe

| Carpeta | Contenido | Decisión propuesta |
|---|---|---|
| `backend/` | FastAPI, SQLAlchemy, pagos cripto con JSON-RPC | Mantener como referencia hasta que Spring Boot lo iguale |
| `backend-springboot/` | Spring Boot 3.3.5, Java 21, entidades y controladores básicos | Reutilizar la base y reorganizar por módulos |
| `frontend/` | React 18, TypeScript, Vite, Tailwind, Leaflet | Reutilizar el diseño y conectar a la API |
| `database/` | Esquema PostgreSQL con PostGIS | Convertir en migraciones Flyway |
| `mobile/` | App Flutter | Fuera del stack pedido; decidir si se conserva |

## 3. Verificaciones ejecutadas

| Verificación | Resultado |
|---|---|
| Compilación de Spring Boot | Correcta; **0 pruebas** |
| TypeScript (`tsc --noEmit`) y build de Vite | Correctos |
| Pruebas Python, sin `test_blockchain_api.py` | 55 aprobadas |
| `test_blockchain_api.py` | La primera prueba pasa y la segunda se cuelga al preparar el cliente de pruebas (hilo de `aiosqlite`). Causa raíz sin determinar |
| `test_schema_sync.py` ejecutado solo | Falla porque no importa los modelos; pasa junto con el resto |
| `ruff check` (paso del CI) | No ejecutado: Ruff no está instalado ni figura en `requirements.txt`, así que ese paso del CI debería fallar |
| `npm audit` | 1 vulnerabilidad alta y 1 moderada (Vite y esbuild) |
| Herramientas locales | Hay Java 21, Maven, Node 24 y PostgreSQL 16. Faltan Docker y Redis |

Las pruebas se ejecutaron en una copia temporal del código, sin el archivo `.env`.

## 4. Hallazgos de seguridad

### 4.1. FastAPI

| Gravedad | Hallazgo | Evidencia | Estado |
|---|---|---|---|
| Crítica | Cualquiera podía cambiar la contraseña de cualquier cuenta; el PIN era fijo y la API lo devolvía | Reproducido con una prueba | **Corregido** (recuperación desactivada) |
| Crítica | `JWT_SECRET` tiene un valor por defecto público en el código y en Docker Compose; si no se define, se pueden falsificar tokens de administrador | Lectura de código | Pendiente |
| Alta | La contraseña por defecto de PostgreSQL en Compose es `barboya` | Lectura de código | Pendiente |
| Alta | El cliente puede asignar un viaje a cualquier conductor y con cualquier valor, sin que exista una oferta suya | `routers/trips.py`, `customer_accepts_offer` | Pendiente |
| Media | El webhook de pagos acepta peticiones sin firma (hoy no mueve dinero) | `routers/webhooks.py` | Pendiente |
| Media | CORS permite cualquier origen por defecto | `core/config.py` | Pendiente |

### 4.2. Spring Boot (no debe desplegarse)

| Gravedad | Hallazgo | Evidencia |
|---|---|---|
| Crítica | El registro público permite elegir el rol `admin` | Probado en servidor local |
| Crítica | Un cliente accede a rutas `/admin/**` | Probado en servidor local |
| Crítica | Un pago cripto inventado queda `confirmed` sin verificar la cadena | Probado en servidor local |
| Crítica | La respuesta del registro expone el hash de la contraseña | Probado en servidor local |
| Crítica | Las rutas para aprobar recargas no comprueban el rol | Lectura de código |
| Crítica | Cualquier usuario autenticado puede consultar, aceptar, completar o cancelar viajes ajenos | Lectura de código |
| Alta | El enlace público de seguimiento devuelve la entidad completa del viaje, con PIN y usuarios | Lectura de código |
| Alta | WebSocket sin autenticación, CORS abierto y secreto JWT por defecto | Lectura de código |

## 5. Brechas funcionales

- **Interfaz sin conexión:** la recarga Nequi/Daviplata solo cambia el saldo en pantalla. El SOS no envía nada al servidor, aunque la pantalla afirma que las coordenadas se transmitieron. Es un riesgo para las personas y debe corregirse antes de cualquier uso.
- **Accesos de demostración:** la pantalla de ingreso incluye botones que rellenan credenciales, también de administrador.
- **Incompatibilidad:** el frontend espera campos como `access_token` y rutas que Spring Boot no tiene (usa `accessToken`). Hoy no pueden funcionar juntos.
- **Spring Boot:** los pedidos son un marcador vacío; faltan Flyway, Actuator, correo, OpenAPI y Web3j; usa H2 y `ddl-auto: update`.
- **Frontend:** faltan React Router, TanStack Query, React Hook Form, Zod, Axios y shadcn/ui.
- **Sin implementar:** OTP, MFA, refresh tokens, libro contable de doble partida, proveedor de pagos, liquidaciones, contratos inteligentes y reputación.

## 6. Repositorio

- `frontend/node_modules`, `backend-springboot/target/` (incluye un `.jar` de decenas de MB) y una base H2 `.mv.db` están versionados.
- `.gitignore` ya los excluye (**hecho**), pero siguen en Git hasta ejecutar `git rm -r --cached`. Ese comando no los borra del disco. El historial conservará las copias anteriores.

## 7. Plan por fases

Cada fase termina cuando sus pruebas pasan.

| Fase | Alcance | Criterio de salida |
|---|---|---|
| 0. Contención | Recuperación de contraseña (hecho), `.gitignore` (hecho), secretos obligatorios, oferta de viajes, webhook, botones de demostración, SOS | Sin hallazgos críticos abiertos en el backend en uso |
| 1. Base Spring Boot | Módulos, Flyway, PostgreSQL, roles, JWT con refresh tokens, OTP y recuperación, OpenAPI, Actuator, CI | Pruebas de autorización por cada rol |
| 2. Base React | Router, TanStack Query, formularios con Zod, shadcn/ui, autenticación conectada | Registro e ingreso funcionan de extremo a extremo |
| 3. Comercio y pedidos | Comercios, productos, inventario, pedidos, PIN de entrega, tiempo real | Pedido completo probado entre cliente y comercio |
| 4. Domiciliarios | Verificación, documentos, ubicación, asignación | Entrega completa con evidencia |
| 5. Finanzas | Libro de doble partida, pasarela en sandbox, webhooks firmados, idempotencia, conciliación | Las cuentas cuadran en pruebas con fallos y reintentos |
| 6. Blockchain | `PaymentEscrow`, `CommissionManager`, `OrderVerification`, `RefundManager`, `ReputationRegistry`; Hardhat y Web3j en red de pruebas | Pruebas y análisis de contratos aprobados |
| 7. Move | Viajes, negociación, seguridad y SOS real | Revisión jurídica aprobada antes de activar |
| 8. Operación | Administración, reputación, notificaciones, soporte | Acciones críticas auditadas |
| 9. Lanzamiento | Endurecimiento, despliegue, monitoreo, retiro de FastAPI | Lista de producción completa |

## 8. Pendientes de decisión y de terceros

**Decisiones del proyecto**
1. Autorizar el retiro de `node_modules`, `target/` y la base H2 del índice de Git.
2. Conservar o no la app Flutter.
3. Autorizar la reorganización del esqueleto Spring Boot por módulos.
4. Completar los requisitos: el documento recibido se corta en la sección 16 (Notificaciones).

**Cuentas y credenciales:** pasarela de pagos colombiana en sandbox, correo o SMS, proveedor de mapas, nodo RPC y billetera de red de pruebas, y acceso a la base PostgreSQL de desarrollo.

**Revisión legal (antes de operar):** transporte de pasajeros, en especial en motocicleta; tratamiento de datos personales y biométricos (Ley 1581 de 2012); y manejo de dinero de terceros en billetera y liquidaciones.
