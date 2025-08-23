-- =====================================================
-- V106: Simple Fix for User Status Enum Values
-- =====================================================
-- Purpose: Convert user_status enum values to uppercase to match Java enum
-- Pattern: Simple approach using CASE to map values
-- Architecture: Ensures consistency between Java and PostgreSQL enums
-- Standards: Consistent uppercase enum values

-- Drop views that use users table
DROP VIEW IF EXISTS active_supervisor_technician_assignments CASCADE;
DROP VIEW IF EXISTS active_admin_supervisor_assignments CASCADE;
DROP VIEW IF EXISTS v_daily_audit_summary CASCADE;
DROP VIEW IF EXISTS v_user_audit_summary CASCADE;
DROP VIEW IF EXISTS v_sensitive_operations CASCADE;
DROP VIEW IF EXISTS v_recycle_bin CASCADE;
DROP VIEW IF EXISTS v_top_performers CASCADE;
DROP VIEW IF EXISTS v_active_supervisor_schools CASCADE;
DROP VIEW IF EXISTS active_technicians CASCADE;
DROP VIEW IF EXISTS company_usage_summary CASCADE;
DROP VIEW IF EXISTS admin_workload_summary CASCADE;

-- Disable triggers
ALTER TABLE users DISABLE TRIGGER ALL;

-- Add temporary column
ALTER TABLE users ADD COLUMN status_new VARCHAR(30);

-- Copy and transform values
UPDATE users SET status_new = 
    CASE status::text
        WHEN 'active' THEN 'ACTIVE'
        WHEN 'inactive' THEN 'INACTIVE'
        WHEN 'suspended' THEN 'SUSPENDED'
        WHEN 'pending_verification' THEN 'PENDING_VERIFICATION'
        WHEN 'locked' THEN 'LOCKED'
        WHEN 'archived' THEN 'ARCHIVED'
        ELSE UPPER(status::text)
    END;

-- Drop old column
ALTER TABLE users DROP COLUMN status CASCADE;

-- Create new enum type
DROP TYPE IF EXISTS user_status CASCADE;
CREATE TYPE user_status AS ENUM (
    'PENDING_VERIFICATION',
    'ACTIVE', 
    'INACTIVE',
    'SUSPENDED',
    'LOCKED',
    'ARCHIVED'
);

-- Rename and convert column
ALTER TABLE users RENAME COLUMN status_new TO status;
ALTER TABLE users ALTER COLUMN status TYPE user_status USING status::user_status;
ALTER TABLE users ALTER COLUMN status SET NOT NULL;
ALTER TABLE users ALTER COLUMN status SET DEFAULT 'PENDING_VERIFICATION'::user_status;

-- Re-enable triggers
ALTER TABLE users ENABLE TRIGGER ALL;

-- Log completion
DO $$
BEGIN
    RAISE NOTICE 'User status enum values successfully converted to uppercase';
END $$;