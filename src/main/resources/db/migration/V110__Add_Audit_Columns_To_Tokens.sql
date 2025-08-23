-- ============================================================================
-- V110: Add Audit Columns to Token Tables
-- ============================================================================
-- Purpose: Add missing audit columns to match BaseEntity structure
-- Author: CAFM System
-- Date: 2025-01-15
-- ============================================================================

-- Add missing columns to password_reset_tokens if they don't exist
ALTER TABLE password_reset_tokens 
ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies(id) ON DELETE CASCADE;

ALTER TABLE password_reset_tokens 
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE password_reset_tokens 
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

ALTER TABLE password_reset_tokens 
ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);

ALTER TABLE password_reset_tokens 
ADD COLUMN IF NOT EXISTS modified_by VARCHAR(255);

ALTER TABLE password_reset_tokens 
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Modify ip_address column type to VARCHAR if needed
ALTER TABLE password_reset_tokens 
ALTER COLUMN ip_address TYPE VARCHAR(45) USING ip_address::VARCHAR;

-- Add missing columns to email_verification_tokens if table exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'email_verification_tokens') THEN
        -- Add missing audit columns
        ALTER TABLE email_verification_tokens 
        ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
        
        ALTER TABLE email_verification_tokens 
        ADD COLUMN IF NOT EXISTS modified_by VARCHAR(255);
        
        ALTER TABLE email_verification_tokens 
        ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
    END IF;
END;
$$;

-- Create trigger for updated_at if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger 
        WHERE tgname = 'update_password_reset_tokens_updated_at'
    ) THEN
        CREATE TRIGGER update_password_reset_tokens_updated_at
            BEFORE UPDATE ON password_reset_tokens
            FOR EACH ROW
            EXECUTE FUNCTION update_password_reset_token_updated_at();
    END IF;
END;
$$;

-- Create the trigger function if it doesn't exist
CREATE OR REPLACE FUNCTION update_password_reset_token_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Migration Validation
-- ============================================================================
DO $$
BEGIN
    -- Verify all audit columns exist on password_reset_tokens
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'password_reset_tokens' 
        AND column_name IN ('created_by', 'modified_by', 'version', 'updated_at', 'deleted_at', 'company_id')
        HAVING COUNT(*) = 6
    ) THEN
        RAISE EXCEPTION 'Not all audit columns were added to password_reset_tokens table';
    END IF;
    
    RAISE NOTICE 'Audit columns migration completed successfully';
END;
$$;