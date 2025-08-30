package com.cafm.cafmbackend.dto.mobile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mobile synchronization request DTO.
 * 
 * Purpose: Request structure for mobile app synchronization
 * Pattern: Request/Response pattern for mobile sync operations
 * Java 23: Record-based request with efficient data structures
 * Architecture: Mobile sync layer request contract
 * Standards: Follows mobile-first sync protocol design
 */
public record MobileSyncRequest(
    UUID deviceId,
    String appVersion,
    LocalDateTime lastSyncTime,
    List<String> entityTypes,
    Map<String, LocalDateTime> entityLastSyncTimes,
    int batchSize,
    boolean fullSync,
    MobileDeviceInfo deviceInfo
) {
    
    /**
     * Default batch size for sync operations.
     */
    public static final int DEFAULT_BATCH_SIZE = 50;
    
    /**
     * Maximum batch size to prevent memory issues.
     */
    public static final int MAX_BATCH_SIZE = 200;
    
    /**
     * Get effective batch size with bounds checking.
     */
    public int getEffectiveBatchSize() {
        if (batchSize <= 0) return DEFAULT_BATCH_SIZE;
        return Math.min(batchSize, MAX_BATCH_SIZE);
    }
    
    /**
     * Check if this is a full synchronization request.
     */
    public boolean isFullSync() {
        return fullSync || lastSyncTime == null;
    }
    
    /**
     * Get last sync time for specific entity type.
     */
    public LocalDateTime getLastSyncTime(String entityType) {
        if (entityLastSyncTimes == null) return lastSyncTime;
        return entityLastSyncTimes.getOrDefault(entityType, lastSyncTime);
    }
    
    /**
     * Mobile device information for analytics and optimization.
     */
    public record MobileDeviceInfo(
        String platform, // "iOS" or "Android"
        String osVersion,
        String deviceModel,
        String networkType, // "wifi", "cellular", "offline"
        int batteryLevel,
        long availableStorage
    ) {
        
        /**
         * Check if device is on cellular connection.
         */
        public boolean isCellularConnection() {
            return "cellular".equalsIgnoreCase(networkType);
        }
        
        /**
         * Check if device has low battery.
         */
        public boolean isLowBattery() {
            return batteryLevel < 20;
        }
        
        /**
         * Check if device has limited storage.
         */
        public boolean isLowStorage() {
            return availableStorage < 100_000_000; // Less than 100MB
        }
    }
}