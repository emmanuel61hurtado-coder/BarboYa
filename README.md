# Barboya

Plataforma de movilidad, mensajería y domicilios para **Barbosa, Santander**.
Mezcla la negociación de precio de inDrive con la experiencia unificada de DiDi,
con cobro híbrido para conductores (pase por tiempo **o** fee fijo por carrera).

```
barboya/
├── database/schema.sql        PostgreSQL + PostGIS (enums, tablas, índices GIST, triggers, zonas semilla)
├── backend/                   FastAPI async · SQLAlchemy 2.0 · Pydantic v2 · WebSockets · Redis
│   └── app/{core,models,schemas,routers,services,websockets}
├── mobile/                    Flutter · BLoC · feature-first + clean architecture
│   └── lib/{core,features/{auth,home,negotiation}}
└── docker-compose.yml         PostGIS + Redis + API
```

## Arrancar el backend

```bash
docker compose up --build        # PostGIS, Redis, API y React en :8000
# Sin Docker: construir React y luego iniciar FastAPI
cd frontend
npm ci
npm run build
cd ../backend
pip install -r requirements.txt
uvicorn app.main:app --reload
```
FastAPI sirve el build de React y la API desde el mismo origen. En desarrollo, Vite
se ejecuta con `cd frontend && npm run dev` y redirige `/api` al backend en `:8000`.
El modo local SQLite crea automáticamente el usuario de demostración Pasajero
(`+573001112233` / `Cliente123!`) solo cuando `ENVIRONMENT=development`.
Documentación interactiva: http://localhost:8000/docs · Pruebas: `cd backend && pytest`

## Arrancar la app

```bash
cd mobile
flutter create .                 # genera android/ e ios/ (una sola vez)
flutter pub get
flutter run                      # baseUrl por defecto: http://10.0.2.2:8000 (emulador Android)
```
Ajusta `baseUrl` en `lib/core/network/api_client.dart` si pruebas en un dispositivo real.

## Cobro híbrido (corazón del negocio)

`backend/app/services/billing.py` decide si un conductor puede aceptar:

| Esquema | Para ACEPTAR | Al COMPLETAR |
|---|---|---|
| `subscription` | pase vigente (`subscription_expires_at > now`) | no descuenta nada (100 % de la tarifa) |
| `per_trip_fee` | `wallet_balance_cop >= fee` | descuenta el fee fijo, una sola vez por viaje (índice único) |

Prioridad del fee: override del conductor → fee de la zona → `DEFAULT_TRIP_FEE_COP`.
El admin cambia el esquema por conductor con `PUT /admin/drivers/billing`, aprueba recargas con
`POST /admin/topups/{id}/decision` y activa pases con `POST /admin/subscriptions`.
Las validaciones ocurren con la fila del conductor bloqueada (`FOR UPDATE`) para evitar carreras.

## Flujo "Pon Tu Precio"

1. Cliente: `POST /trips/quote` → tarifa sugerida (zona + km + recargo nocturno) → ajusta con ±$500.
2. `POST /trips` crea el viaje (PIN de 4 dígitos, enlace público de seguimiento) y avisa por WebSocket (`trip.new`) **solo** a conductores online, verificados y elegibles dentro de 3 km (PostGIS `ST_DWithin`).
3. Conductor: `POST /trips/{id}/driver-accept` (acepta tu precio) o `POST /trips/{id}/offers` (contraoferta) → llega `offer.new` al cliente.
4. Cliente: `POST /trips/{id}/accept` o `/counter`. Al aceptar se re-valida el cobro y se emite `offer.accepted`.
5. Conductor valida el PIN (`/verify-pin`) y cierra (`/complete`).

WebSocket: `ws://host/ws?token=<JWT>`. Mensajes `{"type": "...", "payload": {...}}`.

## Estado del proyecto (léelo antes de producción)

**Implementado y probado:** esquema SQL, lógica de cobro y tarifas (7 pruebas unitarias en verde), rutas de la API, gestor de WebSockets.

**Escrito pero sin ejecutar contra servicios reales:** los endpoints con PostGIS/Redis/WebSocket (no se levantó Postgres ni Redis al construirlo) y **toda la app Flutter** (no había SDK de Flutter, así que no se compiló). Espera pequeños ajustes al primer `flutter run` / `docker compose up`.

**Pendiente (no incluido):** módulo de comercios/pedidos y panel admin (existen en el esquema SQL, faltan endpoints y UI), app de conductor, verificación facial con prueba de vida, subida de archivos a S3/Supabase, mapas y GPS (`geolocator`), SOS y alertas "¿Todo bien?", integración Nequi/Daviplata (hoy las recargas son manuales con aprobación admin), migraciones Alembic, autenticación OTP. Las coordenadas de `home_page.dart` y los polígonos de zonas del SQL son **aproximados** y deben reemplazarse por datos reales de Barbosa.
