-- ================================
-- V17: Update Damage Counts Table
-- ================================
-- Table for tracking damaged equipment and items inventory
-- The table already exists from V1, so we only add missing columns
-- Priority column already exists as enum type priority_level

DO $$
BEGIN
    -- Table already exists from V1, only add missing columns
    
    -- Add school_name column if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                  WHERE table_name = 'damage_counts' AND column_name = 'school_name') THEN
        ALTER TABLE damage_counts ADD COLUMN school_name VARCHAR(255);
        UPDATE damage_counts dc 
        SET school_name = COALESCE(s.name, 'Unknown School')
        FROM schools s 
        WHERE dc.school_id = s.id;
        ALTER TABLE damage_counts ALTER COLUMN school_name SET NOT NULL;
    END IF;
    
    -- Add status column if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                  WHERE table_name = 'damage_counts' AND column_name = 'status') THEN
        ALTER TABLE damage_counts ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'draft';
    END IF;
    
    -- Add JSONB columns for damage tracking
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                  WHERE table_name = 'damage_counts' AND column_name = 'item_counts') THEN
        ALTER TABLE damage_counts ADD COLUMN item_counts JSONB DEFAULT '{}';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                  WHERE table_name = 'damage_counts' AND column_name = 'section_photos') THEN
        ALTER TABLE damage_counts ADD COLUMN section_photos JSONB DEFAULT '{}';
    END IF;
    
    -- Add metadata columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                  WHERE table_name = 'damage_counts' AND column_name = 'total_items_count') THEN
        ALTER TABLE damage_counts ADD COLUMN total_items_count INTEGER DEFAULT 0;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                  WHERE table_name = 'damage_counts' AND column_name = 'estimated_repair_cost') THEN
        -- Column might already exist from V1 with slightly different name
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'damage_counts' AND column_name = 'estimated_cost') THEN
            ALTER TABLE damage_counts ADD COLUMN estimated_cost DECIMAL(12, 2);
        END IF;
    END IF;
    
    -- Priority column already exists as enum type priority_level from V1
    -- No need to add or modify it
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                  WHERE table_name = 'damage_counts' AND column_name = 'notes') THEN
        ALTER TABLE damage_counts ADD COLUMN notes TEXT;
    END IF;
    
    -- Add timestamp columns if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                  WHERE table_name = 'damage_counts' AND column_name = 'submitted_at') THEN
        ALTER TABLE damage_counts ADD COLUMN submitted_at TIMESTAMP WITH TIME ZONE;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                  WHERE table_name = 'damage_counts' AND column_name = 'reviewed_at') THEN
        ALTER TABLE damage_counts ADD COLUMN reviewed_at TIMESTAMP WITH TIME ZONE;
    END IF;
    
    -- Add status constraint
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                  WHERE table_name = 'damage_counts' 
                  AND constraint_name = 'chk_damage_counts_status') THEN
        ALTER TABLE damage_counts ADD CONSTRAINT chk_damage_counts_status 
            CHECK (status IN ('draft', 'submitted', 'reviewed', 'approved', 'rejected', 'completed'));
    END IF;
    
    -- Do not add priority constraint as the column uses enum type priority_level
END $$;

-- Create indexes only if they don't exist
DO $$
BEGIN
    -- Some indexes may already exist from V1 and V5
    -- Only create new indexes for the columns we're adding
    
    -- Status index for new column
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'damage_counts' AND column_name = 'status') AND
       NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_damage_counts_status') THEN
        CREATE INDEX idx_damage_counts_status ON damage_counts(status);
    END IF;
    
    -- Submitted_at index for new column
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'damage_counts' AND column_name = 'submitted_at') AND
       NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_damage_counts_submitted_at') THEN
        CREATE INDEX idx_damage_counts_submitted_at ON damage_counts(submitted_at);
    END IF;
    
    -- Reviewed_at index for new column
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'damage_counts' AND column_name = 'reviewed_at') AND
       NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_damage_counts_reviewed_at') THEN
        CREATE INDEX idx_damage_counts_reviewed_at ON damage_counts(reviewed_at);
    END IF;
    
    -- Composite index with status
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'damage_counts' AND column_name = 'status') AND
       NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_damage_counts_company_status') THEN
        CREATE INDEX idx_damage_counts_company_status ON damage_counts(company_id, status);
    END IF;
    
    -- GIN indexes for new JSONB columns
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'damage_counts' AND column_name = 'item_counts') AND
       NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_damage_counts_item_counts_gin') THEN
        CREATE INDEX idx_damage_counts_item_counts_gin ON damage_counts USING GIN (item_counts);
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'damage_counts' AND column_name = 'section_photos') AND
       NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_damage_counts_section_photos_gin') THEN
        CREATE INDEX idx_damage_counts_section_photos_gin ON damage_counts USING GIN (section_photos);
    END IF;
END $$;

-- Comments for new columns only
DO $$
BEGIN
    -- Only add comments for columns that exist
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'damage_counts' AND column_name = 'item_counts') THEN
        COMMENT ON COLUMN damage_counts.item_counts IS 'JSONB containing counts of damaged items organized by category (mechanical, electrical, civil, safety, AC)';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'damage_counts' AND column_name = 'section_photos') THEN
        COMMENT ON COLUMN damage_counts.section_photos IS 'Photos of damaged items organized by category';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'damage_counts' AND column_name = 'total_items_count') THEN
        COMMENT ON COLUMN damage_counts.total_items_count IS 'Calculated sum of all damaged items';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'damage_counts' AND column_name = 'estimated_cost') THEN
        COMMENT ON COLUMN damage_counts.estimated_cost IS 'Estimated total replacement cost in SAR';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'damage_counts' AND column_name = 'status') THEN
        COMMENT ON COLUMN damage_counts.status IS 'Workflow status (draft, submitted, reviewed, approved, rejected, completed)';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'damage_counts' AND column_name = 'school_name') THEN
        COMMENT ON COLUMN damage_counts.school_name IS 'Denormalized school name for performance';
    END IF;
END $$;