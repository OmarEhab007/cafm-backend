package com.cafm.cafmbackend.shared.exception;

import java.util.UUID;

/**
 * Exception thrown when a tenant access violation occurs.
 * 
 * Purpose: Signals unauthorized cross-tenant access attempts
 * Pattern: Domain-specific security exception with audit context
 * Java 23: Enhanced exception with tenant context
 * Architecture: Multi-tenant security enforcement exception
 * Standards: Security-aware exception with proper audit logging
 */
public class TenantAccessViolationException extends SecurityException {

    private final UUID tenantId;
    private final String resource;
    private final String operation;

    public TenantAccessViolationException(String message, UUID tenantId, String resource, String operation) {
        super(message);
        this.tenantId = tenantId;
        this.resource = resource;
        this.operation = operation;
    }

    public TenantAccessViolationException(String message, Throwable cause, UUID tenantId, String resource, String operation) {
        super(message, cause);
        this.tenantId = tenantId;
        this.resource = resource;
        this.operation = operation;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getResource() {
        return resource;
    }

    public String getOperation() {
        return operation;
    }

    public static TenantAccessViolationException create(UUID tenantId, String resource, String operation) {
        String message = String.format("Access denied to %s for operation %s in tenant %s", 
                                      resource, operation, tenantId);
        return new TenantAccessViolationException(message, tenantId, resource, operation);
    }

    public static TenantAccessViolationException createForCrossTenantAccess(UUID attemptedTenantId, UUID actualTenantId, String resource) {
        String message = String.format("Attempted cross-tenant access to %s: user tenant %s, resource tenant %s", 
                                      resource, actualTenantId, attemptedTenantId);
        return new TenantAccessViolationException(message, attemptedTenantId, resource, "ACCESS");
    }
}