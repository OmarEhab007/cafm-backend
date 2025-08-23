-- ============================================================================
-- V109: Password Reset Tokens Table
-- ============================================================================
-- Purpose: Add password reset functionality with secure token management
-- Author: CAFM System
-- Date: 2025-01-15
-- ============================================================================

-- ============================================================================
-- Password Reset Tokens Table
-- ============================================================================
-- Skip this migration if table already exists (it was created in a previous migration)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'password_reset_tokens') THEN
        RAISE NOTICE 'Table password_reset_tokens already exists, skipping creation';
    ELSE
        CREATE TABLE password_reset_tokens (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            token_hash VARCHAR(255) NOT NULL UNIQUE,
            expires_at TIMESTAMP NOT NULL,
            used BOOLEAN DEFAULT false,
            used_at TIMESTAMP,
            ip_address VARCHAR(45),
            
            -- Multi-tenant support
            company_id UUID REFERENCES companies(id) ON DELETE CASCADE,
            
            -- Audit fields (matching BaseEntity)
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP,
            created_by VARCHAR(255),
            modified_by VARCHAR(255),
            version BIGINT DEFAULT 0,
            
            -- Constraints
            CONSTRAINT check_used_at CHECK (
                (used = false AND used_at IS NULL) OR 
                (used = true AND used_at IS NOT NULL)
            )
        );
        
        -- Indexes for performance (only create if table was just created)
        CREATE INDEX idx_reset_token_hash ON password_reset_tokens(token_hash) 
        WHERE used = false AND deleted_at IS NULL;

        CREATE INDEX idx_reset_expires ON password_reset_tokens(expires_at) 
        WHERE used = false AND deleted_at IS NULL;

        CREATE INDEX idx_reset_user ON password_reset_tokens(user_id) 
        WHERE deleted_at IS NULL;

        CREATE INDEX idx_reset_used ON password_reset_tokens(used, used_at) 
        WHERE deleted_at IS NULL;
    END IF;
END;
$$;

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_password_reset_token_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

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

-- ============================================================================
-- Helper Functions
-- ============================================================================

-- Function to clean up expired password reset tokens
CREATE OR REPLACE FUNCTION cleanup_expired_reset_tokens()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM password_reset_tokens
    WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '7 days'
    OR (used = true AND used_at < CURRENT_TIMESTAMP - INTERVAL '30 days');
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to check if user has active reset token
CREATE OR REPLACE FUNCTION has_active_reset_token(user_uuid UUID)
RETURNS BOOLEAN AS $$
DECLARE
    has_token BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1 
        FROM password_reset_tokens
        WHERE user_id = user_uuid
        AND used = false
        AND expires_at > CURRENT_TIMESTAMP
        AND deleted_at IS NULL
    ) INTO has_token;
    
    RETURN has_token;
END;
$$ LANGUAGE plpgsql;

-- Function to invalidate all reset tokens for a user
CREATE OR REPLACE FUNCTION invalidate_user_reset_tokens(user_uuid UUID)
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER;
BEGIN
    UPDATE password_reset_tokens
    SET used = true, 
        used_at = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP
    WHERE user_id = user_uuid
    AND used = false
    AND deleted_at IS NULL;
    
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- Function to get reset token statistics
CREATE OR REPLACE FUNCTION get_reset_token_stats(company_uuid UUID DEFAULT NULL)
RETURNS TABLE(
    total_tokens BIGINT,
    used_tokens BIGINT,
    active_tokens BIGINT,
    expired_tokens BIGINT,
    avg_time_to_use INTERVAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) AS total_tokens,
        COUNT(*) FILTER (WHERE used = true) AS used_tokens,
        COUNT(*) FILTER (WHERE used = false AND expires_at > CURRENT_TIMESTAMP) AS active_tokens,
        COUNT(*) FILTER (WHERE used = false AND expires_at <= CURRENT_TIMESTAMP) AS expired_tokens,
        AVG(used_at - created_at) FILTER (WHERE used = true) AS avg_time_to_use
    FROM password_reset_tokens
    WHERE (company_uuid IS NULL OR company_id = company_uuid)
    AND deleted_at IS NULL;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Add Comments
-- ============================================================================
COMMENT ON TABLE password_reset_tokens IS 'Stores password reset tokens for secure password recovery';
COMMENT ON COLUMN password_reset_tokens.token_hash IS 'Hashed reset token for security';
COMMENT ON COLUMN password_reset_tokens.expires_at IS 'Token expiration timestamp';
COMMENT ON COLUMN password_reset_tokens.used IS 'Whether the token has been used';
COMMENT ON COLUMN password_reset_tokens.used_at IS 'Timestamp when token was used';
COMMENT ON COLUMN password_reset_tokens.ip_address IS 'IP address from which reset was requested';

COMMENT ON FUNCTION cleanup_expired_reset_tokens() IS 'Removes expired and old used reset tokens';
COMMENT ON FUNCTION has_active_reset_token(UUID) IS 'Checks if a user has an active reset token';
COMMENT ON FUNCTION invalidate_user_reset_tokens(UUID) IS 'Invalidates all reset tokens for a user';
COMMENT ON FUNCTION get_reset_token_stats(UUID) IS 'Returns password reset statistics for monitoring';

-- ============================================================================
-- Migration Validation
-- ============================================================================
DO $$
BEGIN
    -- Verify table exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                   WHERE table_name = 'password_reset_tokens') THEN
        RAISE EXCEPTION 'Table password_reset_tokens was not created';
    END IF;
    
    -- Since the table already existed, we just need to ensure it has the basic structure
    -- The audit columns will be added in V110
    RAISE NOTICE 'Password reset tokens migration completed successfully';
END;
$$;