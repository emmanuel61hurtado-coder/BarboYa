-- Libro contable de doble partida y comisiones. Pesos colombianos enteros.

-- Se fijan al crear el pedido: cambiar las tasas no altera lo ya pactado
ALTER TABLE orders
    ADD COLUMN merchant_commission_cop INTEGER NOT NULL DEFAULT 0 CHECK (merchant_commission_cop >= 0),
    ADD COLUMN courier_commission_cop  INTEGER NOT NULL DEFAULT 0 CHECK (courier_commission_cop >= 0);

CREATE TABLE ledger_entries (
    id          UUID PRIMARY KEY,
    kind        VARCHAR(12)  NOT NULL CHECK (kind IN ('COMMISSION', 'PAYMENT', 'ADJUSTMENT', 'REVERSAL')),
    -- Comercio o domiciliario de quien es la cuenta
    party_id    UUID         NOT NULL REFERENCES users (id),
    order_id    UUID         REFERENCES orders (id),
    amount_cop  INTEGER      NOT NULL CHECK (amount_cop > 0),
    method      VARCHAR(16)  CHECK (method IN ('BANK_TRANSFER', 'NEQUI', 'CASH')),
    reference   VARCHAR(80),
    note        VARCHAR(300),
    reversal_of UUID         REFERENCES ledger_entries (id),
    -- NULL: asiento generado por el sistema
    created_by  UUID,
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ledger_commission_order CHECK (kind <> 'COMMISSION' OR order_id IS NOT NULL),
    CONSTRAINT ledger_payment_proof CHECK (kind <> 'PAYMENT' OR (method IS NOT NULL AND reference IS NOT NULL)),
    CONSTRAINT ledger_reversal_target CHECK ((kind = 'REVERSAL') = (reversal_of IS NOT NULL)),
    -- Un asiento solo se reversa una vez
    CONSTRAINT ledger_reversal_once UNIQUE (reversal_of)
);
-- Un pedido genera como maximo una comision por participante
CREATE UNIQUE INDEX ledger_one_commission ON ledger_entries (order_id, party_id) WHERE kind = 'COMMISSION';
CREATE INDEX idx_ledger_party ON ledger_entries (party_id, created_at DESC);
CREATE INDEX idx_ledger_reference ON ledger_entries (method, lower(reference)) WHERE kind = 'PAYMENT';

CREATE TABLE ledger_lines (
    id         UUID PRIMARY KEY,
    entry_id   UUID        NOT NULL REFERENCES ledger_entries (id),
    account    VARCHAR(20) NOT NULL CHECK (account IN (
                   'RECEIVABLE', 'COMMISSION_REVENUE', 'PLATFORM_FUNDS', 'WAIVED')),
    party_id   UUID        REFERENCES users (id),
    debit_cop  INTEGER     NOT NULL CHECK (debit_cop >= 0),
    credit_cop INTEGER     NOT NULL CHECK (credit_cop >= 0),
    CONSTRAINT ledger_line_one_side CHECK ((debit_cop = 0) <> (credit_cop = 0)),
    CONSTRAINT ledger_line_party CHECK ((account = 'RECEIVABLE') = (party_id IS NOT NULL))
);
CREATE INDEX idx_ledger_lines_entry ON ledger_lines (entry_id);
CREATE INDEX idx_ledger_lines_receivable ON ledger_lines (party_id) WHERE account = 'RECEIVABLE';

-- Al confirmar la transaccion, cada asiento tiene que cuadrar
CREATE FUNCTION require_balanced_entry() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    target UUID;
    lines  INTEGER;
    diff   BIGINT;
BEGIN
    IF TG_TABLE_NAME = 'ledger_entries' THEN
        target := NEW.id;
    ELSE
        target := NEW.entry_id;
    END IF;
    SELECT count(*), coalesce(sum(debit_cop) - sum(credit_cop), 0)
      INTO lines, diff FROM ledger_lines WHERE entry_id = target;
    IF lines < 2 OR diff <> 0 THEN
        RAISE EXCEPTION 'asiento contable descuadrado: %', target;
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER ledger_entries_balanced
    AFTER INSERT ON ledger_entries DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION require_balanced_entry();
CREATE CONSTRAINT TRIGGER ledger_lines_balanced
    AFTER INSERT ON ledger_lines DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION require_balanced_entry();

-- Nada se corrige reescribiendo: se registra un asiento nuevo
CREATE TRIGGER ledger_entries_no_change BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION forbid_history_changes();
CREATE TRIGGER ledger_entries_no_truncate BEFORE TRUNCATE ON ledger_entries
    FOR EACH STATEMENT EXECUTE FUNCTION forbid_history_changes();
CREATE TRIGGER ledger_lines_no_change BEFORE UPDATE OR DELETE ON ledger_lines
    FOR EACH ROW EXECUTE FUNCTION forbid_history_changes();
CREATE TRIGGER ledger_lines_no_truncate BEFORE TRUNCATE ON ledger_lines
    FOR EACH STATEMENT EXECUTE FUNCTION forbid_history_changes();
