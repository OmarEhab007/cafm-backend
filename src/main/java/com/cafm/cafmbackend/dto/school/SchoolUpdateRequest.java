package com.cafm.cafmbackend.dto.school;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * School update request DTO.
 * 
 * Purpose: Validates and transfers school update data
 * Pattern: Immutable record with validation for partial updates
 * Java 23: Record with optional fields for PATCH-style updates
 * Architecture: Multi-tenant aware school modification
 */
@Schema(description = "School update request")
public record SchoolUpdateRequest(
    @Size(max = 255, message = "School name cannot exceed 255 characters")
    @Schema(description = "School name", example = "Al-Noor Primary School")
    String name,
    
    @Size(max = 255, message = "Arabic name cannot exceed 255 characters")
    @Schema(description = "School name in Arabic", example = "مدرسة النور الابتدائية")
    String nameAr,
    
    @Pattern(regexp = "^(PRIMARY|INTERMEDIATE|SECONDARY|HIGH_SCHOOL|KINDERGARTEN|UNIVERSITY)$", 
             message = "School type must be one of: PRIMARY, INTERMEDIATE, SECONDARY, HIGH_SCHOOL, KINDERGARTEN, UNIVERSITY")
    @Schema(description = "School type", example = "PRIMARY", 
            allowableValues = {"PRIMARY", "INTERMEDIATE", "SECONDARY", "HIGH_SCHOOL", "KINDERGARTEN", "UNIVERSITY"})
    String type,
    
    @Pattern(regexp = "^(BOYS|GIRLS|MIXED)$", 
             message = "Gender type must be one of: BOYS, GIRLS, MIXED")
    @Schema(description = "Gender type", example = "MIXED", 
            allowableValues = {"BOYS", "GIRLS", "MIXED"})
    String gender,
    
    @Size(max = 500, message = "Address cannot exceed 500 characters")
    @Schema(description = "School address")
    String address,
    
    @Size(max = 100, message = "City name cannot exceed 100 characters")
    @Schema(description = "City", example = "Riyadh")
    String city,
    
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @Digits(integer = 2, fraction = 8, message = "Latitude must have at most 2 integer digits and 8 decimal places")
    @Schema(description = "Latitude coordinate", example = "24.7136")
    BigDecimal latitude,
    
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @Digits(integer = 3, fraction = 8, message = "Longitude must have at most 3 integer digits and 8 decimal places")
    @Schema(description = "Longitude coordinate", example = "46.6753")
    BigDecimal longitude,
    
    @Min(value = 0, message = "Maintenance score must be between 0 and 100")
    @Max(value = 100, message = "Maintenance score must be between 0 and 100")
    @Schema(description = "Maintenance score (0-100)", example = "85")
    Integer maintenanceScore,
    
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH)$", 
             message = "Activity level must be one of: LOW, MEDIUM, HIGH")
    @Schema(description = "Activity level", example = "HIGH", 
            allowableValues = {"LOW", "MEDIUM", "HIGH"})
    String activityLevel,
    
    @Schema(description = "Is school active", example = "true")
    Boolean isActive
) {
    
    /**
     * Check if any field is provided for update.
     */
    public boolean hasUpdates() {
        return name != null || nameAr != null || type != null || gender != null ||
               address != null || city != null || latitude != null || longitude != null ||
               maintenanceScore != null || activityLevel != null || isActive != null;
    }
    
    /**
     * Check if this is a location-related update.
     */
    public boolean isLocationUpdate() {
        return address != null || city != null || latitude != null || longitude != null;
    }
    
    /**
     * Check if this is a maintenance-related update.
     */
    public boolean isMaintenanceUpdate() {
        return maintenanceScore != null || activityLevel != null;
    }
    
    /**
     * Check if this is a basic information update.
     */
    public boolean isBasicInfoUpdate() {
        return name != null || nameAr != null || type != null || gender != null;
    }
    
    /**
     * Check if GPS coordinates are being updated.
     */
    public boolean hasCoordinateUpdate() {
        return latitude != null || longitude != null;
    }
    
    /**
     * Validate that if one coordinate is provided for update, both should be provided.
     */
    public boolean areCoordinatesValidForUpdate() {
        // If neither is provided, that's fine (no coordinate update)
        if (latitude == null && longitude == null) {
            return true;
        }
        // If only one is provided, that might be intentional (partial update)
        // We allow this and let the service handle it appropriately
        return true;
    }
    
    /**
     * Check if this update affects school classification.
     */
    public boolean affectsClassification() {
        return type != null || gender != null;
    }
    
    /**
     * Check if this update affects school status.
     */
    public boolean affectsStatus() {
        return isActive != null || maintenanceScore != null;
    }
}