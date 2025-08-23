package com.cafm.cafmbackend.dto.workorder;

import com.cafm.cafmbackend.data.enums.WorkOrderPriority;
import com.cafm.cafmbackend.data.enums.WorkOrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Complete work order details DTO.
 * Used for viewing full work order information with all related data.
 */
public record WorkOrderDetailResponse(
    // Identifiers
    UUID id,
    String workOrderNumber,
    
    // Basic Information
    String title,
    String description,
    WorkOrderStatus status,
    WorkOrderPriority priority,
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
    String schoolContactName,
    String schoolContactPhone,
    
    // Report Information (if linked)
    UUID reportId,
    String reportNumber,
    String reportTitle,
    UUID reportedById,
    String reportedByName,
    
    // Assignment Information
    UUID assignedToId,
    String assignedToName,
    String assignedToEmail,
    String assignedToPhone,
    String assignedToSpecialization,
    UUID assignedById,
    String assignedByName,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime assignmentDate,
    String assignmentNotes,
    
    // Team Members (if team work)
    List<TeamMember> teamMembers,
    
    // Scheduling
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime scheduledStart,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime scheduledEnd,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime actualStart,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime actualEnd,
    
    // Tasks
    List<WorkOrderTask> tasks,
    Integer completedTasksCount,
    Integer totalTasksCount,
    
    // Materials
    List<MaterialUsage> materials,
    BigDecimal totalMaterialCost,
    
    // Cost Information
    BigDecimal estimatedCost,
    BigDecimal laborCost,
    BigDecimal materialCost,
    BigDecimal additionalCost,
    BigDecimal totalCost,
    String costNotes,
    
    // Time Tracking
    BigDecimal estimatedHours,
    BigDecimal actualHours,
    BigDecimal overtimeHours,
    
    // Progress Information
    Integer progressPercentage,
    String progressNotes,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime lastProgressUpdate,
    String lastProgressUpdateBy,
    
    // Attachments
    List<Attachment> attachments,
    List<Photo> photos,
    
    // Verification (if completed)
    Boolean isVerified,
    UUID verifiedById,
    String verifiedByName,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime verifiedAt,
    String verificationNotes,
    String verificationSignature,
    
    // Customer Feedback
    String customerName,
    String customerSignature,
    Boolean customerSatisfied,
    String customerFeedback,
    Integer customerRating,
    
    // Issues and Notes
    String issuesEncountered,
    String resolutionNotes,
    String specialInstructions,
    String safetyNotes,
    
    // Asset Information (if linked)
    UUID assetId,
    String assetName,
    String assetCode,
    
    // Status Flags
    Boolean isOverdue,
    Boolean isUrgent,
    Boolean requiresAdditionalParts,
    Boolean requiresAdditionalTime,
    Boolean safetyIncidentOccurred,
    
    // Activity Timeline
    List<WorkOrderActivity> activities,
    
    // Metadata
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    String createdBy,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt,
    String updatedBy
) {
    /**
     * Team member information
     */
    public record TeamMember(
        UUID userId,
        String name,
        String role,
        String specialization,
        BigDecimal hoursWorked
    ) {}
    
    /**
     * Work order task with completion status
     */
    public record WorkOrderTask(
        UUID id,
        Integer taskNumber,
        String title,
        String description,
        String status,
        Boolean isMandatory,
        BigDecimal estimatedHours,
        BigDecimal actualHours,
        UUID assignedToId,
        String assignedToName,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime completedAt,
        String completedBy,
        String notes
    ) {
        public boolean isCompleted() {
            return "completed".equalsIgnoreCase(status);
        }
    }
    
    /**
     * Material usage record
     */
    public record MaterialUsage(
        UUID id,
        String name,
        String unit,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal totalCost,
        UUID inventoryItemId,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime usedAt,
        String usedBy,
        String notes
    ) {}
    
    /**
     * Attachment record
     */
    public record Attachment(
        UUID id,
        String name,
        String type,
        String url,
        Long sizeInBytes,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime uploadedAt,
        String uploadedBy
    ) {}
    
    /**
     * Photo record
     */
    public record Photo(
        UUID id,
        String url,
        String thumbnailUrl,
        String caption,
        String photoType, // before, during, after
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime takenAt,
        String takenBy
    ) {}
    
    /**
     * Work order activity timeline
     */
    public record WorkOrderActivity(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        String action,
        String performedBy,
        String description,
        String oldValue,
        String newValue,
        String notes
    ) {}
    
    /**
     * Calculate total actual cost
     */
    public BigDecimal calculateTotalCost() {
        BigDecimal total = BigDecimal.ZERO;
        
        if (laborCost != null) total = total.add(laborCost);
        if (materialCost != null) total = total.add(materialCost);
        if (additionalCost != null) total = total.add(additionalCost);
        
        return total;
    }
    
    /**
     * Calculate cost variance
     */
    public BigDecimal getCostVariance() {
        if (estimatedCost == null || totalCost == null) {
            return BigDecimal.ZERO;
        }
        return totalCost.subtract(estimatedCost);
    }
    
    /**
     * Calculate time variance
     */
    public BigDecimal getTimeVariance() {
        if (estimatedHours == null || actualHours == null) {
            return BigDecimal.ZERO;
        }
        return actualHours.subtract(estimatedHours);
    }
    
    /**
     * Check if work order can transition to a status
     */
    public boolean canTransitionTo(WorkOrderStatus newStatus) {
        return switch (status) {
            case DRAFT -> newStatus == WorkOrderStatus.PENDING || newStatus == WorkOrderStatus.CANCELLED;
            case PENDING -> newStatus == WorkOrderStatus.ASSIGNED || newStatus == WorkOrderStatus.CANCELLED;
            case ASSIGNED -> newStatus == WorkOrderStatus.IN_PROGRESS || newStatus == WorkOrderStatus.CANCELLED;
            case IN_PROGRESS -> newStatus == WorkOrderStatus.ON_HOLD || 
                               newStatus == WorkOrderStatus.COMPLETED || 
                               newStatus == WorkOrderStatus.CANCELLED;
            case ON_HOLD -> newStatus == WorkOrderStatus.IN_PROGRESS || newStatus == WorkOrderStatus.CANCELLED;
            case COMPLETED -> newStatus == WorkOrderStatus.VERIFIED;
            case CANCELLED -> false;
            case VERIFIED -> false;
        };
    }
    
    /**
     * Check if all mandatory tasks are completed
     */
    public boolean areMandatoryTasksCompleted() {
        if (tasks == null || tasks.isEmpty()) {
            return true;
        }
        
        return tasks.stream()
            .filter(WorkOrderTask::isMandatory)
            .allMatch(WorkOrderTask::isCompleted);
    }
    
    /**
     * Get completion percentage based on tasks
     */
    public int getTaskCompletionPercentage() {
        if (totalTasksCount == null || totalTasksCount == 0) {
            return 0;
        }
        if (completedTasksCount == null) {
            return 0;
        }
        return (completedTasksCount * 100) / totalTasksCount;
    }
}