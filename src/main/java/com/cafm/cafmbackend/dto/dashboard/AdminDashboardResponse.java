package com.cafm.cafmbackend.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Comprehensive dashboard response for administrators.
 * Provides system-wide overview, analytics, and management insights.
 */
public record AdminDashboardResponse(
    // Admin Info
    UUID adminId,
    String adminName,
    String companyName,
    String role,
    
    // System Overview
    SystemOverview systemOverview,
    
    // Report Analytics
    ReportAnalytics reportAnalytics,
    
    // Work Order Analytics
    WorkOrderAnalytics workOrderAnalytics,
    
    // Technician Performance
    List<TechnicianPerformance> technicianPerformance,
    
    // School Statistics
    List<SchoolStatistics> schoolStatistics,
    
    // Cost Analysis
    CostAnalysis costAnalysis,
    
    // Trending Issues
    List<TrendingIssue> trendingIssues,
    
    // Overdue Items
    OverdueItems overdueItems,
    
    // Recent Activities
    List<AdminActivity> recentActivities,
    
    // System Health
    SystemHealth systemHealth,
    
    // Upcoming Scheduled Maintenance
    List<ScheduledMaintenance> upcomingMaintenance
) {
    /**
     * System-wide overview statistics
     */
    public record SystemOverview(
        // Entity counts
        Integer totalSchools,
        Integer activeSchools,
        Integer totalUsers,
        Integer activeTechnicians,
        Integer activeSupervisors,
        
        // Current status counts
        Integer openReports,
        Integer activeWorkOrders,
        Integer pendingApprovals,
        Integer overdueWorkOrders,
        
        // Today's activity
        Integer reportsCreatedToday,
        Integer workOrdersCompletedToday,
        Integer newUsersToday,
        
        // Month-to-date
        Integer reportsMTD,
        Integer workOrdersMTD,
        BigDecimal costMTD,
        
        // Alerts
        Integer criticalAlerts,
        Integer warningAlerts
    ) {}
    
    /**
     * Report analytics
     */
    public record ReportAnalytics(
        // Status distribution
        Map<String, Integer> reportsByStatus,
        Map<String, Integer> reportsByPriority,
        Map<String, Integer> reportsByCategory,
        
        // Trend data (last 30 days)
        List<DailyCount> dailyReportCounts,
        
        // Performance metrics
        Double averageApprovalTime,
        Double averageResolutionTime,
        Double firstTimeApprovalRate,
        Double rejectionRate,
        
        // Top reporters
        List<TopReporter> topReporters
    ) {}
    
    /**
     * Work order analytics
     */
    public record WorkOrderAnalytics(
        // Status distribution
        Map<String, Integer> workOrdersByStatus,
        Map<String, Integer> workOrdersByPriority,
        
        // Performance metrics
        Double averageCompletionTime,
        Double onTimeCompletionRate,
        Double firstTimeFixRate,
        Integer backlogCount,
        
        // Efficiency metrics
        BigDecimal averageCostPerWorkOrder,
        BigDecimal averageHoursPerWorkOrder,
        Double technicianUtilizationRate,
        
        // Trend data
        List<DailyCount> dailyCompletions,
        List<WeeklyTrend> weeklyTrends
    ) {}
    
    /**
     * Technician performance metrics
     */
    public record TechnicianPerformance(
        UUID technicianId,
        String technicianName,
        String specialization,
        
        // Work metrics
        Integer assignedWorkOrders,
        Integer completedWorkOrders,
        Integer inProgressWorkOrders,
        
        // Performance metrics
        Double completionRate,
        Double averageCompletionTime,
        Double customerSatisfactionScore,
        Integer totalHoursWorked,
        
        // Efficiency
        BigDecimal costPerHour,
        Double productivityScore,
        Boolean isAvailable,
        
        // Current assignment
        String currentLocation,
        String currentWorkOrder
    ) {}
    
    /**
     * School statistics
     */
    public record SchoolStatistics(
        UUID schoolId,
        String schoolName,
        String district,
        
        // Report metrics
        Integer totalReports,
        Integer openReports,
        Integer completedReports,
        
        // Work order metrics
        Integer activeWorkOrders,
        Integer completedWorkOrdersMTD,
        
        // Cost metrics
        BigDecimal totalCostMTD,
        BigDecimal averageCostPerReport,
        
        // Performance
        Double resolutionRate,
        Double satisfactionScore,
        
        // Maintenance
        LocalDate lastPreventiveMaintenance,
        LocalDate nextScheduledMaintenance
    ) {}
    
    /**
     * Cost analysis
     */
    public record CostAnalysis(
        // Current period costs
        BigDecimal totalCostToday,
        BigDecimal totalCostThisWeek,
        BigDecimal totalCostThisMonth,
        BigDecimal totalCostThisYear,
        
        // Cost breakdown
        BigDecimal laborCost,
        BigDecimal materialCost,
        BigDecimal contractorCost,
        BigDecimal otherCost,
        
        // Budget comparison
        BigDecimal monthlyBudget,
        BigDecimal budgetUtilization,
        BigDecimal projectedMonthEnd,
        
        // Cost by category
        Map<String, BigDecimal> costByCategory,
        Map<String, BigDecimal> costBySchool,
        
        // Trends
        List<MonthlyCost> monthlyTrends,
        Double costGrowthRate
    ) {}
    
    /**
     * Trending issues
     */
    public record TrendingIssue(
        String category,
        String description,
        Integer occurrenceCount,
        List<String> affectedSchools,
        String trend, // INCREASING, DECREASING, STABLE
        Double changePercentage,
        String recommendedAction
    ) {}
    
    /**
     * Overdue items summary
     */
    public record OverdueItems(
        List<OverdueReport> overdueReports,
        List<OverdueWorkOrder> overdueWorkOrders,
        Integer totalOverdueCount,
        Integer criticalOverdueCount
    ) {
        public record OverdueReport(
            UUID reportId,
            String reportNumber,
            String title,
            String schoolName,
            Integer daysOverdue,
            String priority
        ) {}
        
        public record OverdueWorkOrder(
            UUID workOrderId,
            String workOrderNumber,
            String title,
            String assignedTo,
            Integer daysOverdue,
            String priority
        ) {}
    }
    
    /**
     * Admin activity log entry
     */
    public record AdminActivity(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        String activityType,
        String description,
        String performedBy,
        UUID relatedEntityId,
        String impact // HIGH, MEDIUM, LOW
    ) {}
    
    /**
     * System health metrics
     */
    public record SystemHealth(
        String overallStatus, // HEALTHY, DEGRADED, CRITICAL
        Double systemUptime,
        Integer activeUsers,
        Double apiResponseTime,
        Long pendingNotifications,
        Long queuedJobs,
        
        // Resource utilization
        Double databaseUsage,
        Double storageUsage,
        Integer apiCallsToday,
        
        // Error metrics
        Integer errorsToday,
        Integer failedLoginsToday,
        List<SystemIssue> currentIssues
    ) {
        public record SystemIssue(
            String component,
            String issue,
            String severity,
            LocalDateTime since
        ) {}
    }
    
    /**
     * Scheduled maintenance entry
     */
    public record ScheduledMaintenance(
        UUID maintenanceId,
        String type, // PREVENTIVE, INSPECTION, CALIBRATION
        String description,
        UUID schoolId,
        String schoolName,
        UUID assetId,
        String assetName,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate scheduledDate,
        String assignedTo,
        String status,
        Boolean isOverdue
    ) {}
    
    // Helper records
    public record DailyCount(
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate date,
        Integer count
    ) {}
    
    public record WeeklyTrend(
        String week,
        Integer created,
        Integer completed,
        Integer cancelled
    ) {}
    
    public record MonthlyCost(
        String month,
        BigDecimal cost,
        Integer workOrderCount
    ) {}
    
    public record TopReporter(
        UUID userId,
        String name,
        Integer reportCount,
        Double approvalRate
    ) {}
    
    /**
     * Calculate dashboard KPI score
     */
    public Double calculateKPIScore() {
        double score = 100.0;
        
        // Deduct for overdue items
        if (overdueItems != null && overdueItems.totalOverdueCount != null) {
            score -= Math.min(overdueItems.totalOverdueCount * 2, 30);
        }
        
        // Deduct for system health issues
        if (systemHealth != null && "CRITICAL".equals(systemHealth.overallStatus)) {
            score -= 20;
        } else if ("DEGRADED".equals(systemHealth.overallStatus)) {
            score -= 10;
        }
        
        // Bonus for good performance
        if (workOrderAnalytics != null && workOrderAnalytics.onTimeCompletionRate != null) {
            if (workOrderAnalytics.onTimeCompletionRate > 0.9) {
                score += 10;
            }
        }
        
        return Math.max(0, Math.min(100, score));
    }
}