package com.cafm.cafmbackend.dto.offline;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data package for offline mode support in mobile app.
 * Contains essential data for working without network connection.
 */
public record OfflineDataPackage(
    // Package metadata
    String packageId,
    String version,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime generatedAt,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime expiresAt,
    
    // User context
    UUID userId,
    String userRole,
    List<UUID> assignedSchoolIds,
    
    // Core data for offline work
    OfflineData data,
    
    // Configuration
    OfflineConfig config,
    
    // Validation rules
    ValidationRules validationRules,
    
    // Package integrity
    String checksum,
    Long sizeInBytes,
    CompressionInfo compression
) {
    /**
     * Offline data content
     */
    public record OfflineData(
        // Essential entities
        List<School> schools,
        List<User> users,
        List<Report> reports,
        List<WorkOrder> workOrders,
        List<Asset> assets,
        
        // Reference data
        ReferenceData referenceData,
        
        // Templates
        List<ReportTemplate> reportTemplates,
        List<ChecklistTemplate> checklistTemplates,
        
        // Cached files
        List<CachedFile> cachedFiles
    ) {
        public record School(
            UUID id,
            String code,
            String name,
            String district,
            String address,
            Double latitude,
            Double longitude,
            Map<String, String> contacts,
            List<String> buildings,
            Boolean isActive
        ) {}
        
        public record User(
            UUID id,
            String name,
            String role,
            String email,
            String phone,
            String photoUrl,
            Boolean isActive
        ) {}
        
        public record Report(
            UUID id,
            String reportNumber,
            String title,
            String status,
            String priority,
            UUID schoolId,
            UUID createdById,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime createdAt,
            Boolean canEdit,
            Boolean canDelete
        ) {}
        
        public record WorkOrder(
            UUID id,
            String workOrderNumber,
            String title,
            String status,
            String priority,
            UUID assignedToId,
            UUID schoolId,
            List<Task> tasks,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime scheduledStart,
            Boolean canUpdate
        ) {
            public record Task(
                UUID id,
                String title,
                String status,
                Boolean isMandatory
            ) {}
        }
        
        public record Asset(
            UUID id,
            String assetCode,
            String name,
            String type,
            String location,
            UUID schoolId,
            String status,
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDateTime lastMaintenanceDate,
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDateTime nextMaintenanceDate
        ) {}
        
        public record ReferenceData(
            Map<String, List<String>> categories,
            Map<String, List<String>> priorities,
            Map<String, List<String>> statuses,
            Map<String, String> translations,
            List<String> commonIssues,
            List<String> commonLocations,
            Map<String, Object> systemConstants
        ) {}
        
        public record ReportTemplate(
            String templateId,
            String name,
            String category,
            List<Field> fields,
            List<String> requiredPhotos,
            Map<String, Object> defaults
        ) {
            public record Field(
                String name,
                String type,
                String label,
                Boolean required,
                List<String> options,
                Map<String, Object> validation
            ) {}
        }
        
        public record ChecklistTemplate(
            String templateId,
            String name,
            String category,
            List<CheckItem> items,
            Integer estimatedMinutes
        ) {
            public record CheckItem(
                String id,
                String description,
                String type,
                Boolean required,
                List<String> options
            ) {}
        }
        
        public record CachedFile(
            UUID fileId,
            String url,
            String localPath,
            String mimeType,
            Long sizeInBytes,
            String entityType,
            UUID entityId,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime cachedAt
        ) {}
    }
    
    /**
     * Offline configuration
     */
    public record OfflineConfig(
        // Sync settings
        Integer syncIntervalMinutes,
        Integer maxOfflineDays,
        Boolean autoSync,
        Boolean syncOnWiFiOnly,
        
        // Storage limits
        Long maxCacheSizeMB,
        Integer maxPhotosPerReport,
        Integer photoQuality,
        Boolean compressPhotos,
        
        // Behavior settings
        Boolean allowOfflineCreation,
        Boolean allowOfflineEditing,
        Boolean allowOfflineDeletion,
        List<String> offlineEnabledFeatures,
        
        // Queue settings
        Integer maxQueueSize,
        Integer retryAttempts,
        Integer retryDelaySeconds,
        
        // Data retention
        Integer keepDeletedDays,
        Integer keepCompletedDays,
        Boolean purgeOldData
    ) {}
    
    /**
     * Validation rules for offline data entry
     */
    public record ValidationRules(
        Map<String, FieldValidation> reportFields,
        Map<String, FieldValidation> workOrderFields,
        List<BusinessRule> businessRules,
        Map<String, List<String>> requiredFields
    ) {
        public record FieldValidation(
            String fieldName,
            String dataType,
            Boolean required,
            Integer minLength,
            Integer maxLength,
            String pattern,
            List<String> allowedValues,
            Map<String, Object> customRules
        ) {}
        
        public record BusinessRule(
            String ruleId,
            String description,
            String entityType,
            String condition,
            String action,
            String errorMessage
        ) {}
    }
    
    /**
     * Compression information
     */
    public record CompressionInfo(
        String algorithm,
        Double compressionRatio,
        Long originalSize,
        Long compressedSize
    ) {}
    
    /**
     * Factory method for supervisor offline package
     */
    public static OfflineDataPackage forSupervisor(
        UUID supervisorId,
        List<UUID> schoolIds,
        OfflineData data
    ) {
        return new OfflineDataPackage(
            UUID.randomUUID().toString(),
            "1.0",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(7),
            supervisorId,
            "SUPERVISOR",
            schoolIds,
            data,
            new OfflineConfig(
                30, 7, true, false,
                500L, 5, 80, true,
                true, true, false,
                List.of("CREATE_REPORT", "EDIT_DRAFT", "VIEW_WORK_ORDERS"),
                100, 3, 60,
                7, 30, true
            ),
            null,
            null,
            null,
            null
        );
    }
    
    /**
     * Factory method for technician offline package
     */
    public static OfflineDataPackage forTechnician(
        UUID technicianId,
        OfflineData data
    ) {
        return new OfflineDataPackage(
            UUID.randomUUID().toString(),
            "1.0",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(3),
            technicianId,
            "TECHNICIAN",
            null,
            data,
            new OfflineConfig(
                15, 3, true, false,
                300L, 3, 70, true,
                false, true, false,
                List.of("UPDATE_WORK_ORDER", "COMPLETE_TASK", "ADD_NOTES"),
                50, 3, 30,
                3, 14, true
            ),
            null,
            null,
            null,
            null
        );
    }
    
    /**
     * Check if package is expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Calculate days until expiration
     */
    public long getDaysUntilExpiration() {
        if (expiresAt == null) {
            return -1;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expiresAt);
    }
    
    /**
     * Check if feature is enabled offline
     */
    public boolean isFeatureEnabledOffline(String feature) {
        return config != null && 
               config.offlineEnabledFeatures != null && 
               config.offlineEnabledFeatures.contains(feature);
    }
}