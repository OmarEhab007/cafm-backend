package com.cafm.cafmbackend.application.service;

import com.cafm.cafmbackend.infrastructure.persistence.entity.AuditLog;
import com.cafm.cafmbackend.infrastructure.persistence.entity.AuditLog.AuditAction;
import com.cafm.cafmbackend.infrastructure.persistence.entity.AuditLog.AuditStatus;
import com.cafm.cafmbackend.infrastructure.persistence.repository.AuditLogRepository;
import com.cafm.cafmbackend.application.service.tenant.TenantContextService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Service for audit logging.
 * 
 * Purpose: Provide comprehensive audit logging for all system actions
 * Pattern: Async service with builder pattern
 * Java 23: Virtual threads for async logging
 * Architecture: Service layer with async processing
 * Standards: Non-blocking audit logging
 */
@Service
public class AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final TenantContextService tenantContextService;
    
    @Autowired
    public AuditService(AuditLogRepository auditLogRepository, 
                       ObjectMapper objectMapper,
                       TenantContextService tenantContextService) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.tenantContextService = tenantContextService;
    }
    
    /**
     * Log a simple audit event
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(AuditAction action, String entityType, UUID entityId, String entityName) {
        try {
            AuditLog.Builder builder = new AuditLog.Builder(action, entityType)
                .withEntity(entityId, entityName);
            
            enrichWithContext(builder);
            
            AuditLog auditLog = builder.build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Failed to save audit log", e);
        }
    }
    
    /**
     * Log an audit event with changes
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logChange(AuditAction action, String entityType, UUID entityId, 
                         Object oldValue, Object newValue) {
        try {
            String oldJson = oldValue != null ? objectMapper.writeValueAsString(oldValue) : null;
            String newJson = newValue != null ? objectMapper.writeValueAsString(newValue) : null;
            String changes = calculateChanges(oldValue, newValue);
            
            AuditLog.Builder builder = new AuditLog.Builder(action, entityType)
                .withEntity(entityId, null)
                .withChanges(oldJson, newJson, changes);
            
            enrichWithContext(builder);
            
            AuditLog auditLog = builder.build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Failed to save audit log with changes", e);
        }
    }
    
    /**
     * Log authentication event
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuthentication(AuditAction action, String username, boolean success, 
                                 String ipAddress, String userAgent) {
        try {
            AuditLog.Builder builder = new AuditLog.Builder(action, "Authentication")
                .withRequest(ipAddress, userAgent, UUID.randomUUID().toString());
            
            if (username != null) {
                builder.withUser(null, username);
            }
            
            if (!success) {
                builder.withResult(AuditStatus.FAILURE, 401, null);
            }
            
            AuditLog auditLog = builder.build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Failed to save authentication audit log", e);
        }
    }
    
    /**
     * Log API request
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logApiRequest(String endpoint, String method, int responseCode, 
                             long durationMs, HttpServletRequest request) {
        try {
            AuditAction action = mapMethodToAction(method);
            String entityType = extractEntityType(endpoint);
            
            AuditLog.Builder builder = new AuditLog.Builder(action, entityType)
                .withEndpoint(endpoint, method)
                .withResult(responseCode < 400 ? AuditStatus.SUCCESS : AuditStatus.FAILURE, 
                           responseCode, durationMs)
                .withRequest(getClientIP(request), request.getHeader("User-Agent"), 
                           request.getHeader("X-Request-Id"));
            
            enrichWithContext(builder);
            
            AuditLog auditLog = builder.build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Failed to save API request audit log", e);
        }
    }
    
    /**
     * Log security event
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSecurityEvent(AuditAction action, String details, HttpServletRequest request) {
        try {
            AuditLog.Builder builder = new AuditLog.Builder(action, "Security")
                .withRequest(getClientIP(request), request.getHeader("User-Agent"), 
                           request.getHeader("X-Request-Id"))
                .withChanges(null, null, details);
            
            enrichWithContext(builder);
            
            AuditLog auditLog = builder.build();
            auditLogRepository.save(auditLog);
            
            // Also log to application logs for security monitoring
            logger.warn("Security event: {} - {}", action, details);
            
        } catch (Exception e) {
            logger.error("Failed to save security audit log", e);
        }
    }
    
    /**
     * Log bulk operation
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logBulkOperation(AuditAction action, String entityType, int count, 
                                boolean success, String details) {
        try {
            AuditLog.Builder builder = new AuditLog.Builder(action, entityType)
                .withChanges(null, null, String.format("Bulk operation on %d items: %s", count, details))
                .withResult(success ? AuditStatus.SUCCESS : AuditStatus.FAILURE, null, null);
            
            enrichWithContext(builder);
            
            AuditLog auditLog = builder.build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Failed to save bulk operation audit log", e);
        }
    }
    
    /**
     * Enrich audit log with context information
     */
    private void enrichWithContext(AuditLog.Builder builder) {
        // Get authentication context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            builder.withUser(null, username);
        }
        
        // Get tenant context
        UUID companyId = tenantContextService.getCurrentTenant();
        if (companyId != null) {
            builder.withCompany(companyId);
        }
    }
    
    /**
     * Calculate changes between old and new values
     */
    private String calculateChanges(Object oldValue, Object newValue) {
        // Simple implementation - can be enhanced with deep diff
        if (oldValue == null && newValue != null) {
            return "Created";
        } else if (oldValue != null && newValue == null) {
            return "Deleted";
        } else {
            return "Updated";
        }
    }
    
    /**
     * Map HTTP method to audit action
     */
    private AuditAction mapMethodToAction(String method) {
        return switch (method.toUpperCase()) {
            case "GET" -> AuditAction.READ;
            case "POST" -> AuditAction.CREATE;
            case "PUT", "PATCH" -> AuditAction.UPDATE;
            case "DELETE" -> AuditAction.DELETE;
            default -> AuditAction.READ;
        };
    }
    
    /**
     * Extract entity type from endpoint
     */
    private String extractEntityType(String endpoint) {
        if (endpoint == null) return "Unknown";
        
        String[] parts = endpoint.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            if (!part.isEmpty() && !part.matches("\\d+") && !part.matches("[a-f0-9-]{36}")) {
                return part.substring(0, 1).toUpperCase() + part.substring(1);
            }
        }
        
        return "Unknown";
    }
    
    /**
     * Get client IP address
     */
    private String getClientIP(HttpServletRequest request) {
        if (request == null) return null;
        
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    // ==================== SPECIFIC DOMAIN EVENT LOGGING ====================

    /**
     * Log work order operation.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logWorkOrderOperation(String operation, UUID workOrderId, String details) {
        try {
            AuditAction action = mapOperationToAction(operation);
            AuditLog.Builder builder = new AuditLog.Builder(action, "WorkOrder")
                .withEntity(workOrderId, details);
            
            enrichWithContext(builder);
            
            AuditLog auditLog = builder.build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Failed to save work order audit log", e);
        }
    }

    /**
     * Log report operation.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logReportOperation(String operation, UUID reportId, String details) {
        try {
            AuditAction action = mapOperationToAction(operation);
            AuditLog.Builder builder = new AuditLog.Builder(action, "Report")
                .withEntity(reportId, details);
            
            enrichWithContext(builder);
            
            AuditLog auditLog = builder.build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Failed to save report audit log", e);
        }
    }

    /**
     * Log asset operation.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAssetOperation(String operation, UUID assetId, String details) {
        try {
            AuditAction action = mapOperationToAction(operation);
            AuditLog.Builder builder = new AuditLog.Builder(action, "Asset")
                .withEntity(assetId, details);
            
            enrichWithContext(builder);
            
            AuditLog auditLog = builder.build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Failed to save asset audit log", e);
        }
    }

    /**
     * Log inventory operation.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logInventoryOperation(String operation, UUID itemId, String details) {
        try {
            AuditAction action = mapOperationToAction(operation);
            AuditLog.Builder builder = new AuditLog.Builder(action, "InventoryItem")
                .withEntity(itemId, details);
            
            enrichWithContext(builder);
            
            AuditLog auditLog = builder.build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Failed to save inventory audit log", e);
        }
    }

    /**
     * Log file operation.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFileOperation(String operation, UUID fileId, String details) {
        try {
            AuditAction action = mapOperationToAction(operation);
            AuditLog.Builder builder = new AuditLog.Builder(action, "FileMetadata")
                .withEntity(fileId, details);
            
            enrichWithContext(builder);
            
            AuditLog auditLog = builder.build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Failed to save file audit log", e);
        }
    }

    /**
     * Log notification event.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logNotificationEvent(String eventType, UUID notificationId, UUID userId, String details) {
        try {
            AuditAction action = mapOperationToAction(eventType);
            AuditLog.Builder builder = new AuditLog.Builder(action, "Notification")
                .withEntity(notificationId, details)
                .withUser(userId, null);
            
            enrichWithContext(builder);
            
            AuditLog auditLog = builder.build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Failed to save notification audit log", e);
        }
    }

    /**
     * Log security event (overloaded version without HttpServletRequest).
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSecurityEvent(String eventType, String details) {
        try {
            AuditAction action = mapOperationToAction(eventType);
            AuditLog.Builder builder = new AuditLog.Builder(action, "Security")
                .withChanges(null, null, details);
            
            enrichWithContext(builder);
            
            AuditLog auditLog = builder.build();
            auditLogRepository.save(auditLog);
            
            // Also log to application logs for security monitoring
            logger.warn("Security event: {} - {}", eventType, details);
            
        } catch (Exception e) {
            logger.error("Failed to save security audit log", e);
        }
    }

    /**
     * Log user action.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUserAction(String action, UUID userId, String details) {
        try {
            AuditAction auditAction = mapOperationToAction(action);
            AuditLog.Builder builder = new AuditLog.Builder(auditAction, "User")
                .withEntity(userId, details);
            
            enrichWithContext(builder);
            
            AuditLog auditLog = builder.build();
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            logger.error("Failed to save user action audit log", e);
        }
    }

    // ==================== QUERY METHODS ====================

    /**
     * Get audit logs for a specific entity.
     */
    @Transactional(readOnly = true)
    public java.util.List<AuditLog> getAuditLogsForEntity(String entityType, UUID entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }

    /**
     * Get recent audit logs for company.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AuditLog> getRecentAuditLogs(UUID companyId, 
            org.springframework.data.domain.Pageable pageable) {
        return auditLogRepository.findByCompanyIdOrderByTimestampDesc(companyId, pageable);
    }

    /**
     * Get audit logs by user.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AuditLog> getAuditLogsByUser(UUID userId, 
            org.springframework.data.domain.Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    /**
     * Get audit logs by action type.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AuditLog> getAuditLogsByAction(AuditAction action, 
            UUID companyId, org.springframework.data.domain.Pageable pageable) {
        return auditLogRepository.findByActionAndCompanyIdOrderByTimestampDesc(action, companyId, pageable);
    }

    /**
     * Get audit logs within date range.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AuditLog> getAuditLogsInDateRange(
            LocalDateTime startDate, LocalDateTime endDate, UUID companyId,
            org.springframework.data.domain.Pageable pageable) {
        return auditLogRepository.findByTimestampBetweenAndCompanyIdOrderByTimestampDesc(
            startDate, endDate, companyId, pageable);
    }

    /**
     * Search audit logs.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AuditLog> searchAuditLogs(String searchTerm, 
            UUID companyId, org.springframework.data.domain.Pageable pageable) {
        return auditLogRepository.searchAuditLogs(searchTerm, companyId, pageable);
    }

    /**
     * Get audit statistics.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAuditStatistics(UUID companyId, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> stats = new java.util.HashMap<>();
        
        long totalEvents = auditLogRepository.countByCompanyIdAndTimestampBetween(companyId, startDate, endDate);
        stats.put("totalEvents", totalEvents);
        
        // Count by action type
        Map<AuditAction, Long> actionCounts = new java.util.HashMap<>();
        for (AuditAction action : AuditAction.values()) {
            long count = auditLogRepository.countByActionAndCompanyIdAndTimestampBetween(
                action, companyId, startDate, endDate);
            actionCounts.put(action, count);
        }
        stats.put("actionCounts", actionCounts);
        
        // Count by entity type
        java.util.List<Object[]> entityCounts = auditLogRepository.countByEntityTypeAndCompanyId(companyId, startDate, endDate);
        Map<String, Long> entityStats = new java.util.HashMap<>();
        for (Object[] row : entityCounts) {
            String entityType = (String) row[0];
            Long count = (Long) row[1];
            entityStats.put(entityType, count);
        }
        stats.put("entityCounts", entityStats);
        
        // Get most active users
        java.util.List<Object[]> userCounts = auditLogRepository.countByUserAndCompanyId(companyId, startDate, endDate);
        Map<String, Long> userStats = new java.util.HashMap<>();
        for (Object[] row : userCounts) {
            String username = (String) row[0];
            Long count = (Long) row[1];
            if (username != null) {
                userStats.put(username, count);
            }
        }
        stats.put("userCounts", userStats);
        
        return stats;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Map operation string to AuditAction enum.
     */
    private AuditAction mapOperationToAction(String operation) {
        if (operation == null) return AuditAction.OTHER;
        
        String upperOperation = operation.toUpperCase();
        
        if (upperOperation.contains("CREATE") || upperOperation.contains("ADD") || 
            upperOperation.contains("REGISTER") || upperOperation.contains("UPLOAD")) {
            return AuditAction.CREATE;
        } else if (upperOperation.contains("UPDATE") || upperOperation.contains("MODIFY") || 
                   upperOperation.contains("EDIT") || upperOperation.contains("CHANGE")) {
            return AuditAction.UPDATE;
        } else if (upperOperation.contains("DELETE") || upperOperation.contains("REMOVE") || 
                   upperOperation.contains("UNREGISTER")) {
            return AuditAction.DELETE;
        } else if (upperOperation.contains("LOGIN") || upperOperation.contains("LOGOUT") || 
                   upperOperation.contains("AUTH")) {
            return AuditAction.LOGIN;
        } else if (upperOperation.contains("ACCESS") || upperOperation.contains("VIEW") || 
                   upperOperation.contains("READ")) {
            return AuditAction.ACCESS;
        } else {
            return AuditAction.OTHER;
        }
    }
}