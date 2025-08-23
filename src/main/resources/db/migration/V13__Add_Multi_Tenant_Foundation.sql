-- ============================================
-- V13__Add_Multi_Tenant_Foundation.sql
-- Migration to add multi-tenancy foundation with companies table and tenant isolation
-- Implements hybrid multi-tenant architecture with Row-Level Security (RLS)
-- ============================================

-- ============================================
-- STEP 1: CREATE COMPANIES TABLE
-- ============================================

-- Create company subscription plan enum
CREATE TYPE subscription_plan AS ENUM (
    'free', 'basic', 'professional', 'enterprise'
);

-- Create company status enum  
CREATE TYPE company_status AS ENUM (
    'active', 'inactive', 'suspended', 'trial', 'pending_setup'
);

-- Main companies table
CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    domain VARCHAR(255) UNIQUE,
    subdomain VARCHAR(100) UNIQUE,
    
    -- Contact Information
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    primary_contact_name VARCHAR(255),
    
    -- Business Information
    industry VARCHAR(100),
    country VARCHAR(100) DEFAULT 'Saudi Arabia',
    city VARCHAR(100),
    address TEXT,
    postal_code VARCHAR(20),
    tax_number VARCHAR(50),
    commercial_registration VARCHAR(50),
    
    -- Configuration
    timezone VARCHAR(50) DEFAULT 'Asia/Riyadh',
    locale VARCHAR(10) DEFAULT 'ar_SA',
    currency VARCHAR(3) DEFAULT 'SAR',
    
    -- Subscription & Limits
    subscription_plan subscription_plan DEFAULT 'free',
    subscription_start_date DATE,
    subscription_end_date DATE,
    max_users INTEGER DEFAULT 10,
    max_schools INTEGER DEFAULT 5,
    max_supervisors INTEGER DEFAULT 3,
    max_technicians INTEGER DEFAULT 15,
    max_storage_gb INTEGER DEFAULT 5,
    
    -- Status & Settings
    status company_status DEFAULT 'pending_setup',
    is_active BOOLEAN DEFAULT true,
    settings JSONB DEFAULT '{}',
    features JSONB DEFAULT '{}',
    
    -- Multi-tenant Security
    data_classification VARCHAR(50) DEFAULT 'internal',
    compliance_requirements JSONB DEFAULT '{}',
    
    -- Audit Trail
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID
);

-- Add constraints
ALTER TABLE companies
    ADD CONSTRAINT check_company_max_users_positive 
        CHECK (max_users > 0),
    ADD CONSTRAINT check_company_max_schools_positive 
        CHECK (max_schools > 0),
    ADD CONSTRAINT check_company_max_supervisors_positive 
        CHECK (max_supervisors > 0),
    ADD CONSTRAINT check_company_max_technicians_positive 
        CHECK (max_technicians > 0),
    ADD CONSTRAINT check_company_max_storage_positive 
        CHECK (max_storage_gb > 0),
    ADD CONSTRAINT check_company_subscription_dates 
        CHECK (subscription_end_date IS NULL OR subscription_end_date >= subscription_start_date),
    ADD CONSTRAINT check_company_domain_format 
        CHECK (domain IS NULL OR domain ~* '^[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'),
    ADD CONSTRAINT check_company_subdomain_format 
        CHECK (subdomain IS NULL OR subdomain ~* '^[a-z0-9-]+$');

