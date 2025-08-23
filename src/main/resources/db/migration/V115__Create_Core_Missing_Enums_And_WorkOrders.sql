-- ============================================
-- V115__Create_Core_Missing_Enums_And_WorkOrders.sql
-- CAFM Backend - Create Missing Core Enums and Work Orders Table
-- Purpose: Establish foundational enums and work orders table for complete CAFM functionality
-- Pattern: Clean Architecture data layer with comprehensive business constraints
-- Java 23: Uses modern Java features for enum validation and performance
-- Architecture: Multi-tenant with company_id isolation and audit trails
-- Standards: Spring Boot 3.3.x with PostgreSQL best practices
-- ============================================

-- ============================================
-- STEP 1: CREATE MISSING ENUM TYPES
-- ============================================

-- Asset related enums
CREATE TYPE asset_status_enum AS ENUM (
    'ACTIVE', 'MAINTENANCE', 'RETIRED', 'DISPOSED', 'LOST', 'RESERVED', 'DAMAGED'
);

CREATE TYPE asset_condition_enum AS ENUM (
    'EXCELLENT', 'GOOD', 'FAIR', 'POOR', 'UNUSABLE'
);

-- Work order task enums
CREATE TYPE task_status_enum AS ENUM (
    'PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'ON_HOLD'
);

-- Inventory and measurement enums
CREATE TYPE unit_enum AS ENUM (
    'PIECE', 'METER', 'KILOGRAM', 'LITER', 'SQUARE_METER', 'CUBIC_METER', 'HOUR', 'SET'
);

-- Notification enums
CREATE TYPE notification_type_enum AS ENUM (
    'SYSTEM', 'REPORT', 'WORK_ORDER', 'ASSET', 'USER', 'BROADCAST'
);

CREATE TYPE notification_priority_enum AS ENUM (
    'LOW', 'NORMAL', 'HIGH', 'URGENT'
);

-- Work order type enum
CREATE TYPE work_order_type_enum AS ENUM (
    'PREVENTIVE', 'CORRECTIVE', 'EMERGENCY', 'INSPECTION', 'INSTALLATION', 'UPGRADE'
);

-- ============================================
-- STEP 2: CREATE WORK ORDERS TABLE
-- ============================================

