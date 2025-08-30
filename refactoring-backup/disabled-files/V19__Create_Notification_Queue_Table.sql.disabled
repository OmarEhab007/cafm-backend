-- ================================
-- V21: Create Notification Queue Table
-- ================================
-- Queue for managing push notifications to mobile devices
-- Supports retry logic and batch processing

CREATE TABLE IF NOT EXISTS notification_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Target information
    user_id UUID,                          -- Target user (optional for broadcast)
    device_token TEXT,                     -- Specific device token (optional)
    topic VARCHAR(100),                    -- Topic for broadcast notifications
    
    -- Notification content
    title VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    data JSONB DEFAULT '{}',              -- Additional data payload
    
    -- Notification type and category
    notification_type VARCHAR(50),         -- Type: report, work_order, alert, etc.
    category VARCHAR(50),                  -- iOS category for actions
    priority VARCHAR(20) DEFAULT 'normal',
    
    -- Platform specific
    platform VARCHAR(20),                  -- ios, android, web
    sound VARCHAR(100),                    -- Custom sound file
    badge INTEGER,                         -- Badge number for iOS
    
    -- Processing status
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    processed BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    
    -- Retry logic
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    error_code VARCHAR(50),
    
    -- Scheduling
    scheduled_for TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    
    -- Response tracking
    fcm_message_id VARCHAR(255),          -- Firebase message ID
    fcm_response JSONB,                   -- Full FCM response
    
    -- Foreign keys
    company_id UUID NOT NULL,
    created_by UUID,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_notification_queue_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_queue_company FOREIGN KEY (company_id) 
        REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_queue_created_by FOREIGN KEY (created_by) 
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_notification_status CHECK (status IN (
        'pending', 'processing', 'sent', 'failed', 'expired', 'cancelled'
    )),
    CONSTRAINT chk_notification_priority CHECK (priority IN ('low', 'normal', 'high', 'urgent')),
    CONSTRAINT chk_notification_platform CHECK (platform IN ('ios', 'android', 'web'))
);

-- FCM tokens table for device management
CREATE TABLE IF NOT EXISTS fcm_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- User and device info
    user_id UUID NOT NULL,
    device_token TEXT NOT NULL,
    device_id VARCHAR(255),               -- Unique device identifier
    
    -- Platform and app info
    platform VARCHAR(20) NOT NULL,        -- ios, android
    app_version VARCHAR(50),
    os_version VARCHAR(50),
    device_model VARCHAR(100),
    device_name VARCHAR(255),
    
    -- Token status
    is_active BOOLEAN DEFAULT TRUE,
    is_valid BOOLEAN DEFAULT TRUE,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP WITH TIME ZONE,
    
    -- Error tracking
    failure_count INTEGER DEFAULT 0,
    last_failure_at TIMESTAMP WITH TIME ZONE,
    last_failure_reason TEXT,
    
    -- Company association
    company_id UUID NOT NULL,
    
    -- Constraints
    CONSTRAINT fk_fcm_tokens_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_fcm_tokens_company FOREIGN KEY (company_id) 
        REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT chk_fcm_platform CHECK (platform IN ('ios', 'android')),
    CONSTRAINT uq_fcm_token_device UNIQUE (user_id, device_token)
);

-- Indexes for notification_queue
CREATE INDEX idx_notification_queue_user_id ON notification_queue(user_id);
CREATE INDEX idx_notification_queue_status ON notification_queue(status);
CREATE INDEX idx_notification_queue_processed ON notification_queue(processed);
CREATE INDEX idx_notification_queue_priority ON notification_queue(priority);
CREATE INDEX idx_notification_queue_scheduled_for ON notification_queue(scheduled_for);
CREATE INDEX idx_notification_queue_created_at ON notification_queue(created_at);
CREATE INDEX idx_notification_queue_company_id ON notification_queue(company_id);

-- Composite indexes for processing
CREATE INDEX idx_notification_queue_pending ON notification_queue(status, scheduled_for) 
    WHERE status = 'pending' AND processed = FALSE;
CREATE INDEX idx_notification_queue_retry ON notification_queue(status, next_retry_at) 
    WHERE status = 'failed' AND retry_count < max_retries;

-- Indexes for fcm_tokens
CREATE INDEX idx_fcm_tokens_user_id ON fcm_tokens(user_id);
CREATE INDEX idx_fcm_tokens_device_token ON fcm_tokens(device_token);
CREATE INDEX idx_fcm_tokens_platform ON fcm_tokens(platform);
CREATE INDEX idx_fcm_tokens_is_active ON fcm_tokens(is_active);
CREATE INDEX idx_fcm_tokens_company_id ON fcm_tokens(company_id);

-- Composite indexes
CREATE INDEX idx_fcm_tokens_user_platform ON fcm_tokens(user_id, platform);
CREATE INDEX idx_fcm_tokens_active_tokens ON fcm_tokens(user_id, is_active, is_valid);

-- Comments
COMMENT ON TABLE notification_queue IS 'Queue for managing push notifications with retry logic';
COMMENT ON COLUMN notification_queue.data IS 'Additional data payload for the notification';
COMMENT ON COLUMN notification_queue.fcm_message_id IS 'Firebase Cloud Messaging message ID for tracking';
COMMENT ON TABLE fcm_tokens IS 'Stores FCM device tokens for push notifications';
COMMENT ON COLUMN fcm_tokens.device_token IS 'FCM registration token for the device';