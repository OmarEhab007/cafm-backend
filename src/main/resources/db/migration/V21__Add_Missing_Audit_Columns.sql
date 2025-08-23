-- V21__Add_Missing_Audit_Columns.sql
-- Add missing audit columns to support entity auditing

-- ============================================
-- Add audit columns to users table
-- ============================================
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS created_by UUID,
ADD COLUMN IF NOT EXISTS modified_by UUID,
ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(500),
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- ============================================
-- Add audit columns to companies table  
-- ============================================
ALTER TABLE companies 
ADD COLUMN IF NOT EXISTS modified_by UUID,
ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(500),
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- ============================================
-- Add audit columns to schools table
-- ============================================
ALTER TABLE schools 
ADD COLUMN IF NOT EXISTS modified_by UUID,
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- ============================================
-- Add audit columns to reports table
-- ============================================
ALTER TABLE reports 
ADD COLUMN IF NOT EXISTS modified_by UUID,
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- ============================================
-- Add audit columns to work_orders table
-- ============================================
ALTER TABLE work_orders 
ADD COLUMN IF NOT EXISTS modified_by UUID,
ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(500),
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- ============================================
-- Add audit columns to assets table (if exists)
-- ============================================
DO $$ 
BEGIN 
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'assets') THEN
        ALTER TABLE assets 
        ADD COLUMN IF NOT EXISTS created_by UUID,
        ADD COLUMN IF NOT EXISTS modified_by UUID,
        ADD COLUMN IF NOT EXISTS deleted_by UUID,
        ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(500),
        ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
    END IF;
END $$;

-- ============================================
-- Add audit columns to inventory_items table (if exists)
-- ============================================
DO $$ 
BEGIN 
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'inventory_items') THEN
        ALTER TABLE inventory_items 
        ADD COLUMN IF NOT EXISTS created_by UUID,
        ADD COLUMN IF NOT EXISTS modified_by UUID,
        ADD COLUMN IF NOT EXISTS deleted_by UUID,
        ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(500),
        ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
    END IF;
END $$;

-- ============================================
-- Add audit columns to inventory_transactions table (if exists)
-- ============================================
DO $$ 
BEGIN 
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'inventory_transactions') THEN
        ALTER TABLE inventory_transactions 
        ADD COLUMN IF NOT EXISTS modified_by UUID,
        ADD COLUMN IF NOT EXISTS deleted_by UUID,
        ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
        ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(500),
        ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
    END IF;
END $$;

-- ============================================
-- Add audit columns to work_order_tasks table (if exists)
-- ============================================
DO $$ 
BEGIN 
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'work_order_tasks') THEN
        ALTER TABLE work_order_tasks 
        ADD COLUMN IF NOT EXISTS created_by UUID,
        ADD COLUMN IF NOT EXISTS modified_by UUID,
        ADD COLUMN IF NOT EXISTS deleted_by UUID,
        ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
        ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(500),
        ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
    END IF;
END $$;

-- ============================================
-- Add audit columns to work_order_materials table (if exists)
-- ============================================
DO $$ 
BEGIN 
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'work_order_materials') THEN
        ALTER TABLE work_order_materials 
        ADD COLUMN IF NOT EXISTS created_by UUID,
        ADD COLUMN IF NOT EXISTS modified_by UUID,
        ADD COLUMN IF NOT EXISTS deleted_by UUID,
        ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
        ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(500),
        ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
    END IF;
END $$;

-- ============================================
-- Add audit columns to work_order_attachments table (if exists)
-- ============================================
DO $$ 
BEGIN 
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'work_order_attachments') THEN
        ALTER TABLE work_order_attachments 
        ADD COLUMN IF NOT EXISTS created_by UUID,
        ADD COLUMN IF NOT EXISTS modified_by UUID,
        ADD COLUMN IF NOT EXISTS deleted_by UUID,
        ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
        ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(500),
        ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
    END IF;
END $$;

-- ============================================
-- Add audit columns to supervisor_attendance table (if exists)
-- ============================================
DO $$ 
BEGIN 
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'supervisor_attendance') THEN
        ALTER TABLE supervisor_attendance 
        ADD COLUMN IF NOT EXISTS created_by UUID,
        ADD COLUMN IF NOT EXISTS modified_by UUID,
        ADD COLUMN IF NOT EXISTS deleted_by UUID,
        ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
        ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(500),
        ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
    END IF;
END $$;

-- ============================================
-- Add audit columns to asset_categories table (if exists)
-- ============================================
DO $$ 
BEGIN 
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'asset_categories') THEN
        ALTER TABLE asset_categories 
        ADD COLUMN IF NOT EXISTS created_by UUID,
        ADD COLUMN IF NOT EXISTS modified_by UUID,
        ADD COLUMN IF NOT EXISTS deleted_by UUID,
        ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
        ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(500),
        ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
    END IF;
END $$;

-- ============================================
-- Add audit columns to inventory_categories table (if exists)
-- ============================================
DO $$ 
BEGIN 
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'inventory_categories') THEN
        ALTER TABLE inventory_categories 
        ADD COLUMN IF NOT EXISTS created_by UUID,
        ADD COLUMN IF NOT EXISTS modified_by UUID,
        ADD COLUMN IF NOT EXISTS deleted_by UUID,
        ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
        ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(500),
        ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
    END IF;
END $$;

-- ============================================
-- Add audit columns to asset_maintenance table (if exists)
-- ============================================
DO $$ 
BEGIN 
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'asset_maintenance') THEN
        ALTER TABLE asset_maintenance 
        ADD COLUMN IF NOT EXISTS created_by UUID,
        ADD COLUMN IF NOT EXISTS modified_by UUID,
        ADD COLUMN IF NOT EXISTS deleted_by UUID,
        ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
        ADD COLUMN IF NOT EXISTS deletion_reason VARCHAR(500),
        ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
    END IF;
END $$;

-- ============================================
-- Update existing records to have default version
-- ============================================
UPDATE users SET version = 0 WHERE version IS NULL;
UPDATE companies SET version = 0 WHERE version IS NULL;
UPDATE schools SET version = 0 WHERE version IS NULL;
UPDATE reports SET version = 0 WHERE version IS NULL;
UPDATE work_orders SET version = 0 WHERE version IS NULL;

-- ============================================
-- Add comments for documentation
-- ============================================
COMMENT ON COLUMN users.created_by IS 'ID of the user who created this record';
COMMENT ON COLUMN users.modified_by IS 'ID of the user who last modified this record';
COMMENT ON COLUMN users.deletion_reason IS 'Reason for soft deletion';
COMMENT ON COLUMN users.version IS 'Version number for optimistic locking';