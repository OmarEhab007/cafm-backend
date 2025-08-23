package com.cafm.cafmbackend.dto.school;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * School creation request DTO.
 * 
 * Purpose: Validates and transfers school creation data
 * Pattern: Immutable record with comprehensive validation
 * Java 23: Record with Bean Validation and spatial coordinates
 * Architecture: Multi-tenant aware school creation
 */
@Schema(description = "School creation request")
public record SchoolCreateRequest(
    @NotBlank(message = "School code is required")
    @Size(max = 50, message = "School code cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "School code can only contain uppercase letters, numbers, underscores, and hyphens")
    @Schema(description = "Unique school code", example = "SCH001", required = true)
    String code,
    
    @NotBlank(message = "School name is required")
    @Size(max = 255, message = "School name cannot exceed 255 characters")
    @Schema(description = "School name", example = "Al-Noor Primary School", required = true)
    String name,
    
    @Size(max = 255, message = "Arabic name cannot exceed 255 characters")
    @Schema(description = "School name in Arabic", example = "مدرسة النور الابتدائية")
    String nameAr,
    
    @NotBlank(message = "School type is required")
    @Pattern(regexp = "^(PRIMARY|INTERMEDIATE|SECONDARY|HIGH_SCHOOL|KINDERGARTEN|UNIVERSITY)$", 
             message = "School type must be one of: PRIMARY, INTERMEDIATE, SECONDARY, HIGH_SCHOOL, KINDERGARTEN, UNIVERSITY")
    @Schema(description = "School type", example = "PRIMARY", required = true, 
            allowableValues = {"PRIMARY", "INTERMEDIATE", "SECONDARY", "HIGH_SCHOOL", "KINDERGARTEN", "UNIVERSITY"})
    String type,
    
    @NotBlank(message = "Gender type is required")
    @Pattern(regexp = "^(BOYS|GIRLS|MIXED)$", 
             message = "Gender type must be one of: BOYS, GIRLS, MIXED")
    @Schema(description = "Gender type", example = "MIXED", required = true, 
            allowableValues = {"BOYS", "GIRLS", "MIXED"})
    String gender,
    
    @Size(max = 500, message = "Address cannot exceed 500 characters")
    @Schema(description = "School address")
    String address,
    
    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City name cannot exceed 100 characters")
    @Schema(description = "City", example = "Riyadh", required = true)
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
    @Schema(description = "Initial maintenance score (0-100)", example = "85")
    Integer maintenanceScore,
    
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH)$", 
             message = "Activity level must be one of: LOW, MEDIUM, HIGH")
    @Schema(description = "Activity level", example = "HIGH", 
            allowableValues = {"LOW", "MEDIUM", "HIGH"})
    String activityLevel
) {
    
    /**
     * Get effective activity level with default.
     */
    public String getEffectiveActivityLevel() {
        return activityLevel != null && !activityLevel.trim().isEmpty() 
            ? activityLevel 
            : "MEDIUM";
    }
    
    /**
     * Get effective maintenance score with default.
     */
    public Integer getEffectiveMaintenanceScore() {
        return maintenanceScore != null ? maintenanceScore : 75;
    }
    
    /**
     * Check if GPS coordinates are provided.
     */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
    
    /**
     * Validate that if one coordinate is provided, both must be provided.
     */
    public boolean areCoordinatesValid() {
        return (latitude == null && longitude == null) || 
               (latitude != null && longitude != null);
    }
    
    /**
     * Get display name for the school (Arabic if available, otherwise English).
     */
    public String getDisplayName() {
        return (nameAr != null && !nameAr.trim().isEmpty()) ? nameAr : name;
    }
    
    /**
     * Generate suggested subdomain from school name.
     */
    public String generateSuggestedSubdomain() {
        return name.toLowerCase()
                  .replaceAll("[^a-z0-9\\s]", "")
                  .replaceAll("\\s+", "-")
                  .replaceAll("-{2,}", "-")
                  .replaceAll("^-|-$", "");
    }
}