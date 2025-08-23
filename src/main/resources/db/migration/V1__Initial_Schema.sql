-- ============================================
-- V1__Initial_Schema.sql
-- CAFM Backend Initial Database Schema
-- Spring Boot 3.3.x with Java 23
-- ============================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- AUDIT FUNCTION
-- ============================================
-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- USER MANAGEMENT TABLES
-- ============================================

-- Roles table
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create default roles
INSERT INTO roles (name, description) VALUES 
    ('ROLE_ADMIN', 'Administrator with full system access'),
    ('ROLE_SUPERVISOR', 'Supervisor who manages schools and reports'),
    ('ROLE_TECHNICIAN', 'Technician who performs maintenance work'),
    ('ROLE_VIEWER', 'Read-only access to reports and data');

-- Users table (replaces Supabase auth)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    
    -- Additional profile fields
    employee_id VARCHAR(50) UNIQUE,
    iqama_id VARCHAR(50),
    plate_number VARCHAR(50),
    plate_letters_en VARCHAR(10),
    plate_letters_ar VARCHAR(10),
    
    -- Status fields
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    is_locked BOOLEAN DEFAULT FALSE,
    
    -- Timestamps
    last_login_at TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_phone_format CHECK (phone IS NULL OR phone ~* '^\+?[0-9]{7,15}$')
);

-- User roles junction table (many-to-many)
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    assigned_by UUID REFERENCES users(id),
    PRIMARY KEY (user_id, role_id)
);

-- Refresh tokens for JWT
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_reason VARCHAR(255),
    user_agent TEXT,
    ip_address INET,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Password reset tokens
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMP WITH TIME ZONE,
    ip_address INET,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- SCHOOL MANAGEMENT TABLES
-- ============================================

-- Schools table
CREATE TABLE schools (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255),
    type VARCHAR(50), -- PRIMARY, SECONDARY, HIGH_SCHOOL, etc.
    gender VARCHAR(20), -- BOYS, GIRLS, MIXED
    
    -- Location
    address TEXT,
    city VARCHAR(100),
    district VARCHAR(100),
    postal_code VARCHAR(20),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    
    -- Contact
    phone VARCHAR(20),
    email VARCHAR(255),
    principal_name VARCHAR(255),
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Metadata
    student_count INTEGER,
    staff_count INTEGER,
    building_area DECIMAL(10, 2), -- in square meters
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

-- Supervisor-School assignments
CREATE TABLE supervisor_schools (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    supervisor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    assigned_by UUID REFERENCES users(id),
    unassigned_at TIMESTAMP WITH TIME ZONE,
    unassigned_by UUID REFERENCES users(id),
    is_active BOOLEAN DEFAULT TRUE,
    notes TEXT,
    
    -- Ensure unique active assignment
    CONSTRAINT unique_active_assignment UNIQUE (supervisor_id, school_id, is_active)
);

-- ============================================
-- REPORTS AND MAINTENANCE TABLES
-- ============================================

-- Report status enum-like check
CREATE TYPE report_status AS ENUM (
    'DRAFT',
    'SUBMITTED',
    'IN_REVIEW',
    'APPROVED',
    'IN_PROGRESS',
    'COMPLETED',
    'REJECTED',
    'CANCELLED'
);

-- Priority levels
CREATE TYPE priority_level AS ENUM (
    'LOW',
    'MEDIUM',
    'HIGH',
    'CRITICAL'
);

