-- V12: Create Admin Supervisor Assignment Table
-- Migration to create the admin_supervisors table for supervisor assignments to admins

-- Create admin_supervisors table
CREATE TABLE admin_supervisors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID NOT NULL,
    supervisor_id UUID NOT NULL,
    assigned_date DATE NOT NULL,
    end_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT true,
    region VARCHAR(100),
    authority_level INTEGER DEFAULT 3,
    max_schools_oversight INTEGER DEFAULT 50,
    supervisors_managed INTEGER DEFAULT 1,
    total_schools_covered INTEGER DEFAULT 0,
    technicians_covered INTEGER DEFAULT 0,
    efficiency_rating DOUBLE PRECISION,
    assignment_notes TEXT,
    assigned_by_user_id UUID,
    is_primary_admin BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

-- Add foreign key constraints
ALTER TABLE admin_supervisors 
    ADD CONSTRAINT fk_admin_supervisors_admin 
        FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_admin_supervisors_supervisor 
        FOREIGN KEY (supervisor_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_admin_supervisors_assigned_by 
        FOREIGN KEY (assigned_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_admin_supervisors_created_by 
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_admin_supervisors_updated_by 
        FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL;

-- Add check constraints
ALTER TABLE admin_supervisors
    ADD CONSTRAINT check_admin_assigned_date_valid 
        CHECK (assigned_date <= CURRENT_DATE),
    ADD CONSTRAINT check_admin_end_date_after_assigned 
        CHECK (end_date IS NULL OR end_date >= assigned_date),
    ADD CONSTRAINT check_admin_authority_level_valid 
        CHECK (authority_level BETWEEN 1 AND 5),
    ADD CONSTRAINT check_admin_max_schools_positive 
        CHECK (max_schools_oversight > 0),
    ADD CONSTRAINT check_admin_supervisors_managed_positive 
        CHECK (supervisors_managed >= 0),
    ADD CONSTRAINT check_admin_schools_covered_non_negative 
        CHECK (total_schools_covered >= 0),
    ADD CONSTRAINT check_admin_technicians_covered_non_negative 
        CHECK (technicians_covered >= 0),
    ADD CONSTRAINT check_admin_efficiency_rating_valid 
        CHECK (efficiency_rating IS NULL OR (efficiency_rating >= 0.0 AND efficiency_rating <= 100.0)),
    ADD CONSTRAINT check_admin_schools_within_max 
        CHECK (total_schools_covered <= max_schools_oversight);

-- Create unique constraint to prevent duplicate active primary admin assignments
CREATE UNIQUE INDEX idx_admin_supervisors_unique_primary_active 
    ON admin_supervisors (supervisor_id) 
    WHERE is_active = true AND is_primary_admin = true;

-- Create unique constraint to prevent duplicate active admin-supervisor pairs
CREATE UNIQUE INDEX idx_admin_supervisors_unique_pair_active 
    ON admin_supervisors (admin_id, supervisor_id) 
    WHERE is_active = true;

-- Create indexes for performance
CREATE INDEX idx_admin_supervisors_admin ON admin_supervisors (admin_id);
CREATE INDEX idx_admin_supervisors_supervisor ON admin_supervisors (supervisor_id);
CREATE INDEX idx_admin_supervisors_assigned_date ON admin_supervisors (assigned_date);
CREATE INDEX idx_admin_supervisors_active ON admin_supervisors (is_active) WHERE is_active = true;
CREATE INDEX idx_admin_supervisors_region ON admin_supervisors (region) WHERE region IS NOT NULL;
CREATE INDEX idx_admin_supervisors_authority_level ON admin_supervisors (authority_level);
CREATE INDEX idx_admin_supervisors_primary_admin ON admin_supervisors (is_primary_admin) WHERE is_primary_admin = true;

-- Composite indexes for common queries
CREATE INDEX idx_admin_supervisors_admin_active 
    ON admin_supervisors (admin_id, is_active) 
    WHERE is_active = true;

CREATE INDEX idx_admin_supervisors_region_active 
    ON admin_supervisors (region, is_active) 
    WHERE is_active = true AND region IS NOT NULL;

CREATE INDEX idx_admin_supervisors_authority_active 
    ON admin_supervisors (authority_level, is_active) 
    WHERE is_active = true;

CREATE INDEX idx_admin_supervisors_performance 
    ON admin_supervisors (total_schools_covered, technicians_covered, efficiency_rating)
    WHERE is_active = true;

-- Add updated_at trigger
CREATE OR REPLACE FUNCTION update_admin_supervisors_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_admin_supervisors_updated_at
    BEFORE UPDATE ON admin_supervisors
    FOR EACH ROW
    EXECUTE FUNCTION update_admin_supervisors_updated_at();

-- Add comments for documentation
COMMENT ON TABLE admin_supervisors IS 'Assignment relationships between admins and supervisors for hierarchical management';
COMMENT ON COLUMN admin_supervisors.admin_id IS 'Reference to admin user who manages the supervisor';
COMMENT ON COLUMN admin_supervisors.supervisor_id IS 'Reference to supervisor user being managed';
COMMENT ON COLUMN admin_supervisors.assigned_date IS 'Date when management assignment started';
COMMENT ON COLUMN admin_supervisors.end_date IS 'Date when management assignment ended (NULL if active)';
COMMENT ON COLUMN admin_supervisors.is_active IS 'Whether the management assignment is currently active';
COMMENT ON COLUMN admin_supervisors.region IS 'Geographic region or district managed';
COMMENT ON COLUMN admin_supervisors.authority_level IS 'Authority level 1-5 (1=highest authority, 5=lowest)';
COMMENT ON COLUMN admin_supervisors.max_schools_oversight IS 'Maximum number of schools this supervisor can oversee';
COMMENT ON COLUMN admin_supervisors.supervisors_managed IS 'Number of supervisors managed (including self)';
COMMENT ON COLUMN admin_supervisors.total_schools_covered IS 'Total number of schools covered by this supervisor';
COMMENT ON COLUMN admin_supervisors.technicians_covered IS 'Total number of technicians under this supervisor';
COMMENT ON COLUMN admin_supervisors.efficiency_rating IS 'Efficiency rating percentage (0-100)';
COMMENT ON COLUMN admin_supervisors.assignment_notes IS 'Notes about the management assignment';
COMMENT ON COLUMN admin_supervisors.assigned_by_user_id IS 'User who created this management assignment';
COMMENT ON COLUMN admin_supervisors.is_primary_admin IS 'Whether this admin is the primary manager for this supervisor';

-- Create a view for active admin-supervisor assignments with user details
CREATE OR REPLACE VIEW active_admin_supervisor_assignments AS
SELECT 
    aso.id,
    aso.admin_id,
    a.username as admin_username,
    a.full_name as admin_name,
    a.email as admin_email,
    aso.supervisor_id,
    s.username as supervisor_username,
    s.full_name as supervisor_name,
    s.email as supervisor_email,
    s.department as supervisor_department,
    aso.assigned_date,
    aso.region,
    aso.authority_level,
    aso.max_schools_oversight,
    aso.supervisors_managed,
    aso.total_schools_covered,
    aso.technicians_covered,
    aso.efficiency_rating,
    aso.is_primary_admin,
    -- Calculate coverage ratio
    CASE 
        WHEN aso.max_schools_oversight > 0 
        THEN ROUND((aso.total_schools_covered::DECIMAL / aso.max_schools_oversight::DECIMAL) * 100, 2)
        ELSE 0 
    END as coverage_percentage,
    -- Calculate technician to supervisor ratio
    CASE 
        WHEN aso.supervisors_managed > 0 
        THEN ROUND(aso.technicians_covered::DECIMAL / aso.supervisors_managed::DECIMAL, 2)
        ELSE 0 
    END as technician_per_supervisor_ratio,
    aso.assignment_notes,
    aso.created_at,
    aso.updated_at
FROM admin_supervisors aso
JOIN users a ON aso.admin_id = a.id
JOIN users s ON aso.supervisor_id = s.id
WHERE aso.is_active = true
    AND a.user_type IN ('admin', 'super_admin')
    AND s.user_type = 'supervisor'
    AND a.deleted_at IS NULL
    AND s.deleted_at IS NULL
ORDER BY aso.authority_level ASC, aso.assigned_date DESC;

COMMENT ON VIEW active_admin_supervisor_assignments IS 'View of active admin-supervisor assignments with user details and calculated metrics';

-- Create a view for admin workload summary
CREATE OR REPLACE VIEW admin_workload_summary AS
SELECT 
    a.id as admin_id,
    a.username as admin_username,
    a.full_name as admin_name,
    a.user_type as admin_type,
    COUNT(aso.id) as supervisors_managed,
    SUM(aso.total_schools_covered) as total_schools_overseen,
    SUM(aso.technicians_covered) as total_technicians_overseen,
    AVG(aso.efficiency_rating) as avg_efficiency_rating,
    COUNT(CASE WHEN aso.is_primary_admin = true THEN 1 END) as primary_assignments,
    COUNT(CASE WHEN aso.authority_level <= 2 THEN 1 END) as high_authority_assignments,
    STRING_AGG(DISTINCT aso.region, ', ') as regions_covered
FROM users a
LEFT JOIN admin_supervisors aso ON a.id = aso.admin_id AND aso.is_active = true
WHERE a.user_type IN ('admin', 'super_admin')
    AND a.deleted_at IS NULL
    AND a.is_active = true
GROUP BY a.id, a.username, a.full_name, a.user_type
ORDER BY supervisors_managed DESC NULLS LAST;

COMMENT ON VIEW admin_workload_summary IS 'Summary view of admin workload and management statistics';

-- Insert sample data (optional - for testing)
-- This will only work if there are existing admin and supervisor users
-- Uncomment the following lines if you want to create sample assignments

-- INSERT INTO admin_supervisors (
--     admin_id, 
--     supervisor_id, 
--     assigned_date, 
--     region,
--     authority_level,
--     max_schools_oversight,
--     assignment_notes
-- ) 
-- SELECT 
--     a.id as admin_id,
--     s.id as supervisor_id,
--     CURRENT_DATE as assigned_date,
--     CASE 
--         WHEN s.city IS NOT NULL THEN s.city || ' Region'
--         ELSE 'Central Region'
--     END as region,
--     CASE 
--         WHEN a.user_type = 'super_admin' THEN 1
--         WHEN a.user_type = 'admin' THEN 3
--         ELSE 5
--     END as authority_level,
--     CASE 
--         WHEN a.user_type = 'super_admin' THEN 100
--         WHEN a.user_type = 'admin' THEN 50
--         ELSE 25
--     END as max_schools_oversight,
--     'Initial assignment' as assignment_notes
-- FROM users a
-- CROSS JOIN users s
-- WHERE a.user_type IN ('admin', 'super_admin')
--     AND s.user_type = 'supervisor'
--     AND a.deleted_at IS NULL 
--     AND s.deleted_at IS NULL
--     AND a.is_active = true 
--     AND s.is_active = true
-- LIMIT 10; -- Create max 10 sample assignments