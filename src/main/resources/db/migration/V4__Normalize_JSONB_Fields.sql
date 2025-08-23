-- ============================================
-- V4__Normalize_JSONB_Fields.sql
-- Database Optimization Phase 1: Normalize JSONB Fields
-- Converts JSONB storage to proper relational tables
-- ============================================

-- ============================================
-- STEP 1: CREATE NORMALIZED TABLES FOR PHOTOS
-- ============================================

-- Create centralized attachments table for all photo/file storage
CREATE TABLE IF NOT EXISTS attachments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(50) NOT NULL, -- 'report', 'maintenance', 'achievement', 'damage', 'attendance'
    entity_id UUID NOT NULL,
    attachment_type VARCHAR(50) NOT NULL, -- 'before', 'after', 'during', 'completion', 'evidence'
    file_url TEXT NOT NULL,
    file_name VARCHAR(255),
    file_size BIGINT,
    mime_type VARCHAR(100),
    thumbnail_url TEXT,
    cloudinary_public_id VARCHAR(255),
    description TEXT,
    metadata JSONB DEFAULT '{}',
    uploaded_by UUID NOT NULL REFERENCES users(id),
    upload_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID REFERENCES users(id),
    
    -- Check constraints
    CONSTRAINT chk_entity_type CHECK (entity_type IN (
        'report', 'maintenance', 'achievement', 'damage', 'attendance', 'profile'
    )),
    CONSTRAINT chk_attachment_type CHECK (attachment_type IN (
        'before', 'after', 'during', 'completion', 'evidence', 
        'document', 'signature', 'avatar', 'general'
    ))
);

-- Create indexes for attachments
CREATE INDEX idx_attachments_entity ON attachments(entity_type, entity_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_attachments_uploaded_by ON attachments(uploaded_by);
CREATE INDEX idx_attachments_upload_timestamp ON attachments(upload_timestamp DESC);
CREATE INDEX idx_attachments_deleted_at ON attachments(deleted_at);

-- ============================================
-- STEP 2: CREATE MAINTENANCE ITEMS TABLE
-- ============================================

-- Create maintenance item categories
CREATE TABLE IF NOT EXISTS maintenance_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    name_ar VARCHAR(100),
    description TEXT,
    icon VARCHAR(50),
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_category_name UNIQUE (name)
);

-- Create maintenance items template
CREATE TABLE IF NOT EXISTS maintenance_item_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category_id UUID NOT NULL REFERENCES maintenance_categories(id),
    name VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255),
    unit VARCHAR(50) DEFAULT 'piece',
    default_quantity INTEGER DEFAULT 0,
    min_quantity INTEGER DEFAULT 0,
    max_quantity INTEGER DEFAULT 1000,
    is_countable BOOLEAN DEFAULT TRUE,
    requires_photo BOOLEAN DEFAULT FALSE,
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_item_name_per_category UNIQUE (category_id, name)
);

-- Create actual maintenance items (from maintenance_counts)
CREATE TABLE IF NOT EXISTS maintenance_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    maintenance_count_id UUID NOT NULL REFERENCES maintenance_counts(id) ON DELETE CASCADE,
    template_id UUID REFERENCES maintenance_item_templates(id),
    category VARCHAR(100) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    total_count INTEGER NOT NULL DEFAULT 0,
    working_count INTEGER NOT NULL DEFAULT 0,
    damaged_count INTEGER NOT NULL DEFAULT 0,
    missing_count INTEGER NOT NULL DEFAULT 0,
    notes TEXT,
    location VARCHAR(255),
    verified_by UUID REFERENCES users(id),
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Check constraints
    CONSTRAINT chk_counts_non_negative CHECK (
        total_count >= 0 AND 
        working_count >= 0 AND 
        damaged_count >= 0 AND 
        missing_count >= 0
    ),
    CONSTRAINT chk_count_totals CHECK (
        working_count + damaged_count + missing_count <= total_count
    )
);

-- Create indexes for maintenance items
CREATE INDEX idx_maintenance_items_count_id ON maintenance_items(maintenance_count_id);
CREATE INDEX idx_maintenance_items_template_id ON maintenance_items(template_id);
CREATE INDEX idx_maintenance_items_category ON maintenance_items(category);

-- ============================================
-- STEP 3: CREATE DAMAGE ITEMS TABLE
-- ============================================

