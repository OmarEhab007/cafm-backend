package com.cafm.cafmbackend.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for updating an existing inventory item.
 * 
 * Purpose: Validates and transfers inventory item update data from API to service layer
 * Pattern: Record-based DTO with optional fields for partial updates
 * Java 23: Uses record for immutable data transfer object
 * Architecture: API layer DTO with validation constraints
 * Standards: Bean validation, OpenAPI documentation, partial update support
 */
@Schema(description = "Request to update an existing inventory item")
public record InventoryItemUpdateRequest(
    
    @Size(max = 200, message = "Item name cannot exceed 200 characters")
    @Schema(description = "Item name in English", example = "Heavy Duty Hammer")
    String name,
    
    @Size(max = 200, message = "Arabic name cannot exceed 200 characters")
    @Schema(description = "Item name in Arabic", example = "مطرقة ثقيلة")
    String nameAr,
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Schema(description = "Item description", example = "Heavy duty construction hammer for professional use")
    String description,
    
    @Schema(description = "Category ID this item belongs to")
    UUID categoryId,
    
    @Size(max = 100, message = "Manufacturer cannot exceed 100 characters")
    @Schema(description = "Manufacturer name", example = "Stanley Tools Pro")
    String manufacturer,
    
    @Size(max = 100, message = "Model cannot exceed 100 characters")
    @Schema(description = "Model number", example = "STHT51512-HD")
    String model,
    
    @Size(max = 50, message = "SKU cannot exceed 50 characters")
    @Schema(description = "Stock keeping unit", example = "STL-HAMMER-20OZ")
    String sku,
    
    @Size(max = 100, message = "Barcode cannot exceed 100 characters")
    @Schema(description = "Barcode number", example = "123456789013")
    String barcode,
    
    @Size(max = 20, message = "Unit of measure cannot exceed 20 characters")
    @Schema(description = "Unit of measure", example = "each")
    String unitOfMeasure,
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum stock cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid minimum stock format")
    @Schema(description = "Minimum stock level", example = "3.00")
    BigDecimal minimumStock,
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Reorder level cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid reorder level format")
    @Schema(description = "Reorder level threshold", example = "8.00")
    BigDecimal reorderLevel,
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Maximum stock cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid maximum stock format")
    @Schema(description = "Maximum stock level", example = "150.00")
    BigDecimal maximumStock,
    
    @Size(max = 100, message = "Supplier cannot exceed 100 characters")
    @Schema(description = "Primary supplier", example = "Professional Hardware Inc.")
    String supplier,
    
    @Size(max = 100, message = "Location cannot exceed 100 characters")
    @Schema(description = "Storage location", example = "Warehouse B - Shelf 1")
    String location,
    
    @Schema(description = "Notes or additional information", example = "Professional grade tool for heavy construction")
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    String notes,
    
    @Schema(description = "Whether item is active")
    Boolean isActive
) {
    
    /**
     * Check if this is a meaningful update (at least one field is not null).
     */
    public boolean hasUpdates() {
        return name != null || nameAr != null || description != null || 
               categoryId != null || manufacturer != null || model != null ||
               sku != null || barcode != null || unitOfMeasure != null ||
               minimumStock != null || reorderLevel != null || maximumStock != null ||
               supplier != null || location != null || notes != null || isActive != null;
    }
    
    /**
     * Check if stock level parameters are being updated.
     */
    public boolean hasStockLevelUpdates() {
        return minimumStock != null || reorderLevel != null || maximumStock != null;
    }
    
    /**
     * Check if basic information is being updated.
     */
    public boolean hasBasicInfoUpdates() {
        return name != null || nameAr != null || description != null || 
               manufacturer != null || model != null;
    }
    
    /**
     * Check if classification is being updated.
     */
    public boolean hasClassificationUpdates() {
        return categoryId != null || sku != null || barcode != null || unitOfMeasure != null;
    }
}