package com.cafm.cafmbackend.dto.notification;

import com.cafm.cafmbackend.data.entity.NotificationQueue.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating notifications.
 */
public record NotificationRequest(
    UUID userId,
    
    List<UUID> userIds,
    
    String topic,
    
    @NotNull(message = "Title is required")
    @Size(min = 1, max = 500, message = "Title must be between 1 and 500 characters")
    String title,
    
    @NotNull(message = "Body is required")
    @Size(min = 1, max = 5000, message = "Body must be between 1 and 5000 characters")
    String body,
    
    Map<String, Object> data,
    
    NotificationType notificationType,
    
    String category,
    
    NotificationPriority priority,
    
    Platform platform,
    
    String sound,
    
    Integer badge,
    
    LocalDateTime scheduledFor,
    
    LocalDateTime expiresAt,
    
    Boolean sendEmail,
    
    Boolean sendPush,
    
    Boolean sendInApp
) {
    /**
     * Creates a simple notification for a single user.
     */
    public static NotificationRequest simple(
            UUID userId,
            String title,
            String body) {
        return new NotificationRequest(
            userId,
            null,
            null,
            title,
            body,
            Map.of(),
            NotificationType.SYSTEM,
            null,
            NotificationPriority.NORMAL,
            null,
            null,
            null,
            null,
            null,
            false,
            true,
            true
        );
    }
    
    /**
     * Creates a broadcast notification to a topic.
     */
    public static NotificationRequest broadcast(
            String topic,
            String title,
            String body,
            NotificationPriority priority) {
        return new NotificationRequest(
            null,
            null,
            topic,
            title,
            body,
            Map.of(),
            NotificationType.BROADCAST,
            null,
            priority,
            null,
            "default",
            null,
            null,
            LocalDateTime.now().plusDays(7),
            false,
            true,
            true
        );
    }
    
    /**
     * Creates a report notification.
     */
    public static NotificationRequest reportNotification(
            UUID userId,
            UUID reportId,
            String reportTitle,
            String status) {
        return new NotificationRequest(
            userId,
            null,
            null,
            "Report Update: " + reportTitle,
            "Your report status has been updated to: " + status,
            Map.of(
                "reportId", reportId.toString(),
                "status", status,
                "type", "report_update"
            ),
            NotificationType.REPORT,
            "report",
            NotificationPriority.NORMAL,
            null,
            "notification",
            null,
            null,
            LocalDateTime.now().plusDays(3),
            false,
            true,
            true
        );
    }
    
    /**
     * Creates a work order notification.
     */
    public static NotificationRequest workOrderNotification(
            UUID userId,
            UUID workOrderId,
            String workOrderTitle,
            NotificationPriority priority) {
        return new NotificationRequest(
            userId,
            null,
            null,
            "New Work Order Assigned",
            "You have been assigned a new work order: " + workOrderTitle,
            Map.of(
                "workOrderId", workOrderId.toString(),
                "title", workOrderTitle,
                "type", "work_order_assigned"
            ),
            NotificationType.WORK_ORDER,
            "work_order",
            priority,
            null,
            "work_order",
            1,
            null,
            LocalDateTime.now().plusDays(1),
            true,
            true,
            true
        );
    }
    
    /**
     * Builder for complex notifications.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private UUID userId;
        private List<UUID> userIds;
        private String topic;
        private String title;
        private String body;
        private Map<String, Object> data = Map.of();
        private NotificationType notificationType = NotificationType.SYSTEM;
        private String category;
        private NotificationPriority priority = NotificationPriority.NORMAL;
        private Platform platform;
        private String sound;
        private Integer badge;
        private LocalDateTime scheduledFor;
        private LocalDateTime expiresAt;
        private Boolean sendEmail = false;
        private Boolean sendPush = true;
        private Boolean sendInApp = true;
        
        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder userIds(List<UUID> userIds) {
            this.userIds = userIds;
            return this;
        }
        
        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder body(String body) {
            this.body = body;
            return this;
        }
        
        public Builder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }
        
        public Builder notificationType(NotificationType notificationType) {
            this.notificationType = notificationType;
            return this;
        }
        
        public Builder category(String category) {
            this.category = category;
            return this;
        }
        
        public Builder priority(NotificationPriority priority) {
            this.priority = priority;
            return this;
        }
        
        public Builder platform(Platform platform) {
            this.platform = platform;
            return this;
        }
        
        public Builder sound(String sound) {
            this.sound = sound;
            return this;
        }
        
        public Builder badge(Integer badge) {
            this.badge = badge;
            return this;
        }
        
        public Builder scheduledFor(LocalDateTime scheduledFor) {
            this.scheduledFor = scheduledFor;
            return this;
        }
        
        public Builder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        
        public Builder sendEmail(Boolean sendEmail) {
            this.sendEmail = sendEmail;
            return this;
        }
        
        public Builder sendPush(Boolean sendPush) {
            this.sendPush = sendPush;
            return this;
        }
        
        public Builder sendInApp(Boolean sendInApp) {
            this.sendInApp = sendInApp;
            return this;
        }
        
        public NotificationRequest build() {
            return new NotificationRequest(
                userId,
                userIds,
                topic,
                title,
                body,
                data,
                notificationType,
                category,
                priority,
                platform,
                sound,
                badge,
                scheduledFor,
                expiresAt,
                sendEmail,
                sendPush,
                sendInApp
            );
        }
    }
}