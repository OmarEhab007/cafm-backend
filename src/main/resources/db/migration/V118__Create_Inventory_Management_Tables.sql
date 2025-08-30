-- ============================================
-- V118__Create_Inventory_Management_Tables.sql
-- CAFM Backend - Enhance Existing Inventory Management System
-- Purpose: Add missing columns and features to existing inventory tables
-- Pattern: Clean Architecture with aggregate design for inventory domain
-- Java 23: Uses records for DTOs and sealed interfaces for transaction types
-- Architecture: Multi-tenant with real-time stock tracking and automatic reorder points
-- Standards: Full audit trails with FIFO/LIFO costing and batch tracking
-- ============================================

-- ============================================
-- STEP 1: ENHANCE EXISTING INVENTORY_ITEMS TABLE
-- ============================================

-- Add missing columns to existing inventory_items table
DO $$
BEGIN
    -- Add barcode column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='barcode') THEN
        ALTER TABLE inventory_items ADD COLUMN barcode VARCHAR(100);
    END IF;
    
    -- Add qr_code column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='qr_code') THEN
        ALTER TABLE inventory_items ADD COLUMN qr_code VARCHAR(255);
    END IF;
    
    -- Add subcategory column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='subcategory') THEN
        ALTER TABLE inventory_items ADD COLUMN subcategory VARCHAR(100);
    END IF;
    
    -- Add manufacturer column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='manufacturer') THEN
        ALTER TABLE inventory_items ADD COLUMN manufacturer VARCHAR(100);
    END IF;
    
    -- Add enhanced stock columns if they don't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='available_stock') THEN
        ALTER TABLE inventory_items ADD COLUMN available_stock DECIMAL(10,2) DEFAULT 0;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='reserved_stock') THEN
        ALTER TABLE inventory_items ADD COLUMN reserved_stock DECIMAL(10,2) DEFAULT 0;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='in_transit_stock') THEN
        ALTER TABLE inventory_items ADD COLUMN in_transit_stock DECIMAL(10,2) DEFAULT 0;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='safety_stock') THEN
        ALTER TABLE inventory_items ADD COLUMN safety_stock DECIMAL(10,2) DEFAULT 0;
    END IF;
    
    -- Add cost columns if they don't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='standard_cost') THEN
        ALTER TABLE inventory_items ADD COLUMN standard_cost DECIMAL(10,2);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='current_market_cost') THEN
        ALTER TABLE inventory_items ADD COLUMN current_market_cost DECIMAL(10,2);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='total_inventory_value') THEN
        ALTER TABLE inventory_items ADD COLUMN total_inventory_value DECIMAL(12,2) DEFAULT 0;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='costing_method') THEN
        ALTER TABLE inventory_items ADD COLUMN costing_method VARCHAR(20) DEFAULT 'WEIGHTED_AVERAGE';
    END IF;
    
    -- Add enhanced storage columns if they don't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='default_warehouse') THEN
        ALTER TABLE inventory_items ADD COLUMN default_warehouse VARCHAR(100);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='default_location') THEN
        ALTER TABLE inventory_items ADD COLUMN default_location VARCHAR(100);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='shelf_location') THEN
        ALTER TABLE inventory_items ADD COLUMN shelf_location VARCHAR(100);
    END IF;
    
    -- Add enhanced boolean flags if they don't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='is_serialized') THEN
        ALTER TABLE inventory_items ADD COLUMN is_serialized BOOLEAN DEFAULT FALSE;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='is_batch_tracked') THEN
        ALTER TABLE inventory_items ADD COLUMN is_batch_tracked BOOLEAN DEFAULT FALSE;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='is_perishable') THEN
        ALTER TABLE inventory_items ADD COLUMN is_perishable BOOLEAN DEFAULT FALSE;
    END IF;
    
    -- Add lifecycle management columns if they don't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='shelf_life_days') THEN
        ALTER TABLE inventory_items ADD COLUMN shelf_life_days INTEGER;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='lead_time_days') THEN
        ALTER TABLE inventory_items ADD COLUMN lead_time_days INTEGER;
    END IF;
    
    -- Add JSONB columns if they don't exist  
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='dimensions') THEN
        ALTER TABLE inventory_items ADD COLUMN dimensions JSONB DEFAULT '{}';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_items' AND column_name='custom_fields') THEN
        ALTER TABLE inventory_items ADD COLUMN custom_fields JSONB DEFAULT '{}';
    END IF;

END $$;

