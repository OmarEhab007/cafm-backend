package com.cafm.cafmbackend.dto.school;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * School response DTO.
 * 
 * Purpose: Represents complete school information for API responses
 * Pattern: Immutable record with comprehensive school data
 * Java 23: Record with spatial coordinates and maintenance metrics
 * Architecture: Multi-tenant aware school representation
 */
@Schema(description = "School information response")
public record SchoolResponse(
    @Schema(description = "School ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID id,
    
    @Schema(description = "School code", example = "SCH001")
    String code,
    
    @Schema(description = "School name", example = "Al-Noor Primary School")
    String name,
    
    @Schema(description = "School name in Arabic", example = "مدرسة النور الابتدائية")
    String nameAr,
    
    @Schema(description = "School type", example = "PRIMARY")
    String type,
    
    @Schema(description = "Gender type", example = "MIXED")
    String gender,
    
    @Schema(description = "School address")
    String address,
    
    @Schema(description = "City", example = "Riyadh")
    String city,
    
    @Schema(description = "Latitude coordinate", example = "24.7136")
    BigDecimal latitude,
    
    @Schema(description = "Longitude coordinate", example = "46.6753")
    BigDecimal longitude,
    
    @Schema(description = "Is school active", example = "true")
    Boolean isActive,
    
    @Schema(description = "Maintenance score (0-100)", example = "85")
    Integer maintenanceScore,
    
    @Schema(description = "Activity level", example = "HIGH")
    String activityLevel,
    
    @Schema(description = "Company ID", example = "987fcdeb-51a2-4db8-b456-123456789012")
    UUID companyId,
    
    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt,
    
    @Schema(description = "Last update timestamp")
    LocalDateTime updatedAt,
    
    // Calculated fields
    @Schema(description = "Total number of reports", example = "25")
    Long totalReports,
    
    @Schema(description = "Pending reports count", example = "3")
    Long pendingReports,
    
    @Schema(description = "Active work orders count", example = "5")
    Long activeWorkOrders,
    
    @Schema(description = "Total assets count", example = "150")
    Long totalAssets,
    
    @Schema(description = "Assets requiring maintenance", example = "8")
    Long assetsNeedingMaintenance,
    
    @Schema(description = "Number of assigned supervisors", example = "2")
    Long assignedSupervisors
) {
    
    /**
     * Get effective display name (Arabic name if available, otherwise English).
     */
    public String getEffectiveDisplayName() {
        return (nameAr != null && !nameAr.trim().isEmpty()) ? nameAr : name;
    }
    
    /**
     * Check if school has GPS coordinates.
     */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
    
    /**
     * Get maintenance status based on score.
     */
    public String getMaintenanceStatus() {
        if (maintenanceScore == null) {
            return "UNKNOWN";
        }
        
        if (maintenanceScore >= 90) {
            return "EXCELLENT";
        } else if (maintenanceScore >= 75) {
            return "GOOD";
        } else if (maintenanceScore >= 60) {
            return "FAIR";
        } else if (maintenanceScore >= 40) {
            return "POOR";
        } else {
            return "CRITICAL";
        }
    }
    
    /**
     * Check if school needs immediate attention.
     */
    public boolean needsAttention() {
        return (maintenanceScore != null && maintenanceScore < 60) ||
               (pendingReports != null && pendingReports > 10) ||
               (assetsNeedingMaintenance != null && assetsNeedingMaintenance > 20);
    }
    
    /**
     * Get maintenance score color for UI display.
     */
    public String getMaintenanceScoreColor() {
        if (maintenanceScore == null) {
            return "gray";
        }
        
        if (maintenanceScore >= 75) {
            return "green";
        } else if (maintenanceScore >= 50) {
            return "yellow";
        } else {
            return "red";
        }
    }
    
    /**
     * Calculate maintenance workload percentage.
     */
    public int getMaintenanceWorkloadPercentage() {
        if (totalReports == null || totalReports == 0 || pendingReports == null) {
            return 0;
        }
        return (int) Math.round((pendingReports.doubleValue() / totalReports) * 100);
    }
}