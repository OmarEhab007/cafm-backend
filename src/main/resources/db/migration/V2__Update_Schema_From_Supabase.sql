-- ============================================
-- V2__Update_Schema_From_Supabase.sql
-- CAFM Backend Schema Updates from Supabase Analysis
-- Adds missing tables and fields from existing Supabase production
-- Spring Boot 3.3.x with Java 23
-- ============================================

-- ============================================
-- ALTER EXISTING TABLES
-- ============================================

-- Add missing fields to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS technicians_detailed JSONB DEFAULT '[]';
ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_user_id VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS admin_id UUID REFERENCES users(id);

-- Add unique constraint for auth_user_id
ALTER TABLE users ADD CONSTRAINT unique_auth_user_id UNIQUE (auth_user_id);

-- Update reports table to include school_name for denormalization (used in Flutter apps)
ALTER TABLE reports ADD COLUMN IF NOT EXISTS school_name VARCHAR(255);

-- Since status is an ENUM type, we need to alter the enum itself
-- First, check if the new values don't already exist and add them
DO $$ 
BEGIN
    -- Add missing enum values if they don't exist
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'pending' AND enumtypid = 'report_status'::regtype) THEN
        ALTER TYPE report_status ADD VALUE IF NOT EXISTS 'pending';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'in_progress' AND enumtypid = 'report_status'::regtype) THEN
        ALTER TYPE report_status ADD VALUE IF NOT EXISTS 'in_progress';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'completed' AND enumtypid = 'report_status'::regtype) THEN
        ALTER TYPE report_status ADD VALUE IF NOT EXISTS 'completed';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'late' AND enumtypid = 'report_status'::regtype) THEN
        ALTER TYPE report_status ADD VALUE IF NOT EXISTS 'late';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'late_completed' AND enumtypid = 'report_status'::regtype) THEN
        ALTER TYPE report_status ADD VALUE IF NOT EXISTS 'late_completed';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'cancelled' AND enumtypid = 'report_status'::regtype) THEN
        ALTER TYPE report_status ADD VALUE IF NOT EXISTS 'cancelled';
    END IF;
END $$;

-- ============================================
-- CREATE NEW TABLES FROM SUPABASE SCHEMA
-- ============================================

-- Admins table (separate from users for admin panel)
CREATE TABLE IF NOT EXISTS admins (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    auth_user_id VARCHAR(255),
    role VARCHAR(50) DEFAULT 'admin',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_admin_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- Supervisors table (detailed supervisor profiles)
CREATE TABLE IF NOT EXISTS supervisors (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    iqama_id VARCHAR(50),
    plate_numbers VARCHAR(50),
    plate_english_letters VARCHAR(10),
    plate_arabic_letters VARCHAR(10),
    work_id VARCHAR(50),
    auth_user_id VARCHAR(255),
    admin_id UUID REFERENCES admins(id),
    technicians TEXT[], -- Array of technician names
    technicians_detailed JSONB DEFAULT '[]', -- Detailed technician info
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_supervisor_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_supervisor_phone_format CHECK (phone ~* '^\\+?[0-9]{7,15}$')
);

-- Profiles table (extends user information)
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(255),
    avatar_url TEXT,
    bio TEXT,
    department VARCHAR(100),
    position VARCHAR(100),
    date_of_birth DATE,
    nationality VARCHAR(100),
    emergency_contact VARCHAR(20),
    emergency_contact_name VARCHAR(255),
    address TEXT,
    city VARCHAR(100),
    postal_code VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Unique constraint
    CONSTRAINT unique_user_profile UNIQUE (user_id)
);

-- Maintenance reports table (specific to maintenance workflow)
CREATE TABLE IF NOT EXISTS maintenance_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    school_name VARCHAR(255) NOT NULL,
    supervisor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    report_data JSONB NOT NULL DEFAULT '{}',
    photos JSONB DEFAULT '[]',
    completion_note TEXT,
    completion_photos JSONB DEFAULT '[]',
    submitted_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Check constraints
    CONSTRAINT chk_maintenance_report_status 
        CHECK (status IN ('draft', 'submitted', 'in_progress', 'completed', 'late_completed'))
);

-- Supervisor attendance table (updated with photo_url)
ALTER TABLE supervisor_attendance ADD COLUMN IF NOT EXISTS photo_url TEXT;

