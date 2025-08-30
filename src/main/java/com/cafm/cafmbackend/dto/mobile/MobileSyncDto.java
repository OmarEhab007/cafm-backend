package com.cafm.cafmbackend.dto.mobile;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base interface for mobile synchronization DTOs.
 * 
 * Purpose: Common contract for all mobile-optimized data transfer objects
 * Pattern: Interface segregation for mobile sync capabilities
 * Java 23: Interface with default methods for sync metadata
 * Architecture: Mobile sync layer base contract
 * Standards: Provides consistent sync metadata across all mobile DTOs
 */
public interface MobileSyncDto {
    
    /**
     * Get the type of entity for sync routing.
     */
    String getEntityType();
    
    /**
     * Get the unique identifier of the entity.
     */
    UUID getEntityId();
    
    /**
     * Get the last modification timestamp for conflict resolution.
     */
    LocalDateTime getLastModified();
    
    /**
     * Get the version for optimistic locking.
     */
    Long getVersion();
    
    /**
     * Check if this DTO represents a deleted entity.
     */
    default boolean isDeleted() {
        return false;
    }
    
    /**
     * Get sync priority (higher = more important).
     */
    default int getSyncPriority() {
        return 1;
    }
    
    /**
     * Check if this entity should be synced based on user permissions.
     */
    default boolean shouldSync(UUID userId, String userRole) {
        return true; // Default: sync everything
    }
}