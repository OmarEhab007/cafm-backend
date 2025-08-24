-- Create mobile sync support tables
-- Purpose: Support offline data synchronization and push notifications for mobile apps
-- Pattern: Mobile-first design with conflict resolution and device tracking
-- Architecture: Extends multi-tenant structure with device and sync management
-- Standards: Follows existing naming conventions and UUID primary keys

-- Device registrations table for tracking mobile devices
CREATE TABLE device_registrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    device_token VARCHAR(500) NOT NULL,
    device_type VARCHAR(20) NOT NULL CHECK (device_type IN ('ANDROID', 'IOS')),
    device_name VARCHAR(255),
    app_version VARCHAR(50),
    os_version VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_seen_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT uk_device_registrations_token UNIQUE (device_token),
    CONSTRAINT uk_device_registrations_user_device UNIQUE (user_id, device_token)
);

-- Sync logs table for tracking synchronization history
CREATE TABLE sync_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES device_registrations(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    sync_type VARCHAR(50) NOT NULL CHECK (sync_type IN ('FULL', 'INCREMENTAL', 'PUSH', 'PULL')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    records_synced INTEGER NOT NULL DEFAULT 0,
    records_failed INTEGER NOT NULL DEFAULT 0,
    conflicts_count INTEGER NOT NULL DEFAULT 0,
    data_size_bytes BIGINT DEFAULT 0,
    sync_duration_ms INTEGER,
    error_message TEXT,
    metadata JSONB,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Sync conflicts table for handling data conflicts during synchronization
CREATE TABLE sync_conflicts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sync_log_id UUID NOT NULL REFERENCES sync_logs(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    server_value TEXT,
    client_value TEXT,
    resolution_strategy VARCHAR(50) CHECK (resolution_strategy IN ('SERVER_WINS', 'CLIENT_WINS', 'MERGE', 'MANUAL')),
    resolved_value TEXT,
    is_resolved BOOLEAN NOT NULL DEFAULT false,
    resolved_by UUID REFERENCES users(id),
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Notification topics table for push notification management
CREATE TABLE notification_topics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    topic_key VARCHAR(200) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT uk_notification_topics_company_key UNIQUE (company_id, topic_key)
);

-- Device topic subscriptions table for managing topic subscriptions per device
CREATE TABLE device_topic_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID NOT NULL REFERENCES device_registrations(id) ON DELETE CASCADE,
    topic_id UUID NOT NULL REFERENCES notification_topics(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    is_subscribed BOOLEAN NOT NULL DEFAULT true,
    subscribed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    unsubscribed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT uk_device_topic_subscriptions UNIQUE (device_id, topic_id)
);

-- User locations table for tracking GPS positions
CREATE TABLE user_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    accuracy_meters DECIMAL(8, 2),
    altitude_meters DECIMAL(8, 2),
    speed_mps DECIMAL(8, 2),
    heading_degrees DECIMAL(5, 2),
    location_source VARCHAR(20) CHECK (location_source IN ('GPS', 'NETWORK', 'PASSIVE')),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Add FCM token field to users table for push notifications
ALTER TABLE users ADD COLUMN fcm_token VARCHAR(500);

-- Create indexes for performance
CREATE INDEX idx_device_registrations_user_active ON device_registrations(user_id, is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_device_registrations_company ON device_registrations(company_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_device_registrations_last_seen ON device_registrations(last_seen_at) WHERE deleted_at IS NULL;

CREATE INDEX idx_sync_logs_user_status ON sync_logs(user_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_sync_logs_device ON sync_logs(device_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_sync_logs_company_created ON sync_logs(company_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_sync_logs_started_at ON sync_logs(started_at) WHERE deleted_at IS NULL;

CREATE INDEX idx_sync_conflicts_sync_log ON sync_conflicts(sync_log_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_sync_conflicts_entity ON sync_conflicts(entity_type, entity_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_sync_conflicts_unresolved ON sync_conflicts(is_resolved) WHERE deleted_at IS NULL AND is_resolved = false;

CREATE INDEX idx_notification_topics_company_active ON notification_topics(company_id, is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_notification_topics_key ON notification_topics(topic_key) WHERE deleted_at IS NULL;

CREATE INDEX idx_device_topic_subscriptions_device ON device_topic_subscriptions(device_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_device_topic_subscriptions_topic ON device_topic_subscriptions(topic_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_device_topic_subscriptions_subscribed ON device_topic_subscriptions(device_id, is_subscribed) WHERE deleted_at IS NULL;

CREATE INDEX idx_user_locations_user_recorded ON user_locations(user_id, recorded_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_user_locations_company_recorded ON user_locations(company_id, recorded_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_user_locations_coordinates ON user_locations(latitude, longitude) WHERE deleted_at IS NULL;

CREATE INDEX idx_users_fcm_token ON users(fcm_token) WHERE fcm_token IS NOT NULL AND deleted_at IS NULL;

-- Update updated_at timestamps automatically
CREATE TRIGGER update_device_registrations_updated_at BEFORE UPDATE ON device_registrations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_sync_logs_updated_at BEFORE UPDATE ON sync_logs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_sync_conflicts_updated_at BEFORE UPDATE ON sync_conflicts FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_notification_topics_updated_at BEFORE UPDATE ON notification_topics FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_device_topic_subscriptions_updated_at BEFORE UPDATE ON device_topic_subscriptions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_user_locations_updated_at BEFORE UPDATE ON user_locations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE device_registrations IS 'Mobile device registrations for push notifications and sync tracking';
COMMENT ON TABLE sync_logs IS 'History of data synchronization operations between mobile apps and server';
COMMENT ON TABLE sync_conflicts IS 'Data conflicts that occurred during synchronization and their resolution';
COMMENT ON TABLE notification_topics IS 'Push notification topics for organizing notifications by category';
COMMENT ON TABLE device_topic_subscriptions IS 'Device subscriptions to notification topics';
COMMENT ON TABLE user_locations IS 'GPS location history for mobile users';