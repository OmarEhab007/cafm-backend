package com.cafm.cafmbackend.security;

import java.util.UUID;

/**
 * Thread-local storage for current tenant context.
 * 
 * Purpose: Provides secure multi-tenant context management
 * Pattern: Thread-local storage pattern for request-scoped data
 * Java 23: Leverages enhanced thread-local operations
 * Architecture: Security utility for tenant isolation
 * Standards: Thread-safe tenant context with proper cleanup
 */
public final class TenantContext {
    
    private static final ThreadLocal<UUID> currentCompanyId = new ThreadLocal<>();
    private static final ThreadLocal<UUID> currentUserId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUserEmail = new ThreadLocal<>();
    
    // Private constructor to prevent instantiation
    private TenantContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Set the current company ID for the request thread.
     * 
     * @param companyId The company ID to set
     * @throws IllegalArgumentException if companyId is null
     */
    public static void setCurrentCompanyId(UUID companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID cannot be null");
        }
        currentCompanyId.set(companyId);
    }
    
    /**
     * Get the current company ID for the request thread.
     * 
     * @return Current company ID
     * @throws IllegalStateException if no company ID is set
     */
    public static UUID getCurrentCompanyId() {
        UUID companyId = currentCompanyId.get();
        if (companyId == null) {
            throw new IllegalStateException("No company ID set in current context");
        }
        return companyId;
    }
    
    /**
     * Check if a company ID is set in the current context.
     * 
     * @return true if company ID is set, false otherwise
     */
    public static boolean hasCurrentCompanyId() {
        return currentCompanyId.get() != null;
    }
    
    /**
     * Set the current user ID for the request thread.
     * 
     * @param userId The user ID to set
     */
    public static void setCurrentUserId(UUID userId) {
        currentUserId.set(userId);
    }
    
    /**
     * Get the current user ID for the request thread.
     * 
     * @return Current user ID, may be null if not set
     */
    public static UUID getCurrentUserId() {
        return currentUserId.get();
    }
    
    /**
     * Set the current user email for the request thread.
     * 
     * @param email The user email to set
     */
    public static void setCurrentUserEmail(String email) {
        currentUserEmail.set(email);
    }
    
    /**
     * Get the current user email for the request thread.
     * 
     * @return Current user email, may be null if not set
     */
    public static String getCurrentUserEmail() {
        return currentUserEmail.get();
    }
    
    /**
     * Clear all tenant context data for the current thread.
     * This should be called after each request to prevent memory leaks.
     */
    public static void clear() {
        currentCompanyId.remove();
        currentUserId.remove();
        currentUserEmail.remove();
    }
    
    /**
     * Set complete tenant context in one call.
     * 
     * @param companyId The company ID
     * @param userId The user ID (optional)
     * @param userEmail The user email (optional)
     */
    public static void setContext(UUID companyId, UUID userId, String userEmail) {
        setCurrentCompanyId(companyId);
        if (userId != null) {
            setCurrentUserId(userId);
        }
        if (userEmail != null) {
            setCurrentUserEmail(userEmail);
        }
    }
    
    /**
     * Get a summary of the current tenant context for logging.
     * 
     * @return Context summary string
     */
    public static String getContextSummary() {
        return String.format("TenantContext[companyId=%s, userId=%s, userEmail=%s]",
                getCurrentCompanyId(), getCurrentUserId(), getCurrentUserEmail());
    }
}