CREATE TABLE IF NOT EXISTS damage_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    damage_count_id UUID NOT NULL REFERENCES damage_counts(id) ON DELETE CASCADE,
    section_key VARCHAR(100) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    damage_type VARCHAR(50),
    severity VARCHAR(20), -- 'minor', 'moderate', 'severe', 'critical'
    quantity INTEGER DEFAULT 1,
    estimated_cost DECIMAL(10, 2),
    repair_priority INTEGER DEFAULT 5, -- 1-10 scale
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Check constraints
    CONSTRAINT chk_severity CHECK (severity IN ('minor', 'moderate', 'severe', 'critical')),
    CONSTRAINT chk_repair_priority CHECK (repair_priority BETWEEN 1 AND 10)
);

-- Create indexes for damage items
CREATE INDEX idx_damage_items_count_id ON damage_items(damage_count_id);
CREATE INDEX idx_damage_items_section_key ON damage_items(section_key);
CREATE INDEX idx_damage_items_severity ON damage_items(severity);

-- ============================================
-- STEP 4: CREATE REPORT DATA TABLE
-- ============================================

-- Create structured report data table
CREATE TABLE IF NOT EXISTS report_data_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_id UUID REFERENCES reports(id) ON DELETE CASCADE,
    maintenance_report_id UUID REFERENCES maintenance_reports(id) ON DELETE CASCADE,
    field_key VARCHAR(100) NOT NULL,
    field_value TEXT,
    field_type VARCHAR(50), -- 'text', 'number', 'boolean', 'date', 'selection'
    field_metadata JSONB DEFAULT '{}',
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- At least one foreign key must be present
    CONSTRAINT chk_has_parent CHECK (
        report_id IS NOT NULL OR maintenance_report_id IS NOT NULL
    )
);

-- Create indexes for report data
CREATE INDEX idx_report_data_report_id ON report_data_items(report_id);
CREATE INDEX idx_report_data_maintenance_id ON report_data_items(maintenance_report_id);
CREATE INDEX idx_report_data_field_key ON report_data_items(field_key);

-- ============================================
-- STEP 5: CREATE SURVEY RESPONSES TABLE
-- ============================================

CREATE TABLE IF NOT EXISTS survey_responses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    maintenance_count_id UUID REFERENCES maintenance_counts(id) ON DELETE CASCADE,
    question_key VARCHAR(100) NOT NULL,
    question_text TEXT,
    response_type VARCHAR(50) NOT NULL, -- 'yes_no', 'text', 'number', 'scale', 'multiple_choice'
    response_value TEXT,
    response_metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_response_type CHECK (response_type IN (
        'yes_no', 'text', 'number', 'scale', 'multiple_choice', 'date', 'time'
    ))
);

-- Create indexes for survey responses
CREATE INDEX idx_survey_responses_count_id ON survey_responses(maintenance_count_id);
CREATE INDEX idx_survey_responses_question_key ON survey_responses(question_key);

-- ============================================
-- STEP 6: MIGRATE EXISTING JSONB DATA
-- ============================================

-- Migrate report images to attachments table (if images column exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'reports' AND column_name = 'images') THEN
        INSERT INTO attachments (entity_type, entity_id, attachment_type, file_url, uploaded_by)
        SELECT 
            'report' as entity_type,
            r.id as entity_id,
            'evidence' as attachment_type,
            jsonb_array_elements_text(r.images) as file_url,
            r.supervisor_id as uploaded_by
        FROM reports r
        WHERE r.images IS NOT NULL AND jsonb_array_length(r.images) > 0;
    END IF;
END $$;

-- Migrate completion photos to attachments table (if column exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'reports' AND column_name = 'completion_photos') THEN
        INSERT INTO attachments (entity_type, entity_id, attachment_type, file_url, uploaded_by)
        SELECT 
            'report' as entity_type,
            r.id as entity_id,
            'completion' as attachment_type,
            jsonb_array_elements_text(r.completion_photos) as file_url,
            r.supervisor_id as uploaded_by
        FROM reports r
        WHERE r.completion_photos IS NOT NULL AND jsonb_array_length(r.completion_photos) > 0;
    END IF;
END $$;

