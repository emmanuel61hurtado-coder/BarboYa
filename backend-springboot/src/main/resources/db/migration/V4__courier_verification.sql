-- Verificacion de domiciliarios

CREATE TABLE courier_profiles (
    user_id         UUID PRIMARY KEY REFERENCES users (id),
    document_number VARCHAR(20) NOT NULL,
    vehicle_type    VARCHAR(12) NOT NULL CHECK (vehicle_type IN ('BICYCLE', 'MOTORCYCLE', 'CAR')),
    plate           VARCHAR(10),
    -- Cuando autorizo el tratamiento de sus datos, incluida la foto del rostro
    consent_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    version         BIGINT      NOT NULL DEFAULT 0,
    -- Una persona, una cuenta
    CONSTRAINT courier_profiles_document_key UNIQUE (document_number),
    CONSTRAINT courier_profiles_plate CHECK (vehicle_type = 'BICYCLE' OR plate IS NOT NULL)
);

-- El archivo se guarda cifrado fuera de la base; aqui solo su referencia
CREATE TABLE courier_documents (
    id           UUID PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES courier_profiles (user_id),
    type         VARCHAR(20) NOT NULL CHECK (type IN (
                     'IDENTITY', 'FACE_PHOTO', 'DRIVER_LICENSE', 'SOAT', 'TECHNICAL_REVIEW')),
    status       VARCHAR(10) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    storage_key  VARCHAR(36) NOT NULL,
    content_type VARCHAR(40) NOT NULL,
    size_bytes   INTEGER     NOT NULL CHECK (size_bytes > 0),
    expires_on   DATE,
    review_note  VARCHAR(300),
    reviewed_by  UUID,
    reviewed_at  TIMESTAMPTZ,
    uploaded_at  TIMESTAMPTZ NOT NULL,
    version      BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT courier_documents_type_key UNIQUE (user_id, type),
    CONSTRAINT courier_documents_storage_key UNIQUE (storage_key)
);
