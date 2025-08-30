package com.cafm.cafmbackend.dto.mobile;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO representing the result of work order assignment optimization.
 * Contains assignment details and optimization metrics.
 */
public record WorkOrderAssignmentResult(
        @NotNull
        UUID workOrderId,
        
        @NotNull
        UUID assignedTechnicianId,
        
        double optimizationScore,
        
        int estimatedTravelTime,
        
        String reasonCode,
        
        String assignmentMethod,
        
        Double skillMatchScore,
        
        Double distanceScore,
        
        Double workloadScore,
        
        Double priorityScore
) {
    public WorkOrderAssignmentResult {
        // Validation
        if (workOrderId == null) {
            throw new IllegalArgumentException("Work order ID cannot be null");
        }
        
        if (assignedTechnicianId == null) {
            throw new IllegalArgumentException("Assigned technician ID cannot be null");
        }
        
        if (optimizationScore < 0 || optimizationScore > 1) {
            throw new IllegalArgumentException("Optimization score must be between 0 and 1");
        }
        
        if (estimatedTravelTime < 0) {
            throw new IllegalArgumentException("Estimated travel time cannot be negative");
        }
        
        // Set default values
        if (reasonCode == null) {
            reasonCode = "AUTO_ASSIGNMENT";
        }
        
        if (assignmentMethod == null) {
            assignmentMethod = "OPTIMIZATION_ALGORITHM";
        }
    }
    
    // Convenience constructor for basic assignment
    public WorkOrderAssignmentResult(UUID workOrderId, UUID assignedTechnicianId, 
                                   double optimizationScore, int estimatedTravelTime, 
                                   String reasonCode) {
        this(workOrderId, assignedTechnicianId, optimizationScore, estimatedTravelTime, 
             reasonCode, "OPTIMIZATION_ALGORITHM", null, null, null, null);
    }
}