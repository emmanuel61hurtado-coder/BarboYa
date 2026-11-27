-- Plazo de entrega, fechas limite de reclamos y eliminacion de cuentas

-- Cuando se elimina una cuenta sus datos personales se reemplazan, pero la fila queda
-- para no romper el historial de pedidos y cuentas que la ley obliga a conservar
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMPTZ;

-- Maximo prometido por el comercio entre el pedido y la entrega (hasta 30 dias)
ALTER TABLE businesses ADD COLUMN max_delivery_minutes INTEGER
    CHECK (max_delivery_minutes BETWEEN 10 AND 43200);
ALTER TABLE orders ADD COLUMN deliver_by TIMESTAMPTZ;

ALTER TABLE support_tickets ADD COLUMN due_at TIMESTAMPTZ;
UPDATE support_tickets SET due_at = created_at + INTERVAL '21 days';
ALTER TABLE support_tickets ALTER COLUMN due_at SET NOT NULL;
ALTER TABLE support_tickets DROP CONSTRAINT support_tickets_category_check;
ALTER TABLE support_tickets ADD CONSTRAINT support_tickets_category_check CHECK (category IN (
    'ORDER_PROBLEM', 'DELIVERY_FAILED', 'PAYMENT', 'ACCOUNT', 'OTHER', 'PERSONAL_DATA'));
CREATE INDEX idx_support_due ON support_tickets (status, due_at);
