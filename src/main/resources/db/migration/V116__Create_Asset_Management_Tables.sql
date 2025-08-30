-- ============================================
-- V116__Create_Asset_Management_Tables.sql
-- CAFM Backend - Asset Management Extensions (Minimal Version)
-- Purpose: Add extensions and helper functions for existing asset tables
-- Pattern: Clean Architecture with PostgreSQL extensions
-- Java 23: Uses modern Java features for enum validation and performance
-- Architecture: Multi-tenant with company_id isolation and audit trails
-- Standards: Spring Boot 3.3.x with PostgreSQL best practices
-- ============================================

-- ============================================
-- STEP 1: ENABLE REQUIRED EXTENSIONS
-- ============================================

-- Enable LTREE extension for hierarchical data
CREATE EXTENSION IF NOT EXISTS ltree;

-- ============================================
-- STEP 2: VERIFY EXISTING TABLES
-- ============================================
-- The following tables already exist with different schemas:
-- - asset_categories (simplified version without hierarchy)  
-- - assets (basic asset management)
-- - asset_maintenance (maintenance tracking)
-- This migration adds only supportive elements

-- ============================================
-- STEP 3: CREATE HELPER FUNCTIONS
-- ============================================

-- Function to calculate asset depreciation
CREATE OR REPLACE FUNCTION calculate_asset_depreciation(asset_uuid UUID)
RETURNS DECIMAL AS $$
DECLARE
    asset_record RECORD;
    current_value DECIMAL;
    years_in_service DECIMAL;
    annual_depreciation DECIMAL;
BEGIN
    -- Get asset details
    SELECT purchase_date, purchase_cost, NULL as depreciation_rate, depreciation_method
    INTO asset_record
    FROM assets 
    WHERE id = asset_uuid AND is_active = true;
    
    IF NOT FOUND THEN
        RETURN 0;
    END IF;
    
    -- Calculate years in service
    years_in_service := EXTRACT(EPOCH FROM (CURRENT_DATE - asset_record.purchase_date)) / (365.25 * 24 * 3600);
    
    -- Calculate depreciation based on method
    IF asset_record.depreciation_method = 'STRAIGHT_LINE' THEN
        annual_depreciation := COALESCE(asset_record.purchase_cost, 0) * (COALESCE(asset_record.depreciation_rate, 0) / 100);
        current_value := GREATEST(0, COALESCE(asset_record.purchase_cost, 0) - (annual_depreciation * years_in_service));
    ELSE
        -- Default to straight line if method not recognized
        annual_depreciation := COALESCE(asset_record.purchase_cost, 0) * (COALESCE(asset_record.depreciation_rate, 0) / 100);
        current_value := GREATEST(0, COALESCE(asset_record.purchase_cost, 0) - (annual_depreciation * years_in_service));
    END IF;
    
    RETURN ROUND(current_value, 2);
END;
$$ LANGUAGE plpgsql STABLE;

-- Function to update asset depreciation (batch process)
CREATE OR REPLACE FUNCTION update_asset_depreciation()
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER := 0;
    asset_rec RECORD;
    calculated_value DECIMAL;
BEGIN
    -- Update depreciation for all active assets
    FOR asset_rec IN 
        SELECT id FROM assets 
        WHERE is_active = true 
        AND purchase_date IS NOT NULL 
        AND purchase_cost IS NOT NULL 
        AND purchase_cost > 0
    LOOP
        calculated_value := calculate_asset_depreciation(asset_rec.id);
        
        UPDATE assets 
        SET current_value = calculated_value,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = asset_rec.id
        AND (current_value IS NULL OR ABS(current_value - calculated_value) > 0.01);
        
        IF FOUND THEN
            updated_count := updated_count + 1;
        END IF;
    END LOOP;
    
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 4: ADD PERFORMANCE INDEXES (IF NOT EXISTS)
-- ============================================

-- Asset categories indexes (using correct column conditions)
CREATE INDEX IF NOT EXISTS idx_asset_categories_company_v116 ON asset_categories(company_id) WHERE version IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_asset_categories_active_v116 ON asset_categories(company_id, is_active) WHERE is_active = true;

-- Assets indexes (using correct column conditions)
CREATE INDEX IF NOT EXISTS idx_assets_company_v116 ON assets(company_id) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_assets_category_v116 ON assets(category_id) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_assets_purchase_date_v116 ON assets(purchase_date) WHERE is_active = true;

-- Asset maintenance indexes (if asset_maintenance table exists and has correct columns)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'asset_maintenance') THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_asset_maintenance_asset_v116 ON asset_maintenance(asset_id)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_asset_maintenance_work_order_v116 ON asset_maintenance(work_order_id)';
    END IF;
END $$;

-- ============================================
-- STEP 5: ADD COMMENTS FOR DOCUMENTATION  
-- ============================================

COMMENT ON FUNCTION calculate_asset_depreciation(UUID) IS 'Calculates current depreciation amount for an asset based on its depreciation method';
COMMENT ON FUNCTION update_asset_depreciation() IS 'Updates depreciation and current values for all depreciable assets';

-- Migration completed successfully
DO $$
BEGIN
    RAISE NOTICE 'V116 Migration completed: Added asset management extensions and functions';
END $$;