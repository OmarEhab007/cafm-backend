-- Fix missing updated_at column in inventory_categories table

-- Add updated_at column if it doesn't exist
ALTER TABLE inventory_categories 
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- Update existing records to have updated_at same as created_at if null
UPDATE inventory_categories 
SET updated_at = COALESCE(created_at, CURRENT_TIMESTAMP) 
WHERE updated_at IS NULL;

-- Make the column NOT NULL after setting default values
ALTER TABLE inventory_categories 
ALTER COLUMN updated_at SET NOT NULL;

-- Create or replace trigger to update the updated_at column
CREATE OR REPLACE FUNCTION update_inventory_categories_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop trigger if exists and recreate
DROP TRIGGER IF EXISTS update_inventory_categories_updated_at ON inventory_categories;

CREATE TRIGGER update_inventory_categories_updated_at
    BEFORE UPDATE ON inventory_categories
    FOR EACH ROW
    EXECUTE FUNCTION update_inventory_categories_updated_at();

-- Add comment
COMMENT ON COLUMN inventory_categories.updated_at IS 'Timestamp when the inventory category was last updated';

-- Verify the column was added successfully
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'inventory_categories' 
        AND column_name = 'updated_at'
    ) THEN
        RAISE EXCEPTION 'Failed to add updated_at column to inventory_categories table';
    END IF;
END $$;