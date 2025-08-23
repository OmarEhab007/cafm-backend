package com.cafm.cafmbackend.dto.asset;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Asset statistics response DTO.
 * 
 * Purpose: Provides comprehensive asset metrics and analytics
 * Pattern: Aggregated data response with calculated statistics and financial insights
 * Java 23: Record with computed metrics for dashboard displays
 * Architecture: Multi-tenant aware asset analytics with lifecycle tracking
 */
@Schema(description = "Asset statistics and analytics")
public record AssetStatsResponse(
    // Overall Counts
    @Schema(description = "Total number of assets", example = "500")
    Long totalAssets,
    
    @Schema(description = "Active assets count", example = "450")
    Long activeAssets,
    
    @Schema(description = "Reserved assets count", example = "30")
    Long reservedAssets,
    
    @Schema(description = "Assets in maintenance", example = "15")
    Long maintenanceAssets,
    
    @Schema(description = "Retired assets count", example = "4")
    Long retiredAssets,
    
    @Schema(description = "Disposed assets count", example = "1")
    Long disposedAssets,
    
    // By Condition
    @Schema(description = "Assets in excellent condition", example = "100")
    Long excellentConditionAssets,
    
    @Schema(description = "Assets in good condition", example = "250")
    Long goodConditionAssets,
    
    @Schema(description = "Assets in fair condition", example = "120")
    Long fairConditionAssets,
    
    @Schema(description = "Assets in poor condition", example = "25")
    Long poorConditionAssets,
    
    @Schema(description = "Assets in critical condition", example = "5")
    Long criticalConditionAssets,
    
    // Financial Metrics
    @Schema(description = "Total purchase value", example = "750000.00")
    BigDecimal totalPurchaseValue,
    
    @Schema(description = "Total current value", example = "600000.00")
    BigDecimal totalCurrentValue,
    
    @Schema(description = "Total depreciation amount", example = "150000.00")
    BigDecimal totalDepreciation,
    
    @Schema(description = "Average asset cost", example = "1500.00")
    BigDecimal averageAssetCost,
    
    @Schema(description = "Total maintenance cost", example = "125000.00")
    BigDecimal totalMaintenanceCost,
    
    @Schema(description = "Average maintenance cost per asset", example = "250.00")
    BigDecimal averageMaintenanceCost,
    
    // Maintenance Metrics
    @Schema(description = "Assets due for maintenance", example = "35")
    Long assetsDueForMaintenance,
    
    @Schema(description = "Overdue maintenance assets", example = "12")
    Long overdueMaintenanceAssets,
    
    @Schema(description = "Assets with no maintenance history", example = "45")
    Long assetsWithoutMaintenanceHistory,
    
    @Schema(description = "Average maintenance frequency days", example = "90")
    Double averageMaintenanceFrequency,
    
    // Warranty Metrics
    @Schema(description = "Assets under warranty", example = "180")
    Long assetsUnderWarranty,
    
    @Schema(description = "Assets with expired warranty", example = "320")
    Long assetsWithExpiredWarranty,
    
    @Schema(description = "Assets without warranty info", example = "0")
    Long assetsWithoutWarrantyInfo,
    
    @Schema(description = "Warranties expiring in 30 days", example = "25")
    Long warrantiesExpiringSoon,
    
    // Assignment Metrics
    @Schema(description = "Assigned assets count", example = "380")
    Long assignedAssets,
    
    @Schema(description = "Unassigned assets count", example = "120")
    Long unassignedAssets,
    
    @Schema(description = "Asset utilization percentage", example = "76.0")
    BigDecimal utilizationPercentage,
    
    // Age Analysis
    @Schema(description = "Average age in years", example = "3.2")
    Double averageAgeYears,
    
    @Schema(description = "Assets less than 1 year old", example = "80")
    Long newAssets,
    
    @Schema(description = "Assets 1-3 years old", example = "200")
    Long recentAssets,
    
    @Schema(description = "Assets 3-5 years old", example = "150")
    Long matureAssets,
    
    @Schema(description = "Assets over 5 years old", example = "70")
    Long oldAssets,
    
    // Category Breakdown
    @Schema(description = "Assets by category breakdown")
    Map<String, Long> assetsByCategory,
    
    @Schema(description = "Value by category breakdown")
    Map<String, BigDecimal> valueByCategory,
    
    // Location Breakdown
    @Schema(description = "Assets by school breakdown")
    Map<String, Long> assetsBySchool,
    
    @Schema(description = "Assets by department breakdown")
    Map<String, Long> assetsByDepartment,
    
    // Purchase Trends
    @Schema(description = "Assets purchased this year", example = "120")
    Long assetsPurchasedThisYear,
    
    @Schema(description = "Assets purchased last year", example = "180")
    Long assetsPurchasedLastYear,
    
    @Schema(description = "Purchase trend percentage", example = "-33.3")
    BigDecimal purchaseTrendPercentage,
    
    @Schema(description = "Value purchased this year", example = "180000.00")
    BigDecimal valuePurchasedThisYear,
    
    // Disposal Metrics
    @Schema(description = "Assets disposed this year", example = "15")
    Long assetsDisposedThisYear,
    
    @Schema(description = "Disposal value this year", example = "5000.00")
    BigDecimal disposalValueThisYear,
    
    // Risk Indicators
    @Schema(description = "High risk assets (poor/critical condition)", example = "30")
    Long highRiskAssets,
    
    @Schema(description = "Assets needing immediate attention", example = "25")
    Long assetsNeedingAttention,
    
    @Schema(description = "Compliance issues count", example = "8")
    Long complianceIssues,
    
    // Performance Metrics
    @Schema(description = "Overall asset health score (0-100)", example = "78")
    Integer overallHealthScore,
    
    @Schema(description = "Maintenance efficiency score (0-100)", example = "85")
    Integer maintenanceEfficiencyScore,
    
    @Schema(description = "Financial performance score (0-100)", example = "72")
    Integer financialPerformanceScore,
    
    // Trends
    @Schema(description = "Asset count trend vs previous period", example = "5.2")
    BigDecimal assetCountTrend,
    
    @Schema(description = "Asset value trend vs previous period", example = "-2.8")
    BigDecimal assetValueTrend,
    
    @Schema(description = "Overall trend direction", example = "stable")
    String overallTrend,
    
    // Report Date
    @Schema(description = "Report generation date")
    LocalDate reportDate
) {
    
    /**
     * Calculate asset utilization rate.
     */
    public BigDecimal getAssetUtilizationRate() {
        if (totalAssets == null || totalAssets == 0) {
            return BigDecimal.ZERO;
        }
        
        long inUse = (assignedAssets != null ? assignedAssets : 0) + 
                    (reservedAssets != null ? reservedAssets : 0);
        
        return BigDecimal.valueOf(inUse)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalAssets), 1, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Calculate depreciation rate.
     */
    public BigDecimal getDepreciationRate() {
        if (totalPurchaseValue == null || totalPurchaseValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal depreciation = totalDepreciation != null ? totalDepreciation : BigDecimal.ZERO;
        return depreciation.multiply(BigDecimal.valueOf(100))
                         .divide(totalPurchaseValue, 1, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Calculate maintenance coverage percentage.
     */
    public BigDecimal getMaintenanceCoveragePercentage() {
        if (totalAssets == null || totalAssets == 0) {
            return BigDecimal.ZERO;
        }
        
        long withHistory = totalAssets - (assetsWithoutMaintenanceHistory != null ? assetsWithoutMaintenanceHistory : 0);
        return BigDecimal.valueOf(withHistory)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalAssets), 1, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Calculate warranty coverage percentage.
     */
    public BigDecimal getWarrantyCoveragePercentage() {
        if (totalAssets == null || totalAssets == 0) {
            return BigDecimal.ZERO;
        }
        
        long underWarranty = assetsUnderWarranty != null ? assetsUnderWarranty : 0;
        return BigDecimal.valueOf(underWarranty)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalAssets), 1, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Get the most valuable category.
     */
    public String getMostValuableCategory() {
        if (valueByCategory == null || valueByCategory.isEmpty()) {
            return "No categories";
        }
        
        return valueByCategory.entrySet().stream()
                             .max(Map.Entry.comparingByValue())
                             .map(Map.Entry::getKey)
                             .orElse("Unknown");
    }
    
    /**
     * Get the category with most assets.
     */
    public String getLargestCategory() {
        if (assetsByCategory == null || assetsByCategory.isEmpty()) {
            return "No categories";
        }
        
        return assetsByCategory.entrySet().stream()
                              .max(Map.Entry.comparingByValue())
                              .map(Map.Entry::getKey)
                              .orElse("Unknown");
    }
    
    /**
     * Get overall risk level.
     */
    public String getOverallRiskLevel() {
        if (totalAssets == null || totalAssets == 0) {
            return "UNKNOWN";
        }
        
        long riskAssets = (highRiskAssets != null ? highRiskAssets : 0) +
                         (overdueMaintenanceAssets != null ? overdueMaintenanceAssets : 0);
        
        double riskPercentage = (riskAssets * 100.0) / totalAssets;
        
        if (riskPercentage >= 25) return "HIGH";
        if (riskPercentage >= 15) return "MEDIUM";
        if (riskPercentage >= 5) return "LOW";
        return "MINIMAL";
    }
    
    /**
     * Check if maintenance is effective.
     */
    public boolean isMaintenanceEffective() {
        if (maintenanceEfficiencyScore == null) {
            return false;
        }
        return maintenanceEfficiencyScore >= 75;
    }
    
    /**
     * Check if financial performance is good.
     */
    public boolean isFinancialPerformanceGood() {
        if (financialPerformanceScore == null) {
            return false;
        }
        return financialPerformanceScore >= 70;
    }
    
    /**
     * Get priority action items based on statistics.
     */
    public java.util.List<String> getPriorityActions() {
        java.util.List<String> actions = new java.util.ArrayList<>();
        
        if (criticalConditionAssets != null && criticalConditionAssets > 0) {
            actions.add("Address " + criticalConditionAssets + " assets in critical condition");
        }
        
        if (overdueMaintenanceAssets != null && overdueMaintenanceAssets > 0) {
            actions.add("Complete overdue maintenance for " + overdueMaintenanceAssets + " assets");
        }
        
        if (warrantiesExpiringSoon != null && warrantiesExpiringSoon > 0) {
            actions.add("Review " + warrantiesExpiringSoon + " warranties expiring soon");
        }
        
        if (unassignedAssets != null && unassignedAssets > totalAssets * 0.3) {
            actions.add("Assign " + unassignedAssets + " unassigned assets");
        }
        
        if (complianceIssues != null && complianceIssues > 0) {
            actions.add("Resolve " + complianceIssues + " compliance issues");
        }
        
        return actions;
    }
    
    /**
     * Get fleet age distribution summary.
     */
    public String getFleetAgeDistribution() {
        if (totalAssets == null || totalAssets == 0) {
            return "No assets";
        }
        
        long newCount = newAssets != null ? newAssets : 0;
        long recentCount = recentAssets != null ? recentAssets : 0;
        long matureCount = matureAssets != null ? matureAssets : 0;
        long oldCount = oldAssets != null ? oldAssets : 0;
        
        double newPercent = (newCount * 100.0) / totalAssets;
        double oldPercent = (oldCount * 100.0) / totalAssets;
        
        if (newPercent > 40) return "YOUNG_FLEET";
        if (oldPercent > 40) return "AGING_FLEET";
        if (matureCount > totalAssets * 0.5) return "MATURE_FLEET";
        return "MIXED_FLEET";
    }
}