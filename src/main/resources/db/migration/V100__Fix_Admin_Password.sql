-- ============================================
-- V100__Fix_Admin_Password.sql  
-- Fix admin password using the original working hash from V1
-- ============================================

-- Use the original working hash from V1 migration for "Admin@123"
-- This hash: '$2a$10$qn5QWEpqh3EqMRNeqnSjLuNFUgIBHx9JKlKjBg.ZGNh4Qfvb7ZdNe'

-- Update admin@cafm.com with original working hash
UPDATE users 
SET 
    password_hash = '$2a$10$qn5QWEpqh3EqMRNeqnSjLuNFUgIBHx9JKlKjBg.ZGNh4Qfvb7ZdNe',
    password_changed_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'admin@cafm.com';

-- Update admin@cafm.local with same working hash
UPDATE users 
SET 
    password_hash = '$2a$10$qn5QWEpqh3EqMRNeqnSjLuNFUgIBHx9JKlKjBg.ZGNh4Qfvb7ZdNe',
    password_changed_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'admin@cafm.local';

-- Update test@cafm.com if it exists  
UPDATE users 
SET 
    password_hash = '$2a$10$qn5QWEpqh3EqMRNeqnSjLuNFUgIBHx9JKlKjBg.ZGNh4Qfvb7ZdNe',
    password_changed_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'test@cafm.com';

-- Log the password update
DO $$
BEGIN
    RAISE NOTICE 'Admin users updated with password: Admin@123';
    RAISE NOTICE 'Affected emails: admin@cafm.com, admin@cafm.local, test@cafm.com';
    RAISE NOTICE 'Use password: Admin@123 for all admin accounts';
END $$;