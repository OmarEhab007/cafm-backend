package com.cafm.cafmbackend.dto.report;

import com.cafm.cafmbackend.data.enums.ReportPriority;
import com.cafm.cafmbackend.data.enums.ReportStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight DTO for displaying reports in lists (mobile app and admin panel).
 * Contains only essential information for quick loading and display.
 */
public record ReportListResponse(
    UUID id,
    String reportNumber,
    
    // Basic info
    String title,
    ReportStatus status,
    ReportPriority priority,
    String category,
    
    // School info
    UUID schoolId,
    String schoolName,
    String schoolCode,
    
    // Supervisor info
    UUID supervisorId,
    String supervisorName,
    
    // Assignment info (if assigned)
    UUID assignedToId,
    String assignedToName,
    
    // Dates
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDateTime reportedDate,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDateTime scheduledDate,
    
    // Quick indicators
    Boolean isUrgent,
    Boolean isSafetyHazard,
    Boolean hasPhotos,
    String thumbnailUrl,
    
    // Status indicators
    Boolean isOverdue,
    Integer daysOpen,
    
    // Work order reference (if created)
    UUID workOrderId,
    String workOrderNumber
) {
    /**
     * Calculate if report is overdue based on priority and days open
     */
    public boolean calculateIsOverdue() {
        if (status == ReportStatus.COMPLETED || status == ReportStatus.CANCELLED) {
            return false;
        }
        
        if (daysOpen == null) return false;
        
        return switch (priority) {
            case CRITICAL -> daysOpen > 0.5;
            case URGENT -> daysOpen > 1;
            case HIGH -> daysOpen > 3;
            case MEDIUM -> daysOpen > 7;
            case LOW -> daysOpen > 14;

        };
    }
    
    /**
     * Get status badge color for UI
     */
    public String getStatusColor() {
        return switch (status) {
            case DRAFT -> "#808080";        // Gray
            case SUBMITTED -> "#007BFF";     // Blue
            case IN_REVIEW -> "#FFC107";     // Amber
            case APPROVED -> "#28A745";      // Green
            case REJECTED -> "#DC3545";      // Red
            case IN_PROGRESS -> "#17A2B8";   // Cyan
            case PENDING -> "#FD7E14";       // Orange
            case COMPLETED -> "#28A745";     // Green
            case CANCELLED -> "#6C757D";     // Dark Gray
            case LATE -> "#DC3545";          // Red for overdue
            case LATE_COMPLETED -> "#20C997"; // Teal for late completion
        };
    }
    
    /**
     * Get priority badge color for UI
     */
    public String getPriorityColor() {
        return switch (priority) {
            case CRITICAL -> "#DC3545"; //Red
            case URGENT -> "#FF6384";
            case HIGH -> "#FD7E14";     // Orange
            case MEDIUM -> "#FFC107";   // Yellow
            case LOW -> "#28A745";      // Green
        };
    }
    
    /**
     * Check if report can be edited by supervisor
     */
    public boolean canEdit() {
        return status == ReportStatus.DRAFT || status == ReportStatus.REJECTED;
    }
    
    /**
     * Check if report can be deleted by supervisor
     */
    public boolean canDelete() {
        return status == ReportStatus.DRAFT;
    }
}