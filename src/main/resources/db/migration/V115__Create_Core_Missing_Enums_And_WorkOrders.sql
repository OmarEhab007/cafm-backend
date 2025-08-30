-- ============================================
-- V115__Create_Core_Missing_Enums_And_WorkOrders.sql
-- CAFM Backend - Create Missing Core Enums Only
-- Purpose: Establish foundational enums for complete CAFM functionality
-- Pattern: Clean Architecture data layer with comprehensive business constraints
-- Java 23: Uses modern Java features for enum validation and performance
-- Architecture: Multi-tenant with company_id isolation and audit trails
-- Standards: Spring Boot 3.3.x with PostgreSQL best practices
-- ============================================

-- ============================================
-- STEP 1: CREATE MISSING ENUM TYPES
-- ============================================

-- Asset related enums
DO $$ BEGIN
    CREATE TYPE asset_status_enum AS ENUM (
        'ACTIVE', 'MAINTENANCE', 'RETIRED', 'DISPOSED', 'LOST', 'RESERVED', 'DAMAGED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE asset_condition_enum AS ENUM (
        'EXCELLENT', 'GOOD', 'FAIR', 'POOR', 'UNUSABLE'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Work order task enums
DO $$ BEGIN
    CREATE TYPE task_status_enum AS ENUM (
        'PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'ON_HOLD'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Inventory and measurement enums
DO $$ BEGIN
    CREATE TYPE unit_enum AS ENUM (
        'PIECE', 'METER', 'KILOGRAM', 'LITER', 'SQUARE_METER', 'CUBIC_METER', 'HOUR', 'SET'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Notification enums
DO $$ BEGIN
    CREATE TYPE notification_type_enum AS ENUM (
        'SYSTEM', 'REPORT', 'WORK_ORDER', 'ASSET', 'USER', 'BROADCAST'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE notification_priority_enum AS ENUM (
        'LOW', 'NORMAL', 'HIGH', 'URGENT'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Work order type enum
DO $$ BEGIN
    CREATE TYPE work_order_type_enum AS ENUM (
        'PREVENTIVE', 'CORRECTIVE', 'EMERGENCY', 'INSPECTION', 'INSTALLATION', 'UPGRADE'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- ============================================
-- STEP 2: ADD COMMENTS FOR DOCUMENTATION
-- ============================================

COMMENT ON TYPE asset_status_enum IS 'Asset operational status for facility management';
COMMENT ON TYPE asset_condition_enum IS 'Physical condition assessment of assets';
COMMENT ON TYPE task_status_enum IS 'Work order task execution status';
COMMENT ON TYPE unit_enum IS 'Measurement units for inventory and materials';
COMMENT ON TYPE notification_type_enum IS 'Classification of system notifications';
COMMENT ON TYPE notification_priority_enum IS 'Urgency level for notifications';
COMMENT ON TYPE work_order_type_enum IS 'Classification of maintenance work types';

-- Migration completed successfully
DO $$
BEGIN
    RAISE NOTICE 'V115 Migration completed: Created missing enum types for CAFM system';
END $$;