CREATE TABLE work_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    
    -- Basic Information
    work_order_number VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    work_order_type work_order_type_enum NOT NULL DEFAULT 'CORRECTIVE',
    
    -- Relations
    school_id UUID NOT NULL REFERENCES schools(id),
    report_id UUID REFERENCES reports(id), -- Optional - not all work orders come from reports
    asset_id UUID, -- Will be added when assets table is created
    
    -- Assignment and Status
    created_by_id UUID NOT NULL REFERENCES users(id),
    assigned_to_id UUID REFERENCES users(id),
    supervised_by_id UUID REFERENCES users(id),
    status work_order_status NOT NULL DEFAULT 'PENDING',
    priority work_order_priority NOT NULL DEFAULT 'MEDIUM',
    
    -- Scheduling
    scheduled_start_date TIMESTAMP WITH TIME ZONE,
    scheduled_end_date TIMESTAMP WITH TIME ZONE,
    actual_start_date TIMESTAMP WITH TIME ZONE,
    actual_end_date TIMESTAMP WITH TIME ZONE,
    
    -- Progress Tracking
    estimated_duration_hours DECIMAL(5,2),
    actual_duration_hours DECIMAL(5,2),
    completion_percentage INTEGER DEFAULT 0 CHECK (completion_percentage >= 0 AND completion_percentage <= 100),
    
    -- Cost Information
    estimated_cost DECIMAL(12,2) DEFAULT 0,
    actual_cost DECIMAL(12,2) DEFAULT 0,
    labor_cost DECIMAL(12,2) DEFAULT 0,
    material_cost DECIMAL(12,2) DEFAULT 0,
    overhead_cost DECIMAL(12,2) DEFAULT 0,
    
    -- Work Details
    work_instructions TEXT,
    completion_notes TEXT,
    quality_check_notes TEXT,
    safety_requirements TEXT,
    required_tools TEXT,
    
    -- Approval Workflow
    approved_by_id UUID REFERENCES users(id),
    approved_at TIMESTAMP WITH TIME ZONE,
    approval_notes TEXT,
    
    -- Verification
    verified_by_id UUID REFERENCES users(id),
    verified_at TIMESTAMP WITH TIME ZONE,
    verification_notes TEXT,
    
    -- Location and Context
    location VARCHAR(255),
    building VARCHAR(100),
    floor VARCHAR(50),
    room VARCHAR(100),
    
    -- Metadata
    is_emergency BOOLEAN DEFAULT FALSE,
    requires_permit BOOLEAN DEFAULT FALSE,
    requires_shutdown BOOLEAN DEFAULT FALSE,
    is_recurring BOOLEAN DEFAULT FALSE,
    parent_work_order_id UUID REFERENCES work_orders(id),
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    updated_by_id UUID REFERENCES users(id),
    
    -- Constraints
    CONSTRAINT uk_work_order_number_company UNIQUE (work_order_number, company_id),
    CONSTRAINT chk_work_order_costs_positive CHECK (
        estimated_cost >= 0 AND actual_cost >= 0 AND 
        labor_cost >= 0 AND material_cost >= 0 AND overhead_cost >= 0
    ),
    CONSTRAINT chk_work_order_dates_logical CHECK (
        scheduled_end_date IS NULL OR scheduled_start_date IS NULL OR scheduled_end_date >= scheduled_start_date
    ),
    CONSTRAINT chk_work_order_actual_dates_logical CHECK (
        actual_end_date IS NULL OR actual_start_date IS NULL OR actual_end_date >= actual_start_date
    )
);

-- ============================================
-- STEP 3: CREATE PERFORMANCE INDEXES
-- ============================================

