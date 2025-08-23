package com.cafm.cafmbackend.dto.asset;

import com.cafm.cafmbackend.data.enums.AssetCondition;
import com.cafm.cafmbackend.data.enums.AssetStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Asset response DTO.
 * 
 * Purpose: Represents complete asset information for API responses
 * Pattern: Immutable record with comprehensive asset data including financial and maintenance info
 * Java 23: Record with calculated fields for lifecycle management
 * Architecture: Multi-tenant aware asset representation with relationships
 */
@Schema(description = "Asset information response")
public record AssetResponse(
    @Schema(description = "Asset ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID id,
    
    @Schema(description = "Asset code", example = "LAPTOP-001")
    String assetCode,
    
    @Schema(description = "Asset name", example = "Dell Latitude 5520")
    String name,
    
    @Schema(description = "Asset name in Arabic", example = "لابتوب ديل")
    String nameAr,
    
    @Schema(description = "Asset description")
    String description,
    
    @Schema(description = "Manufacturer", example = "Dell")
    String manufacturer,
    
    @Schema(description = "Model", example = "Latitude 5520")
    String model,
    
    @Schema(description = "Serial number", example = "DL5520001")
    String serialNumber,
    
    @Schema(description = "Barcode", example = "123456789012")
    String barcode,
    
    // Purchase Information
    @Schema(description = "Purchase date")
    LocalDate purchaseDate,
    
    @Schema(description = "Purchase order number", example = "PO-2024-001")
    String purchaseOrderNumber,
    
    @Schema(description = "Supplier", example = "Dell Technologies")
    String supplier,
    
    @Schema(description = "Warranty start date")
    LocalDate warrantyStartDate,
    
    @Schema(description = "Warranty end date")
    LocalDate warrantyEndDate,
    
    // Financial Information
    @Schema(description = "Purchase cost", example = "1500.00")
    BigDecimal purchaseCost,
    
    @Schema(description = "Current value", example = "1200.00")
    BigDecimal currentValue,
    
    @Schema(description = "Salvage value", example = "100.00")
    BigDecimal salvageValue,
    
    @Schema(description = "Depreciation method", example = "straight_line")
    String depreciationMethod,
    
    // Location & Assignment
    @Schema(description = "School ID")
    UUID schoolId,
    
    @Schema(description = "School name", example = "Al-Noor Primary School")
    String schoolName,
    
    @Schema(description = "Department", example = "IT Department")
    String department,
    
    @Schema(description = "Location", example = "Room 201")
    String location,
    
    @Schema(description = "Assigned user ID")
    UUID assignedToId,
    
    @Schema(description = "Assigned user name", example = "John Doe")
    String assignedToName,
    
    @Schema(description = "Assignment date")
    LocalDate assignmentDate,
    
    // Maintenance Information
    @Schema(description = "Last maintenance date")
    LocalDate lastMaintenanceDate,
    
    @Schema(description = "Next maintenance date")
    LocalDate nextMaintenanceDate,
    
    @Schema(description = "Maintenance frequency in days", example = "90")
    Integer maintenanceFrequencyDays,
    
    @Schema(description = "Total maintenance cost", example = "350.00")
    BigDecimal totalMaintenanceCost,
    
    // Status & Condition
    @Schema(description = "Asset status")
    AssetStatus status,
    
    @Schema(description = "Asset condition")
    AssetCondition condition,
    
    @Schema(description = "Is asset active", example = "true")
    Boolean isActive,
    
    // Category Information
    @Schema(description = "Category ID")
    UUID categoryId,
    
    @Schema(description = "Category name", example = "Laptops")
    String categoryName,
    
    // Disposal Information
    @Schema(description = "Disposal date")
    LocalDate disposalDate,
    
    @Schema(description = "Disposal method", example = "recycling")
    String disposalMethod,
    
    @Schema(description = "Disposal value", example = "50.00")
    BigDecimal disposalValue,
    
    @Schema(description = "Disposal reason")
    String disposalReason,
    
    // Metadata
    @Schema(description = "Company ID", example = "987fcdeb-51a2-4db8-b456-123456789012")
    UUID companyId,
    
    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt,
    
    @Schema(description = "Last update timestamp")
    LocalDateTime updatedAt,
    
    // Calculated fields
    @Schema(description = "Age in years", example = "2.5")
    Double ageInYears,
    
    @Schema(description = "Is under warranty", example = "true")
    Boolean isUnderWarranty,
    
    @Schema(description = "Warranty days remaining", example = "120")
    Long warrantyDaysRemaining,
    
    @Schema(description = "Is maintenance due", example = "false")
    Boolean isMaintenanceDue,
    
    @Schema(description = "Days until next maintenance", example = "15")
    Long daysUntilMaintenance,
    
    @Schema(description = "Total depreciation", example = "300.00")
    BigDecimal totalDepreciation,
    
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
     * Get maintenance status indicator.
     */
    public String getMaintenanceStatus() {
        if (Boolean.TRUE.equals(isMaintenanceDue)) {
            return "OVERDUE";
        } else if (daysUntilMaintenance != null && daysUntilMaintenance <= 7) {
            return "DUE_SOON";
        } else if (lastMaintenanceDate == null) {
            return "NO_HISTORY";
        } else {
            return "CURRENT";
        }
    }
    
    /**
     * Get warranty status indicator.
     */
    public String getWarrantyStatus() {
        if (warrantyEndDate == null) {
            return "NO_WARRANTY";
        } else if (Boolean.FALSE.equals(isUnderWarranty)) {
            return "EXPIRED";
        } else if (warrantyDaysRemaining != null && warrantyDaysRemaining <= 30) {
            return "EXPIRING_SOON";
        } else {
            return "ACTIVE";
        }
    }
    
    /**
     * Get financial health indicator.
     */
    public String getFinancialHealth() {
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
            } else {
                return "LIKE_NEW";
            }
        }
        
        return "UNKNOWN";
    }
    
    /**
     * Get overall asset health score (0-100).
     */
    public Integer getHealthScore() {
        int score = 100;
        
        // Condition impact (40% weight)
        if (condition != null) {
            score -= switch (condition) {
                case EXCELLENT -> 0;
                case GOOD -> 10;
                case FAIR -> 25;
                case POOR -> 40;
                case UNUSABLE -> 60;
            };
        }
        
        // Maintenance impact (30% weight)
        if (Boolean.TRUE.equals(isMaintenanceDue)) {
            score -= 30;
        } else if (daysUntilMaintenance != null && daysUntilMaintenance <= 7) {
            score -= 15;
        }
        
        // Age impact (20% weight)
        if (ageInYears != null) {
            if (ageInYears > 5) score -= 20;
            else if (ageInYears > 3) score -= 10;
            else if (ageInYears > 1) score -= 5;
        }
        
        // Warranty impact (10% weight)
        if (Boolean.FALSE.equals(isUnderWarranty)) {
            score -= 10;
        }
        
        return Math.max(0, score);
    }
    
    /**
     * Check if asset needs immediate attention.
     */
    public boolean needsAttention() {
        return Boolean.TRUE.equals(isMaintenanceDue) ||
               (condition != null && (condition == AssetCondition.POOR || condition == AssetCondition.UNUSABLE)) ||
               status == AssetStatus.MAINTENANCE ||
               (warrantyDaysRemaining != null && warrantyDaysRemaining <= 7);
    }
    
    /**
     * Get total cost of ownership.
     */
    public BigDecimal getTotalCostOfOwnership() {
        BigDecimal tco = purchaseCost != null ? purchaseCost : BigDecimal.ZERO;
        if (totalMaintenanceCost != null) {
            tco = tco.add(totalMaintenanceCost);
        }
        return tco;
    }
}