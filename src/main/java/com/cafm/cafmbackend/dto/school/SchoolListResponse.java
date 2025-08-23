package com.cafm.cafmbackend.dto.school;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * School list response DTO for paginated results.
 * 
 * Purpose: Lightweight school representation for list views and tables
 * Pattern: Simplified record with essential fields only
 * Java 23: Record optimized for memory efficiency in large datasets
 * Architecture: Multi-tenant aware school listing
 */
@Schema(description = "School list item response")
public record SchoolListResponse(
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
    
    @Schema(description = "City", example = "Riyadh")
    String city,
    
    @Schema(description = "Is school active", example = "true")
    Boolean isActive,
    
    @Schema(description = "Maintenance score (0-100)", example = "85")
    Integer maintenanceScore,
    
    @Schema(description = "Activity level", example = "HIGH")
    String activityLevel,
    
    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt,
    
    // Summary statistics
    @Schema(description = "Total number of reports", example = "25")
    Long totalReports,
    
    @Schema(description = "Pending reports count", example = "3")
    Long pendingReports,
    
    @Schema(description = "Active work orders count", example = "5")
    Long activeWorkOrders,
    
    @Schema(description = "Number of assigned supervisors", example = "2")
    Long assignedSupervisors,
    
    @Schema(description = "Has GPS coordinates", example = "true")
    Boolean hasCoordinates
) {
    
    /**
     * Get effective display name (Arabic name if available, otherwise English).
     */
    public String getEffectiveDisplayName() {
        return (nameAr != null && !nameAr.trim().isEmpty()) ? nameAr : name;
    }
    
    /**
     * Get short display name for compact UI elements.
     */
    public String getShortDisplayName() {
        String displayName = getEffectiveDisplayName();
        if (displayName.length() <= 30) {
            return displayName;
        }
        return displayName.substring(0, 27) + "...";
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
     * Get maintenance status color for UI display.
     */
    public String getMaintenanceStatusColor() {
        String status = getMaintenanceStatus();
        return switch (status) {
            case "EXCELLENT", "GOOD" -> "green";
            case "FAIR" -> "yellow";
            case "POOR", "CRITICAL" -> "red";
            default -> "gray";
        };
    }
    
    /**
     * Check if school needs immediate attention.
     */
    public boolean needsAttention() {
        return (maintenanceScore != null && maintenanceScore < 60) ||
               (pendingReports != null && pendingReports > 10) ||
               Boolean.FALSE.equals(isActive);
    }
    
    /**
     * Get priority level for maintenance.
     */
    public String getMaintenancePriority() {
        if (!Boolean.TRUE.equals(isActive)) {
            return "INACTIVE";
        }
        
        if (pendingReports != null && pendingReports > 15) {
            return "URGENT";
        }
        
        if (maintenanceScore != null) {
            if (maintenanceScore < 40) {
                return "CRITICAL";
            } else if (maintenanceScore < 60) {
                return "HIGH";
            } else if (maintenanceScore < 75) {
                return "MEDIUM";
            }
        }
        
        return "LOW";
    }
    
    /**
     * Get workload indicator for supervisors.
     */
    public String getWorkloadIndicator() {
        if (activeWorkOrders == null) {
            return "UNKNOWN";
        }
        
        if (activeWorkOrders == 0) {
            return "LIGHT";
        } else if (activeWorkOrders <= 5) {
            return "MODERATE";
        } else if (activeWorkOrders <= 10) {
            return "BUSY";
        } else {
            return "OVERLOADED";
        }
    }
    
    /**
     * Check if school has sufficient supervisor coverage.
     */
    public boolean hasSufficientSupervisorCoverage() {
        if (assignedSupervisors == null) {
            return false;
        }
        
        // Business rule: at least 1 supervisor per school, 2+ for large schools
        long requiredSupervisors = (activeWorkOrders != null && activeWorkOrders > 10) ? 2 : 1;
        return assignedSupervisors >= requiredSupervisors;
    }
    
    /**
     * Get type display name for UI.
     */
    public String getTypeDisplayName() {
        if (type == null) {
            return "Unknown";
        }
        
        return switch (type.toUpperCase()) {
            case "PRIMARY" -> "Primary School";
            case "INTERMEDIATE" -> "Intermediate School";
            case "SECONDARY" -> "Secondary School";
            case "HIGH_SCHOOL" -> "High School";
            case "KINDERGARTEN" -> "Kindergarten";
            case "UNIVERSITY" -> "University";
            default -> type;
        };
    }
    
    /**
     * Get gender display name for UI.
     */
    public String getGenderDisplayName() {
        if (gender == null) {
            return "Unknown";
        }
        
        return switch (gender.toUpperCase()) {
            case "BOYS" -> "Boys Only";
            case "GIRLS" -> "Girls Only";
            case "MIXED" -> "Mixed";
            default -> gender;
        };
    }
}