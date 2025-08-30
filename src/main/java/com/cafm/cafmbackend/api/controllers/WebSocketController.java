package com.cafm.cafmbackend.api.controllers;

import com.cafm.cafmbackend.domain.services.WebSocketNotificationService;
import com.cafm.cafmbackend.dto.mobile.LocationUpdateDto;
import com.cafm.cafmbackend.dto.mobile.NotificationDto;
import com.cafm.cafmbackend.dto.mobile.WorkOrderUpdateDto;
import com.cafm.cafmbackend.shared.util.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WebSocket message controller that handles real-time communication for the CAFM system.
 * 
 * This controller manages:
 * - Real-time work order updates and notifications
 * - Live location tracking for supervisors and technicians
 * - Instant messaging and communication between users
 * - System alerts and emergency notifications
 * - Dashboard real-time data updates
 * 
 * All WebSocket messages are authenticated and tenant-isolated through interceptors.
 * The controller uses Spring's @MessageMapping for handling incoming messages
 * and @SendTo for broadcasting responses to appropriate channels.
 */
@Controller
public class WebSocketController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);
    
    private final WebSocketNotificationService notificationService;

    public WebSocketController(WebSocketNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Handles work order status updates and broadcasts to relevant subscribers
     */
    @MessageMapping("/work-order/update")
    @PreAuthorize("hasRole('SUPERVISOR') or hasRole('TECHNICIAN') or hasRole('ADMIN')")
    public void handleWorkOrderUpdate(@Payload WorkOrderUpdateDto update, 
                                    SimpMessageHeaderAccessor headerAccessor,
                                    Principal principal) {
        
        UUID tenantId = TenantContext.getCurrentCompanyId();
        String username = principal.getName();
        
        log.info("Received work order update from user: {} for tenant: {}", username, tenantId);
        
        try {
            // Validate and process the work order update
            validateWorkOrderUpdate(update);
            
            // Broadcast update to relevant subscribers
            notificationService.broadcastWorkOrderUpdate(update, tenantId);
            
            // Send acknowledgment back to sender
            notificationService.sendWorkOrderUpdateAck(update.workOrderId(), username, true);
            
            log.debug("Work order update processed successfully: {}", update.workOrderId());
            
        } catch (Exception e) {
            log.error("Error processing work order update", e);
            notificationService.sendWorkOrderUpdateAck(update.workOrderId(), username, false);
        }
    }

    /**
     * Handles real-time location updates from supervisors and technicians
     */
    @MessageMapping("/location/update")
    @PreAuthorize("hasRole('SUPERVISOR') or hasRole('TECHNICIAN')")
    public void handleLocationUpdate(@Payload LocationUpdateDto locationUpdate,
                                   Principal principal) {
        
        UUID tenantId = TenantContext.getCurrentCompanyId();
        String username = principal.getName();
        
        log.debug("Received location update from user: {} for tenant: {}", username, tenantId);
        
        try {
            // Validate location data
            validateLocationUpdate(locationUpdate);
            
            // Process and broadcast location update
            notificationService.broadcastLocationUpdate(locationUpdate, username, tenantId);
            
            log.debug("Location update processed for user: {}", username);
            
        } catch (Exception e) {
            log.error("Error processing location update for user: {}", username, e);
        }
    }

    /**
     * Handles instant messages between users
     */
    @MessageMapping("/message/send")
    @PreAuthorize("hasRole('SUPERVISOR') or hasRole('TECHNICIAN') or hasRole('ADMIN')")
    public void handleInstantMessage(@Payload NotificationDto message,
                                   Principal principal) {
        
        UUID tenantId = TenantContext.getCurrentCompanyId();
        String senderUsername = principal.getName();
        
        log.info("Received message from user: {} to recipient: {} for tenant: {}", 
                senderUsername, message.recipientId(), tenantId);
        
        try {
            // Validate message content
            validateInstantMessage(message);
            
            // Send message to specific recipient
            notificationService.sendInstantMessage(message, senderUsername, tenantId);
            
            // Send delivery confirmation to sender
            notificationService.sendMessageDeliveryConfirmation(message.id(), senderUsername, true);
            
            log.debug("Instant message delivered successfully: {}", message.id());
            
        } catch (Exception e) {
            log.error("Error processing instant message", e);
            notificationService.sendMessageDeliveryConfirmation(message.id(), senderUsername, false);
        }
    }

    /**
     * Handles system alert broadcasts
     */
    @MessageMapping("/alert/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public void handleSystemAlert(@Payload NotificationDto alert,
                                Principal principal) {
        
        UUID tenantId = TenantContext.getCurrentCompanyId();
        String adminUsername = principal.getName();
        
        log.info("Received system alert broadcast from admin: {} for tenant: {}", adminUsername, tenantId);
        
        try {
            // Validate alert content
            validateSystemAlert(alert);
            
            // Broadcast alert to all users in tenant
            notificationService.broadcastSystemAlert(alert, tenantId);
            
            log.info("System alert broadcasted successfully by admin: {}", adminUsername);
            
        } catch (Exception e) {
            log.error("Error broadcasting system alert", e);
        }
    }

    /**
     * Handles dashboard data subscription requests
     */
    @MessageMapping("/dashboard/subscribe")
    @SendTo("/topic/dashboard/updates")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public String handleDashboardSubscription(Principal principal) {
        
        UUID tenantId = TenantContext.getCurrentCompanyId();
        String username = principal.getName();
        
        log.info("User {} subscribed to dashboard updates for tenant: {}", username, tenantId);
        
        // Register user for dashboard updates
        notificationService.registerDashboardSubscription(username, tenantId);
        
        return String.format("Dashboard subscription activated for user: %s", username);
    }

    /**
     * Handles ping/pong messages for connection health monitoring
     */
    @MessageMapping("/ping")
    @SendTo("/queue/pong")
    public String handlePing(@Payload String ping, Principal principal) {
        log.debug("Received ping from user: {}", principal.getName());
        return "pong-" + LocalDateTime.now();
    }

    // Validation helper methods

    private void validateWorkOrderUpdate(WorkOrderUpdateDto update) {
        if (update == null) {
            throw new IllegalArgumentException("Work order update cannot be null");
        }
        
        if (update.workOrderId() == null) {
            throw new IllegalArgumentException("Work order ID is required");
        }
        
        if (update.status() == null) {
            throw new IllegalArgumentException("Work order status is required");
        }
        
        // Additional validation logic as needed
    }

    private void validateLocationUpdate(LocationUpdateDto locationUpdate) {
        if (locationUpdate == null) {
            throw new IllegalArgumentException("Location update cannot be null");
        }
        
        if (locationUpdate.latitude() == null || locationUpdate.longitude() == null) {
            throw new IllegalArgumentException("Latitude and longitude are required");
        }
        
        // Validate coordinate ranges
        double lat = locationUpdate.latitude();
        double lng = locationUpdate.longitude();
        
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Invalid latitude: must be between -90 and 90");
        }
        
        if (lng < -180 || lng > 180) {
            throw new IllegalArgumentException("Invalid longitude: must be between -180 and 180");
        }
    }

    private void validateInstantMessage(NotificationDto message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        if (message.recipientId() == null) {
            throw new IllegalArgumentException("Recipient ID is required");
        }
        
        if (message.message() == null || message.message().trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        
        if (message.message().length() > 1000) {
            throw new IllegalArgumentException("Message too long: maximum 1000 characters");
        }
    }

    private void validateSystemAlert(NotificationDto alert) {
        if (alert == null) {
            throw new IllegalArgumentException("Alert cannot be null");
        }
        
        if (alert.message() == null || alert.message().trim().isEmpty()) {
            throw new IllegalArgumentException("Alert message cannot be empty");
        }
        
        if (alert.type() == null) {
            throw new IllegalArgumentException("Alert type is required");
        }
    }
}