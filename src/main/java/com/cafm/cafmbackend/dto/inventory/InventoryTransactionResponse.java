package com.cafm.cafmbackend.dto.inventory;

import com.cafm.cafmbackend.shared.enums.InventoryTransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for inventory transaction details.
 * 
 * Purpose: Transfers inventory transaction data from service layer to API
 * Pattern: Record-based DTO with transaction tracking information
 * Java 23: Uses record for immutable data transfer object
 * Architecture: API layer DTO with audit trail information
 * Standards: OpenAPI documentation, comprehensive transaction data
 */
@Schema(description = "Inventory transaction details")
public record InventoryTransactionResponse(
    
    @Schema(description = "Transaction unique identifier")
    UUID id,
    
    @Schema(description = "Inventory item ID")
    UUID inventoryItemId,
    
    @Schema(description = "Inventory item code")
    String itemCode,
    
    @Schema(description = "Inventory item name")
    String itemName,
    
    @Schema(description = "Transaction type", example = "STOCK_IN")
    InventoryTransactionType transactionType,
    
    @Schema(description = "Transaction quantity", example = "15.00")
    BigDecimal quantity,
    
    @Schema(description = "Unit cost at time of transaction", example = "25.99")
    BigDecimal unitCost,
    
    @Schema(description = "Total cost of transaction", example = "389.85")
    BigDecimal totalCost,
    
    @Schema(description = "Stock level before transaction", example = "10.00")
    BigDecimal stockBefore,
    
    @Schema(description = "Stock level after transaction", example = "25.00")
    BigDecimal stockAfter,
    
    @Schema(description = "Transaction reference number")
    String reference,
    
    @Schema(description = "Transaction reason or description")
    String reason,
    
    @Schema(description = "Work order ID if transaction is related to work order")
    UUID workOrderId,
    
    @Schema(description = "Work order number if related")
    String workOrderNumber,
    
    @Schema(description = "User who performed the transaction")
    UUID performedBy,
    
    @Schema(description = "User name who performed the transaction")
    String performedByName,
    
    @Schema(description = "Transaction timestamp")
    LocalDateTime transactionDate,
    
    @Schema(description = "Additional notes")
    String notes,
    
    @Schema(description = "Approval status", example = "APPROVED")
    String approvalStatus,
    
    @Schema(description = "Approved by user ID")
    UUID approvedBy,
    
    @Schema(description = "Approved by user name")
    String approvedByName,
    
    @Schema(description = "Approval date")
    LocalDateTime approvalDate,
    
    @Schema(description = "Company ID")
    UUID companyId,
    
    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt,
    
    @Schema(description = "Last update timestamp")
    LocalDateTime updatedAt,
    
    // Computed fields
    @Schema(description = "Whether transaction increases stock")
    Boolean isStockIncrease,
    
    @Schema(description = "Whether transaction decreases stock")
    Boolean isStockDecrease,
    
    @Schema(description = "Whether transaction requires approval")
    Boolean requiresApproval,
    
    @Schema(description = "Whether transaction is pending approval")
    Boolean isPendingApproval,
    
    @Schema(description = "Days since transaction")
    Integer daysSinceTransaction
) {
    
    /**
     * Check if transaction is approved.
     */
    public boolean isApproved() {
        return "APPROVED".equals(approvalStatus);
    }
    
    /**
     * Check if transaction is rejected.
     */
    public boolean isRejected() {
        return "REJECTED".equals(approvalStatus);
    }
    
    /**
     * Check if transaction can be reversed.
     */
    public boolean canBeReversed() {
        return isApproved() && transactionDate != null && 
               transactionDate.isAfter(LocalDateTime.now().minusDays(30));
    }
    
    /**
     * Get transaction impact on stock.
     */
    public BigDecimal getStockImpact() {
        if (stockAfter != null && stockBefore != null) {
            return stockAfter.subtract(stockBefore);
        }
        return quantity != null ? quantity : BigDecimal.ZERO;
    }
    
    /**
     * Get transaction value (absolute).
     */
    public BigDecimal getTransactionValue() {
        if (totalCost != null) {
            return totalCost.abs();
        }
        if (unitCost != null && quantity != null) {
            return unitCost.multiply(quantity).abs();
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Get transaction status summary.
     */
    public String getStatusSummary() {
        if (isPendingApproval != null && isPendingApproval) {
            return "Pending Approval";
        }
        if (isApproved()) {
            return "Approved";
        }
        if (isRejected()) {
            return "Rejected";
        }
        return "Processing";
    }
}