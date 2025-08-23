-- ============================================
-- V119__Create_Notifications_And_User_Management_Tables.sql
-- CAFM Backend - Create Enhanced Notifications and User Management Tables
-- Purpose: Create comprehensive notification system and improved user role management
-- Pattern: Clean Architecture with event-driven notification delivery
-- Java 23: Uses sealed interfaces for notification types and pattern matching
-- Architecture: Multi-tenant with real-time notification delivery and audit trails
-- Standards: Full security with email verification and role-based access control
-- ============================================

-- ============================================
-- STEP 1: CREATE NOTIFICATIONS TABLE
-- ============================================

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    
    -- Recipient Information
    user_id UUID REFERENCES users(id), -- NULL for broadcast notifications
    recipient_email VARCHAR(255),
    recipient_phone VARCHAR(20),
    
    -- Notification Content
    title VARCHAR(500) NOT NULL,
    message TEXT NOT NULL,
    message_ar TEXT, -- Arabic translation
    
    -- Notification Classification
    notification_type notification_type_enum NOT NULL,
    priority notification_priority_enum DEFAULT 'NORMAL',
    category VARCHAR(50),
    subcategory VARCHAR(50),
    
    -- Rich Content
    data JSONB DEFAULT '{}', -- Additional structured data
    action_buttons JSONB DEFAULT '[]', -- Array of action button definitions
    attachments JSONB DEFAULT '[]', -- Array of attachment references
    
    -- Delivery Channels
    delivery_channels VARCHAR(100) DEFAULT 'IN_APP', -- IN_APP,EMAIL,SMS,PUSH,WEB
    
    -- Targeting and Context
    entity_type VARCHAR(50), -- REPORT, WORK_ORDER, ASSET, USER, etc.
    entity_id UUID,
    related_entity_type VARCHAR(50),
    related_entity_id UUID,
    
    -- Scheduling and Timing
    scheduled_for TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    
    -- Status Tracking
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    is_dismissed BOOLEAN DEFAULT FALSE,
    dismissed_at TIMESTAMP WITH TIME ZONE,
    
    -- Delivery Status
    delivery_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SENT, DELIVERED, FAILED, EXPIRED
    delivered_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    failure_reason TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    
    -- Response Tracking
    clicked_at TIMESTAMP WITH TIME ZONE,
    action_taken VARCHAR(100), -- Which action button was clicked
    response_data JSONB DEFAULT '{}',
    
    -- Grouping and Threading
    notification_group VARCHAR(100), -- Group related notifications
    thread_id UUID, -- For conversation-style notifications
    parent_notification_id UUID REFERENCES notifications(id),
    
    -- Template and Localization
    template_name VARCHAR(100),
    template_version VARCHAR(20),
    locale VARCHAR(10) DEFAULT 'en',
    
    -- Sender Information
    sender_id UUID REFERENCES users(id),
    sender_name VARCHAR(255),
    sender_type VARCHAR(20) DEFAULT 'SYSTEM', -- SYSTEM, USER, AUTOMATED
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_id UUID REFERENCES users(id),
    
    -- Constraints
    CONSTRAINT chk_notifications_delivery_status CHECK (
        delivery_status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'EXPIRED', 'CANCELLED')
    ),
    CONSTRAINT chk_notifications_sender_type CHECK (
        sender_type IN ('SYSTEM', 'USER', 'AUTOMATED', 'EXTERNAL')
    ),
    CONSTRAINT chk_notifications_retry_count CHECK (
        retry_count >= 0 AND retry_count <= max_retries
    ),
    CONSTRAINT chk_notifications_expiry_after_scheduled CHECK (
        expires_at IS NULL OR expires_at > scheduled_for
    )
);

-- ============================================
-- STEP 2: CREATE EMAIL VERIFICATION TOKENS TABLE
-- ============================================

CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    
    -- User Information
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    
    -- Token Information
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    token_type VARCHAR(20) DEFAULT 'EMAIL_VERIFICATION', -- EMAIL_VERIFICATION, EMAIL_CHANGE, PASSWORD_RESET
    
    -- Validity
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMP WITH TIME ZONE,
    
    -- Security
    ip_address INET,
    user_agent TEXT,
    verification_attempts INTEGER DEFAULT 0,
    max_attempts INTEGER DEFAULT 5,
    
    -- Metadata
    additional_data JSONB DEFAULT '{}',
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_email_verification_tokens_type CHECK (
        token_type IN ('EMAIL_VERIFICATION', 'EMAIL_CHANGE', 'PASSWORD_RESET')
    ),
    CONSTRAINT chk_email_verification_tokens_attempts CHECK (
        verification_attempts >= 0 AND verification_attempts <= max_attempts
    )
);

-- ============================================
-- STEP 3: CREATE ENHANCED USER_ROLES TABLE
-- ============================================

-- Note: The basic user_roles table exists from V1, but we'll create an enhanced version
-- First, check if we need to modify the existing table or create a new one
DO $$
BEGIN
    -- Check if the existing user_roles table has the columns we need
    IF EXISTS (
        SELECT FROM information_schema.columns 
        WHERE table_name = 'user_roles' AND column_name = 'role_id'
    ) THEN
        -- The existing table uses role_id (references roles table)
        -- We'll add additional columns to support our enhanced role management
        
        -- Add new columns if they don't exist
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'user_roles' AND column_name = 'company_id') THEN
            ALTER TABLE user_roles ADD COLUMN company_id UUID REFERENCES companies(id) ON DELETE CASCADE;
        END IF;
        
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'user_roles' AND column_name = 'role_name') THEN
            ALTER TABLE user_roles ADD COLUMN role_name VARCHAR(50);
        END IF;
        
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'user_roles' AND column_name = 'is_active') THEN
            ALTER TABLE user_roles ADD COLUMN is_active BOOLEAN DEFAULT TRUE;
        END IF;
        
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'user_roles' AND column_name = 'expires_at') THEN
            ALTER TABLE user_roles ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;
        END IF;
        
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'user_roles' AND column_name = 'assigned_by') THEN
            ALTER TABLE user_roles ADD COLUMN assigned_by UUID REFERENCES users(id);
        END IF;
        
        -- Update existing records with company_id and role_name
        UPDATE user_roles ur
        SET 
            company_id = u.company_id,
            role_name = r.name
        FROM users u, roles r
        WHERE ur.user_id = u.id AND ur.role_id = r.id
        AND ur.company_id IS NULL;
        
        -- Make company_id NOT NULL
        ALTER TABLE user_roles ALTER COLUMN company_id SET NOT NULL;
        
    ELSE
        -- Create new user_roles table with enhanced structure
        DROP TABLE IF EXISTS user_roles CASCADE;
        
        CREATE TABLE user_roles (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            role_id UUID REFERENCES roles(id), -- Keep for backward compatibility
            role_name VARCHAR(50) NOT NULL,
            
            -- Assignment Tracking
            assigned_by UUID REFERENCES users(id),
            assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
            expires_at TIMESTAMP WITH TIME ZONE,
            
            -- Status
            is_active BOOLEAN DEFAULT TRUE,
            
            -- Role Context
            scope VARCHAR(50) DEFAULT 'GLOBAL', -- GLOBAL, SCHOOL, DEPARTMENT
            scope_entity_id UUID, -- ID of school, department, etc.
            
            -- Permissions Override
            permissions_override JSONB DEFAULT '{}',
            
            -- Audit Fields
            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
            
            -- Constraints
            CONSTRAINT uk_user_roles_user_role_scope UNIQUE (user_id, role_name, scope, scope_entity_id),
            CONSTRAINT chk_user_roles_scope CHECK (
                scope IN ('GLOBAL', 'SCHOOL', 'DEPARTMENT', 'PROJECT')
            )
        );
    END IF;
END $$;

-- ============================================
-- STEP 4: CREATE SUPERVISOR_SCHOOLS ENHANCED TABLE
-- ============================================

