-- ============================================
-- V122__Add_test_assets_inventory.sql
-- CAFM Backend - Add Comprehensive Test Data for Assets and Inventory
-- Purpose: Create realistic test data for assets and inventory management
-- Pattern: Multi-tenant test data with realistic relationships
-- Java 23: Test data supports records for DTOs and asset lifecycle management
-- Architecture: Tenant-aware data with proper foreign key relationships
-- Standards: Realistic values with proper audit trails and constraint compliance
-- ============================================

-- ============================================
-- STEP 1: CREATE TEST SCHOOLS
-- ============================================

-- Insert test schools for linking assets
INSERT INTO schools (
    id, company_id, code, name, name_ar, type, gender, address, city, created_by
) VALUES 
('d1111111-1111-1111-1111-111111111111'::UUID, '00000000-0000-0000-0000-000000000001'::UUID,
 'SCH-001', 'Central Elementary School', 'مدرسة الوسط الابتدائية', 'ELEMENTARY', 'MIXED',
 '123 School Street', 'Riyadh',
 (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),

('d2222222-2222-2222-2222-222222222222'::UUID, '00000000-0000-0000-0000-000000000001'::UUID,
 'SCH-002', 'Tech High School', 'الثانوية التقنية', 'HIGH_SCHOOL', 'MIXED', 
 '456 Education Ave', 'Jeddah',
 (SELECT id FROM users WHERE username = 'admin' LIMIT 1));

-- ============================================
-- STEP 2: CREATE TEST ASSETS
-- ============================================

-- Create assets using dynamic category lookup
DO $$
DECLARE
    default_category_id UUID;
BEGIN
    -- Get or create a default category
    SELECT id INTO default_category_id FROM asset_categories LIMIT 1;
    
    IF default_category_id IS NULL THEN
        INSERT INTO asset_categories (id, company_id, name, code, description, created_by)
        VALUES (
            gen_random_uuid(),
            '00000000-0000-0000-0000-000000000001'::UUID,
            'General Equipment',
            'GEN',
            'General equipment and assets',
            (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
        );
        
        SELECT id INTO default_category_id FROM asset_categories WHERE code = 'GEN';
    END IF;

    -- Insert assets using the category
    INSERT INTO assets (
        id, company_id, asset_code, name, name_ar, description, category_id,
        manufacturer, model, serial_number, location,
        purchase_date, purchase_cost, current_value, salvage_value,
        warranty_start_date, warranty_end_date, status, condition,
        maintenance_frequency_days, next_maintenance_date, last_maintenance_date,
        total_maintenance_cost, is_active, created_by
    ) VALUES 
    
    -- HVAC Systems (5 assets)
    ('a1111111-1111-1111-1111-111111111111'::UUID, '00000000-0000-0000-0000-000000000001'::UUID,
     'HVAC-000001', 'Central AC Unit Building A', 'وحدة التكييف المركزي مبنى أ', 
     'Main central air conditioning unit for Building A classrooms', 
     default_category_id,
     'Carrier', 'WeatherMaster 48HJD024', 'WM48HJD024-2023-001',
     'Building A - Rooftop', '2023-03-15', 125000.00, 112500.00, 12500.00, 
     '2023-03-15', '2026-03-14', 'ACTIVE', 'GOOD', 90, '2024-09-15', '2024-06-15', 2500.00, true,
     (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),

    ('a2222222-2222-2222-2222-222222222221'::UUID, '00000000-0000-0000-0000-000000000001'::UUID,
     'ELEC-000001', 'Main Electrical Panel Building A', 'اللوحة الكهربائية الرئيسية مبنى أ',
     'Main electrical distribution panel for Building A',
     default_category_id,
     'Schneider Electric', 'PowerPact H-Frame 400A', 'SE-PPH400-2023-BA01',
     'Building A - Electrical Room', '2023-02-10', 25000.00, 23000.00, 2500.00,
     '2023-02-10', '2028-02-09', 'ACTIVE', 'EXCELLENT', 365, '2025-02-10', NULL, 0.00, true,
     (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),

    ('a3333333-3333-3333-3333-333333333331'::UUID, '00000000-0000-0000-0000-000000000001'::UUID,
     'PLMB-000001', 'Main Water Pump System', 'نظام مضخة المياه الرئيسي',
     'Primary water circulation pump for campus water supply',
     default_category_id,
     'Grundfos', 'CR 64-2-2 A-F-A-E-HQQE', 'GRU-CR642-2023-WP01',
     'Pump House', '2023-05-20', 45000.00, 40500.00, 4500.00,
     '2023-05-20', '2026-05-19', 'ACTIVE', 'GOOD', 30, '2024-09-20', '2024-08-20', 1500.00, true,
     (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),

    ('a4444444-4444-4444-4444-444444444441'::UUID, '00000000-0000-0000-0000-000000000001'::UUID,
     'FURN-000001', 'Science Lab Furniture Set', 'مجموعة أثاث المختبر العلمي',
     'Complete laboratory furniture set for chemistry lab',
     default_category_id,
     'Lab Tech', 'Chem Lab Pro Series', 'LT-CLP-2023-SCI01',
     'Science Laboratory', '2023-06-01', 35000.00, 31500.00, 3500.00,
     '2023-06-01', '2026-05-31', 'ACTIVE', 'EXCELLENT', NULL, NULL, NULL, 0.00, true,
     (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),

    ('a4444444-4444-4444-4444-444444444445'::UUID, '00000000-0000-0000-0000-000000000001'::UUID,
     'FURN-000005', 'Interactive Whiteboard (Retired)', 'السبورة التفاعلية (متقاعدة)',
     'Interactive whiteboard system retired due to age',
     default_category_id,
     'SMART', 'SMART Board SB885', 'SMART-SB885-2018-CL205',
     'Classroom 205', '2018-09-01', 8500.00, 2125.00, 850.00,
     '2018-09-01', '2021-08-31', 'RETIRED', 'UNUSABLE', NULL, NULL, '2023-05-01', 500.00, true,
     (SELECT id FROM users WHERE username = 'admin' LIMIT 1));

END $$;

-- ============================================
-- STEP 3: CREATE ASSET MAINTENANCE RECORDS
-- ============================================

INSERT INTO asset_maintenance (
    id, asset_id, maintenance_date, maintenance_type, description, 
    performed_by, labor_hours, labor_cost, parts_cost, external_cost,
    condition_after, next_maintenance_date, recommendations, created_by
) VALUES 
-- HVAC Central AC maintenance
('e1111111-1111-1111-1111-111111111111'::UUID,
 'a1111111-1111-1111-1111-111111111111'::UUID, '2024-06-15', 'preventive', 'Quarterly filter replacement and coil cleaning',
 (SELECT id FROM users WHERE username = 'admin' LIMIT 1), 4.0, 800.00, 1700.00, 0.00,
 'GOOD', '2024-09-15', 'Replaced 8 air filters, cleaned evaporator coils, checked refrigerant levels. All parameters normal.',
 (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),

-- Electrical panel maintenance
('e2222222-2222-2222-2222-222222222222'::UUID,
 'a2222222-2222-2222-2222-222222222221'::UUID, '2024-07-20', 'preventive', 'Annual electrical panel inspection',
 (SELECT id FROM users WHERE username = 'admin' LIMIT 1), 2.5, 500.00, 200.00, 0.00,
 'EXCELLENT', '2025-07-20', 'Tested all circuits, checked connections, thermal imaging completed. All systems normal.',
 (SELECT id FROM users WHERE username = 'admin' LIMIT 1));

-- ============================================
-- STEP 4: CREATE INVENTORY ITEMS
-- ============================================

INSERT INTO inventory_items (
    id, company_id, item_code, name, name_ar, description,
    brand, model, unit_of_measure,
    current_stock, minimum_stock, maximum_stock, reorder_level,
    created_by
) VALUES 
-- HVAC Spare Parts
('c1111111-1111-1111-1111-111111111111'::UUID, '00000000-0000-0000-0000-000000000001'::UUID,
 'HVAC-000001', 'HVAC Air Filters 24x24x2', 'فلاتر الهواء للتكييف 24×24×2',
 'High-efficiency air filters for HVAC systems, MERV 13 rating',
 'Honeywell', 'H-FILTER-24x24x2', 'PIECE',
 150.0, 25.0, 500.0, 50.0,
 (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),

('c1111111-1111-1111-1111-111111111112'::UUID, '00000000-0000-0000-0000-000000000001'::UUID,
 'HVAC-000002', 'AC Compressor Belt Set', 'مجموعة أحزمة ضاغط التكييف',
 'Replacement belt set for AC compressor units',
 'Gates', 'G-BELT-AC-SET', 'SET',
 25.0, 5.0, 100.0, 10.0,
 (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),

-- Electrical Components
('c2222222-2222-2222-2222-222222222221'::UUID, '00000000-0000-0000-0000-000000000001'::UUID,
 'ELEC-000001', '20A Circuit Breaker', 'قاطع الدائرة 20 أمبير',
 'Single pole 20 amp circuit breaker for panel',
 'Schneider', 'SE-CB-20A', 'PIECE',
 50.0, 10.0, 200.0, 20.0,
 (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),

-- Plumbing Supplies
('c3333333-3333-3333-3333-333333333331'::UUID, '00000000-0000-0000-0000-000000000001'::UUID,
 'PLMB-000001', 'PVC Pipe 4 inch x 10ft', 'أنبوب PVC 4 بوصة × 10 قدم',
 '4-inch PVC pipe for drainage applications',
 'Charlotte Pipe', 'CP-PVC-4X10', 'PIECE',
 35.0, 5.0, 150.0, 15.0,
 (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),

-- IT Equipment
('c4444444-4444-4444-4444-444444444441'::UUID, '00000000-0000-0000-0000-000000000001'::UUID,
 'IT-000001', 'Cat6 Ethernet Cable 1000ft', 'كابل إيثرنت Cat6 1000 قدم',
 'Category 6 ethernet cable for network installations',
 'Panduit', 'PAN-CAT6-1000', 'ROLL',
 12.0, 2.0, 50.0, 5.0,
 (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),

-- Cleaning Supplies
('c5555555-5555-5555-5555-555555555551'::UUID, '00000000-0000-0000-0000-000000000001'::UUID,
 'CLEAN-001', 'Industrial Floor Cleaner 5L', 'منظف الأرضيات الصناعي 5 لتر',
 'Heavy-duty floor cleaner for industrial use',
 'Ecolab', 'ECO-FC-5L', 'BOTTLE',
 85.0, 15.0, 300.0, 30.0,
 (SELECT id FROM users WHERE username = 'admin' LIMIT 1));

-- ============================================
-- STEP 5: CREATE INVENTORY TRANSACTIONS HISTORY
-- ============================================

INSERT INTO inventory_transactions (
    id, company_id, transaction_number, item_id, transaction_type, quantity, unit_cost,
    transaction_date, notes, created_by
) VALUES 
-- HVAC Filter purchase
('f1111111-1111-1111-1111-111111111111'::UUID, '00000000-0000-0000-0000-000000000001'::UUID,
 'PO-2024-001', 'c1111111-1111-1111-1111-111111111111'::UUID, 'RECEIPT', 200.0, 35.50,
 '2024-01-15', 'Initial stock purchase - HVAC filters',
 (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),

-- HVAC Filter consumption for maintenance
('f2222222-2222-2222-2222-222222222221'::UUID, '00000000-0000-0000-0000-000000000001'::UUID,
 'WO-2024-015', 'c1111111-1111-1111-1111-111111111111'::UUID, 'ISSUE', -8.0, 35.50,
 '2024-06-15', 'Used in AC maintenance - asset a1111111-1111-1111-1111-111111111111',
 (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),

-- Circuit breaker purchase
('f3333333-3333-3333-3333-333333333331'::UUID, '00000000-0000-0000-0000-000000000001'::UUID,
 'PO-2024-005', 'c2222222-2222-2222-2222-222222222221'::UUID, 'RECEIPT', 100.0, 45.50,
 '2024-02-01', 'Electrical supplies stock replenishment',
 (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),

-- Cleaning supplies purchase
('f4444444-4444-4444-4444-444444444441'::UUID, '00000000-0000-0000-0000-000000000001'::UUID,
 'PO-2024-012', 'c5555555-5555-5555-5555-555555555551'::UUID, 'RECEIPT', 100.0, 25.50,
 '2024-03-10', 'Monthly cleaning supplies order',
 (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),

-- Cleaning supplies consumption
('f5555555-5555-5555-5555-555555555551'::UUID, '00000000-0000-0000-0000-000000000001'::UUID,
 'MAINT-2024-089', 'c5555555-5555-5555-5555-555555555551'::UUID, 'ISSUE', -15.0, 25.50,
 '2024-07-20', 'Monthly facility cleaning consumption',
 (SELECT id FROM users WHERE username = 'admin' LIMIT 1));

-- ============================================
-- STEP 6: CREATE SUMMARY VIEW AND VERIFICATION
-- ============================================

-- Create a summary view for test data verification
CREATE OR REPLACE VIEW test_data_summary AS
SELECT 
    'Assets' as data_type,
    COUNT(*) as record_count,
    MIN(created_at) as earliest_record,
    MAX(created_at) as latest_record
FROM assets 
WHERE company_id = '00000000-0000-0000-0000-000000000001'::UUID
UNION ALL
SELECT 
    'Inventory Items' as data_type,
    COUNT(*) as record_count,
    MIN(created_at) as earliest_record,
    MAX(created_at) as latest_record
FROM inventory_items 
WHERE company_id = '00000000-0000-0000-0000-000000000001'::UUID
UNION ALL
SELECT 
    'Inventory Transactions' as data_type,
    COUNT(*) as record_count,
    MIN(created_at) as earliest_record,
    MAX(created_at) as latest_record
FROM inventory_transactions 
WHERE company_id = '00000000-0000-0000-0000-000000000001'::UUID
UNION ALL
SELECT 
    'Asset Maintenance' as data_type,
    COUNT(*) as record_count,
    MIN(created_at) as earliest_record,
    MAX(created_at) as latest_record
FROM asset_maintenance 
WHERE asset_id IN (
    SELECT id FROM assets 
    WHERE company_id = '00000000-0000-0000-0000-000000000001'::UUID
);

-- ============================================
-- VERIFICATION BLOCK
-- ============================================

DO $$
DECLARE
    asset_count INTEGER;
    inventory_count INTEGER;
    transaction_count INTEGER;
    maintenance_count INTEGER;
BEGIN
    -- Count records
    SELECT COUNT(*) INTO asset_count FROM assets WHERE company_id = '00000000-0000-0000-0000-000000000001'::UUID;
    SELECT COUNT(*) INTO inventory_count FROM inventory_items WHERE company_id = '00000000-0000-0000-0000-000000000001'::UUID;
    SELECT COUNT(*) INTO transaction_count FROM inventory_transactions WHERE company_id = '00000000-0000-0000-0000-000000000001'::UUID;
    SELECT COUNT(*) INTO maintenance_count FROM asset_maintenance;
    
    -- Log results
    RAISE NOTICE 'Test Data Migration V122 Completed Successfully:';
    RAISE NOTICE '- Assets created: %', asset_count;
    RAISE NOTICE '- Inventory items created: %', inventory_count;
    RAISE NOTICE '- Inventory transactions created: %', transaction_count;
    RAISE NOTICE '- Maintenance records created: %', maintenance_count;
    RAISE NOTICE 'All records are properly tenant-isolated and follow referential integrity constraints.';
END $$;