-- Add partial unique index for barcode (only unique when not null)
DO $$
BEGIN
    -- Only create the index if the barcode column exists
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='inventory_items' AND column_name='barcode') THEN
        -- Drop existing index if it exists with wrong definition
        DROP INDEX IF EXISTS uk_inventory_items_barcode_company;
        -- Create the correct index
        CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_items_barcode_company 
            ON inventory_items(barcode, company_id) 
            WHERE barcode IS NOT NULL;
    END IF;
END $$;

-- ============================================
-- STEP 2: ENHANCE EXISTING INVENTORY_TRANSACTIONS TABLE
-- ============================================

-- Add missing columns to existing inventory_transactions table
DO $$
BEGIN
    -- Add enhanced columns if they don't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_transactions' AND column_name='batch_number') THEN
        ALTER TABLE inventory_transactions ADD COLUMN batch_number VARCHAR(50);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_transactions' AND column_name='serial_numbers') THEN
        ALTER TABLE inventory_transactions ADD COLUMN serial_numbers TEXT[];
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_transactions' AND column_name='expiry_date') THEN
        ALTER TABLE inventory_transactions ADD COLUMN expiry_date DATE;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_transactions' AND column_name='manufacturing_date') THEN
        ALTER TABLE inventory_transactions ADD COLUMN manufacturing_date DATE;
    END IF;
    
    -- Add workflow columns if they don't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_transactions' AND column_name='requested_by_id') THEN
        ALTER TABLE inventory_transactions ADD COLUMN requested_by_id UUID REFERENCES users(id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_transactions' AND column_name='processed_by_id') THEN
        ALTER TABLE inventory_transactions ADD COLUMN processed_by_id UUID REFERENCES users(id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_transactions' AND column_name='processed_at') THEN
        ALTER TABLE inventory_transactions ADD COLUMN processed_at TIMESTAMP WITH TIME ZONE;
    END IF;
    
    -- Add quality control columns if they don't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_transactions' AND column_name='quality_checked') THEN
        ALTER TABLE inventory_transactions ADD COLUMN quality_checked BOOLEAN DEFAULT FALSE;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_transactions' AND column_name='quality_check_date') THEN
        ALTER TABLE inventory_transactions ADD COLUMN quality_check_date TIMESTAMP WITH TIME ZONE;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_transactions' AND column_name='quality_check_notes') THEN
        ALTER TABLE inventory_transactions ADD COLUMN quality_check_notes TEXT;
    END IF;
    
    -- Add external reference columns if they don't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_transactions' AND column_name='external_transaction_id') THEN
        ALTER TABLE inventory_transactions ADD COLUMN external_transaction_id VARCHAR(100);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_transactions' AND column_name='receipt_number') THEN
        ALTER TABLE inventory_transactions ADD COLUMN receipt_number VARCHAR(50);
    END IF;
    
    -- Add warehouse/bin tracking if they don't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_transactions' AND column_name='warehouse') THEN
        ALTER TABLE inventory_transactions ADD COLUMN warehouse VARCHAR(100);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_transactions' AND column_name='bin_number') THEN
        ALTER TABLE inventory_transactions ADD COLUMN bin_number VARCHAR(50);
    END IF;
    
    -- Add cost impact column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_transactions' AND column_name='cost_impact') THEN
        ALTER TABLE inventory_transactions ADD COLUMN cost_impact DECIMAL(12,2);
    END IF;
    
    -- Add status column if it doesn't exist (with proper constraint)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='inventory_transactions' AND column_name='status') THEN
        ALTER TABLE inventory_transactions ADD COLUMN status VARCHAR(20) DEFAULT 'PROCESSED';
    END IF;

END $$;

-- ============================================
-- STEP 3: ADD INVENTORY_ITEM_ID FK TO WORK_ORDER_MATERIALS (CONDITIONAL)
-- ============================================

-- Only add inventory_item_id column and FK if it doesn't already exist
DO $$
BEGIN
    -- Add inventory_item_id column to work_order_materials if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='work_order_materials' AND column_name='inventory_item_id') THEN
        ALTER TABLE work_order_materials ADD COLUMN inventory_item_id UUID;
        
        -- Add the foreign key constraint
        ALTER TABLE work_order_materials 
            ADD CONSTRAINT fk_work_order_materials_inventory_item_id 
            FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id);
    END IF;
END $$;

-- ============================================
-- STEP 4: CREATE PERFORMANCE INDEXES
-- ============================================

