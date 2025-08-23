-- ============================================
-- V117__Create_WorkOrder_Supporting_Tables.sql
-- CAFM Backend - Create Work Order Supporting Tables
-- Purpose: Create task tracking, material consumption, and attachment management for work orders
-- Pattern: Clean Architecture with aggregate composition for work order entities
-- Java 23: Uses records for DTOs and sealed classes for task states
-- Architecture: Multi-tenant with detailed task breakdown and material tracking
-- Standards: Full audit trails with file management and cost tracking
-- ============================================

-- ============================================
-- STEP 1: CREATE WORK ORDER TASKS TABLE
-- ============================================

CREATE TABLE work_order_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    work_order_id UUID NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    
    -- Task Information
    title VARCHAR(255) NOT NULL,
    description TEXT,
    task_type VARCHAR(50) DEFAULT 'MANUAL', -- MANUAL, INSPECTION, TESTING, CALIBRATION
    
    -- Task Organization
    order_number INTEGER DEFAULT 0,
    is_mandatory BOOLEAN DEFAULT FALSE,
    prerequisite_task_ids UUID[] DEFAULT '{}', -- Array of task IDs that must be completed first
    
    -- Assignment and Status
    assigned_to_id UUID REFERENCES users(id),
    status task_status_enum DEFAULT 'PENDING',
    priority INTEGER DEFAULT 5 CHECK (priority >= 1 AND priority <= 10),
    
    -- Time Tracking
    estimated_duration_minutes INTEGER,
    actual_duration_minutes INTEGER,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    
    -- Cost Information
    estimated_labor_cost DECIMAL(10,2) DEFAULT 0,
    actual_labor_cost DECIMAL(10,2) DEFAULT 0,
    hourly_rate DECIMAL(8,2),
    
    -- Task Details
    instructions TEXT,
    safety_requirements TEXT,
    tools_required TEXT,
    completion_criteria TEXT,
    completion_notes TEXT,
    
    -- Quality Control
    requires_verification BOOLEAN DEFAULT FALSE,
    verified_by_id UUID REFERENCES users(id),
    verified_at TIMESTAMP WITH TIME ZONE,
    verification_notes TEXT,
    quality_score INTEGER CHECK (quality_score >= 1 AND quality_score <= 10),
    
    -- Progress Tracking
    progress_percentage INTEGER DEFAULT 0 CHECK (progress_percentage >= 0 AND progress_percentage <= 100),
    milestone_achieved BOOLEAN DEFAULT FALSE,
    
    -- Attachments and References
    reference_documents JSONB DEFAULT '[]',
    completion_photos JSONB DEFAULT '[]',
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID REFERENCES users(id),
    updated_by_id UUID REFERENCES users(id),
    
    -- Constraints
    CONSTRAINT chk_work_order_tasks_duration_positive CHECK (
        estimated_duration_minutes IS NULL OR estimated_duration_minutes > 0
    ),
    CONSTRAINT chk_work_order_tasks_actual_duration_positive CHECK (
        actual_duration_minutes IS NULL OR actual_duration_minutes > 0
    ),
    CONSTRAINT chk_work_order_tasks_costs_positive CHECK (
        estimated_labor_cost >= 0 AND actual_labor_cost >= 0
    ),
    CONSTRAINT chk_work_order_tasks_hourly_rate_positive CHECK (
        hourly_rate IS NULL OR hourly_rate > 0
    ),
    CONSTRAINT chk_work_order_tasks_completion_dates CHECK (
        completed_at IS NULL OR started_at IS NULL OR completed_at >= started_at
    ),
    CONSTRAINT chk_work_order_tasks_verification_dates CHECK (
        verified_at IS NULL OR completed_at IS NULL OR verified_at >= completed_at
    )
);

-- ============================================
-- STEP 2: CREATE WORK ORDER MATERIALS TABLE
-- ============================================

