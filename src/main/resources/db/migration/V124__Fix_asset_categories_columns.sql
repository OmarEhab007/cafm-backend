-- Migration V124: Fix missing updated_at column in asset_categories table
-- 
-- Purpose: Add missing updated_at column to asset_categories table and create update trigger
-- This ensures consistency with BaseEntity pattern used throughout the application
-- 
-- Pattern: Following the same pattern used in assets and inventory_items tables
-- Architecture: Maintains audit trail consistency across all domain entities
-- Standards: Ensures all entities extending BaseEntity have complete audit columns

-- Add missing updated_at column to asset_categories table
ALTER TABLE asset_categories 
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- Update existing records to have current timestamp for updated_at
UPDATE asset_categories 
SET updated_at = COALESCE(created_at, CURRENT_TIMESTAMP)
WHERE updated_at IS NULL;

-- Make the column NOT NULL after setting default values
ALTER TABLE asset_categories 
ALTER COLUMN updated_at SET NOT NULL;

-- Create trigger to automatically update updated_at column on record modification
-- This follows the same pattern used in other tables (assets, inventory_items, etc.)
DROP TRIGGER IF EXISTS update_asset_categories_updated_at ON asset_categories;

CREATE TRIGGER update_asset_categories_updated_at
    BEFORE UPDATE ON asset_categories
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add helpful comment to document the purpose of this column
COMMENT ON COLUMN asset_categories.updated_at IS 'Timestamp when the record was last updated, automatically maintained by trigger';

-- Verify the fix by checking that all asset-related tables now have consistent audit columns
-- This ensures our multi-tenant asset management system maintains proper audit trails
DO $$
BEGIN
    -- Check that asset_categories now has updated_at column
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'asset_categories' 
        AND column_name = 'updated_at'
    ) THEN
        RAISE EXCEPTION 'Migration failed: updated_at column not found in asset_categories table';
    END IF;
    
    -- Check that the trigger exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.triggers 
        WHERE trigger_name = 'update_asset_categories_updated_at'
        AND event_object_table = 'asset_categories'
    ) THEN
        RAISE EXCEPTION 'Migration failed: update trigger not found for asset_categories table';
    END IF;
    
    RAISE NOTICE 'Migration V124 completed successfully: asset_categories table now has consistent audit columns';
END $$;