-- Reports/Work Orders table
CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_number VARCHAR(50) UNIQUE NOT NULL,
    
    -- Relations
    school_id UUID NOT NULL REFERENCES schools(id),
    supervisor_id UUID NOT NULL REFERENCES users(id),
    assigned_to UUID REFERENCES users(id), -- Technician assignment
    
    -- Report details
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100),
    sub_category VARCHAR(100),
    
    -- Status and priority
    status report_status DEFAULT 'DRAFT',
    priority priority_level DEFAULT 'MEDIUM',
    
    -- Dates
    reported_date DATE NOT NULL DEFAULT CURRENT_DATE,
    scheduled_date DATE,
    completed_date DATE,
    
    -- Work details
    work_description TEXT,
    materials_used TEXT,
    labor_hours DECIMAL(5, 2),
    estimated_cost DECIMAL(10, 2),
    actual_cost DECIMAL(10, 2),
    
    -- Approval workflow
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    approved_by UUID REFERENCES users(id),
    approved_at TIMESTAMP WITH TIME ZONE,
    
    -- Metadata
    is_urgent BOOLEAN DEFAULT FALSE,
    requires_shutdown BOOLEAN DEFAULT FALSE,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

-- Report images/attachments
CREATE TABLE report_attachments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    attachment_type VARCHAR(50), -- BEFORE, AFTER, DURING, DOCUMENT
    description TEXT,
    uploaded_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Report comments/notes
CREATE TABLE report_comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    comment TEXT NOT NULL,
    is_internal BOOLEAN DEFAULT FALSE, -- Internal notes not visible to all
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- MAINTENANCE TRACKING TABLES
-- ============================================

-- Maintenance counts/inventory
CREATE TABLE maintenance_counts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id),
    supervisor_id UUID NOT NULL REFERENCES users(id),
    
    -- Count details
    count_date DATE NOT NULL DEFAULT CURRENT_DATE,
    category VARCHAR(100) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    
    -- Quantities
    total_count INTEGER NOT NULL DEFAULT 0,
    working_count INTEGER NOT NULL DEFAULT 0,
    damaged_count INTEGER NOT NULL DEFAULT 0,
    missing_count INTEGER NOT NULL DEFAULT 0,
    
    -- Additional info
    location VARCHAR(255),
    notes TEXT,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Damage assessments
CREATE TABLE damage_counts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id),
    supervisor_id UUID NOT NULL REFERENCES users(id),
    
    -- Assessment details
    assessment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    area VARCHAR(255) NOT NULL,
    damage_type VARCHAR(100) NOT NULL,
    severity VARCHAR(50), -- MINOR, MODERATE, SEVERE, CRITICAL
    
    -- Description
    description TEXT NOT NULL,
    recommended_action TEXT,
    estimated_repair_cost DECIMAL(10, 2),
    
    -- Priority
    priority priority_level DEFAULT 'MEDIUM',
    requires_immediate_action BOOLEAN DEFAULT FALSE,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- ATTENDANCE AND ACHIEVEMENTS
-- ============================================

-- Supervisor attendance
CREATE TABLE supervisor_attendance (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    supervisor_id UUID NOT NULL REFERENCES users(id),
    school_id UUID NOT NULL REFERENCES schools(id),
    
    -- Attendance details
    attendance_date DATE NOT NULL,
    check_in_time TIME WITH TIME ZONE,
    check_out_time TIME WITH TIME ZONE,
    
    -- Location verification
    check_in_latitude DECIMAL(10, 8),
    check_in_longitude DECIMAL(11, 8),
    check_out_latitude DECIMAL(10, 8),
    check_out_longitude DECIMAL(11, 8),
    
    -- Work summary
    work_summary TEXT,
    issues_found TEXT,
    actions_taken TEXT,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Unique constraint for one attendance per day per supervisor per school
    CONSTRAINT unique_daily_attendance UNIQUE (supervisor_id, school_id, attendance_date)
);

-- Achievement photos/documentation
CREATE TABLE achievement_photos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id),
    supervisor_id UUID NOT NULL REFERENCES users(id),
    report_id UUID REFERENCES reports(id),
    
    -- Achievement details
    title VARCHAR(255) NOT NULL,
    description TEXT,
    achievement_date DATE NOT NULL DEFAULT CURRENT_DATE,
    category VARCHAR(100),
    
    -- Files
    before_photo_path TEXT,
    after_photo_path TEXT,
    
    -- Metadata
    improvement_percentage INTEGER,
    time_taken_hours DECIMAL(5, 2),
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================

