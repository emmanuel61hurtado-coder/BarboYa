-- Identidad, sesiones, codigos de verificacion y auditoria

CREATE TABLE users (
    id                UUID PRIMARY KEY,
    full_name         VARCHAR(120) NOT NULL,
    email             VARCHAR(254) NOT NULL,
    phone             VARCHAR(16)  NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    role              VARCHAR(20)  NOT NULL CHECK (role IN (
                          'CUSTOMER', 'COURIER', 'MOTO_DRIVER', 'CAR_DRIVER',
                          'RESTAURANT', 'STORE', 'ADMIN', 'SUPERADMIN')),
    status            VARCHAR(24)  NOT NULL CHECK (status IN (
                          'ACTIVE', 'PENDING_VERIFICATION', 'SUSPENDED')),
    email_verified_at TIMESTAMPTZ,
    failed_logins     INTEGER      NOT NULL DEFAULT 0 CHECK (failed_logins >= 0),
    locked_until      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT users_email_key UNIQUE (email),
    CONSTRAINT users_phone_key UNIQUE (phone),
    CONSTRAINT users_email_lowercase CHECK (email = lower(email))
);

CREATE TABLE auth_sessions (
    id           UUID PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    revoked_at   TIMESTAMPTZ,
    ip           VARCHAR(64),
    user_agent   VARCHAR(255)
);
CREATE INDEX idx_auth_sessions_user ON auth_sessions (user_id, created_at DESC);

-- Solo se guarda la huella SHA-256: el token nunca queda en la base
CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY,
    session_id UUID        NOT NULL REFERENCES auth_sessions (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    CONSTRAINT refresh_tokens_hash_key UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_tokens_session ON refresh_tokens (session_id);

CREATE TABLE verification_codes (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose     VARCHAR(32) NOT NULL CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    code_hash   VARCHAR(64) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    attempts    INTEGER     NOT NULL DEFAULT 0 CHECK (attempts >= 0)
);
CREATE INDEX idx_verification_codes_lookup ON verification_codes (user_id, purpose, created_at DESC);

-- Sin claves foraneas: el historial sobrevive a lo que describe
CREATE TABLE audit_events (
    id         UUID PRIMARY KEY,
    actor_id   UUID,
    target_id  UUID,
    action     VARCHAR(40) NOT NULL,
    detail     VARCHAR(500),
    ip         VARCHAR(64),
    user_agent VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_audit_events_actor ON audit_events (actor_id, created_at DESC);
CREATE INDEX idx_audit_events_target ON audit_events (target_id, created_at DESC);
CREATE INDEX idx_audit_events_created ON audit_events (created_at DESC);

CREATE FUNCTION forbid_audit_changes() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit_events es de solo insercion';
END;
$$;

CREATE TRIGGER audit_events_no_change
    BEFORE UPDATE OR DELETE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION forbid_audit_changes();

CREATE TRIGGER audit_events_no_truncate
    BEFORE TRUNCATE ON audit_events
    FOR EACH STATEMENT EXECUTE FUNCTION forbid_audit_changes();
