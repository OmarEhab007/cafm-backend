-- ============================================
-- V7__Comprehensive_Audit_System.sql
-- Database Optimization Phase 3: Comprehensive Audit System
-- Implements full audit logging with triggers and history tracking
-- ============================================

-- ============================================
-- STEP 1: CREATE AUDIT SCHEMA
-- ============================================

-- Create separate schema for audit tables (optional, for better organization)
-- CREATE SCHEMA IF NOT EXISTS audit;

-- ============================================
-- STEP 2: CREATE AUDIT LOG TABLE
-- ============================================

-- Main audit log table
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    -- Table and record information
    table_name VARCHAR(100) NOT NULL,
    record_id UUID,
    
    -- Operation details
    operation VARCHAR(20) NOT NULL, -- INSERT, UPDATE, DELETE, SELECT, TRUNCATE
    operation_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- User information
    user_id UUID REFERENCES users(id),
    username VARCHAR(100),
    user_ip INET,
    user_agent TEXT,
    session_id VARCHAR(255),
    
    -- Data changes
    old_values JSONB,
    new_values JSONB,
    changed_fields TEXT[],
    
    -- Additional context
    query_text TEXT,
    row_count INTEGER,
    execution_time_ms INTEGER,
    
    -- Application context
    application_name VARCHAR(100),
    application_version VARCHAR(50),
    request_id VARCHAR(255),
    correlation_id VARCHAR(255),
    
    -- Indexing for performance
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for audit log
CREATE INDEX idx_audit_log_table_record ON audit_log(table_name, record_id);
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_operation ON audit_log(operation);
CREATE INDEX idx_audit_log_timestamp ON audit_log(operation_timestamp DESC);
CREATE INDEX idx_audit_log_correlation ON audit_log(correlation_id);
CREATE INDEX idx_audit_log_session ON audit_log(session_id);

-- Regular index for recent records (without WHERE clause since CURRENT_TIMESTAMP is not immutable)
CREATE INDEX idx_audit_log_recent ON audit_log(operation_timestamp DESC);

-- ============================================
-- STEP 3: CREATE HISTORY TABLES FOR KEY ENTITIES
-- ============================================

-- Reports history table
CREATE TABLE IF NOT EXISTS reports_history (
    history_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    
    -- All columns from reports table
    report_number VARCHAR(50),
    school_id UUID,
    supervisor_id UUID,
    assigned_to UUID,
    title VARCHAR(255),
    description TEXT,
    category VARCHAR(100),
    sub_category VARCHAR(100),
    status report_status,
    priority priority_level,
    reported_date DATE,
    scheduled_date DATE,
    completed_date DATE,
    work_description TEXT,
    materials_used TEXT,
    labor_hours DECIMAL(5, 2),
    estimated_cost DECIMAL(10, 2),
    actual_cost DECIMAL(10, 2),
    reviewed_by UUID,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    approved_by UUID,
    approved_at TIMESTAMP WITH TIME ZONE,
    is_urgent BOOLEAN,
    requires_shutdown BOOLEAN,
    
    -- History metadata
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_to TIMESTAMP WITH TIME ZONE,
    modified_by UUID REFERENCES users(id),
    modification_reason TEXT,
    
    -- Create composite index for versioning
    CONSTRAINT unique_report_version UNIQUE (id, version_number)
);

CREATE INDEX idx_reports_history_id ON reports_history(id);
CREATE INDEX idx_reports_history_valid_from ON reports_history(valid_from DESC);
CREATE INDEX idx_reports_history_valid_to ON reports_history(valid_to DESC);

-- Users history table
CREATE TABLE IF NOT EXISTS users_history (
    history_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    
    -- User fields (excluding password for security)
    email VARCHAR(255),
    username VARCHAR(100),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    employee_id VARCHAR(50),
    user_type user_type,
    status user_status,
    department VARCHAR(100),
    position VARCHAR(100),
    is_active BOOLEAN,
    is_locked BOOLEAN,
    
    -- History metadata
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_to TIMESTAMP WITH TIME ZONE,
    modified_by UUID REFERENCES users(id),
    modification_reason TEXT,
    
    CONSTRAINT unique_user_version UNIQUE (id, version_number)
);

