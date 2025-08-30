-- ============================================
-- V117__Create_WorkOrder_Supporting_Tables.sql
-- CAFM Backend - Add Indexes and Functions for Work Order Tables
-- Purpose: Add performance indexes and utility functions for existing work order tables
-- Pattern: Clean Architecture with aggregate composition for work order entities
-- Java 23: Uses records for DTOs and sealed classes for task states
-- Architecture: Multi-tenant with detailed task breakdown and material tracking
-- Standards: Full audit trails with file management and cost tracking
-- ============================================

-- Note: Tables work_order_tasks, work_order_materials, and work_order_attachments already exist
-- This migration only adds missing indexes and utility functions

-- ============================================
-- STEP 1: CREATE PERFORMANCE INDEXES FOR EXISTING TABLES
-- ============================================

-- Work Order Tasks Indexes (using existing column names)
CREATE INDEX IF NOT EXISTS idx_work_order_tasks_work_order ON work_order_tasks(work_order_id);
CREATE INDEX IF NOT EXISTS idx_work_order_tasks_assigned_to ON work_order_tasks(assigned_to) WHERE assigned_to IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_work_order_tasks_status ON work_order_tasks(status);
CREATE INDEX IF NOT EXISTS idx_work_order_tasks_order ON work_order_tasks(work_order_id, task_number);
CREATE INDEX IF NOT EXISTS idx_work_order_tasks_pending ON work_order_tasks(work_order_id, status) WHERE status = 'pending';
CREATE INDEX IF NOT EXISTS idx_work_order_tasks_in_progress ON work_order_tasks(assigned_to, status) WHERE status = 'in_progress';
CREATE INDEX IF NOT EXISTS idx_work_order_tasks_completed ON work_order_tasks(completed_at) WHERE completed_at IS NOT NULL;

-- Work Order Materials Indexes (using existing column names)
CREATE INDEX IF NOT EXISTS idx_work_order_materials_work_order ON work_order_materials(work_order_id);
CREATE INDEX IF NOT EXISTS idx_work_order_materials_name ON work_order_materials(material_name);
CREATE INDEX IF NOT EXISTS idx_work_order_materials_code ON work_order_materials(material_code) WHERE material_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_work_order_materials_created_by ON work_order_materials(created_by) WHERE created_by IS NOT NULL;

-- Work Order Attachments Indexes (using existing column names)
CREATE INDEX IF NOT EXISTS idx_work_order_attachments_work_order ON work_order_attachments(work_order_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_work_order_attachments_type ON work_order_attachments(attachment_type) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_work_order_attachments_uploaded_by ON work_order_attachments(uploaded_by) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_work_order_attachments_file_type ON work_order_attachments(file_type) WHERE deleted_at IS NULL;

-- ============================================
-- STEP 2: CREATE UTILITY FUNCTIONS
-- ============================================

-- Function to calculate work order progress based on task completion
CREATE OR REPLACE FUNCTION calculate_work_order_progress(p_work_order_id UUID)
RETURNS TABLE (
    total_tasks INTEGER,
    completed_tasks INTEGER,
    progress_percentage DECIMAL(5,2),
    estimated_hours DECIMAL(10,2),
    actual_hours DECIMAL(10,2)
)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*)::INTEGER as total_tasks,
        COUNT(CASE WHEN completed_at IS NOT NULL THEN 1 END)::INTEGER as completed_tasks,
        CASE 
            WHEN COUNT(*) > 0 THEN 
                ROUND((COUNT(CASE WHEN completed_at IS NOT NULL THEN 1 END) * 100.0 / COUNT(*))::DECIMAL, 2)
            ELSE 0.00
        END as progress_percentage,
        COALESCE(SUM(estimated_hours), 0) as estimated_hours,
        COALESCE(SUM(actual_hours), 0) as actual_hours
    FROM work_order_tasks 
    WHERE work_order_id = p_work_order_id
    AND deleted_at IS NULL;
END;
$$;

