-- Direcciones guardadas del cliente y precios promocionales

CREATE TABLE customer_addresses (
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (id),
    label      VARCHAR(40)  NOT NULL,
    address    VARCHAR(200) NOT NULL,
    notes      VARCHAR(300),
    latitude   DOUBLE PRECISION CHECK (latitude BETWEEN -90 AND 90),
    longitude  DOUBLE PRECISION CHECK (longitude BETWEEN -180 AND 180),
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT customer_addresses_point CHECK ((latitude IS NULL) = (longitude IS NULL))
);
CREATE UNIQUE INDEX customer_addresses_label ON customer_addresses (user_id, lower(label));

-- Rebaja de un producto entre dos fechas (inclusive), en hora de Colombia
ALTER TABLE products
    ADD COLUMN promo_price_cop INTEGER CHECK (promo_price_cop >= 100),
    ADD COLUMN promo_starts_on DATE,
    ADD COLUMN promo_ends_on   DATE,
    ADD CONSTRAINT products_promo_complete CHECK (
        (promo_price_cop IS NULL) = (promo_starts_on IS NULL)
        AND (promo_price_cop IS NULL) = (promo_ends_on IS NULL)),
    ADD CONSTRAINT products_promo_is_discount CHECK (promo_price_cop < price_cop),
    ADD CONSTRAINT products_promo_dates CHECK (promo_ends_on >= promo_starts_on);