-- Check if supervisor_schools table needs enhancement
DO $$
BEGIN
    -- Add company_id if it doesn't exist (it should from V13, but let's be safe)
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'supervisor_schools' AND column_name = 'company_id') THEN
        ALTER TABLE supervisor_schools ADD COLUMN company_id UUID REFERENCES companies(id) ON DELETE CASCADE;
        
        -- Update existing records
        UPDATE supervisor_schools ss
        SET company_id = u.company_id
        FROM users u
        WHERE ss.supervisor_id = u.id AND ss.company_id IS NULL;
        
        -- Make it NOT NULL
        ALTER TABLE supervisor_schools ALTER COLUMN company_id SET NOT NULL;
    END IF;
    
    -- Add additional columns for enhanced functionality
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'supervisor_schools' AND column_name = 'role_type') THEN
        ALTER TABLE supervisor_schools ADD COLUMN role_type VARCHAR(50) DEFAULT 'PRIMARY';
    END IF;
    
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'supervisor_schools' AND column_name = 'permissions') THEN
        ALTER TABLE supervisor_schools ADD COLUMN permissions JSONB DEFAULT '{}';
    END IF;
    
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'supervisor_schools' AND column_name = 'max_work_orders') THEN
        ALTER TABLE supervisor_schools ADD COLUMN max_work_orders INTEGER;
    END IF;
END $$;

-- ============================================
-- STEP 5: CREATE PERFORMANCE INDEXES
-- ============================================

-- Notifications Indexes
CREATE INDEX idx_notifications_company_user ON notifications(company_id, user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_notifications_unread ON notifications(user_id, is_read, created_at DESC) 
    WHERE is_read = FALSE AND user_id IS NOT NULL;
CREATE INDEX idx_notifications_type_priority ON notifications(notification_type, priority, created_at DESC);
CREATE INDEX idx_notifications_entity ON notifications(entity_type, entity_id) 
    WHERE entity_type IS NOT NULL AND entity_id IS NOT NULL;
CREATE INDEX idx_notifications_scheduled ON notifications(scheduled_for) WHERE delivery_status = 'PENDING';
CREATE INDEX idx_notifications_delivery_status ON notifications(delivery_status, retry_count) 
    WHERE delivery_status IN ('PENDING', 'FAILED');
CREATE INDEX idx_notifications_expired ON notifications(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX idx_notifications_group ON notifications(notification_group) WHERE notification_group IS NOT NULL;
CREATE INDEX idx_notifications_thread ON notifications(thread_id) WHERE thread_id IS NOT NULL;

-- Email Verification Tokens Indexes
CREATE INDEX idx_email_verification_tokens_user ON email_verification_tokens(user_id);
CREATE INDEX idx_email_verification_tokens_hash ON email_verification_tokens(token_hash);
CREATE INDEX idx_email_verification_tokens_email ON email_verification_tokens(email);
CREATE INDEX idx_email_verification_tokens_expires ON email_verification_tokens(expires_at) 
    WHERE is_used = FALSE;
CREATE INDEX idx_email_verification_tokens_company ON email_verification_tokens(company_id);

-- Enhanced User Roles Indexes (only create if they don't exist)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_indexes WHERE indexname = 'idx_user_roles_company_user') THEN
        CREATE INDEX idx_user_roles_company_user ON user_roles(company_id, user_id);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_indexes WHERE indexname = 'idx_user_roles_user_active') THEN
        CREATE INDEX idx_user_roles_user_active ON user_roles(user_id, is_active) WHERE is_active = TRUE;
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_indexes WHERE indexname = 'idx_user_roles_role_name') THEN
        CREATE INDEX idx_user_roles_role_name ON user_roles(role_name);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_indexes WHERE indexname = 'idx_user_roles_scope') THEN
        CREATE INDEX idx_user_roles_scope ON user_roles(scope, scope_entity_id) WHERE scope != 'GLOBAL';
    END IF;
END $$;

-- Enhanced Supervisor Schools Indexes
CREATE INDEX idx_supervisor_schools_company_supervisor ON supervisor_schools(company_id, supervisor_id);
CREATE INDEX idx_supervisor_schools_role_type ON supervisor_schools(role_type) WHERE role_type != 'PRIMARY';

-- Full-text search indexes
CREATE INDEX idx_notifications_content_search ON notifications 
    USING GIN(to_tsvector('english', title || ' ' || message));

