package com.cafm.cafmbackend.data.entity;

import com.cafm.cafmbackend.data.entity.base.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a notification in the queue for push notifications.
 * Supports retry logic, scheduling, and platform-specific features.
 */
@Entity
@Table(name = "notification_queue")
public class NotificationQueue extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "device_token")
    private String deviceToken;

    @Column(name = "topic", length = 100)
    private String topic;

    @NotNull
    @Column(name = "title", nullable = false, length = 500)
    @Size(max = 500)
    private String title;

    @NotNull
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Type(JsonType.class)
    @Column(name = "data", columnDefinition = "jsonb")
    private Map<String, Object> data = new HashMap<>();

    @Column(name = "notification_type", length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "priority", length = 20)
    @Enumerated(EnumType.STRING)
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Column(name = "platform", length = 20)
    @Enumerated(EnumType.STRING)
    private Platform platform;

    @Column(name = "sound", length = 100)
    private String sound;

    @Column(name = "badge")
    private Integer badge;

    @NotNull
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "processed")
    private Boolean processed = false;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "scheduled_for")
    private LocalDateTime scheduledFor;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "fcm_message_id")
    private String fcmMessageId;

    @Type(JsonType.class)
    @Column(name = "fcm_response", columnDefinition = "jsonb")
    private Map<String, Object> fcmResponse;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user")
    private User createdByUser;

    public NotificationQueue() {
        super();
    }

    private NotificationQueue(Builder builder) {
        this.user = builder.user;
        this.deviceToken = builder.deviceToken;
        this.topic = builder.topic;
        this.title = builder.title;
        this.body = builder.body;
        this.data = builder.data;
        this.notificationType = builder.notificationType;
        this.category = builder.category;
        this.priority = builder.priority;
        this.platform = builder.platform;
        this.sound = builder.sound;
        this.badge = builder.badge;
        this.status = builder.status;
        this.scheduledFor = builder.scheduledFor;
        this.expiresAt = builder.expiresAt;
        this.company = builder.company;
        this.createdByUser = builder.createdByUser;
    }

    public void markAsSent(String messageId, Map<String, Object> response) {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.processed = true;
        this.fcmMessageId = messageId;
        this.fcmResponse = response;
    }

    public void markAsFailed(String errorMessage, String errorCode) {
        this.status = NotificationStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.retryCount++;
        
        if (retryCount < maxRetries) {
            this.status = NotificationStatus.RETRY;
            // Exponential backoff: 5 min, 15 min, 45 min
            int delayMinutes = 5 * (int) Math.pow(3, retryCount - 1);
            this.nextRetryAt = LocalDateTime.now().plusMinutes(delayMinutes);
        }
    }

    public boolean canRetry() {
        return retryCount < maxRetries && !isExpired();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isScheduled() {
        return scheduledFor != null && LocalDateTime.now().isBefore(scheduledFor);
    }

    public void addData(String key, Object value) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put(key, value);
    }

    // Getters and Setters
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public NotificationPriority getPriority() {
        return priority;
    }

    public void setPriority(NotificationPriority priority) {
        this.priority = priority;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public String getSound() {
        return sound;
    }

    public void setSound(String sound) {
        this.sound = sound;
    }

    public Integer getBadge() {
        return badge;
    }

    public void setBadge(Integer badge) {
        this.badge = badge;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public Boolean getProcessed() {
        return processed;
    }

    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(LocalDateTime failedAt) {
        this.failedAt = failedAt;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public LocalDateTime getScheduledFor() {
        return scheduledFor;
    }

    public void setScheduledFor(LocalDateTime scheduledFor) {
        this.scheduledFor = scheduledFor;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getFcmMessageId() {
        return fcmMessageId;
    }

    public void setFcmMessageId(String fcmMessageId) {
        this.fcmMessageId = fcmMessageId;
    }

    public Map<String, Object> getFcmResponse() {
        return fcmResponse;
    }

    public void setFcmResponse(Map<String, Object> fcmResponse) {
        this.fcmResponse = fcmResponse;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private User user;
        private String deviceToken;
        private String topic;
        private String title;
        private String body;
        private Map<String, Object> data = new HashMap<>();
        private NotificationType notificationType;
        private String category;
        private NotificationPriority priority = NotificationPriority.NORMAL;
        private Platform platform;
        private String sound;
        private Integer badge;
        private NotificationStatus status = NotificationStatus.PENDING;
        private LocalDateTime scheduledFor;
        private LocalDateTime expiresAt;
        private Company company;
        private User createdByUser;

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder deviceToken(String deviceToken) {
            this.deviceToken = deviceToken;
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

        public Builder status(NotificationStatus status) {
            this.status = status;
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

        public Builder company(Company company) {
            this.company = company;
            return this;
        }

        public Builder createdByUser(User createdByUser) {
            this.createdByUser = createdByUser;
            return this;
        }

        public NotificationQueue build() {
            return new NotificationQueue(this);
        }
    }

    public enum NotificationType {
        REPORT,
        WORK_ORDER,
        ALERT,
        REMINDER,
        SYSTEM,
        BROADCAST
    }

    public enum NotificationPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    public enum NotificationStatus {
        PENDING,
        PROCESSING,
        SENT,
        FAILED,
        RETRY,
        EXPIRED,
        CANCELLED
    }

    public enum Platform {
        IOS,
        ANDROID,
        WEB
    }
}