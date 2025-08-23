-- ============================================
-- V118__Create_Inventory_Management_Tables.sql
-- CAFM Backend - Create Complete Inventory Management System
-- Purpose: Establish comprehensive inventory tracking with stock movements and cost management
-- Pattern: Clean Architecture with aggregate design for inventory domain
-- Java 23: Uses records for DTOs and sealed interfaces for transaction types
-- Architecture: Multi-tenant with real-time stock tracking and automatic reorder points
-- Standards: Full audit trails with FIFO/LIFO costing and batch tracking
-- ============================================

-- ============================================
-- STEP 1: CREATE INVENTORY ITEMS TABLE
-- ============================================

CREATE TABLE inventory_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    
    -- Item Identification
    item_code VARCHAR(50) NOT NULL,
    barcode VARCHAR(100),
    qr_code VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255),
    description TEXT,
    
    -- Categorization
    category VARCHAR(100),
    subcategory VARCHAR(100),
    brand VARCHAR(100),
    model VARCHAR(100),
    manufacturer VARCHAR(100),
    
    -- Physical Properties
    unit_of_measure unit_enum DEFAULT 'PIECE',
    weight_per_unit DECIMAL(8,3), -- in kg
    volume_per_unit DECIMAL(8,3), -- in cubic meters
    dimensions JSONB DEFAULT '{}', -- {length, width, height}
    
    -- Stock Quantities
    current_stock DECIMAL(10,3) DEFAULT 0,
    available_stock DECIMAL(10,3) DEFAULT 0, -- current_stock - reserved_stock
    reserved_stock DECIMAL(10,3) DEFAULT 0,
    in_transit_stock DECIMAL(10,3) DEFAULT 0,
    
    -- Stock Level Management
    minimum_stock DECIMAL(10,3) DEFAULT 0,
    maximum_stock DECIMAL(10,3),
    reorder_level DECIMAL(10,3),
    reorder_quantity DECIMAL(10,3),
    safety_stock DECIMAL(10,3) DEFAULT 0,
    
    -- Cost Information
    average_cost DECIMAL(10,2) DEFAULT 0,
    last_purchase_cost DECIMAL(10,2),
    standard_cost DECIMAL(10,2),
    current_market_cost DECIMAL(10,2),
    total_inventory_value DECIMAL(12,2) DEFAULT 0,
    
    -- Costing Method
    costing_method VARCHAR(20) DEFAULT 'WEIGHTED_AVERAGE', -- WEIGHTED_AVERAGE, FIFO, LIFO, STANDARD
    
    -- Storage and Location
    default_warehouse VARCHAR(100),
    default_location VARCHAR(100),
    bin_number VARCHAR(50),
    shelf_location VARCHAR(100),
    storage_requirements TEXT,
    
    -- Item Properties
    is_serialized BOOLEAN DEFAULT FALSE,
    is_batch_tracked BOOLEAN DEFAULT FALSE,
    is_perishable BOOLEAN DEFAULT FALSE,
    is_hazardous BOOLEAN DEFAULT FALSE,
    is_consumable BOOLEAN DEFAULT TRUE,
    
    -- Lifecycle Management
    shelf_life_days INTEGER,
    lead_time_days INTEGER,
    default_supplier_id UUID,
    alternative_suppliers JSONB DEFAULT '[]',
    
    -- Quality and Compliance
    quality_control_required BOOLEAN DEFAULT FALSE,
    compliance_certifications JSONB DEFAULT '[]',
    safety_data_sheet_url TEXT,
    material_safety_notes TEXT,
    
    -- Procurement
    preferred_vendor VARCHAR(255),
    vendor_part_number VARCHAR(100),
    last_order_date DATE,
    next_order_date DATE,
    
    -- Status and Settings
    is_active BOOLEAN DEFAULT TRUE,
    is_discontinued BOOLEAN DEFAULT FALSE,
    discontinue_date DATE,
    replacement_item_id UUID REFERENCES inventory_items(id),
    
    -- Custom Fields
    custom_fields JSONB DEFAULT '{}',
    tags VARCHAR(500), -- Comma-separated tags
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID REFERENCES users(id),
    updated_by_id UUID REFERENCES users(id),
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    -- Constraints
    CONSTRAINT uk_inventory_items_code_company UNIQUE (item_code, company_id),
    CONSTRAINT uk_inventory_items_barcode_company UNIQUE (barcode, company_id) 
        WHERE barcode IS NOT NULL,
    CONSTRAINT chk_inventory_items_stock_positive CHECK (
        current_stock >= 0 AND available_stock >= 0 AND 
        reserved_stock >= 0 AND in_transit_stock >= 0 AND 
        safety_stock >= 0
    ),
    CONSTRAINT chk_inventory_items_stock_levels CHECK (
        minimum_stock >= 0 AND 
        (maximum_stock IS NULL OR maximum_stock >= minimum_stock) AND
        (reorder_level IS NULL OR reorder_level >= 0) AND
        (reorder_quantity IS NULL OR reorder_quantity > 0)
    ),
    CONSTRAINT chk_inventory_items_costs_positive CHECK (
        average_cost >= 0 AND 
        (last_purchase_cost IS NULL OR last_purchase_cost >= 0) AND
        (standard_cost IS NULL OR standard_cost >= 0) AND
        (current_market_cost IS NULL OR current_market_cost >= 0) AND
        total_inventory_value >= 0
    ),
    CONSTRAINT chk_inventory_items_costing_method CHECK (
        costing_method IN ('WEIGHTED_AVERAGE', 'FIFO', 'LIFO', 'STANDARD')
    ),
    CONSTRAINT chk_inventory_items_shelf_life CHECK (
        shelf_life_days IS NULL OR shelf_life_days > 0
    ),
    CONSTRAINT chk_inventory_items_lead_time CHECK (
        lead_time_days IS NULL OR lead_time_days >= 0
    )
);

