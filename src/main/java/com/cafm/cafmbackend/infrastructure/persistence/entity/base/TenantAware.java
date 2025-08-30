package com.cafm.cafmbackend.infrastructure.persistence.entity.base;

import com.cafm.cafmbackend.infrastructure.persistence.entity.Company;

import java.util.UUID;

/**
 * Interface for entities that are tenant-aware in multi-tenant architecture.
 * 
 * Explanation:
 * - Purpose: Defines contract for tenant isolation at entity level
 * - Pattern: Strategy pattern for tenant-aware operations
 * - Java 23: Uses modern interface features with default methods
 * - Architecture: Core multi-tenant abstraction for data layer
 * - Standards: Provides type safety and compile-time checks for tenant operations
 */
public interface TenantAware {
    
    /**
     * Get the company/tenant this entity belongs to
     * @return Company entity or null if not assigned
     */
    Company getCompany();
    
    /**
     * Set the company/tenant for this entity
     * @param company Company entity to assign
     */
    void setCompany(Company company);
    
    /**
     * Get the company ID (tenant ID) for this entity
     * @return UUID of the company or null if not assigned
     */
    default UUID getCompanyId() {
        Company company = getCompany();
        return company != null ? company.getId() : null;
    }
    
    /**
     * Check if this entity belongs to the specified company
     * @param companyId Company ID to check against
     * @return true if entity belongs to the company, false otherwise
     */
    default boolean belongsToCompany(UUID companyId) {
        if (companyId == null) {
            return false;
        }
        UUID entityCompanyId = getCompanyId();
        return companyId.equals(entityCompanyId);
    }
    
    /**
     * Check if this entity has a valid tenant assignment
     * @return true if company is assigned, false otherwise
     */
    default boolean hasTenant() {
        return getCompany() != null;
    }
    
    /**
     * Validate that this entity can be accessed by the specified tenant
     * @param requestingTenantId The tenant attempting to access this entity
     * @return true if access is allowed, false otherwise
     */
    default boolean validateTenantAccess(UUID requestingTenantId) {
        if (requestingTenantId == null) {
            return false;
        }
        
        // Allow system tenant to access everything
        if ("00000000-0000-0000-0000-000000000001".equals(requestingTenantId.toString())) {
            return true;
        }
        
        return belongsToCompany(requestingTenantId);
    }
    
    /**
     * Get a human-readable description of the tenant assignment
     * @return String description for logging/debugging
     */
    default String getTenantDescription() {
        Company company = getCompany();
        if (company == null) {
            return "No tenant assigned";
        }
        return String.format("Tenant: %s (ID: %s)", company.getName(), company.getId());
    }
}