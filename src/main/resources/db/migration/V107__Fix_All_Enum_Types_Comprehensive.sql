-- ============================================
-- V107__Fix_All_Enum_Types_Comprehensive.sql
-- Fix all PostgreSQL enum types to use uppercase values
-- Purpose: Standardize all enum types to uppercase for Java compatibility
-- ============================================

-- ============================================
-- STEP 1: Drop dependent views, functions, and materialized views
-- ============================================
DROP VIEW IF EXISTS work_order_summary CASCADE;
DROP VIEW IF EXISTS inventory_transaction_summary CASCADE;
DROP VIEW IF EXISTS active_work_orders CASCADE;
DROP VIEW IF EXISTS pending_work_orders CASCADE;
DROP VIEW IF EXISTS inventory_levels CASCADE;

-- Drop view that depends on work_order status
DROP VIEW IF EXISTS school_overview CASCADE;

-- Drop materialized views that depend on user_type and work_order status
DROP MATERIALIZED VIEW IF EXISTS mv_company_dashboard_stats CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_supervisor_workload CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_school_performance CASCADE;

-- Drop trigger function that depends on user_type
DROP FUNCTION IF EXISTS validate_user_type_change() CASCADE;

-- ============================================
-- STEP 2: Temporarily disable user-defined triggers
-- ============================================
-- Disable user-defined triggers for users table
DO $$
DECLARE
    trigger_name TEXT;
BEGIN
    FOR trigger_name IN 
        SELECT tgname FROM pg_trigger 
        WHERE tgrelid = 'users'::regclass 
        AND NOT tgisinternal 
        AND tgname NOT LIKE 'RI_ConstraintTrigger%'
    LOOP
        EXECUTE format('ALTER TABLE users DISABLE TRIGGER %I', trigger_name);
    END LOOP;
END $$;

-- Disable user-defined triggers for work_orders table (if exists)
DO $$
DECLARE
    trigger_name TEXT;
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'work_orders') THEN
        FOR trigger_name IN 
            SELECT tgname FROM pg_trigger 
            WHERE tgrelid = 'work_orders'::regclass 
            AND NOT tgisinternal 
            AND tgname NOT LIKE 'RI_ConstraintTrigger%'
        LOOP
            EXECUTE format('ALTER TABLE work_orders DISABLE TRIGGER %I', trigger_name);
        END LOOP;
    END IF;
END $$;

-- Disable user-defined triggers for inventory_transactions table (if exists)
DO $$
DECLARE
    trigger_name TEXT;
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'inventory_transactions') THEN
        FOR trigger_name IN 
            SELECT tgname FROM pg_trigger 
            WHERE tgrelid = 'inventory_transactions'::regclass 
            AND NOT tgisinternal 
            AND tgname NOT LIKE 'RI_ConstraintTrigger%'
        LOOP
            EXECUTE format('ALTER TABLE inventory_transactions DISABLE TRIGGER %I', trigger_name);
        END LOOP;
    END IF;
END $$;

-- ============================================
-- STEP 3: Fix user_type enum
-- ============================================
-- Add temporary column
ALTER TABLE users ADD COLUMN user_type_new VARCHAR(50);

-- Convert existing values to uppercase
UPDATE users SET user_type_new = CASE user_type::text
    WHEN 'viewer' THEN 'VIEWER'
    WHEN 'technician' THEN 'TECHNICIAN'
    WHEN 'supervisor' THEN 'SUPERVISOR'
    WHEN 'admin' THEN 'ADMIN'
    WHEN 'super_admin' THEN 'SUPER_ADMIN'
    ELSE UPPER(user_type::text)
END;

-- Drop old column and type
ALTER TABLE users DROP COLUMN user_type;
DROP TYPE IF EXISTS user_type CASCADE;

-- Create new type with uppercase values
CREATE TYPE user_type AS ENUM (
    'VIEWER', 'TECHNICIAN', 'SUPERVISOR', 'ADMIN', 'SUPER_ADMIN'
);

-- Add column with new type
ALTER TABLE users ADD COLUMN user_type user_type;
UPDATE users SET user_type = user_type_new::user_type;
ALTER TABLE users ALTER COLUMN user_type SET NOT NULL;
ALTER TABLE users ALTER COLUMN user_type SET DEFAULT 'VIEWER'::user_type;

-- Drop temporary column
ALTER TABLE users DROP COLUMN user_type_new;

-- ============================================
-- STEP 4: Fix work_order_status enum
-- ============================================
-- Add temporary column
ALTER TABLE work_orders ADD COLUMN status_new VARCHAR(50);

