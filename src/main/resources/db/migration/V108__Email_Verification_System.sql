-- ============================================================================
-- V10: Email Verification System
-- ============================================================================
-- Purpose: Add email verification functionality with secure token management
-- Author: CAFM System
-- Date: 2025-01-15
-- ============================================================================

-- Add email_verified_at column to users table if it doesn't exist
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP;

-- Create index for email verification status
CREATE INDEX IF NOT EXISTS idx_users_email_verified 
ON users(email_verified, email_verified_at) 
WHERE email_verified = true;

-- ============================================================================
-- Email Verification Tokens Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified BOOLEAN DEFAULT false,
    verified_at TIMESTAMP,
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
    CONSTRAINT check_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT check_verified_at CHECK (
        (verified = false AND verified_at IS NULL) OR 
        (verified = true AND verified_at IS NOT NULL)
    )
);

-- Indexes for performance
CREATE INDEX idx_email_token_hash ON email_verification_tokens(token_hash) 
WHERE verified = false AND deleted_at IS NULL;

CREATE INDEX idx_email_expires ON email_verification_tokens(expires_at) 
WHERE verified = false AND deleted_at IS NULL;

CREATE INDEX idx_email_user ON email_verification_tokens(user_id) 
WHERE deleted_at IS NULL;

CREATE INDEX idx_email_verified ON email_verification_tokens(verified, verified_at) 
WHERE deleted_at IS NULL;

CREATE INDEX idx_email_address ON email_verification_tokens(email) 
WHERE verified = false AND deleted_at IS NULL;

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_email_verification_token_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_email_verification_tokens_updated_at
    BEFORE UPDATE ON email_verification_tokens
    FOR EACH ROW
    EXECUTE FUNCTION update_email_verification_token_updated_at();

-- ============================================================================
-- Helper Functions
-- ============================================================================

-- Function to clean up expired email verification tokens
CREATE OR REPLACE FUNCTION cleanup_expired_email_tokens()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM email_verification_tokens
    WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '7 days'
    OR (verified = true AND verified_at < CURRENT_TIMESTAMP - INTERVAL '30 days');
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to check if user's email is verified
CREATE OR REPLACE FUNCTION is_email_verified(user_uuid UUID)
RETURNS BOOLEAN AS $$
DECLARE
    verified_status BOOLEAN;
BEGIN
    SELECT email_verified INTO verified_status
    FROM users
    WHERE id = user_uuid;
    
    RETURN COALESCE(verified_status, false);
END;
$$ LANGUAGE plpgsql;

-- Function to get verification statistics
CREATE OR REPLACE FUNCTION get_email_verification_stats(company_uuid UUID DEFAULT NULL)
RETURNS TABLE(
    total_users BIGINT,
    verified_users BIGINT,
    unverified_users BIGINT,
    pending_tokens BIGINT,
    expired_tokens BIGINT,
    verification_rate NUMERIC(5,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(DISTINCT u.id) AS total_users,
        COUNT(DISTINCT u.id) FILTER (WHERE u.email_verified = true) AS verified_users,
        COUNT(DISTINCT u.id) FILTER (WHERE u.email_verified = false OR u.email_verified IS NULL) AS unverified_users,
        COUNT(DISTINCT t.id) FILTER (WHERE t.verified = false AND t.expires_at > CURRENT_TIMESTAMP) AS pending_tokens,
        COUNT(DISTINCT t.id) FILTER (WHERE t.verified = false AND t.expires_at <= CURRENT_TIMESTAMP) AS expired_tokens,
        CASE 
            WHEN COUNT(DISTINCT u.id) > 0 
            THEN ROUND(COUNT(DISTINCT u.id) FILTER (WHERE u.email_verified = true)::NUMERIC * 100.0 / COUNT(DISTINCT u.id), 2)
            ELSE 0
        END AS verification_rate
    FROM users u
    LEFT JOIN email_verification_tokens t ON u.id = t.user_id
    WHERE (company_uuid IS NULL OR u.company_id = company_uuid)
    AND u.deleted_at IS NULL;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Grant Permissions
-- ============================================================================
-- Grant necessary permissions to application role (adjust role name as needed)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON email_verification_tokens TO cafm_app;
-- GRANT USAGE ON SEQUENCE email_verification_tokens_id_seq TO cafm_app;

-- ============================================================================
-- Add Comments
-- ============================================================================
COMMENT ON TABLE email_verification_tokens IS 'Stores email verification tokens for user email confirmation';
COMMENT ON COLUMN email_verification_tokens.token_hash IS 'Hashed verification token for security';
COMMENT ON COLUMN email_verification_tokens.email IS 'Email address being verified';
COMMENT ON COLUMN email_verification_tokens.expires_at IS 'Token expiration timestamp';
COMMENT ON COLUMN email_verification_tokens.verified IS 'Whether the email has been verified';
COMMENT ON COLUMN email_verification_tokens.verified_at IS 'Timestamp when email was verified';
COMMENT ON COLUMN email_verification_tokens.ip_address IS 'IP address from which verification was requested';

COMMENT ON COLUMN users.email_verified_at IS 'Timestamp when user email was verified';

COMMENT ON FUNCTION cleanup_expired_email_tokens() IS 'Removes expired and old verified email tokens';
COMMENT ON FUNCTION is_email_verified(UUID) IS 'Checks if a user has verified their email';
COMMENT ON FUNCTION get_email_verification_stats(UUID) IS 'Returns email verification statistics for monitoring';

-- ============================================================================
-- Migration Validation
-- ============================================================================
DO $$
BEGIN
    -- Verify table creation
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                   WHERE table_name = 'email_verification_tokens') THEN
        RAISE EXCEPTION 'Table email_verification_tokens was not created';
    END IF;
    
    -- Verify column addition
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'users' AND column_name = 'email_verified_at') THEN
        RAISE EXCEPTION 'Column email_verified_at was not added to users table';
    END IF;
    
    RAISE NOTICE 'Email verification system migration completed successfully';
END;
$$;