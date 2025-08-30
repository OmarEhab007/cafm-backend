-- ================================
-- V16: Create or Update Maintenance Counts Table
-- ================================
-- Handles both creation of new table and updating existing table
-- Table for tracking complex maintenance inspections with JSONB data

DO $$ 
BEGIN
    -- Check if table exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'maintenance_counts') THEN
        -- Add all missing columns one by one
        
        -- Add status column if missing
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'maintenance_counts' AND column_name = 'status') THEN
            ALTER TABLE maintenance_counts ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'draft';
        END IF;
        
        -- Add school_name column if missing
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'maintenance_counts' AND column_name = 'school_name') THEN
            ALTER TABLE maintenance_counts ADD COLUMN school_name VARCHAR(255);
            UPDATE maintenance_counts mc 
            SET school_name = COALESCE(s.name, 'Unknown School')
            FROM schools s 
            WHERE mc.school_id = s.id;
            ALTER TABLE maintenance_counts ALTER COLUMN school_name SET NOT NULL;
        END IF;
        
        -- Add JSONB columns if missing
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'maintenance_counts' AND column_name = 'item_counts') THEN
            ALTER TABLE maintenance_counts ADD COLUMN item_counts JSONB DEFAULT '{}';
        END IF;
        
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'maintenance_counts' AND column_name = 'text_answers') THEN
            ALTER TABLE maintenance_counts ADD COLUMN text_answers JSONB DEFAULT '{}';
        END IF;
        
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'maintenance_counts' AND column_name = 'yes_no_answers') THEN
            ALTER TABLE maintenance_counts ADD COLUMN yes_no_answers JSONB DEFAULT '{}';
        END IF;
        
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'maintenance_counts' AND column_name = 'yes_no_with_counts') THEN
            ALTER TABLE maintenance_counts ADD COLUMN yes_no_with_counts JSONB DEFAULT '{}';
        END IF;
        
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'maintenance_counts' AND column_name = 'survey_answers') THEN
            ALTER TABLE maintenance_counts ADD COLUMN survey_answers JSONB DEFAULT '{}';
        END IF;
        
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'maintenance_counts' AND column_name = 'maintenance_notes') THEN
            ALTER TABLE maintenance_counts ADD COLUMN maintenance_notes JSONB DEFAULT '{}';
        END IF;
        
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'maintenance_counts' AND column_name = 'fire_safety_alarm_panel_data') THEN
            ALTER TABLE maintenance_counts ADD COLUMN fire_safety_alarm_panel_data JSONB DEFAULT '{}';
        END IF;
        
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'maintenance_counts' AND column_name = 'fire_safety_condition_only_data') THEN
            ALTER TABLE maintenance_counts ADD COLUMN fire_safety_condition_only_data JSONB DEFAULT '{}';
        END IF;
        
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'maintenance_counts' AND column_name = 'fire_safety_expiry_dates') THEN
            ALTER TABLE maintenance_counts ADD COLUMN fire_safety_expiry_dates JSONB DEFAULT '{}';
        END IF;
        
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'maintenance_counts' AND column_name = 'section_photos') THEN
            ALTER TABLE maintenance_counts ADD COLUMN section_photos JSONB DEFAULT '{}';
        END IF;
        
        -- Add submitted_at column if missing
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'maintenance_counts' AND column_name = 'submitted_at') THEN
            ALTER TABLE maintenance_counts ADD COLUMN submitted_at TIMESTAMP WITH TIME ZONE;
        END IF;
        
        -- Add constraint if it doesn't exist
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                      WHERE table_name = 'maintenance_counts' 
                      AND constraint_name = 'chk_maintenance_counts_status') THEN
            ALTER TABLE maintenance_counts ADD CONSTRAINT chk_maintenance_counts_status 
                CHECK (status IN ('draft', 'submitted', 'reviewed', 'completed'));
        END IF;
    ELSE
        -- Create the table if it doesn't exist
        CREATE TABLE maintenance_counts (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            
            -- Foreign keys
            school_id UUID NOT NULL,
            supervisor_id UUID NOT NULL,
            company_id UUID NOT NULL,
            
            -- Basic information
            school_name VARCHAR(255) NOT NULL,
            status VARCHAR(20) NOT NULL DEFAULT 'draft',
            
            -- Complex JSONB fields for different data types
            item_counts JSONB DEFAULT '{}',
            text_answers JSONB DEFAULT '{}',
            yes_no_answers JSONB DEFAULT '{}',
            yes_no_with_counts JSONB DEFAULT '{}',
            survey_answers JSONB DEFAULT '{}',
            maintenance_notes JSONB DEFAULT '{}',
            fire_safety_alarm_panel_data JSONB DEFAULT '{}',
            fire_safety_condition_only_data JSONB DEFAULT '{}',
            fire_safety_expiry_dates JSONB DEFAULT '{}',
            section_photos JSONB DEFAULT '{}',
            
            -- Timestamps
            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
            submitted_at TIMESTAMP WITH TIME ZONE,
            
            -- Soft delete
            deleted_at TIMESTAMP WITH TIME ZONE,
            
            -- Constraints
            CONSTRAINT fk_maintenance_counts_school FOREIGN KEY (school_id) 
                REFERENCES schools(id) ON DELETE CASCADE,
            CONSTRAINT fk_maintenance_counts_supervisor FOREIGN KEY (supervisor_id) 
                REFERENCES users(id) ON DELETE CASCADE,
            CONSTRAINT fk_maintenance_counts_company FOREIGN KEY (company_id) 
                REFERENCES companies(id) ON DELETE CASCADE,
            CONSTRAINT chk_maintenance_counts_status 
                CHECK (status IN ('draft', 'submitted', 'reviewed', 'completed'))
        );
    END IF;
