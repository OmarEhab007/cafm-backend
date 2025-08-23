package com.cafm.cafmbackend.dto.auth;

/**
 * Password reset response DTO.
 */
public record PasswordResetResponse(
    String message,
    boolean success
) {
    public static PasswordResetResponse successResponse() {
        return new PasswordResetResponse(
            "If the email exists, a password reset link has been sent.",
            true
        );
    }
    
    public static PasswordResetResponse errorResponse(String message) {
        return new PasswordResetResponse(message, false);
    }
}