-- School achievements table (matching Supabase structure)
CREATE TABLE IF NOT EXISTS school_achievements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    school_name VARCHAR(255) NOT NULL,
    supervisor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    photos JSONB NOT NULL DEFAULT '[]',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP WITH TIME ZONE,
    
    -- Check constraints
    CONSTRAINT chk_school_achievement_type 
        CHECK (achievement_type IN ('maintenance_achievement', 'ac_achievement', 'checklist')),
    CONSTRAINT chk_school_achievement_status 
        CHECK (status IN ('draft', 'submitted'))
);

-- Individual damage count photos table
CREATE TABLE IF NOT EXISTS damage_count_photos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    damage_count_id UUID NOT NULL REFERENCES damage_counts(id) ON DELETE CASCADE,
    section_key VARCHAR(100) NOT NULL,
    photo_url TEXT NOT NULL,
    photo_description TEXT,
    upload_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- File uploads table (track all uploaded files)
CREATE TABLE IF NOT EXISTS file_uploads (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    file_url TEXT NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    uploaded_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    storage_provider VARCHAR(50) DEFAULT 'minio',
    storage_path TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- FCM tokens table for push notifications
CREATE TABLE IF NOT EXISTS user_fcm_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fcm_token TEXT NOT NULL,
    device_id VARCHAR(255),
    platform VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Unique constraint for token
    CONSTRAINT unique_fcm_token UNIQUE(fcm_token),
    
    -- Check constraints
    CONSTRAINT chk_fcm_platform CHECK (platform IN ('android', 'ios', 'web'))
);

-- Notification queue table
CREATE TABLE IF NOT EXISTS notification_queue (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    data JSONB DEFAULT '{}',
    priority VARCHAR(20) DEFAULT 'normal',
    processed BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Check constraints
    CONSTRAINT chk_notification_priority CHECK (priority IN ('low', 'normal', 'high'))
);

-- Notifications history table
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    data JSONB DEFAULT '{}',
    read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- App versions table (for update management)
CREATE TABLE IF NOT EXISTS app_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    version_name VARCHAR(20) NOT NULL,
    version_code INTEGER NOT NULL,
    platform VARCHAR(20) NOT NULL,
    minimum_supported_version INTEGER,
    force_update BOOLEAN DEFAULT FALSE,
    update_message TEXT,
    download_url TEXT,
    changelog TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Check constraints
    CONSTRAINT chk_app_platform CHECK (platform IN ('android', 'ios', 'web'))
);

-- ============================================
-- ADDITIONAL INDEXES FOR NEW TABLES
-- ============================================

-- Admins indexes
CREATE INDEX IF NOT EXISTS idx_admins_email ON admins(email);
CREATE INDEX IF NOT EXISTS idx_admins_auth_user_id ON admins(auth_user_id);

-- Supervisors indexes
CREATE INDEX IF NOT EXISTS idx_supervisors_email ON supervisors(email);
CREATE INDEX IF NOT EXISTS idx_supervisors_username ON supervisors(username);
CREATE INDEX IF NOT EXISTS idx_supervisors_work_id ON supervisors(work_id);
CREATE INDEX IF NOT EXISTS idx_supervisors_auth_user_id ON supervisors(auth_user_id);
CREATE INDEX IF NOT EXISTS idx_supervisors_admin_id ON supervisors(admin_id);

-- Profiles indexes
CREATE INDEX IF NOT EXISTS idx_profiles_user_id ON profiles(user_id);

