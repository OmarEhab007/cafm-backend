-- ============================================================================
-- V112: Security Tables for API Keys and Audit Logging
-- ============================================================================
-- Purpose: Create tables for API key authentication and audit logging
-- Author: CAFM System
-- Date: 2025-01-15
-- ============================================================================

-- ============================================================================
-- API Keys Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key_name VARCHAR(255) NOT NULL UNIQUE,
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    key_prefix VARCHAR(8) NOT NULL,
    description TEXT,
    company_id UUID NOT NULL REFERENCES companies(id),
    is_active BOOLEAN DEFAULT true,
    expires_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    last_used_ip VARCHAR(45),
    rate_limit_tier VARCHAR(20) DEFAULT 'STANDARD',
    allowed_ips TEXT,
    usage_count BIGINT DEFAULT 0,
    created_by_user_id UUID,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_by_user_id UUID,
    revoke_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT fk_api_key_company FOREIGN KEY (company_id) REFERENCES companies(id)
);

-- Indexes for API keys
CREATE INDEX idx_api_keys_prefix ON api_keys(key_prefix) WHERE is_active = true;
CREATE INDEX idx_api_keys_company ON api_keys(company_id);
CREATE INDEX idx_api_keys_expires ON api_keys(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX idx_api_keys_last_used ON api_keys(last_used_at);

-- ============================================================================
-- API Key Scopes Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS api_key_scopes (
    api_key_id UUID NOT NULL,
    scope VARCHAR(50) NOT NULL,
    PRIMARY KEY (api_key_id, scope),
    CONSTRAINT fk_api_key_scope FOREIGN KEY (api_key_id) REFERENCES api_keys(id) ON DELETE CASCADE
);

-- ============================================================================
-- Audit Logs Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id UUID,
    username VARCHAR(255),
    company_id UUID,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    entity_name VARCHAR(255),
    old_values TEXT,
    new_values TEXT,
    changes TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    request_id VARCHAR(36),
    session_id VARCHAR(255),
    status VARCHAR(20) DEFAULT 'SUCCESS',
    error_message TEXT,
    duration_ms BIGINT,
    api_endpoint VARCHAR(255),
    http_method VARCHAR(10),
    response_code INTEGER
);

-- Indexes for audit logs
CREATE INDEX idx_audit_entity_action ON audit_logs(entity_type, action);
CREATE INDEX idx_audit_user_timestamp ON audit_logs(user_id, timestamp DESC);
CREATE INDEX idx_audit_company_timestamp ON audit_logs(company_id, timestamp DESC);
CREATE INDEX idx_audit_entity_id ON audit_logs(entity_id);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_status ON audit_logs(status) WHERE status != 'SUCCESS';

-- ============================================================================
-- Rate Limit Buckets Table (for persistent rate limiting)
-- ============================================================================
CREATE TABLE IF NOT EXISTS rate_limit_buckets (
    id VARCHAR(255) PRIMARY KEY,
    tokens BIGINT NOT NULL,
    last_refill TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE
);

-- Index for cleanup
CREATE INDEX idx_rate_limit_expires ON rate_limit_buckets(expires_at);

-- ============================================================================
-- Security Events Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS security_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description TEXT,
    ip_address VARCHAR(45),
    user_id UUID,
    username VARCHAR(255),
    company_id UUID,
    details JSONB,
    resolved BOOLEAN DEFAULT false,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by UUID,
    resolution_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for security events
CREATE INDEX idx_security_events_type ON security_events(event_type);
CREATE INDEX idx_security_events_severity ON security_events(severity);
CREATE INDEX idx_security_events_ip ON security_events(ip_address);
CREATE INDEX idx_security_events_unresolved ON security_events(created_at DESC) WHERE resolved = false;

-- ============================================================================
-- Functions
-- ============================================================================

-- Function to clean up expired API keys
CREATE OR REPLACE FUNCTION cleanup_expired_api_keys() RETURNS void AS $$
BEGIN
    UPDATE api_keys 
    SET is_active = false 
    WHERE expires_at IS NOT NULL 
    AND expires_at < CURRENT_TIMESTAMP 
    AND is_active = true;
END;
$$ LANGUAGE plpgsql;

-- Function to clean up old audit logs (keep last 90 days)
CREATE OR REPLACE FUNCTION cleanup_old_audit_logs() RETURNS void AS $$
BEGIN
    DELETE FROM audit_logs 
    WHERE timestamp < CURRENT_TIMESTAMP - INTERVAL '90 days';
END;
$$ LANGUAGE plpgsql;

-- Function to clean up expired rate limit buckets
CREATE OR REPLACE FUNCTION cleanup_expired_rate_limits() RETURNS void AS $$
BEGIN
    DELETE FROM rate_limit_buckets 
    WHERE expires_at IS NOT NULL 
    AND expires_at < CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Scheduled Jobs (using pg_cron if available)
-- ============================================================================
-- Note: Requires pg_cron extension to be installed
-- CREATE EXTENSION IF NOT EXISTS pg_cron;

-- Schedule cleanup jobs (uncomment if pg_cron is available)
-- SELECT cron.schedule('cleanup-expired-api-keys', '0 * * * *', 'SELECT cleanup_expired_api_keys();');
-- SELECT cron.schedule('cleanup-old-audit-logs', '0 2 * * *', 'SELECT cleanup_old_audit_logs();');
-- SELECT cron.schedule('cleanup-expired-rate-limits', '*/10 * * * *', 'SELECT cleanup_expired_rate_limits();');

-- ============================================================================
-- Sample Data for Testing (remove in production)
-- ============================================================================
-- Insert a test API key (password: test-api-key-12345678)
-- INSERT INTO api_keys (key_name, key_hash, key_prefix, description, company_id, rate_limit_tier)
-- SELECT 
--     'test-api-key',
--     '$2a$10$dummyhash', -- Replace with actual BCrypt hash
--     'test1234',
--     'Test API key for development',
--     id,
--     'STANDARD'
-- FROM companies 
-- WHERE name = 'Test Company'
-- LIMIT 1;

-- ============================================================================
-- Validation
-- ============================================================================
DO $$
BEGIN
    -- Check if api_keys table exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'api_keys') THEN
        RAISE EXCEPTION 'Table api_keys was not created';
    END IF;
    
    -- Check if audit_logs table exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'audit_logs') THEN
        RAISE EXCEPTION 'Table audit_logs was not created';
    END IF;
    
    RAISE NOTICE 'Security tables created successfully';
END;
$$;