-- Create indexes for companies
CREATE INDEX idx_companies_status ON companies (status) WHERE deleted_at IS NULL;
CREATE INDEX idx_companies_active ON companies (is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_companies_subscription_plan ON companies (subscription_plan);
CREATE INDEX idx_companies_domain ON companies (domain) WHERE domain IS NOT NULL;
CREATE INDEX idx_companies_subdomain ON companies (subdomain) WHERE subdomain IS NOT NULL;
CREATE INDEX idx_companies_country ON companies (country);
CREATE INDEX idx_companies_created_at ON companies (created_at);
CREATE INDEX idx_companies_deleted_at ON companies (deleted_at);

-- GIN index for JSONB columns
CREATE INDEX idx_companies_settings ON companies USING GIN (settings);
CREATE INDEX idx_companies_features ON companies USING GIN (features);
CREATE INDEX idx_companies_compliance ON companies USING GIN (compliance_requirements);

-- ============================================
-- STEP 2: ADD COMPANY_ID TO EXISTING TABLES
-- ============================================

-- Add company_id to users table
ALTER TABLE users 
    ADD COLUMN company_id UUID REFERENCES companies(id) ON DELETE CASCADE;

-- Add company_id to schools table
ALTER TABLE schools 
    ADD COLUMN company_id UUID REFERENCES companies(id) ON DELETE CASCADE;

-- Add company_id to reports table  
ALTER TABLE reports 
    ADD COLUMN company_id UUID REFERENCES companies(id) ON DELETE CASCADE;

-- Add company_id to supervisor_schools table
ALTER TABLE supervisor_schools 
    ADD COLUMN company_id UUID REFERENCES companies(id) ON DELETE CASCADE;

-- Add company_id to supervisor_technicians table
ALTER TABLE supervisor_technicians 
    ADD COLUMN company_id UUID REFERENCES companies(id) ON DELETE CASCADE;

-- Add company_id to admin_supervisors table
ALTER TABLE admin_supervisors 
    ADD COLUMN company_id UUID REFERENCES companies(id) ON DELETE CASCADE;

-- Add company_id to maintenance_counts table (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'maintenance_counts') THEN
        ALTER TABLE maintenance_counts ADD COLUMN company_id UUID REFERENCES companies(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Add company_id to damage_counts table (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'damage_counts') THEN
        ALTER TABLE damage_counts ADD COLUMN company_id UUID REFERENCES companies(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Add company_id to report_attachments table (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'report_attachments') THEN
        ALTER TABLE report_attachments ADD COLUMN company_id UUID REFERENCES companies(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Add company_id to report_comments table (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'report_comments') THEN
        ALTER TABLE report_comments ADD COLUMN company_id UUID REFERENCES companies(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Add company_id to notifications table (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'notifications') THEN
        ALTER TABLE notifications ADD COLUMN company_id UUID REFERENCES companies(id) ON DELETE CASCADE;
    END IF;
END $$;

-- ============================================
-- STEP 3: CREATE INDEXES FOR TENANT FILTERING
-- ============================================

-- Core tenant filtering indexes
CREATE INDEX idx_users_company_id ON users (company_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_schools_company_id ON schools (company_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reports_company_id ON reports (company_id);
CREATE INDEX idx_supervisor_schools_company_id ON supervisor_schools (company_id);
CREATE INDEX idx_supervisor_technicians_company_id ON supervisor_technicians (company_id);
CREATE INDEX idx_admin_supervisors_company_id ON admin_supervisors (company_id);

-- Composite indexes for common tenant queries
CREATE INDEX idx_users_company_type_status 
    ON users (company_id, user_type, status) 
    WHERE deleted_at IS NULL;

CREATE INDEX idx_schools_company_active 
    ON schools (company_id, is_active) 
    WHERE deleted_at IS NULL;

CREATE INDEX idx_reports_company_status_date 
    ON reports (company_id, status, created_at);

-- ============================================
-- STEP 4: CREATE DEFAULT SYSTEM COMPANY
-- ============================================

-- Insert default system company for existing data
INSERT INTO companies (
    id,
    name,
    display_name,
    domain,
    subdomain,
    contact_email,
    industry,
    country,
    city,
    subscription_plan,
    subscription_start_date,
    max_users,
    max_schools,
    max_supervisors,
    max_technicians,
    max_storage_gb,
    status,
    is_active,
    settings,
    created_at
) VALUES (
    '00000000-0000-0000-0000-000000000001'::UUID,
    'Default System Company',
    'CAFM System',
    'system.cafm.local',
    'system',
    'admin@cafm.local',
    'Software',
    'Saudi Arabia',
    'Riyadh',
    'enterprise'::subscription_plan,
    CURRENT_DATE,
    1000,
    500,
    100,
    2000,
    1000,
    'active'::company_status,
    true,
    '{"is_system_company": true, "allow_multi_tenant": true}'::JSONB,
    CURRENT_TIMESTAMP
);

-- ============================================
-- STEP 5: ASSIGN EXISTING DATA TO DEFAULT COMPANY
-- ============================================

-- Update all existing users to belong to default company
UPDATE users 
SET company_id = '00000000-0000-0000-0000-000000000001'::UUID 
WHERE company_id IS NULL;

-- Update all existing schools to belong to default company  
UPDATE schools 
SET company_id = '00000000-0000-0000-0000-000000000001'::UUID 
WHERE company_id IS NULL;

-- Update all existing reports to belong to default company
UPDATE reports 
SET company_id = '00000000-0000-0000-0000-000000000001'::UUID 
WHERE company_id IS NULL;

-- Update supervisor_schools assignments
UPDATE supervisor_schools 
SET company_id = '00000000-0000-0000-0000-000000000001'::UUID 
WHERE company_id IS NULL;

-- Update supervisor_technicians assignments  
UPDATE supervisor_technicians 
SET company_id = '00000000-0000-0000-0000-000000000001'::UUID 
WHERE company_id IS NULL;

-- Update admin_supervisors assignments
UPDATE admin_supervisors 
SET company_id = '00000000-0000-0000-0000-000000000001'::UUID 
WHERE company_id IS NULL;

-- Update other tables if they exist
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'maintenance_counts') THEN
        UPDATE maintenance_counts SET company_id = '00000000-0000-0000-0000-000000000001'::UUID WHERE company_id IS NULL;
    END IF;
    
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'damage_counts') THEN
        UPDATE damage_counts SET company_id = '00000000-0000-0000-0000-000000000001'::UUID WHERE company_id IS NULL;
    END IF;
    
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'report_attachments') THEN
        UPDATE report_attachments SET company_id = '00000000-0000-0000-0000-000000000001'::UUID WHERE company_id IS NULL;
    END IF;
    
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'report_comments') THEN
        UPDATE report_comments SET company_id = '00000000-0000-0000-0000-000000000001'::UUID WHERE company_id IS NULL;
    END IF;
    
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'notifications') THEN
        UPDATE notifications SET company_id = '00000000-0000-0000-0000-000000000001'::UUID WHERE company_id IS NULL;
    END IF;
END $$;

-- ============================================
-- STEP 6: MAKE COMPANY_ID NOT NULL FOR CORE TABLES
-- ============================================

-- Now make company_id NOT NULL for core tables
ALTER TABLE users 
    ALTER COLUMN company_id SET NOT NULL;

ALTER TABLE schools 
    ALTER COLUMN company_id SET NOT NULL;

ALTER TABLE reports 
    ALTER COLUMN company_id SET NOT NULL;

ALTER TABLE supervisor_schools 
    ALTER COLUMN company_id SET NOT NULL;

ALTER TABLE supervisor_technicians 
    ALTER COLUMN company_id SET NOT NULL;

ALTER TABLE admin_supervisors 
    ALTER COLUMN company_id SET NOT NULL;

-- ============================================
-- STEP 7: ROW-LEVEL SECURITY SETUP
-- ============================================

-- Enable RLS on core tenant tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE schools ENABLE ROW LEVEL SECURITY;
ALTER TABLE reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE supervisor_schools ENABLE ROW LEVEL SECURITY;
ALTER TABLE supervisor_technicians ENABLE ROW LEVEL SECURITY;
ALTER TABLE admin_supervisors ENABLE ROW LEVEL SECURITY;

-- Create RLS policies for tenant isolation
-- Users policy
CREATE POLICY tenant_isolation_users ON users
    FOR ALL USING (company_id = COALESCE(
        NULLIF(current_setting('app.current_company_id', true), '')::UUID,
        '00000000-0000-0000-0000-000000000001'::UUID
    ));

-- Schools policy
CREATE POLICY tenant_isolation_schools ON schools
    FOR ALL USING (company_id = COALESCE(
        NULLIF(current_setting('app.current_company_id', true), '')::UUID,
        '00000000-0000-0000-0000-000000000001'::UUID
    ));

-- Reports policy
CREATE POLICY tenant_isolation_reports ON reports
    FOR ALL USING (company_id = COALESCE(
        NULLIF(current_setting('app.current_company_id', true), '')::UUID,
        '00000000-0000-0000-0000-000000000001'::UUID
    ));

-- Supervisor schools policy
CREATE POLICY tenant_isolation_supervisor_schools ON supervisor_schools
    FOR ALL USING (company_id = COALESCE(
        NULLIF(current_setting('app.current_company_id', true), '')::UUID,
        '00000000-0000-0000-0000-000000000001'::UUID
    ));

-- Supervisor technicians policy
CREATE POLICY tenant_isolation_supervisor_technicians ON supervisor_technicians
    FOR ALL USING (company_id = COALESCE(
        NULLIF(current_setting('app.current_company_id', true), '')::UUID,
        '00000000-0000-0000-0000-000000000001'::UUID
    ));

-- Admin supervisors policy
CREATE POLICY tenant_isolation_admin_supervisors ON admin_supervisors
    FOR ALL USING (company_id = COALESCE(
        NULLIF(current_setting('app.current_company_id', true), '')::UUID,
        '00000000-0000-0000-0000-000000000001'::UUID
    ));

-- ============================================
-- STEP 8: CREATE MULTI-TENANT UTILITY FUNCTIONS
-- ============================================

-- Function to set tenant context
CREATE OR REPLACE FUNCTION set_tenant_context(tenant_id UUID)
RETURNS VOID AS $$
BEGIN
    -- Set the tenant context for RLS
    PERFORM set_config('app.current_company_id', tenant_id::text, false);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get current tenant context
CREATE OR REPLACE FUNCTION get_current_tenant()
RETURNS UUID AS $$
BEGIN
    RETURN NULLIF(current_setting('app.current_company_id', true), '')::UUID;
END;
$$ LANGUAGE plpgsql;

-- Function to validate tenant access
CREATE OR REPLACE FUNCTION validate_tenant_access(tenant_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    company_active BOOLEAN;
BEGIN
    SELECT is_active INTO company_active 
    FROM companies 
    WHERE id = tenant_id 
    AND status = 'active' 
    AND deleted_at IS NULL;
    
    RETURN COALESCE(company_active, false);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get company resource usage
CREATE OR REPLACE FUNCTION get_company_usage(tenant_id UUID)
RETURNS TABLE (
    users_count BIGINT,
    schools_count BIGINT,
    reports_count BIGINT,
    storage_used_mb BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        (SELECT COUNT(*) FROM users WHERE company_id = tenant_id AND deleted_at IS NULL),
        (SELECT COUNT(*) FROM schools WHERE company_id = tenant_id AND deleted_at IS NULL),
        (SELECT COUNT(*) FROM reports WHERE company_id = tenant_id),
        0::BIGINT; -- Placeholder for storage calculation
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================
-- STEP 9: CREATE MULTI-TENANT VIEWS
-- ============================================

-- View for active companies with usage stats
CREATE OR REPLACE VIEW company_usage_summary AS
SELECT 
    c.id,
    c.name,
    c.display_name,
    c.subscription_plan,
    c.status,
    c.max_users,
    c.max_schools,
    c.max_supervisors,
    c.max_technicians,
    -- Current usage counts
    COALESCE(u.users_count, 0) as current_users,
    COALESCE(s.schools_count, 0) as current_schools,
    COALESCE(sp.supervisors_count, 0) as current_supervisors,
    COALESCE(t.technicians_count, 0) as current_technicians,
    COALESCE(r.reports_count, 0) as total_reports,
    -- Calculate usage percentages
    CASE 
        WHEN c.max_users > 0 THEN ROUND((COALESCE(u.users_count, 0)::DECIMAL / c.max_users::DECIMAL) * 100, 2)
        ELSE 0 
    END as users_usage_percentage,
    CASE 
        WHEN c.max_schools > 0 THEN ROUND((COALESCE(s.schools_count, 0)::DECIMAL / c.max_schools::DECIMAL) * 100, 2)
        ELSE 0 
    END as schools_usage_percentage,
    c.created_at,
    c.subscription_end_date
FROM companies c
-- Users count
LEFT JOIN (
    SELECT company_id, COUNT(*) as users_count 
    FROM users 
    WHERE deleted_at IS NULL 
    GROUP BY company_id
) u ON c.id = u.company_id
-- Schools count
LEFT JOIN (
    SELECT company_id, COUNT(*) as schools_count 
    FROM schools 
    WHERE deleted_at IS NULL 
    GROUP BY company_id
) s ON c.id = s.company_id
-- Supervisors count
LEFT JOIN (
    SELECT company_id, COUNT(*) as supervisors_count 
    FROM users 
    WHERE user_type = 'supervisor' AND deleted_at IS NULL 
    GROUP BY company_id
) sp ON c.id = sp.company_id
-- Technicians count
LEFT JOIN (
    SELECT company_id, COUNT(*) as technicians_count 
    FROM users 
    WHERE user_type = 'technician' AND deleted_at IS NULL 
    GROUP BY company_id
) t ON c.id = t.company_id
-- Reports count
LEFT JOIN (
    SELECT company_id, COUNT(*) as reports_count 
    FROM reports 
    GROUP BY company_id
) r ON c.id = r.company_id
WHERE c.deleted_at IS NULL
ORDER BY c.created_at DESC;

-- ============================================
-- STEP 10: ADD UPDATED_AT TRIGGER FOR COMPANIES
-- ============================================

-- Create updated_at trigger function for companies
CREATE OR REPLACE FUNCTION update_companies_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger
CREATE TRIGGER trg_companies_updated_at
    BEFORE UPDATE ON companies
    FOR EACH ROW
    EXECUTE FUNCTION update_companies_updated_at();

-- ============================================
-- STEP 11: ADD COMMENTS FOR DOCUMENTATION
-- ============================================

-- Table comments
COMMENT ON TABLE companies IS 'Multi-tenant companies/organizations table with subscription management';
COMMENT ON COLUMN companies.id IS 'Unique company identifier used as tenant ID';
COMMENT ON COLUMN companies.name IS 'Official company name';
COMMENT ON COLUMN companies.display_name IS 'Display name for UI (can be different from official name)';
COMMENT ON COLUMN companies.domain IS 'Company email domain for user validation';
COMMENT ON COLUMN companies.subdomain IS 'Unique subdomain for tenant access (e.g., company1.cafm.com)';
COMMENT ON COLUMN companies.subscription_plan IS 'Current subscription tier';
COMMENT ON COLUMN companies.max_users IS 'Maximum number of users allowed for this tenant';
COMMENT ON COLUMN companies.max_schools IS 'Maximum number of schools allowed for this tenant';
COMMENT ON COLUMN companies.settings IS 'Company-specific configuration settings';
COMMENT ON COLUMN companies.features IS 'Enabled features for this tenant';
COMMENT ON COLUMN companies.data_classification IS 'Data security classification level';

-- View comments
COMMENT ON VIEW company_usage_summary IS 'Summary of company resource usage vs limits';

-- Function comments
COMMENT ON FUNCTION set_tenant_context(UUID) IS 'Sets the current tenant context for RLS policies';
COMMENT ON FUNCTION get_current_tenant() IS 'Returns the currently set tenant ID';
COMMENT ON FUNCTION validate_tenant_access(UUID) IS 'Validates if a tenant is active and accessible';
COMMENT ON FUNCTION get_company_usage(UUID) IS 'Returns current resource usage for a company';

-- End of V13 Multi-Tenant Foundation migration