package com.cafm.cafmbackend.dto.mobile;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Mobile sync response DTO for sending server changes to mobile devices.
 * 
 * Purpose: Provides server-side changes and conflict resolutions to mobile clients
 * Pattern: Immutable record with comprehensive sync result structure
 * Java 23: Uses records for DTOs with pattern matching support
 * Architecture: API layer DTO for sync response with conflict handling
 * Standards: JSON serialization, validation, comprehensive error reporting
 */
public record MobileSyncResponse(
    @JsonProperty("sync_id")
    @NotNull
    String syncId,
    
    @JsonProperty("sync_status")
    @NotNull
    SyncStatus syncStatus,
    
    @JsonProperty("server_changes")
    @NotNull
    ServerChanges serverChanges,
    
    @JsonProperty("conflicts")
    List<SyncConflict> conflicts,
    
    @JsonProperty("errors")
    List<SyncError> errors,
    
    @JsonProperty("server_timestamp")
    @NotNull
    LocalDateTime serverTimestamp,
    
    @JsonProperty("next_sync_token")
    String nextSyncToken,
    
    @JsonProperty("statistics")
    SyncStatistics statistics,
    
    @JsonProperty("metadata")
    Map<String, Object> metadata
) {
    
    public enum SyncStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        FAILED,
        CONFLICTS_PENDING
    }
    
    /**
     * Server changes to be applied on client.
     */
    public record ServerChanges(
        @JsonProperty("created")
        @NotNull
        List<EntityData> created,
        
        @JsonProperty("updated")
        @NotNull
        List<EntityData> updated,
        
        @JsonProperty("deleted")
        @NotNull
        List<DeletedEntity> deleted,
        
        @JsonProperty("change_count")
        int changeCount
    ) {}
    
    /**
     * Entity data from server.
     */
    public record EntityData(
        @JsonProperty("entity_type")
        @NotNull
        String entityType,
        
        @JsonProperty("entity_id")
        @NotNull
        String entityId,
        
        @JsonProperty("data")
        @NotNull
        Map<String, Object> data,
        
        @JsonProperty("version")
        Long version,
        
        @JsonProperty("last_modified")
        LocalDateTime lastModified,
        
        @JsonProperty("checksum")
        String checksum
    ) {}
    
    /**
     * Deleted entity information.
     */
    public record DeletedEntity(
        @JsonProperty("entity_type")
        @NotNull
        String entityType,
        
        @JsonProperty("entity_id")
        @NotNull
        String entityId,
        
        @JsonProperty("deleted_at")
        @NotNull
        LocalDateTime deletedAt
    ) {}
    
    /**
     * Sync conflict information.
     */
    public record SyncConflict(
        @JsonProperty("conflict_id")
        @NotNull
        String conflictId,
        
        @JsonProperty("entity_type")
        @NotNull
        String entityType,
        
        @JsonProperty("entity_id")
        String entityId,
        
        @JsonProperty("conflict_type")
        @NotNull
        String conflictType,
        
        @JsonProperty("client_data")
        Map<String, Object> clientData,
        
        @JsonProperty("server_data")
        Map<String, Object> serverData,
        
        @JsonProperty("resolution")
        ConflictResolution resolution,
        
        @JsonProperty("message")
        String message
    ) {}
    
    /**
     * Conflict resolution information.
     */
    public record ConflictResolution(
        @JsonProperty("strategy")
        @NotNull
        String strategy,
        
        @JsonProperty("resolved_data")
        Map<String, Object> resolvedData,
        
        @JsonProperty("resolved_by")
        String resolvedBy,
        
        @JsonProperty("resolved_at")
        LocalDateTime resolvedAt
    ) {}
    
    /**
     * Sync error information.
     */
    public record SyncError(
        @JsonProperty("entity_type")
        String entityType,
        
        @JsonProperty("entity_id")
        String entityId,
        
        @JsonProperty("error_code")
        @NotNull
        String errorCode,
        
        @JsonProperty("message")
        @NotNull
        String message,
        
        @JsonProperty("details")
        Map<String, Object> details
    ) {}
    
    /**
     * Sync statistics.
     */
    public record SyncStatistics(
        @JsonProperty("client_changes_processed")
        int clientChangesProcessed,
        
        @JsonProperty("server_changes_sent")
        int serverChangesSent,
        
        @JsonProperty("conflicts_detected")
        int conflictsDetected,
        
        @JsonProperty("conflicts_resolved")
        int conflictsResolved,
        
        @JsonProperty("errors_count")
        int errorsCount,
        
        @JsonProperty("sync_duration_ms")
        long syncDurationMs,
        
        @JsonProperty("data_size_bytes")
        long dataSizeBytes
    ) {}
}