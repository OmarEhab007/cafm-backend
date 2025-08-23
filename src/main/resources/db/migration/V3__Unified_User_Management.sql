-- ============================================
-- V3__Unified_User_Management.sql
-- Database Optimization Phase 1: Unified User Management
-- Consolidates user-related tables and improves data structure
-- ============================================

-- ============================================
-- STEP 1: CREATE NEW UNIFIED STRUCTURE
-- ============================================

-- Create user type enum
CREATE TYPE user_type AS ENUM ('admin', 'supervisor', 'technician', 'viewer', 'super_admin');

-- Create user status enum
CREATE TYPE user_status AS ENUM ('active', 'inactive', 'suspended', 'pending_verification');

-- Create custom domain types for validated fields
CREATE DOMAIN email_address AS VARCHAR(255)
    CHECK (VALUE ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

CREATE DOMAIN phone_number AS VARCHAR(20)
    CHECK (VALUE IS NULL OR VALUE ~* '^\+?[0-9]{7,15}$');

CREATE DOMAIN iqama_number AS VARCHAR(50)
    CHECK (VALUE IS NULL OR LENGTH(VALUE) >= 10);

-- Enhanced users table with all consolidated fields
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS user_type user_type DEFAULT 'viewer',
    ADD COLUMN IF NOT EXISTS status user_status DEFAULT 'pending_verification',
    ADD COLUMN IF NOT EXISTS full_name VARCHAR(255) GENERATED ALWAYS AS
        (COALESCE(first_name, '') || ' ' || COALESCE(last_name, '')) STORED,
    ADD COLUMN IF NOT EXISTS department VARCHAR(100),
    ADD COLUMN IF NOT EXISTS position VARCHAR(100),
    ADD COLUMN IF NOT EXISTS avatar_url TEXT,
    ADD COLUMN IF NOT EXISTS bio TEXT,
    ADD COLUMN IF NOT EXISTS date_of_birth DATE,
    ADD COLUMN IF NOT EXISTS nationality VARCHAR(100),
    ADD COLUMN IF NOT EXISTS emergency_contact phone_number,
    ADD COLUMN IF NOT EXISTS emergency_contact_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address TEXT,
    ADD COLUMN IF NOT EXISTS city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20),
    ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

-- Add check constraint for date of birth
ALTER TABLE users ADD CONSTRAINT chk_date_of_birth
    CHECK (date_of_birth IS NULL OR date_of_birth < CURRENT_DATE);

-- Create index for soft delete queries
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users(deleted_at);
CREATE INDEX IF NOT EXISTS idx_users_user_type ON users(user_type);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_full_name ON users(full_name);

-- ============================================
-- STEP 2: CREATE TECHNICIANS TABLE
-- Replace JSONB technicians_detailed with proper table
-- ============================================

CREATE TABLE IF NOT EXISTS technicians (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    supervisor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    work_id VARCHAR(50),
    profession VARCHAR(100),
    phone phone_number,
    email email_address,
    iqama_id iqama_number,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID REFERENCES users(id)
);

CREATE INDEX idx_technicians_supervisor_id ON technicians(supervisor_id);
CREATE INDEX idx_technicians_is_active ON technicians(is_active);
CREATE INDEX idx_technicians_deleted_at ON technicians(deleted_at);
CREATE UNIQUE INDEX unique_technician_work_id_active ON technicians(work_id) WHERE deleted_at IS NULL AND work_id IS NOT NULL;

-- ============================================
-- STEP 3: MIGRATE DATA FROM OLD TABLES
-- ============================================

-- Migrate admin data to users table
INSERT INTO users (
    email, username, password_hash, first_name, last_name,
    user_type, status, email_verified, is_active,
    auth_user_id, created_at, updated_at
)
SELECT
    a.email,
    LOWER(REPLACE(a.name, ' ', '_')) as username,
    '$2a$10$default_password_hash' as password_hash, -- Will need to be reset
    SPLIT_PART(a.name, ' ', 1) as first_name,
    SUBSTRING(a.name FROM POSITION(' ' IN a.name) + 1) as last_name,
    CASE
        WHEN a.role = 'super_admin' THEN 'super_admin'::user_type
        ELSE 'admin'::user_type
    END as user_type,
    'active'::user_status as status,
    TRUE as email_verified,
    TRUE as is_active,
    a.auth_user_id,
    a.created_at,
    a.updated_at