CREATE TABLE work_order_materials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    work_order_id UUID NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    work_order_task_id UUID REFERENCES work_order_tasks(id), -- Optional - link to specific task
    
    -- Material Information
    inventory_item_id UUID, -- Will reference inventory_items table once created
    material_name VARCHAR(255) NOT NULL,
    material_code VARCHAR(50),
    description TEXT,
    
    -- Quantity and Units
    estimated_quantity DECIMAL(10,3) NOT NULL,
    actual_quantity_used DECIMAL(10,3) DEFAULT 0,
    unit_of_measure unit_enum DEFAULT 'PIECE',
    
    -- Cost Information
    unit_cost DECIMAL(10,2),
    estimated_total_cost DECIMAL(12,2),
    actual_total_cost DECIMAL(12,2) DEFAULT 0,
    
    -- Material Properties
    material_category VARCHAR(100),
    material_grade VARCHAR(50),
    material_specifications JSONB DEFAULT '{}',
    
    -- Supplier Information
    supplier_name VARCHAR(255),
    supplier_part_number VARCHAR(100),
    purchase_order_number VARCHAR(50),
    
    -- Status and Tracking
    material_status VARCHAR(20) DEFAULT 'REQUIRED', -- REQUIRED, ORDERED, RECEIVED, ISSUED, USED
    requested_date DATE,
    required_date DATE,
    received_date DATE,
    issued_date DATE,
    
    -- Quality and Safety
    batch_number VARCHAR(50),
    expiry_date DATE,
    quality_certificate VARCHAR(255),
    safety_data_sheet VARCHAR(255),
    is_hazardous BOOLEAN DEFAULT FALSE,
    
    -- Waste and Returns
    waste_quantity DECIMAL(10,3) DEFAULT 0,
    returned_quantity DECIMAL(10,3) DEFAULT 0,
    waste_reason TEXT,
    return_reason TEXT,
    
    -- Requested by
    requested_by_id UUID REFERENCES users(id),
    approved_by_id UUID REFERENCES users(id),
    issued_by_id UUID REFERENCES users(id),
    
    -- Notes
    notes TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID REFERENCES users(id),
    updated_by_id UUID REFERENCES users(id),
    
    -- Constraints
    CONSTRAINT chk_work_order_materials_quantities_positive CHECK (
        estimated_quantity > 0 AND 
        actual_quantity_used >= 0 AND
        waste_quantity >= 0 AND
        returned_quantity >= 0
    ),
    CONSTRAINT chk_work_order_materials_costs_positive CHECK (
        unit_cost IS NULL OR unit_cost >= 0
    ),
    CONSTRAINT chk_work_order_materials_dates_logical CHECK (
        received_date IS NULL OR requested_date IS NULL OR received_date >= requested_date
    ),
    CONSTRAINT chk_work_order_materials_status CHECK (
        material_status IN ('REQUIRED', 'ORDERED', 'RECEIVED', 'ISSUED', 'USED', 'CANCELLED')
    )
);

-- ============================================
-- STEP 3: CREATE WORK ORDER ATTACHMENTS TABLE
-- ============================================

