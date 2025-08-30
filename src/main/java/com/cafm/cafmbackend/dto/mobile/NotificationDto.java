package com.cafm.cafmbackend.dto.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for notifications sent via WebSocket.
 * Used for system alerts, messages, and real-time communications.
 */
public record NotificationDto(
        @NotNull
        UUID id,
        
        @NotBlank
        String type,
        
        @NotBlank
        String message,
        
        UUID recipientId,
        
        LocalDateTime timestamp,
        
        Boolean isRead,
        
        String priority,
        
        UUID workOrderId,
        
        UUID senderId,
        
        String category
) {
    public NotificationDto {
        // Validation
        if (id == null) {
            throw new IllegalArgumentException("Notification ID cannot be null");
        }
        
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Notification type cannot be null or empty");
        }
        
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        
        // Set default values
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        
        if (isRead == null) {
            isRead = false;
        }
        
        if (priority == null) {
            priority = "NORMAL";
        }
        
        if (category == null) {
            category = "GENERAL";
        }
    }
    
    // Convenience constructors for common notification types
    public static NotificationDto workOrderNotification(UUID workOrderId, String message) {
        return new NotificationDto(
                UUID.randomUUID(),
                "WORK_ORDER_UPDATE",
                message,
                null,
                LocalDateTime.now(),
                false,
                "NORMAL",
                workOrderId,
                null,
                "WORK_ORDER"
        );
    }
    
    public static NotificationDto systemAlert(String message, String priority) {
        return new NotificationDto(
                UUID.randomUUID(),
                "SYSTEM_ALERT",
                message,
                null,
                LocalDateTime.now(),
                false,
                priority,
                null,
                null,
                "SYSTEM"
        );
    }
    
    public static NotificationDto instantMessage(UUID senderId, UUID recipientId, String message) {
        return new NotificationDto(
                UUID.randomUUID(),
                "INSTANT_MESSAGE",
                message,
                recipientId,
                LocalDateTime.now(),
                false,
                "NORMAL",
                null,
                senderId,
                "MESSAGE"
        );
    }
}