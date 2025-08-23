package com.cafm.cafmbackend.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Token refresh request DTO for obtaining new access token.
 */
public record TokenRefreshRequest(
    @NotBlank(message = "Refresh token is required")
    String refreshToken,
    
    // Optional device information for security
    String deviceId,
    String ipAddress
) {
    /**
     * Constructor with only required field
     */
    public TokenRefreshRequest(String refreshToken) {
        this(refreshToken, null, null);
    }
}