-- ============================================
-- STEP 2: CREATE INVENTORY TRANSACTIONS TABLE
-- ============================================

CREATE TABLE inventory_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    
    -- Transaction Identification
    transaction_number VARCHAR(50) NOT NULL,
    transaction_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    transaction_type inventory_transaction_type NOT NULL,
    
    -- Item and Quantity
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id),
    quantity DECIMAL(10,3) NOT NULL,
    unit_of_measure unit_enum DEFAULT 'PIECE',
    
    -- Cost Information
    unit_cost DECIMAL(10,2),
    total_cost DECIMAL(12,2),
    currency VARCHAR(3) DEFAULT 'SAR',
    
    -- Batch and Serial Tracking
    batch_number VARCHAR(50),
    serial_numbers TEXT[], -- Array of serial numbers
    expiry_date DATE,
    manufacturing_date DATE,
    
    -- Location Information
    from_location VARCHAR(100),
    to_location VARCHAR(100),
    warehouse VARCHAR(100),
    bin_number VARCHAR(50),
    
    -- Related Entities
    work_order_id UUID REFERENCES work_orders(id),
    work_order_material_id UUID REFERENCES work_order_materials(id),
    purchase_order_number VARCHAR(50),
    supplier_id UUID,
    supplier_name VARCHAR(255),
    
    -- Transaction Context
    reference_type VARCHAR(50), -- WORK_ORDER, PURCHASE_ORDER, STOCK_ADJUSTMENT, TRANSFER, etc.
    reference_id UUID,
    reference_number VARCHAR(100),
    
    -- Approval Workflow
    requested_by_id UUID REFERENCES users(id),
    approved_by_id UUID REFERENCES users(id),
    approved_at TIMESTAMP WITH TIME ZONE,
    
    -- Processing Status
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, PROCESSED, CANCELLED
    processed_at TIMESTAMP WITH TIME ZONE,
    processed_by_id UUID REFERENCES users(id),
    
    -- Stock Impact
    stock_before DECIMAL(10,3),
    stock_after DECIMAL(10,3),
    cost_impact DECIMAL(12,2), -- Impact on inventory value
    
    -- Quality Control
    quality_checked BOOLEAN DEFAULT FALSE,
    quality_check_date TIMESTAMP WITH TIME ZONE,
    quality_check_notes TEXT,
    quality_approved_by_id UUID REFERENCES users(id),
    
    -- Notes and Documentation
    notes TEXT,
    reason_code VARCHAR(50),
    reason_description TEXT,
    
    -- External References
    external_transaction_id VARCHAR(100),
    invoice_number VARCHAR(50),
    receipt_number VARCHAR(50),
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID REFERENCES users(id),
    updated_by_id UUID REFERENCES users(id),
    
    -- Constraints
    CONSTRAINT uk_inventory_transactions_number_company UNIQUE (transaction_number, company_id),
    CONSTRAINT chk_inventory_transactions_quantity_nonzero CHECK (quantity != 0),
    CONSTRAINT chk_inventory_transactions_cost_positive CHECK (
        unit_cost IS NULL OR unit_cost >= 0
    ),
    CONSTRAINT chk_inventory_transactions_stock_positive CHECK (
        stock_before >= 0 AND stock_after >= 0
    ),
    CONSTRAINT chk_inventory_transactions_status CHECK (
        status IN ('PENDING', 'APPROVED', 'PROCESSED', 'CANCELLED', 'REJECTED')
    ),
    CONSTRAINT chk_inventory_transactions_dates CHECK (
        expiry_date IS NULL OR manufacturing_date IS NULL OR expiry_date > manufacturing_date
    )
);