-- Inventory Items Indexes (conditional on column existence)
CREATE INDEX IF NOT EXISTS idx_inventory_items_company_active ON inventory_items(company_id, is_active);

DO $$
BEGIN
    -- Create category index if columns exist
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='inventory_items' AND column_name='category') THEN
        CREATE INDEX IF NOT EXISTS idx_inventory_items_category ON inventory_items(company_id, category);
    END IF;
    
    -- Create barcode index if column exists
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='inventory_items' AND column_name='barcode') THEN
        CREATE INDEX IF NOT EXISTS idx_inventory_items_barcode ON inventory_items(barcode) WHERE barcode IS NOT NULL;
    END IF;
    
    -- Create manufacturer index if column exists
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='inventory_items' AND column_name='manufacturer') THEN
        CREATE INDEX IF NOT EXISTS idx_inventory_items_manufacturer ON inventory_items(manufacturer, model);
    END IF;
END $$;

-- Stock Level Indexes
CREATE INDEX IF NOT EXISTS idx_inventory_items_low_stock ON inventory_items(company_id, current_stock, minimum_stock) 
    WHERE current_stock <= minimum_stock AND is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_inventory_items_reorder_required ON inventory_items(company_id, current_stock, reorder_level) 
    WHERE reorder_level IS NOT NULL AND current_stock <= reorder_level AND is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_inventory_items_out_of_stock ON inventory_items(company_id, current_stock) 
    WHERE current_stock = 0 AND is_active = TRUE;

-- Cost and Value Indexes (conditional on column existence)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='inventory_items' AND column_name='total_inventory_value') THEN
        CREATE INDEX IF NOT EXISTS idx_inventory_items_high_value ON inventory_items(total_inventory_value DESC) 
            WHERE total_inventory_value > 0;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='inventory_items' AND column_name='costing_method') THEN
        CREATE INDEX IF NOT EXISTS idx_inventory_items_costing_method ON inventory_items(costing_method);
    END IF;
    
    -- Location and Storage Indexes
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='inventory_items' AND column_name='default_warehouse') THEN
        CREATE INDEX IF NOT EXISTS idx_inventory_items_warehouse ON inventory_items(default_warehouse);
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='inventory_items' AND column_name='default_location') THEN
        CREATE INDEX IF NOT EXISTS idx_inventory_items_location ON inventory_items(default_warehouse, default_location);
    END IF;
END $$;

-- Inventory Transactions Indexes (use existing column names)
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_item_enhanced ON inventory_transactions(item_id, transaction_date DESC);
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_company_date_enhanced ON inventory_transactions(company_id, transaction_date DESC);
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_type_enhanced ON inventory_transactions(transaction_type, transaction_date DESC);
-- Only create status index if status column exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='inventory_transactions' AND column_name='status') THEN
        CREATE INDEX IF NOT EXISTS idx_inventory_transactions_status ON inventory_transactions(status) WHERE status != 'PROCESSED';
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_work_order_enhanced ON inventory_transactions(work_order_id) 
    WHERE work_order_id IS NOT NULL;

-- Transaction Processing Indexes (conditional on column existence)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='inventory_transactions' AND column_name='status') THEN
        CREATE INDEX IF NOT EXISTS idx_inventory_transactions_pending ON inventory_transactions(company_id, status, transaction_date) 
            WHERE status = 'PENDING';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='inventory_transactions' AND column_name='approved_by') THEN
        CREATE INDEX IF NOT EXISTS idx_inventory_transactions_approval ON inventory_transactions(approved_by, approved_at) 
            WHERE approved_by IS NOT NULL;
    END IF;
    
    -- Batch and Serial Tracking Indexes
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='inventory_transactions' AND column_name='batch_number') THEN
        CREATE INDEX IF NOT EXISTS idx_inventory_transactions_batch ON inventory_transactions(item_id, batch_number) 
            WHERE batch_number IS NOT NULL;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='inventory_transactions' AND column_name='expiry_date') THEN
        CREATE INDEX IF NOT EXISTS idx_inventory_transactions_expiry ON inventory_transactions(expiry_date) 
            WHERE expiry_date IS NOT NULL;
    END IF;
    
    -- Supplier and Purchase Indexes  
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='inventory_transactions' AND column_name='supplier') THEN
        CREATE INDEX IF NOT EXISTS idx_inventory_transactions_supplier_name ON inventory_transactions(supplier) 
            WHERE supplier IS NOT NULL;
    END IF;
END $$;

