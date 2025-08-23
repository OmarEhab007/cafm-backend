package com.cafm.cafmbackend.dto.sync;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request for syncing data between mobile app and server.
 * Supports incremental sync based on last sync timestamp.
 */
public record SyncRequest(
    @NotNull(message = "Device ID is required")
    String deviceId,
    
    @NotNull(message = "User ID is required")
    UUID userId,
    
    @PastOrPresent(message = "Last sync time cannot be in the future")
    LocalDateTime lastSyncTime,
    
    // Entity types to sync
    List<String> entitiesToSync,
    
    // Local changes to push
    LocalChanges localChanges,
    
    // Device and app info
    DeviceInfo deviceInfo,
    
    // Sync preferences
    SyncPreferences preferences
) {
    /**
     * Local changes from mobile app
     */
    public record LocalChanges(
        List<ReportChange> reportChanges,
        List<WorkOrderUpdate> workOrderUpdates,
        List<TaskUpdate> taskUpdates,
        List<PhotoUpload> pendingPhotos,
        List<SignatureUpload> pendingSignatures
    ) {
        public record ReportChange(
            UUID localId,
            UUID serverId,
            String changeType, // CREATE, UPDATE, DELETE
            String reportData,
            LocalDateTime changeTimestamp
        ) {}
        
        public record WorkOrderUpdate(
            UUID workOrderId,
            String updateType,
            String updateData,
            LocalDateTime updateTimestamp
        ) {}
        
        public record TaskUpdate(
            UUID taskId,
            String status,
            String notes,
            LocalDateTime updateTimestamp
        ) {}
        
        public record PhotoUpload(
            String localPath,
            UUID relatedEntityId,
            String entityType,
            String mimeType,
            Long sizeInBytes,
            String base64Data
        ) {}
        
        public record SignatureUpload(
            UUID entityId,
            String entityType,
            String signatureType,
            String base64Signature
        ) {}
    }
    
    /**
     * Device information for tracking
     */
    public record DeviceInfo(
        String platform, // iOS, Android
        String osVersion,
        String appVersion,
        String deviceModel,
        String deviceName,
        Boolean isTablet,
        Integer screenWidth,
        Integer screenHeight,
        String networkType, // WiFi, 4G, 5G
        Integer batteryLevel,
        Long availableStorage,
        String timezone,
        String locale
    ) {}
    
    /**
     * Sync preferences
     */
    public record SyncPreferences(
        Boolean includePhotos,
        Boolean compressPhotos,
        Integer photoQuality, // 1-100
        Boolean includeAttachments,
        Integer maxRecordsPerEntity,
        List<String> excludedFields,
        Boolean deltaOnly
    ) {}
    
    /**
     * Factory method for initial sync
     */
    public static SyncRequest initialSync(String deviceId, UUID userId, DeviceInfo deviceInfo) {
        return new SyncRequest(
            deviceId,
            userId,
            null,
            List.of("schools", "reports", "work_orders", "reference_data"),
            null,
            deviceInfo,
            new SyncPreferences(true, true, 80, true, 100, null, false)
        );
    }
    
    /**
     * Factory method for incremental sync
     */
    public static SyncRequest incrementalSync(
        String deviceId, 
        UUID userId, 
        LocalDateTime lastSync,
        LocalChanges changes
    ) {
        return new SyncRequest(
            deviceId,
            userId,
            lastSync,
            null,
            changes,
            null,
            new SyncPreferences(false, true, 60, false, 50, null, true)
        );
    }
    
    /**
     * Check if this is an initial sync
     */
    public boolean isInitialSync() {
        return lastSyncTime == null;
    }
    
    /**
     * Check if there are local changes to push
     */
    public boolean hasLocalChanges() {
        return localChanges != null && (
            (localChanges.reportChanges() != null && !localChanges.reportChanges().isEmpty()) ||
            (localChanges.workOrderUpdates() != null && !localChanges.workOrderUpdates().isEmpty()) ||
            (localChanges.taskUpdates() != null && !localChanges.taskUpdates().isEmpty()) ||
            (localChanges.pendingPhotos() != null && !localChanges.pendingPhotos().isEmpty()) ||
            (localChanges.pendingSignatures() != null && !localChanges.pendingSignatures().isEmpty())
        );
    }
}