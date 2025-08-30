-- ============================================
-- V15__Update_Views_For_Multi_Tenancy.sql
-- Update views and materialized views for multi-tenant support
-- ============================================

-- ============================================
-- SECTION 1: DROP EXISTING MATERIALIZED VIEWS
-- ============================================

DROP MATERIALIZED VIEW IF EXISTS mv_dashboard_stats CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_dashboard_metrics CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_school_maintenance_summary CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_school_performance CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_supervisor_workload CASCADE;

-- ============================================
-- SECTION 2: DROP EXISTING VIEWS
-- ============================================

DROP VIEW IF EXISTS active_admin_supervisor_assignments CASCADE;
DROP VIEW IF EXISTS active_supervisor_technician_assignments CASCADE;
DROP VIEW IF EXISTS school_overview CASCADE;
DROP VIEW IF EXISTS school_report_summary CASCADE;
DROP VIEW IF EXISTS supervisor_team_overview CASCADE;
DROP VIEW IF EXISTS technician_workload_view CASCADE;
DROP VIEW IF EXISTS user_overview CASCADE;

-- ============================================
-- SECTION 3: RECREATE VIEWS WITH MULTI-TENANCY
-- ============================================

-- Active Supervisor-Technician Assignments View
CREATE OR REPLACE VIEW active_supervisor_technician_assignments AS
SELECT 
    st.id,
    st.company_id,
    st.supervisor_id,
    s.username AS supervisor_username,
    s.full_name AS supervisor_name,
    s.email AS supervisor_email,
    st.technician_id,
    t.username AS technician_username,
    t.full_name AS technician_name,
    t.email AS technician_email,
    t.specialization AS technician_specialization,
    t.skill_level AS technician_skill_level,
    st.assigned_date,
    st.primary_specialization,
    st.priority_level,
    st.tasks_assigned,
    st.tasks_completed,
    CASE 
        WHEN st.tasks_assigned > 0 
        THEN ROUND((st.tasks_completed::NUMERIC / st.tasks_assigned::NUMERIC) * 100, 2)
        ELSE 0
    END AS completion_rate_percentage,
    st.avg_completion_time_hours,
    st.assignment_notes,
    st.created_at,
    st.updated_at
FROM supervisor_technicians st
JOIN users s ON st.supervisor_id = s.id AND s.company_id = st.company_id
JOIN users t ON st.technician_id = t.id AND t.company_id = st.company_id
WHERE st.is_active = true 
    AND s.user_type = 'supervisor'
    AND t.user_type = 'technician'
    AND s.deleted_at IS NULL 
    AND t.deleted_at IS NULL
ORDER BY st.priority_level, st.assigned_date DESC;

-- Active Admin-Supervisor Assignments View
CREATE OR REPLACE VIEW active_admin_supervisor_assignments AS
SELECT 
    aso.id,
    aso.company_id,
    aso.admin_id,
    a.username AS admin_username,
    a.full_name AS admin_name,
    a.email AS admin_email,
    aso.supervisor_id,
    s.username AS supervisor_username,
    s.full_name AS supervisor_name,
    s.email AS supervisor_email,
    s.department AS supervisor_department,
    aso.assigned_date,
    aso.region,
    aso.authority_level,
    aso.max_schools_oversight,
    aso.supervisors_managed,
    aso.total_schools_covered,
    aso.technicians_covered,
    aso.efficiency_rating,
    aso.is_primary_admin,
    CASE 
        WHEN aso.max_schools_oversight > 0 
        THEN ROUND((aso.total_schools_covered::NUMERIC / aso.max_schools_oversight::NUMERIC) * 100, 2)
        ELSE 0
    END AS coverage_percentage,
    CASE 
        WHEN aso.supervisors_managed > 0 
        THEN ROUND(aso.technicians_covered::NUMERIC / aso.supervisors_managed::NUMERIC, 2)
        ELSE 0
    END AS technician_per_supervisor_ratio,
    aso.assignment_notes,
    aso.created_at,
    aso.updated_at
FROM admin_supervisors aso
JOIN users a ON aso.admin_id = a.id AND a.company_id = aso.company_id
JOIN users s ON aso.supervisor_id = s.id AND s.company_id = aso.company_id
WHERE aso.is_active = true 
    AND a.user_type IN ('admin', 'super_admin')
    AND s.user_type = 'supervisor'
    AND a.deleted_at IS NULL 
    AND s.deleted_at IS NULL
ORDER BY aso.authority_level, aso.assigned_date DESC;

