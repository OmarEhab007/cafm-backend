package com.cafm.cafmbackend.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Logout request DTO for ending user session.
 */
public record LogoutRequest(
    @NotBlank(message = "Access token is required")
    String accessToken,
    
    String refreshToken, // Optional, for invalidating refresh token
    Boolean logoutFromAllDevices // Optional, for global logout
) {
    /**
     * Constructor with only access token
     */
    public LogoutRequest(String accessToken) {
        this(accessToken, null, false);
    }
}