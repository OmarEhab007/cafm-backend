package com.cafm.cafmbackend.dto.auth;

import java.util.UUID;

/**
 * Token validation response DTO.
 */
public record TokenValidationResponse(
    boolean valid,
    UUID userId,
    String email,
    Long expiresIn,
    String message
) {
    public static TokenValidationResponse valid(UUID userId, String email, Long expiresIn) {
        return new TokenValidationResponse(true, userId, email, expiresIn, "Token is valid");
    }
    
    public static TokenValidationResponse invalid(String message) {
        return new TokenValidationResponse(false, null, null, null, message);
    }
}