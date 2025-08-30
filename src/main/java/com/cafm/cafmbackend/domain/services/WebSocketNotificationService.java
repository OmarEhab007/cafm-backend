package com.cafm.cafmbackend.domain.services;

import com.cafm.cafmbackend.dto.mobile.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service for handling WebSocket-based real-time notifications in the CAFM system.
 * 
 * This service provides:
 * - Real-time work order updates and status changes
 * - Live location tracking and broadcasting
 * - Instant messaging between system users
 * - System-wide alert broadcasts
 * - Dashboard real-time data updates
 * 
 * The service uses Spring's SimpMessagingTemplate for WebSocket message delivery
 * and maintains tenant isolation to ensure secure multi-tenant operation.
 * Supports async notification delivery for high-performance operation.
 */
@Service
public class WebSocketNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketNotificationService.class);
    
    private final SimpMessagingTemplate messagingTemplate;
    
    // Active subscriptions tracking for efficient message routing
    private final ConcurrentMap<String, UUID> userTenantMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> tenantSubscriptionCount = new ConcurrentHashMap<>();

    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcasts work order updates to all relevant subscribers within the tenant
     */
    public CompletableFuture<Void> broadcastWorkOrderUpdate(WorkOrderUpdateDto update, UUID tenantId) {
        return CompletableFuture.runAsync(() -> {
            log.debug("Broadcasting work order update: {} for tenant: {}", update.workOrderId(), tenantId);
            
            try {
                // Create notification message
                NotificationDto notification = new NotificationDto(
                        UUID.randomUUID(),
                        "WORK_ORDER_UPDATE",
                        String.format("Work Order %s status changed to %s", 
                                update.workOrderId(), update.status()),
                        null, // No specific recipient - broadcast
                        LocalDateTime.now(),
                        false,
                        "NORMAL", // priority
                        update.workOrderId(), // workOrderId
                        null, // senderId
                        "WORK_ORDER" // category
                );
                
                // Broadcast to tenant-specific topic
                String topic = String.format("/topic/tenant/%s/work-orders", tenantId);
                messagingTemplate.convertAndSend(topic, update);
                
                // Also send general notification
                String notificationTopic = String.format("/topic/tenant/%s/notifications", tenantId);
                messagingTemplate.convertAndSend(notificationTopic, notification);
                
                log.debug("Work order update broadcasted successfully");
                
            } catch (Exception e) {
                log.error("Error broadcasting work order update", e);
                throw new RuntimeException("Failed to broadcast work order update", e);
            }
        });
    }

    /**
     * Sends work order update acknowledgment to specific user
     */
    public void sendWorkOrderUpdateAck(UUID workOrderId, String username, boolean success) {
        try {
            NotificationDto ack = new NotificationDto(
                    UUID.randomUUID(),
                    success ? "UPDATE_ACK_SUCCESS" : "UPDATE_ACK_FAILURE",
                    success ? "Work order updated successfully" : "Failed to update work order",
                    null,
                    LocalDateTime.now(),
                    false,
                    success ? "HIGH" : "URGENT", // priority
                    workOrderId, // workOrderId
                    null, // senderId
                    "ACK" // category
            );
            
            String userQueue = String.format("/user/%s/queue/acks", username);
            messagingTemplate.convertAndSend(userQueue, ack);
            
            log.debug("Work order update acknowledgment sent to user: {}", username);
            
        } catch (Exception e) {
            log.error("Error sending work order update acknowledgment", e);
        }
    }

    /**
     * Broadcasts location updates to supervisors and relevant personnel
     */
    public CompletableFuture<Void> broadcastLocationUpdate(LocationUpdateDto locationUpdate, 
                                                          String username, UUID tenantId) {
        return CompletableFuture.runAsync(() -> {
            log.debug("Broadcasting location update for user: {} in tenant: {}", username, tenantId);
            
            try {
                // Create enhanced location update with user info
                EnhancedLocationUpdate enhancedUpdate = new EnhancedLocationUpdate(
                        username,
                        locationUpdate.latitude(),
                        locationUpdate.longitude(),
                        locationUpdate.accuracy(),
                        locationUpdate.timestamp(),
                        locationUpdate.batteryLevel(),
                        calculateMovementStatus(locationUpdate)
                );
                
                // Broadcast to supervisors in the tenant
                String supervisorTopic = String.format("/topic/tenant/%s/locations/supervisors", tenantId);
                messagingTemplate.convertAndSend(supervisorTopic, enhancedUpdate);
                
                // Broadcast to admin dashboard
                String adminTopic = String.format("/topic/tenant/%s/dashboard/locations", tenantId);
                messagingTemplate.convertAndSend(adminTopic, enhancedUpdate);
                
                log.debug("Location update broadcasted successfully for user: {}", username);
                
            } catch (Exception e) {
                log.error("Error broadcasting location update", e);
            }
        });
    }

    /**
     * Sends instant message to specific recipient
     */
    public CompletableFuture<Void> sendInstantMessage(NotificationDto message, 
                                                     String senderUsername, UUID tenantId) {
        return CompletableFuture.runAsync(() -> {
            log.debug("Sending instant message from: {} to recipient: {}", 
                    senderUsername, message.recipientId());
            
            try {
                // Enhance message with sender info and tenant context
                InstantMessage instantMessage = new InstantMessage(
                        message.id(),
                        senderUsername,
                        message.recipientId().toString(),
                        message.message(),
                        message.timestamp(),
                        tenantId
                );
                
                // Send to recipient's private queue
                String recipientQueue = String.format("/user/%s/queue/messages", 
                        getRecipientUsername(message.recipientId()));
                messagingTemplate.convertAndSend(recipientQueue, instantMessage);
                
                // Also notify via general notification topic for the recipient
                String recipientNotificationTopic = String.format("/user/%s/queue/notifications", 
                        getRecipientUsername(message.recipientId()));
                messagingTemplate.convertAndSend(recipientNotificationTopic, message);
                
                log.debug("Instant message delivered successfully");
                
            } catch (Exception e) {
                log.error("Error sending instant message", e);
                throw new RuntimeException("Failed to send instant message", e);
            }
        });
    }

    /**
     * Sends message delivery confirmation to sender
     */
    public void sendMessageDeliveryConfirmation(UUID messageId, String senderUsername, boolean delivered) {
        try {
            NotificationDto confirmation = new NotificationDto(
                    UUID.randomUUID(),
                    delivered ? "MESSAGE_DELIVERED" : "MESSAGE_DELIVERY_FAILED",
                    delivered ? "Message delivered successfully" : "Failed to deliver message",
                    null,
                    LocalDateTime.now(),
                    false,
                    "LOW", // priority
                    null, // workOrderId
                    null, // senderId
                    "DELIVERY" // category
            );
            
            String senderQueue = String.format("/user/%s/queue/confirmations", senderUsername);
            messagingTemplate.convertAndSend(senderQueue, confirmation);
            
            log.debug("Message delivery confirmation sent to: {}", senderUsername);
            
        } catch (Exception e) {
            log.error("Error sending message delivery confirmation", e);
        }
    }

    /**
     * Broadcasts system-wide alerts to all users in the tenant
     */
    public CompletableFuture<Void> broadcastSystemAlert(NotificationDto alert, UUID tenantId) {
        return CompletableFuture.runAsync(() -> {
            log.info("Broadcasting system alert to all users in tenant: {}", tenantId);
            
            try {
                // Enhance alert with system context
                SystemAlert systemAlert = new SystemAlert(
                        alert.id(),
                        alert.type(),
                        alert.message(),
                        alert.timestamp(),
                        tenantId,
                        "SYSTEM",
                        calculateAlertPriority(alert.type())
                );
                
                // Broadcast to all users in tenant
                String alertTopic = String.format("/topic/tenant/%s/alerts", tenantId);
                messagingTemplate.convertAndSend(alertTopic, systemAlert);
                
                // Also send to general notifications topic
                String notificationTopic = String.format("/topic/tenant/%s/notifications", tenantId);
                messagingTemplate.convertAndSend(notificationTopic, alert);
                
                log.info("System alert broadcasted successfully to tenant: {}", tenantId);
                
            } catch (Exception e) {
                log.error("Error broadcasting system alert", e);
                throw new RuntimeException("Failed to broadcast system alert", e);
            }
        });
    }

    /**
     * Registers user for dashboard updates subscription
     */
    public void registerDashboardSubscription(String username, UUID tenantId) {
        log.debug("Registering dashboard subscription for user: {} in tenant: {}", username, tenantId);
        
        try {
            // Track user-tenant mapping
            userTenantMap.put(username, tenantId);
            
            // Update tenant subscription count
            tenantSubscriptionCount.merge(tenantId, 1, Integer::sum);
            
            // Send initial dashboard data
            sendInitialDashboardData(username, tenantId);
            
            log.debug("Dashboard subscription registered successfully for user: {}", username);
            
        } catch (Exception e) {
            log.error("Error registering dashboard subscription", e);
        }
    }

    /**
     * Sends real-time dashboard updates to subscribed users
     */
    public CompletableFuture<Void> sendDashboardUpdate(DashboardUpdateDto update, UUID tenantId) {
        return CompletableFuture.runAsync(() -> {
            log.debug("Sending dashboard update to tenant: {}", tenantId);
            
            try {
                String dashboardTopic = String.format("/topic/tenant/%s/dashboard/updates", tenantId);
                messagingTemplate.convertAndSend(dashboardTopic, update);
                
                log.debug("Dashboard update sent successfully to tenant: {}", tenantId);
                
            } catch (Exception e) {
                log.error("Error sending dashboard update", e);
            }
        });
    }

    /**
     * Unregisters user from dashboard subscriptions (called on disconnect)
     */
    public void unregisterDashboardSubscription(String username) {
        UUID tenantId = userTenantMap.remove(username);
        if (tenantId != null) {
            tenantSubscriptionCount.computeIfPresent(tenantId, (key, count) -> count > 1 ? count - 1 : null);
            log.debug("Dashboard subscription unregistered for user: {}", username);
        }
    }

    // Helper methods

    private String calculateMovementStatus(LocationUpdateDto locationUpdate) {
        // Simplified movement detection - in real implementation would compare with previous location
        return locationUpdate.accuracy() < 10 ? "STATIONARY" : "MOVING";
    }

    private String getRecipientUsername(UUID recipientId) {
        // In real implementation, this would query user repository to get username by ID
        // For now, return a placeholder - this needs proper implementation
        return "recipient-" + recipientId.toString();
    }

    private String calculateAlertPriority(String alertType) {
        return switch (alertType.toUpperCase()) {
            case "EMERGENCY", "CRITICAL" -> "HIGH";
            case "WARNING", "MAINTENANCE" -> "MEDIUM";
            default -> "LOW";
        };
    }

    private void sendInitialDashboardData(String username, UUID tenantId) {
        try {
            // Create initial dashboard data payload
            DashboardUpdateDto initialData = new DashboardUpdateDto(
                    "INITIAL_LOAD",
                    LocalDateTime.now(),
                    "Dashboard connection established",
                    null // Would include actual dashboard metrics in real implementation
            );
            
            String userQueue = String.format("/user/%s/queue/dashboard", username);
            messagingTemplate.convertAndSend(userQueue, initialData);
            
            log.debug("Initial dashboard data sent to user: {}", username);
            
        } catch (Exception e) {
            log.error("Error sending initial dashboard data", e);
        }
    }

    // Record DTOs for WebSocket messages

    public record EnhancedLocationUpdate(
            String username,
            Double latitude,
            Double longitude,
            Float accuracy,
            LocalDateTime timestamp,
            Integer batteryLevel,
            String movementStatus
    ) {}

    public record InstantMessage(
            UUID id,
            String senderUsername,
            String recipientUsername,
            String message,
            LocalDateTime timestamp,
            UUID tenantId
    ) {}

    public record SystemAlert(
            UUID id,
            String type,
            String message,
            LocalDateTime timestamp,
            UUID tenantId,
            String source,
            String priority
    ) {}

    public record DashboardUpdateDto(
            String updateType,
            LocalDateTime timestamp,
            String description,
            Object data
    ) {}
}