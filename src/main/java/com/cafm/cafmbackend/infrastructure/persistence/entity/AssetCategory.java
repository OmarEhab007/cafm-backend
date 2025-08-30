package com.cafm.cafmbackend.infrastructure.persistence.entity;

import com.cafm.cafmbackend.infrastructure.persistence.entity.base.TenantAwareEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Asset Category entity for organizing and categorizing assets.
 * Includes depreciation settings for financial tracking.
 */
@Entity
@Table(name = "asset_categories")
@NamedQueries({
    @NamedQuery(
        name = "AssetCategory.findByCompany",
        query = "SELECT ac FROM AssetCategory ac WHERE ac.company.id = :companyId AND ac.isActive = true ORDER BY ac.name"
    ),
    @NamedQuery(
        name = "AssetCategory.findWithAssets",
        query = "SELECT DISTINCT ac FROM AssetCategory ac LEFT JOIN FETCH ac.assets WHERE ac.company.id = :companyId"
    )
})
public class AssetCategory extends TenantAwareEntity {
    // SECURITY: Asset categorization with tenant isolation
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @NotNull(message = "Company is required")
    private Company company;
    
    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name cannot exceed 100 characters")
    private String name;
    
    @Column(name = "name_ar", length = 100)
    @Size(max = 100, message = "Arabic name cannot exceed 100 characters")
    private String nameAr;
    
    @Column(name = "depreciation_rate", precision = 5, scale = 2)
    @DecimalMin(value = "0.0", message = "Depreciation rate cannot be negative")
    @DecimalMax(value = "100.0", message = "Depreciation rate cannot exceed 100%")
    private BigDecimal depreciationRate = new BigDecimal("10");
    
    @Column(name = "useful_life_years")
    @Min(value = 1, message = "Useful life must be at least 1 year")
    @Max(value = 100, message = "Useful life cannot exceed 100 years")
    private Integer usefulLifeYears;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // ========== Relationships ==========
    
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private Set<Asset> assets = new HashSet<>();
    
    // ========== Constructors ==========
    
    public AssetCategory() {
        super();
    }
    
    public AssetCategory(Company company, String name) {
        this();
        this.company = company;
        this.name = name;
    }
    
    public AssetCategory(Company company, String name, BigDecimal depreciationRate, Integer usefulLifeYears) {
        this(company, name);
        this.depreciationRate = depreciationRate;
        this.usefulLifeYears = usefulLifeYears;
    }
    
    // ========== Business Methods ==========
    
    /**
     * Calculate annual depreciation amount for a given asset value
     */
    public BigDecimal calculateAnnualDepreciation(BigDecimal assetValue, BigDecimal salvageValue) {
        if (assetValue == null || assetValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        if (usefulLifeYears != null && usefulLifeYears > 0) {
            // Straight-line depreciation based on useful life
            BigDecimal depreciableAmount = assetValue.subtract(
                salvageValue != null ? salvageValue : BigDecimal.ZERO
            );
            return depreciableAmount.divide(
                new BigDecimal(usefulLifeYears), 2, BigDecimal.ROUND_HALF_UP
            );
        } else if (depreciationRate != null && depreciationRate.compareTo(BigDecimal.ZERO) > 0) {
            // Percentage-based depreciation
            return assetValue.multiply(depreciationRate)
                .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculate monthly depreciation
     */
    public BigDecimal calculateMonthlyDepreciation(BigDecimal assetValue, BigDecimal salvageValue) {
        BigDecimal annualDepreciation = calculateAnnualDepreciation(assetValue, salvageValue);
        return annualDepreciation.divide(new BigDecimal("12"), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Get total asset count in this category
     */
    public int getAssetCount() {
        return assets != null ? assets.size() : 0;
    }
    
    /**
     * Get active asset count
     */
    public long getActiveAssetCount() {
        if (assets == null) return 0;
        return assets.stream()
            .filter(asset -> asset.getStatus() != null && asset.getStatus().isOperational())
            .count();
    }
    
    /**
     * Calculate total value of assets in this category
     */
    public BigDecimal getTotalAssetValue() {
        if (assets == null || assets.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return assets.stream()
            .map(Asset::getCurrentValue)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculate average asset age in years
     */
    public Double getAverageAssetAge() {
        if (assets == null || assets.isEmpty()) {
            return 0.0;
        }
        
        return assets.stream()
            .mapToDouble(Asset::getAgeInYears)
            .average()
            .orElse(0.0);
    }
    
    /**
     * Check if category has assets
     */
    public boolean hasAssets() {
        return assets != null && !assets.isEmpty();
    }
    
    /**
     * Get depreciation method description
     */
    public String getDepreciationMethod() {
        if (usefulLifeYears != null && usefulLifeYears > 0) {
            return "Straight-line over " + usefulLifeYears + " years";
        } else if (depreciationRate != null && depreciationRate.compareTo(BigDecimal.ZERO) > 0) {
            return "Declining balance at " + depreciationRate + "% per year";
        }
        return "No depreciation";
    }
    
    /**
     * Validate depreciation settings
     */
    public boolean hasValidDepreciationSettings() {
        return (usefulLifeYears != null && usefulLifeYears > 0) ||
               (depreciationRate != null && depreciationRate.compareTo(BigDecimal.ZERO) > 0);
    }
    
    /**
     * Get display name based on locale
     */
    public String getDisplayName(String locale) {
        if ("ar".equals(locale) && nameAr != null && !nameAr.trim().isEmpty()) {
            return nameAr;
        }
        return name;
    }
    
    /**
     * Activate or deactivate category
     */
    public void setActive(boolean active) {
        this.isActive = active;
        if (!active && assets != null) {
            // Optionally handle assets when category is deactivated
            for (Asset asset : assets) {
                // Could trigger notifications or validations
            }
        }
    }
    
    // ========== Getters and Setters ==========
    
    public Company getCompany() {
        return company;
    }
    
    public void setCompany(Company company) {
        this.company = company;
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
    
    public BigDecimal getDepreciationRate() {
        return depreciationRate;
    }
    
    public void setDepreciationRate(BigDecimal depreciationRate) {
        this.depreciationRate = depreciationRate;
    }
    
    public Integer getUsefulLifeYears() {
        return usefulLifeYears;
    }
    
    public void setUsefulLifeYears(Integer usefulLifeYears) {
        this.usefulLifeYears = usefulLifeYears;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Set<Asset> getAssets() {
        return assets;
    }
    
    public void setAssets(Set<Asset> assets) {
        this.assets = assets;
    }
    
    // ========== toString, equals, hashCode ==========
    
    @Override
    public String toString() {
        return String.format("AssetCategory[id=%s, name=%s, depRate=%s%%, lifeYears=%d, active=%s]",
            getId(), name, depreciationRate, usefulLifeYears, isActive);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssetCategory)) return false;
        if (!super.equals(o)) return false;
        AssetCategory that = (AssetCategory) o;
        return Objects.equals(company, that.company) &&
               Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), company, name);
    }
}