CREATE INDEX idx_users_history_id ON users_history(id);
CREATE INDEX idx_users_history_valid_from ON users_history(valid_from DESC);

-- Schools history table
CREATE TABLE IF NOT EXISTS schools_history (
    history_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    
    -- School fields
    code VARCHAR(50),
    name VARCHAR(255),
    name_ar VARCHAR(255),
    type VARCHAR(50),
    gender VARCHAR(20),
    address TEXT,
    city VARCHAR(100),
    district VARCHAR(100),
    postal_code VARCHAR(20),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    phone VARCHAR(20),
    email VARCHAR(255),
    principal_name VARCHAR(255),
    is_active BOOLEAN,
    student_count INTEGER,
    staff_count INTEGER,
    building_area DECIMAL(10, 2),
    
    -- History metadata
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_to TIMESTAMP WITH TIME ZONE,
    modified_by UUID REFERENCES users(id),
    modification_reason TEXT,
    
    CONSTRAINT unique_school_version UNIQUE (id, version_number)
);

CREATE INDEX idx_schools_history_id ON schools_history(id);
CREATE INDEX idx_schools_history_valid_from ON schools_history(valid_from DESC);

-- ============================================
-- STEP 4: CREATE AUDIT TRIGGER FUNCTIONS
-- ============================================

-- Generic audit trigger function
CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
DECLARE
    audit_user_id UUID;
    audit_username VARCHAR(100);
    old_data JSONB;
    new_data JSONB;
    changed_fields TEXT[];
BEGIN
    -- Get current user information (would be set by application)
    audit_user_id := current_setting('app.current_user_id', TRUE)::UUID;
    audit_username := current_setting('app.current_username', TRUE);
    
    -- Handle different operations
    IF (TG_OP = 'DELETE') THEN
        old_data := to_jsonb(OLD);
        new_data := NULL;
        
        INSERT INTO audit_log (
            table_name,
            record_id,
            operation,
            user_id,
            username,
            old_values,
            new_values,
            user_ip,
            session_id,
            application_name
        ) VALUES (
            TG_TABLE_NAME,
            OLD.id,
            TG_OP,
            audit_user_id,
            audit_username,
            old_data,
            new_data,
            inet_client_addr(),
            current_setting('app.session_id', TRUE),
            current_setting('application_name', TRUE)
        );
        
        RETURN OLD;
    ELSIF (TG_OP = 'UPDATE') THEN
        old_data := to_jsonb(OLD);
        new_data := to_jsonb(NEW);
        
        -- Calculate changed fields
        SELECT array_agg(key) INTO changed_fields
        FROM jsonb_each(old_data) o
        FULL OUTER JOIN jsonb_each(new_data) n USING (key)
        WHERE o.value IS DISTINCT FROM n.value;
        
        -- Only log if there are actual changes
        IF array_length(changed_fields, 1) > 0 THEN
            INSERT INTO audit_log (
                table_name,
                record_id,
                operation,
                user_id,
                username,
                old_values,
                new_values,
                changed_fields,
                user_ip,
                session_id,
                application_name
            ) VALUES (
                TG_TABLE_NAME,
                NEW.id,
                TG_OP,
                audit_user_id,
                audit_username,
                old_data,
                new_data,
                changed_fields,
                inet_client_addr(),
                current_setting('app.session_id', TRUE),
                current_setting('application_name', TRUE)
            );
        END IF;
        
        RETURN NEW;
    ELSIF (TG_OP = 'INSERT') THEN
        old_data := NULL;
        new_data := to_jsonb(NEW);
        
        INSERT INTO audit_log (
            table_name,
            record_id,
            operation,
            user_id,
            username,
            old_values,
            new_values,
            user_ip,
            session_id,
            application_name
        ) VALUES (
            TG_TABLE_NAME,
            NEW.id,
            TG_OP,
            audit_user_id,
            audit_username,
            old_data,
            new_data,
            inet_client_addr(),
            current_setting('app.session_id', TRUE),
            current_setting('application_name', TRUE)
        );
        
        RETURN NEW;
    ELSIF (TG_OP = 'TRUNCATE') THEN
        INSERT INTO audit_log (
            table_name,
            operation,
            user_id,
            username,
            user_ip,
            session_id,
            application_name
        ) VALUES (
            TG_TABLE_NAME,
            TG_OP,
            audit_user_id,
            audit_username,
            inet_client_addr(),
            current_setting('app.session_id', TRUE),
            current_setting('application_name', TRUE)
        );
        
        RETURN NULL;
    END IF;
    
    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- History trigger function for reports
