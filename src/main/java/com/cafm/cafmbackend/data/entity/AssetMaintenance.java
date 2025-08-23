package com.cafm.cafmbackend.data.entity;

import com.cafm.cafmbackend.data.entity.base.BaseEntity;
import com.cafm.cafmbackend.data.enums.AssetCondition;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Asset Maintenance entity for tracking maintenance history of assets.
 * Records all maintenance activities, costs, and outcomes.
 */
@Entity
@Table(name = "asset_maintenance")
@NamedQueries({
    @NamedQuery(
        name = "AssetMaintenance.findByAsset",
        query = "SELECT am FROM AssetMaintenance am WHERE am.asset.id = :assetId ORDER BY am.maintenanceDate DESC"
    ),
    @NamedQuery(
        name = "AssetMaintenance.findByWorkOrder",
        query = "SELECT am FROM AssetMaintenance am WHERE am.workOrder.id = :workOrderId"
    ),
    @NamedQuery(
        name = "AssetMaintenance.findByDateRange",
        query = "SELECT am FROM AssetMaintenance am WHERE am.maintenanceDate BETWEEN :startDate AND :endDate ORDER BY am.maintenanceDate"
    )
})
public class AssetMaintenance extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    @NotNull(message = "Asset is required")
    private Asset asset;
    
    @Column(name = "maintenance_date", nullable = false)
    @NotNull(message = "Maintenance date is required")
    @PastOrPresent(message = "Maintenance date cannot be in the future")
    private LocalDate maintenanceDate;
    
    @Column(name = "maintenance_type", length = 50)
    @Size(max = 50, message = "Maintenance type cannot exceed 50 characters")
    private String maintenanceType; // 'preventive', 'corrective', 'inspection', 'calibration'
    
    // ========== Work Performed ==========
    
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Description is required")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;
    
    // ========== Cost ==========
    
    @Column(name = "labor_hours", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Labor hours cannot be negative")
    private BigDecimal laborHours;
    
    @Column(name = "labor_cost", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Labor cost cannot be negative")
    private BigDecimal laborCost;
    
    @Column(name = "parts_cost", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Parts cost cannot be negative")
    private BigDecimal partsCost;
    
    @Column(name = "external_cost", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "External cost cannot be negative")
    private BigDecimal externalCost;
    
    @Column(name = "total_cost", precision = 10, scale = 2, insertable = false, updatable = false)
    private BigDecimal totalCost;
    
    // ========== Results ==========
    
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_after", length = 30)
    private AssetCondition conditionAfter;
    
    @Column(name = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;
    
    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;
    
    // ========== Audit ==========
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private User createdByUser;
    
    // ========== Constructors ==========
    
    public AssetMaintenance() {
        super();
        this.maintenanceDate = LocalDate.now();
    }
    
    public AssetMaintenance(Asset asset, String description, String maintenanceType) {
        this();
        this.asset = asset;
        this.description = description;
        this.maintenanceType = maintenanceType;
    }
    
    public AssetMaintenance(Asset asset, String description, String maintenanceType, User performedBy) {
        this(asset, description, maintenanceType);
        this.performedBy = performedBy;
    }
    
    // ========== Business Methods ==========
    
    /**
     * Calculate total cost
     */
    public BigDecimal calculateTotalCost() {
        BigDecimal total = BigDecimal.ZERO;
        if (laborCost != null) total = total.add(laborCost);
        if (partsCost != null) total = total.add(partsCost);
        if (externalCost != null) total = total.add(externalCost);
        return total;
    }
    
    /**
     * Calculate labor cost from hours and rate
     */
    public void calculateLaborCost(BigDecimal hourlyRate) {
        if (laborHours != null && hourlyRate != null) {
            this.laborCost = laborHours.multiply(hourlyRate).setScale(2, BigDecimal.ROUND_HALF_UP);
        }
    }
    
    /**
     * Check if this is preventive maintenance
     */
    public boolean isPreventive() {
        return "preventive".equalsIgnoreCase(maintenanceType);
    }
    
    /**
     * Check if this is corrective maintenance
     */
    public boolean isCorrective() {
        return "corrective".equalsIgnoreCase(maintenanceType);
    }
    
    /**
     * Check if this is inspection
     */
    public boolean isInspection() {
        return "inspection".equalsIgnoreCase(maintenanceType);
    }
    
    /**
     * Check if this is calibration
     */
    public boolean isCalibration() {
        return "calibration".equalsIgnoreCase(maintenanceType);
    }
    
    /**
     * Check if maintenance improved condition
     */
    public boolean improvedCondition() {
        if (conditionAfter == null || asset == null || asset.getCondition() == null) {
            return false;
        }
        return conditionAfter.getConditionScore() > asset.getCondition().getConditionScore();
    }
    
    /**
     * Get maintenance type display name
     */
    public String getMaintenanceTypeDisplayName() {
        if (maintenanceType == null) return "Unknown";
        
        switch (maintenanceType.toLowerCase()) {
            case "preventive":
                return "Preventive Maintenance";
            case "corrective":
                return "Corrective Maintenance";
            case "inspection":
                return "Inspection";
            case "calibration":
                return "Calibration";
            default:
                return maintenanceType.substring(0, 1).toUpperCase() + maintenanceType.substring(1);
        }
    }
    
    /**
     * Get maintenance effectiveness rating
     */
    public String getEffectivenessRating() {
        if (conditionAfter == null) {
            return "Not Assessed";
        }
        
        switch (conditionAfter) {
            case EXCELLENT:
            case GOOD:
                return "Highly Effective";
            case FAIR:
                return "Moderately Effective";
            case POOR:
                return "Minimally Effective";
            case UNUSABLE:
                return "Ineffective";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Calculate cost effectiveness (cost per condition point improvement)
     */
    public BigDecimal calculateCostEffectiveness() {
        if (conditionAfter == null || asset == null || asset.getCondition() == null) {
            return null;
        }
        
        int improvement = conditionAfter.getConditionScore() - asset.getCondition().getConditionScore();
        if (improvement <= 0) {
            return null; // No improvement or condition worsened
        }
        
        BigDecimal cost = getTotalCost();
        if (cost == null || cost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO; // Free maintenance
        }
        
        return cost.divide(new BigDecimal(improvement), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Get days since maintenance
     */
    public long getDaysSinceMaintenance() {
        if (maintenanceDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(maintenanceDate, LocalDate.now());
    }
    
    /**
     * Check if follow-up is needed
     */
    public boolean needsFollowUp() {
        return recommendations != null && 
               (recommendations.toLowerCase().contains("follow") || 
                recommendations.toLowerCase().contains("urgent") ||
                recommendations.toLowerCase().contains("required"));
    }
    
    /**
     * Apply maintenance results to asset
     */
    public void applyToAsset() {
        if (asset == null) return;
        
        asset.setLastMaintenanceDate(this.maintenanceDate);
        
        if (conditionAfter != null) {
            asset.setCondition(conditionAfter);
        }
        
        if (nextMaintenanceDate != null) {
            asset.setNextMaintenanceDate(nextMaintenanceDate);
        }
        
        BigDecimal cost = getTotalCost();
        if (cost != null) {
            BigDecimal currentTotal = asset.getTotalMaintenanceCost();
            if (currentTotal == null) currentTotal = BigDecimal.ZERO;
            asset.setTotalMaintenanceCost(currentTotal.add(cost));
        }
    }
    
    /**
     * Generate summary text
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMaintenanceTypeDisplayName());
        sb.append(" performed on ").append(maintenanceDate);
        
        if (performedBy != null) {
            sb.append(" by ").append(performedBy.getFullName());
        }
        
        if (workOrder != null) {
            sb.append(" (WO#").append(workOrder.getWorkOrderNumber()).append(")");
        }
        
        BigDecimal cost = getTotalCost();
        if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(" - Cost: $").append(cost);
        }
        
        if (conditionAfter != null) {
            sb.append(" - Condition: ").append(conditionAfter.getDisplayName());
        }
        
        return sb.toString();
    }
    
    // ========== Getters and Setters ==========
    
    public Asset getAsset() {
        return asset;
    }
    
    public void setAsset(Asset asset) {
        this.asset = asset;
    }
    
    public LocalDate getMaintenanceDate() {
        return maintenanceDate;
    }
    
    public void setMaintenanceDate(LocalDate maintenanceDate) {
        this.maintenanceDate = maintenanceDate;
    }
    
    public String getMaintenanceType() {
        return maintenanceType;
    }
    
    public void setMaintenanceType(String maintenanceType) {
        this.maintenanceType = maintenanceType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public User getPerformedBy() {
        return performedBy;
    }
    
    public void setPerformedBy(User performedBy) {
        this.performedBy = performedBy;
    }
    
    public WorkOrder getWorkOrder() {
        return workOrder;
    }
    
    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }
    
    public BigDecimal getLaborHours() {
        return laborHours;
    }
    
    public void setLaborHours(BigDecimal laborHours) {
        this.laborHours = laborHours;
    }
    
    public BigDecimal getLaborCost() {
        return laborCost;
    }
    
    public void setLaborCost(BigDecimal laborCost) {
        this.laborCost = laborCost;
    }
    
    public BigDecimal getPartsCost() {
        return partsCost;
    }
    
    public void setPartsCost(BigDecimal partsCost) {
        this.partsCost = partsCost;
    }
    
    public BigDecimal getExternalCost() {
        return externalCost;
    }
    
    public void setExternalCost(BigDecimal externalCost) {
        this.externalCost = externalCost;
    }
    
    public BigDecimal getTotalCost() {
        if (totalCost == null) {
            return calculateTotalCost();
        }
        return totalCost;
    }
    
    public AssetCondition getConditionAfter() {
        return conditionAfter;
    }
    
    public void setConditionAfter(AssetCondition conditionAfter) {
        this.conditionAfter = conditionAfter;
    }
    
    public LocalDate getNextMaintenanceDate() {
        return nextMaintenanceDate;
    }
    
    public void setNextMaintenanceDate(LocalDate nextMaintenanceDate) {
        this.nextMaintenanceDate = nextMaintenanceDate;
    }
    
    public String getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }
    
    public User getCreatedByUser() {
        return createdByUser;
    }
    
    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }
    
    // ========== toString, equals, hashCode ==========
    
    @Override
    public String toString() {
        return String.format("AssetMaintenance[id=%s, asset=%s, date=%s, type=%s, cost=%s]",
            getId(), 
            asset != null ? asset.getAssetCode() : "null",
            maintenanceDate, 
            maintenanceType,
            getTotalCost());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssetMaintenance)) return false;
        if (!super.equals(o)) return false;
        AssetMaintenance that = (AssetMaintenance) o;
        return Objects.equals(asset, that.asset) &&
               Objects.equals(maintenanceDate, that.maintenanceDate) &&
               Objects.equals(maintenanceType, that.maintenanceType);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), asset, maintenanceDate, maintenanceType);
    }
}