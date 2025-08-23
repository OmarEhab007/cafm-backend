package com.cafm.cafmbackend.api.controllers;

import com.cafm.cafmbackend.dto.notification.FCMTokenRequest;
import com.cafm.cafmbackend.dto.notification.NotificationRequest;
import com.cafm.cafmbackend.dto.notification.NotificationResponse;
import com.cafm.cafmbackend.dto.notification.PushNotificationRequest;
import com.cafm.cafmbackend.service.NotificationService;
import com.cafm.cafmbackend.service.PushNotificationService;
import com.cafm.cafmbackend.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * REST controller for notification management operations.
 * 
 * Purpose: Provides comprehensive notification endpoints for in-app notifications,
 * push notifications, email notifications, and FCM token management.
 * 
 * Pattern: RESTful controller with comprehensive OpenAPI documentation
 * Java 23: Modern controller patterns with async support
 * Architecture: API layer with proper security annotations
 * Standards: Follows REST conventions and security best practices
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notification Management", description = "Notification sending and management operations")
@SecurityRequirement(name = "Bearer Authentication")
public class NotificationController {

    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public NotificationController(NotificationService notificationService,
                                PushNotificationService pushNotificationService,
                                CurrentUserService currentUserService,
                                ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.pushNotificationService = pushNotificationService;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
    }

    // ==================== IN-APP NOTIFICATIONS ====================

