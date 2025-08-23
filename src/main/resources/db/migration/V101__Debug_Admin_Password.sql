-- ============================================
-- V101__Debug_Admin_Password.sql
-- Debug admin password and show stored hash
-- ============================================

-- Show current admin users and their password hashes for debugging
DO $$
DECLARE
    user_record RECORD;
BEGIN
    RAISE NOTICE 'DEBUG: Current admin users and their password hashes:';
    
    FOR user_record IN 
        SELECT id, email, password_hash, user_type, status, is_active, is_locked, email_verified
        FROM users 
        WHERE email IN ('admin@cafm.com', 'admin@cafm.local', 'test@cafm.com')
        ORDER BY email
    LOOP
        RAISE NOTICE 'Email: %, Hash: %, UserType: %, Status: %, Active: %, Locked: %, Verified: %',
            user_record.email, 
            user_record.password_hash,
            user_record.user_type,
            user_record.status,
            user_record.is_active,
            user_record.is_locked,
            user_record.email_verified;
    END LOOP;
END $$;