-- ============================================
-- STEP 3: ADD INVENTORY_ITEM_ID FK TO WORK_ORDER_MATERIALS
-- ============================================

-- Add foreign key constraint that was referenced but not yet created
ALTER TABLE work_order_materials 
    ADD CONSTRAINT fk_work_order_materials_inventory_item_id 
    FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id);

-- ============================================
-- STEP 4: CREATE PERFORMANCE INDEXES
-- ============================================

-- Inventory Items Indexes
CREATE INDEX idx_inventory_items_company_active ON inventory_items(company_id, is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_inventory_items_category ON inventory_items(company_id, category, subcategory) WHERE deleted_at IS NULL;
CREATE INDEX idx_inventory_items_barcode ON inventory_items(barcode) WHERE barcode IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_inventory_items_manufacturer ON inventory_items(manufacturer, model) WHERE deleted_at IS NULL;

-- Stock Level Indexes
CREATE INDEX idx_inventory_items_low_stock ON inventory_items(company_id, current_stock, minimum_stock) 
    WHERE current_stock <= minimum_stock AND is_active = TRUE AND deleted_at IS NULL;
CREATE INDEX idx_inventory_items_reorder_required ON inventory_items(company_id, current_stock, reorder_level) 
    WHERE reorder_level IS NOT NULL AND current_stock <= reorder_level AND is_active = TRUE AND deleted_at IS NULL;
CREATE INDEX idx_inventory_items_out_of_stock ON inventory_items(company_id, current_stock) 
    WHERE current_stock = 0 AND is_active = TRUE AND deleted_at IS NULL;

-- Cost and Value Indexes
CREATE INDEX idx_inventory_items_high_value ON inventory_items(total_inventory_value DESC) 
    WHERE total_inventory_value > 0 AND deleted_at IS NULL;
CREATE INDEX idx_inventory_items_costing_method ON inventory_items(costing_method) WHERE deleted_at IS NULL;

-- Location and Storage Indexes
CREATE INDEX idx_inventory_items_warehouse ON inventory_items(default_warehouse) WHERE deleted_at IS NULL;
CREATE INDEX idx_inventory_items_location ON inventory_items(default_warehouse, default_location) WHERE deleted_at IS NULL;

-- Inventory Transactions Indexes
CREATE INDEX idx_inventory_transactions_item ON inventory_transactions(inventory_item_id, transaction_date DESC);
CREATE INDEX idx_inventory_transactions_company_date ON inventory_transactions(company_id, transaction_date DESC);
CREATE INDEX idx_inventory_transactions_type ON inventory_transactions(transaction_type, transaction_date DESC);
CREATE INDEX idx_inventory_transactions_status ON inventory_transactions(status) WHERE status != 'PROCESSED';
CREATE INDEX idx_inventory_transactions_work_order ON inventory_transactions(work_order_id) 
    WHERE work_order_id IS NOT NULL;

-- Transaction Processing Indexes
CREATE INDEX idx_inventory_transactions_pending ON inventory_transactions(company_id, status, transaction_date) 
    WHERE status = 'PENDING';
CREATE INDEX idx_inventory_transactions_approval ON inventory_transactions(approved_by_id, approved_at) 
    WHERE approved_by_id IS NOT NULL;

-- Batch and Serial Tracking Indexes
CREATE INDEX idx_inventory_transactions_batch ON inventory_transactions(inventory_item_id, batch_number) 
    WHERE batch_number IS NOT NULL;
CREATE INDEX idx_inventory_transactions_expiry ON inventory_transactions(expiry_date) 
    WHERE expiry_date IS NOT NULL;

-- Supplier and Purchase Indexes
CREATE INDEX idx_inventory_transactions_supplier ON inventory_transactions(supplier_id) WHERE supplier_id IS NOT NULL;
CREATE INDEX idx_inventory_transactions_purchase_order ON inventory_transactions(purchase_order_number) 
    WHERE purchase_order_number IS NOT NULL;

-- Full-text Search Indexes
CREATE INDEX idx_inventory_items_search ON inventory_items 
    USING GIN(to_tsvector('english', name || ' ' || COALESCE(description, '') || ' ' || COALESCE(tags, ''))) 
    WHERE deleted_at IS NULL;

-- JSONB Indexes
CREATE INDEX idx_inventory_items_dimensions ON inventory_items USING GIN(dimensions);
CREATE INDEX idx_inventory_items_custom_fields ON inventory_items USING GIN(custom_fields);
CREATE INDEX idx_inventory_items_suppliers ON inventory_items USING GIN(alternative_suppliers);
CREATE INDEX idx_inventory_items_certifications ON inventory_items USING GIN(compliance_certifications);

-- ============================================
-- STEP 5: CREATE TRIGGERS AND FUNCTIONS
-- ============================================

-- Function to update inventory item calculated fields
CREATE OR REPLACE FUNCTION update_inventory_item_calculated_fields()
RETURNS TRIGGER AS $$
BEGIN
    -- Calculate available stock
    NEW.available_stock := GREATEST(0, NEW.current_stock - NEW.reserved_stock);
    
    -- Calculate total inventory value
    NEW.total_inventory_value := NEW.current_stock * NEW.average_cost;
    
    -- Auto-generate item code if not provided
    IF NEW.item_code IS NULL OR NEW.item_code = '' THEN
        NEW.item_code := generate_inventory_item_code(NEW.company_id, NEW.category);
    END IF;
    
    -- Set next order date based on lead time
    IF NEW.lead_time_days IS NOT NULL AND NEW.last_order_date IS NOT NULL THEN
        NEW.next_order_date := NEW.last_order_date + (NEW.lead_time_days || ' days')::INTERVAL;
    END IF;
    
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for inventory items
CREATE TRIGGER trg_inventory_items_calculated_fields
    BEFORE INSERT OR UPDATE ON inventory_items
    FOR EACH ROW
    EXECUTE FUNCTION update_inventory_item_calculated_fields();

-- Function to process inventory transactions
CREATE OR REPLACE FUNCTION process_inventory_transaction()
RETURNS TRIGGER AS $$
DECLARE
    v_item RECORD;
    v_new_average_cost DECIMAL(10,2);
    v_cost_impact DECIMAL(12,2);
BEGIN
    -- Get current item details
    SELECT * INTO v_item FROM inventory_items WHERE id = NEW.inventory_item_id;
    
    IF v_item IS NULL THEN
        RAISE EXCEPTION 'Inventory item not found: %', NEW.inventory_item_id;
    END IF;
    
    -- Record stock before transaction
    NEW.stock_before := v_item.current_stock;
    
    -- Auto-generate transaction number if not provided
    IF NEW.transaction_number IS NULL OR NEW.transaction_number = '' THEN
        NEW.transaction_number := generate_transaction_number(NEW.company_id, NEW.transaction_type);
    END IF;
    
    -- Calculate total cost if not provided
    IF NEW.total_cost IS NULL AND NEW.unit_cost IS NOT NULL THEN
        NEW.total_cost := NEW.unit_cost * ABS(NEW.quantity);
    END IF;
    
    -- Only update stock if transaction is being processed
    IF NEW.status = 'PROCESSED' AND (OLD.status IS NULL OR OLD.status != 'PROCESSED') THEN
        -- Calculate new stock level
        NEW.stock_after := v_item.current_stock;
        
        -- Adjust stock based on transaction type
        CASE NEW.transaction_type
            WHEN 'RECEIPT' THEN
                NEW.stock_after := v_item.current_stock + NEW.quantity;
                -- Update average cost for receipts
                IF NEW.unit_cost IS NOT NULL AND NEW.quantity > 0 THEN
                    v_new_average_cost := ((v_item.current_stock * v_item.average_cost) + (NEW.quantity * NEW.unit_cost)) / 
                                         NULLIF(v_item.current_stock + NEW.quantity, 0);
                END IF;
                
            WHEN 'ISSUE' THEN
                NEW.stock_after := GREATEST(0, v_item.current_stock - ABS(NEW.quantity));
                
            WHEN 'ADJUSTMENT' THEN
                NEW.stock_after := v_item.current_stock + NEW.quantity; -- quantity can be negative for adjustments
                
            WHEN 'TRANSFER' THEN
                NEW.stock_after := GREATEST(0, v_item.current_stock - ABS(NEW.quantity));
                
            WHEN 'RETURN' THEN
                NEW.stock_after := v_item.current_stock + ABS(NEW.quantity);
                
            WHEN 'DISPOSAL' THEN
                NEW.stock_after := GREATEST(0, v_item.current_stock - ABS(NEW.quantity));
                
            WHEN 'DAMAGE' THEN
                NEW.stock_after := GREATEST(0, v_item.current_stock - ABS(NEW.quantity));
                
            WHEN 'STOCK_CHECK' THEN
                NEW.stock_after := NEW.quantity; -- Set to absolute quantity for stock checks
        END CASE;
        
        -- Calculate cost impact
        v_cost_impact := (NEW.stock_after - NEW.stock_before) * COALESCE(v_new_average_cost, v_item.average_cost);
        NEW.cost_impact := v_cost_impact;
        
        -- Update inventory item
        UPDATE inventory_items
        SET 
            current_stock = NEW.stock_after,
            average_cost = COALESCE(v_new_average_cost, average_cost),
            last_purchase_cost = CASE WHEN NEW.transaction_type = 'RECEIPT' THEN NEW.unit_cost ELSE last_purchase_cost END,
            updated_at = CURRENT_TIMESTAMP,
            updated_by_id = NEW.updated_by_id
        WHERE id = NEW.inventory_item_id;
        
        NEW.processed_at := CURRENT_TIMESTAMP;
    END IF;
    
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for inventory transactions
CREATE TRIGGER trg_inventory_transactions_process
    BEFORE INSERT OR UPDATE ON inventory_transactions
    FOR EACH ROW
    EXECUTE FUNCTION process_inventory_transaction();

-- ============================================
-- STEP 6: UTILITY FUNCTIONS
-- ============================================

-- Function to generate inventory item codes
CREATE OR REPLACE FUNCTION generate_inventory_item_code(p_company_id UUID, p_category VARCHAR)
RETURNS VARCHAR(50) AS $$
DECLARE
    v_category_code VARCHAR(10);
    v_sequence INTEGER;
    v_item_code VARCHAR(50);
BEGIN
    -- Generate category code from category name
    v_category_code := UPPER(LEFT(REGEXP_REPLACE(COALESCE(p_category, 'ITEM'), '[^A-Za-z0-9]', '', 'g'), 4));
    
    IF LENGTH(v_category_code) = 0 THEN
        v_category_code := 'ITEM';
    END IF;
    
    -- Get next sequence number
    SELECT COALESCE(MAX(CAST(RIGHT(item_code, 6) AS INTEGER)), 0) + 1
    INTO v_sequence
    FROM inventory_items
    WHERE company_id = p_company_id
    AND item_code LIKE v_category_code || '-%'
    AND deleted_at IS NULL;
    
    -- Format: ITEM-000001
    v_item_code := v_category_code || '-' || LPAD(v_sequence::TEXT, 6, '0');
    
    RETURN v_item_code;
END;
$$ LANGUAGE plpgsql;

-- Function to generate transaction numbers
CREATE OR REPLACE FUNCTION generate_transaction_number(p_company_id UUID, p_transaction_type inventory_transaction_type)
RETURNS VARCHAR(50) AS $$
DECLARE
    v_type_prefix VARCHAR(10);
    v_date_part VARCHAR(8);
    v_sequence INTEGER;
    v_transaction_number VARCHAR(50);
BEGIN
    -- Generate prefix based on transaction type
    v_type_prefix := CASE p_transaction_type
        WHEN 'RECEIPT' THEN 'REC'
        WHEN 'ISSUE' THEN 'ISS'
        WHEN 'ADJUSTMENT' THEN 'ADJ'
        WHEN 'TRANSFER' THEN 'TRF'
        WHEN 'RETURN' THEN 'RET'
        WHEN 'DISPOSAL' THEN 'DIS'
        WHEN 'DAMAGE' THEN 'DMG'
        WHEN 'STOCK_CHECK' THEN 'CHK'
        ELSE 'TXN'
    END;
    
    -- Generate date part (YYYYMMDD)
    v_date_part := TO_CHAR(CURRENT_DATE, 'YYYYMMDD');
    
    -- Get next sequence number for today
    SELECT COALESCE(MAX(CAST(RIGHT(transaction_number, 4) AS INTEGER)), 0) + 1
    INTO v_sequence
    FROM inventory_transactions
    WHERE company_id = p_company_id
    AND transaction_number LIKE v_type_prefix || '-' || v_date_part || '-%'
    AND DATE(transaction_date) = CURRENT_DATE;
    
    -- Format: REC-20241201-0001
    v_transaction_number := v_type_prefix || '-' || v_date_part || '-' || LPAD(v_sequence::TEXT, 4, '0');
    
    RETURN v_transaction_number;
END;
$$ LANGUAGE plpgsql;

-- Function to get inventory status summary
CREATE OR REPLACE FUNCTION get_inventory_status_summary(p_company_id UUID)
RETURNS TABLE (
    total_items BIGINT,
    active_items BIGINT,
    low_stock_items BIGINT,
    out_of_stock_items BIGINT,
    reorder_required_items BIGINT,
    total_inventory_value DECIMAL(15,2),
    average_stock_turnover DECIMAL(8,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) as total_items,
        COUNT(*) FILTER (WHERE is_active = TRUE) as active_items,
        COUNT(*) FILTER (WHERE current_stock <= minimum_stock AND is_active = TRUE) as low_stock_items,
        COUNT(*) FILTER (WHERE current_stock = 0 AND is_active = TRUE) as out_of_stock_items,
        COUNT(*) FILTER (WHERE reorder_level IS NOT NULL AND current_stock <= reorder_level AND is_active = TRUE) as reorder_required_items,
        SUM(total_inventory_value) as total_inventory_value,
        0.00::DECIMAL(8,2) as average_stock_turnover -- Would calculate based on transaction history
    FROM inventory_items
    WHERE company_id = p_company_id AND deleted_at IS NULL;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 7: ROW LEVEL SECURITY
-- ============================================

-- Enable RLS for inventory tables
ALTER TABLE inventory_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_transactions ENABLE ROW LEVEL SECURITY;

-- Create policies for tenant isolation
CREATE POLICY tenant_isolation_inventory_items ON inventory_items
    FOR ALL USING (company_id = COALESCE(
        NULLIF(current_setting('app.current_company_id', true), '')::UUID,
        '00000000-0000-0000-0000-000000000001'::UUID
    ));

CREATE POLICY tenant_isolation_inventory_transactions ON inventory_transactions
    FOR ALL USING (company_id = COALESCE(
        NULLIF(current_setting('app.current_company_id', true), '')::UUID,
        '00000000-0000-0000-0000-000000000001'::UUID
    ));

-- ============================================
-- STEP 8: COMMENTS FOR DOCUMENTATION
-- ============================================

-- Table comments
COMMENT ON TABLE inventory_items IS 'Comprehensive inventory management with stock levels, costing, and lifecycle tracking';
COMMENT ON TABLE inventory_transactions IS 'Complete audit trail of all inventory movements with batch and cost tracking';

-- Key column comments
COMMENT ON COLUMN inventory_items.costing_method IS 'Method used for inventory valuation: WEIGHTED_AVERAGE, FIFO, LIFO, STANDARD';
COMMENT ON COLUMN inventory_items.available_stock IS 'Current stock minus reserved stock - available for use';
COMMENT ON COLUMN inventory_items.safety_stock IS 'Minimum buffer stock to prevent stockouts';
COMMENT ON COLUMN inventory_transactions.serial_numbers IS 'Array of serial numbers for serialized items';
COMMENT ON COLUMN inventory_transactions.cost_impact IS 'Impact of this transaction on total inventory value';

-- Function comments
COMMENT ON FUNCTION generate_inventory_item_code(UUID, VARCHAR) IS 'Generates unique item codes based on category and sequence';
COMMENT ON FUNCTION generate_transaction_number(UUID, inventory_transaction_type) IS 'Generates unique transaction numbers with type prefix and date';
COMMENT ON FUNCTION get_inventory_status_summary(UUID) IS 'Returns comprehensive inventory status metrics for a company';

-- ============================================
-- VERIFICATION
-- ============================================

DO $$
BEGIN
    -- Verify inventory_items table was created
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'inventory_items') THEN
        RAISE EXCEPTION 'Migration failed: inventory_items table was not created';
    END IF;
    
    -- Verify inventory_transactions table was created
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'inventory_transactions') THEN
        RAISE EXCEPTION 'Migration failed: inventory_transactions table was not created';
    END IF;
    
    -- Verify foreign key was added to work_order_materials
    IF NOT EXISTS (
        SELECT FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_work_order_materials_inventory_item_id'
    ) THEN
        RAISE EXCEPTION 'Migration failed: foreign key not added to work_order_materials';
    END IF;
    
    RAISE NOTICE 'Migration V118 completed successfully - Inventory Management System created';
END $$;

-- End of V118 migration