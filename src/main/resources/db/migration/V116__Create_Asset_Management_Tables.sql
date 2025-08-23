-- ============================================
-- V116__Create_Asset_Management_Tables.sql
-- CAFM Backend - Create Complete Asset Management System
-- Purpose: Establish comprehensive asset tracking, categorization, and lifecycle management
-- Pattern: Clean Architecture with domain-driven design for asset entities
-- Java 23: Leverages records for DTOs and pattern matching for asset state transitions
-- Architecture: Multi-tenant with hierarchical asset categories and audit trails
-- Standards: Full CRUD with asset depreciation, maintenance scheduling, and location tracking
-- ============================================

-- ============================================
-- STEP 1: CREATE ASSET CATEGORIES TABLE
-- ============================================

CREATE TABLE asset_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    
    -- Category Information
    name VARCHAR(100) NOT NULL,
    name_ar VARCHAR(100),
    code VARCHAR(20) NOT NULL,
    description TEXT,
    
    -- Hierarchy Support
    parent_category_id UUID REFERENCES asset_categories(id),
    level INTEGER NOT NULL DEFAULT 1,
    path LTREE, -- Hierarchical path for efficient queries
    
    -- Category Attributes
    icon VARCHAR(100), -- Icon identifier for UI
    color VARCHAR(7), -- Hex color code for UI
    default_depreciation_rate DECIMAL(5,2), -- Default annual depreciation percentage
    default_useful_life_years INTEGER,
    
    -- Maintenance Configuration
    requires_preventive_maintenance BOOLEAN DEFAULT FALSE,
    default_maintenance_interval_days INTEGER,
    maintenance_checklist JSONB DEFAULT '[]',
    
    -- Category Settings
    is_depreciable BOOLEAN DEFAULT TRUE,
    is_trackable BOOLEAN DEFAULT TRUE,
    requires_serial_number BOOLEAN DEFAULT FALSE,
    requires_location BOOLEAN DEFAULT TRUE,
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID REFERENCES users(id),
    updated_by_id UUID REFERENCES users(id),
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    -- Constraints
    CONSTRAINT uk_asset_categories_code_company UNIQUE (code, company_id),
    CONSTRAINT uk_asset_categories_name_company UNIQUE (name, company_id),
    CONSTRAINT chk_asset_categories_depreciation_rate CHECK (
        default_depreciation_rate IS NULL OR (default_depreciation_rate >= 0 AND default_depreciation_rate <= 100)
    ),
    CONSTRAINT chk_asset_categories_useful_life CHECK (
        default_useful_life_years IS NULL OR default_useful_life_years > 0
    ),
    CONSTRAINT chk_asset_categories_maintenance_interval CHECK (
        default_maintenance_interval_days IS NULL OR default_maintenance_interval_days > 0
    ),
    CONSTRAINT chk_asset_categories_level CHECK (level > 0 AND level <= 10)
);

-- ============================================
-- STEP 2: CREATE ASSETS TABLE
-- ============================================

