-- Store view definitions before dropping them
CREATE TEMP TABLE view_definitions AS
SELECT viewname, definition 
FROM pg_views 
WHERE schemaname = 'public' 
AND definition LIKE '%companies%';

-- Drop all views that depend on the companies table
DROP VIEW IF EXISTS company_usage_summary CASCADE;
DROP VIEW IF EXISTS v_active_users CASCADE;
DROP VIEW IF EXISTS user_overview CASCADE;
DROP VIEW IF EXISTS v_admins CASCADE;
DROP VIEW IF EXISTS v_supervisors CASCADE;

-- Drop existing constraints that use the enums
ALTER TABLE companies ALTER COLUMN status TYPE VARCHAR(50);
ALTER TABLE companies ALTER COLUMN subscription_plan TYPE VARCHAR(50);

-- Update existing data to uppercase
UPDATE companies 
SET status = UPPER(status)
WHERE status IS NOT NULL;

UPDATE companies 
SET subscription_plan = UPPER(subscription_plan)
WHERE subscription_plan IS NOT NULL;

-- Convert 'pending_setup' to 'PENDING_SETUP'
UPDATE companies 
SET status = 'PENDING_SETUP' 
WHERE LOWER(status) = 'pending_setup';

-- Drop old enum types
DROP TYPE IF EXISTS company_status CASCADE;
DROP TYPE IF EXISTS subscription_plan CASCADE;

-- Recreate enum types with uppercase values to match Java enums
CREATE TYPE company_status AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'SUSPENDED',
    'TRIAL',
    'PENDING_SETUP'
);

CREATE TYPE subscription_plan AS ENUM (
    'FREE',
    'BASIC',
    'PROFESSIONAL',
    'ENTERPRISE'
);

-- Re-apply the enum types to columns
ALTER TABLE companies ALTER COLUMN status TYPE company_status USING status::company_status;
ALTER TABLE companies ALTER COLUMN subscription_plan TYPE subscription_plan USING subscription_plan::subscription_plan;

-- Recreate the company_usage_summary view
CREATE OR REPLACE VIEW company_usage_summary AS
SELECT 
    c.id AS company_id,
    c.name AS company_name,
    c.status,
    c.subscription_plan,
    c.max_users,
    c.max_schools,
    c.max_supervisors,
    c.max_technicians,
    COUNT(DISTINCT u.id) AS current_users,
    COUNT(DISTINCT s.id) AS current_schools,
    COUNT(DISTINCT CASE WHEN u.user_type = 'supervisor' THEN u.id END) AS current_supervisors,
    COUNT(DISTINCT CASE WHEN u.user_type = 'technician' THEN u.id END) AS current_technicians,
    c.created_at,
    c.updated_at
FROM companies c
LEFT JOIN users u ON c.id = u.company_id AND u.deleted_at IS NULL
LEFT JOIN schools s ON c.id = s.company_id AND s.deleted_at IS NULL
WHERE c.deleted_at IS NULL
GROUP BY c.id, c.name, c.status, c.subscription_plan, c.max_users, 
         c.max_schools, c.max_supervisors, c.max_technicians, c.created_at, c.updated_at;