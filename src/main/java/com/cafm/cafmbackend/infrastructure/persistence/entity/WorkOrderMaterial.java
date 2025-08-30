package com.cafm.cafmbackend.infrastructure.persistence.entity;

import com.cafm.cafmbackend.infrastructure.persistence.entity.base.TenantAwareEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Work Order Material entity for tracking materials used in work orders.
 * Links inventory items to work orders with quantity and cost tracking.
 */
@Entity
@Table(name = "work_order_materials")
public class WorkOrderMaterial extends TenantAwareEntity {
    // SECURITY: Material tracking with tenant isolation
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    @NotNull(message = "Work order is required")
    private WorkOrder workOrder;
    
    @Column(name = "material_name", nullable = false, length = 255)
    @NotBlank(message = "Material name is required")
    @Size(max = 255, message = "Material name cannot exceed 255 characters")
    private String materialName;
    
    @Column(name = "material_code", length = 50)
    @Size(max = 50, message = "Material code cannot exceed 50 characters")
    private String materialCode;
    
    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private BigDecimal quantity;
    
    @Column(name = "unit_of_measure", length = 50)
    @Size(max = 50, message = "Unit of measure cannot exceed 50 characters")
    private String unitOfMeasure;
    
    @Column(name = "unit_cost", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Unit cost cannot be negative")
    private BigDecimal unitCost;
    
    @Column(name = "total_cost", precision = 10, scale = 2, insertable = false, updatable = false)
    private BigDecimal totalCost;
    
    @Column(name = "supplier", length = 255)
    @Size(max = 255, message = "Supplier name cannot exceed 255 characters")
    private String supplier;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    // ========== Constructors ==========
    
    public WorkOrderMaterial() {
        super();
    }
    
    public WorkOrderMaterial(WorkOrder workOrder, String materialName, BigDecimal quantity) {
        this();
        this.workOrder = workOrder;
        this.materialName = materialName;
        this.quantity = quantity;
    }
    
    public WorkOrderMaterial(WorkOrder workOrder, String materialName, String materialCode, 
                           BigDecimal quantity, String unitOfMeasure, BigDecimal unitCost) {
        this(workOrder, materialName, quantity);
        this.materialCode = materialCode;
        this.unitOfMeasure = unitOfMeasure;
        this.unitCost = unitCost;
    }
    
    // ========== Business Methods ==========
    
    /**
     * Calculate total cost based on quantity and unit cost
     */
    public BigDecimal calculateTotalCost() {
        if (quantity == null || unitCost == null) {
            return BigDecimal.ZERO;
        }
        return quantity.multiply(unitCost).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Update quantity and recalculate total
     */
    public void updateQuantity(BigDecimal newQuantity) {
        if (newQuantity == null || newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        this.quantity = newQuantity;
    }
    
    /**
     * Update unit cost and recalculate total
     */
    public void updateUnitCost(BigDecimal newUnitCost) {
        if (newUnitCost == null || newUnitCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit cost cannot be negative");
        }
        this.unitCost = newUnitCost;
    }
    
    /**
     * Check if material has cost information
     */
    public boolean hasCostInformation() {
        return unitCost != null && unitCost.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Get formatted quantity with unit
     */
    public String getFormattedQuantity() {
        if (unitOfMeasure != null && !unitOfMeasure.trim().isEmpty()) {
            return quantity + " " + unitOfMeasure;
        }
        return quantity.toString();
    }
    
    /**
     * Get formatted material description
     */
    public String getFormattedDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(materialName);
        if (materialCode != null && !materialCode.trim().isEmpty()) {
            sb.append(" (").append(materialCode).append(")");
        }
        return sb.toString();
    }
    
    /**
     * Check if this is a bulk item (quantity > 10)
     */
    public boolean isBulkItem() {
        return quantity != null && quantity.compareTo(new BigDecimal("10")) > 0;
    }
    
    /**
     * Apply discount to unit cost
     */
    public void applyDiscount(BigDecimal discountPercentage) {
        if (unitCost == null || discountPercentage == null) {
            return;
        }
        if (discountPercentage.compareTo(BigDecimal.ZERO) < 0 || 
            discountPercentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }
        
        BigDecimal discountFactor = BigDecimal.ONE.subtract(
            discountPercentage.divide(new BigDecimal("100"), 4, BigDecimal.ROUND_HALF_UP)
        );
        this.unitCost = this.unitCost.multiply(discountFactor).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    // ========== Getters and Setters ==========
    
    public WorkOrder getWorkOrder() {
        return workOrder;
    }
    
    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }
    
    public String getMaterialName() {
        return materialName;
    }
    
    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }
    
    public String getMaterialCode() {
        return materialCode;
    }
    
    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }
    
    public BigDecimal getQuantity() {
        return quantity;
    }
    
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
    
    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }
    
    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
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
    
    public String getSupplier() {
        return supplier;
    }
    
    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    // ========== toString, equals, hashCode ==========
    
    @Override
    public String toString() {
        return String.format("WorkOrderMaterial[id=%s, workOrder=%s, material=%s, quantity=%s, unitCost=%s]",
            getId(), 
            workOrder != null ? workOrder.getWorkOrderNumber() : "null",
            materialName, 
            quantity, 
            unitCost);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkOrderMaterial)) return false;
        if (!super.equals(o)) return false;
        WorkOrderMaterial that = (WorkOrderMaterial) o;
        return Objects.equals(workOrder, that.workOrder) &&
               Objects.equals(materialName, that.materialName) &&
               Objects.equals(materialCode, that.materialCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), workOrder, materialName, materialCode);
    }
}