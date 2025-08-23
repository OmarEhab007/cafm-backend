package com.cafm.cafmbackend.dto.notification;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for registering or updating FCM device tokens.
 */
public record FCMTokenRequest(
    @NotNull(message = "Token is required")
    @Size(min = 10, max = 500, message = "Token must be between 10 and 500 characters")
    String token,
    
    @NotNull(message = "Platform is required")
    @Pattern(regexp = "android|ios|web", message = "Platform must be one of: android, ios, web")
    String platform,
    
    @Size(max = 255, message = "Device ID must not exceed 255 characters")
    String deviceId,
    
    @Size(max = 255, message = "Device name must not exceed 255 characters")
    String deviceName,
    
    @Size(max = 100, message = "Device model must not exceed 100 characters")
    String deviceModel,
    
    @Size(max = 50, message = "OS version must not exceed 50 characters")
    String osVersion,
    
    @Size(max = 50, message = "App version must not exceed 50 characters")
    String appVersion,
    
    Boolean notificationEnabled,
    
    Boolean soundEnabled,
    
    Boolean vibrationEnabled,
    
    @Size(max = 10, message = "Language code must not exceed 10 characters")
    String language,
    
    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    String timezone,
    
    List<String> topics
) {
    /**
     * Creates a basic token registration request.
     */
    public static FCMTokenRequest basic(
            String token,
            String platform,
            String deviceId) {
        return new FCMTokenRequest(
            token,
            platform,
            deviceId,
            null,
            null,
            null,
            null,
            true,
            true,
            true,
            "ar",
            null,
            List.of()
        );
    }
    
    /**
     * Creates a mobile token registration request.
     */
    public static FCMTokenRequest mobile(
            String token,
            String platform,
            String deviceId,
            String deviceModel,
            String osVersion,
            String appVersion) {
        return new FCMTokenRequest(
            token,
            platform,
            deviceId,
            null,
            deviceModel,
            osVersion,
            appVersion,
            true,
            true,
            true,
            "ar",
            java.util.TimeZone.getDefault().getID(),
            List.of()
        );
    }
}