package com.cafm.cafmbackend.api.controllers;

import com.cafm.cafmbackend.domain.services.MobileNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for notification management.
 * 
 * Purpose: Provides APIs for mobile and web notification management
 * Pattern: RESTful API with comprehensive notification support  
 * Java 23: Uses virtual threads for I/O operations and modern Spring Boot patterns
 * Architecture: Controller layer handling notification operations with proper security
 * Standards: OpenAPI documentation, comprehensive validation, proper error handling
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Push and in-app notification management APIs")
@SecurityRequirement(name = "bearer-jwt")
public class NotificationController {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    
    private final MobileNotificationService notificationService;
    
    @Autowired
    public NotificationController(MobileNotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    /**
     * Get unread notification count.
     */
    @GetMapping("/unread-count")
    @Operation(
        summary = "Get unread notification count",
        description = "Get count of unread notifications for current user"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.debug("Getting unread notification count for user: {}", currentUser.getUsername());
        
        int count = notificationService.getUnreadNotificationCount(currentUser.getUsername());
        
        Map<String, Object> response = Map.of(
            "unread_count", count,
            "user_email", currentUser.getUsername()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Mark notification as read.
     */
    @PostMapping("/{notificationId}/mark-read")
    @Operation(
        summary = "Mark notification as read",
        description = "Mark a specific notification as read"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable @Parameter(description = "Notification ID") String notificationId,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Marking notification as read: {} for user: {}", notificationId, currentUser.getUsername());
        
        notificationService.markNotificationAsRead(notificationId, currentUser.getUsername());
        
        Map<String, Object> response = Map.of(
            "status", "SUCCESS",
            "notification_id", notificationId,
            "message", "Notification marked as read"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Subscribe to notification topic.
     */
    @PostMapping("/topics/{topic}/subscribe")
    @Operation(
        summary = "Subscribe to topic",
        description = "Subscribe current user to notification topic"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<Map<String, Object>> subscribeToTopic(
            @PathVariable @Parameter(description = "Topic name") String topic,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Subscribing user {} to topic: {}", currentUser.getUsername(), topic);
        
        notificationService.subscribeToTopic(currentUser.getUsername(), topic);
        
        Map<String, Object> response = Map.of(
            "status", "SUCCESS",
            "topic", topic,
            "message", "Successfully subscribed to topic"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Unsubscribe from notification topic.
     */
    @PostMapping("/topics/{topic}/unsubscribe")
    @Operation(
        summary = "Unsubscribe from topic",
        description = "Unsubscribe current user from notification topic"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<Map<String, Object>> unsubscribeFromTopic(
            @PathVariable @Parameter(description = "Topic name") String topic,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Unsubscribing user {} from topic: {}", currentUser.getUsername(), topic);
        
        notificationService.unsubscribeFromTopic(currentUser.getUsername(), topic);
        
        Map<String, Object> response = Map.of(
            "status", "SUCCESS",
            "topic", topic,
            "message", "Successfully unsubscribed from topic"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Send test notification (admin only).
     */
    @PostMapping("/test")
    @Operation(
        summary = "Send test notification",
        description = "Send a test push notification to current user (admin only)"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> sendTestNotification(
            @RequestBody Map<String, Object> testRequest,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Sending test notification for admin user: {}", currentUser.getUsername());
        
        String title = (String) testRequest.getOrDefault("title", "Test Notification");
        String message = (String) testRequest.getOrDefault("message", "This is a test notification from CAFM system");
        
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) testRequest.getOrDefault("data", Map.of());
        
        notificationService.sendPushNotification(currentUser.getUsername(), title, message, data);
        
        Map<String, Object> response = Map.of(
            "status", "SUCCESS",
            "title", title,
            "message", message,
            "sent_to", currentUser.getUsername()
        );
        
        return ResponseEntity.ok(response);
    }
}