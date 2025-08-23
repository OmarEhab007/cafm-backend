-- ============================================
-- V14__Database_Optimization_Phase1.sql
-- Priority 1: Complete Multi-tenancy & Add Core Business Tables
-- ============================================

-- ============================================
-- SECTION 1: COMPLETE MULTI-TENANCY
-- ============================================

-- Add company_id to remaining critical tables
ALTER TABLE maintenance_items ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies(id) ON DELETE CASCADE;
ALTER TABLE maintenance_reports ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies(id) ON DELETE CASCADE;
ALTER TABLE damage_items ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies(id) ON DELETE CASCADE;
ALTER TABLE file_uploads ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies(id) ON DELETE CASCADE;
ALTER TABLE survey_responses ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies(id) ON DELETE CASCADE;
ALTER TABLE supervisor_attendance ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies(id) ON DELETE CASCADE;
ALTER TABLE achievement_photos ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies(id) ON DELETE CASCADE;
ALTER TABLE school_achievements ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies(id) ON DELETE CASCADE;
ALTER TABLE user_fcm_tokens ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies(id) ON DELETE CASCADE;
ALTER TABLE analytics_summary ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies(id) ON DELETE CASCADE;
ALTER TABLE compliance_reports ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies(id) ON DELETE CASCADE;
ALTER TABLE maintenance_categories ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies(id) ON DELETE CASCADE;

-- Update existing records to default company
UPDATE maintenance_items SET company_id = '00000000-0000-0000-0000-000000000001' WHERE company_id IS NULL;
UPDATE maintenance_reports SET company_id = '00000000-0000-0000-0000-000000000001' WHERE company_id IS NULL;
UPDATE damage_items SET company_id = '00000000-0000-0000-0000-000000000001' WHERE company_id IS NULL;
UPDATE file_uploads SET company_id = '00000000-0000-0000-0000-000000000001' WHERE company_id IS NULL;
UPDATE survey_responses SET company_id = '00000000-0000-0000-0000-000000000001' WHERE company_id IS NULL;
UPDATE supervisor_attendance SET company_id = '00000000-0000-0000-0000-000000000001' WHERE company_id IS NULL;
UPDATE achievement_photos SET company_id = '00000000-0000-0000-0000-000000000001' WHERE company_id IS NULL;
UPDATE school_achievements SET company_id = '00000000-0000-0000-0000-000000000001' WHERE company_id IS NULL;
UPDATE user_fcm_tokens SET company_id = '00000000-0000-0000-0000-000000000001' WHERE company_id IS NULL;
UPDATE analytics_summary SET company_id = '00000000-0000-0000-0000-000000000001' WHERE company_id IS NULL;
UPDATE compliance_reports SET company_id = '00000000-0000-0000-0000-000000000001' WHERE company_id IS NULL;
UPDATE maintenance_categories SET company_id = '00000000-0000-0000-0000-000000000001' WHERE company_id IS NULL;

-- Make company_id NOT NULL after migration
ALTER TABLE maintenance_items ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE maintenance_reports ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE damage_items ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE file_uploads ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE survey_responses ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE supervisor_attendance ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE achievement_photos ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE school_achievements ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE user_fcm_tokens ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE analytics_summary ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE compliance_reports ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE maintenance_categories ALTER COLUMN company_id SET NOT NULL;

-- Enable RLS on these tables
ALTER TABLE maintenance_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE maintenance_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE damage_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE file_uploads ENABLE ROW LEVEL SECURITY;
ALTER TABLE survey_responses ENABLE ROW LEVEL SECURITY;
ALTER TABLE supervisor_attendance ENABLE ROW LEVEL SECURITY;
ALTER TABLE achievement_photos ENABLE ROW LEVEL SECURITY;
ALTER TABLE school_achievements ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_fcm_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE analytics_summary ENABLE ROW LEVEL SECURITY;
ALTER TABLE compliance_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE maintenance_categories ENABLE ROW LEVEL SECURITY;

-- Create RLS policies
CREATE POLICY tenant_isolation_maintenance_items ON maintenance_items
    FOR ALL USING (company_id = COALESCE(
        NULLIF(current_setting('app.current_company_id', true), '')::UUID,
        '00000000-0000-0000-0000-000000000001'::UUID
    ));

