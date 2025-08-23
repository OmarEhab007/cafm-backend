-- ============================================
-- V5__Performance_Optimizations.sql
-- Database Optimization Phase 1: Performance Optimizations
-- Adds partitioning, advanced indexes, and materialized views
-- ============================================

-- ============================================
-- STEP 1: CREATE PARTITIONED TABLES
-- ============================================

-- Create new partitioned reports table (without constraints that would conflict)
CREATE TABLE reports_partitioned (
    LIKE reports INCLUDING DEFAULTS INCLUDING STORAGE
) PARTITION BY RANGE (created_at);

-- Add the primary key with partition key included
ALTER TABLE reports_partitioned ADD PRIMARY KEY (id, created_at);

-- Create partitions for reports (monthly partitions for 2 years)
CREATE TABLE reports_2024_01 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
CREATE TABLE reports_2024_02 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
CREATE TABLE reports_2024_03 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');
CREATE TABLE reports_2024_04 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');
CREATE TABLE reports_2024_05 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');
CREATE TABLE reports_2024_06 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2024-06-01') TO ('2024-07-01');
CREATE TABLE reports_2024_07 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2024-07-01') TO ('2024-08-01');
CREATE TABLE reports_2024_08 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2024-08-01') TO ('2024-09-01');
CREATE TABLE reports_2024_09 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2024-09-01') TO ('2024-10-01');
CREATE TABLE reports_2024_10 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');
CREATE TABLE reports_2024_11 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');
CREATE TABLE reports_2024_12 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

-- Create 2025 partitions
CREATE TABLE reports_2025_01 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
CREATE TABLE reports_2025_02 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
CREATE TABLE reports_2025_03 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');
CREATE TABLE reports_2025_04 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');
CREATE TABLE reports_2025_05 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');
CREATE TABLE reports_2025_06 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');
CREATE TABLE reports_2025_07 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');
CREATE TABLE reports_2025_08 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');
CREATE TABLE reports_2025_09 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');
CREATE TABLE reports_2025_10 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');
CREATE TABLE reports_2025_11 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');
CREATE TABLE reports_2025_12 PARTITION OF reports_partitioned
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

-- Create default partition for future dates
CREATE TABLE reports_default PARTITION OF reports_partitioned DEFAULT;

-- Create new partitioned notifications table (weekly partitions)
CREATE TABLE notifications_partitioned (
    LIKE notifications INCLUDING DEFAULTS INCLUDING STORAGE
) PARTITION BY RANGE (created_at);

-- Add the primary key with partition key included
ALTER TABLE notifications_partitioned ADD PRIMARY KEY (id, created_at);

-- Function to create weekly partitions
CREATE OR REPLACE FUNCTION create_weekly_partition(
    table_name TEXT,
    start_date DATE,
    end_date DATE
) RETURNS VOID AS $$
DECLARE
    partition_name TEXT;
    partition_start TEXT;
    partition_end TEXT;
BEGIN
    partition_name := table_name || '_' || to_char(start_date, 'YYYY_MM_DD');
    partition_start := start_date::TEXT;
    partition_end := end_date::TEXT;
    
    EXECUTE format('CREATE TABLE %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
        partition_name, table_name, partition_start, partition_end);
END;
$$ LANGUAGE plpgsql;

-- Create notification partitions for current year (weekly)
DO $$
DECLARE
    start_date DATE := '2025-01-01';
    end_date DATE;
BEGIN
    WHILE start_date < '2026-01-01' LOOP
        end_date := start_date + INTERVAL '1 week';
        PERFORM create_weekly_partition('notifications_partitioned', start_date, end_date);
        start_date := end_date;
    END LOOP;
END $$;

-- Create default partition for notifications
CREATE TABLE notifications_default PARTITION OF notifications_partitioned DEFAULT;

