package com.cafm.cafmbackend.dto.workorder;

import com.cafm.cafmbackend.shared.enums.WorkOrderPriority;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for creating work orders from reports or directly.
 * Used by admins to convert approved reports into actionable work orders.
 */
public record WorkOrderCreateRequest(
    // Link to report (optional - can create standalone work order)
    UUID reportId,
    
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    String title,
    
    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    String description,
    
    @NotNull(message = "Priority is required")
    WorkOrderPriority priority,
    
    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category cannot exceed 50 characters")
    String category,
    
    // School information (required if not linked to report)
    UUID schoolId,
    
    // Location details
    String location,
    String building,
    String floor,
    String roomNumber,
    
    // Assignment
    UUID assignedToId,
    String assignmentNotes,
    
    // Scheduling
    @NotNull(message = "Scheduled start date is required")
    @Future(message = "Scheduled start must be in the future")
    LocalDateTime scheduledStart,
    
    @NotNull(message = "Scheduled end date is required")
    @Future(message = "Scheduled end must be in the future")
    LocalDateTime scheduledEnd,
    
    // Cost estimation
    @DecimalMin(value = "0.0", message = "Estimated cost cannot be negative")
    @DecimalMax(value = "999999.99", message = "Estimated cost cannot exceed 999,999.99")
    BigDecimal estimatedCost,
    
    @DecimalMin(value = "0.1", message = "Estimated hours must be positive")
    @DecimalMax(value = "999.9", message = "Estimated hours cannot exceed 999.9")
    BigDecimal estimatedHours,
    
    // Initial task checklist
    List<TaskItem> tasks,
    
    // Special requirements
    String specialInstructions,
    List<String> requiredSkills,
    List<String> requiredTools,
    
    // Safety requirements
    Boolean requiresSafetyGear,
    String safetyNotes,
    
    // Materials needed (preliminary list)
    List<MaterialItem> estimatedMaterials,
    
    // Notifications
    Boolean notifyTechnician,
    Boolean notifySchool,
    
    // Asset reference (if maintenance is for specific asset)
    UUID assetId
) {
    /**
     * Task item for initial checklist
     */
    public record TaskItem(
        @NotBlank(message = "Task title is required")
        String title,
        String description,
        Boolean isMandatory,
        BigDecimal estimatedHours,
        Integer orderNumber
    ) {}
    
    /**
     * Material item for estimation
     */
    public record MaterialItem(
        String name,
        String unit,
        BigDecimal quantity,
        BigDecimal estimatedCost
    ) {}
    
    /**
     * Factory method to create work order from approved report
     */
    public static WorkOrderCreateRequest fromReport(
        UUID reportId,
        String title,
        String description,
        WorkOrderPriority priority,
        String category,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd,
        BigDecimal estimatedCost
    ) {
        return new WorkOrderCreateRequest(
            reportId, title, description, priority, category,
            null, null, null, null, null,
            null, null,
            scheduledStart, scheduledEnd,
            estimatedCost, null,
            List.of(), null, null, null,
            false, null, null,
            true, true, null
        );
    }
    
    /**
     * Factory method for emergency work order
     */
    public static WorkOrderCreateRequest emergency(
        String title,
        String description,
        UUID schoolId,
        UUID technicianId,
        String location
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new WorkOrderCreateRequest(
            null, title, description, WorkOrderPriority.EMERGENCY, "emergency",
            schoolId, location, null, null, null,
            technicianId, "Emergency assignment - immediate response required",
            now.plusHours(1), now.plusHours(4),
            null, new BigDecimal("4.0"),
            List.of(), "URGENT: Safety hazard - respond immediately", null, null,
            true, "Follow all safety protocols", null,
            true, true, null
        );
    }
    
    /**
     * Factory method for preventive maintenance
     */
    public static WorkOrderCreateRequest preventiveMaintenance(
        String title,
        UUID assetId,
        UUID schoolId,
        LocalDateTime scheduledDate,
        List<TaskItem> maintenanceTasks
    ) {
        return new WorkOrderCreateRequest(
            null, title, "Scheduled preventive maintenance", 
            WorkOrderPriority.LOW, "preventive",
            schoolId, null, null, null, null,
            null, null,
            scheduledDate, scheduledDate.plusHours(2),
            null, new BigDecimal("2.0"),
            maintenanceTasks, null, null, null,
            false, null, null,
            false, true, assetId
        );
    }
    
    /**
     * Validate that scheduled end is after scheduled start
     */
    public boolean isScheduleValid() {
        return scheduledStart != null && scheduledEnd != null && 
               scheduledEnd.isAfter(scheduledStart);
    }
    
    /**
     * Validate that either reportId or schoolId is provided
     */
    public boolean hasValidLocation() {
        return reportId != null || schoolId != null;
    }
    
    /**
     * Calculate total estimated cost including materials
     */
    public BigDecimal calculateTotalEstimatedCost() {
        BigDecimal total = estimatedCost != null ? estimatedCost : BigDecimal.ZERO;
        
        if (estimatedMaterials != null) {
            for (MaterialItem material : estimatedMaterials) {
                if (material.estimatedCost != null) {
                    total = total.add(material.estimatedCost);
                }
            }
        }
        
        return total;
    }
}