package com.cafm.cafmbackend.dto.workorder;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for updating individual tasks within a work order.
 * Used by technicians to mark task progress and completion.
 */
public record TaskUpdateRequest(
    @NotNull(message = "Task ID is required")
    UUID taskId,
    
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "pending|in_progress|completed|skipped", 
             message = "Status must be one of: pending, in_progress, completed, skipped")
    String status,
    
    // Time tracking
    @DecimalMin(value = "0.0", message = "Actual hours cannot be negative")
    @DecimalMax(value = "99.9", message = "Actual hours cannot exceed 99.9")
    BigDecimal actualHours,
    
    // Notes about the task
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    String notes,
    
    // Completion details
    LocalDateTime completedAt,
    UUID completedById,
    
    // Issues encountered
    Boolean hadIssues,
    String issueDescription,
    
    // Photos specific to this task
    List<String> photoUrls,
    
    // Skip reason (if status is 'skipped')
    String skipReason,
    Boolean skipApprovedBy
) {
    /**
     * Factory method for marking task as completed
     */
    public static TaskUpdateRequest complete(
        UUID taskId,
        BigDecimal actualHours,
        String notes
    ) {
        return new TaskUpdateRequest(
            taskId, "completed", actualHours, notes,
            LocalDateTime.now(), null,
            false, null, null, null, false
        );
    }
    
    /**
     * Factory method for starting a task
     */
    public static TaskUpdateRequest start(UUID taskId) {
        return new TaskUpdateRequest(
            taskId, "in_progress", null, null,
            null, null, false, null, null, null, false
        );
    }
    
    /**
     * Factory method for skipping a task
     */
    public static TaskUpdateRequest skip(
        UUID taskId,
        String skipReason,
        Boolean approved
    ) {
        return new TaskUpdateRequest(
            taskId, "skipped", BigDecimal.ZERO, null,
            LocalDateTime.now(), null,
            false, null, null, skipReason, approved
        );
    }
    
    /**
     * Factory method for reporting task issues
     */
    public static TaskUpdateRequest reportIssue(
        UUID taskId,
        String issueDescription,
        List<String> photoUrls
    ) {
        return new TaskUpdateRequest(
            taskId, "in_progress", null, null,
            null, null,
            true, issueDescription, photoUrls, null, false
        );
    }
    
    /**
     * Validate that skipped tasks have a reason
     */
    public boolean isValidSkip() {
        if (!"skipped".equals(status)) {
            return true;
        }
        return skipReason != null && !skipReason.isBlank();
    }
    
    /**
     * Validate that completed tasks have hours recorded
     */
    public boolean isValidCompletion() {
        if (!"completed".equals(status)) {
            return true;
        }
        return actualHours != null && actualHours.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if this is a completion update
     */
    public boolean isCompletion() {
        return "completed".equals(status);
    }
    
    /**
     * Check if this task reports issues
     */
    public boolean hasIssues() {
        return Boolean.TRUE.equals(hadIssues) || 
               (issueDescription != null && !issueDescription.isBlank());
    }
}

/**
 * DTO for bulk updating multiple tasks at once.
 * Useful for marking multiple tasks as complete or updating their status together.
 */
record TaskBulkUpdateRequest(
    @NotEmpty(message = "Task IDs list cannot be empty")
    List<UUID> taskIds,
    
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "pending|in_progress|completed|skipped", 
             message = "Status must be one of: pending, in_progress, completed, skipped")
    String status,
    
    // Common notes for all tasks
    String notes,
    
    // Bulk completion
    Boolean markAllCompleted,
    LocalDateTime completionTime,
    
    // Bulk skip
    String skipReason,
    Boolean skipApproved
) {
    /**
     * Factory method for bulk completion
     */
    public static TaskBulkUpdateRequest completeAll(
        List<UUID> taskIds,
        String notes
    ) {
        return new TaskBulkUpdateRequest(
            taskIds, "completed", notes,
            true, LocalDateTime.now(),
            null, false
        );
    }
    
    /**
     * Factory method for bulk status update
     */
    public static TaskBulkUpdateRequest updateStatus(
        List<UUID> taskIds,
        String status
    ) {
        return new TaskBulkUpdateRequest(
            taskIds, status, null,
            false, null, null, false
        );
    }
    
    /**
     * Factory method for bulk skip
     */
    public static TaskBulkUpdateRequest skipAll(
        List<UUID> taskIds,
        String skipReason
    ) {
        return new TaskBulkUpdateRequest(
            taskIds, "skipped", null,
            false, null, skipReason, false
        );
    }
}