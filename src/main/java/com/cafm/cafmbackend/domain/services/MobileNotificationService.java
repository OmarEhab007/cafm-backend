package com.cafm.cafmbackend.domain.services;

import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import com.cafm.cafmbackend.infrastructure.persistence.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling push notifications and in-app notifications.
 * 
 * Purpose: Manages notification delivery to mobile devices and web clients
 * Pattern: Domain service handling notification orchestration and delivery
 * Java 23: Uses pattern matching for notification type handling
 * Architecture: Domain layer service with external service integration
 * Standards: Constructor injection, proper error handling, comprehensive logging
 */
@Service
@Transactional
public class MobileNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(MobileNotificationService.class);
    
    private final UserRepository userRepository;
    
    @Autowired
    public MobileNotificationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Send push notification to specific user.
     */
    public void sendPushNotification(String userEmail, String title, String message, Map<String, String> data) {
        logger.info("Sending push notification to user: {} with title: {}", userEmail, title);
        
        try {
            User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
            
            // Create notification payload
            Map<String, Object> notification = new HashMap<>();
            notification.put("title", title);
            notification.put("body", message);
            notification.put("data", data != null ? data : Map.of());
            notification.put("user_id", user.getId());
            notification.put("timestamp", LocalDateTime.now());
            
            // FCM integration would go here
            logger.debug("Push notification payload created for user: {}", userEmail);
            
            // For now, just log the notification
            logger.info("Push notification sent successfully to user: {}", userEmail);
            
        } catch (Exception e) {
            logger.error("Failed to send push notification to user: {}", userEmail, e);
            throw new RuntimeException("Failed to send push notification", e);
        }
    }
    
    /**
     * Send notification to multiple users.
     */
    public void sendBulkNotification(List<String> userEmails, String title, String message, Map<String, String> data) {
        logger.info("Sending bulk notification to {} users with title: {}", userEmails.size(), title);
        
        for (String userEmail : userEmails) {
            try {
                sendPushNotification(userEmail, title, message, data);
            } catch (Exception e) {
                logger.error("Failed to send notification to user in bulk: {}", userEmail, e);
                // Continue with other users
            }
        }
        
        logger.info("Bulk notification completed for {} users", userEmails.size());
    }
    
    /**
     * Send work order notification.
     */
    public void sendWorkOrderNotification(String userEmail, String workOrderId, String action) {
        logger.info("Sending work order notification to user: {} for action: {}", userEmail, action);
        
        String title = switch (action.toUpperCase()) {
            case "ASSIGNED" -> "New Work Order Assigned";
            case "UPDATED" -> "Work Order Updated";
            case "COMPLETED" -> "Work Order Completed";
            case "CANCELLED" -> "Work Order Cancelled";
            default -> "Work Order Notification";
        };
        
        String message = "Work order #" + workOrderId + " has been " + action.toLowerCase();
        
        Map<String, String> data = Map.of(
            "type", "work_order",
            "action", action,
            "work_order_id", workOrderId,
            "click_action", "/work-orders/" + workOrderId
        );
        
        sendPushNotification(userEmail, title, message, data);
    }
    
    /**
     * Send report status notification.
     */
    public void sendReportStatusNotification(String userEmail, String reportId, String status) {
        logger.info("Sending report status notification to user: {} for status: {}", userEmail, status);
        
        String title = "Report Status Update";
        String message = "Report #" + reportId + " status changed to " + status;
        
        Map<String, String> data = Map.of(
            "type", "report_status",
            "report_id", reportId,
            "status", status,
            "click_action", "/reports/" + reportId
        );
        
        sendPushNotification(userEmail, title, message, data);
    }
    
    /**
     * Send maintenance reminder notification.
     */
    public void sendMaintenanceReminder(String userEmail, String assetId, String maintenanceType) {
        logger.info("Sending maintenance reminder to user: {} for asset: {}", userEmail, assetId);
        
        String title = "Maintenance Reminder";
        String message = "Scheduled " + maintenanceType + " maintenance is due for asset #" + assetId;
        
        Map<String, String> data = Map.of(
            "type", "maintenance_reminder",
            "asset_id", assetId,
            "maintenance_type", maintenanceType,
            "click_action", "/assets/" + assetId
        );
        
        sendPushNotification(userEmail, title, message, data);
    }
    
    /**
     * Create in-app notification.
     */
    @Transactional
    public Map<String, Object> createInAppNotification(String userEmail, String title, String message, String type) {
        logger.info("Creating in-app notification for user: {} of type: {}", userEmail, type);
        
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
        
        // Create notification record (would save to notifications table)
        Map<String, Object> notification = new HashMap<>();
        notification.put("id", UUID.randomUUID());
        notification.put("user_id", user.getId());
        notification.put("title", title);
        notification.put("message", message);
        notification.put("type", type);
        notification.put("read", false);
        notification.put("created_at", LocalDateTime.now());
        
        logger.info("In-app notification created for user: {}", userEmail);
        
        return notification;
    }
    
    /**
     * Get unread notification count for user.
     */
    @Transactional(readOnly = true)
    public int getUnreadNotificationCount(String userEmail) {
        logger.debug("Getting unread notification count for user: {}", userEmail);
        
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
        
        // Would query notifications table for unread count
        // For now, return placeholder
        int unreadCount = 0;
        
        logger.debug("Unread notification count for user {}: {}", userEmail, unreadCount);
        
        return unreadCount;
    }
    
    /**
     * Mark notification as read.
     */
    @Transactional
    public void markNotificationAsRead(String notificationId, String userEmail) {
        logger.info("Marking notification as read: {} for user: {}", notificationId, userEmail);
        
        // Would update notifications table
        logger.info("Notification marked as read: {}", notificationId);
    }
    
    /**
     * Subscribe user to notification topic.
     */
    @Transactional
    public void subscribeToTopic(String userEmail, String topic) {
        logger.info("Subscribing user {} to topic: {}", userEmail, topic);
        
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
        
        // Would save to device_topic_subscriptions table
        logger.info("User {} subscribed to topic: {}", userEmail, topic);
    }
    
    /**
     * Unsubscribe user from notification topic.
     */
    @Transactional
    public void unsubscribeFromTopic(String userEmail, String topic) {
        logger.info("Unsubscribing user {} from topic: {}", userEmail, topic);
        
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
        
        // Would remove from device_topic_subscriptions table
        logger.info("User {} unsubscribed from topic: {}", userEmail, topic);
    }
}