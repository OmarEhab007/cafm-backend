-- ============================================================================
-- V111: Performance Optimization Indexes
-- ============================================================================
-- Purpose: Add database indexes for common queries to improve performance
-- Author: CAFM System
-- Date: 2025-01-15
-- ============================================================================

-- ============================================================================
-- User Table Indexes
-- ============================================================================
-- Index for email lookup (authentication)
CREATE INDEX IF NOT EXISTS idx_users_email_lower 
ON users(LOWER(email)) 
WHERE deleted_at IS NULL;

-- Index for company-based user queries
CREATE INDEX IF NOT EXISTS idx_users_company_status 
ON users(company_id, status) 
WHERE deleted_at IS NULL;

-- Index for user type queries
CREATE INDEX IF NOT EXISTS idx_users_type_company 
ON users(user_type, company_id) 
WHERE deleted_at IS NULL;

-- ============================================================================
-- School Table Indexes
-- ============================================================================
-- Index for school code lookup
CREATE INDEX IF NOT EXISTS idx_schools_code_company 
ON schools(code, company_id) 
WHERE deleted_at IS NULL;

-- Index for active schools by company
CREATE INDEX IF NOT EXISTS idx_schools_active_company 
ON schools(company_id, is_active) 
WHERE deleted_at IS NULL;

-- Index for school name search
CREATE INDEX IF NOT EXISTS idx_schools_name_trgm 
ON schools USING gin(name gin_trgm_ops) 
WHERE deleted_at IS NULL;

-- Index for city-based queries
CREATE INDEX IF NOT EXISTS idx_schools_city_company 
ON schools(city, company_id) 
WHERE deleted_at IS NULL;

-- ============================================================================
-- Asset Table Indexes
-- ============================================================================
-- Index for asset code lookup
CREATE INDEX IF NOT EXISTS idx_assets_code_company 
ON assets(asset_code, company_id);

-- Index for asset status queries
CREATE INDEX IF NOT EXISTS idx_assets_status_company 
ON assets(status, company_id);

-- Index for asset condition queries
CREATE INDEX IF NOT EXISTS idx_assets_condition_company 
ON assets(condition, company_id);

-- Index for school-based asset queries
CREATE INDEX IF NOT EXISTS idx_assets_school_status 
ON assets(school_id, status);

-- Index for assigned assets
CREATE INDEX IF NOT EXISTS idx_assets_assigned_to_status 
ON assets(assigned_to, status);

-- ============================================================================
-- Report Table Indexes
-- ============================================================================
-- Index for report status queries
CREATE INDEX IF NOT EXISTS idx_reports_status_company 
ON reports(status, company_id) 
WHERE deleted_at IS NULL;

-- Index for school-based report queries
CREATE INDEX IF NOT EXISTS idx_reports_school_status 
ON reports(school_id, status) 
WHERE deleted_at IS NULL;

-- Index for supervisor assignment queries
CREATE INDEX IF NOT EXISTS idx_reports_supervisor_status 
ON reports(supervisor_id, status) 
WHERE deleted_at IS NULL;

-- Index for date-based queries
CREATE INDEX IF NOT EXISTS idx_reports_created_at_company 
ON reports(created_at DESC, company_id) 
WHERE deleted_at IS NULL;

-- Index for priority queries
CREATE INDEX IF NOT EXISTS idx_reports_priority_status 
ON reports(priority, status) 
WHERE deleted_at IS NULL;

-- ============================================================================
-- Work Order Table Indexes
-- ============================================================================
-- Index for work order status queries
CREATE INDEX IF NOT EXISTS idx_work_orders_status_company 
ON work_orders(status, company_id);

-- Index for assigned work orders
CREATE INDEX IF NOT EXISTS idx_work_orders_assigned_status 
ON work_orders(assigned_to, status);

-- Index for report-based queries
CREATE INDEX IF NOT EXISTS idx_work_orders_report_id 
ON work_orders(report_id);

-- Index for priority and scheduled end
CREATE INDEX IF NOT EXISTS idx_work_orders_priority_scheduled 
ON work_orders(priority, scheduled_end) 
WHERE status != 'COMPLETED';

-- ============================================================================
-- Company Table Indexes
-- ============================================================================
-- Index for domain lookup
CREATE UNIQUE INDEX IF NOT EXISTS idx_companies_domain_unique 
ON companies(domain) 
WHERE deleted_at IS NULL AND domain IS NOT NULL;

