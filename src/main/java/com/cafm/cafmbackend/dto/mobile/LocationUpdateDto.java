package com.cafm.cafmbackend.dto.mobile;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * DTO for location updates sent via WebSocket.
 * Used for real-time tracking of supervisors and technicians.
 */
public record LocationUpdateDto(
        @NotNull
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        Double latitude,
        
        @NotNull
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        Double longitude,
        
        Float accuracy,
        
        LocalDateTime timestamp,
        
        Integer batteryLevel,
        
        String deviceId,
        
        Boolean isMoving
) {
    public LocationUpdateDto {
        // Validation
        if (latitude == null) {
            throw new IllegalArgumentException("Latitude cannot be null");
        }
        
        if (longitude == null) {
            throw new IllegalArgumentException("Longitude cannot be null");
        }
        
        // Set default values
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        
        if (accuracy == null) {
            accuracy = 10.0f; // Default accuracy in meters
        }
        
        if (batteryLevel != null && (batteryLevel < 0 || batteryLevel > 100)) {
            throw new IllegalArgumentException("Battery level must be between 0 and 100");
        }
        
        if (isMoving == null) {
            isMoving = false;
        }
    }
}