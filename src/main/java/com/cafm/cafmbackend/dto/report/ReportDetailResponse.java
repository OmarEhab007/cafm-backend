package com.cafm.cafmbackend.dto.report;

import com.cafm.cafmbackend.data.enums.ReportPriority;
import com.cafm.cafmbackend.data.enums.ReportStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Complete report details DTO for viewing full report information.
 * Used in both mobile app (supervisor view) and admin panel.
 */
public record ReportDetailResponse(
    // Identifiers
    UUID id,
    String reportNumber,
    
    // Basic Information
    String title,
    String description,
    ReportStatus status,
    ReportPriority priority,
    String category,
    
    // Location Details
    String location,
    String building,
    String floor,
    String roomNumber,
    
    // School Information
    UUID schoolId,
    String schoolName,
    String schoolCode,
    String schoolAddress,
    String schoolDistrict,
    
    // Supervisor Information
    UUID supervisorId,
    String supervisorName,
    String supervisorEmail,
    String supervisorPhone,
    
    // Assignment Information
    UUID assignedToId,
    String assignedToName,
    String assignedToEmail,
    String assignedToPhone,
    String assignedToSpecialization,
    
    // Damage Assessment
    String damageAssessment,
    Boolean isUrgent,
    Boolean isSafetyHazard,
    
    // Photos and Attachments
    List<PhotoAttachment> photos,
    List<DocumentAttachment> documents,
    
    // Dates
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDateTime reportedDate,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDateTime scheduledDate,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDateTime completedDate,
    
    // Cost Information
    BigDecimal estimatedCost,
    BigDecimal actualCost,
    String costNotes,
    
    // Work Order Information (if created)
    WorkOrderSummary workOrder,
    
    // Review Information
    String reviewedBy,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime reviewedAt,
    String reviewNotes,
    String rejectionReason,
    
    // Completion Information
    String completionNotes,
    String verifiedBy,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime verifiedAt,
    
    // Contact Information
    String contactName,
    String contactPhone,
    
    // Additional Information
    String notes,
    List<String> tags,
    
    // Timeline/History
    List<ReportActivity> activities,
    
    // Statistics
    Integer daysOpen,
    Boolean isOverdue,
    Integer commentsCount
) {
    /**
     * Photo attachment record
     */
    public record PhotoAttachment(
        UUID id,
        String url,
        String thumbnailUrl,
        String caption,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime uploadedAt,
        String uploadedBy
    ) {}
    
    /**
     * Document attachment record
     */
    public record DocumentAttachment(
        UUID id,
        String name,
        String url,
        String type,
        Long sizeInBytes,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime uploadedAt,
        String uploadedBy
    ) {}
    
    /**
     * Work order summary when report has been converted
     */
    public record WorkOrderSummary(
        UUID id,
        String workOrderNumber,
        String status,
        String assignedToName,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime scheduledStart,
        BigDecimal totalCost,
        Integer completedTasksCount,
        Integer totalTasksCount
    ) {}
    
    /**
     * Report activity/timeline entry
     */
    public record ReportActivity(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        String action,
        String performedBy,
        String description,
        String oldValue,
        String newValue
    ) {}
    
    /**
     * Check if report can transition to a specific status
     */
    public boolean canTransitionTo(ReportStatus newStatus) {
        return switch (status) {
            case DRAFT -> newStatus == ReportStatus.SUBMITTED;
            case SUBMITTED -> newStatus == ReportStatus.IN_REVIEW || newStatus == ReportStatus.CANCELLED;
            case IN_REVIEW -> newStatus == ReportStatus.APPROVED || newStatus == ReportStatus.REJECTED;
            case REJECTED -> newStatus == ReportStatus.SUBMITTED || newStatus == ReportStatus.CANCELLED;
            case APPROVED -> newStatus == ReportStatus.IN_PROGRESS || newStatus == ReportStatus.CANCELLED;
            case IN_PROGRESS -> newStatus == ReportStatus.COMPLETED || newStatus == ReportStatus.PENDING || newStatus == ReportStatus.LATE;
            case PENDING -> newStatus == ReportStatus.IN_PROGRESS || newStatus == ReportStatus.CANCELLED;
            case COMPLETED -> false; // Final state
            case CANCELLED -> false; // Final state
            case LATE -> newStatus == ReportStatus.LATE_COMPLETED || newStatus == ReportStatus.CANCELLED;
            case LATE_COMPLETED -> false; // Final state
        };
    }
    
    /**
     * Check if report requires immediate attention
     */
    public boolean requiresImmediateAttention() {
        return (Boolean.TRUE.equals(isUrgent) || Boolean.TRUE.equals(isSafetyHazard)) 
            && status != ReportStatus.COMPLETED 
            && status != ReportStatus.CANCELLED;
    }
    
    /**
     * Calculate completion percentage based on work order progress
     */
    public Integer getCompletionPercentage() {
        if (workOrder == null || workOrder.totalTasksCount == null || workOrder.totalTasksCount == 0) {
            return 0;
        }
        return (workOrder.completedTasksCount * 100) / workOrder.totalTasksCount;
    }
}