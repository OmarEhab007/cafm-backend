package com.cafm.cafmbackend.data.entity;

import com.cafm.cafmbackend.data.entity.base.TenantAwareEntity;
import com.cafm.cafmbackend.data.enums.AssetStatus;
import com.cafm.cafmbackend.data.enums.AssetCondition;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Asset entity for tracking company assets and equipment.
 * 
 * SECURITY ENHANCEMENT:
 * - Purpose: Now extends TenantAwareEntity for critical asset isolation
 * - Pattern: Multi-tenant security via inherited company management
 * - Java 23: Modern JPA lifecycle callbacks for tenant validation
 * - Architecture: Tenant-aware asset management entity
 * - Standards: NO Lombok, automatic tenant assignment at entity level
 */
@Entity
@Table(name = "assets")
@NamedQueries({
    @NamedQuery(
        name = "Asset.findByCompanyAndStatus",
        query = "SELECT a FROM Asset a WHERE a.company.id = :companyId AND a.status = :status AND a.isActive = true"
    ),
    @NamedQuery(
        name = "Asset.findMaintenanceDue",
        query = "SELECT a FROM Asset a WHERE a.company.id = :companyId AND a.nextMaintenanceDate <= :dueDate AND a.status = 'ACTIVE'"
    ),
    @NamedQuery(
        name = "Asset.findBySchool",
        query = "SELECT a FROM Asset a WHERE a.school.id = :schoolId AND a.isActive = true"
    )
})
public class Asset extends TenantAwareEntity {
    private Company company;
    
    @Column(name = "asset_code", nullable = false, length = 50)
    @NotBlank(message = "Asset code is required")
    @Size(max = 50, message = "Asset code cannot exceed 50 characters")
    private String assetCode;
    
    @Column(name = "name", nullable = false, length = 255)
    @NotBlank(message = "Asset name is required")
    @Size(max = 255, message = "Asset name cannot exceed 255 characters")
    private String name;
    
    @Column(name = "name_ar", length = 255)
    @Size(max = 255, message = "Arabic name cannot exceed 255 characters")
    private String nameAr;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private AssetCategory category;
    
    // ========== Asset Details ==========
    
    @Column(name = "manufacturer", length = 100)
    @Size(max = 100, message = "Manufacturer cannot exceed 100 characters")
    private String manufacturer;
    
    @Column(name = "model", length = 100)
    @Size(max = 100, message = "Model cannot exceed 100 characters")
    private String model;
    
    @Column(name = "serial_number", length = 100)
    @Size(max = 100, message = "Serial number cannot exceed 100 characters")
    private String serialNumber;
    
    @Column(name = "barcode", length = 100)
    @Size(max = 100, message = "Barcode cannot exceed 100 characters")
    private String barcode;
    
    // ========== Purchase Information ==========
    
    @Column(name = "purchase_date")
    private LocalDate purchaseDate;
    
    @Column(name = "purchase_order_number", length = 50)
    @Size(max = 50, message = "Purchase order number cannot exceed 50 characters")
    private String purchaseOrderNumber;
    
    @Column(name = "supplier", length = 255)
    @Size(max = 255, message = "Supplier cannot exceed 255 characters")
    private String supplier;
    
    @Column(name = "warranty_start_date")
    private LocalDate warrantyStartDate;
    
    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;
    
    // ========== Financial ==========
    