-- Function to get work order cost summary
CREATE OR REPLACE FUNCTION get_work_order_cost_summary(p_work_order_id UUID)
RETURNS TABLE (
    labor_cost DECIMAL(10,2),
    material_cost DECIMAL(10,2),
    total_cost DECIMAL(10,2)
)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COALESCE((SELECT SUM(actual_hours * 50.0) FROM work_order_tasks WHERE work_order_id = p_work_order_id), 0) as labor_cost,
        COALESCE((SELECT SUM(total_cost) FROM work_order_materials WHERE work_order_id = p_work_order_id), 0) as material_cost,
        COALESCE((SELECT SUM(actual_hours * 50.0) FROM work_order_tasks WHERE work_order_id = p_work_order_id), 0) +
        COALESCE((SELECT SUM(total_cost) FROM work_order_materials WHERE work_order_id = p_work_order_id), 0) as total_cost;
END;
$$;

-- Function to check if work order can be completed
CREATE OR REPLACE FUNCTION can_complete_work_order(p_work_order_id UUID)
RETURNS BOOLEAN
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_pending_mandatory_tasks INTEGER;
BEGIN
    -- Check for pending mandatory tasks
    SELECT COUNT(*)
    INTO v_pending_mandatory_tasks
    FROM work_order_tasks
    WHERE work_order_id = p_work_order_id
    AND is_mandatory = TRUE
    AND completed_at IS NULL
    AND deleted_at IS NULL;
    
    -- Work order can be completed if no pending mandatory tasks
    RETURN v_pending_mandatory_tasks = 0;
END;
$$;

-- ============================================
-- STEP 3: CREATE NOTIFICATION TRIGGERS (Optional)
-- ============================================

-- Trigger function to update work order status when all tasks are completed
CREATE OR REPLACE FUNCTION update_work_order_on_task_completion()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_work_order_id UUID;
    v_pending_tasks INTEGER;
BEGIN
    -- Get work order ID from NEW or OLD record
    IF TG_OP = 'UPDATE' THEN
        v_work_order_id := COALESCE(NEW.work_order_id, OLD.work_order_id);
    ELSIF TG_OP = 'DELETE' THEN
        v_work_order_id := OLD.work_order_id;
    ELSE
        v_work_order_id := NEW.work_order_id;
    END IF;
    
    -- Count pending tasks for this work order
    SELECT COUNT(*)
    INTO v_pending_tasks
    FROM work_order_tasks
    WHERE work_order_id = v_work_order_id
    AND completed_at IS NULL
    AND deleted_at IS NULL;
    
    -- Update work order status if no pending tasks
    IF v_pending_tasks = 0 THEN
        UPDATE work_orders 
        SET status = 'COMPLETED',
            completed_at = CURRENT_TIMESTAMP
        WHERE id = v_work_order_id
        AND status != 'COMPLETED';
    END IF;
    
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$;

-- Only create trigger if it doesn't already exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger 
        WHERE tgname = 'trigger_update_work_order_on_task_completion'
    ) THEN
        CREATE TRIGGER trigger_update_work_order_on_task_completion
            AFTER INSERT OR UPDATE OR DELETE ON work_order_tasks
            FOR EACH ROW
            EXECUTE FUNCTION update_work_order_on_task_completion();
    END IF;
END;
$$;

-- ============================================
-- STEP 4: ADD HELPFUL COMMENTS
-- ============================================

COMMENT ON FUNCTION calculate_work_order_progress(UUID) IS 'Calculate completion progress for a work order based on task completion status';
COMMENT ON FUNCTION get_work_order_cost_summary(UUID) IS 'Get total labor and material costs for a work order';
COMMENT ON FUNCTION can_complete_work_order(UUID) IS 'Check if work order can be marked as completed (all mandatory tasks done)';
COMMENT ON FUNCTION update_work_order_on_task_completion() IS 'Trigger function to automatically update work order status when all tasks are completed';

-- Migration completed successfully
SELECT 'V117: Work Order supporting indexes and functions created successfully' as result;