-- Index for subdomain lookup
CREATE UNIQUE INDEX IF NOT EXISTS idx_companies_subdomain_unique 
ON companies(subdomain) 
WHERE deleted_at IS NULL AND subdomain IS NOT NULL;

-- Index for subscription queries
CREATE INDEX IF NOT EXISTS idx_companies_subscription_status 
ON companies(status, subscription_end_date) 
WHERE deleted_at IS NULL;

-- ============================================================================
-- Refresh Token Table Indexes
-- ============================================================================
-- Index for token lookup
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash 
ON refresh_tokens(token_hash) 
WHERE revoked = false;

-- Index for user token cleanup
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_expires 
ON refresh_tokens(user_id, expires_at) 
WHERE revoked = false;

-- ============================================================================
-- Supervisor School Indexes
-- ============================================================================
-- Index for supervisor school assignments
CREATE INDEX IF NOT EXISTS idx_supervisor_schools_supervisor 
ON supervisor_schools(supervisor_id);

-- Index for school supervisor lookup
CREATE INDEX IF NOT EXISTS idx_supervisor_schools_school 
ON supervisor_schools(school_id);

-- ============================================================================
-- Composite Indexes for Complex Queries
-- ============================================================================
-- Company statistics query optimization
CREATE INDEX IF NOT EXISTS idx_users_company_type_status 
ON users(company_id, user_type, status) 
WHERE deleted_at IS NULL;

-- School search optimization
CREATE INDEX IF NOT EXISTS idx_schools_company_active_type 
ON schools(company_id, is_active, type) 
WHERE deleted_at IS NULL;

-- Asset maintenance query optimization
CREATE INDEX IF NOT EXISTS idx_assets_next_maintenance 
ON assets(next_maintenance_date, status) 
WHERE status = 'ACTIVE';

-- Report dashboard optimization
CREATE INDEX IF NOT EXISTS idx_reports_company_status_created 
ON reports(company_id, status, created_at DESC) 
WHERE deleted_at IS NULL;

-- ============================================================================
-- Function-based Indexes
-- ============================================================================
-- Case-insensitive email search
CREATE INDEX IF NOT EXISTS idx_users_email_ci 
ON users(LOWER(email));

-- Case-insensitive name searches
CREATE INDEX IF NOT EXISTS idx_schools_name_lower 
ON schools(LOWER(name)) 
WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_users_full_name_lower 
ON users(LOWER(first_name || ' ' || last_name)) 
WHERE deleted_at IS NULL;

-- ============================================================================
-- Partial Indexes for Common Filters
-- ============================================================================
-- Active users only
CREATE INDEX IF NOT EXISTS idx_users_active 
ON users(company_id, email) 
WHERE deleted_at IS NULL AND status = 'ACTIVE';

-- Pending reports only
CREATE INDEX IF NOT EXISTS idx_reports_pending 
ON reports(company_id, created_at DESC) 
WHERE deleted_at IS NULL AND status = 'pending';

-- Active work orders only
CREATE INDEX IF NOT EXISTS idx_work_orders_active 
ON work_orders(assigned_to, scheduled_end) 
WHERE status IN ('ASSIGNED', 'IN_PROGRESS');

-- ============================================================================
-- Enable PostgreSQL Extensions for Better Performance
-- ============================================================================
-- Enable trigram extension for fuzzy text search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Enable btree_gin for better composite indexes
CREATE EXTENSION IF NOT EXISTS btree_gin;

-- Enable btree_gist for exclusion constraints
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ============================================================================
-- Update Table Statistics
-- ============================================================================
-- Analyze tables to update query planner statistics
ANALYZE users;
ANALYZE companies;
ANALYZE schools;
ANALYZE assets;
ANALYZE reports;
ANALYZE work_orders;
ANALYZE supervisor_schools;
ANALYZE refresh_tokens;

-- ============================================================================
-- Validation
-- ============================================================================
DO $$
BEGIN
    -- Check if critical indexes exist
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE indexname = 'idx_users_email_lower'
    ) THEN
        RAISE EXCEPTION 'Critical index idx_users_email_lower was not created';
    END IF;
    
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE indexname = 'idx_reports_status_company'
    ) THEN
        RAISE EXCEPTION 'Critical index idx_reports_status_company was not created';
    END IF;
    
    RAISE NOTICE 'Performance indexes created successfully';
END;
$$;