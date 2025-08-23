package com.cafm.cafmbackend.dto.notification;

import com.cafm.cafmbackend.data.entity.NotificationQueue.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for notification data.
 */
public record NotificationResponse(
    UUID id,
    
    UUID userId,
    String userName,
    
    String topic,
    
    String title,
    String body,
    
    Map<String, Object> data,
    
    NotificationType notificationType,
    
    String category,
    
    NotificationPriority priority,
    
    Platform platform,
    
    String sound,
    Integer badge,
    
    NotificationStatus status,
    
    Boolean processed,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime sentAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime failedAt,
    
    Integer retryCount,
    Integer maxRetries,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime nextRetryAt,
    
    String errorMessage,
    String errorCode,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime scheduledFor,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime expiresAt,
    
    String fcmMessageId,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    String createdBy,
    
    NotificationMetrics metrics
) {
    /**
     * Notification delivery metrics.
     */
    public record NotificationMetrics(
        String deliveryStatus,
        Long deliveryTimeMs,
        Boolean isExpired,
        Boolean canRetry,
        Integer attemptsRemaining,
        String priorityLevel
    ) {
        public static NotificationMetrics calculate(
                NotificationResponse response) {
            
            String deliveryStatus = determineDeliveryStatus(
                response.status(),
                response.processed()
            );
            
            Long deliveryTimeMs = null;
            if (response.sentAt() != null && response.createdAt() != null) {
                deliveryTimeMs = java.time.Duration.between(
                    response.createdAt(),
                    response.sentAt()
                ).toMillis();
            }
            
            boolean isExpired = response.expiresAt() != null &&
                              LocalDateTime.now().isAfter(response.expiresAt());
            
            boolean canRetry = response.retryCount() < response.maxRetries() && !isExpired;
            
            int attemptsRemaining = Math.max(0, 
                response.maxRetries() - response.retryCount());
            
            String priorityLevel = determinePriorityLevel(response.priority());
            
            return new NotificationMetrics(
                deliveryStatus,
                deliveryTimeMs,
                isExpired,
                canRetry,
                attemptsRemaining,
                priorityLevel
            );
        }
        
        private static String determineDeliveryStatus(
                NotificationStatus status,
                Boolean processed) {
            
            if (status == NotificationStatus.SENT) return "DELIVERED";
            if (status == NotificationStatus.FAILED) return "FAILED";
            if (status == NotificationStatus.EXPIRED) return "EXPIRED";
            if (status == NotificationStatus.CANCELLED) return "CANCELLED";
            if (status == NotificationStatus.RETRY) return "RETRYING";
            if (processed) return "PROCESSING";
            return "PENDING";
        }
        
        private static String determinePriorityLevel(NotificationPriority priority) {
            if (priority == null) return "NORMAL";
            
            return switch (priority) {
                case URGENT -> "IMMEDIATE";
                case HIGH -> "HIGH";
                case NORMAL -> "NORMAL";
                case LOW -> "BATCH";
            };
        }
    }
}