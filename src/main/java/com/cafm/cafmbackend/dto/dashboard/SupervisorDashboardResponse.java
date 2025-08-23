package com.cafm.cafmbackend.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Dashboard response for supervisors using the mobile app.
 * Provides quick overview of their schools, reports, and pending tasks.
 */
public record SupervisorDashboardResponse(
    // User Info
    UUID supervisorId,
    String supervisorName,
    String lastLoginTime,
    
    // Quick Stats
    DashboardStats stats,
    
    // Assigned Schools
    List<SchoolSummary> mySchools,
    
    // Recent Reports
    List<RecentReport> recentReports,
    
    // Pending Actions
    List<PendingAction> pendingActions,
    
    // Recent Activities
    List<Activity> recentActivities,
    
    // Notifications Count
    Integer unreadNotifications,
    
    // Performance Metrics
    PerformanceMetrics performanceMetrics,
    
    // Quick Actions Available
    List<QuickAction> quickActions,
    
    // System Alerts
    List<SystemAlert> alerts
) {
    /**
     * Dashboard statistics
     */
    public record DashboardStats(
        // Report counts by status
        Integer draftReports,
        Integer submittedReports,
        Integer inReviewReports,
        Integer approvedReports,
        Integer rejectedReports,
        Integer inProgressReports,
        Integer completedReports,
        
        // Totals
        Integer totalReportsThisMonth,
        Integer totalReportsThisWeek,
        Integer totalReportsToday,
        
        // School counts
        Integer totalSchools,
        Integer activeSchools,
        
        // Urgent items
        Integer urgentReports,
        Integer overdueReports,
        Integer safetyHazards
    ) {}
    
    /**
     * School summary for supervisor
     */
    public record SchoolSummary(
        UUID schoolId,
        String schoolCode,
        String schoolName,
        String district,
        Integer activeReports,
        Integer pendingWorkOrders,
        Boolean hasUrgentIssues,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime lastVisit,
        String principalName,
        String principalPhone
    ) {}
    
    /**
     * Recent report summary
     */
    public record RecentReport(
        UUID reportId,
        String reportNumber,
        String title,
        String schoolName,
        String status,
        String priority,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        Boolean isUrgent,
        Boolean requiresAction,
        String thumbnailUrl
    ) {}
    
    /**
     * Pending action item
     */
    public record PendingAction(
        String actionType, // SUBMIT_REPORT, UPDATE_REPORT, VERIFY_WORK, etc.
        String description,
        UUID entityId,
        String entityType,
        String priority,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dueDate,
        Boolean isOverdue
    ) {}
    
    /**
     * Recent activity entry
     */
    public record Activity(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        String activityType,
        String description,
        String schoolName,
        UUID relatedEntityId,
        String relatedEntityType
    ) {}
    
    /**
     * Performance metrics for supervisor
     */
    public record PerformanceMetrics(
        // Report metrics
        Double averageReportSubmissionTime,
        Integer reportsSubmittedThisMonth,
        Double reportApprovalRate,
        
        // Response metrics
        Double averageResponseTime,
        Double completionRate,
        
        // Quality metrics
        Integer rejectedReportsCount,
        Double firstTimeApprovalRate,
        
        // Comparison with previous period
        Double reportGrowthRate,
        Double performanceScore
    ) {}
    
    /**
     * Quick action for mobile app
     */
    public record QuickAction(
        String actionId,
        String actionName,
        String actionIcon,
        String actionRoute,
        String actionColor,
        Boolean isEnabled,
        String badge // Number or text to show as badge
    ) {}
    
    /**
     * System alert or announcement
     */
    public record SystemAlert(
        String alertType, // INFO, WARNING, ERROR, SUCCESS
        String title,
        String message,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        String actionUrl,
        String actionLabel,
        Boolean isDismissible
    ) {}
    
    /**
     * Create default quick actions for supervisor
     */
    public static List<QuickAction> getDefaultQuickActions() {
        return List.of(
            new QuickAction("create_report", "New Report", "add-circle", "/reports/new", "#007BFF", true, null),
            new QuickAction("view_schools", "My Schools", "school", "/schools", "#28A745", true, null),
            new QuickAction("pending_reviews", "Pending Reviews", "clock", "/reports/pending", "#FFC107", true, null),
            new QuickAction("scan_qr", "Scan QR", "qr-code", "/scan", "#17A2B8", true, null)
        );
    }
    
    /**
     * Check if supervisor has critical items needing attention
     */
    public boolean hasCriticalItems() {
        return (stats != null && (stats.urgentReports > 0 || stats.safetyHazards > 0)) ||
               (pendingActions != null && pendingActions.stream().anyMatch(PendingAction::isOverdue));
    }
    
    /**
     * Calculate overall dashboard health score
     */
    public String getDashboardHealth() {
        if (stats == null) return "UNKNOWN";
        
        if (stats.urgentReports > 5 || stats.overdueReports > 10) {
            return "CRITICAL";
        } else if (stats.urgentReports > 2 || stats.overdueReports > 5) {
            return "WARNING";
        } else if (stats.urgentReports > 0 || stats.overdueReports > 0) {
            return "ATTENTION";
        }
        return "GOOD";
    }
}