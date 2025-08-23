-- PostgreSQL enum type initialization for tests
-- This script creates the necessary enum types for testing

-- Create user_type enum if it doesn't exist
DO $$ BEGIN
    CREATE TYPE user_type AS ENUM (
        'VIEWER',
        'TECHNICIAN', 
        'SUPERVISOR',
        'ADMIN',
        'SUPER_ADMIN'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Create user_status enum if it doesn't exist
DO $$ BEGIN
    CREATE TYPE user_status AS ENUM (
        'PENDING_VERIFICATION',
        'ACTIVE',
        'INACTIVE', 
        'SUSPENDED',
        'LOCKED',
        'ARCHIVED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Create company_status enum if it doesn't exist
DO $$ BEGIN
    CREATE TYPE company_status AS ENUM (
        'ACTIVE',
        'INACTIVE',
        'SUSPENDED',
        'ARCHIVED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Create other enum types that might be needed
DO $$ BEGIN
    CREATE TYPE subscription_plan AS ENUM (
        'FREE',
        'BASIC',
        'PREMIUM',
        'ENTERPRISE'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;