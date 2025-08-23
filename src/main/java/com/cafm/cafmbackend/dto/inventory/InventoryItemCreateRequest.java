package com.cafm.cafmbackend.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for creating a new inventory item.
 * 
 * Purpose: Validates and transfers inventory item creation data from API to service layer
 * Pattern: Record-based DTO with comprehensive validation annotations
 * Java 23: Uses record for immutable data transfer object
 * Architecture: API layer DTO with validation constraints
 * Standards: Bean validation, OpenAPI documentation, defensive validation
 */
@Schema(description = "Request to create a new inventory item")
public record InventoryItemCreateRequest(
    
    @NotBlank(message = "Item code is required")
    @Size(max = 50, message = "Item code cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Item code must contain only uppercase letters, numbers, hyphens and underscores")
    @Schema(description = "Unique item code", example = "TOOL-HAMMER-001")
    String itemCode,
    
    @NotBlank(message = "Item name is required")
    @Size(max = 200, message = "Item name cannot exceed 200 characters")
    @Schema(description = "Item name in English", example = "Standard Hammer")
    String name,
    
    @Size(max = 200, message = "Arabic name cannot exceed 200 characters")
    @Schema(description = "Item name in Arabic", example = "مطرقة عادية")
    String nameAr,
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Schema(description = "Item description", example = "Standard construction hammer for general maintenance work")
    String description,
    
    @NotNull(message = "Category ID is required")
    @Schema(description = "Category ID this item belongs to")
    UUID categoryId,
    
    @Size(max = 100, message = "Manufacturer cannot exceed 100 characters")
    @Schema(description = "Manufacturer name", example = "Stanley Tools")
    String manufacturer,
    
    @Size(max = 100, message = "Model cannot exceed 100 characters")
    @Schema(description = "Model number", example = "STHT51512")
    String model,
    
    @Size(max = 50, message = "SKU cannot exceed 50 characters")
    @Schema(description = "Stock keeping unit", example = "STL-HAMMER-16OZ")
    String sku,
    
    @Size(max = 100, message = "Barcode cannot exceed 100 characters")
    @Schema(description = "Barcode number", example = "123456789012")
    String barcode,
    
    @Size(max = 20, message = "Unit of measure cannot exceed 20 characters")
    @Schema(description = "Unit of measure", example = "each", defaultValue = "each")
    String unitOfMeasure,
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Current stock cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid stock quantity format")
    @Schema(description = "Current stock quantity", example = "25.00")
    BigDecimal currentStock,
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum stock cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid minimum stock format")
    @Schema(description = "Minimum stock level", example = "5.00")
    BigDecimal minimumStock,
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Reorder level cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid reorder level format")
    @Schema(description = "Reorder level threshold", example = "10.00")
    BigDecimal reorderLevel,
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Maximum stock cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid maximum stock format")
    @Schema(description = "Maximum stock level", example = "100.00")
    BigDecimal maximumStock,
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Unit cost cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid unit cost format")
    @Schema(description = "Unit cost", example = "25.99")
    BigDecimal unitCost,
    
    @Size(max = 100, message = "Supplier cannot exceed 100 characters")
    @Schema(description = "Primary supplier", example = "Hardware Supplies Inc.")
    String supplier,
    
    @Size(max = 100, message = "Location cannot exceed 100 characters")
    @Schema(description = "Storage location", example = "Warehouse A - Shelf 3")
    String location,
    
    @Schema(description = "Notes or additional information", example = "High-quality hammer for heavy-duty work")
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    String notes,
    
    @Schema(description = "Whether item is active", defaultValue = "true")
    Boolean isActive
) {
    
    /**
     * Get effective current stock (default to 0 if null).
     */
    public BigDecimal getEffectiveCurrentStock() {
        return currentStock != null ? currentStock : BigDecimal.ZERO;
    }
    
    /**
     * Get effective minimum stock (default to 0 if null).
     */
    public BigDecimal getEffectiveMinimumStock() {
        return minimumStock != null ? minimumStock : BigDecimal.ZERO;
    }
    
    /**
     * Get effective reorder level (default to minimum stock if null).
     */
    public BigDecimal getEffectiveReorderLevel() {
        return reorderLevel != null ? reorderLevel : getEffectiveMinimumStock();
    }
    
    /**
     * Get effective unit of measure (default to "each" if null).
     */
    public String getEffectiveUnitOfMeasure() {
        return unitOfMeasure != null && !unitOfMeasure.trim().isEmpty() ? unitOfMeasure : "each";
    }
    
    /**
     * Get effective active status (default to true if null).
     */
    public Boolean getEffectiveIsActive() {
        return isActive != null ? isActive : true;
    }
}