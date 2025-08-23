-- ============================================
-- V9__Advanced_Features.sql
-- Database Optimization Phase 5: Advanced Features
-- Implements advanced search, analytics, and computed columns
-- ============================================

-- ============================================
-- STEP 1: ENHANCED FULL-TEXT SEARCH
-- ============================================

-- Create custom text search configuration for Arabic support (if doesn't exist)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_ts_config WHERE cfgname = 'arabic_english'
    ) THEN
        CREATE TEXT SEARCH CONFIGURATION arabic_english (COPY = english);
    END IF;
END $$;

-- Add trigram extension for fuzzy search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Enhanced search indexes with trigram support
CREATE INDEX IF NOT EXISTS idx_reports_title_trgm ON reports USING gin (title gin_trgm_ops) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_reports_description_trgm ON reports USING gin (description gin_trgm_ops) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_schools_name_trgm ON schools USING gin (name gin_trgm_ops) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_full_name_trgm ON users USING gin (full_name gin_trgm_ops) WHERE deleted_at IS NULL;

-- Global search function
CREATE OR REPLACE FUNCTION global_search(
    p_search_term TEXT,
    p_limit INTEGER DEFAULT 50
)
RETURNS TABLE (
    table_name TEXT,
    record_id UUID,
    title TEXT,
    description TEXT,
    relevance REAL,
    created_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    WITH search_results AS (
        -- Search reports
        SELECT 
            'reports'::TEXT as table_name,
            r.id as record_id,
            r.title::TEXT,
            r.description::TEXT,
            similarity(r.title, p_search_term) + 
            similarity(r.description, p_search_term) as relevance,
            r.created_at
        FROM reports r
        WHERE r.deleted_at IS NULL
            AND (
                r.title ILIKE '%' || p_search_term || '%'
                OR r.description ILIKE '%' || p_search_term || '%'
                OR r.search_vector @@ plainto_tsquery('english', p_search_term)
            )
        
        UNION ALL
        
        -- Search schools
        SELECT 
            'schools'::TEXT,
            s.id,
            s.name::TEXT,
            s.address::TEXT,
            similarity(s.name, p_search_term) as relevance,
            s.created_at
        FROM schools s
        WHERE s.deleted_at IS NULL
            AND (
                s.name ILIKE '%' || p_search_term || '%'
                OR s.code ILIKE '%' || p_search_term || '%'
                OR s.search_vector @@ plainto_tsquery('english', p_search_term)
            )
        
        UNION ALL
        
        -- Search users
        SELECT 
            'users'::TEXT,
            u.id,
            u.full_name::TEXT,
            u.email::TEXT,
            similarity(u.full_name, p_search_term) as relevance,
            u.created_at
        FROM users u
        WHERE u.deleted_at IS NULL
            AND (
                u.full_name ILIKE '%' || p_search_term || '%'
                OR u.email ILIKE '%' || p_search_term || '%'
                OR u.username ILIKE '%' || p_search_term || '%'
                OR u.search_vector @@ plainto_tsquery('english', p_search_term)
            )
    )
    SELECT * FROM search_results
    ORDER BY relevance DESC, created_at DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 2: ADVANCED ANALYTICS TABLES
-- ============================================

-- Create analytics summary table
CREATE TABLE IF NOT EXISTS analytics_summary (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    metric_date DATE NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    dimension_1 VARCHAR(100), -- e.g., school_id, user_id
    dimension_2 VARCHAR(100), -- e.g., status, category
    metric_value NUMERIC,
    metric_count INTEGER,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_analytics_metric UNIQUE (metric_date, metric_type, dimension_1, dimension_2)
);

CREATE INDEX idx_analytics_summary_date ON analytics_summary(metric_date DESC);
CREATE INDEX idx_analytics_summary_type ON analytics_summary(metric_type);
CREATE INDEX idx_analytics_summary_dimensions ON analytics_summary(dimension_1, dimension_2);

-- Time series data for trending
CREATE TABLE IF NOT EXISTS time_series_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    series_name VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    value NUMERIC NOT NULL,
    tags JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_time_series_name_time ON time_series_data(series_name, timestamp DESC);
CREATE INDEX idx_time_series_tags ON time_series_data USING gin (tags);

-- ============================================
-- STEP 3: COMPUTED COLUMNS AND METRICS
-- ============================================

-- Add computed columns for analytics
ALTER TABLE reports 
    ADD COLUMN IF NOT EXISTS resolution_time_hours INTEGER,
    ADD COLUMN IF NOT EXISTS efficiency_score NUMERIC(5,2);

ALTER TABLE schools
    ADD COLUMN IF NOT EXISTS maintenance_score NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS activity_level VARCHAR(20);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS performance_rating NUMERIC(3,2),
    ADD COLUMN IF NOT EXISTS productivity_score INTEGER;

-- Function to calculate report metrics
CREATE OR REPLACE FUNCTION calculate_report_metrics()
RETURNS TRIGGER AS $$
BEGIN
    -- Calculate resolution time
    IF NEW.completed_date IS NOT NULL AND NEW.reported_date IS NOT NULL THEN
        NEW.resolution_time_hours := 
            EXTRACT(EPOCH FROM (NEW.completed_date - NEW.reported_date)) / 3600;
    END IF;
    
    -- Calculate efficiency score (0-100)
    IF NEW.scheduled_date IS NOT NULL AND NEW.completed_date IS NOT NULL THEN
        IF NEW.completed_date <= NEW.scheduled_date THEN
            NEW.efficiency_score := 100;
        ELSE
            -- Decrease score based on days late
            NEW.efficiency_score := GREATEST(
                0, 
                100 - (NEW.completed_date - NEW.scheduled_date) * 10
            );
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER calculate_report_metrics_trigger
    BEFORE INSERT OR UPDATE ON reports
    FOR EACH ROW EXECUTE FUNCTION calculate_report_metrics();

-- ============================================
-- STEP 4: RANKING AND SCORING SYSTEM
-- ============================================

-- Function to calculate school maintenance score
CREATE OR REPLACE FUNCTION calculate_school_scores()
RETURNS VOID AS $$
BEGIN
    UPDATE schools s
    SET 
        maintenance_score = subquery.score,
        activity_level = CASE
            WHEN subquery.recent_reports >= 10 THEN 'very_active'
            WHEN subquery.recent_reports >= 5 THEN 'active'
            WHEN subquery.recent_reports >= 1 THEN 'moderate'
            ELSE 'inactive'
        END
    FROM (
        SELECT 
            school_id,
            COUNT(*) FILTER (WHERE created_at > CURRENT_DATE - INTERVAL '30 days') as recent_reports,
            AVG(efficiency_score) as avg_efficiency,
            (
                COUNT(*) FILTER (WHERE status IN ('COMPLETED', 'completed'))::NUMERIC / 
                NULLIF(COUNT(*), 0) * 100
            ) as completion_rate,
            -- Composite score calculation
            (
                COALESCE(AVG(efficiency_score), 50) * 0.4 +
                (COUNT(*) FILTER (WHERE status IN ('COMPLETED', 'completed'))::NUMERIC / 
                 NULLIF(COUNT(*), 0) * 100) * 0.6
            ) as score
        FROM reports
        WHERE deleted_at IS NULL
            AND created_at > CURRENT_DATE - INTERVAL '90 days'
        GROUP BY school_id
    ) as subquery
    WHERE s.id = subquery.school_id;
END;
$$ LANGUAGE plpgsql;

-- Function to calculate user performance
CREATE OR REPLACE FUNCTION calculate_user_performance()
RETURNS VOID AS $$
BEGIN
    UPDATE users u
    SET 
        performance_rating = subquery.rating,
        productivity_score = subquery.productivity
    FROM (
        SELECT 
            supervisor_id as user_id,
            COUNT(*) as total_reports,
            AVG(efficiency_score) as avg_efficiency,
            COUNT(*) FILTER (WHERE completed_date IS NOT NULL) as completed_reports,
            -- Performance rating (1-5 scale)
            LEAST(5.0, 
                (AVG(efficiency_score) / 20.0) * 0.5 + 
                (COUNT(*) FILTER (WHERE completed_date IS NOT NULL)::NUMERIC / 
                 GREATEST(COUNT(*), 1) * 5) * 0.5
            ) as rating,
            -- Productivity score
            COUNT(*) FILTER (WHERE created_at > CURRENT_DATE - INTERVAL '30 days') as productivity
        FROM reports
        WHERE deleted_at IS NULL
            AND created_at > CURRENT_DATE - INTERVAL '90 days'
        GROUP BY supervisor_id
    ) as subquery
    WHERE u.id = subquery.user_id;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 5: PREDICTIVE ANALYTICS FUNCTIONS
-- ============================================

-- Function to predict maintenance needs
CREATE OR REPLACE FUNCTION predict_maintenance_needs(
    p_school_id UUID,
    p_days_ahead INTEGER DEFAULT 30
)
RETURNS TABLE (
    predicted_date DATE,
    category VARCHAR(100),
    probability NUMERIC(5,2),
    estimated_items INTEGER
) AS $$
BEGIN
    RETURN QUERY
    WITH historical_patterns AS (
        SELECT 
            category,
            AVG(EXTRACT(DOY FROM created_at)) as avg_day_of_year,
            STDDEV(EXTRACT(DOY FROM created_at)) as stddev_day,
            COUNT(*) as occurrence_count,
            AVG(labor_hours) as avg_labor_hours
        FROM reports
        WHERE school_id = p_school_id
            AND deleted_at IS NULL
            AND created_at > CURRENT_DATE - INTERVAL '2 years'
        GROUP BY category
        HAVING COUNT(*) >= 3
    )
    SELECT 
        (CURRENT_DATE + (avg_day_of_year - EXTRACT(DOY FROM CURRENT_DATE))::INTEGER)::DATE as predicted_date,
        category,
        LEAST(95, 50 + (occurrence_count * 5))::NUMERIC(5,2) as probability,
        CEIL(occurrence_count / 2.0)::INTEGER as estimated_items
    FROM historical_patterns
    WHERE avg_day_of_year BETWEEN EXTRACT(DOY FROM CURRENT_DATE) 
        AND EXTRACT(DOY FROM CURRENT_DATE + p_days_ahead::INTEGER);
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 6: SMART NOTIFICATIONS SYSTEM
-- ============================================

-- Table for notification rules
CREATE TABLE IF NOT EXISTS notification_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(50) NOT NULL, -- 'threshold', 'schedule', 'event'
    target_entity VARCHAR(50), -- 'report', 'school', 'user'
    condition_expression TEXT NOT NULL,
    notification_template TEXT NOT NULL,
    recipients_query TEXT, -- SQL to determine recipients
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Function to evaluate notification rules
CREATE OR REPLACE FUNCTION evaluate_notification_rules()
RETURNS INTEGER AS $$
DECLARE
    rule RECORD;
    should_notify BOOLEAN;
    recipients TEXT[];
    notifications_sent INTEGER := 0;
BEGIN
    FOR rule IN 
        SELECT * FROM notification_rules 
        WHERE is_active = TRUE
    LOOP
        -- Evaluate condition
        EXECUTE 'SELECT ' || rule.condition_expression INTO should_notify;
        
        IF should_notify THEN
            -- Get recipients
            EXECUTE rule.recipients_query INTO recipients;
            
            -- Create notifications
            FOR i IN 1..array_length(recipients, 1) LOOP
                INSERT INTO notification_queue (
                    user_id, title, body, priority
                ) VALUES (
                    recipients[i]::UUID,
                    rule.rule_name,
                    rule.notification_template,
                    'normal'
                );
                notifications_sent := notifications_sent + 1;
            END LOOP;
        END IF;
    END LOOP;
    
    RETURN notifications_sent;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 7: DASHBOARD AGGREGATION VIEWS
-- ============================================

-- Real-time dashboard metrics
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_dashboard_metrics AS
SELECT 
    -- Overall statistics
    (SELECT COUNT(*) FROM reports WHERE deleted_at IS NULL) as total_reports,
    (SELECT COUNT(*) FROM reports WHERE deleted_at IS NULL AND status IN ('DRAFT', 'SUBMITTED', 'pending')) as pending_reports,
    (SELECT COUNT(*) FROM reports WHERE deleted_at IS NULL AND is_overdue = TRUE) as overdue_reports,
    (SELECT COUNT(*) FROM schools WHERE deleted_at IS NULL AND is_active = TRUE) as active_schools,
    (SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND user_type = 'supervisor') as total_supervisors,
    
    -- Performance metrics
    (SELECT AVG(resolution_time_hours) FROM reports WHERE completed_date IS NOT NULL AND deleted_at IS NULL) as avg_resolution_hours,
    (SELECT AVG(efficiency_score) FROM reports WHERE efficiency_score IS NOT NULL AND deleted_at IS NULL) as avg_efficiency_score,
    
    -- Trends (last 30 days)
    (SELECT COUNT(*) FROM reports WHERE created_at > CURRENT_DATE - INTERVAL '30 days' AND deleted_at IS NULL) as reports_last_30_days,
    (SELECT COUNT(*) FROM reports WHERE completed_date > CURRENT_DATE - INTERVAL '30 days' AND deleted_at IS NULL) as completed_last_30_days,
    
    -- Current timestamp
    CURRENT_TIMESTAMP as last_updated;

CREATE UNIQUE INDEX idx_mv_dashboard_metrics ON mv_dashboard_metrics(last_updated);

-- Top performers view
CREATE OR REPLACE VIEW v_top_performers AS
SELECT 
    u.id,
    u.full_name,
    u.user_type,
    u.performance_rating,
    u.productivity_score,
    COUNT(r.id) as total_reports,
    AVG(r.efficiency_score) as avg_efficiency,
    COUNT(r.id) FILTER (WHERE r.completed_date IS NOT NULL) as completed_reports
FROM users u
LEFT JOIN reports r ON u.id = r.supervisor_id 
    AND r.deleted_at IS NULL 
    AND r.created_at > CURRENT_DATE - INTERVAL '30 days'
WHERE u.deleted_at IS NULL 
    AND u.user_type IN ('supervisor', 'technician')
GROUP BY u.id, u.full_name, u.user_type, u.performance_rating, u.productivity_score
ORDER BY u.performance_rating DESC NULLS LAST, completed_reports DESC
LIMIT 10;

-- ============================================
-- STEP 8: SMART ASSIGNMENT SYSTEM
-- ============================================

-- Function to suggest best supervisor for a school
CREATE OR REPLACE FUNCTION suggest_supervisor_for_school(
    p_school_id UUID
)
RETURNS TABLE (
    supervisor_id UUID,
    supervisor_name VARCHAR(255),
    suitability_score NUMERIC(5,2),
    current_workload INTEGER,
    avg_efficiency NUMERIC(5,2),
    distance_km NUMERIC(10,2)
) AS $$
BEGIN
    RETURN QUERY
    WITH school_location AS (
        SELECT latitude, longitude FROM schools WHERE id = p_school_id
    ),
    supervisor_stats AS (
        SELECT 
            u.id,
            u.full_name,
            COUNT(r.id) FILTER (WHERE r.status NOT IN ('COMPLETED', 'completed', 'CANCELLED', 'cancelled')) as current_workload,
            AVG(r.efficiency_score) as avg_efficiency,
            COUNT(DISTINCT ss.school_id) as assigned_schools
        FROM users u
        LEFT JOIN reports r ON u.id = r.supervisor_id AND r.deleted_at IS NULL
        LEFT JOIN supervisor_schools ss ON u.id = ss.supervisor_id AND ss.deleted_at IS NULL AND ss.is_active = TRUE
        WHERE u.user_type = 'supervisor' 
            AND u.deleted_at IS NULL 
            AND u.is_active = TRUE
        GROUP BY u.id, u.full_name
    )
    SELECT 
        ss.id as supervisor_id,
        ss.full_name as supervisor_name,
        -- Suitability score based on workload, efficiency, and capacity
        (
            (100 - LEAST(ss.current_workload * 10, 100)) * 0.4 +  -- Lower workload is better
            COALESCE(ss.avg_efficiency, 75) * 0.4 +              -- Higher efficiency is better
            (CASE WHEN ss.assigned_schools < 5 THEN 100 ELSE 50 END) * 0.2  -- Capacity check
        )::NUMERIC(5,2) as suitability_score,
        ss.current_workload,
        ss.avg_efficiency,
        0::NUMERIC(10,2) as distance_km  -- Would calculate actual distance if coordinates available
    FROM supervisor_stats ss
    ORDER BY suitability_score DESC
    LIMIT 5;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 9: DATA EXPORT FUNCTIONS
-- ============================================

-- Function to export data to JSON format
CREATE OR REPLACE FUNCTION export_data_to_json(
    p_table_name TEXT,
    p_where_clause TEXT DEFAULT NULL,
    p_limit INTEGER DEFAULT NULL
)
RETURNS JSON AS $$
DECLARE
    query TEXT;
    result JSON;
BEGIN
    query := format('SELECT json_agg(t) FROM (SELECT * FROM %I', p_table_name);
    
    IF p_where_clause IS NOT NULL THEN
        query := query || ' WHERE ' || p_where_clause;
    END IF;
    
    IF p_limit IS NOT NULL THEN
        query := query || format(' LIMIT %s', p_limit);
    END IF;
    
    query := query || ') t';
    
    EXECUTE query INTO result;
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 10: AUTOMATED REPORT GENERATION
-- ============================================

-- Table for scheduled reports
CREATE TABLE IF NOT EXISTS scheduled_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_name VARCHAR(100) NOT NULL,
    report_type VARCHAR(50) NOT NULL, -- 'daily', 'weekly', 'monthly'
    query_template TEXT NOT NULL,
    recipients TEXT[],
    last_run TIMESTAMP WITH TIME ZONE,
    next_run TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Function to generate report
CREATE OR REPLACE FUNCTION generate_scheduled_report(
    p_report_id UUID
)
RETURNS JSON AS $$
DECLARE
    report RECORD;
    result JSON;
BEGIN
    SELECT * INTO report FROM scheduled_reports WHERE id = p_report_id;
    
    IF report IS NULL THEN
        RAISE EXCEPTION 'Report not found';
    END IF;
    
    -- Execute the report query
    EXECUTE report.query_template INTO result;
    
    -- Update last run time
    UPDATE scheduled_reports 
    SET last_run = CURRENT_TIMESTAMP,
        next_run = CASE report_type
            WHEN 'daily' THEN CURRENT_TIMESTAMP + INTERVAL '1 day'
            WHEN 'weekly' THEN CURRENT_TIMESTAMP + INTERVAL '1 week'
            WHEN 'monthly' THEN CURRENT_TIMESTAMP + INTERVAL '1 month'
        END
    WHERE id = p_report_id;
    
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 11: PERFORMANCE MONITORING
-- ============================================

-- Query performance tracking
CREATE TABLE IF NOT EXISTS query_performance (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    query_hash VARCHAR(64) NOT NULL,
    query_text TEXT,
    execution_count INTEGER DEFAULT 1,
    total_time_ms NUMERIC(10,2),
    avg_time_ms NUMERIC(10,2),
    max_time_ms NUMERIC(10,2),
    last_executed TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_query_hash UNIQUE (query_hash)
);

-- Function to track query performance
CREATE OR REPLACE FUNCTION track_query_performance(
    p_query TEXT,
    p_execution_time_ms NUMERIC
)
RETURNS VOID AS $$
DECLARE
    query_hash_val VARCHAR(64);
BEGIN
    -- Generate hash of query
    query_hash_val := encode(digest(p_query, 'sha256'), 'hex');
    
    -- Update or insert performance record
    INSERT INTO query_performance (
        query_hash, query_text, execution_count, 
        total_time_ms, avg_time_ms, max_time_ms, last_executed
    ) VALUES (
        query_hash_val, p_query, 1, 
        p_execution_time_ms, p_execution_time_ms, p_execution_time_ms, CURRENT_TIMESTAMP
    )
    ON CONFLICT (query_hash) DO UPDATE SET
        execution_count = query_performance.execution_count + 1,
        total_time_ms = query_performance.total_time_ms + p_execution_time_ms,
        avg_time_ms = (query_performance.total_time_ms + p_execution_time_ms) / (query_performance.execution_count + 1),
        max_time_ms = GREATEST(query_performance.max_time_ms, p_execution_time_ms),
        last_executed = CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 12: SMART CACHING SYSTEM
-- ============================================

-- Create intelligent cache management
CREATE TABLE IF NOT EXISTS smart_cache (
    cache_key VARCHAR(255) PRIMARY KEY,
    cache_value JSONB NOT NULL,
    cache_type VARCHAR(50) NOT NULL,
    hit_count INTEGER DEFAULT 0,
    miss_count INTEGER DEFAULT 0,
    last_accessed TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_smart_cache_expires ON smart_cache(expires_at);
CREATE INDEX idx_smart_cache_type ON smart_cache(cache_type);

-- Function to get or set cache
CREATE OR REPLACE FUNCTION get_or_set_cache(
    p_key VARCHAR(255),
    p_query TEXT,
    p_ttl_minutes INTEGER DEFAULT 60
)
RETURNS JSONB AS $$
DECLARE
    cached_value JSONB;
    new_value JSONB;
BEGIN
    -- Try to get from cache
    SELECT cache_value INTO cached_value
    FROM smart_cache
    WHERE cache_key = p_key AND expires_at > CURRENT_TIMESTAMP;
    
    IF cached_value IS NOT NULL THEN
        -- Update hit count
        UPDATE smart_cache 
        SET hit_count = hit_count + 1,
            last_accessed = CURRENT_TIMESTAMP
        WHERE cache_key = p_key;
        
        RETURN cached_value;
    ELSE
        -- Execute query and cache result
        EXECUTE p_query INTO new_value;
        
        INSERT INTO smart_cache (
            cache_key, cache_value, cache_type, expires_at
        ) VALUES (
            p_key, new_value, 'query_result', 
            CURRENT_TIMESTAMP + (p_ttl_minutes || ' minutes')::INTERVAL
        )
        ON CONFLICT (cache_key) DO UPDATE SET
            cache_value = new_value,
            expires_at = CURRENT_TIMESTAMP + (p_ttl_minutes || ' minutes')::INTERVAL,
            miss_count = smart_cache.miss_count + 1,
            last_accessed = CURRENT_TIMESTAMP;
        
        RETURN new_value;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 13: FINAL OPTIMIZATIONS
-- ============================================

-- Update all calculated fields
CREATE OR REPLACE FUNCTION update_all_calculated_fields()
RETURNS VOID AS $$
BEGIN
    -- Update report metrics
    UPDATE reports SET 
        age_days = CASE 
            WHEN completed_date IS NOT NULL THEN (completed_date - reported_date)
            ELSE (CURRENT_DATE - reported_date)
        END
    WHERE deleted_at IS NULL;
    
    -- Calculate school scores
    PERFORM calculate_school_scores();
    
    -- Calculate user performance
    PERFORM calculate_user_performance();
    
    -- Refresh materialized views
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_dashboard_metrics;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_dashboard_stats;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_school_performance;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_supervisor_workload;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_school_maintenance_summary;
    
    -- Log completion
    INSERT INTO system_logs (level, message, created_at)
    VALUES ('INFO', 'All calculated fields updated successfully', CURRENT_TIMESTAMP);
END;
$$ LANGUAGE plpgsql;

-- Schedule regular updates
CREATE OR REPLACE FUNCTION schedule_calculations()
RETURNS VOID AS $$
BEGIN
    -- This would be called by a cron job or scheduler
    PERFORM update_all_calculated_fields();
    PERFORM evaluate_notification_rules();
    PERFORM update_deletion_stats();
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 14: COMMENTS
-- ============================================

COMMENT ON FUNCTION global_search IS 'Performs fuzzy full-text search across multiple tables';
COMMENT ON FUNCTION predict_maintenance_needs IS 'Predicts future maintenance needs based on historical patterns';
COMMENT ON FUNCTION suggest_supervisor_for_school IS 'Suggests best supervisor assignment based on workload and performance';
COMMENT ON FUNCTION calculate_school_scores IS 'Calculates maintenance and activity scores for schools';
COMMENT ON FUNCTION calculate_user_performance IS 'Calculates performance ratings for users';
COMMENT ON FUNCTION generate_scheduled_report IS 'Generates reports based on scheduled templates';
COMMENT ON FUNCTION get_or_set_cache IS 'Intelligent caching system with TTL and hit tracking';

COMMENT ON TABLE analytics_summary IS 'Aggregated analytics data for reporting';
COMMENT ON TABLE time_series_data IS 'Time series data for trend analysis';
COMMENT ON TABLE notification_rules IS 'Configurable notification rules engine';
COMMENT ON TABLE scheduled_reports IS 'Scheduled report configurations';
COMMENT ON TABLE query_performance IS 'Query performance monitoring data';
COMMENT ON TABLE smart_cache IS 'Intelligent caching system with analytics';

COMMENT ON VIEW v_top_performers IS 'Top performing supervisors based on efficiency metrics';
COMMENT ON MATERIALIZED VIEW mv_dashboard_metrics IS 'Pre-calculated dashboard metrics for fast loading';

-- End of V9 Advanced Features migration