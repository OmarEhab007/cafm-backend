package com.cafm.cafmbackend.dto.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * Token refresh response DTO containing new access token.
 */
public record TokenRefreshResponse(
    String accessToken,
    String refreshToken, // New refresh token if rotation is enabled
    String tokenType,
    Long expiresIn, // seconds until access token expires
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime issuedAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime expiresAt
) {
    /**
     * Constructor without refresh token rotation
     */
    public TokenRefreshResponse(String accessToken, Long expiresIn) {
        this(
            accessToken,
            null,
            "Bearer",
            expiresIn,
            LocalDateTime.now(),
            LocalDateTime.now().plusSeconds(expiresIn)
        );
    }
    
    /**
     * Constructor with refresh token rotation
     */
    public TokenRefreshResponse(String accessToken, String refreshToken, Long expiresIn) {
        this(
            accessToken,
            refreshToken,
            "Bearer",
            expiresIn,
            LocalDateTime.now(),
            LocalDateTime.now().plusSeconds(expiresIn)
        );
    }
}