-- Convert existing values to uppercase
UPDATE work_orders SET status_new = CASE status::text
    WHEN 'draft' THEN 'DRAFT'
    WHEN 'pending' THEN 'PENDING'
    WHEN 'assigned' THEN 'ASSIGNED'
    WHEN 'in_progress' THEN 'IN_PROGRESS'
    WHEN 'on_hold' THEN 'ON_HOLD'
    WHEN 'completed' THEN 'COMPLETED'
    WHEN 'cancelled' THEN 'CANCELLED'
    WHEN 'verified' THEN 'VERIFIED'
    ELSE UPPER(status::text)
END;

-- Drop old column and type
ALTER TABLE work_orders DROP COLUMN status;
DROP TYPE IF EXISTS work_order_status CASCADE;

-- Create new type with uppercase values
CREATE TYPE work_order_status AS ENUM (
    'DRAFT', 'PENDING', 'ASSIGNED', 'IN_PROGRESS',
    'ON_HOLD', 'COMPLETED', 'CANCELLED', 'VERIFIED'
);

-- Add column with new type
ALTER TABLE work_orders ADD COLUMN status work_order_status;
UPDATE work_orders SET status = status_new::work_order_status;
ALTER TABLE work_orders ALTER COLUMN status SET NOT NULL;
ALTER TABLE work_orders ALTER COLUMN status SET DEFAULT 'PENDING'::work_order_status;

-- Drop temporary column
ALTER TABLE work_orders DROP COLUMN status_new;

-- ============================================
-- STEP 5: Fix work_order_priority enum
-- ============================================
-- Add temporary column
ALTER TABLE work_orders ADD COLUMN priority_new VARCHAR(50);

-- Convert existing values to uppercase
UPDATE work_orders SET priority_new = CASE priority::text
    WHEN 'emergency' THEN 'EMERGENCY'
    WHEN 'high' THEN 'HIGH'
    WHEN 'medium' THEN 'MEDIUM'
    WHEN 'low' THEN 'LOW'
    WHEN 'scheduled' THEN 'SCHEDULED'
    ELSE UPPER(priority::text)
END;

-- Drop old column and type
ALTER TABLE work_orders DROP COLUMN priority;
DROP TYPE IF EXISTS work_order_priority CASCADE;

-- Create new type with uppercase values
CREATE TYPE work_order_priority AS ENUM (
    'EMERGENCY', 'HIGH', 'MEDIUM', 'LOW', 'SCHEDULED'
);

-- Add column with new type
ALTER TABLE work_orders ADD COLUMN priority work_order_priority;
UPDATE work_orders SET priority = priority_new::work_order_priority;
ALTER TABLE work_orders ALTER COLUMN priority SET NOT NULL;
ALTER TABLE work_orders ALTER COLUMN priority SET DEFAULT 'MEDIUM'::work_order_priority;

-- Drop temporary column
ALTER TABLE work_orders DROP COLUMN priority_new;

-- ============================================
-- STEP 8: Fix inventory_transaction_type enum
-- ============================================
-- Add temporary column
ALTER TABLE inventory_transactions ADD COLUMN transaction_type_new VARCHAR(50);

-- Convert existing values to uppercase
UPDATE inventory_transactions SET transaction_type_new = CASE transaction_type::text
    WHEN 'receipt' THEN 'RECEIPT'
    WHEN 'issue' THEN 'ISSUE'
    WHEN 'adjustment' THEN 'ADJUSTMENT'
    WHEN 'transfer' THEN 'TRANSFER'
    WHEN 'return' THEN 'RETURN'
    WHEN 'disposal' THEN 'DISPOSAL'
    WHEN 'damage' THEN 'DAMAGE'
    WHEN 'stock_check' THEN 'STOCK_CHECK'
    ELSE UPPER(transaction_type::text)
END;

-- Drop old column and type
ALTER TABLE inventory_transactions DROP COLUMN transaction_type;
DROP TYPE IF EXISTS inventory_transaction_type CASCADE;

-- Create new type with uppercase values
CREATE TYPE inventory_transaction_type AS ENUM (
    'RECEIPT', 'ISSUE', 'ADJUSTMENT', 'TRANSFER',
    'RETURN', 'DISPOSAL', 'DAMAGE', 'STOCK_CHECK'
);

