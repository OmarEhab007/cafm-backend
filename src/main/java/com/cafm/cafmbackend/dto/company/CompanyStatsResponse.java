package com.cafm.cafmbackend.dto.company;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Company statistics response DTO.
 * 
 * Purpose: Provides comprehensive usage metrics and analytics for companies
 * Pattern: Aggregated data response with calculated metrics and percentages
 * Java 23: Record with computed statistics for dashboard displays
 * Architecture: Multi-tenant aware statistics for resource monitoring
 */
@Schema(description = "Company usage statistics and metrics")
public record CompanyStatsResponse(
    // User Statistics
    @Schema(description = "Total user count", example = "45")
    Long totalUsers,
    
    @Schema(description = "Active user count", example = "42")
    Long activeUsers,
    
    @Schema(description = "Inactive user count", example = "3")
    Long inactiveUsers,
    
    @Schema(description = "User utilization percentage", example = "90")
    Integer userUtilizationPercentage,
    
    // School Statistics
    @Schema(description = "Total school count", example = "8")
    Long totalSchools,
    
    @Schema(description = "Active school count", example = "7")
    Long activeSchools,
    
    @Schema(description = "School utilization percentage", example = "80")
    Integer schoolUtilizationPercentage,
    
    // Maintenance Statistics
    @Schema(description = "Total reports created", example = "150")
    Long totalReports,
    
    @Schema(description = "Pending reports", example = "12")
    Long pendingReports,
    
    @Schema(description = "Completed reports", example = "138")
    Long completedReports,
    
    @Schema(description = "Total work orders", example = "120")
    Long totalWorkOrders,
    
    @Schema(description = "Active work orders", example = "15")
    Long activeWorkOrders,
    
    @Schema(description = "Completed work orders", example = "105")
    Long completedWorkOrders,
    
    // Asset Statistics
    @Schema(description = "Total assets", example = "300")
    Long totalAssets,
    
    @Schema(description = "Assets requiring maintenance", example = "25")
    Long assetsRequiringMaintenance,
    
    @Schema(description = "Assets in good condition", example = "275")
    Long assetsInGoodCondition,
    
    // Storage Statistics
    @Schema(description = "Used storage in GB", example = "75.5")
    BigDecimal usedStorageGb,
    
    @Schema(description = "Storage utilization percentage", example = "75")
    Integer storageUtilizationPercentage,
    
    // Activity Statistics
    @Schema(description = "Reports created this month", example = "25")
    Long reportsThisMonth,
    
    @Schema(description = "Work orders completed this month", example = "18")
    Long workOrdersCompletedThisMonth,
    
    @Schema(description = "Average report resolution time in hours", example = "48.5")
    BigDecimal averageResolutionTimeHours,
    
    // Performance Metrics
    @Schema(description = "Performance score (0-100)", example = "85")
    Integer performanceScore,
    
    @Schema(description = "Efficiency rating", example = "Good")
    String efficiencyRating,
    
    // Recent Activity
    @Schema(description = "Last report created timestamp")
    LocalDateTime lastReportCreated,
    
    @Schema(description = "Last work order completed timestamp")
    LocalDateTime lastWorkOrderCompleted,
    
    @Schema(description = "Last user login timestamp")
    LocalDateTime lastUserLogin,
    
    // Breakdown by Category
    @Schema(description = "Reports by priority breakdown")
    Map<String, Long> reportsByPriority,
    
    @Schema(description = "Work orders by status breakdown")
    Map<String, Long> workOrdersByStatus,
    
    @Schema(description = "Assets by condition breakdown")
    Map<String, Long> assetsByCondition,
    
    // Trends (compared to previous period)
    @Schema(description = "User count trend percentage", example = "5.2")
    BigDecimal userTrendPercentage,
    
    @Schema(description = "Report trend percentage", example = "-2.1")
    BigDecimal reportTrendPercentage,
    
    @Schema(description = "Performance trend", example = "improving")
    String performanceTrend
) {
    
    /**
     * Check if company is approaching user limits (>80% utilization).
     */
    public boolean isApproachingUserLimit() {
        return userUtilizationPercentage != null && userUtilizationPercentage > 80;
    }
    
    /**
     * Check if company is approaching school limits (>80% utilization).
     */
    public boolean isApproachingSchoolLimit() {
        return schoolUtilizationPercentage != null && schoolUtilizationPercentage > 80;
    }
    
    /**
     * Check if company is approaching storage limits (>80% utilization).
     */
    public boolean isApproachingStorageLimit() {
        return storageUtilizationPercentage != null && storageUtilizationPercentage > 80;
    }
    
    /**
     * Get overall health status based on various metrics.
     */
    public String getOverallHealthStatus() {
        if (performanceScore == null) {
            return "unknown";
        }
        
        if (performanceScore >= 90) {
            return "excellent";
        } else if (performanceScore >= 75) {
            return "good";
        } else if (performanceScore >= 60) {
            return "fair";
        } else {
            return "needs_attention";
        }
    }
    
    /**
     * Check if there are pending maintenance issues requiring attention.
     */
    public boolean hasPendingMaintenanceIssues() {
        return (pendingReports != null && pendingReports > 0) ||
               (assetsRequiringMaintenance != null && assetsRequiringMaintenance > 0);
    }
    
    /**
     * Get maintenance backlog percentage.
     */
    public Integer getMaintenanceBacklogPercentage() {
        if (totalReports == null || totalReports == 0 || pendingReports == null) {
            return 0;
        }
        return (int) Math.round((pendingReports.doubleValue() / totalReports) * 100);
    }
}