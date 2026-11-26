# BarboYa: diagnóstico técnico y plan por fases

Fecha: 26 de noviembre de 2026. Base revisada: commit `f1d19c5`. Incluye las correcciones de la Fase 0, subidas a `main` en los commits `8ddfbee` y `edb64f4`. El despliegue en el servidor no se ha verificado.

## 1. Resumen

- BarboYa **no está listo para operar con usuarios ni dinero reales**.
- El backend en uso es FastAPI (Python). El backend exigido, Spring Boot, existe como esqueleto inicial con fallos graves de acceso.
- En el panel de comercios, el panel de administración, comida, viajes y envíos, los componentes de React no llaman a la API.
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
| Pruebas Python (suite completa) | 76 aprobadas en 4 corridas seguidas |
| Prueba de punta a punta con la API real (SQLite) | Registro, ingreso, SOS, webhook y recuperación responden como se espera; en producción sin `JWT_SECRET` no arranca |
| TypeScript (`tsc --noEmit`) y build de Vite | Correctos. La interfaz no se probó en un navegador |
| Compilación de Spring Boot | Correcta; **0 pruebas** |
| `docker compose` | No ejecutado: Docker no está instalado |
| `ruff check` (paso del CI) | No ejecutado: Ruff no está instalado ni figura en `requirements.txt`, así que ese paso del CI debería fallar |
| `npm audit` | 1 vulnerabilidad alta y 1 moderada (Vite y esbuild) |
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

- **Interfaz sin conexión:** la recarga Nequi/Daviplata solo cambia el saldo en pantalla.
- **SOS:** la alerta queda guardada, pero nadie la atiende: falta notificar a soporte y contactos, y mostrarla en el panel de administración.
- **Incompatibilidad:** el frontend espera campos como `access_token` y rutas que Spring Boot no tiene (usa `accessToken`). Hoy no pueden funcionar juntos.
- **Spring Boot:** los pedidos son un marcador vacío; faltan Flyway, Actuator, correo, OpenAPI y Web3j; usa H2 y `ddl-auto: update`.
- **Frontend:** faltan React Router, TanStack Query, React Hook Form, Zod, Axios y shadcn/ui.
- **Sin implementar:** OTP, MFA, refresh tokens, libro contable de doble partida, proveedor de pagos, liquidaciones, contratos inteligentes y reputación.

## 6. Repositorio

- `frontend/node_modules` y `backend-springboot/target/` (con un `.jar` de decenas de MB) se retiraron del índice de Git y quedaron en `.gitignore`. El cambio ya está en `main`.
- El historial conserva las copias anteriores.
- Quien actualice otra copia del repositorio debe ejecutar `npm ci` en `frontend/`.

## 7. Plan por fases

Cada fase termina cuando sus pruebas pasan.

| Fase | Alcance | Criterio de salida |
|---|---|---|
| 0. Contención (hecha; despliegue sin verificar) | Recuperación de contraseña, secretos, oferta de viajes, webhook, accesos de demostración, SOS y limpieza de Git | Sin hallazgos críticos abiertos en el backend en uso |
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
1. Conservar o no la app Flutter.
2. Confirmar los teléfonos locales de emergencia antes de volver a mostrarlos.
3. Completar los requisitos: el documento recibido se corta en la sección 16 (Notificaciones).

**Cuentas y credenciales:** pasarela de pagos colombiana en sandbox, correo o SMS, proveedor de mapas, nodo RPC y billetera de red de pruebas, y acceso a la base PostgreSQL de desarrollo.

**Revisión legal (antes de operar):** transporte de pasajeros, en especial en motocicleta; tratamiento de datos personales y biométricos (Ley 1581 de 2012); y manejo de dinero de terceros en billetera y liquidaciones.
