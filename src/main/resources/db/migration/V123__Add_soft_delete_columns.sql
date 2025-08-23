-- Add soft delete columns to assets table
ALTER TABLE assets ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Add soft delete columns to inventory_items table if missing
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Add indexes for soft delete queries
CREATE INDEX IF NOT EXISTS idx_assets_deleted_at ON assets(deleted_at);
CREATE INDEX IF NOT EXISTS idx_inventory_items_deleted_at ON inventory_items(deleted_at);

-- Add comments
COMMENT ON COLUMN assets.deleted_at IS 'Timestamp when the asset was soft deleted';
COMMENT ON COLUMN inventory_items.deleted_at IS 'Timestamp when the inventory item was soft deleted';