    @Operation(
        summary = "Send in-app notification",
        description = "Send an in-app notification to a specific user",
        responses = {
            @ApiResponse(responseCode = "201", description = "Notification sent successfully",
                content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<NotificationResponse> sendInAppNotification(
            @Parameter(description = "User ID to send notification to", required = true)
            @RequestParam UUID userId,
            
            @Parameter(description = "Notification details", required = true)
            @Valid @RequestBody NotificationRequest request) {
        
        NotificationResponse response = notificationService.sendInAppNotification(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Send notifications to multiple users",
        description = "Send in-app notifications to multiple users simultaneously",
        responses = {
            @ApiResponse(responseCode = "202", description = "Notifications are being processed"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping("/send/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> sendInAppNotificationToUsers(
            @Parameter(description = "List of user IDs", required = true)
            @RequestParam List<UUID> userIds,
            
            @Parameter(description = "Notification details", required = true)
            @Valid @RequestBody NotificationRequest request) {
        
        CompletableFuture<List<NotificationResponse>> future = 
            notificationService.sendInAppNotificationToUsers(userIds, request);
        
        return ResponseEntity.accepted().body(Map.of(
            "message", "Notifications are being processed",
            "userCount", userIds.size(),
            "status", "processing"
        ));
    }

    @Operation(
        summary = "Get user notifications",
        description = "Retrieve paginated notifications for the current user",
        responses = {
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @PageableDefault(size = 20) Pageable pageable) {
        
        UUID currentUserId = currentUserService.getCurrentUserId();
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(currentUserId, pageable);
        return ResponseEntity.ok(notifications);
    }

    @Operation(
        summary = "Get notifications for user",
        description = "Retrieve paginated notifications for a specific user (admin/supervisor only)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId,
            
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    @Operation(
        summary = "Mark notification as read",
        description = "Mark a specific notification as read",
        responses = {
            @ApiResponse(responseCode = "204", description = "Notification marked as read"),
            @ApiResponse(responseCode = "404", description = "Notification not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PutMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<Void> markAsRead(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable UUID notificationId) {
        
        notificationService.markNotificationAsRead(notificationId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Delete notification",
        description = "Delete a specific notification",
        responses = {
            @ApiResponse(responseCode = "204", description = "Notification deleted"),
            @ApiResponse(responseCode = "404", description = "Notification not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @DeleteMapping("/{notificationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<Void> deleteNotification(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable UUID notificationId) {
        
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.noContent().build();
    }

    // ==================== PUSH NOTIFICATIONS ====================

    @Operation(
        summary = "Send push notification",
        description = "Send a push notification to a specific user's devices",
        responses = {
            @ApiResponse(responseCode = "202", description = "Push notification queued for delivery"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping("/push/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<Map<String, Object>> sendPushNotification(
            @Parameter(description = "User ID to send notification to", required = true)
            @RequestParam UUID userId,
            
            @Parameter(description = "Push notification details", required = true)
            @Valid @RequestBody PushNotificationRequest request) {
        
        CompletableFuture<Void> future = notificationService.sendPushNotification(userId, request);
        
        return ResponseEntity.accepted().body(Map.of(
            "message", "Push notification queued for delivery",
            "userId", userId.toString(),
            "status", "queued"
        ));
    }

    @Operation(
        summary = "Send push notification to multiple users",
        description = "Send push notifications to multiple users simultaneously",
        responses = {
            @ApiResponse(responseCode = "202", description = "Push notifications queued for delivery"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping("/push/send/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> sendPushNotificationToUsers(
            @Parameter(description = "List of user IDs", required = true)
            @RequestParam List<UUID> userIds,
            
            @Parameter(description = "Push notification details", required = true)
            @Valid @RequestBody PushNotificationRequest request) {
        
        CompletableFuture<Void> future = 
            notificationService.sendPushNotificationToUsers(userIds, request);
        
        return ResponseEntity.accepted().body(Map.of(
            "message", "Push notifications queued for delivery",
            "userCount", userIds.size(),
            "status", "queued"
        ));
    }

    @Operation(
        summary = "Send push notification to topic",
        description = "Send a push notification to all users subscribed to a topic (broadcast)",
        responses = {
            @ApiResponse(responseCode = "202", description = "Topic notification queued for delivery"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping("/push/send/topic")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> sendPushNotificationToTopic(
            @Parameter(description = "Topic name", required = true)
            @RequestParam String topic,
            
            @Parameter(description = "Push notification details", required = true)
            @Valid @RequestBody PushNotificationRequest request) {
        
        CompletableFuture<Void> future = 
            notificationService.sendPushNotificationToTopic(topic, request);
        
        return ResponseEntity.accepted().body(Map.of(
            "message", "Topic notification queued for delivery",
            "topic", topic,
            "status", "queued"
        ));
    }

    // ==================== FCM TOKEN MANAGEMENT ====================

    @Operation(
        summary = "Register FCM token",
        description = "Register a Firebase Cloud Messaging token for push notifications",
        responses = {
            @ApiResponse(responseCode = "201", description = "FCM token registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid token or request"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping("/fcm/token")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<Map<String, Object>> registerFCMToken(
            @Parameter(description = "FCM token registration details", required = true)
            @Valid @RequestBody FCMTokenRequest request) {
        
        UUID currentUserId = currentUserService.getCurrentUserId();
        
        Map<String, String> deviceInfo = Map.of(
            "deviceId", request.deviceId() != null ? request.deviceId() : "",
            "deviceModel", request.deviceModel() != null ? request.deviceModel() : "",
            "osVersion", request.osVersion() != null ? request.osVersion() : "",
            "appVersion", request.appVersion() != null ? request.appVersion() : ""
        );
        
        // Convert device info map to JSON string
        String deviceInfoJson;
        try {
            deviceInfoJson = objectMapper.writeValueAsString(deviceInfo);
        } catch (Exception e) {
            deviceInfoJson = "{}"; // fallback to empty JSON
        }
        
        pushNotificationService.registerToken(
            currentUserId,
            request.token(),
            request.platform(),
            deviceInfoJson
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "message", "FCM token registered successfully",
            "platform", request.platform(),
            "status", "active"
        ));
    }

    @Operation(
        summary = "Unregister FCM token",
        description = "Unregister a Firebase Cloud Messaging token",
        responses = {
            @ApiResponse(responseCode = "204", description = "FCM token unregistered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid token"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @DeleteMapping("/fcm/token")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<Void> unregisterFCMToken(
            @Parameter(description = "FCM token to unregister", required = true)
            @RequestParam String token) {
        
        pushNotificationService.unregisterToken(token);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Get user's active token count",
        description = "Get the count of active FCM tokens for the current user",
        responses = {
            @ApiResponse(responseCode = "200", description = "Token count retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @GetMapping("/fcm/tokens/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<Map<String, Object>> getActiveTokensCount() {
        UUID currentUserId = currentUserService.getCurrentUserId();
        
        long count = pushNotificationService.getActiveTokensCount(currentUserId);
        
        return ResponseEntity.ok(Map.of(
            "userId", currentUserId.toString(),
            "activeTokens", count
        ));
    }

    // ==================== MULTI-CHANNEL NOTIFICATIONS ====================

    @Operation(
        summary = "Send multi-channel notification",
        description = "Send notification through multiple channels (in-app, push, email)",
        responses = {
            @ApiResponse(responseCode = "202", description = "Multi-channel notification initiated"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping("/send/multi-channel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<Map<String, Object>> sendMultiChannelNotification(
            @Parameter(description = "User ID to send notification to", required = true)
            @RequestParam UUID userId,
            
            @Parameter(description = "In-app notification details", required = false)
            @RequestBody(required = false) NotificationRequest inAppRequest,
            
            @Parameter(description = "Push notification details", required = false)
            @RequestBody(required = false) PushNotificationRequest pushRequest,
            
            @Parameter(description = "Email template name", required = false)
            @RequestParam(required = false) String emailTemplate) {
        
        Map<String, Object> emailVariables = Map.of(); // Could be extended to accept variables
        
        CompletableFuture<Map<String, Object>> future = 
            notificationService.sendMultiChannelNotification(
                userId, inAppRequest, pushRequest, emailTemplate, emailVariables);
        
        return ResponseEntity.accepted().body(Map.of(
            "message", "Multi-channel notification initiated",
            "userId", userId.toString(),
            "channels", List.of("in-app", "push", "email"),
            "status", "processing"
        ));
    }

    // ==================== SYSTEM NOTIFICATIONS ====================

    @Operation(
        summary = "Process pending notifications",
        description = "Manually trigger processing of pending notifications (admin only)",
        responses = {
            @ApiResponse(responseCode = "202", description = "Notification processing initiated"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping("/system/process-pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> processPendingNotifications() {
        notificationService.processPendingNotifications();
        
        return ResponseEntity.accepted().body(Map.of(
            "message", "Notification processing initiated",
            "status", "processing"
        ));
    }

    @Operation(
        summary = "Cleanup invalid FCM tokens",
        description = "Cleanup expired or invalid FCM tokens (admin only)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Token cleanup completed"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping("/system/cleanup-tokens")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cleanupInvalidTokens() {
        pushNotificationService.cleanupInvalidTokens();
        
        return ResponseEntity.ok(Map.of(
            "message", "FCM token cleanup completed",
            "status", "completed"
        ));
    }
}