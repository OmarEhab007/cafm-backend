-- ================================
-- V18: Update School Achievements Tables
-- ================================
-- Updates existing school_achievements table from V2 with additional columns
-- Creates new achievement_photo_metadata table for detailed photo metadata
-- Note: achievement_photos table already exists from V1 with different structure

DO $$
BEGIN
    -- Update existing school_achievements table with missing columns
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'school_achievements') THEN
        
        -- Add company_id if missing (should be added by V14 but check anyway)
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'school_achievements' AND column_name = 'company_id') THEN
            ALTER TABLE school_achievements ADD COLUMN company_id UUID;
            UPDATE school_achievements sa SET company_id = u.company_id
            FROM users u WHERE sa.supervisor_id = u.id;
            ALTER TABLE school_achievements ALTER COLUMN company_id SET NOT NULL;
            ALTER TABLE school_achievements ADD CONSTRAINT fk_school_achievements_company 
                FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;
        END IF;
        
        -- Add category column if missing
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'school_achievements' AND column_name = 'category') THEN
            ALTER TABLE school_achievements ADD COLUMN category VARCHAR(100);
        END IF;
        
        -- Add approval workflow columns if missing
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'school_achievements' AND column_name = 'approved_at') THEN
            ALTER TABLE school_achievements ADD COLUMN approved_at TIMESTAMP WITH TIME ZONE;
        END IF;
        
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'school_achievements' AND column_name = 'approved_by') THEN
            ALTER TABLE school_achievements ADD COLUMN approved_by UUID;
            ALTER TABLE school_achievements ADD CONSTRAINT fk_school_achievements_approved_by 
                FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL;
        END IF;
        
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'school_achievements' AND column_name = 'approval_notes') THEN
            ALTER TABLE school_achievements ADD COLUMN approval_notes TEXT;
        END IF;
        
        -- Update status constraint to include more statuses
        IF EXISTS (SELECT 1 FROM information_schema.table_constraints 
                  WHERE table_name = 'school_achievements' 
                  AND constraint_name = 'chk_school_achievement_status') THEN
            ALTER TABLE school_achievements DROP CONSTRAINT chk_school_achievement_status;
        END IF;
        ALTER TABLE school_achievements ADD CONSTRAINT chk_school_achievement_status 
            CHECK (status IN ('draft', 'submitted', 'approved', 'rejected'));
            
    ELSE
        -- Create the table if it doesn't exist (shouldn't happen as V2 creates it)
        CREATE TABLE school_achievements (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
            school_name VARCHAR(255) NOT NULL,
            supervisor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
            achievement_type VARCHAR(50) NOT NULL,
            status VARCHAR(20) NOT NULL DEFAULT 'draft',
            photos JSONB DEFAULT '[]',
            notes TEXT,
            category VARCHAR(100),
            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
            submitted_at TIMESTAMP WITH TIME ZONE,
            approved_at TIMESTAMP WITH TIME ZONE,
            approved_by UUID REFERENCES users(id) ON DELETE SET NULL,
            approval_notes TEXT,
            deleted_at TIMESTAMP WITH TIME ZONE,
            CONSTRAINT chk_school_achievement_type 
                CHECK (achievement_type IN ('maintenance_achievement', 'ac_achievement', 'checklist')),
            CONSTRAINT chk_school_achievement_status 
                CHECK (status IN ('draft', 'submitted', 'approved', 'rejected'))
        );
    END IF;
END $$;

-- Create new table for detailed photo metadata
-- Note: achievement_photos already exists from V1 with different structure
-- So we create achievement_photo_metadata instead
CREATE TABLE IF NOT EXISTS achievement_photo_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    achievement_id UUID NOT NULL,
    photo_url TEXT NOT NULL,
    photo_description TEXT,
    file_size BIGINT,
    mime_type VARCHAR(100),
    width INTEGER,
    height INTEGER,
    upload_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_achievement_photo_metadata_achievement FOREIGN KEY (achievement_id) 
        REFERENCES school_achievements(id) ON DELETE CASCADE
);

-- Create indexes only if they don't exist
DO $$
BEGIN
    -- Indexes for school_achievements
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_school_achievements_school_id') THEN
        CREATE INDEX idx_school_achievements_school_id ON school_achievements(school_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_school_achievements_supervisor_id') THEN
        CREATE INDEX idx_school_achievements_supervisor_id ON school_achievements(supervisor_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_school_achievements_company_id') THEN
        CREATE INDEX idx_school_achievements_company_id ON school_achievements(company_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_school_achievements_type') THEN
        CREATE INDEX idx_school_achievements_type ON school_achievements(achievement_type);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_school_achievements_status') THEN
        CREATE INDEX idx_school_achievements_status ON school_achievements(status);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_school_achievements_created_at') THEN
        CREATE INDEX idx_school_achievements_created_at ON school_achievements(created_at);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_school_achievements_submitted_at') THEN
        CREATE INDEX idx_school_achievements_submitted_at ON school_achievements(submitted_at);
    END IF;
    
    -- New indexes for approval workflow
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'school_achievements' AND column_name = 'approved_at') AND
       NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_school_achievements_approved_at') THEN
        CREATE INDEX idx_school_achievements_approved_at ON school_achievements(approved_at);
    END IF;
    
    -- Composite indexes
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_school_achievements_school_type') THEN
        CREATE INDEX idx_school_achievements_school_type ON school_achievements(school_id, achievement_type);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_school_achievements_type_status') THEN
        CREATE INDEX idx_school_achievements_type_status ON school_achievements(achievement_type, status);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_school_achievements_company_status') THEN
        CREATE INDEX idx_school_achievements_company_status ON school_achievements(company_id, status);
    END IF;
    
    -- GIN index for photos JSONB
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'school_achievements' AND column_name = 'photos') AND
       NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_school_achievements_photos_gin') THEN
        CREATE INDEX idx_school_achievements_photos_gin ON school_achievements USING GIN (photos);
    END IF;
    
    -- Indexes for achievement_photo_metadata
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_achievement_photo_metadata_achievement_id') THEN
        CREATE INDEX idx_achievement_photo_metadata_achievement_id ON achievement_photo_metadata(achievement_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_achievement_photo_metadata_upload_timestamp') THEN
        CREATE INDEX idx_achievement_photo_metadata_upload_timestamp ON achievement_photo_metadata(upload_timestamp);
    END IF;
END $$;

-- Comments
COMMENT ON TABLE school_achievements IS 'Stores photo submissions for school maintenance achievements and checklists';
COMMENT ON COLUMN school_achievements.achievement_type IS 'Type of achievement: maintenance_achievement, ac_achievement, or checklist';
COMMENT ON COLUMN school_achievements.photos IS 'JSONB array containing photo URLs';
COMMENT ON COLUMN school_achievements.category IS 'Additional categorization for achievements';
COMMENT ON COLUMN school_achievements.approved_at IS 'Timestamp when achievement was approved';
COMMENT ON COLUMN school_achievements.approved_by IS 'User who approved the achievement';
COMMENT ON COLUMN school_achievements.approval_notes IS 'Notes from approver';
COMMENT ON TABLE achievement_photo_metadata IS 'Detailed metadata for each achievement photo';