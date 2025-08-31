package com.cafm.cafmbackend.api.controllers;

import com.cafm.cafmbackend.dto.auth.*;
import com.cafm.cafmbackend.security.event.SecurityEventLogger;
import com.cafm.cafmbackend.application.service.AuthService;
import com.cafm.cafmbackend.configuration.web.SwaggerExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for authentication operations.
 * Handles login, registration, password reset, and token refresh.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final SecurityEventLogger securityEventLogger;

    public AuthController(AuthService authService,
                         SecurityEventLogger securityEventLogger) {
        this.authService = authService;
        this.securityEventLogger = securityEventLogger;
    }

    /**
     * Authenticate user and return JWT tokens.
     */
    @PostMapping("/login")
    @Operation(
        summary = "User Authentication", 
        description = "Authenticate user with email and password to obtain JWT tokens for API access. " +
                      "Supports multi-tenant authentication with automatic tenant detection.",
        tags = {"Authentication"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Authentication successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LoginResponse.class),
                examples = @ExampleObject(
                    name = "Successful Login",
                    summary = "Example of successful login response",
                    value = SwaggerExamples.Auth.LOGIN_RESPONSE
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid request parameters",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(
                    name = "Validation Error",
                    summary = "Invalid email format or missing password",
                    value = SwaggerExamples.Error.VALIDATION_ERROR
                )
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Invalid credentials",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(
                    name = "Invalid Credentials",
                    summary = "Wrong email or password",
                    value = SwaggerExamples.Auth.ERROR_RESPONSE
                )
            )
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Account locked, inactive, or suspended",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(
                    name = "Account Locked",
                    summary = "User account is locked or inactive",
                    value = SwaggerExamples.Error.FORBIDDEN_ERROR
                )
            )
        ),
        @ApiResponse(
            responseCode = "429", 
            description = "Too many login attempts",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/ErrorResponse"),
                examples = @ExampleObject(
                    name = "Rate Limited",
                    summary = "Too many failed login attempts",
                    value = SwaggerExamples.Error.RATE_LIMIT_ERROR
                )
            )
        )
    })
    public ResponseEntity<LoginResponse> login(
            @Parameter(
                description = "Login credentials with email and password",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginRequest.class),
                    examples = @ExampleObject(
                        name = "Admin Login",
                        summary = "Example admin login request",
                        value = SwaggerExamples.Auth.LOGIN_REQUEST
                    )
                )
            )
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        logger.info("Login attempt for email: {}", request.email());

        // Get IP address for audit logging
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            LoginResponse response = authService.login(request.email(), request.password(),
                                                      ipAddress, userAgent);

            // Log successful login
            securityEventLogger.logLoginAttempt(request.email(), ipAddress, true, "Login successful");
            logger.info("Successful login for user: {}", response.userId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log failed login
            securityEventLogger.logLoginAttempt(request.email(), ipAddress, false, e.getMessage());
            throw e;
        }
    }

    /**
     * Register a new user (admin only).
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Register new user", description = "Register a new user account (admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid registration data"),
        @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        logger.info("Registration request for email: {} by admin: {}",
                   request.email(), currentUser.getUsername());

        RegisterResponse response = authService.register(request);

        logger.info("Successfully registered user: {}", response.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Refresh access token using refresh token.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Get new access token using refresh token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {

        logger.debug("Token refresh request");

        try {
            TokenRefreshResponse response = authService.refreshToken(request.refreshToken());
            // Token refresh success is logged internally
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.warn("Token refresh failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Logout and invalidate tokens.
     */
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Logout", description = "Logout and invalidate current tokens")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest) {

        logger.info("Logout request for user: {}", currentUser.getUsername());

        // Extract token from header
        String token = authHeader.replace("Bearer ", "");
        authService.logout(currentUser.getUsername(), token);

        // Log logout event
        String ipAddress = getClientIpAddress(httpRequest);
        securityEventLogger.logLogout(currentUser.getUsername(), ipAddress);
        
        logger.info("Successfully logged out user: {}", currentUser.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * Request password reset.
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Request password reset link")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reset link sent if email exists"),
        @ApiResponse(responseCode = "400", description = "Invalid email")
    })
    public ResponseEntity<PasswordResetResponse> forgotPassword(
            @Valid @RequestBody PasswordResetRequest request) {

        logger.info("Password reset requested for email: {}", request.email());

        PasswordResetResponse response = authService.requestPasswordReset(request.email());

        // Always return success to prevent email enumeration
        return ResponseEntity.ok(response);
    }

    /**
     * Reset password with token.
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password using token from email")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset successful"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        logger.info("Password reset attempt with token");

        authService.resetPassword(request.token(), request.newPassword());

        logger.info("Password reset successful");
        return ResponseEntity.ok().build();
    }

    /**
     * Change password for authenticated user.
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Change password", description = "Change password for current user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password changed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid current password"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        logger.info("Password change request for user: {}", currentUser.getUsername());

        authService.changePassword(currentUser.getUsername(),
                                  request.currentPassword(),
                                  request.newPassword());

        logger.info("Password changed successfully for user: {}", currentUser.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * Verify email with token.
     */
    @GetMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verify email address with token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email verified successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    public ResponseEntity<VerifyEmailResponse> verifyEmail(
            @RequestParam @Parameter(description = "Verification token") String token) {

        logger.info("Email verification attempt with token");

        VerifyEmailResponse response = authService.verifyEmail(token);

        logger.info("Email verified successfully for user: {}", response.userId());
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user info.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Get current user", description = "Get information about authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User information retrieved"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<CurrentUserResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails currentUser) {

        logger.debug("Current user info requested for: {}", currentUser.getUsername());

        CurrentUserResponse response = authService.getCurrentUser(currentUser.getUsername());

        return ResponseEntity.ok(response);
    }

    /**
     * Validate token.
     */
    @GetMapping("/validate")
    @Operation(summary = "Validate token", description = "Check if token is valid")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token is valid"),
        @ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    })
    public ResponseEntity<TokenValidationResponse> validateToken(
            @RequestParam @Parameter(description = "JWT token to validate") String token) {

        logger.debug("Token validation request");

        TokenValidationResponse response = authService.validateToken(token);

        return ResponseEntity.ok(response);
    }

    /**
     * Enable two-factor authentication.
     */
    @PostMapping("/2fa/enable")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Enable 2FA", description = "Enable two-factor authentication")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "2FA enabled successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<TwoFactorSetupResponse> enable2FA(
            @AuthenticationPrincipal UserDetails currentUser) {

        logger.info("2FA enable request for user: {}", currentUser.getUsername());

        TwoFactorSetupResponse response = authService.enableTwoFactor(currentUser.getUsername());

        logger.info("2FA enabled for user: {}", currentUser.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Disable two-factor authentication.
     */
    @PostMapping("/2fa/disable")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Disable 2FA", description = "Disable two-factor authentication")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "2FA disabled successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Void> disable2FA(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody DisableTwoFactorRequest request) {

        logger.info("2FA disable request for user: {}", currentUser.getUsername());

        authService.disableTwoFactor(currentUser.getUsername(), request.password());

        logger.info("2FA disabled for user: {}", currentUser.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * Verify 2FA code during login.
     */
    @PostMapping("/2fa/verify")
    @Operation(summary = "Verify 2FA code", description = "Verify two-factor authentication code")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "2FA verification successful"),
        @ApiResponse(responseCode = "400", description = "Invalid code")
    })
    public ResponseEntity<LoginResponse> verify2FA(
            @Valid @RequestBody TwoFactorVerificationRequest request) {

        logger.info("2FA verification attempt");

        LoginResponse response = authService.verifyTwoFactor(request.sessionId(), request.code());

        logger.info("2FA verification successful for user: {}", response.userId());
        return ResponseEntity.ok(response);
    }

    // ========== Utility Methods ==========

    /**
     * Get client IP address from request.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}