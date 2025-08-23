-- ============================================
-- V8__Soft_Delete_Implementation.sql
-- Database Optimization Phase 4: Soft Delete Pattern Implementation
-- Implements comprehensive soft delete across all entities
-- ============================================

-- ============================================
-- STEP 1: ADD SOFT DELETE COLUMNS TO REMAINING TABLES
-- ============================================

-- Add soft delete to reports table
ALTER TABLE reports 
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS deletion_reason TEXT;

-- Add soft delete to schools table
ALTER TABLE schools
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS deletion_reason TEXT;

-- Add soft delete to supervisor_schools table
ALTER TABLE supervisor_schools
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

-- Add soft delete to maintenance_counts table
ALTER TABLE maintenance_counts
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

-- Add soft delete to damage_counts table
ALTER TABLE damage_counts
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

-- Add soft delete to report_attachments table
ALTER TABLE report_attachments
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

-- Add soft delete to report_comments table
ALTER TABLE report_comments
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS edited_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS edited_by UUID REFERENCES users(id);

-- Add soft delete to supervisor_attendance table
ALTER TABLE supervisor_attendance
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

-- Add soft delete to achievement_photos table
ALTER TABLE achievement_photos
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

-- Add soft delete to maintenance_reports table (if exists)
ALTER TABLE maintenance_reports
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

-- Add soft delete to school_achievements table (if exists)
ALTER TABLE school_achievements
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

-- Add soft delete to notifications table
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

-- Add soft delete to file_uploads table
ALTER TABLE file_uploads
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

-- ============================================
-- STEP 2: CREATE SOFT DELETE INDEXES
-- ============================================

