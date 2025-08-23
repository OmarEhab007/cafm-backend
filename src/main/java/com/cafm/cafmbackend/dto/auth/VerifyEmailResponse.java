package com.cafm.cafmbackend.dto.auth;

import java.util.UUID;

/**
 * Email verification response DTO.
 */
public record VerifyEmailResponse(
    UUID userId,
    String email,
    boolean verified,
    String message
) {
    public static VerifyEmailResponse success(UUID userId, String email) {
        return new VerifyEmailResponse(userId, email, true, "Email verified successfully");
    }
    
    public static VerifyEmailResponse error(String message) {
        return new VerifyEmailResponse(null, null, false, message);
    }
}