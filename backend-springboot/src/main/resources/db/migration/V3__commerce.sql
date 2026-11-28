-- Comercios, catalogo y pedidos. Los importes son pesos colombianos enteros.

CREATE TABLE businesses (
    id               UUID PRIMARY KEY,
    owner_id         UUID         NOT NULL REFERENCES users (id),
    name             VARCHAR(120) NOT NULL,
    legal_name       VARCHAR(160),
    nit              VARCHAR(20),
    category         VARCHAR(20)  NOT NULL CHECK (category IN (
                         'RESTAURANT', 'GROCERY', 'PHARMACY', 'BAKERY', 'OTHER')),
    description      VARCHAR(500),
    address          VARCHAR(200) NOT NULL,
    phone            VARCHAR(16)  NOT NULL,
    pickup_enabled   BOOLEAN      NOT NULL,
    delivery_enabled BOOLEAN      NOT NULL,
    delivery_fee_cop INTEGER      NOT NULL CHECK (delivery_fee_cop BETWEEN 0 AND 100000),
    accepting_orders BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT businesses_owner_key UNIQUE (owner_id),
    CONSTRAINT businesses_fulfillment CHECK (pickup_enabled OR delivery_enabled)
);

CREATE TABLE products (
    id          UUID PRIMARY KEY,
    business_id UUID         NOT NULL REFERENCES businesses (id),
    name        VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    category    VARCHAR(60)  NOT NULL,
    price_cop   INTEGER      NOT NULL CHECK (price_cop BETWEEN 100 AND 10000000),
    -- NULL: el comercio no controla existencias de este producto
    stock       INTEGER      CHECK (stock >= 0),
    available   BOOLEAN      NOT NULL,
    -- Los productos no se borran: los pedidos anteriores los referencian
    archived    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_products_business ON products (business_id) WHERE NOT archived;

CREATE TABLE orders (
    id               UUID PRIMARY KEY,
    number           BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1001),
    customer_id      UUID         NOT NULL REFERENCES users (id),
    business_id      UUID         NOT NULL REFERENCES businesses (id),
    status           VARCHAR(20)  NOT NULL CHECK (status IN (
                         'PLACED', 'PREPARING', 'READY', 'OUT_FOR_DELIVERY',
                         'DELIVERED', 'REJECTED', 'CANCELLED')),
    fulfillment      VARCHAR(12)  NOT NULL CHECK (fulfillment IN ('PICKUP', 'DELIVERY')),
    delivery_address VARCHAR(200),
    notes            VARCHAR(300),
    subtotal_cop     INTEGER      NOT NULL CHECK (subtotal_cop > 0),
    delivery_fee_cop INTEGER      NOT NULL CHECK (delivery_fee_cop >= 0),
    total_cop        INTEGER      NOT NULL,
    payment_method   VARCHAR(12)  NOT NULL CHECK (payment_method IN ('CASH')),
    delivery_pin     VARCHAR(4)   NOT NULL,
    pin_attempts     INTEGER      NOT NULL DEFAULT 0 CHECK (pin_attempts >= 0),
    prep_minutes     INTEGER      CHECK (prep_minutes BETWEEN 1 AND 240),
    status_reason    VARCHAR(300),
    idempotency_key  VARCHAR(64)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    delivered_at     TIMESTAMPTZ,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT orders_total CHECK (total_cop = subtotal_cop + delivery_fee_cop),
    CONSTRAINT orders_address CHECK (fulfillment = 'PICKUP' OR delivery_address IS NOT NULL),
    -- Repetir una confirmacion no crea un segundo pedido
    CONSTRAINT orders_idempotency UNIQUE (customer_id, idempotency_key)
);
CREATE INDEX idx_orders_customer ON orders (customer_id, created_at DESC);
CREATE INDEX idx_orders_business ON orders (business_id, status, created_at DESC);

-- Nombre y precio se copian: cambiar el catalogo no altera pedidos hechos
CREATE TABLE order_items (
    id             UUID PRIMARY KEY,
    order_id       UUID         NOT NULL REFERENCES orders (id),
    product_id     UUID         NOT NULL REFERENCES products (id),
    name           VARCHAR(120) NOT NULL,
    unit_price_cop INTEGER      NOT NULL CHECK (unit_price_cop > 0),
    quantity       INTEGER      NOT NULL CHECK (quantity BETWEEN 1 AND 50),
    line_total_cop INTEGER      NOT NULL,
    CONSTRAINT order_items_total CHECK (line_total_cop = unit_price_cop * quantity),
    CONSTRAINT order_items_product UNIQUE (order_id, product_id)
);

CREATE TABLE order_events (
    id         UUID PRIMARY KEY,
    order_id   UUID         NOT NULL REFERENCES orders (id),
    status     VARCHAR(20)  NOT NULL,
    actor_id   UUID         NOT NULL,
    note       VARCHAR(300),
    created_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_order_events_order ON order_events (order_id, created_at);

CREATE FUNCTION forbid_history_changes() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION '% es de solo insercion', TG_TABLE_NAME;
END;
$$;

CREATE TRIGGER order_events_no_change
    BEFORE UPDATE OR DELETE ON order_events
    FOR EACH ROW EXECUTE FUNCTION forbid_history_changes();

CREATE TRIGGER order_events_no_truncate
    BEFORE TRUNCATE ON order_events
    FOR EACH STATEMENT EXECUTE FUNCTION forbid_history_changes();
