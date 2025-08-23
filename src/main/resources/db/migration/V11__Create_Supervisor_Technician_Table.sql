-- V11: Create Supervisor Technician Assignment Table
-- Migration to create the supervisor_technicians table for technician assignments

-- Create supervisor_technicians table
CREATE TABLE supervisor_technicians (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supervisor_id UUID NOT NULL,
    technician_id UUID NOT NULL,
    assigned_date DATE NOT NULL,
    end_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT true,
    primary_specialization VARCHAR(30),
    priority_level INTEGER DEFAULT 5,
    tasks_assigned INTEGER DEFAULT 0,
    tasks_completed INTEGER DEFAULT 0,
    avg_completion_time_hours DOUBLE PRECISION,
    assignment_notes TEXT,
    assigned_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

-- Add foreign key constraints
ALTER TABLE supervisor_technicians 
    ADD CONSTRAINT fk_supervisor_technicians_supervisor 
        FOREIGN KEY (supervisor_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_supervisor_technicians_technician 
        FOREIGN KEY (technician_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_supervisor_technicians_assigned_by 
        FOREIGN KEY (assigned_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_supervisor_technicians_created_by 
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_supervisor_technicians_updated_by 
        FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL;

-- Add check constraints
ALTER TABLE supervisor_technicians
    ADD CONSTRAINT check_assigned_date_valid 
        CHECK (assigned_date <= CURRENT_DATE),
    ADD CONSTRAINT check_end_date_after_assigned 
        CHECK (end_date IS NULL OR end_date >= assigned_date),
    ADD CONSTRAINT check_priority_level_valid 
        CHECK (priority_level BETWEEN 1 AND 10),
    ADD CONSTRAINT check_tasks_non_negative 
        CHECK (tasks_assigned >= 0 AND tasks_completed >= 0),
    ADD CONSTRAINT check_tasks_completed_not_exceed_assigned 
        CHECK (tasks_completed <= tasks_assigned),
    ADD CONSTRAINT check_avg_completion_time_positive 
        CHECK (avg_completion_time_hours IS NULL OR avg_completion_time_hours >= 0);

-- Add check constraint for specialization enum values
ALTER TABLE supervisor_technicians
    ADD CONSTRAINT check_primary_specialization_valid 
        CHECK (primary_specialization IS NULL OR primary_specialization IN (
            'ELECTRICAL', 'PLUMBING', 'HVAC', 'CARPENTRY', 'PAINTING',
            'GENERAL_MAINTENANCE', 'LANDSCAPING', 'CLEANING', 'IT_SUPPORT',
            'SECURITY_SYSTEMS', 'ROOFING', 'FLOORING', 'MASONRY',
            'FIRE_SAFETY', 'PEST_CONTROL'
        ));

-- Create unique constraint to prevent duplicate active assignments
CREATE UNIQUE INDEX idx_supervisor_technicians_unique_active 
    ON supervisor_technicians (technician_id) 
    WHERE is_active = true;

-- Create indexes for performance
CREATE INDEX idx_supervisor_technicians_supervisor ON supervisor_technicians (supervisor_id);
CREATE INDEX idx_supervisor_technicians_technician ON supervisor_technicians (technician_id);
CREATE INDEX idx_supervisor_technicians_assigned_date ON supervisor_technicians (assigned_date);
CREATE INDEX idx_supervisor_technicians_active ON supervisor_technicians (is_active) WHERE is_active = true;
CREATE INDEX idx_supervisor_technicians_specialization ON supervisor_technicians (primary_specialization) WHERE primary_specialization IS NOT NULL;
CREATE INDEX idx_supervisor_technicians_priority ON supervisor_technicians (priority_level);

-- Composite indexes for common queries
CREATE INDEX idx_supervisor_technicians_supervisor_active 
    ON supervisor_technicians (supervisor_id, is_active) 
    WHERE is_active = true;

CREATE INDEX idx_supervisor_technicians_specialization_active 
    ON supervisor_technicians (primary_specialization, is_active) 
    WHERE is_active = true AND primary_specialization IS NOT NULL;

CREATE INDEX idx_supervisor_technicians_performance 
    ON supervisor_technicians (tasks_assigned, tasks_completed, avg_completion_time_hours)
    WHERE is_active = true AND tasks_assigned > 0;

-- Add updated_at trigger
CREATE OR REPLACE FUNCTION update_supervisor_technicians_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_supervisor_technicians_updated_at
    BEFORE UPDATE ON supervisor_technicians
    FOR EACH ROW
    EXECUTE FUNCTION update_supervisor_technicians_updated_at();

-- Add comments for documentation
COMMENT ON TABLE supervisor_technicians IS 'Assignment relationships between supervisors and technicians';
COMMENT ON COLUMN supervisor_technicians.supervisor_id IS 'Reference to supervisor user';
COMMENT ON COLUMN supervisor_technicians.technician_id IS 'Reference to technician user';
COMMENT ON COLUMN supervisor_technicians.assigned_date IS 'Date when assignment started';
COMMENT ON COLUMN supervisor_technicians.end_date IS 'Date when assignment ended (NULL if active)';
COMMENT ON COLUMN supervisor_technicians.is_active IS 'Whether the assignment is currently active';
COMMENT ON COLUMN supervisor_technicians.primary_specialization IS 'Primary specialization for this assignment';
COMMENT ON COLUMN supervisor_technicians.priority_level IS 'Priority level 1-10 (1=highest priority)';
COMMENT ON COLUMN supervisor_technicians.tasks_assigned IS 'Number of tasks assigned to this technician';
COMMENT ON COLUMN supervisor_technicians.tasks_completed IS 'Number of tasks completed by this technician';
COMMENT ON COLUMN supervisor_technicians.avg_completion_time_hours IS 'Average time to complete tasks in hours';
COMMENT ON COLUMN supervisor_technicians.assignment_notes IS 'Notes about the assignment';
COMMENT ON COLUMN supervisor_technicians.assigned_by_user_id IS 'User who created this assignment';

-- Create a view for active assignments with user details
CREATE OR REPLACE VIEW active_supervisor_technician_assignments AS
SELECT 
    st.id,
    st.supervisor_id,
    s.username as supervisor_username,
    s.full_name as supervisor_name,
    s.email as supervisor_email,
    st.technician_id,
    t.username as technician_username,
    t.full_name as technician_name,
    t.email as technician_email,
    t.specialization as technician_specialization,
    t.skill_level as technician_skill_level,
    st.assigned_date,
    st.primary_specialization,
    st.priority_level,
    st.tasks_assigned,
    st.tasks_completed,
    CASE 
        WHEN st.tasks_assigned > 0 
        THEN ROUND((st.tasks_completed::DECIMAL / st.tasks_assigned::DECIMAL) * 100, 2)
        ELSE 0 
    END as completion_rate_percentage,
    st.avg_completion_time_hours,
    st.assignment_notes,
    st.created_at,
    st.updated_at
FROM supervisor_technicians st
JOIN users s ON st.supervisor_id = s.id
JOIN users t ON st.technician_id = t.id
WHERE st.is_active = true
    AND s.user_type = 'supervisor'
    AND t.user_type = 'technician'
    AND s.deleted_at IS NULL
    AND t.deleted_at IS NULL
ORDER BY st.priority_level ASC, st.assigned_date DESC;

COMMENT ON VIEW active_supervisor_technician_assignments IS 'View of active assignments with supervisor and technician details';

-- Insert sample data (optional - for testing)
-- This will only work if there are existing supervisor and technician users
-- Uncomment the following lines if you want to create sample assignments

-- INSERT INTO supervisor_technicians (
--     supervisor_id, 
--     technician_id, 
--     assigned_date, 
--     primary_specialization, 
--     priority_level,
--     assignment_notes
-- ) 
-- SELECT 
--     s.id as supervisor_id,
--     t.id as technician_id,
--     CURRENT_DATE as assigned_date,
--     COALESCE(t.specialization, 'GENERAL_MAINTENANCE') as primary_specialization,
--     3 as priority_level,
--     'Initial assignment' as assignment_notes
-- FROM users s
-- CROSS JOIN users t
-- WHERE s.user_type = 'supervisor' 
--     AND t.user_type = 'technician'
--     AND s.deleted_at IS NULL 
--     AND t.deleted_at IS NULL
--     AND s.is_active = true 
--     AND t.is_active = true
--     AND t.is_available_for_assignment = true
-- LIMIT 5; -- Create max 5 sample assignments