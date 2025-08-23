package com.cafm.cafmbackend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login request DTO for user authentication.
 * Using Java record for immutable data transfer.
 */
public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    String password,
    
    // Optional fields for enhanced security
    String deviceId,
    String deviceName,
    String ipAddress,
    String userAgent,
    
    // Remember me option for longer token expiry
    Boolean rememberMe
) {
    /**
     * Constructor with only required fields
     */
    public LoginRequest(String email, String password) {
        this(email, password, null, null, null, null, false);
    }
    
    /**
     * Constructor with remember me option
     */
    public LoginRequest(String email, String password, Boolean rememberMe) {
        this(email, password, null, null, null, null, rememberMe);
    }
}