CREATE OR REPLACE FUNCTION reports_history_trigger()
RETURNS TRIGGER AS $$
DECLARE
    next_version INTEGER;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        -- Get next version number
        SELECT COALESCE(MAX(version_number), 0) + 1 INTO next_version
        FROM reports_history
        WHERE id = NEW.id;
        
        -- Insert history record
        INSERT INTO reports_history (
            id, version_number,
            report_number, school_id, supervisor_id, assigned_to,
            title, description, category, sub_category,
            status, priority, reported_date, scheduled_date,
            completed_date, work_description, materials_used,
            labor_hours, estimated_cost, actual_cost,
            reviewed_by, reviewed_at, approved_by, approved_at,
            is_urgent, requires_shutdown,
            valid_from, valid_to, modified_by
        ) VALUES (
            OLD.id, next_version,
            OLD.report_number, OLD.school_id, OLD.supervisor_id, OLD.assigned_to,
            OLD.title, OLD.description, OLD.category, OLD.sub_category,
            OLD.status, OLD.priority, OLD.reported_date, OLD.scheduled_date,
            OLD.completed_date, OLD.work_description, OLD.materials_used,
            OLD.labor_hours, OLD.estimated_cost, OLD.actual_cost,
            OLD.reviewed_by, OLD.reviewed_at, OLD.approved_by, OLD.approved_at,
            OLD.is_urgent, OLD.requires_shutdown,
            OLD.updated_at, NEW.updated_at,
            current_setting('app.current_user_id', TRUE)::UUID
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- History trigger function for users
CREATE OR REPLACE FUNCTION users_history_trigger()
RETURNS TRIGGER AS $$
DECLARE
    next_version INTEGER;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        -- Skip if only last_login_at changed
        IF OLD.email = NEW.email 
           AND OLD.username = NEW.username
           AND OLD.first_name IS NOT DISTINCT FROM NEW.first_name
           AND OLD.last_name IS NOT DISTINCT FROM NEW.last_name
           AND OLD.phone IS NOT DISTINCT FROM NEW.phone
           AND OLD.user_type IS NOT DISTINCT FROM NEW.user_type
           AND OLD.status IS NOT DISTINCT FROM NEW.status
           AND OLD.is_active = NEW.is_active
           AND OLD.is_locked = NEW.is_locked THEN
            RETURN NEW;
        END IF;
        
        -- Get next version number
        SELECT COALESCE(MAX(version_number), 0) + 1 INTO next_version
        FROM users_history
        WHERE id = NEW.id;
        
        -- Insert history record (excluding sensitive data)
        INSERT INTO users_history (
            id, version_number,
            email, username, first_name, last_name, phone,
            employee_id, user_type, status, department, position,
            is_active, is_locked,
            valid_from, valid_to, modified_by
        ) VALUES (
            OLD.id, next_version,
            OLD.email, OLD.username, OLD.first_name, OLD.last_name, OLD.phone,
            OLD.employee_id, OLD.user_type, OLD.status, OLD.department, OLD.position,
            OLD.is_active, OLD.is_locked,
            OLD.updated_at, NEW.updated_at,
            current_setting('app.current_user_id', TRUE)::UUID
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 5: APPLY AUDIT TRIGGERS TO TABLES
-- ============================================

-- Apply audit triggers to critical tables
CREATE TRIGGER audit_trigger_users
    AFTER INSERT OR UPDATE OR DELETE ON users
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_trigger_reports
    AFTER INSERT OR UPDATE OR DELETE ON reports
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_trigger_schools
    AFTER INSERT OR UPDATE OR DELETE ON schools
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_trigger_supervisor_schools
    AFTER INSERT OR UPDATE OR DELETE ON supervisor_schools
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_trigger_maintenance_counts
    AFTER INSERT OR UPDATE OR DELETE ON maintenance_counts
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_trigger_damage_counts
    AFTER INSERT OR UPDATE OR DELETE ON damage_counts
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- Apply history triggers
CREATE TRIGGER history_trigger_reports
    AFTER UPDATE ON reports
    FOR EACH ROW EXECUTE FUNCTION reports_history_trigger();

CREATE TRIGGER history_trigger_users
    AFTER UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION users_history_trigger();

-- ============================================
-- STEP 6: CREATE AUDIT ACCESS FUNCTIONS
-- ============================================

-- Function to get audit trail for a specific record
CREATE OR REPLACE FUNCTION get_audit_trail(
    p_table_name VARCHAR(100),
    p_record_id UUID,
    p_limit INTEGER DEFAULT 100
)
RETURNS TABLE (
    operation VARCHAR(20),
    operation_timestamp TIMESTAMP WITH TIME ZONE,
    user_id UUID,
    username VARCHAR(100),
    changed_fields TEXT[],
    old_values JSONB,
    new_values JSONB
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        al.operation,
        al.operation_timestamp,
        al.user_id,
        al.username,
        al.changed_fields,
        al.old_values,
        al.new_values
    FROM audit_log al
    WHERE al.table_name = p_table_name
        AND al.record_id = p_record_id
    ORDER BY al.operation_timestamp DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- Function to get user activity
CREATE OR REPLACE FUNCTION get_user_activity(
    p_user_id UUID,
    p_start_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP - INTERVAL '30 days',
    p_end_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
)
RETURNS TABLE (
    table_name VARCHAR(100),
    operation VARCHAR(20),
    record_id UUID,
    operation_timestamp TIMESTAMP WITH TIME ZONE,
    changed_fields TEXT[]
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        al.table_name,
        al.operation,
        al.record_id,
        al.operation_timestamp,
        al.changed_fields
    FROM audit_log al
    WHERE al.user_id = p_user_id
        AND al.operation_timestamp BETWEEN p_start_date AND p_end_date
    ORDER BY al.operation_timestamp DESC;
END;
$$ LANGUAGE plpgsql;

-- Function to get data changes for a field
CREATE OR REPLACE FUNCTION get_field_changes(
    p_table_name VARCHAR(100),
    p_field_name TEXT,
    p_start_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP - INTERVAL '30 days'
)
RETURNS TABLE (
    record_id UUID,
    operation_timestamp TIMESTAMP WITH TIME ZONE,
    old_value TEXT,
    new_value TEXT,
    changed_by VARCHAR(100)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        al.record_id,
        al.operation_timestamp,
        (al.old_values->p_field_name)::TEXT as old_value,
        (al.new_values->p_field_name)::TEXT as new_value,
        al.username as changed_by
    FROM audit_log al
    WHERE al.table_name = p_table_name
        AND p_field_name = ANY(al.changed_fields)
        AND al.operation_timestamp >= p_start_date
    ORDER BY al.operation_timestamp DESC;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 7: CREATE AUDIT REPORTING VIEWS
-- ============================================

-- View for daily audit summary
CREATE OR REPLACE VIEW v_daily_audit_summary AS
SELECT 
    DATE(operation_timestamp) as audit_date,
    table_name,
    operation,
    COUNT(*) as operation_count,
    COUNT(DISTINCT user_id) as unique_users,
    COUNT(DISTINCT record_id) as affected_records
FROM audit_log
WHERE operation_timestamp >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE(operation_timestamp), table_name, operation
ORDER BY audit_date DESC, operation_count DESC;

-- View for user audit summary
CREATE OR REPLACE VIEW v_user_audit_summary AS
SELECT 
    u.id as user_id,
    u.username,
    u.full_name,
    u.user_type,
    COUNT(al.id) as total_operations,
    COUNT(DISTINCT DATE(al.operation_timestamp)) as active_days,
    MAX(al.operation_timestamp) as last_activity,
    COUNT(CASE WHEN al.operation = 'INSERT' THEN 1 END) as inserts,
    COUNT(CASE WHEN al.operation = 'UPDATE' THEN 1 END) as updates,
    COUNT(CASE WHEN al.operation = 'DELETE' THEN 1 END) as deletes
FROM users u
LEFT JOIN audit_log al ON u.id = al.user_id
    AND al.operation_timestamp >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY u.id, u.username, u.full_name, u.user_type;

-- View for sensitive operations
CREATE OR REPLACE VIEW v_sensitive_operations AS
SELECT 
    al.*
FROM audit_log al
WHERE 
    -- User permission changes
    (al.table_name = 'user_roles')
    -- User account changes
    OR (al.table_name = 'users' AND al.changed_fields && ARRAY['user_type', 'status', 'is_active', 'is_locked'])
    -- Report approvals
    OR (al.table_name = 'reports' AND al.changed_fields && ARRAY['approved_by', 'status'])
    -- School deactivations
    OR (al.table_name = 'schools' AND al.changed_fields && ARRAY['is_active'])
ORDER BY al.operation_timestamp DESC;

-- ============================================
-- STEP 8: CREATE AUDIT RETENTION POLICY
-- ============================================

-- Function to archive old audit logs
CREATE OR REPLACE FUNCTION archive_old_audit_logs(
    p_days_to_keep INTEGER DEFAULT 90
)
RETURNS TABLE (
    archived_count BIGINT,
    deleted_count BIGINT
) AS $$
DECLARE
    archive_before_date TIMESTAMP WITH TIME ZONE;
    archived BIGINT;
    deleted BIGINT;
BEGIN
    archive_before_date := CURRENT_TIMESTAMP - (p_days_to_keep || ' days')::INTERVAL;
    
    -- Create archive table if not exists
    CREATE TABLE IF NOT EXISTS audit_log_archive (LIKE audit_log INCLUDING ALL);
    
    -- Archive old records
    WITH archived_rows AS (
        INSERT INTO audit_log_archive
        SELECT * FROM audit_log
        WHERE operation_timestamp < archive_before_date
        RETURNING 1
    )
    SELECT COUNT(*) INTO archived FROM archived_rows;
    
    -- Delete archived records from main table
    WITH deleted_rows AS (
        DELETE FROM audit_log
        WHERE operation_timestamp < archive_before_date
        RETURNING 1
    )
    SELECT COUNT(*) INTO deleted FROM deleted_rows;
    
    RETURN QUERY SELECT archived, deleted;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 9: CREATE COMPLIANCE REPORTING
-- ============================================

-- Table for compliance requirements
CREATE TABLE IF NOT EXISTS compliance_requirements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    requirement_name VARCHAR(255) NOT NULL,
    description TEXT,
    regulation VARCHAR(100), -- GDPR, HIPAA, SOX, etc.
    data_retention_days INTEGER,
    audit_frequency VARCHAR(50), -- daily, weekly, monthly
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table for compliance reports
CREATE TABLE IF NOT EXISTS compliance_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    requirement_id UUID REFERENCES compliance_requirements(id),
    report_date DATE NOT NULL,
    compliant BOOLEAN NOT NULL,
    findings TEXT,
    evidence JSONB,
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Function to generate compliance report
CREATE OR REPLACE FUNCTION generate_compliance_report(
    p_start_date DATE,
    p_end_date DATE
)
RETURNS TABLE (
    requirement_name VARCHAR(255),
    total_operations BIGINT,
    unauthorized_operations BIGINT,
    data_breaches BIGINT,
    compliance_score NUMERIC(5,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        cr.requirement_name,
        COUNT(al.id) as total_operations,
        COUNT(CASE WHEN al.user_id IS NULL THEN 1 END) as unauthorized_operations,
        0::BIGINT as data_breaches, -- Would need actual breach detection logic
        CASE 
            WHEN COUNT(al.id) > 0 THEN
                (100.0 - (COUNT(CASE WHEN al.user_id IS NULL THEN 1 END)::NUMERIC / COUNT(al.id) * 100))
            ELSE 100.0
        END as compliance_score
    FROM compliance_requirements cr
    LEFT JOIN audit_log al ON al.operation_timestamp BETWEEN p_start_date AND p_end_date
    WHERE cr.is_active = TRUE
    GROUP BY cr.requirement_name;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 10: CREATE AUDIT DASHBOARD FUNCTIONS
-- ============================================

-- Function to get audit statistics
CREATE OR REPLACE FUNCTION get_audit_statistics(
    p_days INTEGER DEFAULT 30
)
RETURNS TABLE (
    metric_name VARCHAR(100),
    metric_value BIGINT
) AS $$
BEGIN
    RETURN QUERY
    -- Total operations
    SELECT 'total_operations'::VARCHAR(100), COUNT(*)::BIGINT
    FROM audit_log
    WHERE operation_timestamp >= CURRENT_TIMESTAMP - (p_days || ' days')::INTERVAL
    
    UNION ALL
    
    -- Unique users
    SELECT 'unique_users'::VARCHAR(100), COUNT(DISTINCT user_id)::BIGINT
    FROM audit_log
    WHERE operation_timestamp >= CURRENT_TIMESTAMP - (p_days || ' days')::INTERVAL
    
    UNION ALL
    
    -- Data modifications
    SELECT 'data_modifications'::VARCHAR(100), COUNT(*)::BIGINT
    FROM audit_log
    WHERE operation IN ('INSERT', 'UPDATE', 'DELETE')
        AND operation_timestamp >= CURRENT_TIMESTAMP - (p_days || ' days')::INTERVAL
    
    UNION ALL
    
    -- Failed operations (would need error tracking)
    SELECT 'failed_operations'::VARCHAR(100), 0::BIGINT
    
    UNION ALL
    
    -- Sensitive operations
    SELECT 'sensitive_operations'::VARCHAR(100), COUNT(*)::BIGINT
    FROM v_sensitive_operations
    WHERE operation_timestamp >= CURRENT_TIMESTAMP - (p_days || ' days')::INTERVAL;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 11: SET UP SCHEDULED MAINTENANCE
-- ============================================

-- Function to perform audit maintenance
CREATE OR REPLACE FUNCTION perform_audit_maintenance()
RETURNS VOID AS $$
DECLARE
    stats_record RECORD;
BEGIN
    -- Archive old logs
    SELECT * INTO stats_record FROM archive_old_audit_logs(90);
    
    -- Log maintenance operation
    INSERT INTO system_logs (level, message, context, created_at)
    VALUES (
        'INFO',
        'Audit maintenance completed',
        jsonb_build_object(
            'archived_count', stats_record.archived_count,
            'deleted_count', stats_record.deleted_count
        ),
        CURRENT_TIMESTAMP
    );
    
    -- Update statistics
    ANALYZE audit_log;
    ANALYZE audit_log_archive;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 12: COMMENTS
-- ============================================

COMMENT ON TABLE audit_log IS 'Comprehensive audit trail for all database operations';
COMMENT ON TABLE reports_history IS 'Historical versions of report records';
COMMENT ON TABLE users_history IS 'Historical versions of user records';
COMMENT ON TABLE schools_history IS 'Historical versions of school records';
COMMENT ON TABLE compliance_requirements IS 'Compliance and regulatory requirements';
COMMENT ON TABLE compliance_reports IS 'Compliance audit reports';

COMMENT ON FUNCTION audit_trigger_function IS 'Generic trigger function for audit logging';
COMMENT ON FUNCTION get_audit_trail IS 'Retrieves audit trail for a specific record';
COMMENT ON FUNCTION get_user_activity IS 'Retrieves activity log for a specific user';
COMMENT ON FUNCTION archive_old_audit_logs IS 'Archives old audit logs based on retention policy';
COMMENT ON FUNCTION generate_compliance_report IS 'Generates compliance report for specified period';

COMMENT ON VIEW v_daily_audit_summary IS 'Daily summary of audit operations';
COMMENT ON VIEW v_user_audit_summary IS 'User activity summary from audit logs';
COMMENT ON VIEW v_sensitive_operations IS 'View of sensitive operations requiring special attention';

-- End of V7 Comprehensive Audit System migration