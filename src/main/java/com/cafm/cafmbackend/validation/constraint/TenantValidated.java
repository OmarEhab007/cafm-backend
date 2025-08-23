package com.cafm.cafmbackend.validation.constraint;

import java.lang.annotation.*;

/**
 * Annotation to mark methods that require tenant validation.
 * 
 * Explanation:
 * - Purpose: Declarative tenant security validation for service methods
 * - Pattern: Aspect-Oriented Programming (AOP) for cross-cutting security concerns
 * - Java 23: Modern annotation syntax with comprehensive configuration options
 * - Architecture: Method-level security with automatic tenant boundary enforcement
 * - Standards: Type-safe tenant validation with clear error messaging
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TenantValidated {
    
    /**
     * Require active tenant context
     * @return true if tenant context is required (default: true)
     */
    boolean requireTenantContext() default true;
    
    /**
     * Allow system tenant to bypass validation
     * @return true if system tenant is allowed (default: true)
     */
    boolean allowSystemTenant() default true;
    
    /**
     * Parameter name containing the tenant ID to validate against
     * If not specified, will use current tenant context
     * @return parameter name or empty string
     */
    String tenantIdParam() default "";
    
    /**
     * Parameter name containing the entity to validate tenant access for
     * The entity must implement TenantAware interface
     * @return parameter name or empty string
     */
    String entityParam() default "";
    
    /**
     * Parameter name containing list of entity IDs to validate
     * All IDs must belong to the current tenant
     * @return parameter name or empty string
     */
    String entityIdsParam() default "";
    
    /**
     * Operation type for audit logging
     * @return operation type (default: "UNKNOWN")
     */
    String operation() default "UNKNOWN";
    
    /**
     * Custom error message when tenant validation fails
     * @return error message or empty string for default message
     */
    String message() default "";
    
    /**
     * Whether to log tenant operations for audit purposes
     * @return true to enable audit logging (default: true)
     */
    boolean auditLog() default true;
    
    /**
     * Whether to throw exception on validation failure or just log
     * @return true to throw exception (default: true)
     */
    boolean throwOnFailure() default true;
    
    /**
     * Validation mode for different scenarios
     */
    enum ValidationMode {
        /** Validate that current tenant context exists and is active */
        REQUIRE_TENANT_CONTEXT,
        
        /** Validate that specified tenant ID matches current context */
        VALIDATE_TENANT_ID,
        
        /** Validate that entity belongs to current tenant */
        VALIDATE_ENTITY_TENANT,
        
        /** Validate that all entity IDs belong to current tenant */
        VALIDATE_ENTITY_IDS,
        
        /** Validate tenant access for read operations */
        READ_ACCESS,
        
        /** Validate tenant access for write operations */
        WRITE_ACCESS,
        
        /** Validate tenant access for delete operations */
        DELETE_ACCESS,
        
        /** Custom validation - handled by aspect */
        CUSTOM
    }
    
    /**
     * Validation mode to use
     * @return validation mode (default: REQUIRE_TENANT_CONTEXT)
     */
    ValidationMode mode() default ValidationMode.REQUIRE_TENANT_CONTEXT;
    
    /**
     * Whether to validate tenant is active and accessible
     * @return true to validate tenant status (default: true)
     */
    boolean validateTenantStatus() default true;
    
    /**
     * Resource type being accessed (for audit logging)
     * @return resource type or empty string
     */
    String resourceType() default "";
}