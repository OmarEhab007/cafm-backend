package com.cafm.cafmbackend.infrastructure.persistence.entity;

import com.cafm.cafmbackend.infrastructure.persistence.entity.base.TenantAwareEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Inventory Item entity for managing stock items.
 * Central entity for inventory management system.
 */
@Entity
@Table(name = "inventory_items")
@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "InventoryItem.withCompany",
        attributeNodes = @NamedAttributeNode("company")
    ),
    @NamedEntityGraph(
        name = "InventoryItem.withCompanyAndCategory",
        attributeNodes = {
            @NamedAttributeNode("company"),
            @NamedAttributeNode("category")
        }
    )
})
@NamedQueries({
    @NamedQuery(
        name = "InventoryItem.findLowStock",
        query = "SELECT ii FROM InventoryItem ii WHERE ii.company.id = :companyId AND ii.currentStock <= ii.minimumStock AND ii.isActive = true"
    ),
    @NamedQuery(
        name = "InventoryItem.findByCategory",
        query = "SELECT ii FROM InventoryItem ii WHERE ii.company.id = :companyId AND ii.category.id = :categoryId AND ii.isActive = true"
    ),
    @NamedQuery(
        name = "InventoryItem.findReorderRequired",
        query = "SELECT ii FROM InventoryItem ii WHERE ii.company.id = :companyId AND ii.currentStock <= ii.reorderLevel AND ii.isActive = true"
    )
})
public class InventoryItem extends TenantAwareEntity {
    // SECURITY: Tenant isolation enforced via TenantAwareEntity inheritance
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @NotNull(message = "Company is required")
    private Company company;
    
    @Column(name = "item_code", nullable = false, length = 50)
    @NotBlank(message = "Item code is required")
    @Size(max = 50, message = "Item code cannot exceed 50 characters")
    private String itemCode;
    
    @Column(name = "name", nullable = false, length = 255)
    @NotBlank(message = "Item name is required")
    @Size(max = 255, message = "Item name cannot exceed 255 characters")
    private String name;
    
    @Column(name = "name_ar", length = 255)
    @Size(max = 255, message = "Arabic name cannot exceed 255 characters")
    private String nameAr;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private InventoryCategory category;
    
    // ========== Specifications ==========
    
    @Column(name = "brand", length = 100)
    @Size(max = 100, message = "Brand cannot exceed 100 characters")
    private String brand;
    
    @Column(name = "model", length = 100)
    @Size(max = 100, message = "Model cannot exceed 100 characters")
    private String model;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specifications", columnDefinition = "jsonb")
    private Map<String, Object> specifications;
    
    // ========== Units and Measurement ==========
    
    @Column(name = "unit_of_measure", nullable = false, length = 50)
    @NotBlank(message = "Unit of measure is required")
    @Size(max = 50, message = "Unit of measure cannot exceed 50 characters")
    private String unitOfMeasure = "PIECE";
    
    // ========== Stock Levels ==========
    
