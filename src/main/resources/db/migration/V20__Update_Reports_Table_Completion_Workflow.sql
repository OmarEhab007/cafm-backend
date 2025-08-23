-- ================================
-- V20: Update Reports Table for Completion Workflow
-- ================================
-- Add missing fields for report completion with photos and notes
-- Align with Flutter app requirements

-- Add completion-related columns if they don't exist
ALTER TABLE reports 
ADD COLUMN IF NOT EXISTS completion_photos JSONB DEFAULT '[]',
ADD COLUMN IF NOT EXISTS completion_note TEXT,
ADD COLUMN IF NOT EXISTS closed_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN IF NOT EXISTS report_source VARCHAR(50) DEFAULT 'mobile',
ADD COLUMN IF NOT EXISTS scheduled_date TIMESTAMP WITH TIME ZONE;

-- Add missing enum values to report_status if they don't exist
DO $$
BEGIN
    -- Note: Enum values in PostgreSQL are case-sensitive
    -- The existing enum uses uppercase values
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'draft' AND enumtypid = 'report_status'::regtype) THEN
        ALTER TYPE report_status ADD VALUE 'draft';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'submitted' AND enumtypid = 'report_status'::regtype) THEN
        ALTER TYPE report_status ADD VALUE 'submitted';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'in_review' AND enumtypid = 'report_status'::regtype) THEN
        ALTER TYPE report_status ADD VALUE 'in_review';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'approved' AND enumtypid = 'report_status'::regtype) THEN
        ALTER TYPE report_status ADD VALUE 'approved';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'rejected' AND enumtypid = 'report_status'::regtype) THEN
        ALTER TYPE report_status ADD VALUE 'rejected';
    END IF;
    -- pending, in_progress, completed, late, late_completed, cancelled already added in V2
END $$;

-- Update priority enum to add missing values
DO $$
BEGIN
    -- Check if priority column is enum or varchar
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'priority_level') THEN
        -- priority_level enum already has LOW, MEDIUM, HIGH, CRITICAL, URGENT
        -- Add any missing values if needed
        IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'emergency' AND enumtypid = 'priority_level'::regtype) THEN
            ALTER TYPE priority_level ADD VALUE 'emergency';
        END IF;
    END IF;
END $$;

-- Add type constraint if type column exists and doesn't have constraint
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_name = 'reports' AND column_name = 'type') THEN
        -- Drop existing constraint if any
        ALTER TABLE reports DROP CONSTRAINT IF EXISTS chk_report_type;
        
        -- Add new constraint with all possible types
        ALTER TABLE reports ADD CONSTRAINT chk_report_type 
            CHECK (type IN (
                'maintenance', 
                'emergency', 
                'routine', 
                'inspection',
                'plumbing',
                'electrical',
                'civil',
                'hvac',
                'safety',
                'other'
            ));
    END IF;
END $$;

-- Create trigger for automatically updating late status
CREATE OR REPLACE FUNCTION update_late_report_status()
RETURNS TRIGGER AS $$
BEGIN
    -- Mark reports as late if they're past due
    IF NEW.scheduled_date IS NOT NULL 
       AND NEW.scheduled_date < CURRENT_TIMESTAMP 
       AND NEW.status IN ('pending', 'PENDING', 'in_progress', 'IN_PROGRESS') THEN
        NEW.status := 'late';
    END IF;
    
    -- Mark completed late reports as late_completed
    IF NEW.status IN ('completed', 'COMPLETED')
       AND OLD.status IN ('late', 'LATE') THEN
        NEW.status := 'late_completed';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'update_report_late_status') THEN
        CREATE TRIGGER update_report_late_status
            BEFORE UPDATE ON reports
            FOR EACH ROW
            EXECUTE FUNCTION update_late_report_status();
    END IF;
END $$;

-- Create indexes for new columns
CREATE INDEX IF NOT EXISTS idx_reports_scheduled_date ON reports(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_reports_closed_at ON reports(closed_at);
CREATE INDEX IF NOT EXISTS idx_reports_report_source ON reports(report_source);

-- GIN index for completion_photos JSONB
CREATE INDEX IF NOT EXISTS idx_reports_completion_photos ON reports USING GIN (completion_photos);

-- Comments
COMMENT ON COLUMN reports.completion_photos IS 'JSONB array of photo URLs taken when completing the report';
COMMENT ON COLUMN reports.completion_note IS 'Note added by technician when completing the report';
COMMENT ON COLUMN reports.closed_at IS 'Timestamp when report was closed/completed';
COMMENT ON COLUMN reports.report_source IS 'Source of the report: mobile, web, api, system';
COMMENT ON COLUMN reports.scheduled_date IS 'Scheduled date for maintenance work';