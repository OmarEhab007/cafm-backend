-- ============================================
-- V103__Generate_New_Hash.sql
-- Set a simple test password that we can verify
-- ============================================

-- Set simple password "admin123" for testing
-- Using PostgreSQL's crypt function to generate BCrypt hash
UPDATE users 
SET 
    password_hash = crypt('admin123', gen_salt('bf')),
    password_changed_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE email IN ('admin@cafm.com', 'admin@cafm.local', 'test@cafm.com');

-- Log the new password
DO $$
BEGIN
    RAISE NOTICE 'Updated admin passwords to simple "admin123" for testing';
    RAISE NOTICE 'This is temporary for debugging the authentication issue';
END $$;