package com.cafm.cafmbackend.shared.util;

import java.util.UUID;

/**
 * Utility class for managing tenant context in multi-tenant applications.
 * 
 * This class provides static methods to manage the current tenant (company) context
 * throughout the application lifecycle. It uses ThreadLocal to ensure thread-safe
 * access to tenant information.
 * 
 * Pattern: Thread-local context holder for tenant isolation
 * Usage: Set tenant context at request boundaries, access in business logic
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_COMPANY_ID = new ThreadLocal<>();
    
    private TenantContext() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Sets the current company (tenant) ID for the current thread.
     * This should be called at the beginning of request processing.
     */
    public static void setCurrentCompanyId(UUID companyId) {
        CURRENT_COMPANY_ID.set(companyId);
    }
    
    /**
     * Gets the current company (tenant) ID for the current thread.
     * Returns null if no tenant context is set.
     */
    public static UUID getCurrentCompanyId() {
        return CURRENT_COMPANY_ID.get();
    }
    
    /**
     * Clears the tenant context for the current thread.
     * This should be called at the end of request processing to prevent memory leaks.
     */
    public static void clear() {
        CURRENT_COMPANY_ID.remove();
    }
    
    /**
     * Checks if a tenant context is currently set.
     */
    public static boolean isSet() {
        return CURRENT_COMPANY_ID.get() != null;
    }
    
    /**
     * Gets the current company ID, throwing an exception if not set.
     * Use this when tenant context is required.
     */
    public static UUID requireCurrentCompanyId() {
        UUID companyId = getCurrentCompanyId();
        if (companyId == null) {
            throw new IllegalStateException("No tenant context set for current thread");
        }
        return companyId;
    }
    
    /**
     * Executes a block of code with a specific tenant context.
     * Automatically cleans up the context after execution.
     */
    public static <T> T executeWithTenant(UUID companyId, java.util.function.Supplier<T> supplier) {
        UUID previousCompanyId = getCurrentCompanyId();
        try {
            setCurrentCompanyId(companyId);
            return supplier.get();
        } finally {
            if (previousCompanyId != null) {
                setCurrentCompanyId(previousCompanyId);
            } else {
                clear();
            }
        }
    }
    
    /**
     * Executes a block of code with a specific tenant context (void return).
     * Automatically cleans up the context after execution.
     */
    public static void executeWithTenant(UUID companyId, Runnable runnable) {
        UUID previousCompanyId = getCurrentCompanyId();
        try {
            setCurrentCompanyId(companyId);
            runnable.run();
        } finally {
            if (previousCompanyId != null) {
                setCurrentCompanyId(previousCompanyId);
            } else {
                clear();
            }
        }
    }
}