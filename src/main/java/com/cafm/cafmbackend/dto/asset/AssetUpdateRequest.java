package com.cafm.cafmbackend.dto.asset;

import com.cafm.cafmbackend.data.enums.AssetCondition;
import com.cafm.cafmbackend.data.enums.AssetStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Asset update request DTO.
 * 
 * Purpose: Validates and transfers asset update data
 * Pattern: Immutable record with validation for partial updates
 * Java 23: Record with optional fields for PATCH-style updates
 * Architecture: Multi-tenant aware asset modification with lifecycle support
 */
@Schema(description = "Asset update request")
public record AssetUpdateRequest(
    @Size(max = 255, message = "Asset name cannot exceed 255 characters")
    @Schema(description = "Asset name", example = "Dell Latitude 5520")
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
    
    @Size(max = 100, message = "Barcode cannot exceed 100 characters")
    @Schema(description = "Barcode", example = "123456789012")
    String barcode,
    
    // Purchase Information (usually not updated)
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
    @DecimalMin(value = "0.0", message = "Current value cannot be negative")
    @DecimalMax(value = "999999.99", message = "Current value cannot exceed 999,999.99")
    @Digits(integer = 6, fraction = 2, message = "Current value must have at most 6 integer digits and 2 decimal places")
    @Schema(description = "Current value", example = "1200.00")
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
    AssetCondition condition,
    
    @Schema(description = "Is asset active", example = "true")
    Boolean isActive
) {
    
    /**
     * Check if any field is provided for update.
     */
    public boolean hasUpdates() {
        return name != null || nameAr != null || description != null || categoryId != null ||
               manufacturer != null || model != null || barcode != null ||
               purchaseOrderNumber != null || supplier != null || 
               warrantyStartDate != null || warrantyEndDate != null ||
               currentValue != null || salvageValue != null || depreciationMethod != null ||
               schoolId != null || department != null || location != null ||
               maintenanceFrequencyDays != null || nextMaintenanceDate != null ||
               status != null || condition != null || isActive != null;
    }
    
    /**
     * Check if this is a basic information update.
     */
    public boolean isBasicInfoUpdate() {
        return name != null || nameAr != null || description != null || 
               manufacturer != null || model != null || barcode != null;
    }
    
    /**
     * Check if this is a financial update.
     */
    public boolean isFinancialUpdate() {
        return currentValue != null || salvageValue != null || depreciationMethod != null;
    }
    
    /**
     * Check if this is a location update.
     */
    public boolean isLocationUpdate() {
        return schoolId != null || department != null || location != null;
    }
    
    /**
     * Check if this is a maintenance update.
     */
    public boolean isMaintenanceUpdate() {
        return maintenanceFrequencyDays != null || nextMaintenanceDate != null;
    }
    
    /**
     * Check if this is a status update.
     */
    public boolean isStatusUpdate() {
        return status != null || condition != null || isActive != null;
    }
    
    /**
     * Check if this is a warranty update.
     */
    public boolean isWarrantyUpdate() {
        return warrantyStartDate != null || warrantyEndDate != null;
    }
    
    /**
     * Check if this is a category assignment update.
     */
    public boolean isCategoryUpdate() {
        return categoryId != null;
    }
    
    /**
     * Check if warranty dates are valid (if both provided).
     */
    public boolean areWarrantyDatesValid() {
        if (warrantyStartDate == null || warrantyEndDate == null) {
            return true; // Only validate if both are provided
        }
        return !warrantyEndDate.isBefore(warrantyStartDate);
    }
    
    /**
     * Check if the status transition is for disposal.
     */
    public boolean isDisposalUpdate() {
        return status == AssetStatus.DISPOSED;
    }
    
    /**
     * Check if the status transition is for retirement.
     */
    public boolean isRetirementUpdate() {
        return status == AssetStatus.RETIRED;
    }
    
    /**
     * Check if the status transition is for maintenance.
     */
    public boolean isMaintenanceStatusUpdate() {
        return status == AssetStatus.MAINTENANCE;
    }
    
    /**
     * Check if condition is being downgraded.
     */
    public boolean isConditionDowngrade(AssetCondition currentCondition) {
        if (condition == null || currentCondition == null) {
            return false;
        }
        
        // Define condition hierarchy (higher ordinal = worse condition)
        return condition.ordinal() > currentCondition.ordinal();
    }

    /**
     * Check if this update requires approval.
     */
    public boolean requiresApproval() {
        return isDisposalUpdate() || isRetirementUpdate() || 
               (isFinancialUpdate() && currentValue != null) ||
               (condition != null && condition == AssetCondition.UNUSABLE);
    }
    
    /**
     * Get effective display name update.
     */
    public String getEffectiveDisplayNameUpdate(String currentName, String currentNameAr) {
        String newName = name != null ? name : currentName;
        String newNameAr = nameAr != null ? nameAr : currentNameAr;
        
        return (newNameAr != null && !newNameAr.trim().isEmpty()) ? newNameAr : newName;
    }
    
    /**
     * Check if location is being cleared.
     */
    public boolean isLocationBeingCleared() {
        return (schoolId != null && schoolId.toString().isEmpty()) ||
               (department != null && department.trim().isEmpty()) ||
               (location != null && location.trim().isEmpty());
    }
}