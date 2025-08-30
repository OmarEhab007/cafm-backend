package com.cafm.cafmbackend.dto.mobile;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Mobile synchronization conflict DTO.
 * 
 * Purpose: Represents data conflicts between mobile and server versions
 * Pattern: Conflict resolution DTO with metadata for decision making
 * Java 23: Record-based conflict representation with resolution strategies
 * Architecture: Mobile sync layer conflict resolution contract
 * Standards: Follows conflict resolution patterns for offline-first systems
 */
public record MobileSyncConflict(
    UUID conflictId,
    String entityType,
    UUID entityId,
    ConflictType conflictType,
    Map<String, Object> clientData,
    Map<String, Object> serverData,
    LocalDateTime clientModified,
    LocalDateTime serverModified,
    String conflictDescription,
    ResolutionStrategy suggestedResolution,
    ConflictMetadata metadata
) {
    
    /**
     * Types of sync conflicts.
     */
    public enum ConflictType {
        UPDATE_UPDATE, // Both client and server modified
        UPDATE_DELETE, // Client updated, server deleted
        DELETE_UPDATE, // Client deleted, server updated
        CONCURRENT_CREATE, // Same entity created on both sides
        VERSION_MISMATCH // Version numbers don't match
    }
    
    /**
     * Automatic conflict resolution strategies.
     */
    public enum ResolutionStrategy {
        SERVER_WINS,    // Use server version
        CLIENT_WINS,    // Use client version
        MERGE_FIELDS,   // Merge non-conflicting fields
        MANUAL_REVIEW,  // Require user intervention
        NEWEST_WINS,    // Use most recently modified
        FIELD_LEVEL     // Field-by-field resolution
    }
    
    /**
     * Get the suggested resolution based on conflict analysis.
     */
    public ResolutionStrategy getSuggestedResolution() {
        return switch (conflictType) {
            case UPDATE_UPDATE -> {
                // Use timestamp comparison for concurrent updates
                if (serverModified.isAfter(clientModified)) {
                    yield ResolutionStrategy.SERVER_WINS;
                } else if (clientModified.isAfter(serverModified)) {
                    yield ResolutionStrategy.CLIENT_WINS;
                } else {
                    yield ResolutionStrategy.MANUAL_REVIEW;
                }
            }
            case UPDATE_DELETE -> ResolutionStrategy.MANUAL_REVIEW; // Critical conflict
            case DELETE_UPDATE -> ResolutionStrategy.MANUAL_REVIEW; // Critical conflict
            case CONCURRENT_CREATE -> ResolutionStrategy.MERGE_FIELDS;
            case VERSION_MISMATCH -> ResolutionStrategy.SERVER_WINS;
        };
    }
    
    /**
     * Check if conflict requires manual intervention.
     */
    public boolean requiresManualResolution() {
        return getSuggestedResolution() == ResolutionStrategy.MANUAL_REVIEW ||
               conflictType == ConflictType.UPDATE_DELETE ||
               conflictType == ConflictType.DELETE_UPDATE;
    }
    
    /**
     * Get conflict severity for prioritization.
     */
    public ConflictSeverity getSeverity() {
        return switch (conflictType) {
            case UPDATE_DELETE, DELETE_UPDATE -> ConflictSeverity.CRITICAL;
            case UPDATE_UPDATE -> ConflictSeverity.HIGH;
            case VERSION_MISMATCH -> ConflictSeverity.MEDIUM;
            case CONCURRENT_CREATE -> ConflictSeverity.LOW;
        };
    }
    
    /**
     * Conflict severity levels.
     */
    public enum ConflictSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    /**
     * Additional metadata for conflict resolution.
     */
    public record ConflictMetadata(
        String userRole,
        String deviceId,
        int retryCount,
        boolean isOfflineGenerated,
        Map<String, String> fieldConflicts // Field name -> conflict description
    ) {
        
        /**
         * Check if this is a repeated conflict.
         */
        public boolean isRepeatedConflict() {
            return retryCount > 0;
        }
        
        /**
         * Check if conflict originated from offline changes.
         */
        public boolean isOfflineConflict() {
            return isOfflineGenerated;
        }
    }
    
    /**
     * Create a simple update-update conflict.
     */
    public static MobileSyncConflict updateConflict(
            String entityType,
            UUID entityId,
            Map<String, Object> clientData,
            Map<String, Object> serverData,
            LocalDateTime clientModified,
            LocalDateTime serverModified) {
        
        return new MobileSyncConflict(
            UUID.randomUUID(),
            entityType,
            entityId,
            ConflictType.UPDATE_UPDATE,
            clientData,
            serverData,
            clientModified,
            serverModified,
            "Both client and server modified the same entity",
            null, // Will be calculated
            new ConflictMetadata(null, null, 0, false, Map.of())
        );
    }
}