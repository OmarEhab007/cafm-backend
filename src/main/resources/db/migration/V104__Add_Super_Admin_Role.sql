-- ============================================
-- V104__Add_Super_Admin_Role.sql
-- Add SUPER_ADMIN role and assign it to admin users
-- ============================================

-- Create SUPER_ADMIN role if it doesn't exist
INSERT INTO roles (name, description) 
VALUES ('ROLE_SUPER_ADMIN', 'Super Administrator with complete system access including multi-tenant operations')
ON CONFLICT (name) DO NOTHING;

-- Add SUPER_ADMIN role to admin users
DO $$
DECLARE
    super_admin_role_id UUID;
    admin_user_id UUID;
BEGIN
    -- Get SUPER_ADMIN role ID
    SELECT id INTO super_admin_role_id FROM roles WHERE name = 'ROLE_SUPER_ADMIN';
    
    IF super_admin_role_id IS NOT NULL THEN
        -- Add ROLE_SUPER_ADMIN to admin@cafm.com
        SELECT id INTO admin_user_id FROM users WHERE email = 'admin@cafm.com';
        IF admin_user_id IS NOT NULL THEN
            INSERT INTO user_roles (user_id, role_id) 
            VALUES (admin_user_id, super_admin_role_id)
            ON CONFLICT (user_id, role_id) DO NOTHING;
            RAISE NOTICE 'Added SUPER_ADMIN role to admin@cafm.com';
        END IF;
        
        -- Add ROLE_SUPER_ADMIN to admin@cafm.local
        SELECT id INTO admin_user_id FROM users WHERE email = 'admin@cafm.local';
        IF admin_user_id IS NOT NULL THEN
            INSERT INTO user_roles (user_id, role_id) 
            VALUES (admin_user_id, super_admin_role_id)
            ON CONFLICT (user_id, role_id) DO NOTHING;
            RAISE NOTICE 'Added SUPER_ADMIN role to admin@cafm.local';
        END IF;
        
        -- Add ROLE_SUPER_ADMIN to test@cafm.com
        SELECT id INTO admin_user_id FROM users WHERE email = 'test@cafm.com';
        IF admin_user_id IS NOT NULL THEN
            INSERT INTO user_roles (user_id, role_id) 
            VALUES (admin_user_id, super_admin_role_id)
            ON CONFLICT (user_id, role_id) DO NOTHING;
            RAISE NOTICE 'Added SUPER_ADMIN role to test@cafm.com';
        END IF;
        
        RAISE NOTICE 'SUPER_ADMIN role successfully assigned to all admin users';
    ELSE
        RAISE NOTICE 'Failed to find or create ROLE_SUPER_ADMIN';
    END IF;
END $$;