-- User indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_employee_id ON users(employee_id);
CREATE INDEX idx_users_is_active ON users(is_active);

-- Token indexes
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_password_reset_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_expires_at ON password_reset_tokens(expires_at);

-- School indexes
CREATE INDEX idx_schools_code ON schools(code);
CREATE INDEX idx_schools_name ON schools(name);
CREATE INDEX idx_schools_is_active ON schools(is_active);

-- Supervisor-School indexes
CREATE INDEX idx_supervisor_schools_supervisor ON supervisor_schools(supervisor_id);
CREATE INDEX idx_supervisor_schools_school ON supervisor_schools(school_id);
CREATE INDEX idx_supervisor_schools_active ON supervisor_schools(is_active);

-- Report indexes
CREATE INDEX idx_reports_school ON reports(school_id);
CREATE INDEX idx_reports_supervisor ON reports(supervisor_id);
CREATE INDEX idx_reports_assigned_to ON reports(assigned_to);
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_priority ON reports(priority);
CREATE INDEX idx_reports_reported_date ON reports(reported_date);
CREATE INDEX idx_reports_number ON reports(report_number);

-- Maintenance indexes
CREATE INDEX idx_maintenance_school ON maintenance_counts(school_id);
CREATE INDEX idx_maintenance_supervisor ON maintenance_counts(supervisor_id);
CREATE INDEX idx_maintenance_date ON maintenance_counts(count_date);

-- Damage indexes
CREATE INDEX idx_damage_school ON damage_counts(school_id);
CREATE INDEX idx_damage_supervisor ON damage_counts(supervisor_id);
CREATE INDEX idx_damage_date ON damage_counts(assessment_date);

-- Attendance indexes
CREATE INDEX idx_attendance_supervisor ON supervisor_attendance(supervisor_id);
CREATE INDEX idx_attendance_school ON supervisor_attendance(school_id);
CREATE INDEX idx_attendance_date ON supervisor_attendance(attendance_date);

-- Achievement indexes
CREATE INDEX idx_achievement_school ON achievement_photos(school_id);
CREATE INDEX idx_achievement_supervisor ON achievement_photos(supervisor_id);
CREATE INDEX idx_achievement_report ON achievement_photos(report_id);

-- ============================================
-- TRIGGERS FOR UPDATED_AT
-- ============================================

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_schools_updated_at BEFORE UPDATE ON schools
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reports_updated_at BEFORE UPDATE ON reports
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_report_comments_updated_at BEFORE UPDATE ON report_comments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_maintenance_counts_updated_at BEFORE UPDATE ON maintenance_counts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_damage_counts_updated_at BEFORE UPDATE ON damage_counts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_supervisor_attendance_updated_at BEFORE UPDATE ON supervisor_attendance
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_achievement_photos_updated_at BEFORE UPDATE ON achievement_photos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- INITIAL DATA (Optional)
-- ============================================

-- Create default admin user (password: Admin@123 - should be changed immediately)
-- Password is BCrypt hash of 'Admin@123'
INSERT INTO users (email, username, password_hash, first_name, last_name, email_verified, is_active)
VALUES (
    'admin@cafm.com',
    'admin',
    '$2a$10$qn5QWEpqh3EqMRNeqnSjLuNFUgIBHx9JKlKjBg.ZGNh4Qfvb7ZdNe',
    'System',
    'Administrator',
    true,
    true
);

-- Assign admin role to default admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';

-- ============================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================

COMMENT ON TABLE users IS 'System users including administrators, supervisors, and technicians';
COMMENT ON TABLE roles IS 'System roles for authorization';
COMMENT ON TABLE schools IS 'Educational institutions managed by the system';
COMMENT ON TABLE reports IS 'Maintenance reports and work orders';
COMMENT ON TABLE supervisor_attendance IS 'Daily attendance tracking for supervisors';
COMMENT ON TABLE achievement_photos IS 'Documentation of completed maintenance work';

-- End of initial schema migration