CREATE POLICY tenant_isolation_maintenance_reports ON maintenance_reports
    FOR ALL USING (company_id = COALESCE(
        NULLIF(current_setting('app.current_company_id', true), '')::UUID,
        '00000000-0000-0000-0000-000000000001'::UUID
    ));

CREATE POLICY tenant_isolation_file_uploads ON file_uploads
    FOR ALL USING (company_id = COALESCE(
        NULLIF(current_setting('app.current_company_id', true), '')::UUID,
        '00000000-0000-0000-0000-000000000001'::UUID
    ));

-- Add more policies for other tables
CREATE POLICY tenant_isolation_survey_responses ON survey_responses
    FOR ALL USING (company_id = COALESCE(
        NULLIF(current_setting('app.current_company_id', true), '')::UUID,
        '00000000-0000-0000-0000-000000000001'::UUID
    ));

-- ============================================
-- SECTION 2: WORK ORDER MANAGEMENT SYSTEM
-- ============================================

-- Create work order status enum
CREATE TYPE work_order_status AS ENUM (
    'draft', 'pending', 'assigned', 'in_progress', 
    'on_hold', 'completed', 'cancelled', 'verified'
);

-- Create work order priority enum
CREATE TYPE work_order_priority AS ENUM (
    'emergency', 'high', 'medium', 'low', 'scheduled'
);

-- Main work orders table
CREATE TABLE work_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    work_order_number VARCHAR(50) NOT NULL,
    report_id UUID REFERENCES reports(id),
    
    -- Assignment
    assigned_to UUID REFERENCES users(id),
    assigned_by UUID REFERENCES users(id),
    assignment_date TIMESTAMP WITH TIME ZONE,
    
    -- Work Details
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    priority work_order_priority DEFAULT 'medium',
    status work_order_status DEFAULT 'pending',
    
    -- Schedule
    scheduled_start TIMESTAMP WITH TIME ZONE,
    scheduled_end TIMESTAMP WITH TIME ZONE,
    actual_start TIMESTAMP WITH TIME ZONE,
    actual_end TIMESTAMP WITH TIME ZONE,
    
    -- Location
    school_id UUID REFERENCES schools(id),
    location_details TEXT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    
    -- Cost Tracking
    estimated_hours DECIMAL(10,2),
    actual_hours DECIMAL(10,2),
    labor_cost DECIMAL(10,2),
    material_cost DECIMAL(10,2),
    other_cost DECIMAL(10,2),
    total_cost DECIMAL(10,2) GENERATED ALWAYS AS (COALESCE(labor_cost, 0) + COALESCE(material_cost, 0) + COALESCE(other_cost, 0)) STORED,
    
    -- Completion
    completion_percentage INTEGER DEFAULT 0 CHECK (completion_percentage BETWEEN 0 AND 100),
    completion_notes TEXT,
    signature_url TEXT,
    verified_by UUID REFERENCES users(id),
    verified_at TIMESTAMP WITH TIME ZONE,
    
    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID REFERENCES users(id),
    
    CONSTRAINT unique_work_order_number_per_company UNIQUE(company_id, work_order_number),
    CONSTRAINT chk_work_order_dates CHECK (scheduled_end IS NULL OR scheduled_end >= scheduled_start),
    CONSTRAINT chk_work_order_actual_dates CHECK (actual_end IS NULL OR actual_end >= actual_start),
    CONSTRAINT chk_work_order_hours CHECK (actual_hours >= 0 AND estimated_hours >= 0),
    CONSTRAINT chk_work_order_costs CHECK (labor_cost >= 0 AND material_cost >= 0 AND other_cost >= 0)
);

-- Work order tasks/checklist
CREATE TABLE work_order_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    work_order_id UUID NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    task_number INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(30) DEFAULT 'pending',
    assigned_to UUID REFERENCES users(id),
    estimated_hours DECIMAL(10,2),
    actual_hours DECIMAL(10,2),
    is_mandatory BOOLEAN DEFAULT false,
    completed_at TIMESTAMP WITH TIME ZONE,
    completed_by UUID REFERENCES users(id),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_task_number_per_work_order UNIQUE(work_order_id, task_number)
);

