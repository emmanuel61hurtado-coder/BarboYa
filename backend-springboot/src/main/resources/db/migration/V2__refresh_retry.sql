-- Permite repetir una renovacion cuya respuesta se perdio, sin dejar de
-- detectar la reutilizacion de un token copiado.

-- Token del que nacio este
ALTER TABLE refresh_tokens ADD COLUMN parent_id UUID;
-- Reemplazado antes de usarse; presentarlo cierra la sesion
ALTER TABLE refresh_tokens ADD COLUMN discarded_at TIMESTAMPTZ;

CREATE INDEX idx_refresh_tokens_parent ON refresh_tokens (parent_id);
