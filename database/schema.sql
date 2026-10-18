-- =====================================================================
-- BARBOYA · Esquema PostgreSQL 15+ con PostGIS
-- Movilidad, mensajería y domicilios locales · Barbosa, Santander (CO)
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pg_trgm;    -- búsqueda por texto en referencias locales

-- ---------------------------------------------------------------------
-- ENUM TYPES
-- ---------------------------------------------------------------------
CREATE TYPE user_role AS ENUM ('customer', 'driver', 'merchant_owner', 'admin');
CREATE TYPE vehicle_type AS ENUM ('moto', 'car', 'bicycle');
CREATE TYPE billing_scheme AS ENUM ('subscription', 'per_trip_fee');
CREATE TYPE subscription_plan AS ENUM ('daily', 'weekly', 'monthly');
CREATE TYPE subscription_status AS ENUM ('pending', 'active', 'expired', 'cancelled');
CREATE TYPE wallet_tx_type AS ENUM ('topup', 'trip_fee', 'subscription_purchase', 'refund', 'adjustment');
CREATE TYPE wallet_tx_status AS ENUM ('pending', 'approved', 'rejected');
CREATE TYPE payment_method AS ENUM ('cash', 'nequi', 'daviplata', 'physical_point');
CREATE TYPE service_type AS ENUM ('passenger', 'delivery', 'errand');
CREATE TYPE trip_status AS ENUM (
    'requested', 'negotiating', 'accepted', 'arrived', 'in_progress',
    'completed', 'cancelled_by_customer', 'cancelled_by_driver', 'expired'
);
CREATE TYPE merchant_category AS ENUM ('restaurant', 'pharmacy', 'grocery', 'bakery', 'hardware', 'other');
CREATE TYPE order_status AS ENUM (
    'placed', 'accepted', 'preparing', 'ready', 'picked_up', 'delivered', 'cancelled', 'rejected'
);
CREATE TYPE security_event_type AS ENUM (
    'sos', 'stopped_too_long', 'are_you_ok_sent', 'are_you_ok_ack',
    'face_check_requested', 'face_check_passed', 'face_check_failed', 'share_link_created'
);

