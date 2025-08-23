package com.cafm.cafmbackend.dto.workorder;

import com.cafm.cafmbackend.data.enums.WorkOrderPriority;
import com.cafm.cafmbackend.data.enums.WorkOrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight DTO for displaying work orders in lists.
 * Optimized for table/list views in both admin panel and mobile app.
 */
public record WorkOrderListResponse(
    // Identifiers
    UUID id,
    String workOrderNumber,
    
    // Basic info
    String title,
    WorkOrderStatus status,
    WorkOrderPriority priority,
    String category,
    
    // School info
    UUID schoolId,
    String schoolName,
    String schoolCode,
    String district,
    
    // Assignment info
    UUID assignedToId,
    String assignedToName,
    String assignedToSpecialization,
    
    // Report reference
    UUID reportId,
    String reportNumber,
    
    // Scheduling
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime scheduledStart,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime scheduledEnd,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime actualStart,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime actualEnd,
    
    // Progress
    Integer progressPercentage,
    Integer completedTasksCount,
    Integer totalTasksCount,
    
    // Cost
    BigDecimal estimatedCost,
    BigDecimal actualCost,
    
    // Status indicators
    Boolean isOverdue,
    Boolean isUrgent,
    Boolean hasIssues,
    Boolean requiresVerification,
    
    // Time tracking
    BigDecimal estimatedHours,
    BigDecimal actualHours,
    
    // Creation info
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    String createdBy
) {
    /**
     * Calculate if work order is overdue
     */
    public boolean calculateIsOverdue() {
        if (status == WorkOrderStatus.COMPLETED || 
            status == WorkOrderStatus.CANCELLED || 
            status == WorkOrderStatus.VERIFIED) {
            return false;
        }
        
        if (scheduledEnd == null) {
            return false;
        }
        
        return LocalDateTime.now().isAfter(scheduledEnd);
    }
    
    /**
     * Get status badge color for UI
     */
    public String getStatusColor() {
        return switch (status) {
            case DRAFT -> "#6C757D";        // Gray
            case PENDING -> "#FFC107";       // Yellow
            case ASSIGNED -> "#007BFF";      // Blue
            case IN_PROGRESS -> "#17A2B8";   // Cyan
            case ON_HOLD -> "#FD7E14";       // Orange
            case COMPLETED -> "#28A745";     // Green
            case CANCELLED -> "#DC3545";     // Red
            case VERIFIED -> "#28A745";      // Green
        };
    }
    
    /**
     * Get priority badge color for UI
     */
    public String getPriorityColor() {
        return switch (priority) {
            case EMERGENCY -> "#DC3545";    // Red
            case HIGH -> "#FD7E14";          // Orange
            case MEDIUM -> "#FFC107";        // Yellow
            case LOW -> "#28A745";           // Green
            case SCHEDULED -> "#6C757D";     // Gray
        };
    }
    
    /**
     * Calculate task completion percentage
     */
    public Integer calculateTaskProgress() {
        if (totalTasksCount == null || totalTasksCount == 0) {
            return 0;
        }
        if (completedTasksCount == null) {
            return 0;
        }
        return (completedTasksCount * 100) / totalTasksCount;
    }
    
    /**
     * Check if work order can be started by technician
     */
    public boolean canStart() {
        return status == WorkOrderStatus.ASSIGNED || status == WorkOrderStatus.ON_HOLD;
    }
    
    /**
     * Check if work order is active
     */
    public boolean isActive() {
        return status == WorkOrderStatus.ASSIGNED || 
               status == WorkOrderStatus.IN_PROGRESS || 
               status == WorkOrderStatus.ON_HOLD;
    }
    
    /**
     * Get cost variance
     */
    public BigDecimal getCostVariance() {
        if (estimatedCost == null || actualCost == null) {
            return BigDecimal.ZERO;
        }
        return actualCost.subtract(estimatedCost);
    }
    
    /**
     * Get time variance in hours
     */
    public BigDecimal getTimeVariance() {
        if (estimatedHours == null || actualHours == null) {
            return BigDecimal.ZERO;
        }
        return actualHours.subtract(estimatedHours);
    }
    
    /**
     * Check if this is a team work order
     */
    public boolean isTeamWork() {
        // Could be expanded to check for multiple assignees
        return false;
    }
    
    /**
     * Get duration in hours
     */
    public Long getDurationHours() {
        if (actualStart == null || actualEnd == null) {
            return null;
        }
        return java.time.Duration.between(actualStart, actualEnd).toHours();
    }
}