    @Column(name = "current_stock", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Current stock cannot be negative")
    private BigDecimal currentStock = BigDecimal.ZERO;
    
    @Column(name = "minimum_stock", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Minimum stock cannot be negative")
    private BigDecimal minimumStock = BigDecimal.ZERO;
    
    @Column(name = "maximum_stock", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Maximum stock cannot be negative")
    private BigDecimal maximumStock;
    
    @Column(name = "reorder_level", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Reorder level cannot be negative")
    private BigDecimal reorderLevel;
    
    @Column(name = "reorder_quantity", precision = 10, scale = 2)
    @DecimalMin(value = "0.01", message = "Reorder quantity must be positive")
    private BigDecimal reorderQuantity;
    
    // ========== Pricing ==========
    
    @Column(name = "average_cost", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Average cost cannot be negative")
    private BigDecimal averageCost;
    
    @Column(name = "last_purchase_cost", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Last purchase cost cannot be negative")
    private BigDecimal lastPurchaseCost;
    
    @Column(name = "selling_price", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Selling price cannot be negative")
    private BigDecimal sellingPrice;
    
    // ========== Location ==========
    
    @Column(name = "warehouse_location", length = 100)
    @Size(max = 100, message = "Warehouse location cannot exceed 100 characters")
    private String warehouseLocation;
    
    @Column(name = "bin_number", length = 50)
    @Size(max = 50, message = "Bin number cannot exceed 50 characters")
    private String binNumber;
    
    // ========== Status ==========
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "is_trackable")
    private Boolean isTrackable = true;
    
    // ========== Relationships ==========
    
    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
    private Set<InventoryTransaction> transactions = new HashSet<>();
    
    // ========== Constructors ==========
    
    public InventoryItem() {
        super();
    }
    
    public InventoryItem(Company company, String itemCode, String name) {
        this();
        this.company = company;
        this.itemCode = itemCode;
        this.name = name;
    }
    
    public InventoryItem(Company company, String itemCode, String name, 
                        String unitOfMeasure, BigDecimal minimumStock) {
        this(company, itemCode, name);
        this.unitOfMeasure = unitOfMeasure;
        this.minimumStock = minimumStock;
    }
    
    // ========== Business Methods ==========
    
    /**
     * Check if item is low on stock
     */
    public boolean isLowStock() {
        if (minimumStock == null) return false;
        return currentStock.compareTo(minimumStock) <= 0;
    }
    
    /**
     * Check if item needs reorder
     */
    public boolean needsReorder() {
        if (reorderLevel == null) return isLowStock();
        return currentStock.compareTo(reorderLevel) <= 0;
    }
    
    /**
     * Check if item is out of stock
     */
    public boolean isOutOfStock() {
        return currentStock.compareTo(BigDecimal.ZERO) <= 0;
    }
    
    /**
     * Check if item is overstocked
     */
    public boolean isOverstocked() {
        if (maximumStock == null) return false;
        return currentStock.compareTo(maximumStock) > 0;
    }
    
    /**
     * Calculate stock percentage
     */
    public Integer getStockPercentage() {
        if (maximumStock == null || maximumStock.compareTo(BigDecimal.ZERO) == 0) {
            return currentStock.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
        }
        BigDecimal percentage = currentStock.divide(maximumStock, 2, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal(100));
        return Math.min(percentage.intValue(), 100);
    }
    
    /**
     * Get stock status
     */
    public String getStockStatus() {
        if (isOutOfStock()) return "OUT_OF_STOCK";
        if (isLowStock()) return "LOW_STOCK";
        if (isOverstocked()) return "OVERSTOCKED";
        if (needsReorder()) return "REORDER_NEEDED";
        return "NORMAL";
    }
    
    /**
     * Get stock status color
     */
    public String getStockStatusColor() {
        switch (getStockStatus()) {
            case "OUT_OF_STOCK":
                return "#dc3545"; // Red
            case "LOW_STOCK":
            case "REORDER_NEEDED":
                return "#ffc107"; // Yellow
            case "OVERSTOCKED":
                return "#17a2b8"; // Cyan
            default:
                return "#28a745"; // Green
        }
    }
    
    /**
     * Update stock level
     */
    public void updateStock(BigDecimal quantity, boolean isIncoming) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        if (isIncoming) {
            this.currentStock = this.currentStock.add(quantity);
        } else {
            if (this.currentStock.compareTo(quantity) < 0) {
                throw new IllegalStateException("Insufficient stock. Available: " + currentStock + ", Required: " + quantity);
            }
            this.currentStock = this.currentStock.subtract(quantity);
        }
    }
    
    /**
     * Adjust stock to specific level
     */
    public void adjustStock(BigDecimal newLevel) {
        if (newLevel == null || newLevel.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Stock level cannot be negative");
        }
        this.currentStock = newLevel;
    }
    
    /**
     * Calculate reorder quantity needed
     */
    public BigDecimal calculateReorderQuantity() {
        if (reorderQuantity != null) {
            return reorderQuantity;
        }
        if (maximumStock != null) {
            return maximumStock.subtract(currentStock);
        }
        if (minimumStock != null) {
            return minimumStock.multiply(new BigDecimal("2")).subtract(currentStock);
        }
        return new BigDecimal("10"); // Default reorder quantity
    }
    
    /**
     * Calculate profit margin
     */
    public BigDecimal calculateProfitMargin() {
        if (sellingPrice == null || averageCost == null || 
            averageCost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return sellingPrice.subtract(averageCost)
            .divide(averageCost, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal(100));
    }
    
    /**
     * Get formatted location
     */
    public String getFormattedLocation() {
        StringBuilder sb = new StringBuilder();
        if (warehouseLocation != null && !warehouseLocation.trim().isEmpty()) {
            sb.append(warehouseLocation);
        }
        if (binNumber != null && !binNumber.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append("Bin: ").append(binNumber);
        }
        return sb.length() > 0 ? sb.toString() : "Unassigned";
    }
    
    /**
     * Update average cost based on new purchase
     */
    public void updateAverageCost(BigDecimal newQuantity, BigDecimal newCost) {
        if (newQuantity == null || newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        
        BigDecimal totalQuantity = currentStock.add(newQuantity);
        if (totalQuantity.compareTo(BigDecimal.ZERO) == 0) {
            this.averageCost = newCost;
        } else {
            BigDecimal currentTotal = currentStock.multiply(
                averageCost != null ? averageCost : BigDecimal.ZERO
            );
            BigDecimal newTotal = newQuantity.multiply(newCost);
            this.averageCost = currentTotal.add(newTotal)
                .divide(totalQuantity, 2, BigDecimal.ROUND_HALF_UP);
        }
        this.lastPurchaseCost = newCost;
    }
    
    // ========== Getters and Setters ==========
    
    public Company getCompany() {
        return company;
    }
    
    public void setCompany(Company company) {
        this.company = company;
    }
    
    public String getItemCode() {
        return itemCode;
    }
    
    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getNameAr() {
        return nameAr;
    }
    
    public void setNameAr(String nameAr) {
        this.nameAr = nameAr;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public InventoryCategory getCategory() {
        return category;
    }
    
    public void setCategory(InventoryCategory category) {
        this.category = category;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public Map<String, Object> getSpecifications() {
        return specifications;
    }
    
    public void setSpecifications(Map<String, Object> specifications) {
        this.specifications = specifications;
    }
    
    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }
    
    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }
    
    public BigDecimal getCurrentStock() {
        return currentStock;
    }
    
    public void setCurrentStock(BigDecimal currentStock) {
        this.currentStock = currentStock;
    }
    
    public BigDecimal getMinimumStock() {
        return minimumStock;
    }
    
    public void setMinimumStock(BigDecimal minimumStock) {
        this.minimumStock = minimumStock;
    }
    
    public BigDecimal getMaximumStock() {
        return maximumStock;
    }
    
    public void setMaximumStock(BigDecimal maximumStock) {
        this.maximumStock = maximumStock;
    }
    
    public BigDecimal getReorderLevel() {
        return reorderLevel;
    }
    
    public void setReorderLevel(BigDecimal reorderLevel) {
        this.reorderLevel = reorderLevel;
    }
    
    public BigDecimal getReorderQuantity() {
        return reorderQuantity;
    }
    
    public void setReorderQuantity(BigDecimal reorderQuantity) {
        this.reorderQuantity = reorderQuantity;
    }
    
    public BigDecimal getAverageCost() {
        return averageCost;
    }
    
    public void setAverageCost(BigDecimal averageCost) {
        this.averageCost = averageCost;
    }
    
    public BigDecimal getLastPurchaseCost() {
        return lastPurchaseCost;
    }
    
    public void setLastPurchaseCost(BigDecimal lastPurchaseCost) {
        this.lastPurchaseCost = lastPurchaseCost;
    }
    
    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }
    
    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }
    
    public String getWarehouseLocation() {
        return warehouseLocation;
    }
    
    public void setWarehouseLocation(String warehouseLocation) {
        this.warehouseLocation = warehouseLocation;
    }
    
    public String getBinNumber() {
        return binNumber;
    }
    
    public void setBinNumber(String binNumber) {
        this.binNumber = binNumber;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Boolean getIsTrackable() {
        return isTrackable;
    }
    
    public void setIsTrackable(Boolean isTrackable) {
        this.isTrackable = isTrackable;
    }
    
    public Set<InventoryTransaction> getTransactions() {
        return transactions;
    }
    
    public void setTransactions(Set<InventoryTransaction> transactions) {
        this.transactions = transactions;
    }
    
    // ========== toString, equals, hashCode ==========
    
    @Override
    public String toString() {
        return String.format("InventoryItem[id=%s, code=%s, name=%s, stock=%s, status=%s]",
            getId(), itemCode, name, currentStock, getStockStatus());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryItem)) return false;
        if (!super.equals(o)) return false;
        InventoryItem that = (InventoryItem) o;
        return Objects.equals(company, that.company) &&
               Objects.equals(itemCode, that.itemCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), company, itemCode);
    }
}