-- ============================================
-- STEP 2: ADVANCED COMPOSITE INDEXES
-- ============================================

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_reports_school_status_date 
    ON reports(school_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_reports_supervisor_priority_status 
    ON reports(supervisor_id, priority, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_reports_scheduled_date_status 
    ON reports(scheduled_date, status);

-- Partial indexes for specific statuses
CREATE INDEX IF NOT EXISTS idx_reports_pending 
    ON reports(school_id, supervisor_id, created_at DESC)
    WHERE status IN ('DRAFT', 'SUBMITTED', 'pending');

CREATE INDEX IF NOT EXISTS idx_reports_overdue 
    ON reports(school_id, scheduled_date, priority, status);

-- School-related composite indexes
CREATE INDEX IF NOT EXISTS idx_schools_active_name 
    ON schools(is_active, name)
    WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_supervisor_schools_active 
    ON supervisor_schools(supervisor_id, is_active, assigned_at DESC)
    WHERE is_active = TRUE;

-- User session and authentication indexes
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_active 
    ON refresh_tokens(user_id, expires_at DESC)
    WHERE revoked = FALSE;

CREATE INDEX IF NOT EXISTS idx_user_fcm_tokens_active 
    ON user_fcm_tokens(user_id, is_active)
    WHERE is_active = TRUE;

-- Maintenance and damage indexes
CREATE INDEX IF NOT EXISTS idx_maintenance_counts_school_date 
    ON maintenance_counts(school_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_damage_counts_school_date 
    ON damage_counts(school_id, created_at DESC);

-- Attachment indexes for file management (check if deleted_at exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'attachments' AND column_name = 'deleted_at') THEN
        CREATE INDEX IF NOT EXISTS idx_attachments_entity_type_size 
            ON attachments(entity_type, entity_id, file_size DESC)
            WHERE deleted_at IS NULL;
    ELSE
        CREATE INDEX IF NOT EXISTS idx_attachments_entity_type_size 
            ON attachments(entity_type, entity_id, file_size DESC);
    END IF;
END $$;

-- ============================================
-- STEP 3: MATERIALIZED VIEWS FOR DASHBOARD
-- ============================================

-- Dashboard statistics materialized view
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_dashboard_stats AS
SELECT 
    'total_schools' as metric,
    COUNT(*)::TEXT as value,
    CURRENT_TIMESTAMP as last_updated
FROM schools 
WHERE is_active = TRUE

UNION ALL

SELECT 
    'total_reports' as metric,
    COUNT(*)::TEXT as value,
    CURRENT_TIMESTAMP as last_updated
FROM reports 
WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'

UNION ALL

SELECT 
    'pending_reports' as metric,
    COUNT(*)::TEXT as value,
    CURRENT_TIMESTAMP as last_updated
FROM reports 
WHERE status IN ('DRAFT', 'SUBMITTED', 'pending', 'IN_REVIEW')

UNION ALL

SELECT 
    'overdue_reports' as metric,
    COUNT(*)::TEXT as value,
    CURRENT_TIMESTAMP as last_updated
FROM reports 
WHERE status NOT IN ('COMPLETED', 'completed', 'CANCELLED', 'cancelled')
    AND scheduled_date < CURRENT_DATE

UNION ALL

SELECT 
    'active_supervisors' as metric,
    COUNT(*)::TEXT as value,
    CURRENT_TIMESTAMP as last_updated
FROM users 
WHERE user_type = 'supervisor' 
    AND status = 'active'
    AND is_active = TRUE;

-- Create unique index for dashboard stats
CREATE UNIQUE INDEX idx_mv_dashboard_stats_metric 
    ON mv_dashboard_stats(metric);

-- School performance materialized view
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_school_performance AS
SELECT 
    s.id as school_id,
    s.name as school_name,
    COUNT(DISTINCT r.id) as total_reports,
    COUNT(DISTINCT CASE WHEN r.status IN ('COMPLETED', 'completed') THEN r.id END) as completed_reports,
    COUNT(DISTINCT CASE WHEN r.status IN ('DRAFT', 'SUBMITTED', 'pending') THEN r.id END) as pending_reports,
    COUNT(DISTINCT CASE 
        WHEN r.status NOT IN ('COMPLETED', 'completed', 'CANCELLED', 'cancelled') 
        AND r.scheduled_date < CURRENT_DATE 
        THEN r.id 
    END) as overdue_reports,
    ROUND(
        CASE 
            WHEN COUNT(DISTINCT r.id) > 0 
            THEN (COUNT(DISTINCT CASE WHEN r.status IN ('COMPLETED', 'completed') THEN r.id END)::NUMERIC / COUNT(DISTINCT r.id)) * 100
            ELSE 0 
        END, 2
    ) as completion_percentage,
    MAX(r.created_at) as last_report_date,
    COUNT(DISTINCT r.supervisor_id) as active_supervisors,
    CURRENT_TIMESTAMP as last_updated
FROM schools s
LEFT JOIN reports r ON s.id = r.school_id 
    AND r.created_at >= CURRENT_DATE - INTERVAL '90 days'
WHERE s.is_active = TRUE
GROUP BY s.id, s.name;

-- Create unique index for school performance
CREATE UNIQUE INDEX idx_mv_school_performance_school_id 
    ON mv_school_performance(school_id);

-- Supervisor workload materialized view
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_supervisor_workload AS
SELECT 
    u.id as supervisor_id,
    u.full_name as supervisor_name,
    u.email,
    COUNT(DISTINCT ss.school_id) as assigned_schools,
    COUNT(DISTINCT r.id) as total_reports_30d,
    COUNT(DISTINCT CASE WHEN r.status IN ('COMPLETED', 'completed') THEN r.id END) as completed_reports_30d,
    COUNT(DISTINCT CASE 
        WHEN r.status NOT IN ('COMPLETED', 'completed', 'CANCELLED', 'cancelled') 
        AND r.scheduled_date < CURRENT_DATE 
        THEN r.id 
    END) as overdue_reports,
    COUNT(DISTINCT t.id) as total_technicians,
    ROUND(
        CASE 
            WHEN COUNT(DISTINCT r.id) > 0 
            THEN (COUNT(DISTINCT CASE WHEN r.status IN ('COMPLETED', 'completed') THEN r.id END)::NUMERIC / COUNT(DISTINCT r.id)) * 100
            ELSE 0 
        END, 2
    ) as completion_rate,
    MAX(r.created_at) as last_activity_date,
    CURRENT_TIMESTAMP as last_updated
FROM users u
LEFT JOIN supervisor_schools ss ON u.id = ss.supervisor_id AND ss.is_active = TRUE
LEFT JOIN reports r ON u.id = r.supervisor_id 
    AND r.created_at >= CURRENT_DATE - INTERVAL '30 days'
LEFT JOIN technicians t ON u.id = t.supervisor_id AND t.is_active = TRUE
WHERE u.user_type = 'supervisor' 
    AND u.status = 'active' 
    AND u.is_active = TRUE
GROUP BY u.id, u.full_name, u.email;

-- Create unique index for supervisor workload
CREATE UNIQUE INDEX idx_mv_supervisor_workload_supervisor_id 
    ON mv_supervisor_workload(supervisor_id);

-- ============================================
-- STEP 4: CACHING TABLES FOR FREQUENTLY ACCESSED DATA
-- ============================================

-- Cache table for user sessions
CREATE TABLE IF NOT EXISTS user_session_cache (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    session_data JSONB NOT NULL,
    last_activity TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create index for session cleanup
CREATE INDEX idx_user_session_cache_expires 
    ON user_session_cache(expires_at);

-- Cache table for school assignments
CREATE TABLE IF NOT EXISTS supervisor_school_cache (
    supervisor_id UUID REFERENCES users(id) ON DELETE CASCADE,
    school_ids UUID[] NOT NULL,
    school_names TEXT[] NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (supervisor_id)
);

-- Cache table for report statistics
CREATE TABLE IF NOT EXISTS report_stats_cache (
    cache_key VARCHAR(100) PRIMARY KEY,
    stats_data JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Create index for cache cleanup
CREATE INDEX idx_report_stats_cache_expires 
    ON report_stats_cache(expires_at);

-- ============================================
-- STEP 5: FULL-TEXT SEARCH CAPABILITIES
-- ============================================

-- Add full-text search columns
ALTER TABLE reports ADD COLUMN IF NOT EXISTS search_vector tsvector;
ALTER TABLE schools ADD COLUMN IF NOT EXISTS search_vector tsvector;
ALTER TABLE users ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Create function to update search vectors
CREATE OR REPLACE FUNCTION update_search_vector() 
RETURNS TRIGGER AS $$
BEGIN
    -- Update reports search vector
    IF TG_TABLE_NAME = 'reports' THEN
        NEW.search_vector := 
            setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
            setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B') ||
            setweight(to_tsvector('english', COALESCE(NEW.school_name, '')), 'C');
    END IF;
    
    -- Update schools search vector
    IF TG_TABLE_NAME = 'schools' THEN
        NEW.search_vector := 
            setweight(to_tsvector('english', COALESCE(NEW.name, '')), 'A') ||
            setweight(to_tsvector('english', COALESCE(NEW.address, '')), 'B') ||
            setweight(to_tsvector('english', COALESCE(NEW.city, '')), 'C');
    END IF;
    
    -- Update users search vector
    IF TG_TABLE_NAME = 'users' THEN
        NEW.search_vector := 
            setweight(to_tsvector('english', COALESCE(NEW.full_name, '')), 'A') ||
            setweight(to_tsvector('english', COALESCE(NEW.email, '')), 'B') ||
            setweight(to_tsvector('english', COALESCE(NEW.username, '')), 'B');
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for search vector updates
CREATE TRIGGER update_reports_search_vector
    BEFORE INSERT OR UPDATE ON reports
    FOR EACH ROW EXECUTE FUNCTION update_search_vector();

CREATE TRIGGER update_schools_search_vector
    BEFORE INSERT OR UPDATE ON schools
    FOR EACH ROW EXECUTE FUNCTION update_search_vector();

CREATE TRIGGER update_users_search_vector
    BEFORE INSERT OR UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_search_vector();

-- Create GIN indexes for full-text search
CREATE INDEX IF NOT EXISTS idx_reports_search 
    ON reports USING GIN (search_vector);
    
CREATE INDEX IF NOT EXISTS idx_schools_search 
    ON schools USING GIN (search_vector);
    
CREATE INDEX IF NOT EXISTS idx_users_search 
    ON users USING GIN (search_vector);

-- ============================================
-- STEP 6: PERFORMANCE MONITORING FUNCTIONS
-- ============================================

-- Function to get slow queries
CREATE OR REPLACE FUNCTION get_slow_queries()
RETURNS TABLE (
    query TEXT,
    calls BIGINT,
    total_time DOUBLE PRECISION,
    mean_time DOUBLE PRECISION,
    max_time DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        pg_stat_statements.query,
        pg_stat_statements.calls,
        pg_stat_statements.total_exec_time,
        pg_stat_statements.mean_exec_time,
        pg_stat_statements.max_exec_time
    FROM pg_stat_statements
    WHERE pg_stat_statements.mean_exec_time > 100  -- queries slower than 100ms
    ORDER BY pg_stat_statements.mean_exec_time DESC
    LIMIT 20;
END;
$$ LANGUAGE plpgsql;

-- Function to get table sizes
CREATE OR REPLACE FUNCTION get_table_sizes()
RETURNS TABLE (
    table_name TEXT,
    size_pretty TEXT,
    size_bytes BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        schemaname||'.'||tablename as table_name,
        pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size_pretty,
        pg_total_relation_size(schemaname||'.'||tablename) as size_bytes
    FROM pg_tables 
    WHERE schemaname = 'public'
    ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 7: AUTOMATED MAINTENANCE PROCEDURES
-- ============================================

-- Function to refresh all materialized views
CREATE OR REPLACE FUNCTION refresh_all_materialized_views()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_dashboard_stats;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_school_performance;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_supervisor_workload;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_school_maintenance_summary;
    
    -- Log the refresh
    INSERT INTO system_logs (level, message, created_at)
    VALUES ('INFO', 'All materialized views refreshed', CURRENT_TIMESTAMP);
    
EXCEPTION WHEN OTHERS THEN
    -- Log the error but don't fail
    INSERT INTO system_logs (level, message, created_at)
    VALUES ('ERROR', 'Failed to refresh materialized views: ' || SQLERRM, CURRENT_TIMESTAMP);
END;
$$ LANGUAGE plpgsql;

-- Function to clean expired cache entries
CREATE OR REPLACE FUNCTION clean_expired_cache()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER := 0;
    temp_count INTEGER := 0;
BEGIN
    -- Clean user session cache
    DELETE FROM user_session_cache WHERE expires_at < CURRENT_TIMESTAMP;
    GET DIAGNOSTICS temp_count = ROW_COUNT;
    deleted_count := deleted_count + temp_count;
    
    -- Clean report stats cache
    DELETE FROM report_stats_cache WHERE expires_at < CURRENT_TIMESTAMP;
    GET DIAGNOSTICS temp_count = ROW_COUNT;
    deleted_count := deleted_count + temp_count;
    
    -- Clean old notifications (older than 6 months)
    DELETE FROM notifications WHERE created_at < CURRENT_DATE - INTERVAL '6 months';
    
    -- Clean revoked refresh tokens (older than 30 days)
    DELETE FROM refresh_tokens 
    WHERE revoked = TRUE AND revoked_at < CURRENT_DATE - INTERVAL '30 days';
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to create new partitions automatically
CREATE OR REPLACE FUNCTION create_future_partitions()
RETURNS VOID AS $$
DECLARE
    next_month DATE;
    partition_name TEXT;
BEGIN
    -- Create next month's reports partition
    next_month := date_trunc('month', CURRENT_DATE + INTERVAL '2 months');
    partition_name := 'reports_' || to_char(next_month, 'YYYY_MM');
    
    BEGIN
        EXECUTE format('CREATE TABLE %I PARTITION OF reports_partitioned FOR VALUES FROM (%L) TO (%L)',
            partition_name, 
            next_month::TEXT, 
            (next_month + INTERVAL '1 month')::TEXT
        );
    EXCEPTION WHEN duplicate_table THEN
        -- Partition already exists, skip
        NULL;
    END;
    
    -- Log the creation
    INSERT INTO system_logs (level, message, created_at)
    VALUES ('INFO', 'Future partition created: ' || partition_name, CURRENT_TIMESTAMP);
    
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 8: SYSTEM LOGS TABLE
-- ============================================

-- Create system logs table for monitoring
CREATE TABLE IF NOT EXISTS system_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    level VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    context JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create index for system logs
CREATE INDEX idx_system_logs_level_date 
    ON system_logs(level, created_at DESC);

-- ============================================
-- STEP 9: INITIAL DATA MIGRATION TO PARTITIONS
-- ============================================

-- Note: In production, this should be done carefully during maintenance window
-- For now, we'll just create the infrastructure

-- Update search vectors for existing data
UPDATE reports SET search_vector = 
    setweight(to_tsvector('english', COALESCE(title, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(description, '')), 'B') ||
    setweight(to_tsvector('english', COALESCE(school_name, '')), 'C')
WHERE search_vector IS NULL;

UPDATE schools SET search_vector = 
    setweight(to_tsvector('english', COALESCE(name, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(address, '')), 'B') ||
    setweight(to_tsvector('english', COALESCE(city, '')), 'C')
WHERE search_vector IS NULL;

UPDATE users SET search_vector = 
    setweight(to_tsvector('english', COALESCE(full_name, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(email, '')), 'B') ||
    setweight(to_tsvector('english', COALESCE(username, '')), 'B')
WHERE search_vector IS NULL;

-- ============================================
-- STEP 10: COMMENTS
-- ============================================

COMMENT ON TABLE reports_partitioned IS 'Partitioned reports table for better performance on large datasets';
COMMENT ON TABLE notifications_partitioned IS 'Partitioned notifications table with weekly partitions';
COMMENT ON MATERIALIZED VIEW mv_dashboard_stats IS 'Cached dashboard statistics for fast loading';
COMMENT ON MATERIALIZED VIEW mv_school_performance IS 'Cached school performance metrics';
COMMENT ON MATERIALIZED VIEW mv_supervisor_workload IS 'Cached supervisor workload statistics';
COMMENT ON TABLE user_session_cache IS 'Cache table for user session data';
COMMENT ON TABLE system_logs IS 'System operation and error logs';

-- End of V5 Performance Optimizations migration