CREATE TABLE assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    
    -- Basic Asset Information
    asset_code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255),
    description TEXT,
    category_id UUID NOT NULL REFERENCES asset_categories(id),
    
    -- Physical Attributes
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    serial_number VARCHAR(100),
    barcode VARCHAR(100),
    qr_code VARCHAR(255),
    
    -- Location and Assignment
    school_id UUID REFERENCES schools(id),
    location VARCHAR(255),
    building VARCHAR(100),
    floor VARCHAR(50),
    room VARCHAR(100),
    assigned_to_id UUID REFERENCES users(id),
    
    -- Financial Information
    purchase_date DATE,
    purchase_cost DECIMAL(12,2),
    current_value DECIMAL(12,2),
    salvage_value DECIMAL(12,2),
    depreciation_rate DECIMAL(5,2), -- Annual depreciation percentage
    depreciation_method VARCHAR(20) DEFAULT 'STRAIGHT_LINE', -- STRAIGHT_LINE, DECLINING_BALANCE
    accumulated_depreciation DECIMAL(12,2) DEFAULT 0,
    
    -- Warranty and Support
    warranty_start_date DATE,
    warranty_end_date DATE,
    warranty_provider VARCHAR(100),
    support_contract VARCHAR(100),
    support_contact VARCHAR(255),
    
    -- Status and Condition
    status asset_status_enum NOT NULL DEFAULT 'ACTIVE',
    condition asset_condition_enum NOT NULL DEFAULT 'GOOD',
    condition_notes TEXT,
    last_inspection_date DATE,
    next_inspection_date DATE,
    
    -- Maintenance Information
    maintenance_frequency_days INTEGER DEFAULT 90,
    next_maintenance_date DATE,
    last_maintenance_date DATE,
    total_maintenance_cost DECIMAL(12,2) DEFAULT 0,
    maintenance_notes TEXT,
    
    -- Usage Tracking
    installation_date DATE,
    commissioning_date DATE,
    last_used_date DATE,
    operating_hours DECIMAL(10,2) DEFAULT 0,
    usage_frequency VARCHAR(20), -- DAILY, WEEKLY, MONTHLY, SEASONAL, RARELY
    
    -- Technical Specifications
    specifications JSONB DEFAULT '{}',
    technical_documents JSONB DEFAULT '[]', -- Array of document references
    energy_rating VARCHAR(20),
    power_consumption DECIMAL(8,2), -- in watts
    weight_kg DECIMAL(8,2),
    dimensions JSONB DEFAULT '{}', -- {length, width, height}
    
    -- Risk and Criticality
    criticality_level VARCHAR(20) DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, CRITICAL
    risk_score INTEGER CHECK (risk_score >= 1 AND risk_score <= 10),
    safety_requirements TEXT,
    environmental_impact VARCHAR(20), -- LOW, MEDIUM, HIGH
    
    -- Parent-Child Relationships
    parent_asset_id UUID REFERENCES assets(id),
    is_component BOOLEAN DEFAULT FALSE,
    component_path LTREE,
    
    -- External References
    external_asset_id VARCHAR(100), -- Reference to external systems
    vendor_id VARCHAR(100),
    purchase_order_number VARCHAR(50),
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID REFERENCES users(id),
    updated_by_id UUID REFERENCES users(id),
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    -- Constraints
    CONSTRAINT uk_assets_code_company UNIQUE (asset_code, company_id),
    CONSTRAINT uk_assets_serial_company UNIQUE (serial_number, company_id, manufacturer) 
        DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT chk_assets_purchase_cost_positive CHECK (purchase_cost IS NULL OR purchase_cost >= 0),
    CONSTRAINT chk_assets_current_value_positive CHECK (current_value IS NULL OR current_value >= 0),
    CONSTRAINT chk_assets_salvage_value_positive CHECK (salvage_value IS NULL OR salvage_value >= 0),
    CONSTRAINT chk_assets_depreciation_rate CHECK (
        depreciation_rate IS NULL OR (depreciation_rate >= 0 AND depreciation_rate <= 100)
    ),
    CONSTRAINT chk_assets_maintenance_frequency CHECK (
        maintenance_frequency_days IS NULL OR maintenance_frequency_days > 0
    ),
    CONSTRAINT chk_assets_warranty_dates CHECK (
        warranty_end_date IS NULL OR warranty_start_date IS NULL OR warranty_end_date >= warranty_start_date
    ),
    CONSTRAINT chk_assets_depreciation_method CHECK (
        depreciation_method IN ('STRAIGHT_LINE', 'DECLINING_BALANCE', 'UNITS_OF_PRODUCTION')
    ),
    CONSTRAINT chk_assets_criticality_level CHECK (
        criticality_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
    ),
    CONSTRAINT chk_assets_usage_frequency CHECK (
        usage_frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'SEASONAL', 'RARELY')
    ),
    CONSTRAINT chk_assets_environmental_impact CHECK (
        environmental_impact IN ('LOW', 'MEDIUM', 'HIGH')
    )
);

-- ============================================
-- STEP 3: ADD ASSET_ID TO WORK_ORDERS
-- ============================================

-- Add asset foreign key to work_orders table
ALTER TABLE work_orders 
    ADD CONSTRAINT fk_work_orders_asset_id 
    FOREIGN KEY (asset_id) REFERENCES assets(id);

CREATE INDEX idx_work_orders_asset_id ON work_orders(asset_id) WHERE deleted_at IS NULL;

-- ============================================
-- STEP 4: CREATE PERFORMANCE INDEXES
-- ============================================