-- Add column with new type
ALTER TABLE inventory_transactions ADD COLUMN transaction_type inventory_transaction_type;
UPDATE inventory_transactions SET transaction_type = transaction_type_new::inventory_transaction_type;
ALTER TABLE inventory_transactions ALTER COLUMN transaction_type SET NOT NULL;

-- Drop temporary column
ALTER TABLE inventory_transactions DROP COLUMN transaction_type_new;

-- ============================================
-- STEP 8: Re-enable triggers
-- ============================================
-- Re-enable user-defined triggers for all tables
DO $$
DECLARE
    trigger_name TEXT;
    table_name TEXT;
BEGIN
    -- Re-enable triggers for users table
    FOR trigger_name IN 
        SELECT tgname FROM pg_trigger 
        WHERE tgrelid = 'users'::regclass 
        AND NOT tgisinternal 
        AND tgname NOT LIKE 'RI_ConstraintTrigger%'
        AND tgenabled = 'D'
    LOOP
        EXECUTE format('ALTER TABLE users ENABLE TRIGGER %I', trigger_name);
    END LOOP;
    
    -- Re-enable triggers for work_orders table (if exists)
    IF EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'work_orders') THEN
        FOR trigger_name IN 
            SELECT tgname FROM pg_trigger 
            WHERE tgrelid = 'work_orders'::regclass 
            AND NOT tgisinternal 
            AND tgname NOT LIKE 'RI_ConstraintTrigger%'
            AND tgenabled = 'D'
        LOOP
            EXECUTE format('ALTER TABLE work_orders ENABLE TRIGGER %I', trigger_name);
        END LOOP;
    END IF;
    
    -- Re-enable triggers for inventory_transactions table (if exists)
    IF EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'inventory_transactions') THEN
        FOR trigger_name IN 
            SELECT tgname FROM pg_trigger 
            WHERE tgrelid = 'inventory_transactions'::regclass 
            AND NOT tgisinternal 
            AND tgname NOT LIKE 'RI_ConstraintTrigger%'
            AND tgenabled = 'D'
        LOOP
            EXECUTE format('ALTER TABLE inventory_transactions ENABLE TRIGGER %I', trigger_name);
        END LOOP;
    END IF;
END $$;

-- ============================================
-- STEP 9: Add indexes for performance
-- ============================================
CREATE INDEX IF NOT EXISTS idx_users_user_type ON users(user_type);
CREATE INDEX IF NOT EXISTS idx_work_orders_status ON work_orders(status);
CREATE INDEX IF NOT EXISTS idx_work_orders_priority ON work_orders(priority);
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_type ON inventory_transactions(transaction_type);

-- ============================================
-- STEP 10: Update any functions that use these enums
-- ============================================
-- Drop and recreate functions if they exist (placeholder for actual functions)
-- Example: Update any stored procedures that reference these enums

-- ============================================
-- STEP 11: Add comments for documentation
-- ============================================
COMMENT ON TYPE user_type IS 'User role types (uppercase values for Java compatibility)';
COMMENT ON TYPE work_order_status IS 'Work order lifecycle status (uppercase values for Java compatibility)';
COMMENT ON TYPE work_order_priority IS 'Work order priority levels (uppercase values for Java compatibility)';
COMMENT ON TYPE inventory_transaction_type IS 'Inventory transaction types (uppercase values for Java compatibility)';

-- ============================================
-- STEP 12: Verify the migration
-- ============================================
DO $$
DECLARE
    v_count INTEGER;
BEGIN
    -- Check users
    SELECT COUNT(*) INTO v_count FROM users WHERE user_type IS NULL;
    IF v_count > 0 THEN
        RAISE EXCEPTION 'Migration failed: % users have NULL user_type', v_count;
    END IF;
    
    -- Check work_orders
    SELECT COUNT(*) INTO v_count FROM work_orders WHERE status IS NULL;
    IF v_count > 0 THEN
        RAISE EXCEPTION 'Migration failed: % work_orders have NULL status', v_count;
    END IF;
    
    SELECT COUNT(*) INTO v_count FROM work_orders WHERE priority IS NULL;
    IF v_count > 0 THEN
        RAISE EXCEPTION 'Migration failed: % work_orders have NULL priority', v_count;
    END IF;
    
    -- Check inventory_transactions
    SELECT COUNT(*) INTO v_count FROM inventory_transactions WHERE transaction_type IS NULL;
    IF v_count > 0 THEN
        RAISE EXCEPTION 'Migration failed: % inventory_transactions have NULL transaction_type', v_count;
    END IF;
    
    RAISE NOTICE 'Migration V107 completed successfully';
END $$;

-- End of migration