END $$;

-- Create indexes only if they don't already exist
DO $$
BEGIN
    -- Basic indexes
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_maintenance_counts_school_id') THEN
        CREATE INDEX idx_maintenance_counts_school_id ON maintenance_counts(school_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_maintenance_counts_supervisor_id') THEN
        CREATE INDEX idx_maintenance_counts_supervisor_id ON maintenance_counts(supervisor_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_maintenance_counts_company_id') THEN
        CREATE INDEX idx_maintenance_counts_company_id ON maintenance_counts(company_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_maintenance_counts_status') THEN
        CREATE INDEX idx_maintenance_counts_status ON maintenance_counts(status);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_maintenance_counts_created_at') THEN
        CREATE INDEX idx_maintenance_counts_created_at ON maintenance_counts(created_at);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_maintenance_counts_submitted_at') THEN
        CREATE INDEX idx_maintenance_counts_submitted_at ON maintenance_counts(submitted_at);
    END IF;
    
    -- Composite indexes
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_maintenance_counts_school_supervisor') THEN
        CREATE INDEX idx_maintenance_counts_school_supervisor ON maintenance_counts(school_id, supervisor_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_maintenance_counts_company_status') THEN
        CREATE INDEX idx_maintenance_counts_company_status ON maintenance_counts(company_id, status);
    END IF;
    
    -- GIN indexes for JSONB - only create if item_counts column exists
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'maintenance_counts' AND column_name = 'item_counts') AND
       NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_maintenance_counts_item_counts') THEN
        CREATE INDEX idx_maintenance_counts_item_counts ON maintenance_counts USING GIN (item_counts);
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'maintenance_counts' AND column_name = 'section_photos') AND
       NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_maintenance_counts_section_photos') THEN
        CREATE INDEX idx_maintenance_counts_section_photos ON maintenance_counts USING GIN (section_photos);
    END IF;
END $$;

-- Comments for documentation
COMMENT ON TABLE maintenance_counts IS 'Stores complex maintenance inspection data with multiple JSONB fields for various data types';
COMMENT ON COLUMN maintenance_counts.item_counts IS 'Numeric counts for maintenance items (e.g., fire extinguishers, pumps)';
COMMENT ON COLUMN maintenance_counts.text_answers IS 'Text field responses (e.g., meter numbers)';
COMMENT ON COLUMN maintenance_counts.yes_no_answers IS 'Boolean yes/no responses';
COMMENT ON COLUMN maintenance_counts.yes_no_with_counts IS 'Yes/no answers with associated numeric counts';
COMMENT ON COLUMN maintenance_counts.survey_answers IS 'Survey responses with predefined options (good/needs maintenance/damaged)';
COMMENT ON COLUMN maintenance_counts.maintenance_notes IS 'Additional notes for specific maintenance items';
COMMENT ON COLUMN maintenance_counts.fire_safety_alarm_panel_data IS 'Specific data for fire alarm panels';
COMMENT ON COLUMN maintenance_counts.fire_safety_condition_only_data IS 'Condition assessment for safety equipment';
COMMENT ON COLUMN maintenance_counts.fire_safety_expiry_dates IS 'Expiry date tracking for safety equipment';
COMMENT ON COLUMN maintenance_counts.section_photos IS 'Photos organized by maintenance section';