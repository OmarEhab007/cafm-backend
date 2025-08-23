package com.cafm.cafmbackend.dto.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response containing synchronized data for mobile app.
 * Optimized for efficient data transfer and offline storage.
 */
public record SyncResponse(
    // Sync metadata
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime serverTime,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime syncTimestamp,
    String syncToken,
    
    // Sync results
    SyncStatus status,
    List<SyncError> errors,
    List<SyncConflict> conflicts,
    
    // Updated data from server
    ServerData serverData,
    
    // Acknowledgments for local changes
    ChangeAcknowledgments acknowledgments,
    
    // Instructions for client
    ClientInstructions instructions,
    
    // Performance metrics
    SyncMetrics metrics
) {
    /**
     * Sync status
     */
    public enum SyncStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        CONFLICT,
        FAILED,
        REQUIRES_FULL_SYNC
    }
    
    /**
     * Sync error details
     */
    public record SyncError(
        String entityType,
        UUID entityId,
        String errorCode,
        String errorMessage,
        String resolution
    ) {}
    
    /**
     * Sync conflict that needs resolution
     */
    public record SyncConflict(
        String entityType,
        UUID entityId,
        String fieldName,
        Object localValue,
        Object serverValue,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime localTimestamp,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime serverTimestamp,
        String resolutionStrategy // SERVER_WINS, CLIENT_WINS, MERGE
    ) {}
    
    /**
     * Server data updates
     */
    public record ServerData(
        // Entity updates
        List<SchoolData> schools,
        List<ReportData> reports,
        List<WorkOrderData> workOrders,
        List<UserData> users,
        
        // Reference data
        ReferenceData referenceData,
        
        // Deleted entities
        List<DeletedEntity> deletedEntities,
        
        // File URLs that need downloading
        List<FileDownload> pendingDownloads
    ) {
        public record SchoolData(
            UUID id,
            String code,
            String name,
            String district,
            String address,
            Double latitude,
            Double longitude,
            String principalName,
            String principalPhone,
            Boolean isActive,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime updatedAt
        ) {}
        
        public record ReportData(
            UUID id,
            String reportNumber,
            String title,
            String description,
            String status,
            String priority,
            UUID schoolId,
            UUID createdById,
            String category,
            String location,
            Boolean isUrgent,
            Boolean isSafetyHazard,
            List<String> photoUrls,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime createdAt,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime updatedAt
        ) {}
        
        public record WorkOrderData(
            UUID id,
            String workOrderNumber,
            UUID reportId,
            String title,
            String status,
            String priority,
            UUID assignedToId,
            UUID schoolId,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime scheduledStart,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime scheduledEnd,
            List<TaskData> tasks,
            Integer progressPercentage,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime updatedAt
        ) {
            public record TaskData(
                UUID id,
                String title,
                String status,
                Boolean isMandatory,
                String notes
            ) {}
        }
        
        public record UserData(
            UUID id,
            String name,
            String email,
            String phone,
            String role,
            String specialization,
            Boolean isActive,
            String photoUrl
        ) {}
        
        public record ReferenceData(
            Map<String, List<String>> categories,
            Map<String, List<String>> priorities,
            Map<String, List<String>> statuses,
            Map<String, String> translations,
            List<MaintenanceType> maintenanceTypes,
            List<AssetType> assetTypes,
            ConfigData config
        ) {
            public record MaintenanceType(
                String code,
                String name,
                String category,
                Integer estimatedMinutes
            ) {}
            
            public record AssetType(
                String code,
                String name,
                String category,
                List<String> commonIssues
            ) {}
            
            public record ConfigData(
                Integer photoMaxSize,
                Integer photoQuality,
                Integer offlineDaysLimit,
                Integer syncIntervalMinutes,
                Boolean autoSync,
                List<String> requiredFields
            ) {}
        }
        
        public record DeletedEntity(
            String entityType,
            UUID entityId,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime deletedAt
        ) {}
        
        public record FileDownload(
            UUID fileId,
            String url,
            String entityType,
            UUID entityId,
            String mimeType,
            Long sizeInBytes,
            String checksum,
            Boolean isRequired
        ) {}
    }
    
    /**
     * Acknowledgments for client changes
     */
    public record ChangeAcknowledgments(
        Map<UUID, UUID> localToServerIdMapping,
        List<ChangeResult> reportResults,
        List<ChangeResult> workOrderResults,
        List<ChangeResult> taskResults,
        List<PhotoUploadResult> photoResults,
        List<SignatureUploadResult> signatureResults
    ) {
        public record ChangeResult(
            UUID localId,
            UUID serverId,
            Boolean success,
            String message
        ) {}
        
        public record PhotoUploadResult(
            String localPath,
            String serverUrl,
            Boolean success,
            String message
        ) {}
        
        public record SignatureUploadResult(
            UUID entityId,
            Boolean success,
            String message
        ) {}
    }
    
    /**
     * Instructions for client actions
     */
    public record ClientInstructions(
        List<ClientAction> requiredActions,
        List<String> clearCacheFor,
        List<String> reloadEntities,
        Boolean forceLogout,
        String updateMessage,
        String minimumAppVersion
    ) {
        public record ClientAction(
            String action, // UPDATE_APP, CLEAR_CACHE, RE_AUTHENTICATE, etc.
            String description,
            Map<String, Object> parameters,
            Boolean isRequired,
            Integer priority
        ) {}
    }
    
    /**
     * Sync performance metrics
     */
    public record SyncMetrics(
        Long syncDurationMs,
        Integer recordsReceived,
        Integer recordsSent,
        Integer conflictsResolved,
        Long dataTransferredBytes,
        Double compressionRatio
    ) {}
    
    /**
     * Factory method for successful sync
     */
    public static SyncResponse success(
        ServerData serverData,
        ChangeAcknowledgments acknowledgments,
        LocalDateTime syncTime
    ) {
        return new SyncResponse(
            LocalDateTime.now(),
            syncTime,
            UUID.randomUUID().toString(),
            SyncStatus.SUCCESS,
            List.of(),
            List.of(),
            serverData,
            acknowledgments,
            null,
            null
        );
    }
    
    /**
     * Factory method for sync with conflicts
     */
    public static SyncResponse withConflicts(
        List<SyncConflict> conflicts,
        ServerData serverData,
        LocalDateTime syncTime
    ) {
        return new SyncResponse(
            LocalDateTime.now(),
            syncTime,
            UUID.randomUUID().toString(),
            SyncStatus.CONFLICT,
            List.of(),
            conflicts,
            serverData,
            null,
            new ClientInstructions(
                List.of(new ClientInstructions.ClientAction(
                    "RESOLVE_CONFLICTS",
                    "Manual conflict resolution required",
                    Map.of("conflictCount", conflicts.size()),
                    true,
                    1
                )),
                null, null, false, null, null
            ),
            null
        );
    }
    
    /**
     * Check if sync was successful
     */
    public boolean isSuccessful() {
        return status == SyncStatus.SUCCESS || status == SyncStatus.PARTIAL_SUCCESS;
    }
    
    /**
     * Check if full sync is required
     */
    public boolean requiresFullSync() {
        return status == SyncStatus.REQUIRES_FULL_SYNC;
    }
    
    /**
     * Check if there are conflicts to resolve
     */
    public boolean hasConflicts() {
        return conflicts != null && !conflicts.isEmpty();
    }
}