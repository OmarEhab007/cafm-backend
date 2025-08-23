package com.cafm.cafmbackend.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to disable two-factor authentication.
 */
public record DisableTwoFactorRequest(
    @NotBlank(message = "Password is required")
    String password
) {}