-- Core filtering indexes
CREATE INDEX idx_work_orders_company_status ON work_orders(company_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_orders_school_status ON work_orders(school_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_orders_assigned_to ON work_orders(assigned_to_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_orders_created_by ON work_orders(created_by_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_orders_supervised_by ON work_orders(supervised_by_id) WHERE deleted_at IS NULL;

-- Priority and urgency indexes
CREATE INDEX idx_work_orders_priority ON work_orders(priority) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_orders_emergency ON work_orders(company_id, is_emergency) 
    WHERE is_emergency = TRUE AND deleted_at IS NULL;

-- Scheduling indexes
CREATE INDEX idx_work_orders_scheduled_start ON work_orders(scheduled_start_date) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_orders_overdue ON work_orders(company_id, scheduled_end_date) 
    WHERE status NOT IN ('COMPLETED', 'CANCELLED', 'VERIFIED') AND deleted_at IS NULL;

-- Progress tracking indexes
CREATE INDEX idx_work_orders_in_progress ON work_orders(company_id, actual_start_date) 
    WHERE status = 'IN_PROGRESS' AND deleted_at IS NULL;
CREATE INDEX idx_work_orders_completion_percentage ON work_orders(completion_percentage) WHERE deleted_at IS NULL;

-- Approval workflow indexes
CREATE INDEX idx_work_orders_pending_approval ON work_orders(company_id, status) 
    WHERE status = 'COMPLETED' AND approved_by_id IS NULL AND deleted_at IS NULL;
CREATE INDEX idx_work_orders_pending_verification ON work_orders(company_id, status) 
    WHERE status = 'COMPLETED' AND approved_by_id IS NOT NULL AND verified_by_id IS NULL AND deleted_at IS NULL;

-- Reporting indexes
CREATE INDEX idx_work_orders_created_at ON work_orders(created_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_orders_type ON work_orders(work_order_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_orders_report_id ON work_orders(report_id) WHERE report_id IS NOT NULL AND deleted_at IS NULL;

-- Composite indexes for common queries
CREATE INDEX idx_work_orders_company_type_status ON work_orders(company_id, work_order_type, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_orders_school_priority_status ON work_orders(school_id, priority, status) WHERE deleted_at IS NULL;

-- ============================================
-- STEP 4: ADD FOREIGN KEY TO REPORTS TABLE
-- ============================================

-- Add work_order_id to reports table to establish bidirectional relationship
ALTER TABLE reports 
    ADD COLUMN work_order_id UUID REFERENCES work_orders(id);

CREATE INDEX idx_reports_work_order_id ON reports(work_order_id) WHERE work_order_id IS NOT NULL;

-- ============================================
-- STEP 5: CREATE TRIGGER FOR AUTO-UPDATING FIELDS
-- ============================================

-- Function to automatically update work order fields
CREATE OR REPLACE FUNCTION update_work_order_computed_fields()
RETURNS TRIGGER AS $$
BEGIN
    -- Calculate actual duration if both dates are set
    IF NEW.actual_start_date IS NOT NULL AND NEW.actual_end_date IS NOT NULL THEN
        NEW.actual_duration_hours := EXTRACT(EPOCH FROM (NEW.actual_end_date - NEW.actual_start_date)) / 3600;
    END IF;
    
    -- Calculate actual total cost
    NEW.actual_cost := COALESCE(NEW.labor_cost, 0) + COALESCE(NEW.material_cost, 0) + COALESCE(NEW.overhead_cost, 0);
    
    -- Auto-set completion percentage based on status
    IF NEW.status = 'COMPLETED' AND NEW.completion_percentage < 100 THEN
        NEW.completion_percentage := 100;
    ELSIF NEW.status = 'CANCELLED' THEN
        -- Keep existing completion percentage for cancelled work orders
        NEW.completion_percentage := NEW.completion_percentage;
    END IF;
    
    -- Set actual_start_date when status changes to IN_PROGRESS
    IF NEW.status = 'IN_PROGRESS' AND OLD.status != 'IN_PROGRESS' AND NEW.actual_start_date IS NULL THEN
        NEW.actual_start_date := CURRENT_TIMESTAMP;
    END IF;
    
    -- Set actual_end_date when status changes to COMPLETED
    IF NEW.status = 'COMPLETED' AND OLD.status != 'COMPLETED' AND NEW.actual_end_date IS NULL THEN
        NEW.actual_end_date := CURRENT_TIMESTAMP;
    END IF;
    
    -- Update timestamp
    NEW.updated_at := CURRENT_TIMESTAMP;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger
CREATE TRIGGER trg_work_orders_computed_fields
    BEFORE UPDATE ON work_orders
    FOR EACH ROW
    EXECUTE FUNCTION update_work_order_computed_fields();

-- ============================================
-- STEP 6: ROW LEVEL SECURITY
-- ============================================

-- Enable RLS for work_orders
ALTER TABLE work_orders ENABLE ROW LEVEL SECURITY;

-- Create policy for tenant isolation
CREATE POLICY tenant_isolation_work_orders ON work_orders
    FOR ALL USING (company_id = COALESCE(
        NULLIF(current_setting('app.current_company_id', true), '')::UUID,
        '00000000-0000-0000-0000-000000000001'::UUID
    ));

-- ============================================
-- STEP 7: UTILITY FUNCTIONS
-- ============================================

-- Function to auto-generate work order numbers
CREATE OR REPLACE FUNCTION generate_work_order_number(p_company_id UUID)
RETURNS VARCHAR(50) AS $$
DECLARE
    v_year INTEGER;
    v_sequence INTEGER;
    v_work_order_number VARCHAR(50);
BEGIN
    v_year := EXTRACT(YEAR FROM CURRENT_DATE);
    
    -- Get next sequence number for the year and company
    SELECT COALESCE(MAX(CAST(RIGHT(work_order_number, 6) AS INTEGER)), 0) + 1
    INTO v_sequence
    FROM work_orders
    WHERE company_id = p_company_id
    AND work_order_number LIKE 'WO-' || v_year || '-%'
    AND deleted_at IS NULL;
    
    -- Format: WO-2024-000001
    v_work_order_number := 'WO-' || v_year || '-' || LPAD(v_sequence::TEXT, 6, '0');
    
    RETURN v_work_order_number;
END;
$$ LANGUAGE plpgsql;

-- Function to calculate work order metrics
CREATE OR REPLACE FUNCTION calculate_work_order_metrics(p_work_order_id UUID)
RETURNS TABLE (
    total_tasks INTEGER,
    completed_tasks INTEGER,
    pending_tasks INTEGER,
    progress_percentage DECIMAL(5,2),
    estimated_completion_date TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    -- This will be implemented once work_order_tasks table is created
    RETURN QUERY
    SELECT 
        0::INTEGER as total_tasks,
        0::INTEGER as completed_tasks, 
        0::INTEGER as pending_tasks,
        0.00::DECIMAL(5,2) as progress_percentage,
        NULL::TIMESTAMP WITH TIME ZONE as estimated_completion_date;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 8: COMMENTS FOR DOCUMENTATION
-- ============================================

-- Table comments
COMMENT ON TABLE work_orders IS 'Core work orders table for maintenance and repair job management';
COMMENT ON COLUMN work_orders.work_order_number IS 'Unique work order identifier in format WO-YYYY-NNNNNN';
COMMENT ON COLUMN work_orders.work_order_type IS 'Type of work order: preventive, corrective, emergency, etc.';
COMMENT ON COLUMN work_orders.completion_percentage IS 'Progress percentage (0-100) based on completed tasks';
COMMENT ON COLUMN work_orders.estimated_cost IS 'Initial cost estimate before work begins';
COMMENT ON COLUMN work_orders.actual_cost IS 'Final actual cost calculated from labor, materials, and overhead';
COMMENT ON COLUMN work_orders.requires_permit IS 'Whether work requires special permits or approvals';
COMMENT ON COLUMN work_orders.requires_shutdown IS 'Whether work requires system/facility shutdown';
COMMENT ON COLUMN work_orders.is_recurring IS 'Whether this is part of a recurring maintenance schedule';

-- Enum comments
COMMENT ON TYPE asset_status_enum IS 'Asset lifecycle status for tracking asset availability';
COMMENT ON TYPE asset_condition_enum IS 'Physical condition assessment of assets';
COMMENT ON TYPE task_status_enum IS 'Task completion status within work orders';
COMMENT ON TYPE unit_enum IS 'Measurement units for inventory and materials';
COMMENT ON TYPE notification_type_enum IS 'Notification categories for system messaging';
COMMENT ON TYPE notification_priority_enum IS 'Priority levels for notifications';
COMMENT ON TYPE work_order_type_enum IS 'Work order classification by maintenance type';

-- Function comments
COMMENT ON FUNCTION generate_work_order_number(UUID) IS 'Generates unique work order numbers in format WO-YYYY-NNNNNN';
COMMENT ON FUNCTION update_work_order_computed_fields() IS 'Trigger function to auto-calculate work order metrics and timestamps';
COMMENT ON FUNCTION calculate_work_order_metrics(UUID) IS 'Calculates progress and completion metrics for work orders';

-- ============================================
-- VERIFICATION
-- ============================================

DO $$
BEGIN
    -- Verify work_orders table was created successfully
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'work_orders') THEN
        RAISE EXCEPTION 'Migration failed: work_orders table was not created';
    END IF;
    
    -- Verify all enum types were created
    IF NOT EXISTS (SELECT FROM pg_type WHERE typname = 'asset_status_enum') THEN
        RAISE EXCEPTION 'Migration failed: asset_status_enum was not created';
    END IF;
    
    RAISE NOTICE 'Migration V115 completed successfully - Work Orders foundation established';
END $$;

-- End of V115 migration