-- ============================================
-- V99__Update_Admin_Password.sql
-- Update admin user password for testing
-- ============================================

-- Update the admin@cafm.com user password to "AdminPass123!"
-- BCrypt hash generated for "AdminPass123!" with strength 10
UPDATE users 
SET 
    password_hash = '$2a$10$9K8zQJ5a7XL2vN4mO1pR6OzF3GQrHgB8wE0cI2dA5nT9uY6vS1kM.',
    password_changed_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'admin@cafm.com';

-- Also create the admin@cafm.local user that the Postman collection expects
INSERT INTO users (
    id,
    email,
    username,
    password_hash,
    first_name,
    last_name,
    user_type,
    status,
    email_verified,
    is_active,
    is_locked,
    company_id,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'admin@cafm.local',
    'admin.local',
    '$2a$10$9K8zQJ5a7XL2vN4mO1pR6OzF3GQrHgB8wE0cI2dA5nT9uY6vS1kM.',
    'System',
    'Admin Local',
    'admin',
    'active',
    true,
    true,
    false,
    '00000000-0000-0000-0000-000000000001'::UUID,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

-- Assign admin role to the admin@cafm.local user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@cafm.local' AND r.name = 'ROLE_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Update existing test@cafm.com user if exists with the same password for consistency
UPDATE users 
SET 
    password_hash = '$2a$10$9K8zQJ5a7XL2vN4mO1pR6OzF3GQrHgB8wE0cI2dA5nT9uY6vS1kM.',
    password_changed_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'test@cafm.com';

-- Log the password update
DO $$
BEGIN
    RAISE NOTICE 'Admin users updated with password: AdminPass123!';
    RAISE NOTICE 'Affected emails: admin@cafm.com, admin@cafm.local, test@cafm.com';
END $$;