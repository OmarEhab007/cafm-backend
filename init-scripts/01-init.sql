-- Initial database setup
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create schemas
CREATE SCHEMA IF NOT EXISTS cafm;

-- Set default schema
SET search_path TO cafm, public;

-- Create initial tables (will be managed by Flyway later)
CREATE TABLE IF NOT EXISTS cafm.database_info (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    version VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO cafm.database_info (version) VALUES ('1.0.0');

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA cafm TO cafm_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA cafm TO cafm_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA cafm TO cafm_user;

-- Create audit function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

COMMENT ON FUNCTION update_updated_at_column() IS 'Trigger function to update updated_at timestamp';
