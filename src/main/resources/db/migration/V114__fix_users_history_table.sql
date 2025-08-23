-- Fix users_history table by adding missing columns
-- Purpose: Add user_type and other missing columns to users_history table
-- Pattern: ALTER TABLE to add missing columns that exist in users table
-- Standards: Idempotent migration with proper column types

-- Add user_type column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users_history' 
        AND column_name = 'user_type'
    ) THEN
        ALTER TABLE users_history 
        ADD COLUMN user_type user_type;
    END IF;
END $$;

-- Add status column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users_history' 
        AND column_name = 'status'
    ) THEN
        ALTER TABLE users_history 
        ADD COLUMN status user_status;
    END IF;
END $$;

-- Add company_id column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users_history' 
        AND column_name = 'company_id'
    ) THEN
        ALTER TABLE users_history 
        ADD COLUMN company_id UUID;
    END IF;
END $$;

-- Add other missing columns
DO $$
BEGIN
    -- Add password_hash if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users_history' 
        AND column_name = 'password_hash'
    ) THEN
        ALTER TABLE users_history 
        ADD COLUMN password_hash VARCHAR(255);
    END IF;

    -- Add email_verified if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users_history' 
        AND column_name = 'email_verified'
    ) THEN
        ALTER TABLE users_history 
        ADD COLUMN email_verified BOOLEAN DEFAULT FALSE;
    END IF;

    -- Add phone_verified if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users_history' 
        AND column_name = 'phone_verified'
    ) THEN
        ALTER TABLE users_history 
        ADD COLUMN phone_verified BOOLEAN DEFAULT FALSE;
    END IF;

    -- Add failed_login_attempts if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users_history' 
        AND column_name = 'failed_login_attempts'
    ) THEN
        ALTER TABLE users_history 
        ADD COLUMN failed_login_attempts INTEGER DEFAULT 0;
    END IF;

    -- Add password_change_required if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users_history' 
        AND column_name = 'password_change_required'
    ) THEN
        ALTER TABLE users_history 
        ADD COLUMN password_change_required BOOLEAN DEFAULT FALSE;
    END IF;

    -- Add password_changed_at if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users_history' 
        AND column_name = 'password_changed_at'
    ) THEN
        ALTER TABLE users_history 
        ADD COLUMN password_changed_at TIMESTAMP WITH TIME ZONE;
    END IF;

    -- Add created_at if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users_history' 
        AND column_name = 'created_at'
    ) THEN
        ALTER TABLE users_history 
        ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;
    END IF;

    -- Add updated_at if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users_history' 
        AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE users_history 
        ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;
    END IF;

    -- Add last_login_at if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users_history' 
        AND column_name = 'last_login_at'
    ) THEN
        ALTER TABLE users_history 
        ADD COLUMN last_login_at TIMESTAMP WITH TIME ZONE;
    END IF;

    -- Add deleted_at if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users_history' 
        AND column_name = 'deleted_at'
    ) THEN
        ALTER TABLE users_history 
        ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
    END IF;

    -- Add deleted_by if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users_history' 
        AND column_name = 'deleted_by'
    ) THEN
        ALTER TABLE users_history 
        ADD COLUMN deleted_by UUID;
    END IF;

    -- Add deletion_reason if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users_history' 
        AND column_name = 'deletion_reason'
    ) THEN
        ALTER TABLE users_history 
        ADD COLUMN deletion_reason TEXT;
    END IF;
END $$;

-- Recreate or update the trigger function to handle all columns
CREATE OR REPLACE FUNCTION users_history_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' OR TG_OP = 'DELETE' THEN
        INSERT INTO users_history (
            id, version_number, email, username, user_type, status,
            first_name, last_name, phone, employee_id, department, position,
            company_id, is_active, is_locked, email_verified, phone_verified,
            password_hash, failed_login_attempts, password_change_required,
            password_changed_at, created_at, updated_at, last_login_at,
            deleted_at, deleted_by, deletion_reason,
            valid_from, valid_to, modified_by, modification_reason
        )
        VALUES (
            OLD.id, COALESCE(OLD.version, 0), OLD.email, OLD.username, OLD.user_type, OLD.status,
            OLD.first_name, OLD.last_name, OLD.phone, OLD.employee_id, OLD.department, OLD.position,
            OLD.company_id, OLD.is_active, OLD.is_locked, OLD.email_verified, OLD.phone_verified,
            OLD.password_hash, OLD.failed_login_attempts, OLD.password_change_required,
            OLD.password_changed_at, OLD.created_at, OLD.updated_at, OLD.last_login_at,
            OLD.deleted_at, OLD.deleted_by, OLD.deletion_reason,
            COALESCE(OLD.updated_at, OLD.created_at, NOW()), 
            CASE 
                WHEN TG_OP = 'DELETE' THEN NOW()
                ELSE NULL 
            END,
            CASE 
                WHEN TG_OP = 'UPDATE' THEN NEW.modified_by
                ELSE OLD.modified_by
            END,
            CASE 
                WHEN TG_OP = 'DELETE' THEN 'Record deleted'
                ELSE 'Record updated'
            END
        );
    END IF;
    
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Ensure trigger exists
DROP TRIGGER IF EXISTS users_history_update_trigger ON users;
CREATE TRIGGER users_history_update_trigger
    AFTER UPDATE OR DELETE ON users
    FOR EACH ROW
    EXECUTE FUNCTION users_history_trigger();

-- Add comment explaining the purpose
COMMENT ON TABLE users_history IS 'Audit history table for tracking changes to user records';