-- ---------------------------------------------------------------------
-- ZONAS (polígonos urbanos/rurales, tarifas base y recargo nocturno)
-- ---------------------------------------------------------------------
CREATE TABLE zones (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                TEXT NOT NULL UNIQUE,                 -- p.ej. "Centro", "Vereda La Palma"
    is_urban            BOOLEAN NOT NULL DEFAULT TRUE,
    boundary            GEOMETRY(MultiPolygon, 4326) NOT NULL,
    base_fare_cop       INTEGER NOT NULL CHECK (base_fare_cop >= 0),
    per_km_cop          INTEGER NOT NULL DEFAULT 0 CHECK (per_km_cop >= 0),
    night_surcharge_pct NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK (night_surcharge_pct BETWEEN 0 AND 100),
    night_start         TIME NOT NULL DEFAULT '20:00',
    night_end           TIME NOT NULL DEFAULT '05:00',
    -- Esquema de cobro por defecto de la zona (configurable por el admin)
    billing_scheme      billing_scheme NOT NULL DEFAULT 'per_trip_fee',
    per_trip_fee_cop    INTEGER NOT NULL DEFAULT 300 CHECK (per_trip_fee_cop >= 0),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_zones_boundary ON zones USING GIST (boundary);

-- ---------------------------------------------------------------------
-- USERS
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role            user_role NOT NULL DEFAULT 'customer',
    full_name       TEXT NOT NULL,
    phone           TEXT NOT NULL UNIQUE CHECK (phone ~ '^\+?[0-9]{10,15}$'),
    email           TEXT UNIQUE,
    password_hash   TEXT NOT NULL,
    avatar_url      TEXT,
    default_address_note TEXT,                                -- "frente al Parque Principal"
    last_location   GEOGRAPHY(Point, 4326),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_last_location ON users USING GIST (last_location);

-- ---------------------------------------------------------------------
-- DRIVER PROFILES
-- ---------------------------------------------------------------------
CREATE TABLE driver_profiles (
    user_id             UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    vehicle_type        vehicle_type NOT NULL,
    vehicle_plate       TEXT NOT NULL UNIQUE,
    vehicle_description TEXT,                                 -- "Honda CB125 roja"
    document_id         TEXT NOT NULL UNIQUE,                 -- cédula
    license_doc_url     TEXT,
    selfie_reference_url TEXT,                                -- foto base para verificación facial
    billing_scheme      billing_scheme NOT NULL DEFAULT 'per_trip_fee',
    -- Billetera prepago (esquema fee por carrera). COP enteros, sin decimales.
    wallet_balance_cop  INTEGER NOT NULL DEFAULT 0 CHECK (wallet_balance_cop >= 0),
    -- Override opcional del fee para este conductor (si NULL se usa el de la zona)
    per_trip_fee_override_cop INTEGER CHECK (per_trip_fee_override_cop >= 0),
    subscription_active BOOLEAN NOT NULL DEFAULT FALSE,       -- denormalizado para consultas rápidas
    subscription_expires_at TIMESTAMPTZ,
    is_online           BOOLEAN NOT NULL DEFAULT FALSE,
    is_verified         BOOLEAN NOT NULL DEFAULT FALSE,
    last_face_check_at  TIMESTAMPTZ,
    face_check_due      BOOLEAN NOT NULL DEFAULT FALSE,
    rating_avg          NUMERIC(3,2) NOT NULL DEFAULT 5.00 CHECK (rating_avg BETWEEN 0 AND 5),
    rating_count        INTEGER NOT NULL DEFAULT 0,
    current_location    GEOGRAPHY(Point, 4326),
    location_updated_at TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_driver_location ON driver_profiles USING GIST (current_location);
CREATE INDEX idx_driver_online ON driver_profiles (is_online) WHERE is_online;

-- ---------------------------------------------------------------------
-- SUBSCRIPTIONS (pase diario / semanal / mensual)
-- ---------------------------------------------------------------------
CREATE TABLE subscriptions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id     UUID NOT NULL REFERENCES driver_profiles(user_id) ON DELETE CASCADE,
    plan          subscription_plan NOT NULL,
    price_cop     INTEGER NOT NULL CHECK (price_cop > 0),
    status        subscription_status NOT NULL DEFAULT 'pending',
    payment_method payment_method NOT NULL,
    payment_reference TEXT,                                   -- # de transferencia Nequi/Daviplata
    proof_url     TEXT,                                       -- comprobante en S3/Supabase
    starts_at     TIMESTAMPTZ,
    expires_at    TIMESTAMPTZ,
    approved_by   UUID REFERENCES users(id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (expires_at IS NULL OR starts_at IS NULL OR expires_at > starts_at)
);
CREATE INDEX idx_subscriptions_driver ON subscriptions (driver_id, status, expires_at DESC);

-- ---------------------------------------------------------------------
-- WALLET TRANSACTIONS (libro mayor inmutable de la bolsa prepago)
-- ---------------------------------------------------------------------
CREATE TABLE wallet_transactions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id      UUID NOT NULL REFERENCES driver_profiles(user_id) ON DELETE CASCADE,
    type           wallet_tx_type NOT NULL,
    status         wallet_tx_status NOT NULL DEFAULT 'approved',
    amount_cop     INTEGER NOT NULL CHECK (amount_cop <> 0),  -- + recarga / - descuento
    balance_after_cop INTEGER CHECK (balance_after_cop >= 0),
    payment_method payment_method,
    payment_reference TEXT,
    proof_url      TEXT,
    trip_id        UUID,                                      -- FK añadida tras crear trips
    order_id       UUID,                                      -- FK añadida tras crear orders
    subscription_id UUID REFERENCES subscriptions(id),
    note           TEXT,
    approved_by    UUID REFERENCES users(id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_wallet_tx_driver ON wallet_transactions (driver_id, created_at DESC);
-- Idempotencia: un solo descuento de fee por viaje
CREATE UNIQUE INDEX uq_wallet_fee_per_trip ON wallet_transactions (trip_id)
    WHERE type = 'trip_fee' AND trip_id IS NOT NULL;

-- ---------------------------------------------------------------------
-- MERCHANTS
-- ---------------------------------------------------------------------
CREATE TABLE merchants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name            TEXT NOT NULL,
    category        merchant_category NOT NULL,
    description     TEXT,
    logo_url        TEXT,
    address_note    TEXT NOT NULL,                            -- "al lado del D1"
    location        GEOGRAPHY(Point, 4326) NOT NULL,
    phone           TEXT,
    whatsapp        TEXT,
    commission_pct  NUMERIC(4,2) NOT NULL DEFAULT 10.00 CHECK (commission_pct BETWEEN 0 AND 30),
    -- Alternativa: suscripción fija por visibilidad destacada
    featured_until  TIMESTAMPTZ,
    featured_fee_cop INTEGER CHECK (featured_fee_cop >= 0),
    is_open         BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_merchants_location ON merchants USING GIST (location);
CREATE INDEX idx_merchants_category ON merchants (category) WHERE is_active;
CREATE INDEX idx_merchants_featured ON merchants (featured_until DESC NULLS LAST);

-- ---------------------------------------------------------------------
-- PRODUCTS
-- ---------------------------------------------------------------------
CREATE TABLE products (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id  UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    name         TEXT NOT NULL,
    description  TEXT,
    price_cop    INTEGER NOT NULL CHECK (price_cop >= 0),
    image_url    TEXT,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,               -- "agotado" con 1 clic
    stock        INTEGER CHECK (stock >= 0),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_products_merchant ON products (merchant_id) WHERE is_available;
CREATE INDEX idx_products_name_trgm ON products USING GIN (name gin_trgm_ops);

-- ---------------------------------------------------------------------
-- TRIPS (transporte, domicilio libre o mandado) con negociación de precio
-- ---------------------------------------------------------------------
CREATE TABLE trips (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id       UUID NOT NULL REFERENCES users(id),
    driver_id         UUID REFERENCES driver_profiles(user_id),
    zone_id           UUID REFERENCES zones(id),
    service_type      service_type NOT NULL,
    status            trip_status NOT NULL DEFAULT 'requested',
    pickup_point      GEOGRAPHY(Point, 4326) NOT NULL,
    pickup_note       TEXT,                                   -- referencia local
    dropoff_point     GEOGRAPHY(Point, 4326) NOT NULL,
    dropoff_note      TEXT,
    distance_m        INTEGER CHECK (distance_m >= 0),
    suggested_fare_cop INTEGER NOT NULL CHECK (suggested_fare_cop >= 0),
    offered_fare_cop  INTEGER NOT NULL CHECK (offered_fare_cop > 0),   -- "Pon tu precio"
    agreed_fare_cop   INTEGER CHECK (agreed_fare_cop > 0),
    platform_fee_cop  INTEGER NOT NULL DEFAULT 0 CHECK (platform_fee_cop >= 0),
    payment_method    payment_method NOT NULL DEFAULT 'cash',
    started_via_whatsapp BOOLEAN NOT NULL DEFAULT FALSE,
    confirmation_pin  CHAR(4) NOT NULL CHECK (confirmation_pin ~ '^[0-9]{4}$'),
    pin_verified_at   TIMESTAMPTZ,
    delivery_proof_url TEXT,
    package_notes     TEXT,
    order_id          UUID,                                   -- si proviene de un comercio
    share_token       TEXT UNIQUE,                            -- enlace público de seguimiento
    customer_rating   SMALLINT CHECK (customer_rating BETWEEN 1 AND 5),
    driver_rating     SMALLINT CHECK (driver_rating BETWEEN 1 AND 5),
    requested_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    accepted_at       TIMESTAMPTZ,
    started_at        TIMESTAMPTZ,
    completed_at      TIMESTAMPTZ,
    CHECK (driver_id IS NOT NULL OR status IN ('requested','negotiating','expired','cancelled_by_customer'))
);
CREATE INDEX idx_trips_pickup ON trips USING GIST (pickup_point);
CREATE INDEX idx_trips_dropoff ON trips USING GIST (dropoff_point);
CREATE INDEX idx_trips_status ON trips (status, requested_at DESC);
CREATE INDEX idx_trips_customer ON trips (customer_id, requested_at DESC);
CREATE INDEX idx_trips_driver ON trips (driver_id, requested_at DESC);

-- Ofertas y contraofertas de cada viaje
CREATE TABLE trip_offers (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id      UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    driver_id    UUID NOT NULL REFERENCES driver_profiles(user_id),
    amount_cop   INTEGER NOT NULL CHECK (amount_cop > 0),
    eta_minutes  SMALLINT,
    status       TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','accepted','rejected','withdrawn','expired')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_trip_offers_trip ON trip_offers (trip_id, created_at DESC);

-- ---------------------------------------------------------------------
-- ORDERS (pedidos a comercios)
-- ---------------------------------------------------------------------
CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id     UUID NOT NULL REFERENCES users(id),
    merchant_id     UUID NOT NULL REFERENCES merchants(id),
    trip_id         UUID REFERENCES trips(id),                -- el domicilio asociado
    status          order_status NOT NULL DEFAULT 'placed',
    items           JSONB NOT NULL,                           -- [{product_id,name,qty,unit_price_cop}]
    subtotal_cop    INTEGER NOT NULL CHECK (subtotal_cop >= 0),
    delivery_fee_cop INTEGER NOT NULL DEFAULT 0 CHECK (delivery_fee_cop >= 0),
    commission_cop  INTEGER NOT NULL DEFAULT 0 CHECK (commission_cop >= 0),
    total_cop       INTEGER NOT NULL CHECK (total_cop >= 0),
    payment_method  payment_method NOT NULL DEFAULT 'cash',
    delivery_address_note TEXT,
    delivery_point  GEOGRAPHY(Point, 4326),
    started_via_whatsapp BOOLEAN NOT NULL DEFAULT FALSE,
    prep_minutes    SMALLINT CHECK (prep_minutes > 0),        -- temporizador de preparación
    accepted_at     TIMESTAMPTZ,
    ready_at        TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_orders_merchant ON orders (merchant_id, status, created_at DESC);
CREATE INDEX idx_orders_customer ON orders (customer_id, created_at DESC);
CREATE INDEX idx_orders_delivery_point ON orders USING GIST (delivery_point);

ALTER TABLE trips ADD CONSTRAINT fk_trips_order FOREIGN KEY (order_id) REFERENCES orders(id);
ALTER TABLE wallet_transactions ADD CONSTRAINT fk_wallet_trip FOREIGN KEY (trip_id) REFERENCES trips(id);
ALTER TABLE wallet_transactions ADD CONSTRAINT fk_wallet_order FOREIGN KEY (order_id) REFERENCES orders(id);

-- ---------------------------------------------------------------------
-- SECURITY LOGS
-- ---------------------------------------------------------------------
CREATE TABLE security_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    trip_id     UUID REFERENCES trips(id),
    event_type  security_event_type NOT NULL,
    location    GEOGRAPHY(Point, 4326),
    details     JSONB NOT NULL DEFAULT '{}'::jsonb,
    resolved    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_security_logs_user ON security_logs (user_id, created_at DESC);
CREATE INDEX idx_security_logs_open ON security_logs (event_type, created_at DESC) WHERE NOT resolved;
CREATE INDEX idx_security_logs_location ON security_logs USING GIST (location);

-- ---------------------------------------------------------------------
-- TRIGGERS: updated_at y sincronización de suscripción activa
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION touch_updated_at() RETURNS trigger AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE OR REPLACE FUNCTION sync_driver_subscription() RETURNS trigger AS $$
BEGIN
    IF NEW.status = 'active' THEN
        UPDATE driver_profiles
           SET subscription_active = TRUE,
               subscription_expires_at = NEW.expires_at
         WHERE user_id = NEW.driver_id;
    END IF;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_subscription_sync AFTER INSERT OR UPDATE OF status ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION sync_driver_subscription();

-- ---------------------------------------------------------------------
-- SEED: zonas de ejemplo para Barbosa (polígonos aproximados, AJUSTAR en el panel admin)
-- Centro de Barbosa, Santander ≈ (-73.6208, 5.9333)
-- ---------------------------------------------------------------------
INSERT INTO zones (name, is_urban, boundary, base_fare_cop, per_km_cop, night_surcharge_pct, billing_scheme, per_trip_fee_cop)
VALUES
('Barbosa Casco Urbano', TRUE,
 ST_Multi(ST_GeomFromText('POLYGON((-73.630 5.925, -73.610 5.925, -73.610 5.942, -73.630 5.942, -73.630 5.925))', 4326)),
 3000, 800, 20, 'per_trip_fee', 300),
('Barbosa Rural', FALSE,
 ST_Multi(ST_GeomFromText('POLYGON((-73.680 5.880, -73.560 5.880, -73.560 6.000, -73.680 6.000, -73.680 5.880))', 4326)),
 5000, 1200, 25, 'per_trip_fee', 500);
