-- ============================================
-- V102__Fix_Admin_User_Type.sql
-- Fix admin@cafm.com user type from viewer to admin
-- ============================================

-- Update admin@cafm.com to have proper admin user type
UPDATE users 
SET 
    user_type = 'admin',
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'admin@cafm.com';

-- Ensure all admin users have ROLE_ADMIN in user_roles table
-- First, get the ROLE_ADMIN role ID
DO $$
DECLARE
    admin_role_id UUID;
    admin_user_id UUID;
BEGIN
    -- Get admin role ID
    SELECT id INTO admin_role_id FROM roles WHERE name = 'ROLE_ADMIN';
    
    IF admin_role_id IS NOT NULL THEN
        -- Add ROLE_ADMIN to admin@cafm.com if not exists
        SELECT id INTO admin_user_id FROM users WHERE email = 'admin@cafm.com';
        IF admin_user_id IS NOT NULL THEN
            INSERT INTO user_roles (user_id, role_id) 
            VALUES (admin_user_id, admin_role_id)
            ON CONFLICT (user_id, role_id) DO NOTHING;
        END IF;
        
        -- Add ROLE_ADMIN to admin@cafm.local if not exists
        SELECT id INTO admin_user_id FROM users WHERE email = 'admin@cafm.local';
        IF admin_user_id IS NOT NULL THEN
            INSERT INTO user_roles (user_id, role_id) 
            VALUES (admin_user_id, admin_role_id)
            ON CONFLICT (user_id, role_id) DO NOTHING;
        END IF;
        
        -- Add ROLE_ADMIN to test@cafm.com if not exists
        SELECT id INTO admin_user_id FROM users WHERE email = 'test@cafm.com';
        IF admin_user_id IS NOT NULL THEN
            INSERT INTO user_roles (user_id, role_id) 
            VALUES (admin_user_id, admin_role_id)
            ON CONFLICT (user_id, role_id) DO NOTHING;
        END IF;
        
        RAISE NOTICE 'Admin roles assigned successfully to all admin users';
    ELSE
        RAISE NOTICE 'ROLE_ADMIN not found in roles table';
    END IF;
END $$;

-- Log the fix
DO $$
BEGIN
    RAISE NOTICE 'Fixed admin@cafm.com user type from viewer to admin';
    RAISE NOTICE 'Ensured all admin users have ROLE_ADMIN assigned';
END $$;