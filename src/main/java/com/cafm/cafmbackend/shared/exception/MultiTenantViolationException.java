package com.cafm.cafmbackend.shared.exception;

/**
 * Exception thrown when a multi-tenant isolation rule is violated.
 * This represents a security violation where a user attempts to access
 * resources from a different tenant than their own.
 * 
 * Architecture: Security-focused exception for tenant boundary violations
 * Pattern: Custom runtime exception with security context
 * Security: Critical for maintaining tenant isolation in multi-tenant architecture
 * Standards: Extends RuntimeException for unchecked exception handling
 */
public class MultiTenantViolationException extends RuntimeException {
    
    private final String userTenantId;
    private final String requestedTenantId;
    private final String resourceType;
    private final String resourceId;
    
    /**
     * Create a multi-tenant violation exception with basic information.
     * 
     * @param message The violation description
     */
    public MultiTenantViolationException(String message) {
        super(message);
        this.userTenantId = null;
        this.requestedTenantId = null;
        this.resourceType = null;
        this.resourceId = null;
    }
    
    /**
     * Create a multi-tenant violation exception with detailed context.
     * 
     * @param message The violation description
     * @param userTenantId The tenant ID of the requesting user
     * @param requestedTenantId The tenant ID of the requested resource
     */
    public MultiTenantViolationException(String message, String userTenantId, String requestedTenantId) {
        super(message);
        this.userTenantId = userTenantId;
        this.requestedTenantId = requestedTenantId;
        this.resourceType = null;
        this.resourceId = null;
    }
    
    /**
     * Create a multi-tenant violation exception with full context.
     * 
     * @param message The violation description
     * @param userTenantId The tenant ID of the requesting user
     * @param requestedTenantId The tenant ID of the requested resource
     * @param resourceType The type of resource being accessed
     * @param resourceId The ID of the resource being accessed
     */
    public MultiTenantViolationException(String message, String userTenantId, String requestedTenantId, 
                                       String resourceType, String resourceId) {
        super(message);
        this.userTenantId = userTenantId;
        this.requestedTenantId = requestedTenantId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    /**
     * Create a multi-tenant violation exception with cause.
     * 
     * @param message The violation description
     * @param cause The underlying cause
     */
    public MultiTenantViolationException(String message, Throwable cause) {
        super(message, cause);
        this.userTenantId = null;
        this.requestedTenantId = null;
        this.resourceType = null;
        this.resourceId = null;
    }
    
    /**
     * Factory method for cross-tenant access violations.
     * 
     * @param userTenantId The tenant ID of the requesting user
     * @param requestedTenantId The tenant ID of the requested resource
     * @return MultiTenantViolationException instance
     */
    public static MultiTenantViolationException crossTenantAccess(String userTenantId, String requestedTenantId) {
        String message = String.format("Cross-tenant access denied: User tenant '%s' cannot access resources from tenant '%s'", 
            userTenantId, requestedTenantId);
        return new MultiTenantViolationException(message, userTenantId, requestedTenantId);
    }
    
    /**
     * Factory method for resource-specific tenant violations.
     * 
     * @param userTenantId The tenant ID of the requesting user
     * @param requestedTenantId The tenant ID of the requested resource
     * @param resourceType The type of resource
     * @param resourceId The resource identifier
     * @return MultiTenantViolationException instance
     */
    public static MultiTenantViolationException resourceAccess(String userTenantId, String requestedTenantId,
                                                              String resourceType, String resourceId) {
        String message = String.format("Tenant isolation violation: Cannot access %s '%s' from different tenant", 
            resourceType, resourceId);
        return new MultiTenantViolationException(message, userTenantId, requestedTenantId, resourceType, resourceId);
    }
    
    /**
     * Factory method for missing tenant context violations.
     * 
     * @return MultiTenantViolationException instance
     */
    public static MultiTenantViolationException missingTenantContext() {
        return new MultiTenantViolationException("Tenant context is required but not available");
    }
    
    public String getUserTenantId() {
        return userTenantId;
    }
    
    public String getRequestedTenantId() {
        return requestedTenantId;
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public String getResourceId() {
        return resourceId;
    }
}