-- Maintenance reports indexes
CREATE INDEX IF NOT EXISTS idx_maintenance_reports_school_id ON maintenance_reports(school_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_reports_supervisor_id ON maintenance_reports(supervisor_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_reports_status ON maintenance_reports(status);
CREATE INDEX IF NOT EXISTS idx_maintenance_reports_created_at ON maintenance_reports(created_at);

-- School achievements indexes
CREATE INDEX IF NOT EXISTS idx_school_achievements_school_id ON school_achievements(school_id);
CREATE INDEX IF NOT EXISTS idx_school_achievements_supervisor_id ON school_achievements(supervisor_id);
CREATE INDEX IF NOT EXISTS idx_school_achievements_type ON school_achievements(achievement_type);
CREATE INDEX IF NOT EXISTS idx_school_achievements_status ON school_achievements(status);

-- Damage count photos indexes
CREATE INDEX IF NOT EXISTS idx_damage_count_photos_damage_count_id ON damage_count_photos(damage_count_id);
CREATE INDEX IF NOT EXISTS idx_damage_count_photos_section_key ON damage_count_photos(section_key);

-- File uploads indexes
CREATE INDEX IF NOT EXISTS idx_file_uploads_uploaded_by ON file_uploads(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_file_uploads_mime_type ON file_uploads(mime_type);
CREATE INDEX IF NOT EXISTS idx_file_uploads_created_at ON file_uploads(created_at);

-- FCM tokens indexes
CREATE INDEX IF NOT EXISTS idx_user_fcm_tokens_user_id ON user_fcm_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_user_fcm_tokens_active ON user_fcm_tokens(is_active);
CREATE INDEX IF NOT EXISTS idx_user_fcm_tokens_platform ON user_fcm_tokens(platform);

-- Notification queue indexes
CREATE INDEX IF NOT EXISTS idx_notification_queue_user_id ON notification_queue(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_queue_processed ON notification_queue(processed);
CREATE INDEX IF NOT EXISTS idx_notification_queue_created_at ON notification_queue(created_at);
CREATE INDEX IF NOT EXISTS idx_notification_queue_priority ON notification_queue(priority);

-- Notifications indexes
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read ON notifications(read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);

-- App versions indexes
CREATE INDEX IF NOT EXISTS idx_app_versions_platform ON app_versions(platform);
CREATE INDEX IF NOT EXISTS idx_app_versions_version_code ON app_versions(version_code);
CREATE INDEX IF NOT EXISTS idx_app_versions_active ON app_versions(is_active);

-- ============================================
-- TRIGGERS FOR UPDATED_AT (NEW TABLES)
-- ============================================

CREATE TRIGGER update_admins_updated_at BEFORE UPDATE ON admins
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_supervisors_updated_at BEFORE UPDATE ON supervisors
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_profiles_updated_at BEFORE UPDATE ON profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_maintenance_reports_updated_at BEFORE UPDATE ON maintenance_reports
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_school_achievements_updated_at BEFORE UPDATE ON school_achievements
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_fcm_tokens_updated_at BEFORE UPDATE ON user_fcm_tokens
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- SAMPLE DATA FOR NEW TABLES
-- ============================================

-- Insert sample admin (using existing admin user)
INSERT INTO admins (name, email, role, auth_user_id)
SELECT 
    CONCAT(first_name, ' ', last_name) as name,
    email,
    'super_admin' as role,
    id::text as auth_user_id
FROM users 
WHERE username = 'admin'
ON CONFLICT (email) DO NOTHING;

-- Insert sample app versions
INSERT INTO app_versions (version_name, version_code, platform, minimum_supported_version, force_update, update_message, changelog)
VALUES 
    ('1.0.0', 1, 'android', 1, FALSE, 'Initial release', 'Initial release of CAFM mobile app'),
    ('1.0.0', 1, 'ios', 1, FALSE, 'Initial release', 'Initial release of CAFM mobile app'),
    ('1.0.0', 1, 'web', 1, FALSE, 'Initial release', 'Initial release of CAFM admin panel')
ON CONFLICT DO NOTHING;

-- ============================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================

COMMENT ON TABLE admins IS 'Administrator accounts for the admin panel application';
COMMENT ON TABLE supervisors IS 'Detailed supervisor profiles with technician management';
COMMENT ON TABLE profiles IS 'Extended user profile information';
COMMENT ON TABLE maintenance_reports IS 'Detailed maintenance workflow reports';
COMMENT ON TABLE school_achievements IS 'School achievement photo documentation';
COMMENT ON TABLE damage_count_photos IS 'Individual photos for damage count records';
COMMENT ON TABLE file_uploads IS 'Central file upload tracking for all uploaded files';
COMMENT ON TABLE user_fcm_tokens IS 'FCM tokens for push notifications to mobile devices';
COMMENT ON TABLE notification_queue IS 'Queue for processing push notifications';
COMMENT ON TABLE notifications IS 'Notification history for users';
COMMENT ON TABLE app_versions IS 'Mobile app version management for forced updates';

-- End of Supabase schema update migration