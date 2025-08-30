package com.cafm.cafmbackend.application.service;

import com.cafm.cafmbackend.infrastructure.persistence.entity.FCMToken;
import com.cafm.cafmbackend.infrastructure.persistence.entity.NotificationQueue;
import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import com.cafm.cafmbackend.infrastructure.persistence.repository.FCMTokenRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.UserRepository;
import com.cafm.cafmbackend.shared.exception.BusinessLogicException;
import com.cafm.cafmbackend.shared.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Firebase Cloud Messaging (FCM) push notification service.
 * 
 * Purpose: Handles FCM push notifications with retry logic, token management,
 * and comprehensive error handling for mobile app notifications.
 * 
 * Pattern: Service layer with async processing and proper error handling
 * Java 23: Uses virtual threads for I/O operations and modern HTTP client
 * Architecture: External service integration with proper resilience patterns
 * Standards: Implements FCM best practices and security guidelines
 */
@Service
public class PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    private final RestTemplate restTemplate;
    private final FCMTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    
    // Async processing
    private final ExecutorService executorService;

    @Value("${app.firebase.fcm.server-key:}")
    private String fcmServerKey;
    
    @Value("${app.firebase.fcm.url:https://fcm.googleapis.com/fcm/send}")
    private String fcmUrl;
    
    @Value("${app.firebase.fcm.enabled:false}")
    private boolean fcmEnabled;
    
    @Value("${app.firebase.project-id:}")
    private String firebaseProjectId;

    public PushNotificationService(RestTemplate restTemplate,
                                 FCMTokenRepository fcmTokenRepository,
                                 UserRepository userRepository,
                                 AuditService auditService,
                                 ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.fcmTokenRepository = fcmTokenRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Send push notification to a specific device token.
     */
    @Async
    public CompletableFuture<Map<String, Object>> sendToToken(NotificationQueue notification) {
        return CompletableFuture.supplyAsync(() -> {
            if (!fcmEnabled || fcmServerKey.isEmpty()) {
                logger.warn("FCM is disabled or server key not configured");
                return Map.of("success", false, "error", "FCM not configured");
            }
            
            try {
                logger.info("Sending FCM notification to token: {}", 
                    maskToken(notification.getDeviceToken()));
                
                Map<String, Object> fcmPayload = buildFCMPayload(notification);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "key=" + fcmServerKey);
                
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(fcmPayload, headers);
                
                ResponseEntity<Map> response = restTemplate.exchange(
                    fcmUrl, HttpMethod.POST, request, Map.class);
                
                Map<String, Object> responseBody = response.getBody();
                
                if (response.getStatusCode().is2xxSuccessful() && responseBody != null) {
                    // Check FCM response for success/failure
                    Integer success = (Integer) responseBody.get("success");
                    Integer failure = (Integer) responseBody.get("failure");
                    
                    if (success != null && success > 0) {
                        logger.info("FCM notification sent successfully");
                        
                        auditService.logNotificationEvent("FCM_NOTIFICATION_SENT", 
                            notification.getId(), notification.getUser().getId(), 
                            notification.getTitle());
                        
                        return Map.of(
                            "success", true,
                            "messageId", responseBody.getOrDefault("multicast_id", ""),
                            "response", responseBody
                        );
                    } else {
                        String errorMessage = "FCM delivery failed";
                        if (responseBody.containsKey("results")) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("results");
                            if (!results.isEmpty() && results.get(0).containsKey("error")) {
                                errorMessage = (String) results.get(0).get("error");
                            }
                        }
                        
                        logger.warn("FCM notification failed: {}", errorMessage);
                        handleTokenError(notification.getDeviceToken(), errorMessage);
                        
                        return Map.of("success", false, "error", errorMessage, "response", responseBody);
                    }
                } else {
                    logger.error("FCM API returned error: {}", response.getStatusCode());
                    return Map.of("success", false, "error", "FCM API error: " + response.getStatusCode());
                }
                
            } catch (Exception e) {
                logger.error("Error sending FCM notification: {}", e.getMessage(), e);
                return Map.of("success", false, "error", e.getMessage());
            }
        }, executorService);
    }

    /**
     * Send push notification to a topic (broadcast).
     */
    @Async
    public CompletableFuture<Map<String, Object>> sendToTopic(NotificationQueue notification) {
        return CompletableFuture.supplyAsync(() -> {
            if (!fcmEnabled || fcmServerKey.isEmpty()) {
                logger.warn("FCM is disabled or server key not configured");
                return Map.of("success", false, "error", "FCM not configured");
            }
            
            try {
                logger.info("Sending FCM notification to topic: {}", notification.getTopic());
                
                Map<String, Object> fcmPayload = buildFCMTopicPayload(notification);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "key=" + fcmServerKey);
                
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(fcmPayload, headers);
                
                ResponseEntity<Map> response = restTemplate.exchange(
                    fcmUrl, HttpMethod.POST, request, Map.class);
                
                Map<String, Object> responseBody = response.getBody();
                
                if (response.getStatusCode().is2xxSuccessful() && responseBody != null) {
                    logger.info("FCM topic notification sent successfully");
                    
                    auditService.logNotificationEvent("FCM_TOPIC_NOTIFICATION_SENT", 
                        notification.getId(), null, notification.getTitle());
                    
                    return Map.of(
                        "success", true,
                        "messageId", responseBody.getOrDefault("message_id", ""),
                        "response", responseBody
                    );
                } else {
                    logger.error("FCM topic notification failed: {}", response.getStatusCode());
                    return Map.of("success", false, "error", "FCM API error: " + response.getStatusCode());
                }
                
            } catch (Exception e) {
                logger.error("Error sending FCM topic notification: {}", e.getMessage(), e);
                return Map.of("success", false, "error", e.getMessage());
            }
        }, executorService);
    }

    /**
     * Register FCM token for a user.
     */
    public void registerToken(UUID userId, String token, String platform, String deviceInfo) {
        try {
            // Check if token already exists
            FCMToken existingToken = fcmTokenRepository.findByToken(token).orElse(null);
            
            if (existingToken != null) {
                // Update existing token
                existingToken.activate();
                if (deviceInfo != null) {
                    updateDeviceInfoFromJson(existingToken, deviceInfo);
                }
                fcmTokenRepository.save(existingToken);
                
                logger.info("FCM token updated for user: {}", userId);
            } else {
                // Create new token
                FCMToken fcmToken = new FCMToken();
                
                // Set the user entity
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    throw new BusinessLogicException("User not found: " + userId, ErrorCode.USER_NOT_FOUND.getCode());
                }
                fcmToken.setUser(user);
                fcmToken.setToken(token);
                fcmToken.setCompany(user.getCompany());
                
                // Set platform (should be one of: android, ios, web)
                fcmToken.setPlatform(platform.toLowerCase());
                fcmToken.setActive(true);
                
                if (deviceInfo != null) {
                    updateDeviceInfoFromJson(fcmToken, deviceInfo);
                }
                
                fcmTokenRepository.save(fcmToken);
                
                logger.info("New FCM token registered for user: {}", userId);
            }
            
            auditService.logSecurityEvent("FCM_TOKEN_REGISTERED", 
                String.format("FCM token registered for user %s", userId));
            
        } catch (Exception e) {
            logger.error("Failed to register FCM token for user {}: {}", userId, e.getMessage(), e);
            throw new BusinessLogicException("Failed to register FCM token", ErrorCode.FCM_TOKEN_REGISTRATION_FAILED.getCode());
        }
    }

    /**
     * Unregister FCM token.
     */
    public void unregisterToken(String token) {
        try {
            FCMToken fcmToken = fcmTokenRepository.findByToken(token).orElse(null);
            
            if (fcmToken != null) {
                fcmToken.setActive(false);
                // Note: updatedAt is handled by BaseEntity automatically
                fcmTokenRepository.save(fcmToken);
                
                logger.info("FCM token unregistered: {}", maskToken(token));
                
                auditService.logSecurityEvent("FCM_TOKEN_UNREGISTERED", 
                    String.format("FCM token unregistered: %s", maskToken(token)));
            }
            
        } catch (Exception e) {
            logger.error("Failed to unregister FCM token: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up expired or invalid tokens.
     */
    public void cleanupInvalidTokens() {
        try {
            // For now, we'll just log that cleanup was requested
            // In a real implementation, we would check creation dates or other criteria
            logger.info("FCM token cleanup requested - implementation pending");
            
        } catch (Exception e) {
            logger.error("Failed to cleanup FCM tokens: {}", e.getMessage(), e);
        }
    }

    /**
     * Get user's active tokens count.
     */
    public long getActiveTokensCount(UUID userId) {
        return fcmTokenRepository.countActiveTokensByUserId(userId);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private Map<String, Object> buildFCMPayload(NotificationQueue notification) {
        Map<String, Object> payload = new HashMap<>();
        
        // Set target
        payload.put("to", notification.getDeviceToken());
        
        // Set notification
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", notification.getTitle());
        notificationData.put("body", notification.getBody());
        
        if (notification.getSound() != null) {
            notificationData.put("sound", notification.getSound());
        }
        
        if (notification.getBadge() != null) {
            notificationData.put("badge", notification.getBadge());
        }
        
        // Platform-specific settings
        if (notification.getPlatform() == NotificationQueue.Platform.IOS) {
            notificationData.put("sound", notification.getSound() != null ? notification.getSound() : "default");
        } else if (notification.getPlatform() == NotificationQueue.Platform.ANDROID) {
            notificationData.put("icon", "ic_notification");
            notificationData.put("color", "#2196F3");
        }
        
        payload.put("notification", notificationData);
        
        // Set data payload
        Map<String, Object> data = new HashMap<>(notification.getData());
        data.put("notificationId", notification.getId().toString());
        data.put("type", notification.getNotificationType().toString());
        data.put("category", notification.getCategory());
        data.put("timestamp", LocalDateTime.now().toString());
        
        payload.put("data", data);
        
        // Set options
        Map<String, Object> options = new HashMap<>();
        
        // Priority
        String priority = switch (notification.getPriority()) {
            case HIGH, URGENT -> "high";
            default -> "normal";
        };
        options.put("priority", priority);
        
        // TTL (Time To Live)
        if (notification.getExpiresAt() != null) {
            long ttlSeconds = java.time.Duration.between(LocalDateTime.now(), notification.getExpiresAt()).getSeconds();
            if (ttlSeconds > 0) {
                options.put("time_to_live", (int) Math.min(ttlSeconds, 2419200)); // Max 28 days
            }
        }
        
        // Collapse key for grouping similar notifications
        if (notification.getCategory() != null) {
            options.put("collapse_key", notification.getCategory());
        }
        
        payload.putAll(options);
        
        return payload;
    }

    private Map<String, Object> buildFCMTopicPayload(NotificationQueue notification) {
        Map<String, Object> payload = buildFCMPayload(notification);
        
        // Replace "to" with topic
        payload.remove("to");
        payload.put("to", "/topics/" + notification.getTopic());
        
        return payload;
    }

    private void handleTokenError(String token, String error) {
        // Handle specific FCM errors
        switch (error) {
            case "NotRegistered":
            case "InvalidRegistration":
                // Token is invalid, mark as inactive
                unregisterToken(token);
                logger.info("Invalid FCM token marked as inactive: {}", maskToken(token));
                break;
                
            case "MessageTooBig":
            case "InvalidDataKey":
            case "InvalidTtl":
                // Payload errors - log for debugging
                logger.warn("FCM payload error for token {}: {}", maskToken(token), error);
                break;
                
            default:
                // Other errors - log but don't deactivate token
                logger.warn("FCM error for token {}: {}", maskToken(token), error);
                break;
        }
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
    
    /**
     * Update FCMToken device info fields from JSON string.
     */
    private void updateDeviceInfoFromJson(FCMToken fcmToken, String deviceInfoJson) {
        try {
            Map<String, String> deviceInfoMap = objectMapper.readValue(
                deviceInfoJson, new TypeReference<Map<String, String>>() {});
            
            if (deviceInfoMap.containsKey("deviceId")) {
                fcmToken.setDeviceId(deviceInfoMap.get("deviceId"));
            }
            // Only deviceId is stored in the database
            // Other fields are not present in the current schema
            
        } catch (Exception e) {
            logger.warn("Failed to parse device info JSON: {}", e.getMessage());
        }
    }
}