-- Work order materials used
CREATE TABLE work_order_materials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    work_order_id UUID NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    material_name VARCHAR(255) NOT NULL,
    material_code VARCHAR(50),
    quantity DECIMAL(10,2) NOT NULL,
    unit_of_measure VARCHAR(50),
    unit_cost DECIMAL(10,2),
    total_cost DECIMAL(10,2) GENERATED ALWAYS AS (quantity * COALESCE(unit_cost, 0)) STORED,
    supplier VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Work order attachments
CREATE TABLE work_order_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    work_order_id UUID NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_url TEXT NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,
    attachment_type VARCHAR(50), -- 'before', 'during', 'after', 'invoice', 'report'
    description TEXT,
    uploaded_by UUID REFERENCES users(id),
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- SECTION 3: INVENTORY MANAGEMENT SYSTEM
-- ============================================

-- Inventory categories
CREATE TABLE inventory_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    name_ar VARCHAR(100),
    parent_category_id UUID REFERENCES inventory_categories(id),
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_category_name_per_company UNIQUE(company_id, name)
);

-- Main inventory items table
CREATE TABLE inventory_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    item_code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255),
    description TEXT,
    category_id UUID REFERENCES inventory_categories(id),
    
    -- Specifications
    brand VARCHAR(100),
    model VARCHAR(100),
    specifications JSONB,
    
    -- Units and measurement
    unit_of_measure VARCHAR(50) NOT NULL DEFAULT 'PIECE',
    
    -- Stock levels
    current_stock DECIMAL(10,2) DEFAULT 0 CHECK (current_stock >= 0),
    minimum_stock DECIMAL(10,2) DEFAULT 0 CHECK (minimum_stock >= 0),
    maximum_stock DECIMAL(10,2) CHECK (maximum_stock IS NULL OR maximum_stock >= minimum_stock),
    reorder_level DECIMAL(10,2) CHECK (reorder_level >= 0),
    reorder_quantity DECIMAL(10,2) CHECK (reorder_quantity > 0),
    
    -- Pricing
    average_cost DECIMAL(10,2) CHECK (average_cost >= 0),
    last_purchase_cost DECIMAL(10,2) CHECK (last_purchase_cost >= 0),
    selling_price DECIMAL(10,2) CHECK (selling_price >= 0),
    
    -- Location
    warehouse_location VARCHAR(100),
    bin_number VARCHAR(50),
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    is_trackable BOOLEAN DEFAULT true,
    
    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_item_code_per_company UNIQUE(company_id, item_code)
);

-- Inventory transactions
CREATE TYPE inventory_transaction_type AS ENUM (
    'receipt', 'issue', 'adjustment', 'transfer', 'return', 'damage', 'initial'
);

CREATE TABLE inventory_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    transaction_number VARCHAR(50) NOT NULL,
    item_id UUID NOT NULL REFERENCES inventory_items(id),
    transaction_type inventory_transaction_type NOT NULL,
    transaction_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Quantities
    quantity DECIMAL(10,2) NOT NULL,
    unit_cost DECIMAL(10,2) CHECK (unit_cost >= 0),
    total_cost DECIMAL(10,2) GENERATED ALWAYS AS (ABS(quantity) * COALESCE(unit_cost, 0)) STORED,
    
    -- Stock levels after transaction
    stock_before DECIMAL(10,2),
    stock_after DECIMAL(10,2),
    
    -- Reference
    reference_type VARCHAR(50), -- 'work_order', 'purchase_order', 'manual', 'transfer'
    reference_id UUID,
    work_order_id UUID REFERENCES work_orders(id),
    
    -- Transfer details (if applicable)
    from_location VARCHAR(100),
    to_location VARCHAR(100),
    
    -- Additional info
    supplier VARCHAR(255),
    invoice_number VARCHAR(50),
    notes TEXT,
    
    -- Audit
    created_by UUID REFERENCES users(id),
    approved_by UUID REFERENCES users(id),
    approved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_transaction_number_per_company UNIQUE(company_id, transaction_number)
);

