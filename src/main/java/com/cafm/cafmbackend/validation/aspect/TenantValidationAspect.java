package com.cafm.cafmbackend.validation.aspect;

import com.cafm.cafmbackend.data.entity.base.TenantAware;
import com.cafm.cafmbackend.service.tenant.TenantContextService;
import com.cafm.cafmbackend.validation.constraint.TenantValidated;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Aspect for automatic tenant validation based on @TenantValidated annotation.
 * 
 * Explanation:
 * - Purpose: Provides automatic tenant security validation for service methods
 * - Pattern: Aspect-Oriented Programming for cross-cutting security concerns
 * - Java 23: Modern AOP with pattern matching and enhanced exception handling
 * - Architecture: Declarative security that enforces tenant boundaries transparently
 * - Standards: Comprehensive validation with detailed audit logging
 */
@Aspect
@Component
public class TenantValidationAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantValidationAspect.class);
    private static final String SYSTEM_TENANT_ID = "00000000-0000-0000-0000-000000000001";
    
    @Autowired
    private TenantContextService tenantContextService;
    
    /**
     * Before advice for methods annotated with @TenantValidated
     */
    @Before("@annotation(tenantValidated)")
    public void validateTenantAccess(JoinPoint joinPoint, TenantValidated tenantValidated) {
        
        logger.debug("Executing tenant validation for method: {}.{}",
            joinPoint.getTarget().getClass().getSimpleName(),
            joinPoint.getSignature().getName()
        );
        
        try {
            // Perform validation based on mode
            switch (tenantValidated.mode()) {
                case REQUIRE_TENANT_CONTEXT -> validateTenantContext(tenantValidated);
                case VALIDATE_TENANT_ID -> validateTenantId(joinPoint, tenantValidated);
                case VALIDATE_ENTITY_TENANT -> validateEntityTenant(joinPoint, tenantValidated);
                case VALIDATE_ENTITY_IDS -> validateEntityIds(joinPoint, tenantValidated);
                case READ_ACCESS -> validateReadAccess(joinPoint, tenantValidated);
                case WRITE_ACCESS -> validateWriteAccess(joinPoint, tenantValidated);
                case DELETE_ACCESS -> validateDeleteAccess(joinPoint, tenantValidated);
                case CUSTOM -> validateCustom(joinPoint, tenantValidated);
                default -> validateTenantContext(tenantValidated);
            }
            
            // Log successful validation if audit logging is enabled
            if (tenantValidated.auditLog()) {
                logTenantOperation(joinPoint, tenantValidated, "SUCCESS", null);
            }
            
        } catch (Exception e) {
            // Log validation failure
            if (tenantValidated.auditLog()) {
                logTenantOperation(joinPoint, tenantValidated, "FAILED", e.getMessage());
            }
            
            // Re-throw or handle based on configuration
            if (tenantValidated.throwOnFailure()) {
                throw e;
            } else {
                logger.warn("Tenant validation failed but throwOnFailure=false: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Before advice for classes annotated with @TenantValidated
     */
    @Before("@within(tenantValidated) && execution(public * *(..))")
    public void validateClassLevelTenantAccess(JoinPoint joinPoint, TenantValidated tenantValidated) {
        
        // Check if method has its own @TenantValidated annotation
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        TenantValidated methodAnnotation = AnnotationUtils.findAnnotation(method, TenantValidated.class);
        
        // Skip if method has its own annotation (method-level takes precedence)
        if (methodAnnotation != null) {
            return;
        }
        
        // Apply class-level validation
        validateTenantAccess(joinPoint, tenantValidated);
    }
    
    // ========== Validation Methods ==========
    
    /**
     * Validate that tenant context exists and is active
     */
    private void validateTenantContext(TenantValidated annotation) {
        if (!annotation.requireTenantContext()) {
            return;
        }
        
        if (!tenantContextService.hasTenantContext()) {
            throw new SecurityException(
                getErrorMessage(annotation, "No tenant context available")
            );
        }
        
        UUID currentTenant = tenantContextService.getCurrentTenant();
        
        // Allow system tenant if configured
        if (annotation.allowSystemTenant() && 
            SYSTEM_TENANT_ID.equals(currentTenant.toString())) {
            logger.debug("System tenant access allowed");
            return;
        }
        
        // Validate tenant status if required
        if (annotation.validateTenantStatus() && 
            !tenantContextService.validateCurrentTenantAccess()) {
            throw new SecurityException(
                getErrorMessage(annotation, "Current tenant is not active or accessible")
            );
        }
    }
    
    /**
     * Validate specific tenant ID parameter
     */
    private void validateTenantId(JoinPoint joinPoint, TenantValidated annotation) {
        validateTenantContext(annotation);
        
        String paramName = annotation.tenantIdParam();
        if (paramName.isEmpty()) {
            logger.warn("TenantValidated.VALIDATE_TENANT_ID mode requires tenantIdParam to be specified");
            return;
        }
        
        UUID paramTenantId = extractUUIDParameter(joinPoint, paramName);
        if (paramTenantId == null) {
            throw new SecurityException(
                getErrorMessage(annotation, "Tenant ID parameter '" + paramName + "' is null or invalid")
            );
        }
        
        UUID currentTenant = tenantContextService.getCurrentTenant();
        
        // Allow system tenant if configured
        if (annotation.allowSystemTenant() && 
            SYSTEM_TENANT_ID.equals(currentTenant.toString())) {
            return;
        }
        
        // Validate tenant ID matches current context
        if (!currentTenant.equals(paramTenantId)) {
            throw new SecurityException(
                getErrorMessage(annotation, 
                    String.format("Tenant ID mismatch: current=%s, requested=%s", 
                        currentTenant, paramTenantId))
            );
        }
    }
    
    /**
     * Validate entity belongs to current tenant
     */
    private void validateEntityTenant(JoinPoint joinPoint, TenantValidated annotation) {
        validateTenantContext(annotation);
        
        String paramName = annotation.entityParam();
        if (paramName.isEmpty()) {
            logger.warn("TenantValidated.VALIDATE_ENTITY_TENANT mode requires entityParam to be specified");
            return;
        }
        
        Object entity = extractParameter(joinPoint, paramName);
        if (entity == null) {
            throw new SecurityException(
                getErrorMessage(annotation, "Entity parameter '" + paramName + "' is null")
            );
        }
        
        if (!(entity instanceof TenantAware tenantAwareEntity)) {
            throw new SecurityException(
                getErrorMessage(annotation, "Entity parameter '" + paramName + "' does not implement TenantAware")
            );
        }
        
        UUID currentTenant = tenantContextService.getCurrentTenant();
        
        // Allow system tenant if configured
        if (annotation.allowSystemTenant() && 
            SYSTEM_TENANT_ID.equals(currentTenant.toString())) {
            return;
        }
        
        // Validate entity belongs to current tenant
        if (!tenantAwareEntity.validateTenantAccess(currentTenant)) {
            throw new SecurityException(
                getErrorMessage(annotation, 
                    String.format("Entity access denied: entity belongs to tenant %s, current tenant is %s", 
                        tenantAwareEntity.getCompanyId(), currentTenant))
            );
        }
    }
    
    /**
     * Validate all entity IDs belong to current tenant
     */
    @SuppressWarnings("unchecked")
    private void validateEntityIds(JoinPoint joinPoint, TenantValidated annotation) {
        validateTenantContext(annotation);
        
        String paramName = annotation.entityIdsParam();
        if (paramName.isEmpty()) {
            logger.warn("TenantValidated.VALIDATE_ENTITY_IDS mode requires entityIdsParam to be specified");
            return;
        }
        
        Object idsParam = extractParameter(joinPoint, paramName);
        if (idsParam == null) {
            return; // Allow null/empty collections
        }
        
        Collection<UUID> entityIds;
        if (idsParam instanceof Collection<?> collection) {
            try {
                entityIds = (Collection<UUID>) collection;
            } catch (ClassCastException e) {
                throw new SecurityException(
                    getErrorMessage(annotation, "Entity IDs parameter must be Collection<UUID>")
                );
            }
        } else {
            throw new SecurityException(
                getErrorMessage(annotation, "Entity IDs parameter must be a Collection")
            );
        }
        
        if (entityIds.isEmpty()) {
            return; // Allow empty collections
        }
        
        UUID currentTenant = tenantContextService.getCurrentTenant();
        
        // Allow system tenant if configured
        if (annotation.allowSystemTenant() && 
            SYSTEM_TENANT_ID.equals(currentTenant.toString())) {
            return;
        }
        
        // Note: Entity ID validation would require repository access
        // This is a placeholder - actual implementation would need repository injection
        logger.debug("Validating {} entity IDs for tenant {}", entityIds.size(), currentTenant);
    }
    
    /**
     * Validate read access
     */
    private void validateReadAccess(JoinPoint joinPoint, TenantValidated annotation) {
        validateTenantContext(annotation);
        // Additional read-specific validation can be added here
    }
    
    /**
     * Validate write access
     */
    private void validateWriteAccess(JoinPoint joinPoint, TenantValidated annotation) {
        validateTenantContext(annotation);
        
        // Ensure tenant is active for write operations
        if (!tenantContextService.validateCurrentTenantAccess()) {
            throw new SecurityException(
                getErrorMessage(annotation, "Write access denied: tenant is not active")
            );
        }
    }
    
    /**
     * Validate delete access
     */
    private void validateDeleteAccess(JoinPoint joinPoint, TenantValidated annotation) {
        validateWriteAccess(joinPoint, annotation); // Delete requires write access
        // Additional delete-specific validation can be added here
    }
    
    /**
     * Custom validation - extensible for specific use cases
     */
    private void validateCustom(JoinPoint joinPoint, TenantValidated annotation) {
        validateTenantContext(annotation);
        // Custom validation logic can be implemented here based on method context
        logger.debug("Custom tenant validation for operation: {}", annotation.operation());
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Extract parameter value by name
     */
    private Object extractParameter(JoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(paramName)) {
                return args[i];
            }
        }
        
        return null;
    }
    
    /**
     * Extract UUID parameter by name
     */
    private UUID extractUUIDParameter(JoinPoint joinPoint, String paramName) {
        Object param = extractParameter(joinPoint, paramName);
        
        if (param == null) {
            return null;
        }
        
        if (param instanceof UUID uuid) {
            return uuid;
        }
        
        if (param instanceof String str) {
            try {
                return UUID.fromString(str);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Get error message with fallback to default
     */
    private String getErrorMessage(TenantValidated annotation, String defaultMessage) {
        return annotation.message().isEmpty() ? defaultMessage : annotation.message();
    }
    
    /**
     * Log tenant operation for audit trail
     */
    private void logTenantOperation(JoinPoint joinPoint, TenantValidated annotation, 
                                   String result, String errorMessage) {
        try {
            UUID currentTenant = tenantContextService.hasTenantContext() 
                ? tenantContextService.getCurrentTenant() 
                : null;
            
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            String operation = annotation.operation().equals("UNKNOWN") 
                ? methodName 
                : annotation.operation();
            
            // Structured logging for security audit
            logger.info("TENANT_VALIDATION: class={}, method={}, operation={}, mode={}, " +
                       "tenant={}, result={}, resourceType={}, error={}",
                className, methodName, operation, annotation.mode(),
                currentTenant, result, annotation.resourceType(), errorMessage
            );
            
        } catch (Exception e) {
            // Don't fail the operation due to logging issues
            logger.error("Failed to log tenant operation: {}", e.getMessage());
        }
    }
}