-- User Overview View
CREATE OR REPLACE VIEW user_overview AS
SELECT 
    u.id,
    u.company_id,
    c.name AS company_name,
    u.username,
    u.email,
    u.full_name,
    u.user_type,
    u.status,
    u.is_active,
    u.department,
    u.specialization,
    u.skill_level,
    u.phone,
    u.last_login_at,
    u.created_at,
    u.updated_at,
    CASE 
        WHEN u.user_type = 'supervisor' THEN (
            SELECT COUNT(*) FROM supervisor_technicians st 
            WHERE st.supervisor_id = u.id AND st.company_id = u.company_id AND st.is_active = true
        )
        WHEN u.user_type = 'admin' THEN (
            SELECT COUNT(*) FROM admin_supervisors aso 
            WHERE aso.admin_id = u.id AND aso.company_id = u.company_id AND aso.is_active = true
        )
        ELSE 0
    END AS subordinates_count,
    CASE 
        WHEN u.user_type = 'technician' THEN (
            SELECT COUNT(*) FROM work_orders wo 
            WHERE wo.assigned_to = u.id AND wo.company_id = u.company_id AND wo.status NOT IN ('completed', 'cancelled')
        )
        WHEN u.user_type = 'supervisor' THEN (
            SELECT COUNT(*) FROM reports r 
            WHERE r.supervisor_id = u.id AND r.company_id = u.company_id AND r.status NOT IN ('COMPLETED', 'CANCELLED')
        )
        ELSE 0
    END AS active_tasks_count
FROM users u
LEFT JOIN companies c ON u.company_id = c.id
WHERE u.deleted_at IS NULL;

-- School Overview View
CREATE OR REPLACE VIEW school_overview AS
SELECT 
    s.id,
    s.company_id,
    c.name AS company_name,
    s.name,
    s.name_ar,
    s.code,
    s.type,
    s.gender,
    s.student_count,
    s.staff_count,
    s.building_area,
    s.is_active,
    s.city,
    s.district,
    s.created_at,
    s.updated_at,
    (SELECT COUNT(*) FROM reports r 
     WHERE r.school_id = s.id AND r.company_id = s.company_id) AS total_reports,
    (SELECT COUNT(*) FROM reports r 
     WHERE r.school_id = s.id AND r.company_id = s.company_id 
     AND r.status NOT IN ('COMPLETED', 'CANCELLED')) AS pending_reports,
    (SELECT COUNT(*) FROM work_orders wo 
     WHERE wo.school_id = s.id AND wo.company_id = s.company_id) AS total_work_orders,
    (SELECT COUNT(*) FROM work_orders wo 
     WHERE wo.school_id = s.id AND wo.company_id = s.company_id 
     AND wo.status NOT IN ('completed', 'cancelled')) AS active_work_orders,
    (SELECT COUNT(*) FROM assets a 
     WHERE a.school_id = s.id AND a.company_id = s.company_id 
     AND a.status = 'active') AS active_assets
FROM schools s
LEFT JOIN companies c ON s.company_id = c.id
WHERE s.deleted_at IS NULL;

-- ============================================
-- SECTION 4: RECREATE MATERIALIZED VIEWS WITH MULTI-TENANCY
-- ============================================

