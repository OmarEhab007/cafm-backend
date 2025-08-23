package com.cafm.cafmbackend.data.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for audit logging.
 * 
 * Purpose: Track all significant actions in the system
 * Pattern: Immutable audit log entity
 * Java 23: Efficient storage of audit data
 * Architecture: Data layer audit entity
 * Standards: Comprehensive audit trail
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_entity_action", columnList = "entity_type, action"),
    @Index(name = "idx_audit_user_timestamp", columnList = "user_id, timestamp DESC"),
    @Index(name = "idx_audit_company_timestamp", columnList = "company_id, timestamp DESC"),
    @Index(name = "idx_audit_entity_id", columnList = "entity_id")
})
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "user_id")
    private UUID userId;
    
    @Column(name = "username")
    private String username;
    
    @Column(name = "company_id")
    private UUID companyId;
    
    @Column(name = "action", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditAction action;
    
    @Column(name = "entity_type", nullable = false)
    private String entityType;
    
    @Column(name = "entity_id")
    private UUID entityId;
    
    @Column(name = "entity_name")
    private String entityName;
    
    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;
    
    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;
    
    @Column(name = "changes", columnDefinition = "TEXT")
    private String changes;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "request_id")
    private String requestId;
    
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private AuditStatus status;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "duration_ms")
    private Long durationMs;
    
    @Column(name = "api_endpoint")
    private String apiEndpoint;
    
    @Column(name = "http_method")
    private String httpMethod;
    
    @Column(name = "response_code")
    private Integer responseCode;
    
    // Constructors
    public AuditLog() {
        this.timestamp = LocalDateTime.now();
        this.status = AuditStatus.SUCCESS;
    }
    
    public AuditLog(AuditAction action, String entityType, UUID entityId) {
        this();
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
    }
    
    // Builder pattern for complex audit logs
    public static class Builder {
        private final AuditLog auditLog;
        
        public Builder(AuditAction action, String entityType) {
            this.auditLog = new AuditLog();
            this.auditLog.action = action;
            this.auditLog.entityType = entityType;
        }
        
        public Builder withUser(UUID userId, String username) {
            auditLog.userId = userId;
            auditLog.username = username;
            return this;
        }
        
        public Builder withCompany(UUID companyId) {
            auditLog.companyId = companyId;
            return this;
        }
        
        public Builder withEntity(UUID entityId, String entityName) {
            auditLog.entityId = entityId;
            auditLog.entityName = entityName;
            return this;
        }
        
        public Builder withChanges(String oldValues, String newValues, String changes) {
            auditLog.oldValues = oldValues;
            auditLog.newValues = newValues;
            auditLog.changes = changes;
            return this;
        }
        
        public Builder withRequest(String ipAddress, String userAgent, String requestId) {
            auditLog.ipAddress = ipAddress;
            auditLog.userAgent = userAgent;
            auditLog.requestId = requestId;
            return this;
        }
        
        public Builder withEndpoint(String apiEndpoint, String httpMethod) {
            auditLog.apiEndpoint = apiEndpoint;
            auditLog.httpMethod = httpMethod;
            return this;
        }
        
        public Builder withResult(AuditStatus status, Integer responseCode, Long durationMs) {
            auditLog.status = status;
            auditLog.responseCode = responseCode;
            auditLog.durationMs = durationMs;
            return this;
        }
        
        public Builder withError(String errorMessage) {
            auditLog.status = AuditStatus.FAILURE;
            auditLog.errorMessage = errorMessage;
            return this;
        }
        
        public AuditLog build() {
            return auditLog;
        }
    }
    
    // Getters (no setters - audit logs are immutable after creation)
    public UUID getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public UUID getUserId() { return userId; }
    public String getUsername() { return username; }
    public UUID getCompanyId() { return companyId; }
    public AuditAction getAction() { return action; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getEntityName() { return entityName; }
    public String getOldValues() { return oldValues; }
    public String getNewValues() { return newValues; }
    public String getChanges() { return changes; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getRequestId() { return requestId; }
    public String getSessionId() { return sessionId; }
    public AuditStatus getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public Long getDurationMs() { return durationMs; }
    public String getApiEndpoint() { return apiEndpoint; }
    public String getHttpMethod() { return httpMethod; }
    public Integer getResponseCode() { return responseCode; }
    
    /**
     * Audit action enumeration
     */
    public enum AuditAction {
        // Authentication
        LOGIN,
        LOGOUT,
        LOGIN_FAILED,
        PASSWORD_RESET,
        PASSWORD_CHANGED,
        
        // CRUD operations
        CREATE,
        READ,
        UPDATE,
        DELETE,
        RESTORE,
        
        // Bulk operations
        BULK_CREATE,
        BULK_UPDATE,
        BULK_DELETE,
        
        // File operations
        UPLOAD,
        DOWNLOAD,
        FILE_DELETE,
        
        // Administrative
        PERMISSION_GRANTED,
        PERMISSION_REVOKED,
        ROLE_ASSIGNED,
        ROLE_REMOVED,
        
        // API operations
        API_KEY_CREATED,
        API_KEY_REVOKED,
        API_KEY_USED,
        
        // Security events
        SECURITY,
        UNAUTHORIZED_ACCESS,
        ACCESS_DENIED,
        TENANT_VIOLATION,
        RATE_LIMIT_EXCEEDED,
        SUSPICIOUS_ACTIVITY,
        
        // System events
        ERROR,
        SYSTEM_CONFIG_CHANGED,
        DATA_EXPORT,
        DATA_IMPORT,
        BACKUP_CREATED,
        MAINTENANCE_MODE,
        
        // Additional actions
        ACCESS,  // For general access logging
        OTHER    // For miscellaneous actions
    }
    
    /**
     * Audit status enumeration
     */
    public enum AuditStatus {
        SUCCESS,
        FAILURE,
        PARTIAL,
        PENDING
    }
}