-- Migrate maintenance report photos (if table and column exist)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'maintenance_reports') 
       AND EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'maintenance_reports' AND column_name = 'photos') THEN
        INSERT INTO attachments (entity_type, entity_id, attachment_type, file_url, uploaded_by)
        SELECT 
            'maintenance' as entity_type,
            m.id as entity_id,
            'evidence' as attachment_type,
            jsonb_array_elements_text(m.photos) as file_url,
            m.supervisor_id as uploaded_by
        FROM maintenance_reports m
        WHERE m.photos IS NOT NULL AND jsonb_array_length(m.photos) > 0;
    END IF;
END $$;

-- Migrate achievement photos (if table and column exist)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'school_achievements') 
       AND EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'school_achievements' AND column_name = 'photos') THEN
        INSERT INTO attachments (entity_type, entity_id, attachment_type, file_url, uploaded_by)
        SELECT 
            'achievement' as entity_type,
            sa.id as entity_id,
            'evidence' as attachment_type,
            jsonb_array_elements_text(sa.photos) as file_url,
            sa.supervisor_id as uploaded_by
        FROM school_achievements sa
        WHERE sa.photos IS NOT NULL AND jsonb_array_length(sa.photos) > 0;
    END IF;
END $$;

-- Migrate maintenance count item data (if table and column exist)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'maintenance_counts') 
       AND EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'maintenance_counts' AND column_name = 'item_counts') THEN
        INSERT INTO maintenance_items (
            maintenance_count_id, 
            category, 
            item_name, 
            total_count, 
            working_count, 
            damaged_count, 
            missing_count
        )
        SELECT 
            mc.id as maintenance_count_id,
            COALESCE((item_data->>'category')::VARCHAR(100), 'General') as category,
            COALESCE((item_data->>'name')::VARCHAR(255), 'Unknown Item') as item_name,
            COALESCE((item_data->>'total')::INTEGER, 0) as total_count,
            COALESCE((item_data->>'working')::INTEGER, 0) as working_count,
            COALESCE((item_data->>'damaged')::INTEGER, 0) as damaged_count,
            COALESCE((item_data->>'missing')::INTEGER, 0) as missing_count
        FROM maintenance_counts mc,
            LATERAL jsonb_array_elements(
                CASE 
                    WHEN mc.item_counts IS NOT NULL AND jsonb_typeof(mc.item_counts) = 'array' 
                    THEN mc.item_counts
                    ELSE '[]'::jsonb
                END
            ) as item_data
        WHERE mc.item_counts IS NOT NULL AND jsonb_array_length(mc.item_counts) > 0;
    END IF;
END $$;

-- ============================================
-- STEP 7: CREATE HELPER FUNCTIONS
-- ============================================