-- Company Dashboard Statistics Materialized View
CREATE MATERIALIZED VIEW mv_company_dashboard_stats AS
WITH company_stats AS (
    SELECT 
        c.id AS company_id,
        c.name AS company_name,
        
        -- User statistics
        (SELECT COUNT(*) FROM users u 
         WHERE u.company_id = c.id AND u.deleted_at IS NULL) AS total_users,
        
        (SELECT COUNT(*) FROM users u 
         WHERE u.company_id = c.id AND u.user_type = 'supervisor' 
         AND u.deleted_at IS NULL) AS total_supervisors,
        
        (SELECT COUNT(*) FROM users u 
         WHERE u.company_id = c.id AND u.user_type = 'technician' 
         AND u.deleted_at IS NULL) AS total_technicians,
        
        -- School statistics
        (SELECT COUNT(*) FROM schools s 
         WHERE s.company_id = c.id AND s.deleted_at IS NULL) AS total_schools,
        
        (SELECT COUNT(*) FROM schools s 
         WHERE s.company_id = c.id AND s.is_active = true 
         AND s.deleted_at IS NULL) AS active_schools,
        
        -- Report statistics
        (SELECT COUNT(*) FROM reports r 
         WHERE r.company_id = c.id 
         AND r.created_at >= CURRENT_DATE - INTERVAL '30 days') AS reports_last_30_days,
        
        (SELECT COUNT(*) FROM reports r 
         WHERE r.company_id = c.id 
         AND r.status IN ('DRAFT', 'SUBMITTED', 'pending', 'IN_REVIEW')) AS pending_reports,
        
        -- Work order statistics
        (SELECT COUNT(*) FROM work_orders wo 
         WHERE wo.company_id = c.id AND wo.status = 'pending') AS pending_work_orders,
        
        (SELECT COUNT(*) FROM work_orders wo 
         WHERE wo.company_id = c.id AND wo.status = 'in_progress') AS in_progress_work_orders,
        
        (SELECT COUNT(*) FROM work_orders wo 
         WHERE wo.company_id = c.id AND wo.status = 'completed' 
         AND wo.actual_end >= CURRENT_DATE - INTERVAL '30 days') AS completed_work_orders_30d,
        
        -- Asset statistics
        (SELECT COUNT(*) FROM assets a 
         WHERE a.company_id = c.id AND a.status = 'active') AS active_assets,
        
        (SELECT COUNT(*) FROM assets a 
         WHERE a.company_id = c.id 
         AND a.next_maintenance_date <= CURRENT_DATE + INTERVAL '7 days' 
         AND a.status = 'active') AS assets_due_maintenance,
        
        -- Inventory statistics
        (SELECT COUNT(*) FROM inventory_items i 
         WHERE i.company_id = c.id 
         AND i.current_stock <= i.minimum_stock 
         AND i.is_active = true) AS low_stock_items,
        
        CURRENT_TIMESTAMP AS last_updated
    FROM companies c
    WHERE c.deleted_at IS NULL AND c.is_active = true
)
SELECT * FROM company_stats;

-- Create unique index for concurrent refresh
CREATE UNIQUE INDEX idx_mv_company_dashboard_stats_company 
ON mv_company_dashboard_stats(company_id);

-- School Performance Materialized View
CREATE MATERIALIZED VIEW mv_school_performance AS
SELECT 
    s.id AS school_id,
    s.company_id,
    s.name AS school_name,
    s.code,
    s.type,
    s.gender,
    
    -- Report metrics
    COUNT(DISTINCT r.id) AS total_reports,
    COUNT(DISTINCT CASE WHEN r.status = 'COMPLETED' THEN r.id END) AS completed_reports,
    COUNT(DISTINCT CASE WHEN r.status IN ('DRAFT', 'SUBMITTED', 'IN_REVIEW') THEN r.id END) AS pending_reports,
    
    -- Work order metrics
    COUNT(DISTINCT wo.id) AS total_work_orders,
    COUNT(DISTINCT CASE WHEN wo.status = 'completed' THEN wo.id END) AS completed_work_orders,
    COUNT(DISTINCT CASE WHEN wo.status = 'in_progress' THEN wo.id END) AS in_progress_work_orders,
    
    -- Performance metrics
    AVG(CASE 
        WHEN wo.status = 'completed' AND wo.scheduled_end IS NOT NULL 
        THEN EXTRACT(EPOCH FROM (wo.actual_end - wo.scheduled_end)) / 3600 
    END) AS avg_completion_delay_hours,
    
    AVG(wo.completion_percentage) AS avg_completion_percentage,
    SUM(wo.total_cost) AS total_maintenance_cost,
    
    -- Asset metrics
    COUNT(DISTINCT a.id) AS total_assets,
    COUNT(DISTINCT CASE WHEN a.status = 'active' THEN a.id END) AS active_assets,
    COUNT(DISTINCT CASE 
        WHEN a.next_maintenance_date <= CURRENT_DATE + INTERVAL '30 days' 
        THEN a.id 
    END) AS assets_due_maintenance_30d,
    
    CURRENT_TIMESTAMP AS last_updated
FROM schools s
LEFT JOIN reports r ON s.id = r.school_id AND s.company_id = r.company_id
LEFT JOIN work_orders wo ON s.id = wo.school_id AND s.company_id = wo.company_id
LEFT JOIN assets a ON s.id = a.school_id AND s.company_id = a.company_id
WHERE s.deleted_at IS NULL
GROUP BY s.id, s.company_id, s.name, s.code, s.type, s.gender;

-- Create unique index for concurrent refresh
CREATE UNIQUE INDEX idx_mv_school_performance_school 
ON mv_school_performance(school_id, company_id);

