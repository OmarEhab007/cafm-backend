package com.cafm.cafmbackend.dto.mobile;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Mobile dashboard response DTO for supervisor app.
 * 
 * Purpose: Provides aggregated dashboard data optimized for mobile display
 * Pattern: Immutable record with comprehensive dashboard metrics
 * Java 23: Uses records for DTOs with automatic getters and equality
 * Architecture: API layer DTO following mobile-first design principles
 * Standards: Jackson annotations for JSON serialization, validation constraints
 */
public record MobileDashboardResponse(
    @JsonProperty("summary")
    @NotNull
    DashboardSummary summary,
    
    @JsonProperty("recent_reports")
    @NotNull
    List<MobileReportSummary> recentReports,
    
    @JsonProperty("pending_work_orders")
    @NotNull
    List<MobileWorkOrderSummary> pendingWorkOrders,
    
    @JsonProperty("notifications")
    @NotNull
    List<MobileNotification> notifications,
    
    @JsonProperty("statistics")
    @NotNull
    DashboardStatistics statistics,
    
    @JsonProperty("last_sync")
    LocalDateTime lastSync,
    
    @JsonProperty("metadata")
    Map<String, Object> metadata
) {
    
    /**
     * Dashboard summary with key metrics.
     */
    public record DashboardSummary(
        @JsonProperty("total_reports")
        int totalReports,
        
        @JsonProperty("pending_reports")
        int pendingReports,
        
        @JsonProperty("completed_today")
        int completedToday,
        
        @JsonProperty("urgent_count")
        int urgentCount,
        
        @JsonProperty("schools_assigned")
        int schoolsAssigned,
        
        @JsonProperty("active_work_orders")
        int activeWorkOrders
    ) {}
    
    /**
     * Mobile-optimized report summary.
     */
    public record MobileReportSummary(
        @JsonProperty("id")
        String id,
        
        @JsonProperty("title")
        String title,
        
        @JsonProperty("school_name")
        String schoolName,
        
        @JsonProperty("status")
        String status,
        
        @JsonProperty("priority")
        String priority,
        
        @JsonProperty("created_at")
        LocalDateTime createdAt,
        
        @JsonProperty("thumbnail_url")
        String thumbnailUrl,
        
        @JsonProperty("location")
        LocationInfo location
    ) {}
    
    /**
     * Mobile-optimized work order summary.
     */
    public record MobileWorkOrderSummary(
        @JsonProperty("id")
        String id,
        
        @JsonProperty("work_order_number")
        String workOrderNumber,
        
        @JsonProperty("title")
        String title,
        
        @JsonProperty("school_name")
        String schoolName,
        
        @JsonProperty("status")
        String status,
        
        @JsonProperty("priority")
        String priority,
        
        @JsonProperty("assigned_technicians")
        List<String> assignedTechnicians,
        
        @JsonProperty("scheduled_date")
        LocalDateTime scheduledDate,
        
        @JsonProperty("estimated_duration")
        Integer estimatedDuration
    ) {}
    
    /**
     * Mobile notification.
     */
    public record MobileNotification(
        @JsonProperty("id")
        String id,
        
        @JsonProperty("type")
        String type,
        
        @JsonProperty("title")
        String title,
        
        @JsonProperty("message")
        String message,
        
        @JsonProperty("timestamp")
        LocalDateTime timestamp,
        
        @JsonProperty("is_read")
        boolean isRead,
        
        @JsonProperty("action_url")
        String actionUrl
    ) {}
    
    /**
     * Dashboard statistics.
     */
    public record DashboardStatistics(
        @JsonProperty("completion_rate")
        double completionRate,
        
        @JsonProperty("average_resolution_time")
        double averageResolutionTime,
        
        @JsonProperty("weekly_trends")
        Map<String, Integer> weeklyTrends,
        
        @JsonProperty("category_distribution")
        Map<String, Integer> categoryDistribution,
        
        @JsonProperty("performance_score")
        double performanceScore
    ) {}
    
    /**
     * Location information.
     */
    public record LocationInfo(
        @JsonProperty("latitude")
        Double latitude,
        
        @JsonProperty("longitude")
        Double longitude,
        
        @JsonProperty("address")
        String address,
        
        @JsonProperty("distance")
        Double distance
    ) {}
}