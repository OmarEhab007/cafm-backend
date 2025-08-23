package com.cafm.cafmbackend.dto.auth;

import java.util.UUID;

/**
 * Minimal registration response DTO.
 */
public record RegisterResponse(
    UUID userId,
    String email,
    String temporaryPassword,
    String message
) {
    public RegisterResponse(UUID userId, String email, String temporaryPassword) {
        this(userId, email, temporaryPassword, "User registered successfully. Temporary password has been set.");
    }
}