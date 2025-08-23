package com.cafm.cafmbackend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Reset password confirmation DTO - Step 2: Reset with token.
 */
public record ResetPasswordConfirmRequest(
    @NotBlank(message = "Reset token is required")
    String resetToken,
    
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
        message = "Password must contain at least one digit, one lowercase, one uppercase, one special character, and no whitespace"
    )
    String newPassword,
    
    @NotBlank(message = "Password confirmation is required")
    String confirmPassword
) {
    /**
     * Validation: passwords must match
     */
    public boolean passwordsMatch() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}