FROM admins a
WHERE NOT EXISTS (
    SELECT 1 FROM users u
    WHERE u.email = a.email OR u.auth_user_id = a.auth_user_id
);

-- Migrate supervisor data to users table
INSERT INTO users (
    email, username, password_hash, phone,
    iqama_id, plate_number, plate_letters_en, plate_letters_ar,
    employee_id, user_type, status, email_verified, is_active,
    auth_user_id, created_at, updated_at
)
SELECT
    s.email,
    s.username,
    '$2a$10$default_password_hash' as password_hash, -- Will need to be reset
    s.phone,
    s.iqama_id,
    s.plate_numbers,
    s.plate_english_letters,
    s.plate_arabic_letters,
    s.work_id,
    'supervisor'::user_type,
    'active'::user_status,
    TRUE as email_verified,
    TRUE as is_active,
    s.auth_user_id,
    s.created_at,
    s.updated_at
FROM supervisors s
WHERE NOT EXISTS (
    SELECT 1 FROM users u
    WHERE u.email = s.email OR u.username = s.username
);

-- Migrate technicians from JSONB to technicians table
INSERT INTO technicians (supervisor_id, name, work_id, profession)
SELECT
    u.id as supervisor_id,
    tech->>'name' as name,
    tech->>'workId' as work_id,
    tech->>'profession' as profession
FROM users u,
    LATERAL jsonb_array_elements(u.technicians_detailed) as tech
WHERE u.technicians_detailed IS NOT NULL
    AND jsonb_array_length(u.technicians_detailed) > 0;

-- Migrate profile data to users table
UPDATE users u
SET
    avatar_url = p.avatar_url,
    bio = p.bio,
    department = p.department,
    position = p.position,
    date_of_birth = p.date_of_birth,
    nationality = p.nationality,
    emergency_contact = p.emergency_contact,
    emergency_contact_name = p.emergency_contact_name,
    address = p.address,
    city = p.city,
    postal_code = p.postal_code
FROM profiles p
WHERE u.id = p.user_id;

-- ============================================
-- STEP 4: CREATE VIEWS FOR BACKWARD COMPATIBILITY
-- ============================================

-- Create view for admins (backward compatibility)
CREATE OR REPLACE VIEW v_admins AS
SELECT
    id,
    COALESCE(full_name, username) as name,
    email,
    auth_user_id,
    CASE
        WHEN user_type = 'super_admin' THEN 'super_admin'
        ELSE 'admin'
    END as role,
    created_at,
    updated_at
FROM users
WHERE user_type IN ('admin', 'super_admin')
    AND deleted_at IS NULL;

-- Create view for supervisors (backward compatibility)
CREATE OR REPLACE VIEW v_supervisors AS
SELECT
    u.id,
    u.username,
    u.email,
    u.phone,
    u.iqama_id,
    u.plate_number as plate_numbers,
    u.plate_letters_en as plate_english_letters,
    u.plate_letters_ar as plate_arabic_letters,
    u.employee_id as work_id,
    u.auth_user_id,
    u.admin_id,
    ARRAY(
        SELECT t.name
        FROM technicians t
        WHERE t.supervisor_id = u.id
            AND t.deleted_at IS NULL
    ) as technicians,
    (
        SELECT jsonb_agg(
            jsonb_build_object(
                'name', t.name,
                'workId', t.work_id,
                'profession', t.profession
            )
        )
        FROM technicians t
        WHERE t.supervisor_id = u.id
            AND t.deleted_at IS NULL
    ) as technicians_detailed,
    u.created_at,
    u.updated_at
FROM users u
WHERE u.user_type = 'supervisor'
    AND u.deleted_at IS NULL;

-- Create view for active users only
CREATE OR REPLACE VIEW v_active_users AS
SELECT * FROM users
WHERE deleted_at IS NULL
    AND is_active = TRUE
    AND status = 'active';

-- ============================================
-- STEP 5: ADD CONSTRAINTS AND INDEXES
-- ============================================

