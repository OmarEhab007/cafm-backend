package com.cafm.cafmbackend.infrastructure.persistence.entity;

import com.cafm.cafmbackend.infrastructure.persistence.entity.base.BaseEntity;
import com.cafm.cafmbackend.shared.enums.InventoryTransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Inventory Transaction entity for tracking all inventory movements.
 * Provides complete audit trail for stock changes.
 */
@Entity
@Table(name = "inventory_transactions")
@NamedQueries({
    @NamedQuery(
        name = "InventoryTransaction.findByItem",
        query = "SELECT it FROM InventoryTransaction it WHERE it.item.id = :itemId ORDER BY it.transactionDate DESC"
    ),
    @NamedQuery(
        name = "InventoryTransaction.findByWorkOrder",
        query = "SELECT it FROM InventoryTransaction it WHERE it.workOrder.id = :workOrderId ORDER BY it.transactionDate"
    ),
    @NamedQuery(
        name = "InventoryTransaction.findPendingApproval",
        query = "SELECT it FROM InventoryTransaction it WHERE it.company.id = :companyId AND it.approvedBy IS NULL AND it.transactionType IN ('ADJUSTMENT', 'DAMAGE')"
    )
})
public class InventoryTransaction extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @NotNull(message = "Company is required")
    private Company company;
    
    @Column(name = "transaction_number", nullable = false, length = 50)
    @NotBlank(message = "Transaction number is required")
    @Size(max = 50, message = "Transaction number cannot exceed 50 characters")
    private String transactionNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    @NotNull(message = "Item is required")
    private InventoryItem item;
    
    @Column(name = "transaction_type", nullable = false)
    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    private InventoryTransactionType transactionType;
    
    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;
    
    // ========== Quantities ==========
    
    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Quantity is required")
    private BigDecimal quantity;
    
    @Column(name = "unit_cost", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Unit cost cannot be negative")
    private BigDecimal unitCost;
    
    @Column(name = "total_cost", precision = 10, scale = 2, insertable = false, updatable = false)
    private BigDecimal totalCost;
    
    // ========== Stock Levels ==========
    
    @Column(name = "stock_before", precision = 10, scale = 2)
    private BigDecimal stockBefore;
    
    @Column(name = "stock_after", precision = 10, scale = 2)
    private BigDecimal stockAfter;
    
    // ========== Reference ==========
    
    @Column(name = "reference_type", length = 50)
    @Size(max = 50, message = "Reference type cannot exceed 50 characters")
    private String referenceType; // 'work_order', 'purchase_order', 'manual', 'transfer'
    
    @Column(name = "reference_id")
    private UUID referenceId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;
    
    // ========== Transfer Details ==========
    
    @Column(name = "from_location", length = 100)
    @Size(max = 100, message = "From location cannot exceed 100 characters")
    private String fromLocation;
    
    @Column(name = "to_location", length = 100)
    @Size(max = 100, message = "To location cannot exceed 100 characters")
    private String toLocation;
    
    // ========== Additional Info ==========
    
    @Column(name = "supplier", length = 255)
    @Size(max = 255, message = "Supplier cannot exceed 255 characters")
    private String supplier;
    
    @Column(name = "invoice_number", length = 50)
    @Size(max = 50, message = "Invoice number cannot exceed 50 characters")
    private String invoiceNumber;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    // ========== Audit ==========
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private User createdByUser;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    // ========== Constructors ==========
    
    public InventoryTransaction() {
        super();
        this.transactionDate = LocalDateTime.now();
    }
    
    public InventoryTransaction(Company company, String transactionNumber, 
                              InventoryItem item, InventoryTransactionType type, 
                              BigDecimal quantity) {
        this();
        this.company = company;
        this.transactionNumber = transactionNumber;
        this.item = item;
        this.transactionType = type;
        this.quantity = quantity;
    }
    
    // ========== Business Methods ==========
    
    /**
     * Apply this transaction to the inventory item
     */
    public void applyToInventory() {
        if (item == null) {
            throw new IllegalStateException("Cannot apply transaction without item");
        }
        
        // Record stock before
        this.stockBefore = item.getCurrentStock();
        
        // Apply based on transaction type
        BigDecimal absQuantity = quantity.abs();
        if (transactionType.increasesStock()) {
            item.updateStock(absQuantity, true);
        } else if (transactionType.decreasesStock()) {
            item.updateStock(absQuantity, false);
        } else if (transactionType == InventoryTransactionType.ADJUSTMENT) {
            // For adjustments, quantity can be positive or negative
            if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                item.updateStock(absQuantity, true);
            } else {
                item.updateStock(absQuantity, false);
            }
        }
        
        // Record stock after
        this.stockAfter = item.getCurrentStock();
        
        // Update average cost if this is a receipt with cost info
        if (transactionType == InventoryTransactionType.RECEIPT && unitCost != null) {
            item.updateAverageCost(absQuantity, unitCost);
        }
    }
    
    /**
     * Reverse this transaction
     */
    public InventoryTransaction createReversal(String reason) {
        InventoryTransaction reversal = new InventoryTransaction();
        reversal.setCompany(this.company);
        reversal.setTransactionNumber(this.transactionNumber + "-REV");
        reversal.setItem(this.item);
        reversal.setTransactionType(InventoryTransactionType.ADJUSTMENT);
        reversal.setQuantity(this.quantity.negate());
        reversal.setUnitCost(this.unitCost);
        reversal.setNotes("Reversal of transaction " + this.transactionNumber + ". Reason: " + reason);
        reversal.setReferenceType("reversal");
        reversal.setReferenceId(this.getId());
        return reversal;
    }


    /**
     * Check if transaction needs approval
     */
    public boolean needsApproval() {
        return transactionType != null && transactionType.requiresApproval() && approvedBy == null;
    }
    
    /**
     * Approve this transaction
     */
    public void approve(User approver) {
        if (!needsApproval()) {
            throw new IllegalStateException("Transaction does not require approval or is already approved");
        }
        this.approvedBy = approver;
        this.approvedAt = LocalDateTime.now();
    }
    
    /**
     * Check if transaction is approved
     */
    public boolean isApproved() {
        if (!transactionType.requiresApproval()) {
            return true; // Auto-approved if not required
        }
        return approvedBy != null;
    }
    
    /**
     * Calculate total cost
     */
    public BigDecimal calculateTotalCost() {
        if (quantity == null || unitCost == null) {
            return BigDecimal.ZERO;
        }
        return quantity.abs().multiply(unitCost).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Get effective quantity (considering transaction type)
     */
    public BigDecimal getEffectiveQuantity() {
        if (quantity == null) return BigDecimal.ZERO;
        
        if (transactionType.increasesStock()) {
            return quantity.abs();
        } else if (transactionType.decreasesStock()) {
            return quantity.abs().negate();
        } else {
            return quantity; // For adjustments, use as-is
        }
    }
    
    /**
     * Get transaction description
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(transactionType.getDisplayName());
        sb.append(": ").append(quantity.abs()).append(" ").append(item.getUnitOfMeasure());
        
        if (workOrder != null) {
            sb.append(" for WO#").append(workOrder.getWorkOrderNumber());
        } else if (invoiceNumber != null) {
            sb.append(" - Invoice: ").append(invoiceNumber);
        } else if (referenceType != null && referenceId != null) {
            sb.append(" - Ref: ").append(referenceType);
        }
        
        return sb.toString();
    }
    
    /**
     * Validate transaction before applying
     */
    public void validate() {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException("Transaction quantity cannot be zero");
        }
        
        if (transactionType.decreasesStock()) {
            BigDecimal absQuantity = quantity.abs();
            if (item.getCurrentStock().compareTo(absQuantity) < 0) {
                throw new IllegalStateException("Insufficient stock. Available: " + 
                    item.getCurrentStock() + ", Required: " + absQuantity);
            }
        }
        
        if (transactionType == InventoryTransactionType.TRANSFER) {
            if (fromLocation == null || toLocation == null) {
                throw new IllegalStateException("Transfer requires both from and to locations");
            }
        }
    }
    
    /**
     * Generate transaction number
     */
    public static String generateTransactionNumber(InventoryTransactionType type, String companyCode) {
        String prefix = type.name().substring(0, 3).toUpperCase();
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7);
        return prefix + "-" + companyCode + "-" + timestamp;
    }
    
    // ========== Getters and Setters ==========
    
    public Company getCompany() {
        return company;
    }
    
    public void setCompany(Company company) {
        this.company = company;
    }
    
    public String getTransactionNumber() {
        return transactionNumber;
    }
    
    public void setTransactionNumber(String transactionNumber) {
        this.transactionNumber = transactionNumber;
    }
    
    public InventoryItem getItem() {
        return item;
    }
    
    public void setItem(InventoryItem item) {
        this.item = item;
    }
    
    public InventoryTransactionType getTransactionType() {
        return transactionType;
    }
    
    public void setTransactionType(InventoryTransactionType transactionType) {
        this.transactionType = transactionType;
    }
    
    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }
    
    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }
    
    public BigDecimal getQuantity() {
        return quantity;
    }
    
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getUnitCost() {
        return unitCost;
    }
    
    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }
    
    public BigDecimal getTotalCost() {
        if (totalCost == null) {
            return calculateTotalCost();
        }
        return totalCost;
    }
    
    public BigDecimal getStockBefore() {
        return stockBefore;
    }
    
    public void setStockBefore(BigDecimal stockBefore) {
        this.stockBefore = stockBefore;
    }
    
    public BigDecimal getStockAfter() {
        return stockAfter;
    }
    
    public void setStockAfter(BigDecimal stockAfter) {
        this.stockAfter = stockAfter;
    }
    
    public String getReferenceType() {
        return referenceType;
    }
    
    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }
    
    public UUID getReferenceId() {
        return referenceId;
    }
    
    public void setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
    }
    
    public WorkOrder getWorkOrder() {
        return workOrder;
    }
    
    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }
    
    public String getFromLocation() {
        return fromLocation;
    }
    
    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }
    
    public String getToLocation() {
        return toLocation;
    }
    
    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }
    
    public String getSupplier() {
        return supplier;
    }
    
    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }
    
    public String getInvoiceNumber() {
        return invoiceNumber;
    }
    
    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public User getCreatedByUser() {
        return createdByUser;
    }
    
    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }
    
    public User getApprovedBy() {
        return approvedBy;
    }
    
    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }
    
    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }
    
    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
    
    // ========== toString, equals, hashCode ==========
    
    @Override
    public String toString() {
        return String.format("InventoryTransaction[id=%s, number=%s, item=%s, type=%s, quantity=%s]",
            getId(), transactionNumber, 
            item != null ? item.getItemCode() : "null",
            transactionType, quantity);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryTransaction)) return false;
        if (!super.equals(o)) return false;
        InventoryTransaction that = (InventoryTransaction) o;
        return Objects.equals(company, that.company) &&
               Objects.equals(transactionNumber, that.transactionNumber);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), company, transactionNumber);
    }
}