-- ============================================
-- SECTION 4: ASSET MANAGEMENT SYSTEM
-- ============================================

-- Asset categories
CREATE TABLE asset_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    name_ar VARCHAR(100),
    depreciation_rate DECIMAL(5,2) DEFAULT 10 CHECK (depreciation_rate BETWEEN 0 AND 100),
    useful_life_years INTEGER CHECK (useful_life_years > 0),
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_asset_category_per_company UNIQUE(company_id, name)
);

-- Main assets table
CREATE TABLE assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    asset_code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255),
    description TEXT,
    category_id UUID REFERENCES asset_categories(id),
    
    -- Asset details
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    serial_number VARCHAR(100),
    barcode VARCHAR(100),
    
    -- Purchase information
    purchase_date DATE,
    purchase_order_number VARCHAR(50),
    supplier VARCHAR(255),
    warranty_start_date DATE,
    warranty_end_date DATE,
    
    -- Financial
    purchase_cost DECIMAL(10,2) CHECK (purchase_cost >= 0),
    current_value DECIMAL(10,2) CHECK (current_value >= 0),
    salvage_value DECIMAL(10,2) CHECK (salvage_value >= 0),
    depreciation_method VARCHAR(30) DEFAULT 'straight_line',
    
    -- Location & Assignment
    school_id UUID REFERENCES schools(id),
    department VARCHAR(100),
    location VARCHAR(255),
    assigned_to UUID REFERENCES users(id),
    assignment_date DATE,
    
    -- Maintenance
    last_maintenance_date DATE,
    next_maintenance_date DATE,
    maintenance_frequency_days INTEGER CHECK (maintenance_frequency_days > 0),
    total_maintenance_cost DECIMAL(10,2) DEFAULT 0,
    
    -- Status
    status VARCHAR(30) DEFAULT 'active', -- 'active', 'maintenance', 'retired', 'disposed', 'lost'
    condition VARCHAR(30) DEFAULT 'good', -- 'excellent', 'good', 'fair', 'poor', 'unusable'
    
    -- Disposal
    disposal_date DATE,
    disposal_method VARCHAR(50),
    disposal_value DECIMAL(10,2),
    disposal_reason TEXT,
    
    -- Metadata
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_asset_code_per_company UNIQUE(company_id, asset_code),
    CONSTRAINT chk_asset_warranty_dates CHECK (warranty_end_date IS NULL OR warranty_end_date >= warranty_start_date),
    CONSTRAINT chk_asset_values CHECK (current_value <= purchase_cost)
);

-- Asset maintenance history
CREATE TABLE asset_maintenance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    maintenance_date DATE NOT NULL,
    maintenance_type VARCHAR(50), -- 'preventive', 'corrective', 'inspection', 'calibration'
    
    -- Work performed
    description TEXT NOT NULL,
    performed_by UUID REFERENCES users(id),
    work_order_id UUID REFERENCES work_orders(id),
    
    -- Cost
    labor_hours DECIMAL(10,2) CHECK (labor_hours >= 0),
    labor_cost DECIMAL(10,2) CHECK (labor_cost >= 0),
    parts_cost DECIMAL(10,2) CHECK (parts_cost >= 0),
    external_cost DECIMAL(10,2) CHECK (external_cost >= 0),
    total_cost DECIMAL(10,2) GENERATED ALWAYS AS 
        (COALESCE(labor_cost, 0) + COALESCE(parts_cost, 0) + COALESCE(external_cost, 0)) STORED,
    
    -- Results
    condition_after VARCHAR(30),
    next_maintenance_date DATE,
    recommendations TEXT,
    
    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id)
);

-- ============================================
-- SECTION 5: CREATE INDEXES FOR PERFORMANCE
-- ============================================

