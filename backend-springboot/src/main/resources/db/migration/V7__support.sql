-- Vencimiento de pedidos, soporte y reclamaciones

-- NULL: cambio hecho por el sistema, por ejemplo un pedido vencido
ALTER TABLE order_events ALTER COLUMN actor_id DROP NOT NULL;
CREATE INDEX idx_orders_placed ON orders (created_at) WHERE status = 'PLACED';

CREATE TABLE support_tickets (
    id          UUID PRIMARY KEY,
    number      BIGINT GENERATED ALWAYS AS IDENTITY,
    opener_id   UUID         NOT NULL REFERENCES users (id),
    order_id    UUID         REFERENCES orders (id),
    category    VARCHAR(20)  NOT NULL CHECK (category IN (
                    'ORDER_PROBLEM', 'DELIVERY_FAILED', 'PAYMENT', 'ACCOUNT', 'OTHER')),
    subject     VARCHAR(120) NOT NULL,
    -- OPEN espera al equipo; ANSWERED espera a quien reclamo
    status      VARCHAR(10)  NOT NULL CHECK (status IN ('OPEN', 'ANSWERED', 'RESOLVED')),
    resolution  VARCHAR(500),
    resolved_by UUID,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT support_resolution CHECK ((status = 'RESOLVED') = (resolution IS NOT NULL))
);
-- Un reclamo abierto por persona y pedido
CREATE UNIQUE INDEX support_one_open_per_order ON support_tickets (opener_id, order_id)
    WHERE order_id IS NOT NULL AND status <> 'RESOLVED';
CREATE INDEX idx_support_opener ON support_tickets (opener_id, updated_at DESC);
CREATE INDEX idx_support_status ON support_tickets (status, updated_at);

CREATE TABLE support_messages (
    id         UUID PRIMARY KEY,
    ticket_id  UUID          NOT NULL REFERENCES support_tickets (id),
    author_id  UUID          NOT NULL REFERENCES users (id),
    staff      BOOLEAN       NOT NULL,
    body       VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_support_messages_ticket ON support_messages (ticket_id, created_at);

CREATE TRIGGER support_messages_no_change BEFORE UPDATE OR DELETE ON support_messages
    FOR EACH ROW EXECUTE FUNCTION forbid_history_changes();
CREATE TRIGGER support_messages_no_truncate BEFORE TRUNCATE ON support_messages
    FOR EACH STATEMENT EXECUTE FUNCTION forbid_history_changes();