-- Function to get all attachments for an entity
CREATE OR REPLACE FUNCTION get_entity_attachments(
    p_entity_type VARCHAR(50),
    p_entity_id UUID
)
RETURNS TABLE (
    id UUID,
    attachment_type VARCHAR(50),
    file_url TEXT,
    file_name VARCHAR(255),
    file_size BIGINT,
    uploaded_by UUID,
    upload_timestamp TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        a.id,
        a.attachment_type,
        a.file_url,
        a.file_name,
        a.file_size,
        a.uploaded_by,
        a.upload_timestamp
    FROM attachments a
    WHERE a.entity_type = p_entity_type
        AND a.entity_id = p_entity_id
        AND a.deleted_at IS NULL
    ORDER BY a.upload_timestamp DESC;
END;
$$ LANGUAGE plpgsql;

-- Function to calculate maintenance statistics
CREATE OR REPLACE FUNCTION calculate_maintenance_stats(
    p_school_id UUID,
    p_date_from DATE DEFAULT NULL,
    p_date_to DATE DEFAULT NULL
)
RETURNS TABLE (
    total_items BIGINT,
    working_items BIGINT,
    damaged_items BIGINT,
    missing_items BIGINT,
    damage_percentage NUMERIC(5,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        SUM(mi.total_count) as total_items,
        SUM(mi.working_count) as working_items,
        SUM(mi.damaged_count) as damaged_items,
        SUM(mi.missing_count) as missing_items,
        CASE 
            WHEN SUM(mi.total_count) > 0 
            THEN ROUND((SUM(mi.damaged_count)::NUMERIC / SUM(mi.total_count)) * 100, 2)
            ELSE 0
        END as damage_percentage
    FROM maintenance_items mi
    JOIN maintenance_counts mc ON mi.maintenance_count_id = mc.id
    WHERE mc.school_id = p_school_id
        AND (p_date_from IS NULL OR mc.created_at >= p_date_from)
        AND (p_date_to IS NULL OR mc.created_at <= p_date_to);
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 8: CREATE MATERIALIZED VIEW FOR PERFORMANCE
-- ============================================

-- Materialized view for maintenance summary by school
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_school_maintenance_summary AS
SELECT 
    mc.school_id,
    s.name as school_name,
    COUNT(DISTINCT mc.id) as total_counts,
    COUNT(DISTINCT mi.id) as total_items,
    SUM(mi.total_count) as total_inventory,
    SUM(mi.working_count) as working_inventory,
    SUM(mi.damaged_count) as damaged_inventory,
    SUM(mi.missing_count) as missing_inventory,
    CASE 
        WHEN SUM(mi.total_count) > 0 
        THEN ROUND((SUM(mi.damaged_count)::NUMERIC / SUM(mi.total_count)) * 100, 2)
        ELSE 0
    END as damage_percentage,
    MAX(mc.created_at) as last_inspection_date
FROM maintenance_counts mc
JOIN schools s ON mc.school_id = s.id
LEFT JOIN maintenance_items mi ON mi.maintenance_count_id = mc.id
GROUP BY mc.school_id, s.name;

-- Create index on materialized view
CREATE UNIQUE INDEX idx_mv_school_maintenance_school_id 
    ON mv_school_maintenance_summary(school_id);

-- ============================================
-- STEP 9: ADD DEFAULT DATA
-- ============================================

-- Insert default maintenance categories
INSERT INTO maintenance_categories (name, name_ar, icon, display_order) VALUES
    ('Electrical', 'كهرباء', 'bolt', 1),
    ('Plumbing', 'سباكة', 'water', 2),
    ('HVAC', 'تكييف', 'air', 3),
    ('Civil', 'مدني', 'building', 4),
    ('Safety', 'سلامة', 'shield', 5),
    ('Furniture', 'أثاث', 'chair', 6),
    ('IT Equipment', 'معدات تقنية', 'computer', 7),
    ('Cleaning', 'نظافة', 'broom', 8)
ON CONFLICT (name) DO NOTHING;

-- Insert sample maintenance item templates
INSERT INTO maintenance_item_templates (category_id, name, name_ar, unit, default_quantity, requires_photo)
SELECT 
    mc.id,
    template.name,
    template.name_ar,
    template.unit,
    template.default_quantity,
    template.requires_photo
FROM maintenance_categories mc
CROSS JOIN (
    VALUES 
        ('Light Bulb', 'مصباح', 'piece', 10, false),
        ('Switch', 'مفتاح', 'piece', 5, true),
        ('Socket', 'مقبس', 'piece', 5, true)
) AS template(name, name_ar, unit, default_quantity, requires_photo)
WHERE mc.name = 'Electrical'
ON CONFLICT (category_id, name) DO NOTHING;

-- ============================================
-- STEP 10: ADD TRIGGERS
-- ============================================

-- Trigger to update maintenance_items updated_at
CREATE TRIGGER update_maintenance_items_updated_at 
    BEFORE UPDATE ON maintenance_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Trigger to update damage_items updated_at
CREATE TRIGGER update_damage_items_updated_at 
    BEFORE UPDATE ON damage_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Trigger to refresh materialized view
CREATE OR REPLACE FUNCTION refresh_maintenance_summary()
RETURNS TRIGGER AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_school_maintenance_summary;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER refresh_maintenance_summary_trigger
    AFTER INSERT OR UPDATE OR DELETE ON maintenance_items
    FOR EACH STATEMENT EXECUTE FUNCTION refresh_maintenance_summary();

-- ============================================
-- STEP 11: ADD COMMENTS
-- ============================================

COMMENT ON TABLE attachments IS 'Centralized storage for all file attachments across the system';
COMMENT ON TABLE maintenance_categories IS 'Categories for maintenance items';
COMMENT ON TABLE maintenance_item_templates IS 'Templates for standardized maintenance items';
COMMENT ON TABLE maintenance_items IS 'Actual maintenance item counts from inspections';
COMMENT ON TABLE damage_items IS 'Detailed damage assessment items';
COMMENT ON TABLE report_data_items IS 'Structured data fields for reports';
COMMENT ON TABLE survey_responses IS 'Survey question responses from maintenance counts';
COMMENT ON MATERIALIZED VIEW mv_school_maintenance_summary IS 'Cached summary of maintenance data by school';

-- End of V4 Normalize JSONB Fields migration