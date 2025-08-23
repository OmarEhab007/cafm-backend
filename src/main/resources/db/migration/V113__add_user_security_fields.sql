-- Add security and verification fields to users table
-- Purpose: Support login attempt tracking, password change enforcement, and verification codes
-- Author: CAFM System
-- Date: 2025-01-17

-- Add failed login attempts tracking
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER DEFAULT 0;

-- Add password change requirement flag
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_change_required BOOLEAN DEFAULT FALSE;

-- Add verification code fields for email/phone verification
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_code VARCHAR(10);
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_code_expiry TIMESTAMP;

-- Add constraints
ALTER TABLE users ADD CONSTRAINT check_failed_login_attempts_non_negative 
    CHECK (failed_login_attempts >= 0);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_verification_code 
    ON users(verification_code) 
    WHERE verification_code IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_password_change_required 
    ON users(password_change_required) 
    WHERE password_change_required = TRUE;

-- Add comments for documentation
COMMENT ON COLUMN users.failed_login_attempts IS 'Number of consecutive failed login attempts';
COMMENT ON COLUMN users.password_change_required IS 'Flag indicating if user must change password on next login';
COMMENT ON COLUMN users.verification_code IS 'Temporary code for email/phone verification';
COMMENT ON COLUMN users.verification_code_expiry IS 'Expiration timestamp for verification code';