package com.cafm.cafmbackend.dto.mobile;

import com.cafm.cafmbackend.shared.enums.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for work order updates sent via WebSocket.
 * Used for real-time work order status changes and progress updates.
 */
public record WorkOrderUpdateDto(
        @NotNull
        UUID workOrderId,
        
        @NotNull
        WorkOrderStatus status,
        
        String notes,
        
        Double progressPercentage,
        
        UUID assignedTechnicianId,
        
        LocalDateTime updatedAt,
        
        String updateSource
) {
    public WorkOrderUpdateDto {
        // Validation
        if (workOrderId == null) {
            throw new IllegalArgumentException("Work order ID cannot be null");
        }
        
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        
        if (progressPercentage != null && (progressPercentage < 0 || progressPercentage > 100)) {
            throw new IllegalArgumentException("Progress percentage must be between 0 and 100");
        }
        
        // Set default values
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        
        if (updateSource == null) {
            updateSource = "SYSTEM";
        }
    }
}