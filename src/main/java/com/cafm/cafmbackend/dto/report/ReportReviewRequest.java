package com.cafm.cafmbackend.dto.report;

import com.cafm.cafmbackend.data.enums.ReportStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for admin to review and approve/reject reports.
 * Used when transitioning report status and providing review feedback.
 */
public record ReportReviewRequest(
    @NotNull(message = "Status is required")
    ReportStatus status,
    
    // Review notes (required for rejection, optional for approval)
    @Size(max = 1000, message = "Review notes cannot exceed 1000 characters")
    String reviewNotes,
    
    // Rejection specific
    @Size(max = 500, message = "Rejection reason cannot exceed 500 characters")
    String rejectionReason,
    
    // Approval specific fields
    @DecimalMin(value = "0.0", message = "Estimated cost cannot be negative")
    @DecimalMax(value = "999999.99", message = "Estimated cost cannot exceed 999,999.99")
    BigDecimal estimatedCost,
    
    @Future(message = "Scheduled date must be in the future")
    LocalDate scheduledDate,
    
    // Assignment (optional - can assign technician during approval)
    UUID assignToTechnicianId,
    
    // Priority override
    Boolean overridePriority,
    String newPriority,
    
    // Work order creation
    Boolean createWorkOrder,
    String workOrderCategory,
    
    // Additional instructions
    @Size(max = 500, message = "Instructions cannot exceed 500 characters")
    String specialInstructions,
    
    // Notify parties
    Boolean notifySupervisor,
    Boolean notifyTechnician,
    Boolean notifySchool
) {
    /**
     * Factory method for quick approval
     */
    public static ReportReviewRequest approve(
        BigDecimal estimatedCost,
        LocalDate scheduledDate
    ) {
        return new ReportReviewRequest(
            ReportStatus.APPROVED,
            null,
            null,
            estimatedCost,
            scheduledDate,
            null,
            false,
            null,
            true,
            null,
            null,
            true,
            false,
            false
        );
    }
    
    /**
     * Factory method for rejection
     */
    public static ReportReviewRequest reject(
        String rejectionReason,
        String reviewNotes
    ) {
        return new ReportReviewRequest(
            ReportStatus.REJECTED,
            reviewNotes,
            rejectionReason,
            null,
            null,
            null,
            false,
            null,
            false,
            null,
            null,
            true,
            false,
            false
        );
    }
    
    /**
     * Factory method for approval with immediate work order and assignment
     */
    public static ReportReviewRequest approveAndAssign(
        BigDecimal estimatedCost,
        LocalDate scheduledDate,
        UUID technicianId,
        String specialInstructions
    ) {
        return new ReportReviewRequest(
            ReportStatus.APPROVED,
            null,
            null,
            estimatedCost,
            scheduledDate,
            technicianId,
            false,
            null,
            true,
            null,
            specialInstructions,
            true,
            true,
            true
        );
    }
    
    /**
     * Validation: rejection must have a reason
     */
    public boolean isValidRejection() {
        return status != ReportStatus.REJECTED || 
               (rejectionReason != null && !rejectionReason.isBlank());
    }
    
    /**
     * Validation: approval should have cost estimate
     */
    public boolean isValidApproval() {
        return status != ReportStatus.APPROVED || estimatedCost != null;
    }
    
    /**
     * Check if this is a status transition that requires work order
     */
    public boolean shouldCreateWorkOrder() {
        return Boolean.TRUE.equals(createWorkOrder) || 
               (status == ReportStatus.APPROVED && assignToTechnicianId != null);
    }
}