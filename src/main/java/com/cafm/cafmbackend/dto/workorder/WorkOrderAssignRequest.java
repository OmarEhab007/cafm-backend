package com.cafm.cafmbackend.dto.workorder;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for assigning or reassigning work orders to technicians.
 * Used by admins and supervisors to manage work distribution.
 */
public record WorkOrderAssignRequest(
    @NotNull(message = "Technician ID is required")
    UUID assignedToId,
    
    // Assignment notes for the technician
    @Size(max = 500, message = "Assignment notes cannot exceed 500 characters")
    String assignmentNotes,
    
    // Schedule override (optional - uses existing if not provided)
    @Future(message = "Scheduled start must be in the future")
    LocalDateTime scheduledStart,
    
    @Future(message = "Scheduled end must be in the future")
    LocalDateTime scheduledEnd,
    
    // Priority override
    Boolean overridePriority,
    String newPriority,
    
    // Additional technicians for team work
    List<UUID> additionalTechnicianIds,
    
    // Special instructions
    String specialInstructions,
    
    // Required skills validation
    List<String> requiredSkills,
    
    // Notification preferences
    Boolean notifyTechnician,
    Boolean notifyPreviousAssignee,
    Boolean sendSms,
    Boolean sendPushNotification,
    
    // Reason for reassignment (if applicable)
    String reassignmentReason,
    
    // Urgency flag
    Boolean isUrgent
) {
    /**
     * Factory method for simple assignment
     */
    public static WorkOrderAssignRequest simple(UUID technicianId) {
        return new WorkOrderAssignRequest(
            technicianId, null, null, null,
            false, null, null, null, null,
            true, false, false, true, null, false
        );
    }
    
    /**
     * Factory method for urgent assignment with notification
     */
    public static WorkOrderAssignRequest urgent(
        UUID technicianId,
        String assignmentNotes,
        LocalDateTime scheduledStart
    ) {
        return new WorkOrderAssignRequest(
            technicianId, assignmentNotes, scheduledStart, null,
            true, "EMERGENCY", null, "Urgent - Please respond immediately", null,
            true, false, true, true, null, true
        );
    }
    
    /**
     * Factory method for team assignment
     */
    public static WorkOrderAssignRequest team(
        UUID leadTechnicianId,
        List<UUID> teamMemberIds,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd
    ) {
        return new WorkOrderAssignRequest(
            leadTechnicianId, "Team assignment - coordinate with team members",
            scheduledStart, scheduledEnd,
            false, null, teamMemberIds, null, null,
            true, false, false, true, null, false
        );
    }
    
    /**
     * Factory method for reassignment
     */
    public static WorkOrderAssignRequest reassign(
        UUID newTechnicianId,
        String reassignmentReason,
        Boolean notifyPrevious
    ) {
        return new WorkOrderAssignRequest(
            newTechnicianId, null, null, null,
            false, null, null, null, null,
            true, notifyPrevious, false, true,
            reassignmentReason, false
        );
    }
    
    /**
     * Validate schedule if provided
     */
    public boolean isScheduleValid() {
        if (scheduledStart == null || scheduledEnd == null) {
            return true; // Schedule is optional
        }
        return scheduledEnd.isAfter(scheduledStart);
    }
    
    /**
     * Check if this is a reassignment (has reason)
     */
    public boolean isReassignment() {
        return reassignmentReason != null && !reassignmentReason.isBlank();
    }
    
    /**
     * Check if this is a team assignment
     */
    public boolean isTeamAssignment() {
        return additionalTechnicianIds != null && !additionalTechnicianIds.isEmpty();
    }
    
    /**
     * Get total number of assigned technicians
     */
    public int getTotalAssignedCount() {
        int count = 1; // Primary assignee
        if (additionalTechnicianIds != null) {
            count += additionalTechnicianIds.size();
        }
        return count;
    }
}