-- Full-text Search Indexes (conditional on column existence)
DO $$
BEGIN
    -- Create search index only if we have the necessary columns
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='inventory_items' AND column_name='name') THEN
        CREATE INDEX IF NOT EXISTS idx_inventory_items_search ON inventory_items 
            USING GIN(to_tsvector('english', name || ' ' || COALESCE(description, '')));
    END IF;
END $$;

-- JSONB Indexes (conditional on column existence)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='inventory_items' AND column_name='dimensions') THEN
        CREATE INDEX IF NOT EXISTS idx_inventory_items_dimensions ON inventory_items USING GIN(dimensions);
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name='inventory_items' AND column_name='custom_fields') THEN
        CREATE INDEX IF NOT EXISTS idx_inventory_items_custom_fields ON inventory_items USING GIN(custom_fields);
    END IF;
END $$;

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
    
    -- Note: next_order_date and last_order_date fields are not part of inventory_items table structure
    -- This logic will be handled in the application layer when needed
    
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
    v_inventory_item_id UUID;
BEGIN
    -- Handle both column names (item_id from existing table, inventory_item_id from new schema)
    v_inventory_item_id := COALESCE(NEW.inventory_item_id, NEW.item_id);
    
    -- Get current item details
    SELECT * INTO v_item FROM inventory_items WHERE id = v_inventory_item_id;
    
    IF v_item IS NULL THEN
        RAISE EXCEPTION 'Inventory item not found: %', v_inventory_item_id;
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
            updated_by_id = COALESCE(NEW.updated_by_id, NEW.modified_by)
        WHERE id = v_inventory_item_id;
        
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

-- Enable RLS for inventory tables (inventory_items already has RLS enabled)
-- ALTER TABLE inventory_items ENABLE ROW LEVEL SECURITY; -- Already enabled
-- RLS is already enabled on inventory_transactions

-- Create policies for tenant isolation if they don't exist
DO $$
BEGIN
    -- Check if policy exists for inventory_items using the correct view
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE tablename = 'inventory_items' 
                   AND policyname = 'tenant_isolation_inventory_items_enhanced') THEN
        -- Drop existing policy if it exists with the same logic
        DROP POLICY IF EXISTS tenant_isolation_inventory_items ON inventory_items;
        
        CREATE POLICY tenant_isolation_inventory_items_enhanced ON inventory_items
            FOR ALL USING (company_id = COALESCE(
                NULLIF(current_setting('app.current_company_id', true), '')::UUID,
                '00000000-0000-0000-0000-000000000001'::UUID
            ));
    END IF;
    
    -- Check if policy exists for inventory_transactions using the correct view  
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE tablename = 'inventory_transactions' 
                   AND policyname = 'tenant_isolation_inventory_transactions_enhanced') THEN
        CREATE POLICY tenant_isolation_inventory_transactions_enhanced ON inventory_transactions
            FOR ALL USING (company_id = COALESCE(
                NULLIF(current_setting('app.current_company_id', true), '')::UUID,
                '00000000-0000-0000-0000-000000000001'::UUID
            ));
    END IF;
END $$;

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
    -- Verify inventory_items table exists
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'inventory_items') THEN
        RAISE EXCEPTION 'Migration failed: inventory_items table does not exist';
    END IF;
    
    -- Verify inventory_transactions table exists
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'inventory_transactions') THEN
        RAISE EXCEPTION 'Migration failed: inventory_transactions table does not exist';
    END IF;
    
    -- Verify barcode column was added to inventory_items
    IF NOT EXISTS (SELECT FROM information_schema.columns 
                   WHERE table_name = 'inventory_items' AND column_name = 'barcode') THEN
        RAISE EXCEPTION 'Migration failed: barcode column was not added to inventory_items';
    END IF;
    
    -- Verify enhanced columns were added to inventory_transactions
    IF NOT EXISTS (SELECT FROM information_schema.columns 
                   WHERE table_name = 'inventory_transactions' AND column_name = 'batch_number') THEN
        RAISE EXCEPTION 'Migration failed: batch_number column was not added to inventory_transactions';
    END IF;
    
    -- Verify barcode index was created
    IF NOT EXISTS (SELECT FROM pg_indexes 
                   WHERE indexname = 'uk_inventory_items_barcode_company') THEN
        RAISE EXCEPTION 'Migration failed: barcode index was not created';
    END IF;
    
    RAISE NOTICE 'Migration V118 completed successfully - Inventory Management System enhanced';
END $$;

-- End of V118 migration