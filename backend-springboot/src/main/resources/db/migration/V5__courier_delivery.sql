-- Entregas con domiciliarios de BarboYa

ALTER TABLE businesses
    ADD COLUMN latitude  DOUBLE PRECISION CHECK (latitude BETWEEN -90 AND 90),
    ADD COLUMN longitude DOUBLE PRECISION CHECK (longitude BETWEEN -180 AND 180),
    ADD COLUMN courier_delivery_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE businesses DROP CONSTRAINT businesses_fulfillment;
ALTER TABLE businesses
    ADD CONSTRAINT businesses_fulfillment
        CHECK (pickup_enabled OR delivery_enabled OR courier_delivery_enabled),
    ADD CONSTRAINT businesses_location CHECK ((latitude IS NULL) = (longitude IS NULL)),
    ADD CONSTRAINT businesses_courier_location CHECK (NOT courier_delivery_enabled OR latitude IS NOT NULL);

ALTER TABLE orders DROP CONSTRAINT orders_status_check;
ALTER TABLE orders DROP CONSTRAINT orders_fulfillment_check;
ALTER TABLE orders
    ADD CONSTRAINT orders_status_check CHECK (status IN (
        'PLACED', 'PREPARING', 'READY', 'OUT_FOR_DELIVERY', 'DELIVERED', 'REJECTED', 'CANCELLED', 'FAILED')),
    ADD CONSTRAINT orders_fulfillment_check CHECK (fulfillment IN ('PICKUP', 'DELIVERY', 'COURIER')),
    ADD COLUMN courier_id         UUID REFERENCES users (id),
    ADD COLUMN delivery_latitude  DOUBLE PRECISION CHECK (delivery_latitude BETWEEN -90 AND 90),
    ADD COLUMN delivery_longitude DOUBLE PRECISION CHECK (delivery_longitude BETWEEN -180 AND 180),
    ADD COLUMN distance_km        DOUBLE PRECISION CHECK (distance_km >= 0),
    -- Lo muestra el comercio; el domiciliario lo escribe al recibir el pedido
    ADD COLUMN pickup_code        VARCHAR(4),
    ADD COLUMN pickup_attempts    INTEGER NOT NULL DEFAULT 0 CHECK (pickup_attempts >= 0),
    ADD COLUMN assigned_at        TIMESTAMPTZ,
    ADD COLUMN picked_up_at       TIMESTAMPTZ,
    -- Foto de entrega, cifrada fuera de la base
    ADD COLUMN evidence_key       VARCHAR(36),
    ADD COLUMN evidence_type      VARCHAR(40);
ALTER TABLE orders ADD CONSTRAINT orders_courier_destination
    CHECK (fulfillment <> 'COURIER' OR (delivery_latitude IS NOT NULL AND delivery_longitude IS NOT NULL));

-- Un domiciliario lleva un pedido a la vez
CREATE UNIQUE INDEX orders_one_active_per_courier ON orders (courier_id)
    WHERE courier_id IS NOT NULL AND status IN ('PLACED', 'PREPARING', 'READY', 'OUT_FOR_DELIVERY');
CREATE INDEX idx_orders_courier ON orders (courier_id, created_at DESC);
CREATE INDEX idx_orders_unassigned ON orders (status)
    WHERE courier_id IS NULL AND fulfillment = 'COURIER';

-- Solo se guarda la ultima posicion, nunca el recorrido
CREATE TABLE courier_presence (
    courier_id UUID PRIMARY KEY REFERENCES courier_profiles (user_id),
    online     BOOLEAN     NOT NULL,
    latitude   DOUBLE PRECISION,
    longitude  DOUBLE PRECISION,
    seen_at    TIMESTAMPTZ,
    version    BIGINT      NOT NULL DEFAULT 0
);
