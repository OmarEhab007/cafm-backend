package com.cafm.cafmbackend.dto.asset;

import com.cafm.cafmbackend.shared.enums.AssetCondition;
import com.cafm.cafmbackend.shared.enums.AssetStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Asset creation request DTO.
 * 
 * Purpose: Validates and transfers asset creation data
 * Pattern: Immutable record with comprehensive validation for asset lifecycle
 * Java 23: Record with Bean Validation for financial and maintenance data
 * Architecture: Multi-tenant aware asset creation with location assignment
 */
@Schema(description = "Asset creation request")
public record AssetCreateRequest(
    @NotBlank(message = "Asset code is required")
    @Size(max = 50, message = "Asset code cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Asset code can only contain uppercase letters, numbers, underscores, and hyphens")
    @Schema(description = "Unique asset code", example = "LAPTOP-001", required = true)
    String assetCode,
    
    @NotBlank(message = "Asset name is required")
    @Size(max = 255, message = "Asset name cannot exceed 255 characters")
    @Schema(description = "Asset name", example = "Dell Latitude 5520", required = true)
    String name,
    
    @Size(max = 255, message = "Arabic name cannot exceed 255 characters")
    @Schema(description = "Asset name in Arabic", example = "لابتوب ديل")
    String nameAr,
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Schema(description = "Asset description")
    String description,
    
    @Schema(description = "Asset category ID")
    UUID categoryId,
    
    // Asset Details
    @Size(max = 100, message = "Manufacturer cannot exceed 100 characters")
    @Schema(description = "Manufacturer", example = "Dell")
    String manufacturer,
    
    @Size(max = 100, message = "Model cannot exceed 100 characters")
    @Schema(description = "Model", example = "Latitude 5520")
    String model,
    
    @Size(max = 100, message = "Serial number cannot exceed 100 characters")
    @Schema(description = "Serial number", example = "DL5520001")
    String serialNumber,
    
    @Size(max = 100, message = "Barcode cannot exceed 100 characters")
    @Schema(description = "Barcode", example = "123456789012")
    String barcode,
    
    // Purchase Information
    @Schema(description = "Purchase date")
    LocalDate purchaseDate,
    
    @Size(max = 50, message = "Purchase order number cannot exceed 50 characters")
    @Schema(description = "Purchase order number", example = "PO-2024-001")
    String purchaseOrderNumber,
    
    @Size(max = 255, message = "Supplier cannot exceed 255 characters")
    @Schema(description = "Supplier", example = "Dell Technologies")
    String supplier,
    
    @Schema(description = "Warranty start date")
    LocalDate warrantyStartDate,
    
    @Schema(description = "Warranty end date")
    LocalDate warrantyEndDate,
    
    // Financial Information
    @DecimalMin(value = "0.0", message = "Purchase cost cannot be negative")
    @DecimalMax(value = "999999.99", message = "Purchase cost cannot exceed 999,999.99")
    @Digits(integer = 6, fraction = 2, message = "Purchase cost must have at most 6 integer digits and 2 decimal places")
    @Schema(description = "Purchase cost", example = "1500.00")
    BigDecimal purchaseCost,
    
    @DecimalMin(value = "0.0", message = "Current value cannot be negative")
    @DecimalMax(value = "999999.99", message = "Current value cannot exceed 999,999.99")
    @Digits(integer = 6, fraction = 2, message = "Current value must have at most 6 integer digits and 2 decimal places")
    @Schema(description = "Current value", example = "1500.00")
    BigDecimal currentValue,
    
    @DecimalMin(value = "0.0", message = "Salvage value cannot be negative")
    @DecimalMax(value = "999999.99", message = "Salvage value cannot exceed 999,999.99")
    @Digits(integer = 6, fraction = 2, message = "Salvage value must have at most 6 integer digits and 2 decimal places")
    @Schema(description = "Salvage value", example = "100.00")
    BigDecimal salvageValue,
    
    @Pattern(regexp = "^(straight_line|declining_balance|sum_of_years|double_declining)$", 
             message = "Depreciation method must be one of: straight_line, declining_balance, sum_of_years, double_declining")
    @Schema(description = "Depreciation method", example = "straight_line", 
            allowableValues = {"straight_line", "declining_balance", "sum_of_years", "double_declining"})
    String depreciationMethod,
    
    // Location & Assignment
    @Schema(description = "School ID for asset location")
    UUID schoolId,
    
    @Size(max = 100, message = "Department cannot exceed 100 characters")
    @Schema(description = "Department", example = "IT Department")
    String department,
    
    @Size(max = 255, message = "Location cannot exceed 255 characters")
    @Schema(description = "Location", example = "Room 201")
    String location,
    
    @Schema(description = "User ID to assign asset to")
    UUID assignedToId,
    
    // Maintenance Information
    @Min(value = 1, message = "Maintenance frequency must be at least 1 day")
    @Max(value = 3650, message = "Maintenance frequency cannot exceed 3650 days (10 years)")
    @Schema(description = "Maintenance frequency in days", example = "90")
    Integer maintenanceFrequencyDays,
    
    @Schema(description = "Next maintenance date")
    LocalDate nextMaintenanceDate,
    
    // Status & Condition
    @Schema(description = "Asset status", example = "ACTIVE")
    AssetStatus status,
    
    @Schema(description = "Asset condition", example = "GOOD")
    AssetCondition condition
) {
    
    /**
     * Get effective depreciation method with default.
     */
    public String getEffectiveDepreciationMethod() {
        return depreciationMethod != null && !depreciationMethod.trim().isEmpty() 
            ? depreciationMethod 
            : "straight_line";
    }
    
    /**
     * Get effective asset status with default.
     */
    public AssetStatus getEffectiveStatus() {
        return status != null ? status : AssetStatus.ACTIVE;
    }
    
    /**
     * Get effective asset condition with default.
     */
    public AssetCondition getEffectiveCondition() {
        return condition != null ? condition : AssetCondition.GOOD;
    }
    
    /**
     * Get effective maintenance frequency with default.
     */
    public Integer getEffectiveMaintenanceFrequencyDays() {
        return maintenanceFrequencyDays != null ? maintenanceFrequencyDays : 90;
    }
    
    /**
     * Get effective current value (defaults to purchase cost).
     */
    public BigDecimal getEffectiveCurrentValue() {
        if (currentValue != null) {
            return currentValue;
        }
        return purchaseCost != null ? purchaseCost : BigDecimal.ZERO;
    }
    
    /**
     * Calculate next maintenance date if not provided.
     */
    public LocalDate getEffectiveNextMaintenanceDate() {
        if (nextMaintenanceDate != null) {
            return nextMaintenanceDate;
        }
        return LocalDate.now().plusDays(getEffectiveMaintenanceFrequencyDays());
    }
    
    /**
     * Check if warranty dates are valid.
     */
    public boolean areWarrantyDatesValid() {
        if (warrantyStartDate == null || warrantyEndDate == null) {
            return true; // Optional fields
        }
        return !warrantyEndDate.isBefore(warrantyStartDate);
    }
    
    /**
     * Check if purchase date is reasonable.
     */
    public boolean isPurchaseDateValid() {
        if (purchaseDate == null) {
            return true; // Optional field
        }
        return !purchaseDate.isAfter(LocalDate.now()) && 
               !purchaseDate.isBefore(LocalDate.now().minusYears(50));
    }
    
    /**
     * Check if financial values are consistent.
     */
    public boolean areFinancialValuesValid() {
        if (purchaseCost == null) {
            return true; // Optional field
        }
        
        // Current value should not exceed purchase cost (unless appreciation)
        if (currentValue != null && currentValue.compareTo(purchaseCost.multiply(BigDecimal.valueOf(1.5))) > 0) {
            return false;
        }
        
        // Salvage value should not exceed purchase cost
        if (salvageValue != null && salvageValue.compareTo(purchaseCost) > 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate asset code format more thoroughly.
     */
    public boolean isAssetCodeFormatValid() {
        if (assetCode == null || assetCode.trim().isEmpty()) {
            return false;
        }
        
        // Should contain at least one letter and one number or hyphen
        return assetCode.matches(".*[A-Z].*") && 
               (assetCode.matches(".*[0-9].*") || assetCode.contains("-") || assetCode.contains("_"));
    }
    
    /**
     * Get display name for the asset.
     */
    public String getDisplayName() {
        return (nameAr != null && !nameAr.trim().isEmpty()) ? nameAr : name;
    }
    
    /**
     * Check if asset has location information.
     */
    public boolean hasLocationInfo() {
        return schoolId != null || 
               (department != null && !department.trim().isEmpty()) ||
               (location != null && !location.trim().isEmpty());
    }
    
    /**
     * Check if asset has complete purchase information.
     */
    public boolean hasCompletePurchaseInfo() {
        return purchaseDate != null && 
               purchaseCost != null && 
               (supplier != null && !supplier.trim().isEmpty());
    }
}