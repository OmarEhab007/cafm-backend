package com.cafm.cafmbackend.dto.notification;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request for sending push notifications to mobile devices.
 * Supports FCM (Firebase Cloud Messaging) for both iOS and Android.
 */
public record PushNotificationRequest(
    // Target specification
    @NotNull(message = "Target type is required")
    TargetType targetType,
    
    List<String> deviceTokens,
    List<UUID> userIds,
    List<UUID> groupIds,
    String topic,
    
    // Notification content
    @NotNull(message = "Notification is required")
    NotificationContent notification,
    
    // Data payload for app processing
    Map<String, String> data,
    
    // Delivery options
    DeliveryOptions options,
    
    // Tracking
    String campaignId,
    String correlationId
) {
    /**
     * Target type for notification
     */
    public enum TargetType {
        DEVICE,     // Specific device tokens
        USER,       // All devices for specific users
        GROUP,      // All users in groups
        TOPIC,      // Topic subscription
        BROADCAST   // All users
    }
    
    /**
     * Notification content
     */
    public record NotificationContent(
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title cannot exceed 100 characters")
        String title,
        
        @NotBlank(message = "Body is required")
        @Size(max = 500, message = "Body cannot exceed 500 characters")
        String body,
        
        String titleLocKey,
        List<String> titleLocArgs,
        String bodyLocKey,
        List<String> bodyLocArgs,
        
        // iOS specific
        String subtitle,
        String badge,
        String sound,
        String category,
        String threadId,
        
        // Android specific
        String androidChannelId,
        String icon,
        String color,
        String tag,
        String clickAction,
        
        // Rich content
        String imageUrl,
        List<ActionButton> actions
    ) {
        public record ActionButton(
            String id,
            String title,
            String action,
            String icon,
            Map<String, String> extras
        ) {}
        
        /**
         * Factory method for simple notification
         */
        public static NotificationContent simple(String title, String body) {
            return new NotificationContent(
                title, body,
                null, null, null, null,
                null, null, "default", null, null,
                "default", null, null, null, null,
                null, null
            );
        }
        
        /**
         * Factory method for work order notification
         */
        public static NotificationContent workOrder(
            String workOrderNumber,
            String action
        ) {
            return new NotificationContent(
                "Work Order " + action,
                "Work order #" + workOrderNumber + " has been " + action.toLowerCase(),
                null, null, null, null,
                null, "1", "work_order.caf", "work_order", workOrderNumber,
                "work_orders", "ic_work_order", "#FF6B6B", workOrderNumber, "OPEN_WORK_ORDER",
                null, null
            );
        }
        
        /**
         * Factory method for report notification
         */
        public static NotificationContent report(
            String reportNumber,
            String status
        ) {
            return new NotificationContent(
                "Report Status Update",
                "Report #" + reportNumber + " is now " + status,
                null, null, null, null,
                null, "1", "report.caf", "report", reportNumber,
                "reports", "ic_report", "#4ECDC4", reportNumber, "OPEN_REPORT",
                null, null
            );
        }
    }
    
    /**
     * Delivery options
     */
    public record DeliveryOptions(
        // Priority
        Priority priority,
        
        // Timing
        LocalDateTime sendAt,
        LocalDateTime expiresAt,
        Integer ttlSeconds,
        
        // Delivery behavior
        Boolean contentAvailable,
        Boolean mutableContent,
        Boolean dryRun,
        
        // Retry policy
        Integer maxRetries,
        Integer retryDelaySeconds,
        
        // Platform specific
        Boolean iosOnly,
        Boolean androidOnly,
        
        // Conditions
        String condition,
        Map<String, String> headers
    ) {
        public enum Priority {
            HIGH,
            NORMAL,
            LOW
        }
        
        /**
         * Factory method for immediate high priority
         */
        public static DeliveryOptions immediate() {
            return new DeliveryOptions(
                Priority.HIGH,
                null, null, 86400,
                true, true, false,
                3, 60,
                false, false,
                null, null
            );
        }
        
        /**
         * Factory method for scheduled notification
         */
        public static DeliveryOptions scheduled(LocalDateTime sendAt) {
            return new DeliveryOptions(
                Priority.NORMAL,
                sendAt, sendAt.plusDays(1), 86400,
                false, false, false,
                3, 60,
                false, false,
                null, null
            );
        }
    }
    
    /**
     * Factory method for user notification
     */
    public static PushNotificationRequest toUser(
        UUID userId,
        String title,
        String body,
        Map<String, String> data
    ) {
        return new PushNotificationRequest(
            TargetType.USER,
            null,
            List.of(userId),
            null,
            null,
            NotificationContent.simple(title, body),
            data,
            DeliveryOptions.immediate(),
            null,
            null
        );
    }
    
    /**
     * Factory method for device notification
     */
    public static PushNotificationRequest toDevice(
        String deviceToken,
        NotificationContent content,
        Map<String, String> data
    ) {
        return new PushNotificationRequest(
            TargetType.DEVICE,
            List.of(deviceToken),
            null,
            null,
            null,
            content,
            data,
            DeliveryOptions.immediate(),
            null,
            null
        );
    }
    
    /**
     * Factory method for topic notification
     */
    public static PushNotificationRequest toTopic(
        String topic,
        String title,
        String body
    ) {
        return new PushNotificationRequest(
            TargetType.TOPIC,
            null,
            null,
            null,
            topic,
            NotificationContent.simple(title, body),
            null,
            DeliveryOptions.immediate(),
            null,
            null
        );
    }
    
    /**
     * Validate target specification
     */
    public boolean isValidTarget() {
        return switch (targetType) {
            case DEVICE -> deviceTokens != null && !deviceTokens.isEmpty();
            case USER -> userIds != null && !userIds.isEmpty();
            case GROUP -> groupIds != null && !groupIds.isEmpty();
            case TOPIC -> topic != null && !topic.isBlank();
            case BROADCAST -> true;
        };
    }
}