-- JSONB indexes
CREATE INDEX idx_notifications_data ON notifications USING GIN(data);
CREATE INDEX idx_notifications_action_buttons ON notifications USING GIN(action_buttons);
CREATE INDEX idx_notifications_response_data ON notifications USING GIN(response_data);

-- ============================================
-- STEP 6: CREATE TRIGGERS AND FUNCTIONS
-- ============================================

-- Function to create notifications for various events
CREATE OR REPLACE FUNCTION create_notification(
    p_company_id UUID,
    p_user_id UUID,
    p_title VARCHAR(500),
    p_message TEXT,
    p_notification_type notification_type_enum,
    p_priority notification_priority_enum DEFAULT 'NORMAL',
    p_entity_type VARCHAR(50) DEFAULT NULL,
    p_entity_id UUID DEFAULT NULL,
    p_data JSONB DEFAULT '{}'
)
RETURNS UUID AS $$
DECLARE
    v_notification_id UUID;
BEGIN
    INSERT INTO notifications (
        company_id, user_id, title, message, notification_type, priority,
        entity_type, entity_id, data, created_by_id
    ) VALUES (
        p_company_id, p_user_id, p_title, p_message, p_notification_type, p_priority,
        p_entity_type, p_entity_id, p_data, p_user_id
    ) RETURNING id INTO v_notification_id;
    
    RETURN v_notification_id;
END;
$$ LANGUAGE plpgsql;

-- Function to mark notifications as read
CREATE OR REPLACE FUNCTION mark_notification_read(p_notification_id UUID, p_user_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE notifications
    SET 
        is_read = TRUE,
        read_at = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_notification_id 
    AND user_id = p_user_id 
    AND is_read = FALSE;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- Function to cleanup expired notifications
CREATE OR REPLACE FUNCTION cleanup_expired_notifications()
RETURNS INTEGER AS $$
DECLARE
    v_count INTEGER;
BEGIN
    -- Mark expired notifications as expired
    UPDATE notifications
    SET 
        delivery_status = 'EXPIRED',
        updated_at = CURRENT_TIMESTAMP
    WHERE expires_at < CURRENT_TIMESTAMP
    AND delivery_status IN ('PENDING', 'FAILED');
    
    GET DIAGNOSTICS v_count = ROW_COUNT;
    
    -- Optionally delete very old notifications (older than 1 year)
    DELETE FROM notifications
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '1 year'
    AND delivery_status = 'EXPIRED';
    
    RETURN v_count;
END;
$$ LANGUAGE plpgsql;

-- Function to generate email verification token
CREATE OR REPLACE FUNCTION generate_email_verification_token(
    p_user_id UUID,
    p_email VARCHAR(255),
    p_token_type VARCHAR(20) DEFAULT 'EMAIL_VERIFICATION'
)
RETURNS VARCHAR(255) AS $$
DECLARE
    v_token VARCHAR(255);
    v_token_hash VARCHAR(255);
    v_company_id UUID;
BEGIN
    -- Generate random token
    v_token := encode(gen_random_bytes(32), 'hex');
    v_token_hash := encode(digest(v_token, 'sha256'), 'hex');
    
    -- Get user's company_id
    SELECT company_id INTO v_company_id FROM users WHERE id = p_user_id;
    
    -- Insert token record
    INSERT INTO email_verification_tokens (
        company_id, user_id, email, token_hash, token_type,
        expires_at
    ) VALUES (
        v_company_id, p_user_id, p_email, v_token_hash, p_token_type,
        CURRENT_TIMESTAMP + INTERVAL '24 hours'
    );
    
    RETURN v_token;
END;
$$ LANGUAGE plpgsql;

-- Function to verify email token
CREATE OR REPLACE FUNCTION verify_email_token(p_token VARCHAR(255))
RETURNS TABLE (
    user_id UUID,
    email VARCHAR(255),
    is_valid BOOLEAN,
    token_type VARCHAR(20)
) AS $$
DECLARE
    v_token_hash VARCHAR(255);
BEGIN
    v_token_hash := encode(digest(p_token, 'sha256'), 'hex');
    
    RETURN QUERY
    SELECT 
        evt.user_id,
        evt.email,
        (NOT evt.is_used AND evt.expires_at > CURRENT_TIMESTAMP AND evt.verification_attempts < evt.max_attempts) as is_valid,
        evt.token_type
    FROM email_verification_tokens evt
    WHERE evt.token_hash = v_token_hash;
    
    -- Update verification attempts
    UPDATE email_verification_tokens
    SET 
        verification_attempts = verification_attempts + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE token_hash = v_token_hash;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 7: ROW LEVEL SECURITY
-- ============================================

-- Enable RLS for new tables
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE email_verification_tokens ENABLE ROW LEVEL SECURITY;

-- Create policies for tenant isolation
CREATE POLICY tenant_isolation_notifications ON notifications
    FOR ALL USING (company_id = COALESCE(
        NULLIF(current_setting('app.current_company_id', true), '')::UUID,
        '00000000-0000-0000-0000-000000000001'::UUID
    ));

CREATE POLICY tenant_isolation_email_verification_tokens ON email_verification_tokens
    FOR ALL USING (company_id = COALESCE(
        NULLIF(current_setting('app.current_company_id', true), '')::UUID,
        '00000000-0000-0000-0000-000000000001'::UUID
    ));

-- Enable RLS for enhanced user_roles (if not already enabled)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT FROM pg_policies WHERE schemaname = 'public' AND tablename = 'user_roles' AND policyname = 'tenant_isolation_user_roles'
    ) THEN
        ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;
        
        CREATE POLICY tenant_isolation_user_roles ON user_roles
            FOR ALL USING (company_id = COALESCE(
                NULLIF(current_setting('app.current_company_id', true), '')::UUID,
                '00000000-0000-0000-0000-000000000001'::UUID
            ));
    END IF;
