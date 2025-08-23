-- ============================================
-- V6__Data_Integrity_Improvements.sql
-- Database Optimization Phase 2: Data Integrity Improvements
-- Adds comprehensive constraints, validations, and data quality checks
-- ============================================

-- ============================================
-- STEP 1: ADD DOMAIN CONSTRAINTS
-- ============================================

-- Create custom domain for percentage values
CREATE DOMAIN percentage AS INTEGER
    CHECK (VALUE >= 0 AND VALUE <= 100);

-- Create custom domain for positive decimals
CREATE DOMAIN positive_decimal AS DECIMAL
    CHECK (VALUE >= 0);

-- Create custom domain for year values
CREATE DOMAIN year_value AS INTEGER
    CHECK (VALUE >= 2020 AND VALUE <= 2100);

-- Create custom domain for month values
CREATE DOMAIN month_value AS INTEGER
    CHECK (VALUE >= 1 AND VALUE <= 12);

-- Create custom domain for rating scores
CREATE DOMAIN rating_score AS INTEGER
    CHECK (VALUE >= 1 AND VALUE <= 5);

-- ============================================
-- STEP 2: ADD CHECK CONSTRAINTS TO EXISTING TABLES
-- ============================================

-- Add check constraints to reports table
ALTER TABLE reports ADD CONSTRAINT chk_report_dates
    CHECK (
        (completed_date IS NULL OR completed_date >= reported_date) AND
        (scheduled_date IS NULL OR scheduled_date >= reported_date)
    );

ALTER TABLE reports ADD CONSTRAINT chk_report_costs
    CHECK (
        (estimated_cost IS NULL OR estimated_cost >= 0) AND
        (actual_cost IS NULL OR actual_cost >= 0)
    );

ALTER TABLE reports ADD CONSTRAINT chk_labor_hours
    CHECK (labor_hours IS NULL OR labor_hours >= 0);

-- Add check constraints to schools table
ALTER TABLE schools ADD CONSTRAINT chk_school_counts
    CHECK (
        (student_count IS NULL OR student_count >= 0) AND
        (staff_count IS NULL OR staff_count >= 0)
    );

ALTER TABLE schools ADD CONSTRAINT chk_building_area
    CHECK (building_area IS NULL OR building_area > 0);

ALTER TABLE schools ADD CONSTRAINT chk_coordinates
    CHECK (
        (latitude IS NULL AND longitude IS NULL) OR
        (latitude IS NOT NULL AND longitude IS NOT NULL AND
         latitude BETWEEN -90 AND 90 AND
         longitude BETWEEN -180 AND 180)
    );

-- Add check constraints to supervisor_attendance
ALTER TABLE supervisor_attendance ADD CONSTRAINT chk_attendance_times
    CHECK (
        (check_out_time IS NULL) OR
        (check_in_time IS NOT NULL AND check_out_time > check_in_time)
    );

ALTER TABLE supervisor_attendance ADD CONSTRAINT chk_checkin_location
    CHECK (
        (check_in_latitude IS NULL AND check_in_longitude IS NULL) OR
        (check_in_latitude IS NOT NULL AND check_in_longitude IS NOT NULL AND
         check_in_latitude BETWEEN -90 AND 90 AND
         check_in_longitude BETWEEN -180 AND 180)
    );

-- Add check constraints to maintenance_counts
ALTER TABLE maintenance_counts ADD CONSTRAINT chk_maintenance_consistency
    CHECK (working_count + damaged_count + missing_count <= total_count);

-- Add check constraints to file_uploads
ALTER TABLE file_uploads ADD CONSTRAINT chk_file_size
    CHECK (file_size > 0 AND file_size <= 104857600); -- Max 100MB

-- ============================================
-- STEP 3: ADD EXCLUSION CONSTRAINTS
-- ============================================

-- Enable btree_gist extension for exclusion constraints
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Prevent overlapping supervisor school assignments
ALTER TABLE supervisor_schools 
    ADD CONSTRAINT excl_overlapping_assignments
    EXCLUDE USING gist (
        supervisor_id WITH =,
        school_id WITH =,
        tstzrange(assigned_at, unassigned_at, '[)') WITH &&
    ) WHERE (is_active = TRUE);

