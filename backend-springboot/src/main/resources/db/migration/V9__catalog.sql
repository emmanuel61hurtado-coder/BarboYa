-- Fotos de productos y horarios de atencion

ALTER TABLE products ADD COLUMN photo_key VARCHAR(36);
CREATE UNIQUE INDEX products_photo_key ON products (photo_key) WHERE photo_key IS NOT NULL;

-- Sin filas: el comercio no usa horario y solo manda el interruptor de pedidos.
-- day: 1 lunes ... 7 domingo. closes menor que opens: cierra despues de medianoche.
CREATE TABLE business_hours (
    business_id UUID    NOT NULL REFERENCES businesses (id),
    day         INTEGER NOT NULL CHECK (day BETWEEN 1 AND 7),
    opens       TIME    NOT NULL,
    closes      TIME    NOT NULL,
    PRIMARY KEY (business_id, day, opens),
    CONSTRAINT business_hours_range CHECK (opens <> closes)
);
