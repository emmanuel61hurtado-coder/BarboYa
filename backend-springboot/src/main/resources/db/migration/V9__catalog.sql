-- Fotos de productos y horarios de atencion

ALTER TABLE products ADD COLUMN photo_key VARCHAR(36);
CREATE UNIQUE INDEX products_photo_key ON products (photo_key) WHERE photo_key IS NOT NULL;

-- Sin filas: el comercio no usa horario y solo manda el interruptor de pedidos.
-- day: 1 lunes ... 7 domingo. closes menor que opens: cierra despues de medianoche.
-- Las horas son minutos desde la medianoche en Colombia: sin zonas horarias que las corran.
CREATE TABLE business_hours (
    business_id UUID    NOT NULL REFERENCES businesses (id),
    day         INTEGER NOT NULL CHECK (day BETWEEN 1 AND 7),
    opens       INTEGER NOT NULL CHECK (opens BETWEEN 0 AND 1439),
    closes      INTEGER NOT NULL CHECK (closes BETWEEN 0 AND 1439),
    PRIMARY KEY (business_id, day, opens),
    CONSTRAINT business_hours_range CHECK (opens <> closes)
);