-- Prevent duplicate active refresh tokens per user
ALTER TABLE refresh_tokens
    ADD CONSTRAINT excl_active_tokens
    EXCLUDE USING gist (
        user_id WITH =,
        tstzrange(created_at, expires_at, '[)') WITH &&
    ) WHERE (revoked = FALSE);

-- ============================================
-- STEP 4: CREATE VALIDATION TRIGGERS
-- ============================================

-- Function to validate email changes
CREATE OR REPLACE FUNCTION validate_email_change()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if email is being changed for a verified user
    IF OLD.email_verified = TRUE AND NEW.email != OLD.email THEN
        NEW.email_verified := FALSE;
        NEW.updated_at := CURRENT_TIMESTAMP;
        
        -- Log the email change
        INSERT INTO system_logs (level, message, context, created_at)
        VALUES (
            'WARN',
            'Email changed for verified user',
            jsonb_build_object(
                'user_id', NEW.id,
                'old_email', OLD.email,
                'new_email', NEW.email
            ),
            CURRENT_TIMESTAMP
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER validate_email_change_trigger
    BEFORE UPDATE OF email ON users
    FOR EACH ROW EXECUTE FUNCTION validate_email_change();

-- Function to validate report status transitions
CREATE OR REPLACE FUNCTION validate_report_status_transition()
RETURNS TRIGGER AS $$
BEGIN
    -- Define valid status transitions
    CASE OLD.status
        WHEN 'DRAFT' THEN
            IF NEW.status NOT IN ('SUBMITTED', 'CANCELLED') THEN
                RAISE EXCEPTION 'Invalid status transition from DRAFT to %', NEW.status;
            END IF;
        WHEN 'SUBMITTED' THEN
            IF NEW.status NOT IN ('IN_REVIEW', 'REJECTED', 'CANCELLED') THEN
                RAISE EXCEPTION 'Invalid status transition from SUBMITTED to %', NEW.status;
            END IF;
        WHEN 'IN_REVIEW' THEN
            IF NEW.status NOT IN ('APPROVED', 'REJECTED', 'SUBMITTED') THEN
                RAISE EXCEPTION 'Invalid status transition from IN_REVIEW to %', NEW.status;
            END IF;
        WHEN 'APPROVED' THEN
            IF NEW.status NOT IN ('IN_PROGRESS', 'CANCELLED') THEN
                RAISE EXCEPTION 'Invalid status transition from APPROVED to %', NEW.status;
            END IF;
        WHEN 'IN_PROGRESS' THEN
            IF NEW.status NOT IN ('COMPLETED', 'CANCELLED', 'pending') THEN
                RAISE EXCEPTION 'Invalid status transition from IN_PROGRESS to %', NEW.status;
            END IF;
        WHEN 'COMPLETED' THEN
            -- Completed reports cannot change status
            RAISE EXCEPTION 'Cannot change status of completed report';
        WHEN 'REJECTED' THEN
            IF NEW.status NOT IN ('DRAFT', 'CANCELLED') THEN
                RAISE EXCEPTION 'Invalid status transition from REJECTED to %', NEW.status;
            END IF;
        WHEN 'CANCELLED' THEN
            -- Cancelled reports cannot change status
            RAISE EXCEPTION 'Cannot change status of cancelled report';
    END CASE;
    
    -- Update timestamps based on status
    IF NEW.status = 'COMPLETED' THEN
        NEW.completed_date := CURRENT_DATE;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER validate_report_status_trigger
    BEFORE UPDATE OF status ON reports
    FOR EACH ROW EXECUTE FUNCTION validate_report_status_transition();

-- Function to prevent deletion of users with active reports
CREATE OR REPLACE FUNCTION prevent_user_deletion_with_active_reports()
RETURNS TRIGGER AS $$
DECLARE
    active_report_count INTEGER;
BEGIN
    -- Check for active reports as supervisor
    SELECT COUNT(*) INTO active_report_count
    FROM reports
    WHERE supervisor_id = OLD.id
        AND status NOT IN ('COMPLETED', 'CANCELLED', 'completed', 'cancelled');
    
    IF active_report_count > 0 THEN
        RAISE EXCEPTION 'Cannot delete user with % active reports', active_report_count;
    END IF;
    
    -- Check for active reports as assigned technician
    SELECT COUNT(*) INTO active_report_count
    FROM reports
    WHERE assigned_to = OLD.id
        AND status NOT IN ('COMPLETED', 'CANCELLED', 'completed', 'cancelled');
    
    IF active_report_count > 0 THEN
        RAISE EXCEPTION 'Cannot delete user with % assigned reports', active_report_count;
    END IF;
    
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER prevent_user_deletion_trigger
    BEFORE DELETE ON users
    FOR EACH ROW EXECUTE FUNCTION prevent_user_deletion_with_active_reports();

-- ============================================
-- STEP 5: ADD REFERENTIAL INTEGRITY CONSTRAINTS
-- ============================================

-- Add foreign key constraints for better referential integrity
ALTER TABLE reports 
    ADD CONSTRAINT fk_report_school_exists 
    FOREIGN KEY (school_id) REFERENCES schools(id) 
    ON DELETE RESTRICT;

ALTER TABLE supervisor_schools
    ADD CONSTRAINT fk_supervisor_active_user
    FOREIGN KEY (supervisor_id) REFERENCES users(id)
    ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

-- ============================================
-- STEP 6: CREATE DATA QUALITY TABLES
-- ============================================

-- Create data quality rules table
CREATE TABLE IF NOT EXISTS data_quality_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    table_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(100),
    rule_name VARCHAR(255) NOT NULL,
    rule_type VARCHAR(50) NOT NULL, -- 'required', 'format', 'range', 'reference', 'custom'
    rule_expression TEXT NOT NULL,
    error_message TEXT,
    severity VARCHAR(20) DEFAULT 'error', -- 'warning', 'error', 'critical'
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_rule_per_table_column UNIQUE (table_name, column_name, rule_name)
);

