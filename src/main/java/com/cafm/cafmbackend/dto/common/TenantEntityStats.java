package com.cafm.cafmbackend.dto.common;

/**
 * Record for tenant entity statistics.
 * 
 * Explanation:
 * - Purpose: Provides statistical information about entity counts per tenant
 * - Pattern: Java 23 record for immutable data transfer
 * - Java 23: Modern record syntax with comprehensive validation
 * - Architecture: Part of common DTOs for cross-cutting concerns
 * - Standards: Type-safe statistical data with clear naming
 */
public record TenantEntityStats(
    long activeCount,
    long deletedCount,
    long totalCount
) {
    
    /**
     * Compact constructor with validation
     */
    public TenantEntityStats {
        if (activeCount < 0 || deletedCount < 0 || totalCount < 0) {
            throw new IllegalArgumentException("Counts cannot be negative");
        }
        
        if (activeCount + deletedCount != totalCount) {
            throw new IllegalArgumentException("Active + deleted counts must equal total count");
        }
    }
    
    /**
     * Calculate deletion percentage
     * @return Percentage of entities that are deleted (0-100)
     */
    public double deletionPercentage() {
        return totalCount == 0 ? 0.0 : (deletedCount * 100.0) / totalCount;
    }
    
    /**
     * Calculate active percentage
     * @return Percentage of entities that are active (0-100)
     */
    public double activePercentage() {
        return totalCount == 0 ? 0.0 : (activeCount * 100.0) / totalCount;
    }
    
    /**
     * Check if there are any entities
     * @return true if total count is greater than 0
     */
    public boolean hasEntities() {
        return totalCount > 0;
    }
    
    /**
     * Check if there are deleted entities that might need cleanup
     * @return true if there are deleted entities
     */
    public boolean hasDeletedEntities() {
        return deletedCount > 0;
    }
}