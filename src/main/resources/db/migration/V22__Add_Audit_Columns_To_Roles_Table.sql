-- V22__Add_Audit_Columns_To_Roles_Table.sql
-- Add missing audit columns to roles table to support BaseEntity audit fields

-- ============================================
-- Add audit columns to roles table
-- ============================================
ALTER TABLE roles 
ADD COLUMN IF NOT EXISTS created_by UUID,
ADD COLUMN IF NOT EXISTS modified_by UUID,
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- ============================================
-- Update existing records to have default version
-- ============================================
UPDATE roles SET version = 0 WHERE version IS NULL;

-- ============================================
-- Add comments for documentation
-- ============================================
COMMENT ON COLUMN roles.created_by IS 'ID of the user who created this role';
COMMENT ON COLUMN roles.modified_by IS 'ID of the user who last modified this role';
COMMENT ON COLUMN roles.version IS 'Version number for optimistic locking';