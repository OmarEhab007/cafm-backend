package com.cafm.cafmbackend.application.service.audit;

import com.cafm.cafmbackend.infrastructure.persistence.entity.AuditLog;
import com.cafm.cafmbackend.infrastructure.persistence.entity.Company;
import com.cafm.cafmbackend.infrastructure.persistence.repository.AuditLogRepository;
import com.cafm.cafmbackend.application.service.tenant.TenantContextService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Comprehensive security audit logging service for tenant operations.
 * 
 * Explanation:
 * - Purpose: Provides detailed audit trails for all tenant-related security events
 * - Pattern: Service layer for security auditing with structured logging
 * - Java 23: Modern audit service with comprehensive event tracking
 * - Architecture: Critical security component for compliance and monitoring
 * - Standards: Comprehensive audit trails for forensic analysis and compliance
 */
@Service
public class TenantSecurityAuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantSecurityAuditService.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private TenantContextService tenantContextService;
    
    /**
     * Log tenant access event
     */
    public void logTenantAccess(UUID tenantId, String source, String ipAddress, String userAgent) {
        TenantSecurityEvent event = TenantSecurityEvent.builder()
            .eventType(SecurityEventType.TENANT_ACCESS)
            .tenantId(tenantId)
            .source(source)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .description("Tenant access granted")
            .build();
        
        logSecurityEvent(event);
    }
    
    /**
     * Log tenant access violation
     */
    public void logTenantViolation(UUID requestedTenantId, UUID actualTenantId, String reason, 
                                  String ipAddress, String userAgent) {
        TenantSecurityEvent event = TenantSecurityEvent.builder()
            .eventType(SecurityEventType.TENANT_VIOLATION)
            .tenantId(actualTenantId)
            .targetTenantId(requestedTenantId)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .description("Tenant access violation: " + reason)
            .severity(SecuritySeverity.HIGH)
            .build();
        
        logSecurityEvent(event);
    }
    
    /**
     * Log cross-tenant access attempt
     */
    public void logCrossTenantAttempt(UUID userTenantId, UUID requestedTenantId, String resource, 
                                     String operation, String ipAddress) {
        TenantSecurityEvent event = TenantSecurityEvent.builder()
            .eventType(SecurityEventType.CROSS_TENANT_ACCESS)
            .tenantId(userTenantId)
            .targetTenantId(requestedTenantId)
            .resource(resource)
            .operation(operation)
            .ipAddress(ipAddress)
            .description(String.format("Cross-tenant access attempt: %s trying to %s %s from tenant %s", 
                userTenantId, operation, resource, requestedTenantId))
            .severity(SecuritySeverity.CRITICAL)
            .build();
        
        logSecurityEvent(event);
    }
    
    /**
     * Log tenant context switch
     */
    public void logTenantSwitch(UUID fromTenantId, UUID toTenantId, String reason, String ipAddress) {
        TenantSecurityEvent event = TenantSecurityEvent.builder()
            .eventType(SecurityEventType.TENANT_SWITCH)
            .tenantId(fromTenantId)
            .targetTenantId(toTenantId)
            .ipAddress(ipAddress)
            .description("Tenant context switch: " + reason)
            .build();
        
        logSecurityEvent(event);
    }
    
    /**
     * Log entity access with tenant validation
     */
    public void logEntityAccess(String entityType, UUID entityId, UUID entityTenantId, 
                               String operation, boolean accessGranted) {
        TenantSecurityEvent event = TenantSecurityEvent.builder()
            .eventType(SecurityEventType.ENTITY_ACCESS)
            .tenantId(getCurrentTenantId())
            .targetTenantId(entityTenantId)
            .resource(entityType)
            .resourceId(entityId.toString())
            .operation(operation)
            .description(String.format("%s %s on %s %s", 
                accessGranted ? "Granted" : "Denied", operation, entityType, entityId))
            .severity(accessGranted ? SecuritySeverity.LOW : SecuritySeverity.HIGH)
            .build();
        
        logSecurityEvent(event);
    }
    
    /**
     * Log authentication event with tenant context
     */
    public void logAuthenticationEvent(String username, UUID tenantId, String eventType, 
                                     boolean successful, String ipAddress, String userAgent) {
        TenantSecurityEvent event = TenantSecurityEvent.builder()
            .eventType(SecurityEventType.AUTHENTICATION)
            .tenantId(tenantId)
            .username(username)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .operation(eventType)
            .description(String.format("Authentication %s: %s for tenant %s", 
                successful ? "successful" : "failed", username, tenantId))
            .severity(successful ? SecuritySeverity.LOW : SecuritySeverity.MEDIUM)
            .build();
        
        logSecurityEvent(event);
    }
    
    /**
     * Log data modification with tenant validation
     */
    public void logDataModification(String entityType, UUID entityId, UUID entityTenantId, 
                                   String operation, Map<String, Object> changes) {
        TenantSecurityEvent event = TenantSecurityEvent.builder()
            .eventType(SecurityEventType.DATA_MODIFICATION)
            .tenantId(getCurrentTenantId())
            .targetTenantId(entityTenantId)
            .resource(entityType)
            .resourceId(entityId.toString())
            .operation(operation)
            .metadata(changes)
            .description(String.format("Data modification: %s on %s %s", operation, entityType, entityId))
            .build();
        
        logSecurityEvent(event);
    }
    
    /**
     * Log bulk operation with tenant validation
     */
    public void logBulkOperation(String entityType, List<UUID> entityIds, String operation, 
                                int successCount, int failureCount) {
        TenantSecurityEvent event = TenantSecurityEvent.builder()
            .eventType(SecurityEventType.BULK_OPERATION)
            .tenantId(getCurrentTenantId())
            .resource(entityType)
            .operation(operation)
            .description(String.format("Bulk %s on %s: %d successful, %d failed", 
                operation, entityType, successCount, failureCount))
            .metadata(Map.of(
                "entityCount", entityIds.size(),
                "successCount", successCount,
                "failureCount", failureCount,
                "entityIds", entityIds.stream().map(UUID::toString).toList()
            ))
            .build();
        
        logSecurityEvent(event);
    }
    
    /**
     * Log tenant configuration change
     */
    public void logTenantConfigChange(UUID tenantId, String configType, String oldValue, 
                                     String newValue, String reason) {
        TenantSecurityEvent event = TenantSecurityEvent.builder()
            .eventType(SecurityEventType.CONFIGURATION_CHANGE)
            .tenantId(tenantId)
            .resource("TenantConfiguration")
            .operation("UPDATE")
            .description(String.format("Configuration change: %s from '%s' to '%s' - %s", 
                configType, oldValue, newValue, reason))
            .metadata(Map.of(
                "configType", configType,
                "oldValue", oldValue,
                "newValue", newValue,
                "reason", reason
            ))
            .severity(SecuritySeverity.MEDIUM)
            .build();
        
        logSecurityEvent(event);
    }
    
    /**
     * Log security incident
     */
    public void logSecurityIncident(String incidentType, String description, UUID affectedTenantId, 
                                   SecuritySeverity severity, Map<String, Object> details) {
        TenantSecurityEvent event = TenantSecurityEvent.builder()
            .eventType(SecurityEventType.SECURITY_INCIDENT)
            .tenantId(affectedTenantId)
            .operation(incidentType)
            .description(description)
            .severity(severity)
            .metadata(details)
            .build();
        
        logSecurityEvent(event);
    }
    
    /**
     * Log API access with rate limiting information
     */
    public void logApiAccess(String endpoint, String method, int responseStatus, 
                            long responseTime, boolean rateLimited) {
        TenantSecurityEvent event = TenantSecurityEvent.builder()
            .eventType(SecurityEventType.API_ACCESS)
            .tenantId(getCurrentTenantId())
            .resource(endpoint)
            .operation(method)
            .description(String.format("API access: %s %s -> %d (%dms)", 
                method, endpoint, responseStatus, responseTime))
            .metadata(Map.of(
                "responseStatus", responseStatus,
                "responseTime", responseTime,
                "rateLimited", rateLimited
            ))
            .severity(rateLimited ? SecuritySeverity.MEDIUM : SecuritySeverity.LOW)
            .build();
        
        logSecurityEvent(event);
    }
    
    /**
     * Get security events for current tenant
     */
    public List<AuditLog> getSecurityEventsForCurrentTenant(LocalDateTime from, LocalDateTime to, 
                                                            SecurityEventType eventType) {
        UUID tenantId = getCurrentTenantId();
        return getSecurityEventsForTenant(tenantId, from, to, eventType);
    }
    
    /**
     * Get security events for specific tenant
     */
    public List<AuditLog> getSecurityEventsForTenant(UUID tenantId, LocalDateTime from, LocalDateTime to, 
                                                     SecurityEventType eventType) {
        // This would require implementing query methods in AuditLogRepository
        // For now, return empty list as placeholder
        return List.of();
    }
    
    /**
     * Get security summary for current tenant
     */
    public SecuritySummary getSecuritySummaryForCurrentTenant(LocalDateTime from, LocalDateTime to) {
        UUID tenantId = getCurrentTenantId();
        return getSecuritySummaryForTenant(tenantId, from, to);
    }
    
    /**
     * Get security summary for specific tenant
     */
    public SecuritySummary getSecuritySummaryForTenant(UUID tenantId, LocalDateTime from, LocalDateTime to) {
        // This would aggregate security events for reporting
        // For now, return empty summary as placeholder
        return new SecuritySummary(tenantId, from, to, 0, 0, 0, 0);
    }
    
    // ========== Private Helper Methods ==========
    
    /**
     * Core method to log security events
     */
    private void logSecurityEvent(TenantSecurityEvent event) {
        try {
            // Enrich event with current context
            enrichEvent(event);
            
            // Log to structured logger
            logToStructuredLogger(event);
            
            // Store in database audit log
            storeAuditLog(event);
            
        } catch (Exception e) {
            logger.error("Failed to log security event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Enrich event with current context information
     */
    private void enrichEvent(TenantSecurityEvent event) {
        // Add timestamp if not set
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }
        
        // Add current user if not set
        if (event.getUsername() == null) {
            event.setUsername(getCurrentUsername());
        }
        
        // Add session ID if available
        if (event.getSessionId() == null) {
            event.setSessionId(getCurrentSessionId());
        }
        
        // Add request context if available
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            if (event.getIpAddress() == null) {
                event.setIpAddress(getClientIpAddress(request));
            }
            if (event.getUserAgent() == null) {
                event.setUserAgent(request.getHeader("User-Agent"));
            }
            if (event.getRequestId() == null) {
                event.setRequestId(request.getHeader("X-Request-ID"));
            }
        }
    }
    
    /**
     * Log event to structured logger for external log aggregation
     */
    private void logToStructuredLogger(TenantSecurityEvent event) {
        // Create structured log entry
        securityLogger.info("TENANT_SECURITY_EVENT: " +
            "eventType={}, tenantId={}, targetTenantId={}, username={}, " +
            "resource={}, operation={}, severity={}, ipAddress={}, " +
            "description={}, timestamp={}, sessionId={}, requestId={}",
            event.getEventType(),
            event.getTenantId(),
            event.getTargetTenantId(),
            event.getUsername(),
            event.getResource(),
            event.getOperation(),
            event.getSeverity(),
            event.getIpAddress(),
            event.getDescription(),
            event.getTimestamp(),
            event.getSessionId(),
            event.getRequestId()
        );
    }
    
    /**
     * Store audit event in database
     */
    private void storeAuditLog(TenantSecurityEvent event) {
        try {
            // Map SecurityEventType to AuditAction
            AuditLog.AuditAction auditAction = mapSecurityEventToAuditAction(event.getEventType());
            
            // Create audit log using builder pattern
            AuditLog.Builder builder = new AuditLog.Builder(auditAction, "TENANT_SECURITY")
                .withUser(getCurrentUserId(), getCurrentUsername())
                .withEntity(
                    event.getTenantId(),
                    event.getResource() != null ? event.getResource() : "TenantSecurityEvent"
                )
                .withRequest(
                    event.getIpAddress(),
                    event.getUserAgent(), 
                    event.getRequestId()
                )
                .withEndpoint(
                    event.getResource() != null ? "/api/security/" + event.getResource() : "/api/security",
                    event.getOperation() != null ? event.getOperation() : "SECURITY_EVENT"
                )
                .withResult(
                    event.getSeverity() == SecuritySeverity.CRITICAL ? 
                        AuditLog.AuditStatus.FAILURE : AuditLog.AuditStatus.SUCCESS,
                    event.getSeverity() == SecuritySeverity.CRITICAL ? 500 : 200,
                    null // duration not available
                );
            
            // Add error message if critical
            if (event.getSeverity() == SecuritySeverity.CRITICAL) {
                builder.withError(event.getDescription());
            }
            
            // Set company for tenant isolation
            if (event.getTenantId() != null) {
                builder.withCompany(event.getTenantId());
            }
            
            AuditLog auditLog = builder.build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Failed to store audit log: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Map SecurityEventType to AuditAction
     */
    private AuditLog.AuditAction mapSecurityEventToAuditAction(SecurityEventType eventType) {
        return switch (eventType) {
            case TENANT_ACCESS -> AuditLog.AuditAction.READ;
            case TENANT_VIOLATION, CROSS_TENANT_ACCESS -> AuditLog.AuditAction.UNAUTHORIZED_ACCESS;
            case TENANT_SWITCH -> AuditLog.AuditAction.UPDATE;
            case ENTITY_ACCESS -> AuditLog.AuditAction.READ;
            case AUTHENTICATION -> AuditLog.AuditAction.LOGIN;
            case DATA_MODIFICATION -> AuditLog.AuditAction.UPDATE;
            case BULK_OPERATION -> AuditLog.AuditAction.BULK_UPDATE;
            case CONFIGURATION_CHANGE -> AuditLog.AuditAction.SYSTEM_CONFIG_CHANGED;
            case SECURITY_INCIDENT -> AuditLog.AuditAction.SECURITY;
            case API_ACCESS -> AuditLog.AuditAction.API_KEY_USED;
        };
    }
    
    /**
     * Create audit details JSON from security event
     */
    private String createAuditDetails(TenantSecurityEvent event) {
        Map<String, Object> details = new HashMap<>();
        details.put("eventType", event.getEventType());
        details.put("severity", event.getSeverity());
        details.put("description", event.getDescription());
        details.put("targetTenantId", event.getTargetTenantId());
        details.put("resource", event.getResource());
        details.put("resourceId", event.getResourceId());
        details.put("source", event.getSource());
        details.put("sessionId", event.getSessionId());
        details.put("requestId", event.getRequestId());
        
        if (event.getMetadata() != null) {
            details.put("metadata", event.getMetadata());
        }
        
        // Convert to JSON string (simplified)
        return details.toString();
    }
    
    /**
     * Get current tenant ID with fallback
     */
    private UUID getCurrentTenantId() {
        try {
            if (tenantContextService.hasTenantContext()) {
                return tenantContextService.getCurrentTenant();
            }
        } catch (Exception e) {
            logger.debug("Could not get current tenant ID: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Get current username from security context
     */
    private String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                return auth.getName();
            }
        } catch (Exception e) {
            logger.debug("Could not get current username: {}", e.getMessage());
        }
        return "anonymous";
    }
    
    /**
     * Get current user ID from security context
     */
    private UUID getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && 
                auth.getPrincipal() instanceof com.cafm.cafmbackend.infrastructure.persistence.entity.User user) {
                return user.getId();
            }
        } catch (Exception e) {
            logger.debug("Could not get current user ID: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Get current session ID
     */
    private String getCurrentSessionId() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null && request.getSession(false) != null) {
                return request.getSession(false).getId();
            }
        } catch (Exception e) {
            logger.debug("Could not get current session ID: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Get current HTTP request
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            logger.debug("Could not get current request: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String clientIP = request.getHeader("X-Forwarded-For");
        if (clientIP == null || clientIP.isEmpty() || "unknown".equalsIgnoreCase(clientIP)) {
            clientIP = request.getHeader("X-Real-IP");
        }
        if (clientIP == null || clientIP.isEmpty() || "unknown".equalsIgnoreCase(clientIP)) {
            clientIP = request.getRemoteAddr();
        }
        return clientIP != null ? clientIP.split(",")[0].trim() : "unknown";
    }
    
    // ========== Inner Classes and Enums ==========
    
    /**
     * Security event types for tenant operations
     */
    public enum SecurityEventType {
        TENANT_ACCESS,
        TENANT_VIOLATION,
        CROSS_TENANT_ACCESS,
        TENANT_SWITCH,
        ENTITY_ACCESS,
        AUTHENTICATION,
        DATA_MODIFICATION,
        BULK_OPERATION,
        CONFIGURATION_CHANGE,
        SECURITY_INCIDENT,
        API_ACCESS
    }
    
    /**
     * Security severity levels
     */
    public enum SecuritySeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    /**
     * Security event data structure
     */
    public static class TenantSecurityEvent {
        private SecurityEventType eventType;
        private UUID tenantId;
        private UUID targetTenantId;
        private String username;
        private String ipAddress;
        private String userAgent;
        private String sessionId;
        private String requestId;
        private String resource;
        private String resourceId;
        private String operation;
        private String description;
        private String source;
        private SecuritySeverity severity = SecuritySeverity.LOW;
        private LocalDateTime timestamp;
        private Map<String, Object> metadata;
        
        // Builder pattern
        public static TenantSecurityEventBuilder builder() {
            return new TenantSecurityEventBuilder();
        }
        
        // Getters and setters
        public SecurityEventType getEventType() { return eventType; }
        public void setEventType(SecurityEventType eventType) { this.eventType = eventType; }
        
        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
        
        public UUID getTargetTenantId() { return targetTenantId; }
        public void setTargetTenantId(UUID targetTenantId) { this.targetTenantId = targetTenantId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getResource() { return resource; }
        public void setResource(String resource) { this.resource = resource; }
        
        public String getResourceId() { return resourceId; }
        public void setResourceId(String resourceId) { this.resourceId = resourceId; }
        
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        
        public SecuritySeverity getSeverity() { return severity; }
        public void setSeverity(SecuritySeverity severity) { this.severity = severity; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    /**
     * Builder for TenantSecurityEvent
     */
    public static class TenantSecurityEventBuilder {
        private final TenantSecurityEvent event = new TenantSecurityEvent();
        
        public TenantSecurityEventBuilder eventType(SecurityEventType eventType) {
            event.setEventType(eventType);
            return this;
        }
        
        public TenantSecurityEventBuilder tenantId(UUID tenantId) {
            event.setTenantId(tenantId);
            return this;
        }
        
        public TenantSecurityEventBuilder targetTenantId(UUID targetTenantId) {
            event.setTargetTenantId(targetTenantId);
            return this;
        }
        
        public TenantSecurityEventBuilder username(String username) {
            event.setUsername(username);
            return this;
        }
        
        public TenantSecurityEventBuilder ipAddress(String ipAddress) {
            event.setIpAddress(ipAddress);
            return this;
        }
        
        public TenantSecurityEventBuilder userAgent(String userAgent) {
            event.setUserAgent(userAgent);
            return this;
        }
        
        public TenantSecurityEventBuilder resource(String resource) {
            event.setResource(resource);
            return this;
        }
        
        public TenantSecurityEventBuilder resourceId(String resourceId) {
            event.setResourceId(resourceId);
            return this;
        }
        
        public TenantSecurityEventBuilder operation(String operation) {
            event.setOperation(operation);
            return this;
        }
        
        public TenantSecurityEventBuilder description(String description) {
            event.setDescription(description);
            return this;
        }
        
        public TenantSecurityEventBuilder source(String source) {
            event.setSource(source);
            return this;
        }
        
        public TenantSecurityEventBuilder severity(SecuritySeverity severity) {
            event.setSeverity(severity);
            return this;
        }
        
        public TenantSecurityEventBuilder metadata(Map<String, Object> metadata) {
            event.setMetadata(metadata);
            return this;
        }
        
        public TenantSecurityEvent build() {
            return event;
        }
    }
    
    /**
     * Security summary for reporting
     */
    public record SecuritySummary(
        UUID tenantId,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        long totalEvents,
        long violations,
        long incidents,
        long criticalEvents
    ) {}
}