package com.cafm.cafmbackend.dto.mobile;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Mobile report submission request DTO optimized for mobile use.
 * 
 * Purpose: Handles report submission from mobile devices with offline capabilities
 * Pattern: Immutable record with comprehensive validation
 * Java 23: Uses records for DTOs with automatic validation
 * Architecture: API layer DTO optimized for mobile data transmission
 * Standards: Bean validation, JSON property mapping, size constraints
 */
public record MobileReportRequest(
    @JsonProperty("client_id")
    String clientId,
    
    @JsonProperty("school_id")
    @NotBlank(message = "School ID is required")
    String schoolId,
    
    @JsonProperty("title")
    @NotBlank(message = "Report title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    String title,
    
    @JsonProperty("description")
    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    String description,
    
    @JsonProperty("category")
    @NotBlank(message = "Category is required")
    String category,
    
    @JsonProperty("priority")
    @NotBlank(message = "Priority is required")
    String priority,
    
    @JsonProperty("location")
    LocationData location,
    
    @JsonProperty("photos")
    List<PhotoData> photos,
    
    @JsonProperty("maintenance_details")
    MaintenanceDetails maintenanceDetails,
    
    @JsonProperty("reported_at")
    LocalDateTime reportedAt,
    
    @JsonProperty("offline_created")
    Boolean offlineCreated,
    
    @JsonProperty("device_info")
    DeviceInfo deviceInfo,
    
    @JsonProperty("metadata")
    Map<String, Object> metadata
) {
    
    /**
     * Location information for the report.
     */
    public record LocationData(
        @JsonProperty("latitude")
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        BigDecimal latitude,
        
        @JsonProperty("longitude")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        BigDecimal longitude,
        
        @JsonProperty("address")
        String address,
        
        @JsonProperty("building")
        String building,
        
        @JsonProperty("floor")
        String floor,
        
        @JsonProperty("room")
        String room,
        
        @JsonProperty("additional_info")
        String additionalInfo
    ) {}
    
    /**
     * Photo data including Base64 encoded image and metadata.
     */
    public record PhotoData(
        @JsonProperty("id")
        String id,
        
        @JsonProperty("filename")
        @NotBlank(message = "Photo filename is required")
        String filename,
        
        @JsonProperty("base64_data")
        @NotBlank(message = "Photo data is required")
        String base64Data,
        
        @JsonProperty("content_type")
        @NotBlank(message = "Content type is required")
        String contentType,
        
        @JsonProperty("size_bytes")
        @Positive(message = "File size must be positive")
        Long sizeBytes,
        
        @JsonProperty("width")
        Integer width,
        
        @JsonProperty("height")
        Integer height,
        
        @JsonProperty("taken_at")
        LocalDateTime takenAt,
        
        @JsonProperty("compression_quality")
        @Min(value = 1, message = "Compression quality must be >= 1")
        @Max(value = 100, message = "Compression quality must be <= 100")
        Integer compressionQuality,
        
        @JsonProperty("caption")
        String caption
    ) {}
    
    /**
     * Maintenance-specific details.
     */
    public record MaintenanceDetails(
        @JsonProperty("asset_id")
        String assetId,
        
        @JsonProperty("asset_tag")
        String assetTag,
        
        @JsonProperty("damage_type")
        String damageType,
        
        @JsonProperty("severity")
        @NotBlank(message = "Severity is required")
        String severity,
        
        @JsonProperty("estimated_cost")
        @DecimalMin(value = "0.0", message = "Cost must be non-negative")
        BigDecimal estimatedCost,
        
        @JsonProperty("urgent")
        Boolean urgent,
        
        @JsonProperty("safety_hazard")
        Boolean safetyHazard,
        
        @JsonProperty("affects_operations")
        Boolean affectsOperations,
        
        @JsonProperty("temporary_fix_applied")
        Boolean temporaryFixApplied,
        
        @JsonProperty("temporary_fix_description")
        String temporaryFixDescription,
        
        @JsonProperty("required_skills")
        List<String> requiredSkills,
        
        @JsonProperty("required_materials")
        List<MaterialItem> requiredMaterials
    ) {}
    
    /**
     * Material item required for maintenance.
     */
    public record MaterialItem(
        @JsonProperty("item_id")
        String itemId,
        
        @JsonProperty("name")
        @NotBlank(message = "Material name is required")
        String name,
        
        @JsonProperty("quantity")
        @Positive(message = "Quantity must be positive")
        Integer quantity,
        
        @JsonProperty("unit")
        String unit,
        
        @JsonProperty("estimated_cost")
        @DecimalMin(value = "0.0", message = "Cost must be non-negative")
        BigDecimal estimatedCost
    ) {}
    
    /**
     * Device information for the submission.
     */
    public record DeviceInfo(
        @JsonProperty("device_id")
        String deviceId,
        
        @JsonProperty("platform")
        String platform,
        
        @JsonProperty("app_version")
        String appVersion,
        
        @JsonProperty("os_version")
        String osVersion,
        
        @JsonProperty("network_type")
        String networkType,
        
        @JsonProperty("battery_level")
        @Min(value = 0, message = "Battery level must be >= 0")
        @Max(value = 100, message = "Battery level must be <= 100")
        Integer batteryLevel
    ) {}
}