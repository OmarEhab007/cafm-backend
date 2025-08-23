package com.cafm.cafmbackend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Reset password request DTO with token and new password.
 */
public record ResetPasswordRequest(
    @NotBlank(message = "Reset token is required")
    String token,
    
    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    String newPassword
) {
    /**
     * Alternative constructor for email-only reset request
     */
    public static ResetPasswordRequest forEmail(String email) {
        return new ResetPasswordRequest(null, null);
    }
}