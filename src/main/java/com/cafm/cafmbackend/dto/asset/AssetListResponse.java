package com.cafm.cafmbackend.dto.asset;

import com.cafm.cafmbackend.shared.enums.AssetCondition;
import com.cafm.cafmbackend.shared.enums.AssetStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Asset list response DTO for paginated results.
 * 
 * Purpose: Lightweight asset representation for list views and tables
 * Pattern: Simplified record with essential fields only
 * Java 23: Record optimized for memory efficiency in large datasets
 * Architecture: Multi-tenant aware asset listing with key indicators
 */
@Schema(description = "Asset list item response")
public record AssetListResponse(
    @Schema(description = "Asset ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID id,
    
    @Schema(description = "Asset code", example = "LAPTOP-001")
    String assetCode,
    
    @Schema(description = "Asset name", example = "Dell Latitude 5520")
    String name,
    
    @Schema(description = "Asset name in Arabic", example = "لابتوب ديل")
    String nameAr,
    
    @Schema(description = "Manufacturer", example = "Dell")
    String manufacturer,
    
    @Schema(description = "Model", example = "Latitude 5520")
    String model,
    
    @Schema(description = "Serial number", example = "DL5520001")
    String serialNumber,
    
    @Schema(description = "Category name", example = "Laptops")
    String categoryName,
    
    @Schema(description = "School name", example = "Al-Noor Primary School")
    String schoolName,
    
    @Schema(description = "Department", example = "IT Department")
    String department,
    
    @Schema(description = "Location", example = "Room 201")
    String location,
    
    @Schema(description = "Assigned user name", example = "John Doe")
    String assignedToName,
    
    @Schema(description = "Purchase date")
    LocalDate purchaseDate,
    
    @Schema(description = "Purchase cost", example = "1500.00")
    BigDecimal purchaseCost,
    
    @Schema(description = "Current value", example = "1200.00")
    BigDecimal currentValue,
    
    @Schema(description = "Asset status")
    AssetStatus status,
    
    @Schema(description = "Asset condition")
    AssetCondition condition,
    
    @Schema(description = "Is asset active", example = "true")
    Boolean isActive,
    
    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt,
    
    // Key indicators for quick assessment
    @Schema(description = "Is under warranty", example = "true")
    Boolean isUnderWarranty,
    
    @Schema(description = "Is maintenance due", example = "false")
    Boolean isMaintenanceDue,
    
    @Schema(description = "Days until next maintenance", example = "15")
    Long daysUntilMaintenance,
    
    @Schema(description = "Age in years", example = "2.5")
    Double ageInYears,
    
    @Schema(description = "Depreciation rate percentage", example = "20.0")
    BigDecimal depreciationRate
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
     * Get formatted location string.
     */
    public String getFormattedLocation() {
        StringBuilder sb = new StringBuilder();
        if (schoolName != null) {
            sb.append(schoolName).append(" - ");
        }
        if (department != null && !department.trim().isEmpty()) {
            sb.append(department).append(" - ");
        }
        if (location != null && !location.trim().isEmpty()) {
            sb.append(location);
        }
        return sb.length() > 0 ? sb.toString() : "Unassigned";
    }
    
    /**
     * Get short location for compact display.
     */
    public String getShortLocation() {
        String fullLocation = getFormattedLocation();
        if (fullLocation.length() <= 40) {
            return fullLocation;
        }
        return fullLocation.substring(0, 37) + "...";
    }
    
    /**
     * Get status color for UI display.
     */
    public String getStatusColor() {
        if (status == null) {
            return "gray";
        }
        
        return switch (status) {
            case ACTIVE -> "green";
            case IN_USE -> "green";
            case RESERVED -> "blue";
            case MAINTENANCE -> "orange";
            case MAINTENANCE_REQUIRED -> "orange";
            case RETIRED -> "gray";
            case DISPOSED -> "red";
            case LOST -> "red";
            case DAMAGED -> "yellow";
        };
    }
    
    /**
     * Get condition color for UI display.
     */
    public String getConditionColor() {
        if (condition == null) {
            return "gray";
        }
        
        return switch (condition) {
            case EXCELLENT -> "green";
            case GOOD -> "blue";
            case FAIR -> "yellow";
            case POOR -> "orange";
            case BROKEN -> "red";
            case UNUSABLE -> "red";
        };
    }
    
    /**
     * Get maintenance status indicator.
     */
    public String getMaintenanceStatus() {
        if (Boolean.TRUE.equals(isMaintenanceDue)) {
            return "OVERDUE";
        } else if (daysUntilMaintenance != null && daysUntilMaintenance <= 7) {
            return "DUE_SOON";
        } else {
            return "CURRENT";
        }
    }
    
    /**
     * Get maintenance status color.
     */
    public String getMaintenanceStatusColor() {
        String maintenanceStatus = getMaintenanceStatus();
        return switch (maintenanceStatus) {
            case "OVERDUE" -> "red";
            case "DUE_SOON" -> "orange";
            case "CURRENT" -> "green";
            default -> "gray";
        };
    }
    
    /**
     * Get warranty status indicator.
     */
    public String getWarrantyStatus() {
        return Boolean.TRUE.equals(isUnderWarranty) ? "ACTIVE" : "EXPIRED";
    }
    
    /**
     * Get warranty status color.
     */
    public String getWarrantyStatusColor() {
        return Boolean.TRUE.equals(isUnderWarranty) ? "green" : "red";
    }
    
    /**
     * Check if asset needs immediate attention.
     */
    public boolean needsAttention() {
        return Boolean.TRUE.equals(isMaintenanceDue) ||
               (condition != null && (condition == AssetCondition.POOR || condition == AssetCondition.UNUSABLE)) ||
               status == AssetStatus.MAINTENANCE ||
               Boolean.FALSE.equals(isActive);
    }
    
    /**
     * Get priority level for attention.
     */
    public String getAttentionPriority() {
        if (!Boolean.TRUE.equals(isActive)) {
            return "INACTIVE";
        }
        
        if (condition == AssetCondition.UNUSABLE || status == AssetStatus.MAINTENANCE) {
            return "UNUSABLE";
        }
        
        if (Boolean.TRUE.equals(isMaintenanceDue) || condition == AssetCondition.POOR) {
            return "HIGH";
        }
        
        if (daysUntilMaintenance != null && daysUntilMaintenance <= 7) {
            return "MEDIUM";
        }
        
        return "LOW";
    }
    
    /**
     * Get financial status indicator.
     */
    public String getFinancialStatus() {
        if (purchaseCost == null || currentValue == null) {
            return "UNKNOWN";
        }
        
        if (depreciationRate != null) {
            if (depreciationRate.compareTo(BigDecimal.valueOf(75)) > 0) {
                return "HEAVILY_DEPRECIATED";
            } else if (depreciationRate.compareTo(BigDecimal.valueOf(50)) > 0) {
                return "MODERATELY_DEPRECIATED";
            } else if (depreciationRate.compareTo(BigDecimal.valueOf(25)) > 0) {
                return "LIGHTLY_DEPRECIATED";
            }
        }
        
        return "CURRENT";
    }
    
    /**
     * Get age category.
     */
    public String getAgeCategory() {
        if (ageInYears == null) {
            return "UNKNOWN";
        }
        
        if (ageInYears < 1) {
            return "NEW";
        } else if (ageInYears < 3) {
            return "RECENT";
        } else if (ageInYears < 5) {
            return "MATURE";
        } else {
            return "OLD";
        }
    }
    
    /**
     * Get assignment status.
     */
    public String getAssignmentStatus() {
        if (assignedToName != null && !assignedToName.trim().isEmpty()) {
            return "ASSIGNED";
        }
        return "UNASSIGNED";
    }
    
    /**
     * Get overall health indicator.
     */
    public String getHealthIndicator() {
        int score = 100;
        
        // Condition impact
        if (condition != null) {
            score -= switch (condition) {
                case EXCELLENT -> 0;
                case GOOD -> 10;
                case FAIR -> 25;
                case POOR -> 40;
                case BROKEN -> 70;
                case UNUSABLE -> 60;
            };
        }
        
        // Maintenance impact
        if (Boolean.TRUE.equals(isMaintenanceDue)) {
            score -= 30;
        } else if (daysUntilMaintenance != null && daysUntilMaintenance <= 7) {
            score -= 15;
        }
        
        // Age impact
        if (ageInYears != null && ageInYears > 5) {
            score -= 10;
        }
        
        if (score >= 80) return "EXCELLENT";
        if (score >= 60) return "GOOD";
        if (score >= 40) return "FAIR";
        if (score >= 20) return "POOR";
        return "CRITICAL";
    }
    
    /**
     * Get type display name based on category.
     */
    public String getTypeDisplayName() {
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            return categoryName;
        }
        return "Uncategorized";
    }
}