-- V10: Add Technician Specialization Support
-- Migration to add specialization, skill level, and related fields to users table

-- Add specialization columns to users table
ALTER TABLE users 
    ADD COLUMN specialization VARCHAR(30),
    ADD COLUMN skill_level VARCHAR(20),
    ADD COLUMN certifications JSONB,
    ADD COLUMN years_of_experience INTEGER,
    ADD COLUMN hourly_rate DOUBLE PRECISION,
    ADD COLUMN is_available_for_assignment BOOLEAN DEFAULT true;

-- Add constraints for new columns
ALTER TABLE users
    ADD CONSTRAINT check_years_of_experience_non_negative 
        CHECK (years_of_experience IS NULL OR years_of_experience >= 0),
    ADD CONSTRAINT check_hourly_rate_non_negative 
        CHECK (hourly_rate IS NULL OR hourly_rate >= 0.0);

-- Add check constraints for enum values
ALTER TABLE users
    ADD CONSTRAINT check_specialization_valid 
        CHECK (specialization IS NULL OR specialization IN (
            'ELECTRICAL', 'PLUMBING', 'HVAC', 'CARPENTRY', 'PAINTING',
            'GENERAL_MAINTENANCE', 'LANDSCAPING', 'CLEANING', 'IT_SUPPORT',
            'SECURITY_SYSTEMS', 'ROOFING', 'FLOORING', 'MASONRY',
            'FIRE_SAFETY', 'PEST_CONTROL'
        ));

ALTER TABLE users
    ADD CONSTRAINT check_skill_level_valid 
        CHECK (skill_level IS NULL OR skill_level IN (
            'BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT', 'MASTER'
        ));

-- Create indexes for performance
CREATE INDEX idx_users_specialization ON users (specialization) WHERE specialization IS NOT NULL;
CREATE INDEX idx_users_skill_level ON users (skill_level) WHERE skill_level IS NOT NULL;
CREATE INDEX idx_users_available_assignment ON users (is_available_for_assignment) WHERE is_available_for_assignment = true;

-- Composite index for common technician queries
CREATE INDEX idx_users_technician_availability 
    ON users (user_type, specialization, skill_level, is_available_for_assignment)
    WHERE user_type = 'technician' AND deleted_at IS NULL;

-- GIN index for certifications JSON field
CREATE INDEX idx_users_certifications_gin ON users USING gin (certifications) WHERE certifications IS NOT NULL;

-- Update search vector to include specialization (if full-text search is used)
-- This will be handled by triggers if they exist

-- Add comments for documentation
COMMENT ON COLUMN users.specialization IS 'Technician specialization area (electrical, plumbing, etc.)';
COMMENT ON COLUMN users.skill_level IS 'Skill level: BEGINNER, INTERMEDIATE, ADVANCED, EXPERT, MASTER';
COMMENT ON COLUMN users.certifications IS 'JSON array of certifications with details';
COMMENT ON COLUMN users.years_of_experience IS 'Years of experience in the field';
COMMENT ON COLUMN users.hourly_rate IS 'Hourly rate for technician work';
COMMENT ON COLUMN users.is_available_for_assignment IS 'Whether technician is available for new assignments';

-- Create a view for active technicians with specialization
CREATE OR REPLACE VIEW active_technicians AS
SELECT 
    u.id,
    u.username,
    u.email,
    u.first_name,
    u.last_name,
    u.full_name,
    u.phone,
    u.employee_id,
    u.specialization,
    u.skill_level,
    u.years_of_experience,
    u.hourly_rate,
    u.performance_rating,
    u.productivity_score,
    u.is_available_for_assignment,
    u.city,
    u.department,
    u.created_at,
    u.last_login_at
FROM users u
WHERE u.user_type = 'technician'
    AND u.status = 'active'
    AND u.is_active = true
    AND u.deleted_at IS NULL
    AND u.is_available_for_assignment = true;

COMMENT ON VIEW active_technicians IS 'View of active technicians available for assignment';

-- Sample data for existing users (optional - only if there are existing technicians)
-- Update existing technician users with default values where appropriate
UPDATE users 
SET 
    specialization = 'GENERAL_MAINTENANCE',
    skill_level = 'INTERMEDIATE',
    is_available_for_assignment = true
WHERE user_type = 'technician' 
    AND specialization IS NULL 
    AND deleted_at IS NULL;

-- Create audit trigger for specialization changes (if audit system is active)
-- This will track changes to specialization-related fields