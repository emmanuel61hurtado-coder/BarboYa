-- =====================================================================
-- BARBOYA · Migración 002
-- Sincroniza una base de datos creada con una versión anterior de
-- database/schema.sql con los modelos SQLAlchemy actuales.
-- Es idempotente: se puede ejecutar varias veces sin efectos adversos.
--
-- Uso (volumen ya inicializado):
--   docker compose exec -T db psql -U barboya -d barboya \
--     < database/migrations/002_sync_models.sql
-- =====================================================================

DO $$ BEGIN
    CREATE TYPE user_gender AS ENUM ('female', 'male', 'other');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE verification_status AS ENUM ('pending', 'under_review', 'verified', 'needs_update', 'rejected', 'suspended');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE merchant_status AS ENUM ('pending_approval', 'approved', 'rejected', 'suspended');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'pending_confirmation';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'rejected';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'courier_search';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'courier_assigned';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'courier_heading_to_merchant';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'in_transit';

-- users -----------------------------------------------------------------
ALTER TABLE users ADD COLUMN IF NOT EXISTS gender user_gender;

-- driver_profiles -------------------------------------------------------
ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS vehicle_brand TEXT;
ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS vehicle_model TEXT;
ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS vehicle_color TEXT;
ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS soat_doc_url TEXT;
ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS soat_expires_at TIMESTAMPTZ;
ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS tech_doc_url TEXT;
ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS tech_expires_at TIMESTAMPTZ;
ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS verification_status verification_status NOT NULL DEFAULT 'pending';
ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS is_female_driver BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;
ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS verified_by UUID REFERENCES users(id);

-- merchants -------------------------------------------------------------
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS cover_image_url TEXT;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS opening_hours TEXT;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS approval_status merchant_status NOT NULL DEFAULT 'approved';
ALTER TABLE merchants ALTER COLUMN location DROP NOT NULL;

-- trips -----------------------------------------------------------------
ALTER TABLE trips ADD COLUMN IF NOT EXISTS prefer_female_driver BOOLEAN NOT NULL DEFAULT FALSE;

-- orders ----------------------------------------------------------------
ALTER TABLE orders ADD COLUMN IF NOT EXISTS courier_id UUID REFERENCES driver_profiles(user_id);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS status_history JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_pin CHAR(4);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
CREATE INDEX IF NOT EXISTS idx_orders_courier ON orders (courier_id, status);
