package com.cafm.cafmbackend.dto.workorder;

import com.cafm.cafmbackend.shared.enums.WorkOrderPriority;
import com.cafm.cafmbackend.shared.enums.WorkOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Work order update request DTO.
 * 
 * Purpose: Validates and transfers work order update data
 * Pattern: Immutable record DTO with optional fields for partial updates
 * Java 23: Record with optional validation for PATCH-style updates
 * Architecture: DTO for work order modification with assignment support
 * Standards: Bean validation with business rules enforcement
 */
@Schema(description = "Work order update request")
public record WorkOrderUpdateRequest(
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    @Schema(description = "Work order title", example = "Fix broken HVAC system")
    String title,
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    @Schema(description = "Work order description")
    String description,
    
    @Schema(description = "Work order priority", example = "HIGH")
    WorkOrderPriority priority,
    
    @Schema(description = "Work order status", example = "IN_PROGRESS")
    WorkOrderStatus status,
    
    @Schema(description = "Assigned technician ID")
    UUID assignedToId,
    
    @Schema(description = "Scheduled start date")
    LocalDateTime scheduledStart,
    
    @Schema(description = "Scheduled end date")
    LocalDateTime scheduledEnd,
    
    @Schema(description = "Estimated cost", example = "150.00")
    @DecimalMin(value = "0.0", message = "Estimated cost cannot be negative")
    @DecimalMax(value = "99999.99", message = "Estimated cost cannot exceed 99,999.99")
    @Digits(integer = 5, fraction = 2, message = "Estimated cost must have at most 5 integer digits and 2 decimal places")
    BigDecimal estimatedCost,
    
    @Schema(description = "Actual cost", example = "175.50")
    @DecimalMin(value = "0.0", message = "Actual cost cannot be negative")
    @DecimalMax(value = "99999.99", message = "Actual cost cannot exceed 99,999.99")
    @Digits(integer = 5, fraction = 2, message = "Actual cost must have at most 5 integer digits and 2 decimal places")
    BigDecimal actualCost,
    
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    @Schema(description = "Work order notes")
    String notes,
    
    @Size(max = 1000, message = "Completion notes cannot exceed 1000 characters")
    @Schema(description = "Completion notes")
    String completionNotes
) {
    
    /**
     * Check if any field is provided for update.
     */
    public boolean hasUpdates() {
        return title != null || description != null || priority != null || status != null ||
               assignedToId != null || scheduledStart != null || scheduledEnd != null ||
               estimatedCost != null || actualCost != null || notes != null || completionNotes != null;
    }
    
    /**
     * Check if this is a basic information update.
     */
    public boolean isBasicInfoUpdate() {
        return title != null || description != null || notes != null;
    }
    
    /**
     * Check if this is a scheduling update.
     */
    public boolean isSchedulingUpdate() {
        return scheduledStart != null || scheduledEnd != null;
    }
    
    /**
     * Check if this is an assignment update.
     */
    public boolean isAssignmentUpdate() {
        return assignedToId != null;
    }
    
    /**
     * Check if this is a status update.
     */
    public boolean isStatusUpdate() {
        return status != null;
    }
    
    /**
     * Check if this is a priority update.
     */
    public boolean isPriorityUpdate() {
        return priority != null;
    }
    
    /**
     * Check if this is a cost update.
     */
    public boolean isCostUpdate() {
        return estimatedCost != null || actualCost != null;
    }
    
    /**
     * Check if this is a completion update.
     */
    public boolean isCompletionUpdate() {
        return completionNotes != null || actualCost != null;
    }
    
    /**
     * Validate scheduled dates if both provided.
     */
    public boolean areScheduledDatesValid() {
        if (scheduledStart == null || scheduledEnd == null) {
            return true; // Only validate if both are provided
        }
        return scheduledEnd.isAfter(scheduledStart);
    }
    
    /**
     * Check if assignment is being cleared.
     */
    public boolean isAssignmentBeingCleared() {
        // Check if assignedToId is explicitly set to a special "clear" value
        // This would need to be handled at the service layer
        return false; // Simplified for now
    }
    
    /**
     * Check if this update requires approval.
     */
    public boolean requiresApproval() {
        return (status != null && status == WorkOrderStatus.COMPLETED) ||
               (actualCost != null && actualCost.compareTo(BigDecimal.valueOf(1000)) > 0);
    }
}