-- Work Orders indexes
CREATE INDEX idx_work_orders_company_id ON work_orders(company_id);
CREATE INDEX idx_work_orders_status ON work_orders(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_orders_priority ON work_orders(priority) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_orders_assigned_to ON work_orders(assigned_to) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_orders_school_id ON work_orders(school_id);
CREATE INDEX idx_work_orders_scheduled_start ON work_orders(scheduled_start) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_orders_company_status_date ON work_orders(company_id, status, scheduled_start) WHERE deleted_at IS NULL;

-- Inventory indexes
CREATE INDEX idx_inventory_items_company_id ON inventory_items(company_id);
CREATE INDEX idx_inventory_items_category_id ON inventory_items(category_id);
CREATE INDEX idx_inventory_items_low_stock ON inventory_items(company_id, item_code) 
    WHERE current_stock <= minimum_stock AND is_active = true;
CREATE INDEX idx_inventory_transactions_company_id ON inventory_transactions(company_id);
CREATE INDEX idx_inventory_transactions_item_id ON inventory_transactions(item_id);
CREATE INDEX idx_inventory_transactions_date ON inventory_transactions(transaction_date);

-- Asset indexes
CREATE INDEX idx_assets_company_id ON assets(company_id);
CREATE INDEX idx_assets_school_id ON assets(school_id);
CREATE INDEX idx_assets_status ON assets(status) WHERE is_active = true;
CREATE INDEX idx_assets_assigned_to ON assets(assigned_to);
CREATE INDEX idx_assets_next_maintenance ON assets(next_maintenance_date) WHERE status = 'active';
CREATE INDEX idx_asset_maintenance_asset_id ON asset_maintenance(asset_id);
CREATE INDEX idx_asset_maintenance_date ON asset_maintenance(maintenance_date);

-- Enable RLS on new tables
ALTER TABLE work_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE work_order_tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE work_order_materials ENABLE ROW LEVEL SECURITY;
ALTER TABLE work_order_attachments ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE asset_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE assets ENABLE ROW LEVEL SECURITY;
ALTER TABLE asset_maintenance ENABLE ROW LEVEL SECURITY;

-- Create RLS policies for new tables
CREATE POLICY tenant_isolation_work_orders ON work_orders
    FOR ALL USING (company_id = COALESCE(
        NULLIF(current_setting('app.current_company_id', true), '')::UUID,
        '00000000-0000-0000-0000-000000000001'::UUID
    ));

CREATE POLICY tenant_isolation_inventory_items ON inventory_items
    FOR ALL USING (company_id = COALESCE(
        NULLIF(current_setting('app.current_company_id', true), '')::UUID,
        '00000000-0000-0000-0000-000000000001'::UUID
    ));

CREATE POLICY tenant_isolation_assets ON assets
    FOR ALL USING (company_id = COALESCE(
        NULLIF(current_setting('app.current_company_id', true), '')::UUID,
        '00000000-0000-0000-0000-000000000001'::UUID
    ));

-- ============================================
-- SECTION 6: CREATE AUDIT TRIGGER
-- ============================================

-- Generic audit trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to new tables
CREATE TRIGGER update_work_orders_updated_at BEFORE UPDATE ON work_orders 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_work_order_tasks_updated_at BEFORE UPDATE ON work_order_tasks 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_inventory_items_updated_at BEFORE UPDATE ON inventory_items 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_assets_updated_at BEFORE UPDATE ON assets 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- SECTION 7: ADD COMMENTS FOR DOCUMENTATION
-- ============================================

-- Work Orders
COMMENT ON TABLE work_orders IS 'Main work order tracking table for maintenance tasks';
COMMENT ON COLUMN work_orders.work_order_number IS 'Unique work order identifier within company';
COMMENT ON COLUMN work_orders.status IS 'Current status of the work order';
COMMENT ON COLUMN work_orders.priority IS 'Priority level for scheduling and resource allocation';

-- Inventory
COMMENT ON TABLE inventory_items IS 'Master inventory items with stock tracking';
COMMENT ON COLUMN inventory_items.current_stock IS 'Current available stock quantity';
COMMENT ON COLUMN inventory_items.reorder_level IS 'Stock level that triggers reorder alert';

-- Assets
COMMENT ON TABLE assets IS 'Company assets and equipment tracking';
COMMENT ON COLUMN assets.current_value IS 'Current book value after depreciation';
COMMENT ON COLUMN assets.next_maintenance_date IS 'Scheduled date for next maintenance';

-- End of V14 migration