CREATE TABLE work_order_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    work_order_id UUID NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    work_order_task_id UUID REFERENCES work_order_tasks(id), -- Optional - link to specific task
    
    -- File Information
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_extension VARCHAR(10),
    
    -- File Classification
    attachment_type VARCHAR(50) NOT NULL, -- BEFORE_PHOTO, AFTER_PHOTO, DURING_PHOTO, DOCUMENT, DRAWING, MANUAL, CERTIFICATE, RECEIPT
    attachment_category VARCHAR(100), -- SAFETY, QUALITY, DOCUMENTATION, EVIDENCE, REFERENCE
    
    -- Content and Description
    title VARCHAR(255),
    description TEXT,
    tags VARCHAR(500), -- Comma-separated tags for searching
    
    -- Image/Photo specific
    image_width INTEGER,
    image_height INTEGER,
    camera_make VARCHAR(100),
    camera_model VARCHAR(100),
    gps_latitude DECIMAL(10,8),
    gps_longitude DECIMAL(11,8),
    taken_at TIMESTAMP WITH TIME ZONE,
    
    -- Document specific
    document_version VARCHAR(20),
    document_author VARCHAR(255),
    page_count INTEGER,
    
    -- Access and Security
    is_public BOOLEAN DEFAULT FALSE,
    requires_approval BOOLEAN DEFAULT FALSE,
    approved_by_id UUID REFERENCES users(id),
    approved_at TIMESTAMP WITH TIME ZONE,
    
    -- Processing Status
    is_processed BOOLEAN DEFAULT FALSE,
    processing_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    thumbnail_path TEXT,
    preview_path TEXT,
    
    -- Cloud Storage
    cloud_storage_url TEXT,
    cloud_storage_provider VARCHAR(50), -- MINIO, AWS_S3, AZURE_BLOB, GOOGLE_CLOUD
    cloud_file_id VARCHAR(255),
    
    -- Upload Information
    uploaded_by_id UUID NOT NULL REFERENCES users(id),
    upload_ip_address INET,
    user_agent TEXT,
    
    -- Virus Scanning
    virus_scan_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, CLEAN, INFECTED, FAILED
    virus_scan_result TEXT,
    virus_scan_date TIMESTAMP WITH TIME ZONE,
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    -- Constraints
    CONSTRAINT chk_work_order_attachments_file_size_positive CHECK (file_size > 0),
    CONSTRAINT chk_work_order_attachments_image_dimensions CHECK (
        (image_width IS NULL AND image_height IS NULL) OR 
        (image_width > 0 AND image_height > 0)
    ),
    CONSTRAINT chk_work_order_attachments_page_count_positive CHECK (
        page_count IS NULL OR page_count > 0
    ),
    CONSTRAINT chk_work_order_attachments_type CHECK (
        attachment_type IN ('BEFORE_PHOTO', 'AFTER_PHOTO', 'DURING_PHOTO', 'DOCUMENT', 'DRAWING', 
                           'MANUAL', 'CERTIFICATE', 'RECEIPT', 'INSPECTION_REPORT', 'WARRANTY', 'OTHER')
    ),
    CONSTRAINT chk_work_order_attachments_category CHECK (
        attachment_category IN ('SAFETY', 'QUALITY', 'DOCUMENTATION', 'EVIDENCE', 'REFERENCE', 'COMPLIANCE', 'OTHER')
    ),
    CONSTRAINT chk_work_order_attachments_processing_status CHECK (
        processing_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')
    ),
    CONSTRAINT chk_work_order_attachments_virus_scan_status CHECK (
        virus_scan_status IN ('PENDING', 'CLEAN', 'INFECTED', 'FAILED', 'SKIPPED')
    ),
    CONSTRAINT chk_work_order_attachments_cloud_provider CHECK (
        cloud_storage_provider IN ('MINIO', 'AWS_S3', 'AZURE_BLOB', 'GOOGLE_CLOUD', 'CLOUDINARY')
    )
);

-- ============================================
-- STEP 4: CREATE PERFORMANCE INDEXES
-- ============================================

-- Work Order Tasks Indexes
CREATE INDEX idx_work_order_tasks_work_order ON work_order_tasks(work_order_id);
CREATE INDEX idx_work_order_tasks_assigned_to ON work_order_tasks(assigned_to_id) WHERE assigned_to_id IS NOT NULL;
CREATE INDEX idx_work_order_tasks_status ON work_order_tasks(status);
CREATE INDEX idx_work_order_tasks_order ON work_order_tasks(work_order_id, order_number);
CREATE INDEX idx_work_order_tasks_pending ON work_order_tasks(work_order_id, status) WHERE status = 'PENDING';
CREATE INDEX idx_work_order_tasks_in_progress ON work_order_tasks(assigned_to_id, status) WHERE status = 'IN_PROGRESS';
CREATE INDEX idx_work_order_tasks_verification ON work_order_tasks(requires_verification, verified_by_id) 
    WHERE requires_verification = TRUE AND verified_by_id IS NULL;

-- Work Order Materials Indexes
CREATE INDEX idx_work_order_materials_work_order ON work_order_materials(work_order_id);
CREATE INDEX idx_work_order_materials_task ON work_order_materials(work_order_task_id) WHERE work_order_task_id IS NOT NULL;
CREATE INDEX idx_work_order_materials_inventory_item ON work_order_materials(inventory_item_id) WHERE inventory_item_id IS NOT NULL;
CREATE INDEX idx_work_order_materials_status ON work_order_materials(material_status);
CREATE INDEX idx_work_order_materials_requested_by ON work_order_materials(requested_by_id) WHERE requested_by_id IS NOT NULL;
CREATE INDEX idx_work_order_materials_required_date ON work_order_materials(required_date) WHERE required_date IS NOT NULL;
CREATE INDEX idx_work_order_materials_hazardous ON work_order_materials(is_hazardous) WHERE is_hazardous = TRUE;