-- Add unique constraint for active users only
CREATE UNIQUE INDEX unique_active_email
    ON users(email)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX unique_active_username
    ON users(username)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX unique_active_employee_id
    ON users(employee_id)
    WHERE deleted_at IS NULL AND employee_id IS NOT NULL;

-- Add composite indexes for common queries
CREATE INDEX idx_users_type_status_active
    ON users(user_type, status, is_active)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_users_created_at_desc
    ON users(created_at DESC)
    WHERE deleted_at IS NULL;

-- Add GIN index for JSONB metadata
CREATE INDEX idx_users_metadata
    ON users USING GIN (metadata);

-- ============================================
-- STEP 6: CREATE FUNCTIONS FOR USER MANAGEMENT
-- ============================================

-- Function to get user with roles
CREATE OR REPLACE FUNCTION get_user_with_roles(p_user_id UUID)
RETURNS TABLE (
    user_id UUID,
    email VARCHAR(255),
    username VARCHAR(100),
    full_name VARCHAR(255),
    user_type user_type,
    roles TEXT[]
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        u.id,
        u.email,
        u.username,
        u.full_name,
        u.user_type,
        ARRAY_AGG(r.name) as roles
    FROM users u
    LEFT JOIN user_roles ur ON u.id = ur.user_id
    LEFT JOIN roles r ON ur.role_id = r.id
    WHERE u.id = p_user_id
        AND u.deleted_at IS NULL
    GROUP BY u.id, u.email, u.username, u.full_name, u.user_type;
END;
$$ LANGUAGE plpgsql;

-- Function to soft delete user
CREATE OR REPLACE FUNCTION soft_delete_user(
    p_user_id UUID,
    p_deleted_by UUID
) RETURNS BOOLEAN AS $$
BEGIN
    UPDATE users
    SET deleted_at = CURRENT_TIMESTAMP,
        deleted_by = p_deleted_by,
        is_active = FALSE,
        status = 'inactive'
    WHERE id = p_user_id
        AND deleted_at IS NULL;

    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 7: ADD TRIGGERS
-- ============================================

-- Trigger to update technicians updated_at
CREATE TRIGGER update_technicians_updated_at
    BEFORE UPDATE ON technicians
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Trigger to validate user type changes
CREATE OR REPLACE FUNCTION validate_user_type_change()
RETURNS TRIGGER AS $$
BEGIN
    -- Prevent changing from admin/super_admin to lower roles without explicit permission
    IF OLD.user_type IN ('admin', 'super_admin')
        AND NEW.user_type NOT IN ('admin', 'super_admin') THEN
        RAISE EXCEPTION 'Cannot downgrade admin privileges without explicit permission';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER validate_user_type_change_trigger
    BEFORE UPDATE OF user_type ON users
    FOR EACH ROW EXECUTE FUNCTION validate_user_type_change();

-- ============================================
-- STEP 8: CLEANUP OLD COLUMNS (OPTIONAL - CAN BE DONE LATER)
-- ============================================

-- These can be dropped after verifying data migration
-- ALTER TABLE users DROP COLUMN IF EXISTS technicians_detailed;
-- DROP TABLE IF EXISTS admins CASCADE;
-- DROP TABLE IF EXISTS supervisors CASCADE;
-- DROP TABLE IF EXISTS profiles CASCADE;

-- ============================================
-- STEP 9: ADD COMMENTS FOR DOCUMENTATION
-- ============================================

COMMENT ON TABLE users IS 'Unified user management table for all user types';
COMMENT ON COLUMN users.user_type IS 'Type of user: admin, supervisor, technician, viewer, super_admin';
COMMENT ON COLUMN users.status IS 'User account status';
COMMENT ON COLUMN users.full_name IS 'Generated full name from first_name and last_name';
COMMENT ON COLUMN users.metadata IS 'Flexible JSON storage for additional user attributes';
COMMENT ON COLUMN users.deleted_at IS 'Soft delete timestamp';

COMMENT ON TABLE technicians IS 'Technicians managed by supervisors';
COMMENT ON VIEW v_admins IS 'Backward compatibility view for admin users';
COMMENT ON VIEW v_supervisors IS 'Backward compatibility view for supervisor users';
COMMENT ON VIEW v_active_users IS 'View of all active, non-deleted users';

-- End of V3 Unified User Management migration