-- Create partial indexes that exclude soft-deleted records for better performance
CREATE INDEX IF NOT EXISTS idx_reports_active ON reports(id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_reports_school_active ON reports(school_id, status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_reports_supervisor_active ON reports(supervisor_id, status) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_schools_active ON schools(id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_schools_code_active ON schools(code) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_supervisor_schools_active ON supervisor_schools(supervisor_id, school_id) 
    WHERE deleted_at IS NULL AND is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_maintenance_counts_active ON maintenance_counts(school_id, count_date DESC) 
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_damage_counts_active ON damage_counts(school_id, assessment_date DESC) 
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_notifications_user_active ON notifications(user_id, created_at DESC) 
    WHERE deleted_at IS NULL AND read = FALSE;

-- ============================================
-- STEP 3: CREATE SOFT DELETE FUNCTIONS
-- ============================================

-- Generic soft delete function
CREATE OR REPLACE FUNCTION soft_delete_record(
    p_table_name TEXT,
    p_record_id UUID,
    p_deleted_by UUID,
    p_deletion_reason TEXT DEFAULT NULL
)
RETURNS BOOLEAN AS $$
DECLARE
    query TEXT;
    result BOOLEAN;
BEGIN
    -- Build dynamic query based on whether deletion_reason column exists
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = p_table_name 
        AND column_name = 'deletion_reason'
    ) THEN
        query := format(
            'UPDATE %I SET deleted_at = CURRENT_TIMESTAMP, deleted_by = $1, deletion_reason = $2 
             WHERE id = $3 AND deleted_at IS NULL',
            p_table_name
        );
        EXECUTE query USING p_deleted_by, p_deletion_reason, p_record_id;
    ELSE
        query := format(
            'UPDATE %I SET deleted_at = CURRENT_TIMESTAMP, deleted_by = $1 
             WHERE id = $2 AND deleted_at IS NULL',
            p_table_name
        );
        EXECUTE query USING p_deleted_by, p_record_id;
    END IF;
    
    GET DIAGNOSTICS result = ROW_COUNT;
    RETURN result > 0;
END;
$$ LANGUAGE plpgsql;

-- Function to restore soft-deleted record
CREATE OR REPLACE FUNCTION restore_deleted_record(
    p_table_name TEXT,
    p_record_id UUID
)
RETURNS BOOLEAN AS $$
DECLARE
    query TEXT;
    result BOOLEAN;
BEGIN
    query := format(
        'UPDATE %I SET deleted_at = NULL, deleted_by = NULL, deletion_reason = NULL 
         WHERE id = $1 AND deleted_at IS NOT NULL',
        p_table_name
    );
    EXECUTE query USING p_record_id;
    
    GET DIAGNOSTICS result = ROW_COUNT;
    RETURN result > 0;
END;
$$ LANGUAGE plpgsql;

-- Function to permanently delete soft-deleted records older than specified days
CREATE OR REPLACE FUNCTION purge_deleted_records(
    p_table_name TEXT,
    p_days_old INTEGER DEFAULT 90
)
RETURNS INTEGER AS $$
DECLARE
    query TEXT;
    deleted_count INTEGER;
BEGIN
    query := format(
        'DELETE FROM %I WHERE deleted_at < CURRENT_TIMESTAMP - INTERVAL ''%s days''',
        p_table_name, p_days_old
    );
    EXECUTE query;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    -- Log the purge operation
    INSERT INTO system_logs (level, message, context, created_at)
    VALUES (
        'INFO',
        format('Purged %s records from %s older than %s days', deleted_count, p_table_name, p_days_old),
        jsonb_build_object(
            'table_name', p_table_name,
            'days_old', p_days_old,
            'deleted_count', deleted_count
        ),
        CURRENT_TIMESTAMP
    );
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 4: CREATE CASCADE SOFT DELETE TRIGGERS
-- ============================================

-- Trigger function for cascade soft delete on reports
CREATE OR REPLACE FUNCTION cascade_soft_delete_reports()
RETURNS TRIGGER AS $$
BEGIN
    -- Soft delete related report attachments
    UPDATE report_attachments 
    SET deleted_at = NEW.deleted_at, deleted_by = NEW.deleted_by
    WHERE report_id = NEW.id AND deleted_at IS NULL;
    
    -- Soft delete related report comments
    UPDATE report_comments 
    SET deleted_at = NEW.deleted_at, deleted_by = NEW.deleted_by
    WHERE report_id = NEW.id AND deleted_at IS NULL;
    
    -- Soft delete related achievement photos
    UPDATE achievement_photos 
    SET deleted_at = NEW.deleted_at, deleted_by = NEW.deleted_by
    WHERE report_id = NEW.id AND deleted_at IS NULL;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER cascade_soft_delete_reports_trigger
    AFTER UPDATE OF deleted_at ON reports
    FOR EACH ROW 
    WHEN (OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL)
    EXECUTE FUNCTION cascade_soft_delete_reports();

-- Trigger function for cascade soft delete on schools
CREATE OR REPLACE FUNCTION cascade_soft_delete_schools()
RETURNS TRIGGER AS $$
BEGIN
    -- Soft delete supervisor school assignments
    UPDATE supervisor_schools 
    SET deleted_at = NEW.deleted_at, deleted_by = NEW.deleted_by
    WHERE school_id = NEW.id AND deleted_at IS NULL;
    
    -- Note: We don't cascade delete reports as they may need to be retained for audit
    -- But we can mark them as from a deleted school
    UPDATE reports 
    SET school_name = school_name || ' (DELETED SCHOOL)'
    WHERE school_id = NEW.id 
        AND deleted_at IS NULL 
        AND school_name NOT LIKE '%(DELETED SCHOOL)%';
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER cascade_soft_delete_schools_trigger
    AFTER UPDATE OF deleted_at ON schools
    FOR EACH ROW 
    WHEN (OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL)
    EXECUTE FUNCTION cascade_soft_delete_schools();

-- ============================================
-- STEP 5: CREATE VIEWS FOR ACTIVE RECORDS
-- ============================================

-- View for active reports (non-deleted)
CREATE OR REPLACE VIEW v_active_reports AS
SELECT * FROM reports WHERE deleted_at IS NULL;

-- View for active schools
CREATE OR REPLACE VIEW v_active_schools AS
SELECT * FROM schools WHERE deleted_at IS NULL AND is_active = TRUE;

-- View for active supervisor schools
CREATE OR REPLACE VIEW v_active_supervisor_schools AS
SELECT ss.*, s.name as school_name, u.full_name as supervisor_name
FROM supervisor_schools ss
JOIN schools s ON ss.school_id = s.id
JOIN users u ON ss.supervisor_id = u.id
WHERE ss.deleted_at IS NULL 
    AND ss.is_active = TRUE
    AND s.deleted_at IS NULL
    AND u.deleted_at IS NULL;

-- View for active maintenance counts
CREATE OR REPLACE VIEW v_active_maintenance_counts AS
SELECT mc.*, s.name as school_name
FROM maintenance_counts mc
JOIN schools s ON mc.school_id = s.id
WHERE mc.deleted_at IS NULL AND s.deleted_at IS NULL;

-- ============================================
-- STEP 6: CREATE DELETED RECORDS TRACKING
-- ============================================

-- Table to track deletion statistics
CREATE TABLE IF NOT EXISTS deletion_stats (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    table_name VARCHAR(100) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    soft_deleted_count INTEGER DEFAULT 0,
    restored_count INTEGER DEFAULT 0,
    purged_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_deletion_stats_period UNIQUE (table_name, period_start, period_end)
);

-- Function to update deletion statistics
CREATE OR REPLACE FUNCTION update_deletion_stats()
RETURNS VOID AS $$
DECLARE
    table_record RECORD;
    deleted_count INTEGER;
    current_month_start DATE;
    current_month_end DATE;
BEGIN
    current_month_start := date_trunc('month', CURRENT_DATE);
    current_month_end := (date_trunc('month', CURRENT_DATE) + INTERVAL '1 month - 1 day')::DATE;
    
    -- List of tables with soft delete
    FOR table_record IN 
        SELECT table_name::TEXT 
        FROM information_schema.columns 
        WHERE column_name = 'deleted_at' 
        AND table_schema = 'public'
    LOOP
        -- Count soft deleted records for current month
        EXECUTE format(
            'SELECT COUNT(*) FROM %I WHERE deleted_at >= $1 AND deleted_at <= $2',
            table_record.table_name
        ) INTO deleted_count USING current_month_start, current_month_end;
        
        -- Update or insert stats
        INSERT INTO deletion_stats (
            table_name, period_start, period_end, soft_deleted_count
        ) VALUES (
            table_record.table_name, current_month_start, current_month_end, deleted_count
        )
        ON CONFLICT (table_name, period_start, period_end) 
        DO UPDATE SET soft_deleted_count = deleted_count;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 7: CREATE RECYCLE BIN FUNCTIONALITY
-- ============================================

-- Recycle bin view to show recently deleted items
CREATE OR REPLACE VIEW v_recycle_bin AS
SELECT 
    'reports' as table_name,
    id,
    report_number as identifier,
    title as description,
    deleted_at,
    deleted_by,
    deletion_reason
FROM reports
WHERE deleted_at IS NOT NULL 
    AND deleted_at > CURRENT_TIMESTAMP - INTERVAL '30 days'

UNION ALL

SELECT 
    'schools' as table_name,
    id,
    code as identifier,
    name as description,
    deleted_at,
    deleted_by,
    deletion_reason
FROM schools
WHERE deleted_at IS NOT NULL 
    AND deleted_at > CURRENT_TIMESTAMP - INTERVAL '30 days'

UNION ALL

SELECT 
    'users' as table_name,
    id,
    username as identifier,
    full_name as description,
    deleted_at,
    deleted_by,
    NULL as deletion_reason
FROM users
WHERE deleted_at IS NOT NULL 
    AND deleted_at > CURRENT_TIMESTAMP - INTERVAL '30 days'

ORDER BY deleted_at DESC;

-- ============================================
-- STEP 8: CREATE POLICIES FOR SOFT DELETE
-- ============================================

-- Function to check if user can delete records
CREATE OR REPLACE FUNCTION can_soft_delete(
    p_user_id UUID,
    p_table_name TEXT,
    p_record_id UUID
)
RETURNS BOOLEAN AS $$
DECLARE
    user_type_val user_type;
    is_owner BOOLEAN;
BEGIN
    -- Get user type
    SELECT user_type INTO user_type_val
    FROM users WHERE id = p_user_id AND deleted_at IS NULL;
    
    -- Admins and super admins can delete anything
    IF user_type_val IN ('admin', 'super_admin') THEN
        RETURN TRUE;
    END IF;
    
    -- Check ownership for specific tables
    IF p_table_name = 'reports' THEN
        SELECT EXISTS(
            SELECT 1 FROM reports 
            WHERE id = p_record_id AND supervisor_id = p_user_id
        ) INTO is_owner;
        RETURN is_owner;
    END IF;
    
    -- Default: deny
    RETURN FALSE;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 9: CREATE CLEANUP PROCEDURES
-- ============================================

-- Scheduled cleanup procedure
CREATE OR REPLACE FUNCTION scheduled_deletion_cleanup()
RETURNS TABLE (
    table_name TEXT,
    purged_count INTEGER
) AS $$
DECLARE
    rec RECORD;
    count INTEGER;
BEGIN
    -- Purge old deleted records from each table
    FOR rec IN 
        SELECT DISTINCT t.table_name::TEXT 
        FROM information_schema.columns t
        WHERE t.column_name = 'deleted_at' 
        AND t.table_schema = 'public'
    LOOP
        count := purge_deleted_records(rec.table_name, 90);
        RETURN QUERY SELECT rec.table_name, count;
    END LOOP;
    
    -- Update deletion stats
    PERFORM update_deletion_stats();
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 10: ADD UNIQUE CONSTRAINTS FOR SOFT DELETE
-- ============================================

-- Ensure unique constraints work with soft delete
-- Drop existing unique constraints and recreate as partial unique indexes

-- For users table
DROP INDEX IF EXISTS unique_active_email;
DROP INDEX IF EXISTS unique_active_username;
CREATE UNIQUE INDEX unique_active_email ON users(LOWER(email)) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX unique_active_username ON users(LOWER(username)) WHERE deleted_at IS NULL;

-- For schools table
ALTER TABLE schools DROP CONSTRAINT IF EXISTS schools_code_key;
CREATE UNIQUE INDEX unique_active_school_code ON schools(code) WHERE deleted_at IS NULL;

-- For reports table
ALTER TABLE reports DROP CONSTRAINT IF EXISTS reports_report_number_key;
CREATE UNIQUE INDEX unique_active_report_number ON reports(report_number) WHERE deleted_at IS NULL;

-- ============================================
-- STEP 11: CREATE SOFT DELETE AUDIT TRIGGERS
-- ============================================

-- Audit soft deletes
CREATE OR REPLACE FUNCTION audit_soft_delete()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL THEN
        -- Record soft delete in audit log
        INSERT INTO audit_log (
            table_name,
            record_id,
            operation,
            user_id,
            old_values,
            new_values,
            changed_fields,
            operation_timestamp
        ) VALUES (
            TG_TABLE_NAME,
            NEW.id,
            'SOFT_DELETE',
            NEW.deleted_by,
            to_jsonb(OLD),
            to_jsonb(NEW),
            ARRAY['deleted_at', 'deleted_by', 'deletion_reason'],
            CURRENT_TIMESTAMP
        );
    ELSIF OLD.deleted_at IS NOT NULL AND NEW.deleted_at IS NULL THEN
        -- Record restore in audit log
        INSERT INTO audit_log (
            table_name,
            record_id,
            operation,
            user_id,
            old_values,
            new_values,
            changed_fields,
            operation_timestamp
        ) VALUES (
            TG_TABLE_NAME,
            NEW.id,
            'RESTORE',
            current_setting('app.current_user_id', TRUE)::UUID,
            to_jsonb(OLD),
            to_jsonb(NEW),
            ARRAY['deleted_at', 'deleted_by', 'deletion_reason'],
            CURRENT_TIMESTAMP
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply audit triggers to main tables
CREATE TRIGGER audit_soft_delete_reports
    AFTER UPDATE OF deleted_at ON reports
    FOR EACH ROW EXECUTE FUNCTION audit_soft_delete();

CREATE TRIGGER audit_soft_delete_schools
    AFTER UPDATE OF deleted_at ON schools
    FOR EACH ROW EXECUTE FUNCTION audit_soft_delete();

CREATE TRIGGER audit_soft_delete_users
    AFTER UPDATE OF deleted_at ON users
    FOR EACH ROW EXECUTE FUNCTION audit_soft_delete();

-- ============================================
-- STEP 12: COMMENTS
-- ============================================

COMMENT ON FUNCTION soft_delete_record IS 'Generic function to soft delete a record from any table';
COMMENT ON FUNCTION restore_deleted_record IS 'Restore a soft-deleted record';
COMMENT ON FUNCTION purge_deleted_records IS 'Permanently delete soft-deleted records older than specified days';
COMMENT ON FUNCTION cascade_soft_delete_reports IS 'Cascade soft delete to related report records';
COMMENT ON FUNCTION cascade_soft_delete_schools IS 'Cascade soft delete to related school records';
COMMENT ON FUNCTION can_soft_delete IS 'Check if user has permission to soft delete a record';
COMMENT ON FUNCTION scheduled_deletion_cleanup IS 'Scheduled cleanup of old soft-deleted records';

COMMENT ON VIEW v_active_reports IS 'View of all non-deleted reports';
COMMENT ON VIEW v_active_schools IS 'View of all non-deleted active schools';
COMMENT ON VIEW v_recycle_bin IS 'Recently deleted items that can be restored';

COMMENT ON TABLE deletion_stats IS 'Statistics tracking for soft delete operations';

-- End of V8 Soft Delete Implementation migration