-- Supervisor Workload Materialized View
CREATE MATERIALIZED VIEW mv_supervisor_workload AS
SELECT 
    u.id AS supervisor_id,
    u.company_id,
    u.username AS supervisor_username,
    u.full_name AS supervisor_name,
    u.department,
    
    -- Technician management
    COUNT(DISTINCT st.technician_id) AS technicians_managed,
    
    -- Report metrics
    COUNT(DISTINCT r.id) AS total_reports,
    COUNT(DISTINCT CASE WHEN r.status IN ('DRAFT', 'SUBMITTED', 'IN_REVIEW') THEN r.id END) AS pending_reports,
    COUNT(DISTINCT CASE WHEN r.status = 'COMPLETED' THEN r.id END) AS completed_reports,
    
    -- Work order metrics (through technicians)
    COUNT(DISTINCT wo.id) AS total_work_orders,
    COUNT(DISTINCT CASE WHEN wo.status = 'in_progress' THEN wo.id END) AS active_work_orders,
    COUNT(DISTINCT CASE WHEN wo.status = 'completed' THEN wo.id END) AS completed_work_orders,
    
    -- Performance metrics
    AVG(st.tasks_completed::NUMERIC / NULLIF(st.tasks_assigned, 0) * 100) AS avg_team_completion_rate,
    AVG(st.avg_completion_time_hours) AS avg_team_completion_time,
    
    -- School coverage
    COUNT(DISTINCT r.school_id) AS schools_covered,
    
    CURRENT_TIMESTAMP AS last_updated
FROM users u
LEFT JOIN supervisor_technicians st ON u.id = st.supervisor_id AND u.company_id = st.company_id AND st.is_active = true
LEFT JOIN reports r ON u.id = r.supervisor_id AND u.company_id = r.company_id
LEFT JOIN work_orders wo ON st.technician_id = wo.assigned_to AND st.company_id = wo.company_id
WHERE u.user_type = 'supervisor' 
    AND u.deleted_at IS NULL 
    AND u.is_active = true
GROUP BY u.id, u.company_id, u.username, u.full_name, u.department;

-- Create unique index for concurrent refresh
CREATE UNIQUE INDEX idx_mv_supervisor_workload_supervisor 
ON mv_supervisor_workload(supervisor_id, company_id);

-- ============================================
-- SECTION 5: CREATE REFRESH FUNCTIONS
-- ============================================

-- Function to refresh all materialized views
CREATE OR REPLACE FUNCTION refresh_all_materialized_views()
RETURNS void AS $$
BEGIN
    -- Refresh with CONCURRENTLY to avoid locking
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_company_dashboard_stats;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_school_performance;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_supervisor_workload;
    
    RAISE NOTICE 'All materialized views refreshed at %', CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- Function to refresh company-specific views
CREATE OR REPLACE FUNCTION refresh_company_materialized_views(p_company_id UUID)
RETURNS void AS $$
BEGIN
    -- Since we can't refresh only specific rows, we refresh all
    -- But this function can be extended later for partition-based views
    PERFORM refresh_all_materialized_views();
    
    RAISE NOTICE 'Materialized views refreshed for company % at %', p_company_id, CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- SECTION 6: CREATE SCHEDULED REFRESH
-- ============================================

-- Schedule automatic refresh (requires pg_cron extension)
-- Uncomment and modify if pg_cron is available:
-- SELECT cron.schedule('refresh-materialized-views', '0 */6 * * *', 'SELECT refresh_all_materialized_views();');

-- ============================================
-- SECTION 7: GRANT PERMISSIONS
-- ============================================

-- Grant SELECT permissions on views and materialized views to application user
GRANT SELECT ON ALL TABLES IN SCHEMA public TO cafm_user;

-- Grant SELECT on specific materialized views
GRANT SELECT ON mv_company_dashboard_stats TO cafm_user;
GRANT SELECT ON mv_school_performance TO cafm_user;
GRANT SELECT ON mv_supervisor_workload TO cafm_user;

-- ============================================
-- SECTION 8: ADD COMMENTS
-- ============================================

COMMENT ON MATERIALIZED VIEW mv_company_dashboard_stats IS 'Company-level dashboard statistics with multi-tenant support';
COMMENT ON MATERIALIZED VIEW mv_school_performance IS 'School performance metrics aggregated by company';
COMMENT ON MATERIALIZED VIEW mv_supervisor_workload IS 'Supervisor workload and team performance metrics';

COMMENT ON VIEW active_supervisor_technician_assignments IS 'Active supervisor-technician assignments with multi-tenant filtering';
COMMENT ON VIEW active_admin_supervisor_assignments IS 'Active admin-supervisor assignments with multi-tenant filtering';
COMMENT ON VIEW user_overview IS 'Comprehensive user information with company context';
COMMENT ON VIEW school_overview IS 'School information with related metrics by company';

-- End of V15 migration