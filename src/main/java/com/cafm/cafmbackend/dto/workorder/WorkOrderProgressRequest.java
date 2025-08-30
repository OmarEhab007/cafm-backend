package com.cafm.cafmbackend.dto.workorder;

import com.cafm.cafmbackend.shared.enums.WorkOrderStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for technicians to update work order progress.
 * Used in mobile app and web portal for field updates.
 */
public record WorkOrderProgressRequest(
    @NotNull(message = "Status is required")
    WorkOrderStatus status,
    
    // Progress notes
    @Size(max = 1000, message = "Progress notes cannot exceed 1000 characters")
    String progressNotes,
    
    // Completed tasks
    List<TaskProgress> completedTasks,
    
    // Time tracking
    @DecimalMin(value = "0.0", message = "Actual hours cannot be negative")
    @DecimalMax(value = "999.9", message = "Actual hours cannot exceed 999.9")
    BigDecimal actualHours,
    
    @DecimalMin(value = "0", message = "Progress percentage must be between 0 and 100")
    @DecimalMax(value = "100", message = "Progress percentage must be between 0 and 100")
    Integer progressPercentage,
    
    // Materials used
    List<MaterialUsed> materialsUsed,
    
    // Photos of work progress
    List<ProgressPhoto> photos,
    
    // Issues encountered
    String issuesEncountered,
    Boolean requiresAdditionalParts,
    Boolean requiresAdditionalTime,
    
    // Safety incidents
    Boolean safetyIncidentOccurred,
    String safetyIncidentDetails,
    
    // Completion details (when status is COMPLETED)
    String completionNotes,
    LocalDateTime actualCompletionTime,
    
    // Verification request
    Boolean requestVerification,
    UUID requestVerificationFrom,
    
    // Hold details (when status is ON_HOLD)
    String holdReason,
    LocalDateTime expectedResumeDate,
    
    // Customer interaction
    String customerContactName,
    String customerSignature, // Base64 encoded signature
    Boolean customerSatisfied,
    String customerFeedback
) {
    /**
     * Task progress update
     */
    public record TaskProgress(
        @NotNull UUID taskId,
        @NotNull Boolean completed,
        BigDecimal actualHours,
        String notes
    ) {}
    
    /**
     * Material used entry
     */
    public record MaterialUsed(
        @NotNull String name,
        @NotNull String unit,
        @NotNull BigDecimal quantity,
        BigDecimal unitCost,
        UUID inventoryItemId,
        String notes
    ) {}
    
    /**
     * Progress photo
     */
    public record ProgressPhoto(
        @NotNull String url,
        String caption,
        @NotNull LocalDateTime takenAt,
        String photoType // before, during, after
    ) {}
    
    /**
     * Factory method for starting work
     */
    public static WorkOrderProgressRequest startWork(String progressNotes) {
        return new WorkOrderProgressRequest(
            WorkOrderStatus.IN_PROGRESS, progressNotes, null,
            null, 0, null, null, null, false, false,
            false, null, null, null, false, null,
            null, null, null, null, false, null
        );
    }
    
    /**
     * Factory method for putting work on hold
     */
    public static WorkOrderProgressRequest putOnHold(
        String holdReason,
        LocalDateTime expectedResumeDate,
        String issuesEncountered
    ) {
        return new WorkOrderProgressRequest(
            WorkOrderStatus.ON_HOLD, null, null,
            null, null, null, null, issuesEncountered, false, false,
            false, null, null, null, false, null,
            holdReason, expectedResumeDate, null, null, false, null
        );
    }
    
    /**
     * Factory method for completing work
     */
    public static WorkOrderProgressRequest complete(
        String completionNotes,
        List<TaskProgress> completedTasks,
        BigDecimal actualHours,
        List<MaterialUsed> materialsUsed,
        String customerSignature
    ) {
        return new WorkOrderProgressRequest(
            WorkOrderStatus.COMPLETED, completionNotes, completedTasks,
            actualHours, 100, materialsUsed, null, null, false, false,
            false, null, completionNotes, LocalDateTime.now(),
            true, null, null, null, null, customerSignature, true, null
        );
    }
    
    /**
     * Factory method for progress update
     */
    public static WorkOrderProgressRequest updateProgress(
        Integer progressPercentage,
        List<TaskProgress> completedTasks,
        BigDecimal hoursWorked,
        String progressNotes
    ) {
        return new WorkOrderProgressRequest(
            WorkOrderStatus.IN_PROGRESS, progressNotes, completedTasks,
            hoursWorked, progressPercentage, null, null, null, false, false,
            false, null, null, null, false, null,
            null, null, null, null, false, null
        );
    }
    
    /**
     * Validate completion requirements
     */
    public boolean isValidCompletion() {
        if (status != WorkOrderStatus.COMPLETED) {
            return true;
        }
        return completionNotes != null && !completionNotes.isBlank() &&
               actualHours != null && actualHours.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Validate hold requirements
     */
    public boolean isValidHold() {
        if (status != WorkOrderStatus.ON_HOLD) {
            return true;
        }
        return holdReason != null && !holdReason.isBlank();
    }
    
    /**
     * Calculate total material cost
     */
    public BigDecimal calculateMaterialCost() {
        if (materialsUsed == null || materialsUsed.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return materialsUsed.stream()
            .filter(m -> m.quantity != null && m.unitCost != null)
            .map(m -> m.quantity.multiply(m.unitCost))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Check if additional resources are needed
     */
    public boolean needsAdditionalResources() {
        return Boolean.TRUE.equals(requiresAdditionalParts) || 
               Boolean.TRUE.equals(requiresAdditionalTime);
    }
}