-- Work Order Attachments Indexes
CREATE INDEX idx_work_order_attachments_work_order ON work_order_attachments(work_order_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_order_attachments_task ON work_order_attachments(work_order_task_id) WHERE work_order_task_id IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_work_order_attachments_type ON work_order_attachments(attachment_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_order_attachments_uploaded_by ON work_order_attachments(uploaded_by_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_order_attachments_mime_type ON work_order_attachments(mime_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_order_attachments_processing ON work_order_attachments(processing_status) WHERE processing_status != 'COMPLETED' AND deleted_at IS NULL;
CREATE INDEX idx_work_order_attachments_virus_scan ON work_order_attachments(virus_scan_status) WHERE virus_scan_status = 'PENDING' AND deleted_at IS NULL;

-- Full-text search indexes
CREATE INDEX idx_work_order_tasks_title_search ON work_order_tasks USING GIN(to_tsvector('english', title || ' ' || COALESCE(description, '')));
CREATE INDEX idx_work_order_materials_name_search ON work_order_materials USING GIN(to_tsvector('english', material_name || ' ' || COALESCE(description, '')));
CREATE INDEX idx_work_order_attachments_search ON work_order_attachments USING GIN(to_tsvector('english', title || ' ' || COALESCE(description, '') || ' ' || COALESCE(tags, ''))) WHERE deleted_at IS NULL;

-- JSONB indexes
CREATE INDEX idx_work_order_tasks_reference_docs ON work_order_tasks USING GIN(reference_documents);
CREATE INDEX idx_work_order_tasks_completion_photos ON work_order_tasks USING GIN(completion_photos);
CREATE INDEX idx_work_order_materials_specifications ON work_order_materials USING GIN(material_specifications);

-- ============================================
-- STEP 5: CREATE TRIGGERS AND FUNCTIONS
-- ============================================

-- Function to update work order task computed fields
CREATE OR REPLACE FUNCTION update_work_order_task_computed_fields()
RETURNS TRIGGER AS $$
BEGIN
    -- Calculate actual labor cost if hourly rate and duration are available
    IF NEW.hourly_rate IS NOT NULL AND NEW.actual_duration_minutes IS NOT NULL THEN
        NEW.actual_labor_cost := NEW.hourly_rate * (NEW.actual_duration_minutes / 60.0);
    END IF;
    
    -- Auto-set started_at when status changes to IN_PROGRESS
    IF NEW.status = 'IN_PROGRESS' AND (OLD.status != 'IN_PROGRESS' OR OLD.status IS NULL) AND NEW.started_at IS NULL THEN
        NEW.started_at := CURRENT_TIMESTAMP;
    END IF;
    
    -- Auto-set completed_at when status changes to COMPLETED
    IF NEW.status = 'COMPLETED' AND (OLD.status != 'COMPLETED' OR OLD.status IS NULL) AND NEW.completed_at IS NULL THEN
        NEW.completed_at := CURRENT_TIMESTAMP;
        NEW.progress_percentage := 100;
    END IF;
    
    -- Calculate actual duration if both timestamps are available
    IF NEW.started_at IS NOT NULL AND NEW.completed_at IS NOT NULL AND NEW.actual_duration_minutes IS NULL THEN
        NEW.actual_duration_minutes := EXTRACT(EPOCH FROM (NEW.completed_at - NEW.started_at)) / 60;
    END IF;
    
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for work order tasks
CREATE TRIGGER trg_work_order_tasks_computed_fields
    BEFORE UPDATE ON work_order_tasks
    FOR EACH ROW
    EXECUTE FUNCTION update_work_order_task_computed_fields();

-- Function to update work order material costs
CREATE OR REPLACE FUNCTION update_work_order_material_costs()
RETURNS TRIGGER AS $$
BEGIN
    -- Calculate estimated total cost
    IF NEW.unit_cost IS NOT NULL AND NEW.estimated_quantity IS NOT NULL THEN
        NEW.estimated_total_cost := NEW.unit_cost * NEW.estimated_quantity;
    END IF;
    
    -- Calculate actual total cost
    IF NEW.unit_cost IS NOT NULL AND NEW.actual_quantity_used IS NOT NULL THEN
        NEW.actual_total_cost := NEW.unit_cost * NEW.actual_quantity_used;
    END IF;
    
    -- Auto-set issued_date when status changes to ISSUED
    IF NEW.material_status = 'ISSUED' AND (OLD.material_status != 'ISSUED' OR OLD.material_status IS NULL) AND NEW.issued_date IS NULL THEN
        NEW.issued_date := CURRENT_DATE;
    END IF;
    
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for work order materials
CREATE TRIGGER trg_work_order_materials_costs
    BEFORE UPDATE ON work_order_materials
    FOR EACH ROW
    EXECUTE FUNCTION update_work_order_material_costs();

-- Function to update work order progress based on tasks
CREATE OR REPLACE FUNCTION update_work_order_progress()
RETURNS TRIGGER AS $$
DECLARE
    v_work_order_id UUID;
    v_total_tasks INTEGER;
    v_completed_tasks INTEGER;
    v_progress_percentage INTEGER;
    v_total_estimated_cost DECIMAL(12,2);
    v_total_actual_cost DECIMAL(12,2);
BEGIN
    -- Get work order ID from the task or material
    IF TG_TABLE_NAME = 'work_order_tasks' THEN
        v_work_order_id := COALESCE(NEW.work_order_id, OLD.work_order_id);
    ELSIF TG_TABLE_NAME = 'work_order_materials' THEN
        v_work_order_id := COALESCE(NEW.work_order_id, OLD.work_order_id);
    END IF;
    
    -- Calculate task progress
    SELECT 
        COUNT(*),
        COUNT(*) FILTER (WHERE status = 'COMPLETED'),
        CASE 
            WHEN COUNT(*) = 0 THEN 0
            ELSE ROUND((COUNT(*) FILTER (WHERE status = 'COMPLETED')::DECIMAL / COUNT(*)) * 100)::INTEGER
        END
    INTO v_total_tasks, v_completed_tasks, v_progress_percentage
    FROM work_order_tasks
    WHERE work_order_id = v_work_order_id;
    
    -- Calculate material costs
    SELECT 
        COALESCE(SUM(estimated_labor_cost), 0) + COALESCE(SUM(m.estimated_total_cost), 0),
        COALESCE(SUM(actual_labor_cost), 0) + COALESCE(SUM(m.actual_total_cost), 0)
    INTO v_total_estimated_cost, v_total_actual_cost
    FROM work_order_tasks t
    LEFT JOIN work_order_materials m ON t.work_order_id = m.work_order_id
    WHERE t.work_order_id = v_work_order_id;
    
    -- Update work order
    UPDATE work_orders
    SET 
        completion_percentage = v_progress_percentage,
        estimated_cost = GREATEST(v_total_estimated_cost, estimated_cost),
        labor_cost = COALESCE((SELECT SUM(actual_labor_cost) FROM work_order_tasks WHERE work_order_id = v_work_order_id), 0),
        material_cost = COALESCE((SELECT SUM(actual_total_cost) FROM work_order_materials WHERE work_order_id = v_work_order_id), 0),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = v_work_order_id;
    
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Create triggers to update work order progress
CREATE TRIGGER trg_work_order_tasks_progress
    AFTER INSERT OR UPDATE OR DELETE ON work_order_tasks
    FOR EACH ROW
    EXECUTE FUNCTION update_work_order_progress();

CREATE TRIGGER trg_work_order_materials_progress
    AFTER INSERT OR UPDATE OR DELETE ON work_order_materials
    FOR EACH ROW
    EXECUTE FUNCTION update_work_order_progress();

-- ============================================
-- STEP 6: UTILITY FUNCTIONS
-- ============================================

-- Function to get work order completion status
CREATE OR REPLACE FUNCTION get_work_order_completion_status(p_work_order_id UUID)
RETURNS TABLE (
    total_tasks INTEGER,
    completed_tasks INTEGER,
    pending_tasks INTEGER,
    in_progress_tasks INTEGER,
    progress_percentage DECIMAL(5,2),
    estimated_completion_date TIMESTAMP WITH TIME ZONE,
    total_estimated_cost DECIMAL(12,2),
    total_actual_cost DECIMAL(12,2)
) AS $$
BEGIN
    RETURN QUERY
    WITH task_summary AS (
        SELECT 
            COUNT(*) as total_tasks,
            COUNT(*) FILTER (WHERE status = 'COMPLETED') as completed_tasks,
            COUNT(*) FILTER (WHERE status = 'PENDING') as pending_tasks,
            COUNT(*) FILTER (WHERE status = 'IN_PROGRESS') as in_progress_tasks,
            SUM(estimated_labor_cost) as task_estimated_cost,
            SUM(actual_labor_cost) as task_actual_cost,
            CASE 
                WHEN COUNT(*) = 0 THEN 0
                ELSE (COUNT(*) FILTER (WHERE status = 'COMPLETED')::DECIMAL / COUNT(*)) * 100
            END as progress_pct
        FROM work_order_tasks
        WHERE work_order_id = p_work_order_id
    ),
    material_summary AS (
        SELECT 
            SUM(estimated_total_cost) as material_estimated_cost,
            SUM(actual_total_cost) as material_actual_cost
        FROM work_order_materials
        WHERE work_order_id = p_work_order_id
    )
    SELECT 
        ts.total_tasks,
        ts.completed_tasks,
        ts.pending_tasks,
        ts.in_progress_tasks,
        ts.progress_pct,
        NULL::TIMESTAMP WITH TIME ZONE, -- Would calculate based on task estimates
        COALESCE(ts.task_estimated_cost, 0) + COALESCE(ms.material_estimated_cost, 0),
        COALESCE(ts.task_actual_cost, 0) + COALESCE(ms.material_actual_cost, 0)
    FROM task_summary ts
    CROSS JOIN material_summary ms;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 7: COMMENTS FOR DOCUMENTATION
-- ============================================

-- Table comments
COMMENT ON TABLE work_order_tasks IS 'Individual tasks within work orders with progress tracking and verification';
COMMENT ON TABLE work_order_materials IS 'Materials and supplies required and used in work orders with cost tracking';
COMMENT ON TABLE work_order_attachments IS 'File attachments for work orders including photos, documents, and drawings';

-- Key column comments
COMMENT ON COLUMN work_order_tasks.prerequisite_task_ids IS 'Array of task IDs that must be completed before this task can start';
COMMENT ON COLUMN work_order_tasks.quality_score IS 'Quality assessment score from 1 (poor) to 10 (excellent)';
COMMENT ON COLUMN work_order_materials.waste_quantity IS 'Quantity of material wasted during the work';
COMMENT ON COLUMN work_order_attachments.virus_scan_status IS 'Status of virus scan for uploaded files';
COMMENT ON COLUMN work_order_attachments.gps_latitude IS 'GPS latitude where photo was taken (for mobile uploads)';

-- Function comments
COMMENT ON FUNCTION get_work_order_completion_status(UUID) IS 'Returns comprehensive completion status and cost information for a work order';
COMMENT ON FUNCTION update_work_order_progress() IS 'Trigger function to update work order progress based on task completion';

-- ============================================
-- VERIFICATION
-- ============================================

DO $$
BEGIN
    -- Verify all tables were created
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'work_order_tasks') THEN
        RAISE EXCEPTION 'Migration failed: work_order_tasks table was not created';
    END IF;
    
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'work_order_materials') THEN
        RAISE EXCEPTION 'Migration failed: work_order_materials table was not created';
    END IF;
    
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'work_order_attachments') THEN
        RAISE EXCEPTION 'Migration failed: work_order_attachments table was not created';
    END IF;
    
    RAISE NOTICE 'Migration V117 completed successfully - Work Order Supporting Tables created';
END $$;

-- End of V117 migration