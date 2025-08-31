package com.cafm.cafmbackend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login request DTO for user authentication.
 * Using Java record for immutable data transfer.
 */
@Schema(description = "Login request containing user credentials")
public record LoginRequest(
    @Schema(description = "User's email address", example = "user@example.com", required = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    String email,
    
    @Schema(description = "User's password", example = "SecurePassword123!", required = true)
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    String password,
    
    @Schema(description = "Unique device identifier for security tracking", example = "device-uuid-123")
    String deviceId,
    
    @Schema(description = "Human-readable device name", example = "iPhone 14 Pro")
    String deviceName,
    
    @Schema(description = "Client IP address", example = "192.168.1.100", hidden = true)
    String ipAddress,
    
    @Schema(description = "Client user agent string", hidden = true)
    String userAgent,
    
    @Schema(description = "Extend token expiration for convenience", example = "false", defaultValue = "false")
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