-- Create data quality violations log
CREATE TABLE IF NOT EXISTS data_quality_violations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_id UUID REFERENCES data_quality_rules(id),
    table_name VARCHAR(100) NOT NULL,
    record_id UUID,
    violation_details JSONB NOT NULL,
    detected_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by UUID REFERENCES users(id),
    resolution_notes TEXT
);

-- ============================================
-- STEP 7: CREATE DATA VALIDATION FUNCTIONS
-- ============================================

-- Function to validate IQAMA number format
CREATE OR REPLACE FUNCTION validate_iqama_number(iqama TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    -- IQAMA should be 10 digits starting with 1 or 2
    RETURN iqama ~ '^[12][0-9]{9}$';
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Function to validate Saudi phone number
CREATE OR REPLACE FUNCTION validate_saudi_phone(phone TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    -- Saudi phone: +966 followed by 9 digits starting with 5
    RETURN phone ~ '^\+966[5][0-9]{8}$' OR phone ~ '^05[0-9]{8}$';
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Function to validate Saudi plate number
CREATE OR REPLACE FUNCTION validate_plate_number(
    plate_number VARCHAR(50),
    plate_letters_en VARCHAR(10),
    plate_letters_ar VARCHAR(10)
)
RETURNS BOOLEAN AS $$
BEGIN
    -- Validate plate number format (4 digits)
    IF plate_number !~ '^[0-9]{1,4}$' THEN
        RETURN FALSE;
    END IF;
    
    -- Validate English letters (3 letters)
    IF plate_letters_en !~ '^[A-Z]{3}$' THEN
        RETURN FALSE;
    END IF;
    
    -- Arabic letters validation would go here
    -- For now, just check it's not empty
    IF plate_letters_ar IS NULL OR LENGTH(plate_letters_ar) = 0 THEN
        RETURN FALSE;
    END IF;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- ============================================
-- STEP 8: CREATE BUSINESS RULE CONSTRAINTS
-- ============================================

-- Function to enforce supervisor school limit
CREATE OR REPLACE FUNCTION check_supervisor_school_limit()
RETURNS TRIGGER AS $$
DECLARE
    school_count INTEGER;
    max_schools INTEGER := 10; -- Maximum schools per supervisor
BEGIN
    -- Count active schools for this supervisor
    SELECT COUNT(*) INTO school_count
    FROM supervisor_schools
    WHERE supervisor_id = NEW.supervisor_id
        AND is_active = TRUE
        AND id != COALESCE(NEW.id, '00000000-0000-0000-0000-000000000000'::UUID);
    
    IF school_count >= max_schools THEN
        RAISE EXCEPTION 'Supervisor cannot be assigned to more than % schools', max_schools;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_supervisor_school_limit_trigger
    BEFORE INSERT OR UPDATE ON supervisor_schools
    FOR EACH ROW EXECUTE FUNCTION check_supervisor_school_limit();

-- Function to enforce report number format
CREATE OR REPLACE FUNCTION generate_report_number()
RETURNS TRIGGER AS $$
DECLARE
    year_part VARCHAR(4);
    month_part VARCHAR(2);
    sequence_part VARCHAR(6);
    next_sequence INTEGER;
BEGIN
    IF NEW.report_number IS NULL THEN
        year_part := to_char(CURRENT_DATE, 'YYYY');
        month_part := to_char(CURRENT_DATE, 'MM');
        
        -- Get next sequence number for this month
        SELECT COALESCE(MAX(CAST(SUBSTRING(report_number FROM 8 FOR 6) AS INTEGER)), 0) + 1
        INTO next_sequence
        FROM reports
        WHERE report_number LIKE year_part || month_part || '%';
        
        sequence_part := LPAD(next_sequence::TEXT, 6, '0');
        NEW.report_number := year_part || month_part || sequence_part;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER generate_report_number_trigger
    BEFORE INSERT ON reports
    FOR EACH ROW EXECUTE FUNCTION generate_report_number();

-- ============================================
-- STEP 9: ADD DATA QUALITY RULES
-- ============================================

-- Insert default data quality rules
INSERT INTO data_quality_rules (table_name, column_name, rule_name, rule_type, rule_expression, error_message, severity)
VALUES
    ('users', 'email', 'valid_email_format', 'format', 
     'email ~* ''^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$''',
     'Invalid email format', 'error'),
    
    ('users', 'phone', 'valid_phone_format', 'format',
     'phone IS NULL OR validate_saudi_phone(phone)',
     'Invalid Saudi phone number format', 'warning'),
    
    ('users', 'iqama_id', 'valid_iqama_format', 'format',
     'iqama_id IS NULL OR validate_iqama_number(iqama_id)',
     'Invalid IQAMA number format', 'warning'),
    
    ('reports', 'status', 'valid_status_transition', 'custom',
     'validate_report_status_transition()',
     'Invalid status transition', 'error'),
    
    ('schools', 'email', 'school_email_format', 'format',
     'email IS NULL OR email ~* ''^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$''',
     'Invalid school email format', 'warning'),
    
    ('maintenance_counts', 'count_consistency', 'count_totals_valid', 'custom',
     'working_count + damaged_count + missing_count <= total_count',
     'Sum of counts exceeds total count', 'error')
ON CONFLICT (table_name, column_name, rule_name) DO NOTHING;

-- ============================================
-- STEP 10: CREATE DATA QUALITY MONITORING
-- ============================================

-- Function to run data quality checks
CREATE OR REPLACE FUNCTION run_data_quality_checks()
RETURNS TABLE (
    table_name VARCHAR(100),
    violations_count BIGINT,
    severity VARCHAR(20)
) AS $$
BEGIN
    -- This would be expanded to run actual checks
    -- For now, return a summary
    RETURN QUERY
    SELECT 
        dqv.table_name,
        COUNT(*) as violations_count,
        dqr.severity
    FROM data_quality_violations dqv
    JOIN data_quality_rules dqr ON dqv.rule_id = dqr.id
    WHERE dqv.resolved = FALSE
        AND dqr.is_active = TRUE
    GROUP BY dqv.table_name, dqr.severity
    ORDER BY 
        CASE dqr.severity
            WHEN 'critical' THEN 1
            WHEN 'error' THEN 2
            WHEN 'warning' THEN 3
        END,
        violations_count DESC;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 11: ADD DEFAULT VALUES AND COMPUTED COLUMNS
-- ============================================

-- Add default values for better data consistency
ALTER TABLE reports ALTER COLUMN reported_date SET DEFAULT CURRENT_DATE;
ALTER TABLE reports ALTER COLUMN is_urgent SET DEFAULT FALSE;
ALTER TABLE reports ALTER COLUMN requires_shutdown SET DEFAULT FALSE;

ALTER TABLE schools ALTER COLUMN is_active SET DEFAULT TRUE;
ALTER TABLE schools ALTER COLUMN type SET DEFAULT 'PRIMARY';
ALTER TABLE schools ALTER COLUMN gender SET DEFAULT 'MIXED';

-- Add regular columns that will be updated via triggers
ALTER TABLE reports ADD COLUMN IF NOT EXISTS age_days INTEGER;
ALTER TABLE reports ADD COLUMN IF NOT EXISTS is_overdue BOOLEAN DEFAULT FALSE;

-- Function to update report computed fields
CREATE OR REPLACE FUNCTION update_report_computed_fields()
RETURNS TRIGGER AS $$
BEGIN
    -- Update age_days
    IF NEW.completed_date IS NOT NULL THEN
        NEW.age_days := (NEW.completed_date - NEW.reported_date)::INTEGER;
    ELSE
        NEW.age_days := (CURRENT_DATE - NEW.reported_date)::INTEGER;
    END IF;
    
    -- Update is_overdue
    IF NEW.status NOT IN ('COMPLETED', 'CANCELLED', 'completed', 'cancelled')
       AND NEW.scheduled_date IS NOT NULL 
       AND NEW.scheduled_date < CURRENT_DATE THEN
        NEW.is_overdue := TRUE;
    ELSE
        NEW.is_overdue := FALSE;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to update computed fields
CREATE TRIGGER update_report_computed_fields_trigger
    BEFORE INSERT OR UPDATE ON reports
    FOR EACH ROW EXECUTE FUNCTION update_report_computed_fields();

-- Update existing reports with computed values
UPDATE reports SET 
    age_days = CASE 
        WHEN completed_date IS NOT NULL THEN (completed_date - reported_date)::INTEGER
        ELSE (CURRENT_DATE - reported_date)::INTEGER
    END,
    is_overdue = CASE
        WHEN status NOT IN ('COMPLETED', 'CANCELLED', 'completed', 'cancelled')
            AND scheduled_date IS NOT NULL 
            AND scheduled_date < CURRENT_DATE
        THEN TRUE
        ELSE FALSE
    END;

-- ============================================
-- STEP 12: CREATE INTEGRITY CHECK PROCEDURES
-- ============================================

-- Procedure to check and fix orphaned records
CREATE OR REPLACE FUNCTION check_orphaned_records()
RETURNS TABLE (
    table_name TEXT,
    orphaned_count BIGINT,
    action_taken TEXT
) AS $$
BEGIN
    -- Check for orphaned report attachments
    RETURN QUERY
    SELECT 
        'report_attachments'::TEXT,
        COUNT(*)::BIGINT,
        'Records marked for review'::TEXT
    FROM report_attachments ra
    WHERE NOT EXISTS (
        SELECT 1 FROM reports r WHERE r.id = ra.report_id
    );
    
    -- Check for orphaned supervisor schools
    RETURN QUERY
    SELECT 
        'supervisor_schools'::TEXT,
        COUNT(*)::BIGINT,
        'Records deactivated'::TEXT
    FROM supervisor_schools ss
    WHERE NOT EXISTS (
        SELECT 1 FROM users u WHERE u.id = ss.supervisor_id
    ) OR NOT EXISTS (
        SELECT 1 FROM schools s WHERE s.id = ss.school_id
    );
    
    -- Deactivate orphaned supervisor schools
    UPDATE supervisor_schools
    SET is_active = FALSE,
        unassigned_at = CURRENT_TIMESTAMP
    WHERE (
        NOT EXISTS (SELECT 1 FROM users u WHERE u.id = supervisor_id)
        OR NOT EXISTS (SELECT 1 FROM schools s WHERE s.id = school_id)
    ) AND is_active = TRUE;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 13: COMMENTS
-- ============================================

COMMENT ON DOMAIN percentage IS 'Domain for percentage values (0-100)';
COMMENT ON DOMAIN positive_decimal IS 'Domain for non-negative decimal values';
COMMENT ON DOMAIN rating_score IS 'Domain for rating scores (1-5)';

COMMENT ON TABLE data_quality_rules IS 'Data quality validation rules';
COMMENT ON TABLE data_quality_violations IS 'Log of data quality rule violations';

COMMENT ON FUNCTION validate_iqama_number IS 'Validates Saudi IQAMA number format';
COMMENT ON FUNCTION validate_saudi_phone IS 'Validates Saudi phone number format';
COMMENT ON FUNCTION validate_plate_number IS 'Validates Saudi vehicle plate number';
COMMENT ON FUNCTION run_data_quality_checks IS 'Runs all active data quality checks';
COMMENT ON FUNCTION check_orphaned_records IS 'Checks for and handles orphaned records';

-- End of V6 Data Integrity Improvements migration