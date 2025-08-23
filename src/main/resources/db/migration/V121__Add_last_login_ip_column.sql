-- Add missing last_login_ip column to users table
-- This fixes the authentication issue where JPA entity expects this column

ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_ip VARCHAR(45);

-- Add comment for documentation
COMMENT ON COLUMN users.last_login_ip IS 'IP address of user last login for security tracking';