    @Column(name = "purchase_cost", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Purchase cost cannot be negative")
    private BigDecimal purchaseCost;
    
    @Column(name = "current_value", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Current value cannot be negative")
    private BigDecimal currentValue;
    
    @Column(name = "salvage_value", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Salvage value cannot be negative")
    private BigDecimal salvageValue;
    
    @Column(name = "depreciation_method", length = 30)
    @Size(max = 30, message = "Depreciation method cannot exceed 30 characters")
    private String depreciationMethod = "straight_line";
    
    // ========== Location & Assignment ==========
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;
    
    @Column(name = "department", length = 100)
    @Size(max = 100, message = "Department cannot exceed 100 characters")
    private String department;
    
    @Column(name = "location", length = 255)
    @Size(max = 255, message = "Location cannot exceed 255 characters")
    private String location;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;
    
    @Column(name = "assignment_date")
    private LocalDate assignmentDate;
    
    // ========== Maintenance ==========
    
    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;
    
    @Column(name = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;
    
    @Column(name = "maintenance_frequency_days")
    @Min(value = 1, message = "Maintenance frequency must be at least 1 day")
    private Integer maintenanceFrequencyDays;
    
    @Column(name = "total_maintenance_cost", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Total maintenance cost cannot be negative")
    private BigDecimal totalMaintenanceCost = BigDecimal.ZERO;
    
    // ========== Status ==========
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AssetStatus status = AssetStatus.ACTIVE;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "condition")
    private AssetCondition condition = AssetCondition.GOOD;
    
    // ========== Disposal ==========
    
    @Column(name = "disposal_date")
    private LocalDate disposalDate;
    
    @Column(name = "disposal_method", length = 50)
    @Size(max = 50, message = "Disposal method cannot exceed 50 characters")
    private String disposalMethod;
    
    @Column(name = "disposal_value", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Disposal value cannot be negative")
    private BigDecimal disposalValue;
    
    @Column(name = "disposal_reason", columnDefinition = "TEXT")
    private String disposalReason;
    
    // ========== Metadata ==========
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // ========== Relationships ==========
    
    @OneToMany(mappedBy = "asset", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<AssetMaintenance> maintenanceHistory = new HashSet<>();
    
    // ========== Constructors ==========
    
    public Asset() {
        super();
    }
    
    public Asset(Company company, String assetCode, String name) {
        this();
        this.company = company;
        this.assetCode = assetCode;
        this.name = name;
    }
    
    public Asset(Company company, String assetCode, String name, BigDecimal purchaseCost) {
        this(company, assetCode, name);
        this.purchaseCost = purchaseCost;
        this.currentValue = purchaseCost;
    }
    
    // ========== Business Methods ==========
    
    /**
     * Calculate age in years
     */
    public double getAgeInYears() {
        if (purchaseDate == null) return 0;
        return ChronoUnit.DAYS.between(purchaseDate, LocalDate.now()) / 365.25;
    }
    
    /**
     * Calculate age in months
     */
    public long getAgeInMonths() {
        if (purchaseDate == null) return 0;
        return ChronoUnit.MONTHS.between(purchaseDate, LocalDate.now());
    }
    
    /**
     * Check if under warranty
     */
    public boolean isUnderWarranty() {
        if (warrantyEndDate == null) return false;
        return LocalDate.now().isBefore(warrantyEndDate);
    }
    
    /**
     * Get warranty days remaining
     */
    public long getWarrantyDaysRemaining() {
        if (!isUnderWarranty()) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), warrantyEndDate);
    }
    
    /**
     * Check if maintenance is due
     */
    public boolean isMaintenanceDue() {
        if (nextMaintenanceDate == null) return false;
        return !LocalDate.now().isBefore(nextMaintenanceDate);
    }
    
    /**
     * Get days until next maintenance
     */
    public long getDaysUntilMaintenance() {
        if (nextMaintenanceDate == null) return -1;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), nextMaintenanceDate);
        return Math.max(0, days);
    }
    
    /**
     * Calculate depreciation
     */
    public BigDecimal calculateDepreciation() {
        if (purchaseCost == null || purchaseDate == null) {
            return BigDecimal.ZERO;
        }
        
        if (category != null && category.hasValidDepreciationSettings()) {
            return category.calculateAnnualDepreciation(purchaseCost, salvageValue);
        }
        
        // Default straight-line depreciation over 5 years
        BigDecimal depreciableAmount = purchaseCost.subtract(
            salvageValue != null ? salvageValue : BigDecimal.ZERO
        );
        return depreciableAmount.divide(new BigDecimal("5"), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Update current value based on depreciation
     */
    public void updateCurrentValue() {
        if (purchaseCost == null || purchaseDate == null) {
            return;
        }
        
        double ageInYears = getAgeInYears();
        BigDecimal annualDepreciation = calculateDepreciation();
        BigDecimal totalDepreciation = annualDepreciation.multiply(
            new BigDecimal(ageInYears).setScale(2, BigDecimal.ROUND_HALF_UP)
        );
        
        BigDecimal newValue = purchaseCost.subtract(totalDepreciation);
        BigDecimal minValue = salvageValue != null ? salvageValue : BigDecimal.ZERO;
        
        this.currentValue = newValue.max(minValue);
    }
    
    /**
     * Schedule next maintenance
     */
    public void scheduleNextMaintenance() {
        if (maintenanceFrequencyDays != null && maintenanceFrequencyDays > 0) {
            LocalDate baseDate = lastMaintenanceDate != null ? lastMaintenanceDate : LocalDate.now();
            this.nextMaintenanceDate = baseDate.plusDays(maintenanceFrequencyDays);
        }
    }
    
    /**
     * Complete maintenance
     */
    public void completeMaintenance(BigDecimal cost, AssetCondition newCondition) {
        this.lastMaintenanceDate = LocalDate.now();
        this.condition = newCondition;
        if (cost != null) {
            this.totalMaintenanceCost = this.totalMaintenanceCost.add(cost);
        }
        scheduleNextMaintenance();
    }
    
    /**
     * Assign to user
     */
    public void assignTo(User user) {
        this.assignedTo = user;
        this.assignmentDate = LocalDate.now();
    }
    
    /**
     * Retire asset
     */
    public void retire(String reason) {
        this.status = AssetStatus.RETIRED;
        this.disposalReason = reason;
        this.assignedTo = null;
    }
    
    /**
     * Dispose asset
     */
    public void dispose(String method, BigDecimal value, String reason) {
        this.status = AssetStatus.DISPOSED;
        this.disposalDate = LocalDate.now();
        this.disposalMethod = method;
        this.disposalValue = value;
        this.disposalReason = reason;
        this.assignedTo = null;
        this.isActive = false;
    }
    
    /**
     * Calculate total cost of ownership
     */
    public BigDecimal calculateTotalCostOfOwnership() {
        BigDecimal tco = purchaseCost != null ? purchaseCost : BigDecimal.ZERO;
        if (totalMaintenanceCost != null) {
            tco = tco.add(totalMaintenanceCost);
        }
        return tco;
    }
    
    /**
     * Calculate ROI if asset generates revenue
     */
    public BigDecimal calculateROI(BigDecimal revenue) {
        if (revenue == null || purchaseCost == null || purchaseCost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal tco = calculateTotalCostOfOwnership();
        BigDecimal profit = revenue.subtract(tco);
        return profit.divide(tco, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100"));
    }
    
    /**
     * Get maintenance cost per year
     */
    public BigDecimal getAnnualMaintenanceCost() {
        double years = getAgeInYears();
        if (years <= 0 || totalMaintenanceCost == null) {
            return BigDecimal.ZERO;
        }
        return totalMaintenanceCost.divide(
            new BigDecimal(years), 2, BigDecimal.ROUND_HALF_UP
        );
    }
    
    /**
     * Get formatted location
     */
    public String getFormattedLocation() {
        StringBuilder sb = new StringBuilder();
        if (school != null) {
            sb.append(school.getName()).append(" - ");
        }
        if (department != null && !department.trim().isEmpty()) {
            sb.append(department).append(" - ");
        }
        if (location != null && !location.trim().isEmpty()) {
            sb.append(location);
        }
        return sb.length() > 0 ? sb.toString() : "Unassigned";
    }
    
    // ========== Getters and Setters ==========
    
    public Company getCompany() {
        return company;
    }
    
    public void setCompany(Company company) {
        this.company = company;
    }
    
    public String getAssetCode() {
        return assetCode;
    }
    
    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode;
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
    
    public AssetCategory getCategory() {
        return category;
    }
    
    public void setCategory(AssetCategory category) {
        this.category = category;
    }
    
    public String getManufacturer() {
        return manufacturer;
    }
    
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getSerialNumber() {
        return serialNumber;
    }
    
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
    
    public String getBarcode() {
        return barcode;
    }
    
    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
    
    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }
    
    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
    
    public String getPurchaseOrderNumber() {
        return purchaseOrderNumber;
    }
    
    public void setPurchaseOrderNumber(String purchaseOrderNumber) {
        this.purchaseOrderNumber = purchaseOrderNumber;
    }
    
    public String getSupplier() {
        return supplier;
    }
    
    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }
    
    public LocalDate getWarrantyStartDate() {
        return warrantyStartDate;
    }
    
    public void setWarrantyStartDate(LocalDate warrantyStartDate) {
        this.warrantyStartDate = warrantyStartDate;
    }
    
    public LocalDate getWarrantyEndDate() {
        return warrantyEndDate;
    }
    
    public void setWarrantyEndDate(LocalDate warrantyEndDate) {
        this.warrantyEndDate = warrantyEndDate;
    }
    
    public BigDecimal getPurchaseCost() {
        return purchaseCost;
    }
    
    public void setPurchaseCost(BigDecimal purchaseCost) {
        this.purchaseCost = purchaseCost;
    }
    
    public BigDecimal getCurrentValue() {
        return currentValue;
    }
    
    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }
    
    public BigDecimal getSalvageValue() {
        return salvageValue;
    }
    
    public void setSalvageValue(BigDecimal salvageValue) {
        this.salvageValue = salvageValue;
    }
    
    public String getDepreciationMethod() {
        return depreciationMethod;
    }
    
    public void setDepreciationMethod(String depreciationMethod) {
        this.depreciationMethod = depreciationMethod;
    }
    
    public School getSchool() {
        return school;
    }
    
    public void setSchool(School school) {
        this.school = school;
    }
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public User getAssignedTo() {
        return assignedTo;
    }
    
    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }
    
    public LocalDate getAssignmentDate() {
        return assignmentDate;
    }
    
    public void setAssignmentDate(LocalDate assignmentDate) {
        this.assignmentDate = assignmentDate;
    }
    
    public LocalDate getLastMaintenanceDate() {
        return lastMaintenanceDate;
    }
    
    public void setLastMaintenanceDate(LocalDate lastMaintenanceDate) {
        this.lastMaintenanceDate = lastMaintenanceDate;
    }
    
    public LocalDate getNextMaintenanceDate() {
        return nextMaintenanceDate;
    }
    
    public void setNextMaintenanceDate(LocalDate nextMaintenanceDate) {
        this.nextMaintenanceDate = nextMaintenanceDate;
    }
    
    public Integer getMaintenanceFrequencyDays() {
        return maintenanceFrequencyDays;
    }
    
    public void setMaintenanceFrequencyDays(Integer maintenanceFrequencyDays) {
        this.maintenanceFrequencyDays = maintenanceFrequencyDays;
    }
    
    public BigDecimal getTotalMaintenanceCost() {
        return totalMaintenanceCost;
    }
    
    public void setTotalMaintenanceCost(BigDecimal totalMaintenanceCost) {
        this.totalMaintenanceCost = totalMaintenanceCost;
    }
    
    public AssetStatus getStatus() {
        return status;
    }
    
    public void setStatus(AssetStatus status) {
        this.status = status;
    }
    
    public AssetCondition getCondition() {
        return condition;
    }
    
    public void setCondition(AssetCondition condition) {
        this.condition = condition;
    }
    
    public LocalDate getDisposalDate() {
        return disposalDate;
    }
    
    public void setDisposalDate(LocalDate disposalDate) {
        this.disposalDate = disposalDate;
    }
    
    public String getDisposalMethod() {
        return disposalMethod;
    }
    
    public void setDisposalMethod(String disposalMethod) {
        this.disposalMethod = disposalMethod;
    }
    
    public BigDecimal getDisposalValue() {
        return disposalValue;
    }
    
    public void setDisposalValue(BigDecimal disposalValue) {
        this.disposalValue = disposalValue;
    }
    
    public String getDisposalReason() {
        return disposalReason;
    }
    
    public void setDisposalReason(String disposalReason) {
        this.disposalReason = disposalReason;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Set<AssetMaintenance> getMaintenanceHistory() {
        return maintenanceHistory;
    }
    
    public void setMaintenanceHistory(Set<AssetMaintenance> maintenanceHistory) {
        this.maintenanceHistory = maintenanceHistory;
    }
    
    // ========== toString, equals, hashCode ==========
    
    @Override
    public String toString() {
        return String.format("Asset[id=%s, code=%s, name=%s, status=%s, condition=%s]",
            getId(), assetCode, name, status, condition);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Asset)) return false;
        if (!super.equals(o)) return false;
        Asset asset = (Asset) o;
        return Objects.equals(company, asset.company) &&
               Objects.equals(assetCode, asset.assetCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), company, assetCode);
    }
}