END $$;

-- ============================================
-- STEP 8: COMMENTS FOR DOCUMENTATION
-- ============================================

-- Table comments
COMMENT ON TABLE notifications IS 'Comprehensive notification system with multi-channel delivery and rich content support';
COMMENT ON TABLE email_verification_tokens IS 'Secure email verification and password reset token management';

-- Column comments for notifications
COMMENT ON COLUMN notifications.delivery_channels IS 'Comma-separated list of delivery channels: IN_APP,EMAIL,SMS,PUSH,WEB';
COMMENT ON COLUMN notifications.action_buttons IS 'JSON array of action button definitions for interactive notifications';
COMMENT ON COLUMN notifications.thread_id IS 'Groups related notifications in conversation threads';
COMMENT ON COLUMN notifications.data IS 'Structured data payload for rich notification content';

-- Column comments for email verification tokens
COMMENT ON COLUMN email_verification_tokens.token_hash IS 'SHA-256 hash of the verification token for secure storage';
COMMENT ON COLUMN email_verification_tokens.verification_attempts IS 'Number of verification attempts made with this token';

-- Function comments
COMMENT ON FUNCTION create_notification IS 'Creates a new notification with specified parameters';
COMMENT ON FUNCTION mark_notification_read IS 'Marks a notification as read by the specified user';
COMMENT ON FUNCTION cleanup_expired_notifications IS 'Cleans up expired notifications and returns count of processed items';
COMMENT ON FUNCTION generate_email_verification_token IS 'Generates secure email verification token and returns plain token';
COMMENT ON FUNCTION verify_email_token IS 'Verifies email token and returns user information and validity status';

-- ============================================
-- VERIFICATION
-- ============================================

DO $$
BEGIN
    -- Verify notifications table was created
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'notifications') THEN
        RAISE EXCEPTION 'Migration failed: notifications table was not created';
    END IF;
    
    -- Verify email_verification_tokens table was created
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'email_verification_tokens') THEN
        RAISE EXCEPTION 'Migration failed: email_verification_tokens table was not created';
    END IF;
    
    -- Verify user_roles table has required columns
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'user_roles' AND column_name = 'company_id') THEN
        RAISE EXCEPTION 'Migration failed: user_roles table was not properly enhanced';
    END IF;
    
    RAISE NOTICE 'Migration V119 completed successfully - Notifications and User Management System created';
END $$;

-- End of V119 migration