package com.cafm.cafmbackend.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Dashboard response for technicians using mobile app or web portal.
 * Focuses on assigned work, daily tasks, and performance metrics.
 */
public record TechnicianDashboardResponse(
    // Technician Info
    UUID technicianId,
    String technicianName,
    String specialization,
    Boolean isAvailable,
    
    // Today's Overview
    TodayOverview todayOverview,
    
    // Assigned Work Orders
    List<AssignedWorkOrder> assignedWorkOrders,
    
    // Today's Tasks
    List<TodayTask> todaysTasks,
    
    // This Week's Schedule
    WeekSchedule weekSchedule,
    
    // Performance Metrics
    TechnicianMetrics performanceMetrics,
    
    // Inventory & Tools
    InventoryStatus inventoryStatus,
    
    // Recent Completions
    List<RecentCompletion> recentCompletions,
    
    // Notifications
    List<TechnicianNotification> notifications,
    
    // Quick Stats
    QuickStats quickStats
) {
    /**
     * Today's overview
     */
    public record TodayOverview(
        Integer scheduledWorkOrders,
        Integer completedWorkOrders,
        Integer pendingTasks,
        BigDecimal hoursWorked,
        BigDecimal hoursRemaining,
        
        // Current/Next assignment
        CurrentAssignment currentAssignment,
        NextAssignment nextAssignment,
        
        // Travel info
        Double totalDistanceToday,
        Integer locationsToVisit,
        String optimalRoute
    ) {
        public record CurrentAssignment(
            UUID workOrderId,
            String workOrderNumber,
            String title,
            String schoolName,
            String location,
            String priority,
            @JsonFormat(pattern = "HH:mm")
            LocalDateTime startTime,
            Integer estimatedMinutes,
            Boolean isOverdue
        ) {}
        
        public record NextAssignment(
            UUID workOrderId,
            String title,
            String schoolName,
            @JsonFormat(pattern = "HH:mm")
            LocalDateTime scheduledTime,
            String travelTime,
            String distance
        ) {}
    }
    
    /**
     * Assigned work order
     */
    public record AssignedWorkOrder(
        UUID workOrderId,
        String workOrderNumber,
        String title,
        String description,
        String status,
        String priority,
        
        // Location
        String schoolName,
        String building,
        String location,
        Double latitude,
        Double longitude,
        
        // Schedule
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime scheduledStart,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime scheduledEnd,
        
        // Progress
        Integer progressPercentage,
        Integer completedTasks,
        Integer totalTasks,
        
        // Requirements
        List<String> requiredTools,
        List<String> requiredMaterials,
        String specialInstructions,
        
        // Contact
        String contactName,
        String contactPhone,
        
        // Flags
        Boolean isUrgent,
        Boolean requiresSafetyGear,
        Boolean hasCustomerSignature
    ) {}
    
    /**
     * Today's task
     */
    public record TodayTask(
        UUID taskId,
        UUID workOrderId,
        String workOrderNumber,
        String taskTitle,
        String schoolName,
        String location,
        String status, // pending, in_progress, completed
        Boolean isMandatory,
        BigDecimal estimatedHours,
        @JsonFormat(pattern = "HH:mm")
        LocalDateTime scheduledTime,
        String priority,
        String notes
    ) {}
    
    /**
     * Week schedule overview
     */
    public record WeekSchedule(
        List<DaySchedule> days,
        Integer totalWorkOrders,
        BigDecimal totalEstimatedHours,
        Integer totalLocations
    ) {
        public record DaySchedule(
            String dayName,
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDateTime date,
            Integer workOrderCount,
            BigDecimal estimatedHours,
            List<ScheduleItem> items
        ) {
            public record ScheduleItem(
                @JsonFormat(pattern = "HH:mm")
                LocalDateTime time,
                String title,
                String location,
                String duration
            ) {}
        }
    }
    
    /**
     * Technician performance metrics
     */
    public record TechnicianMetrics(
        // Completion metrics
        Integer completedThisWeek,
        Integer completedThisMonth,
        Double completionRate,
        Double onTimeRate,
        
        // Efficiency metrics
        Double averageCompletionTime,
        BigDecimal hoursWorkedThisWeek,
        BigDecimal hoursWorkedThisMonth,
        Double productivityScore,
        
        // Quality metrics
        Double firstTimeFixRate,
        Double customerSatisfactionScore,
        Integer positiveReviews,
        Integer totalReviews,
        
        // Comparison
        Double performanceVsLastMonth,
        String performanceTrend, // IMPROVING, STABLE, DECLINING
        Integer rankAmongPeers
    ) {}
    
    /**
     * Inventory and tools status
     */
    public record InventoryStatus(
        List<ToolStatus> assignedTools,
        List<MaterialStock> commonMaterials,
        List<PendingRequest> pendingRequests,
        Boolean hasAllRequiredTools,
        List<String> missingItems
    ) {
        public record ToolStatus(
            String toolName,
            String toolCode,
            String condition,
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDateTime lastInspection,
            Boolean needsCalibration
        ) {}
        
        public record MaterialStock(
            String materialName,
            String unit,
            BigDecimal quantity,
            String status, // SUFFICIENT, LOW, OUT_OF_STOCK
            Boolean needsReorder
        ) {}
        
        public record PendingRequest(
            String itemName,
            BigDecimal quantity,
            String status,
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDateTime requestedDate
        ) {}
    }
    
    /**
     * Recent completion
     */
    public record RecentCompletion(
        UUID workOrderId,
        String workOrderNumber,
        String title,
        String schoolName,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime completedAt,
        BigDecimal hoursWorked,
        String customerFeedback,
        Integer rating,
        Boolean verified
    ) {}
    
    /**
     * Technician notification
     */
    public record TechnicianNotification(
        UUID notificationId,
        String type, // NEW_ASSIGNMENT, PRIORITY_CHANGE, SCHEDULE_CHANGE, etc.
        String title,
        String message,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        Boolean isRead,
        String priority,
        UUID relatedEntityId,
        String actionRequired
    ) {}
    
    /**
     * Quick statistics
     */
    public record QuickStats(
        // Today
        Integer completedToday,
        BigDecimal hoursToday,
        Integer tasksToday,
        
        // This week
        Integer completedThisWeek,
        BigDecimal hoursThisWeek,
        Double efficiencyThisWeek,
        
        // This month
        Integer completedThisMonth,
        BigDecimal earningsThisMonth,
        Double satisfactionThisMonth,
        
        // Achievements
        List<Achievement> recentAchievements
    ) {
        public record Achievement(
            String title,
            String description,
            String icon,
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDateTime earnedDate
        ) {}
    }
    
    /**
     * Calculate today's workload status
     */
    public String getTodayWorkloadStatus() {
        if (todayOverview == null || todayOverview.scheduledWorkOrders == null) {
            return "UNKNOWN";
        }
        
        int scheduled = todayOverview.scheduledWorkOrders;
        if (scheduled == 0) return "FREE";
        if (scheduled <= 2) return "LIGHT";
        if (scheduled <= 4) return "NORMAL";
        if (scheduled <= 6) return "BUSY";
        return "OVERLOADED";
    }
    
    /**
     * Check if technician needs immediate attention for any task
     */
    public boolean hasUrgentTasks() {
        return assignedWorkOrders != null && 
               assignedWorkOrders.stream().anyMatch(AssignedWorkOrder::isUrgent);
    }
    
    /**
     * Calculate completion progress for today
     */
    public int getTodayCompletionPercentage() {
        if (todayOverview == null || todayOverview.scheduledWorkOrders == null || 
            todayOverview.scheduledWorkOrders == 0) {
            return 0;
        }
        
        int completed = todayOverview.completedWorkOrders != null ? 
                       todayOverview.completedWorkOrders : 0;
        return (completed * 100) / todayOverview.scheduledWorkOrders;
    }
}