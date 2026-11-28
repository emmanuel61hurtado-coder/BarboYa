-- Calificaciones: solo despues de un pedido entregado

CREATE TABLE ratings (
    id            UUID PRIMARY KEY,
    order_id      UUID        NOT NULL REFERENCES orders (id),
    author_id     UUID        NOT NULL REFERENCES users (id),
    target_type   VARCHAR(10) NOT NULL CHECK (target_type IN ('BUSINESS', 'COURIER')),
    -- Comercio o persona domiciliaria, segun target_type
    target_id     UUID        NOT NULL,
    stars         INTEGER     NOT NULL CHECK (stars BETWEEN 1 AND 5),
    comment       VARCHAR(500),
    hidden        BOOLEAN     NOT NULL DEFAULT FALSE,
    hidden_reason VARCHAR(300),
    created_at    TIMESTAMPTZ NOT NULL,
    version       BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ratings_one_per_order UNIQUE (order_id, target_type),
    CONSTRAINT ratings_hidden_reason CHECK (hidden = (hidden_reason IS NOT NULL))
);
CREATE INDEX idx_ratings_target ON ratings (target_type, target_id, created_at DESC);
CREATE INDEX idx_ratings_recent ON ratings (hidden, created_at DESC);

-- El equipo puede ocultar una calificacion, nunca cambiarla ni borrarla
CREATE FUNCTION protect_rating() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP <> 'UPDATE'
        OR (NEW.id, NEW.order_id, NEW.author_id, NEW.target_type, NEW.target_id, NEW.stars, NEW.created_at)
            IS DISTINCT FROM
           (OLD.id, OLD.order_id, OLD.author_id, OLD.target_type, OLD.target_id, OLD.stars, OLD.created_at)
        OR NEW.comment IS DISTINCT FROM OLD.comment THEN
        RAISE EXCEPTION 'Las calificaciones no se pueden modificar ni borrar';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ratings_protected BEFORE UPDATE OR DELETE ON ratings
    FOR EACH ROW EXECUTE FUNCTION protect_rating();
CREATE TRIGGER ratings_no_truncate BEFORE TRUNCATE ON ratings
    FOR EACH STATEMENT EXECUTE FUNCTION forbid_history_changes();
