package com.cafm.cafmbackend.dto.inventory;

import com.cafm.cafmbackend.shared.enums.InventoryTransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for inventory item details.
 * 
 * Purpose: Transfers comprehensive inventory item data from service layer to API
 * Pattern: Record-based DTO with computed fields and status indicators
 * Java 23: Uses record for immutable data transfer object
 * Architecture: API layer DTO with business logic indicators
 * Standards: OpenAPI documentation, comprehensive data representation
 */
@Schema(description = "Detailed inventory item information")
public record InventoryItemResponse(
    
    @Schema(description = "Item unique identifier")
    UUID id,
    
    @Schema(description = "Unique item code", example = "TOOL-HAMMER-001")
    String itemCode,
    
    @Schema(description = "Item name in English", example = "Standard Hammer")
    String name,
    
    @Schema(description = "Item name in Arabic", example = "مطرقة عادية")
    String nameAr,
    
    @Schema(description = "Item description")
    String description,
    
    @Schema(description = "Category ID")
    UUID categoryId,
    
    @Schema(description = "Category name")
    String categoryName,
    
    @Schema(description = "Manufacturer name")
    String manufacturer,
    
    @Schema(description = "Model number")
    String model,
    
    @Schema(description = "Stock keeping unit")
    String sku,
    
    @Schema(description = "Barcode number")
    String barcode,
    
    @Schema(description = "Unit of measure", example = "each")
    String unitOfMeasure,
    
    @Schema(description = "Current stock quantity", example = "25.00")
    BigDecimal currentStock,
    
    @Schema(description = "Minimum stock level", example = "5.00")
    BigDecimal minimumStock,
    
    @Schema(description = "Reorder level threshold", example = "10.00")
    BigDecimal reorderLevel,
    
    @Schema(description = "Maximum stock level", example = "100.00")
    BigDecimal maximumStock,
    
    @Schema(description = "Average unit cost", example = "25.99")
    BigDecimal averageUnitCost,
    
    @Schema(description = "Last unit cost", example = "26.50")
    BigDecimal lastUnitCost,
    
    @Schema(description = "Total value of current stock", example = "649.75")
    BigDecimal totalValue,
    
    @Schema(description = "Primary supplier")
    String supplier,
    
    @Schema(description = "Storage location")
    String location,
    
    @Schema(description = "Notes or additional information")
    String notes,
    
    @Schema(description = "Whether item is active")
    Boolean isActive,
    
    @Schema(description = "Company ID")
    UUID companyId,
    
    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt,
    
    @Schema(description = "Last update timestamp")
    LocalDateTime updatedAt,
    
    @Schema(description = "Last transaction timestamp")
    LocalDateTime lastTransactionDate,
    
    @Schema(description = "Last transaction type")
    InventoryTransactionType lastTransactionType,
    
    // Computed fields
    @Schema(description = "Whether stock is below minimum level")
    Boolean isLowStock,
    
    @Schema(description = "Whether stock needs reordering")
    Boolean needsReorder,
    
    @Schema(description = "Whether stock is critically low (below 50% of minimum)")
    Boolean isCriticallyLow,
    
    @Schema(description = "Days since last transaction")
    Integer daysSinceLastTransaction,
    
    @Schema(description = "Stock level percentage (current/maximum)")
    BigDecimal stockLevelPercentage,
    
    @Schema(description = "Quantity needed to reach reorder level")
    BigDecimal quantityToReorder,
    
    @Schema(description = "Available stock (current - reserved)")
    BigDecimal availableStock,
    
    @Schema(description = "Reserved stock quantity")
    BigDecimal reservedStock,
    
    @Schema(description = "Number of active transactions")
    Integer activeTransactionCount
) {
    
    /**
     * Check if item is out of stock.
     */
    public boolean isOutOfStock() {
        return currentStock == null || currentStock.compareTo(BigDecimal.ZERO) <= 0;
    }
    
    /**
     * Check if item is overstocked.
     */
    public boolean isOverstocked() {
        return maximumStock != null && currentStock != null && 
               currentStock.compareTo(maximumStock) > 0;
    }
    
    /**
     * Get stock status as string.
     */
    public String getStockStatus() {
        if (isOutOfStock()) {
            return "OUT_OF_STOCK";
        } else if (isCriticallyLow != null && isCriticallyLow) {
            return "CRITICALLY_LOW";
        } else if (isLowStock != null && isLowStock) {
            return "LOW_STOCK";
        } else if (needsReorder != null && needsReorder) {
            return "NEEDS_REORDER";
        } else if (isOverstocked()) {
            return "OVERSTOCKED";
        } else {
            return "NORMAL";
        }
    }
    
    /**
     * Get stock health score (0-100).
     */
    public int getStockHealthScore() {
        if (isOutOfStock()) {
            return 0;
        }
        
        if (minimumStock == null || minimumStock.compareTo(BigDecimal.ZERO) == 0) {
            return currentStock.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
        }
        
        BigDecimal ratio = currentStock.divide(minimumStock, 2, BigDecimal.ROUND_HALF_UP);
        
        if (ratio.compareTo(BigDecimal.valueOf(2)) >= 0) {
            return 100; // Well stocked
        } else if (ratio.compareTo(BigDecimal.ONE) >= 0) {
            return 70 + (ratio.subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(30))).intValue();
        } else if (ratio.compareTo(BigDecimal.valueOf(0.5)) >= 0) {
            return 40 + (ratio.multiply(BigDecimal.valueOf(60))).intValue();
        } else {
            return Math.max(0, (ratio.multiply(BigDecimal.valueOf(80))).intValue());
        }
    }
}