package com.cafm.cafmbackend.dto.auth;

import com.cafm.cafmbackend.validation.constraint.StrongPassword;
import jakarta.validation.constraints.NotBlank;

/**
 * Change password request DTO.
 */
public record ChangePasswordRequest(
    @NotBlank(message = "Current password is required")
    String currentPassword,
    
    @NotBlank(message = "New password is required")
    @StrongPassword
    String newPassword,
    
    @NotBlank(message = "Password confirmation is required")
    String confirmPassword,
    
    // Optional: logout from other devices after password change
    Boolean logoutOtherSessions
) {
    /**
     * Validation: passwords must match
     */
    public boolean passwordsMatch() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
    
    /**
     * Constructor with required fields only
     */
    public ChangePasswordRequest(String currentPassword, String newPassword, String confirmPassword) {
        this(currentPassword, newPassword, confirmPassword, true);
    }
}