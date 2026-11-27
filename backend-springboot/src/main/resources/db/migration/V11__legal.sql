-- Prueba de las autorizaciones e identificacion de quienes venden

CREATE TABLE legal_acceptances (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id),
    -- TERMS y PRIVACY guardan la version aceptada; ADULT, la declaracion de mayoria de edad
    document    VARCHAR(10)  NOT NULL CHECK (document IN ('TERMS', 'PRIVACY', 'ADULT')),
    version     VARCHAR(40)  NOT NULL,
    accepted_at TIMESTAMPTZ  NOT NULL,
    ip          VARCHAR(64),
    user_agent  VARCHAR(300),
    CONSTRAINT legal_acceptances_once UNIQUE (user_id, document, version)
);

CREATE TRIGGER legal_acceptances_no_change BEFORE UPDATE OR DELETE ON legal_acceptances
    FOR EACH ROW EXECUTE FUNCTION forbid_history_changes();
CREATE TRIGGER legal_acceptances_no_truncate BEFORE TRUNCATE ON legal_acceptances
    FOR EACH STATEMENT EXECUTE FUNCTION forbid_history_changes();

-- Registro exigido a los portales de contacto (Ley 1480 de 2011, articulos 50 y 53).
-- Admiten nulos por los comercios anteriores, que deben completarlos antes de recibir pedidos.
ALTER TABLE businesses
    ADD COLUMN notification_address VARCHAR(200),
    ADD COLUMN contact_email        VARCHAR(254);