-- Asset Categories Indexes
CREATE INDEX idx_asset_categories_company_active ON asset_categories(company_id, is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_asset_categories_parent ON asset_categories(parent_category_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_asset_categories_path ON asset_categories USING GIST(path) WHERE deleted_at IS NULL;
CREATE INDEX idx_asset_categories_code ON asset_categories(code) WHERE deleted_at IS NULL;

-- Asset Core Indexes
CREATE INDEX idx_assets_company_status ON assets(company_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_assets_school_category ON assets(school_id, category_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_assets_assigned_to ON assets(assigned_to_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_assets_category_id ON assets(category_id) WHERE deleted_at IS NULL;

-- Asset Maintenance Indexes
CREATE INDEX idx_assets_maintenance_due ON assets(company_id, next_maintenance_date) 
    WHERE status = 'ACTIVE' AND next_maintenance_date IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_assets_last_maintenance ON assets(last_maintenance_date) WHERE deleted_at IS NULL;

-- Asset Warranty and Inspection Indexes
CREATE INDEX idx_assets_warranty_expiring ON assets(warranty_end_date) 
    WHERE warranty_end_date >= CURRENT_DATE AND deleted_at IS NULL;
CREATE INDEX idx_assets_inspection_due ON assets(next_inspection_date) 
    WHERE next_inspection_date <= CURRENT_DATE + INTERVAL '30 days' AND deleted_at IS NULL;

-- Asset Location Indexes
CREATE INDEX idx_assets_location ON assets(school_id, building, floor) WHERE deleted_at IS NULL;
CREATE INDEX idx_assets_barcode ON assets(barcode) WHERE barcode IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_assets_serial_number ON assets(serial_number) WHERE serial_number IS NOT NULL AND deleted_at IS NULL;

-- Asset Financial Indexes
CREATE INDEX idx_assets_purchase_date ON assets(purchase_date) WHERE deleted_at IS NULL;
CREATE INDEX idx_assets_criticality ON assets(criticality_level) WHERE deleted_at IS NULL;

-- Asset Hierarchy Indexes
CREATE INDEX idx_assets_parent ON assets(parent_asset_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_assets_component_path ON assets USING GIST(component_path) WHERE deleted_at IS NULL;

-- Composite Indexes for Common Queries
CREATE INDEX idx_assets_company_status_category ON assets(company_id, status, category_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_assets_school_status_condition ON assets(school_id, status, condition) WHERE deleted_at IS NULL;

-- JSONB Indexes for Specifications and Documents
CREATE INDEX idx_assets_specifications ON assets USING GIN(specifications);
CREATE INDEX idx_assets_technical_documents ON assets USING GIN(technical_documents);
CREATE INDEX idx_assets_dimensions ON assets USING GIN(dimensions);

-- ============================================
-- STEP 5: CREATE LTREE EXTENSION AND FUNCTIONS
-- ============================================

-- Enable LTREE extension for hierarchical data
CREATE EXTENSION IF NOT EXISTS ltree;

-- Function to maintain category hierarchy path
CREATE OR REPLACE FUNCTION update_asset_category_path()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.parent_category_id IS NULL THEN
        NEW.path := NEW.id::TEXT::LTREE;
        NEW.level := 1;
    ELSE
        SELECT path || NEW.id::TEXT, level + 1
        INTO NEW.path, NEW.level
        FROM asset_categories
        WHERE id = NEW.parent_category_id;
    END IF;
    
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for asset category path maintenance
CREATE TRIGGER trg_asset_categories_path
    BEFORE INSERT OR UPDATE ON asset_categories
    FOR EACH ROW
    EXECUTE FUNCTION update_asset_category_path();

-- Function to maintain asset component hierarchy
CREATE OR REPLACE FUNCTION update_asset_component_path()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.parent_asset_id IS NULL THEN
        NEW.component_path := NEW.id::TEXT::LTREE;
    ELSE
        SELECT component_path || NEW.id::TEXT
        INTO NEW.component_path
        FROM assets
        WHERE id = NEW.parent_asset_id;
        
        NEW.is_component := TRUE;
    END IF;
    
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for asset component path maintenance
CREATE TRIGGER trg_assets_component_path
    BEFORE INSERT OR UPDATE ON assets
    FOR EACH ROW
    EXECUTE FUNCTION update_asset_component_path();

-- ============================================
-- STEP 6: DEPRECIATION AND MAINTENANCE FUNCTIONS
-- ============================================

-- Function to calculate asset depreciation
CREATE OR REPLACE FUNCTION calculate_asset_depreciation(p_asset_id UUID)
RETURNS DECIMAL(12,2) AS $$
DECLARE
    v_asset RECORD;
    v_years_elapsed DECIMAL(10,4);
    v_annual_depreciation DECIMAL(12,2);
    v_total_depreciation DECIMAL(12,2);
BEGIN
    SELECT 
        purchase_cost, salvage_value, depreciation_rate, 
        purchase_date, depreciation_method
    INTO v_asset
    FROM assets
    WHERE id = p_asset_id;
    
    IF v_asset IS NULL OR v_asset.purchase_cost IS NULL OR v_asset.purchase_date IS NULL THEN
        RETURN 0;
    END IF;
    
    -- Calculate years elapsed since purchase
    v_years_elapsed := EXTRACT(EPOCH FROM (CURRENT_DATE - v_asset.purchase_date)) / (365.25 * 24 * 3600);
    
    -- Calculate depreciation based on method
    CASE v_asset.depreciation_method
        WHEN 'STRAIGHT_LINE' THEN
            v_annual_depreciation := (v_asset.purchase_cost - COALESCE(v_asset.salvage_value, 0)) * 
                                   (v_asset.depreciation_rate / 100);
            v_total_depreciation := v_annual_depreciation * v_years_elapsed;
            
        WHEN 'DECLINING_BALANCE' THEN
            v_total_depreciation := v_asset.purchase_cost * 
                                  (1 - POWER(1 - (v_asset.depreciation_rate / 100), v_years_elapsed));
            
        ELSE
            v_total_depreciation := 0;
    END CASE;
    
    -- Ensure depreciation doesn't exceed (purchase_cost - salvage_value)
    v_total_depreciation := LEAST(
        v_total_depreciation,
        v_asset.purchase_cost - COALESCE(v_asset.salvage_value, 0)
    );
    
    RETURN GREATEST(v_total_depreciation, 0);
END;
$$ LANGUAGE plpgsql;

-- Function to update all asset depreciation values
CREATE OR REPLACE FUNCTION update_asset_depreciation()
RETURNS INTEGER AS $$
DECLARE
    v_count INTEGER := 0;
    v_asset_id UUID;
    v_depreciation DECIMAL(12,2);
BEGIN
    FOR v_asset_id IN 
        SELECT id FROM assets 
        WHERE purchase_cost IS NOT NULL 
        AND purchase_date IS NOT NULL 
        AND status NOT IN ('DISPOSED', 'LOST')
        AND deleted_at IS NULL
    LOOP
        v_depreciation := calculate_asset_depreciation(v_asset_id);
        
        UPDATE assets 
        SET 
            accumulated_depreciation = v_depreciation,
            current_value = GREATEST(purchase_cost - v_depreciation, COALESCE(salvage_value, 0)),
            updated_at = CURRENT_TIMESTAMP
        WHERE id = v_asset_id;
        
        v_count := v_count + 1;
    END LOOP;
    
    RETURN v_count;
END;
$$ LANGUAGE plpgsql;

-- Function to generate asset code
CREATE OR REPLACE FUNCTION generate_asset_code(p_company_id UUID, p_category_id UUID)
RETURNS VARCHAR(50) AS $$
DECLARE
    v_category_code VARCHAR(20);
    v_sequence INTEGER;
    v_asset_code VARCHAR(50);
BEGIN
    -- Get category code
    SELECT code INTO v_category_code
    FROM asset_categories
    WHERE id = p_category_id;
    
    IF v_category_code IS NULL THEN
        v_category_code := 'AST';
    END IF;
    
    -- Get next sequence number for the category and company
    SELECT COALESCE(MAX(CAST(RIGHT(asset_code, 6) AS INTEGER)), 0) + 1
    INTO v_sequence
    FROM assets
    WHERE company_id = p_company_id
    AND asset_code LIKE v_category_code || '-%'
    AND deleted_at IS NULL;
    
    -- Format: CAT-000001
    v_asset_code := v_category_code || '-' || LPAD(v_sequence::TEXT, 6, '0');
    
    RETURN v_asset_code;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 7: ROW LEVEL SECURITY
-- ============================================

-- Enable RLS for asset tables
ALTER TABLE asset_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE assets ENABLE ROW LEVEL SECURITY;

-- Create policies for tenant isolation
CREATE POLICY tenant_isolation_asset_categories ON asset_categories
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
-- STEP 8: CREATE DEFAULT ASSET CATEGORIES
-- ============================================

-- Insert default asset categories for system company
INSERT INTO asset_categories (
    company_id, name, name_ar, code, description, 
    default_depreciation_rate, default_useful_life_years,
    requires_preventive_maintenance, default_maintenance_interval_days,
    created_by_id
) VALUES 
(
    '00000000-0000-0000-0000-000000000001'::UUID,
    'HVAC Systems', 'أنظمة التكييف', 'HVAC', 
    'Heating, Ventilation, and Air Conditioning Systems',
    10.0, 15, TRUE, 90,
    (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
),
(
    '00000000-0000-0000-0000-000000000001'::UUID,
    'Electrical Systems', 'الأنظمة الكهربائية', 'ELEC', 
    'Electrical panels, outlets, lighting systems',
    8.0, 20, TRUE, 180,
    (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
),
(
    '00000000-0000-0000-0000-000000000001'::UUID,
    'Plumbing Systems', 'أنظمة السباكة', 'PLMB', 
    'Water supply, drainage, and plumbing fixtures',
    5.0, 25, TRUE, 365,
    (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
),
(
    '00000000-0000-0000-0000-000000000001'::UUID,
    'Safety Equipment', 'معدات الأمان', 'SAFE', 
    'Fire safety, security systems, emergency equipment',
    12.0, 10, TRUE, 30,
    (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
),
(
    '00000000-0000-0000-0000-000000000001'::UUID,
    'Furniture & Fixtures', 'الأثاث والتجهيزات', 'FURN', 
    'Desks, chairs, cabinets, and other furniture',
    10.0, 10, FALSE, NULL,
    (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
);

-- ============================================
-- STEP 9: COMMENTS FOR DOCUMENTATION
-- ============================================

-- Table comments
COMMENT ON TABLE asset_categories IS 'Hierarchical asset category system with maintenance and depreciation settings';
COMMENT ON TABLE assets IS 'Comprehensive asset management with lifecycle tracking, depreciation, and maintenance scheduling';

-- Column comments for asset_categories
COMMENT ON COLUMN asset_categories.path IS 'LTREE path for hierarchical queries and organization';
COMMENT ON COLUMN asset_categories.default_depreciation_rate IS 'Default annual depreciation rate percentage for assets in this category';
COMMENT ON COLUMN asset_categories.maintenance_checklist IS 'JSON array of maintenance checklist items for this category';

-- Column comments for assets
COMMENT ON COLUMN assets.asset_code IS 'Unique asset identifier in format CATEGORY-NNNNNN';
COMMENT ON COLUMN assets.accumulated_depreciation IS 'Total depreciation accumulated since purchase date';
COMMENT ON COLUMN assets.depreciation_method IS 'Depreciation calculation method: STRAIGHT_LINE, DECLINING_BALANCE, or UNITS_OF_PRODUCTION';
COMMENT ON COLUMN assets.criticality_level IS 'Asset criticality for maintenance prioritization';
COMMENT ON COLUMN assets.specifications IS 'JSON object containing technical specifications';
COMMENT ON COLUMN assets.component_path IS 'LTREE path for asset component hierarchy';
COMMENT ON COLUMN assets.risk_score IS 'Risk assessment score from 1 (low) to 10 (high)';

-- Function comments
COMMENT ON FUNCTION calculate_asset_depreciation(UUID) IS 'Calculates current depreciation amount for an asset based on its depreciation method';
COMMENT ON FUNCTION update_asset_depreciation() IS 'Updates depreciation and current values for all depreciable assets';
COMMENT ON FUNCTION generate_asset_code(UUID, UUID) IS 'Generates unique asset codes in format CATEGORY-NNNNNN';

-- ============================================
-- VERIFICATION
-- ============================================

DO $$
BEGIN
    -- Verify asset_categories table was created
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'asset_categories') THEN
        RAISE EXCEPTION 'Migration failed: asset_categories table was not created';
    END IF;
    
    -- Verify assets table was created
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'assets') THEN
        RAISE EXCEPTION 'Migration failed: assets table was not created';
    END IF;
    
    -- Verify default categories were inserted
    IF NOT EXISTS (SELECT FROM asset_categories WHERE code = 'HVAC') THEN
        RAISE EXCEPTION 'Migration failed: default asset categories were not created';
    END IF;
    
    RAISE NOTICE 'Migration V116 completed successfully - Asset Management System established';
END $$;

-- End of V116 migration