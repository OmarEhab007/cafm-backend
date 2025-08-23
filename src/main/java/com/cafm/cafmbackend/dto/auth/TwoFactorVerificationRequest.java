package com.cafm.cafmbackend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Two-factor authentication verification request.
 */
public record TwoFactorVerificationRequest(
    @NotBlank(message = "Session ID is required")
    String sessionId,
    
    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits")
    String code
) {}