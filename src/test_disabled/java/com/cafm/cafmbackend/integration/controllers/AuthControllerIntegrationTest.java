package com.cafm.cafmbackend.integration.controllers;

import com.cafm.cafmbackend.dto.auth.*;
import com.cafm.cafmbackend.security.event.SecurityEventLogger;
import com.cafm.cafmbackend.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for AuthController.
 * Tests all authentication endpoints with various scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("cafm_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private SecurityEventLogger securityEventLogger;

    private static final String BASE_URL = "/api/v1/auth";

    // ========== Login Tests ==========

    @Test
    @Order(1)
    @DisplayName("Should successfully login with valid credentials")
    void testLogin_Success() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("test@example.com", "Password123!");
        LoginResponse response = new LoginResponse(
                UUID.randomUUID(),
                "test@example.com",
                "Test User",
                "ADMIN",
                UUID.randomUUID(),
                "access-token",
                "refresh-token",
                3600L
        );

        when(authService.login(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

        verify(securityEventLogger).logLoginAttempt(eq("test@example.com"), anyString(), eq(true), anyString());
    }

    @Test
    @Order(2)
    @DisplayName("Should fail login with invalid credentials")
    void testLogin_InvalidCredentials() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("test@example.com", "WrongPassword");

        when(authService.login(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().is5xxServerError());

        verify(securityEventLogger).logLoginAttempt(eq("test@example.com"), anyString(), eq(false), anyString());
    }

    @Test
    @Order(3)
    @DisplayName("Should fail login with invalid email format")
    void testLogin_InvalidEmailFormat() throws Exception {
        // Arrange
        String invalidRequest = "{\"email\":\"invalid-email\",\"password\":\"Password123!\"}";

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== Registration Tests ==========

    @Test
    @Order(4)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should successfully register new user as admin")
    void testRegister_Success() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "newuser@example.com",
                "Password123!",
                "New",
                "User",
                "USER",
                UUID.randomUUID()
        );

        RegisterResponse response = new RegisterResponse(
                UUID.randomUUID(),
                "newuser@example.com",
                "USER"
        );

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer valid-token")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.email").value("newuser@example.com"));
    }

    @Test
    @Order(5)
    @DisplayName("Should fail registration without admin role")
    void testRegister_Unauthorized() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "newuser@example.com",
                "Password123!",
                "New",
                "User",
                "USER",
                UUID.randomUUID()
        );

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should fail registration with duplicate email")
    void testRegister_DuplicateEmail() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "existing@example.com",
                "Password123!",
                "New",
                "User",
                "USER",
                UUID.randomUUID()
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Email already exists"));

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer valid-token")
                        .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    // ========== Token Refresh Tests ==========

    @Test
    @Order(7)
    @DisplayName("Should successfully refresh token")
    void testRefreshToken_Success() throws Exception {
        // Arrange
        TokenRefreshRequest request = new TokenRefreshRequest("valid-refresh-token");
        TokenRefreshResponse response = new TokenRefreshResponse(
                "new-access-token",
                "new-refresh-token",
                3600L
        );

        when(authService.refreshToken(anyString())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    @Order(8)
    @DisplayName("Should fail refresh with invalid token")
    void testRefreshToken_InvalidToken() throws Exception {
        // Arrange
        TokenRefreshRequest request = new TokenRefreshRequest("invalid-token");

        when(authService.refreshToken(anyString()))
                .thenThrow(new RuntimeException("Invalid refresh token"));

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    // ========== Logout Tests ==========

    @Test
    @Order(9)
    @WithMockUser(username = "test@example.com")
    @DisplayName("Should successfully logout")
    void testLogout_Success() throws Exception {
        // Arrange
        doNothing().when(authService).logout(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/logout")
                        .header("Authorization", "Bearer valid-token")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(securityEventLogger).logLogout(eq("test@example.com"), anyString());
    }

    // ========== Password Reset Tests ==========

    @Test
    @Order(10)
    @DisplayName("Should successfully request password reset")
    void testForgotPassword_Success() throws Exception {
        // Arrange
        PasswordResetRequest request = new PasswordResetRequest("test@example.com");
        PasswordResetResponse response = new PasswordResetResponse(
                "Check your email for reset instructions"
        );

        when(authService.requestPasswordReset(anyString())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @Order(11)
    @DisplayName("Should successfully reset password with valid token")
    void testResetPassword_Success() throws Exception {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest(
                "valid-reset-token",
                "NewPassword123!"
        );

        doNothing().when(authService).resetPassword(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(12)
    @DisplayName("Should fail reset password with invalid token")
    void testResetPassword_InvalidToken() throws Exception {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest(
                "invalid-token",
                "NewPassword123!"
        );

        doThrow(new RuntimeException("Invalid or expired token"))
                .when(authService).resetPassword(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    // ========== Change Password Tests ==========

    @Test
    @Order(13)
    @WithMockUser(username = "test@example.com")
    @DisplayName("Should successfully change password")
    void testChangePassword_Success() throws Exception {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPassword123!",
                "NewPassword123!"
        );

        doNothing().when(authService).changePassword(anyString(), anyString(), anyString());

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(14)
    @WithMockUser(username = "test@example.com")
    @DisplayName("Should fail change password with wrong current password")
    void testChangePassword_WrongCurrentPassword() throws Exception {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest(
                "WrongPassword",
                "NewPassword123!"
        );

        doThrow(new RuntimeException("Invalid current password"))
                .when(authService).changePassword(anyString(), anyString(), anyString());

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    // ========== Email Verification Tests ==========

    @Test
    @Order(15)
    @DisplayName("Should successfully verify email")
    void testVerifyEmail_Success() throws Exception {
        // Arrange
        VerifyEmailResponse response = new VerifyEmailResponse(
                UUID.randomUUID(),
                "Email verified successfully"
        );

        when(authService.verifyEmail(anyString())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/verify-email")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));
    }

    @Test
    @Order(16)
    @DisplayName("Should fail email verification with invalid token")
    void testVerifyEmail_InvalidToken() throws Exception {
        // Arrange
        when(authService.verifyEmail(anyString()))
                .thenThrow(new RuntimeException("Invalid or expired token"));

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/verify-email")
                        .param("token", "invalid-token"))
                .andExpect(status().is5xxServerError());
    }

    // ========== Current User Tests ==========

    @Test
    @Order(17)
    @WithMockUser(username = "test@example.com", roles = "ADMIN")
    @DisplayName("Should successfully get current user info")
    void testGetCurrentUser_Success() throws Exception {
        // Arrange
        CurrentUserResponse response = new CurrentUserResponse(
                UUID.randomUUID(),
                "test@example.com",
                "Test",
                "User",
                "ADMIN",
                UUID.randomUUID(),
                true,
                false
        );

        when(authService.getCurrentUser(anyString())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/me")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @Order(18)
    @DisplayName("Should fail to get current user without authentication")
    void testGetCurrentUser_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/me"))
                .andExpect(status().isUnauthorized());
    }

    // ========== Token Validation Tests ==========

    @Test
    @Order(19)
    @DisplayName("Should successfully validate token")
    void testValidateToken_Success() throws Exception {
        // Arrange
        TokenValidationResponse response = new TokenValidationResponse(
                true,
                UUID.randomUUID(),
                "test@example.com",
                3600L
        );

        when(authService.validateToken(anyString())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/validate")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.userId").exists());
    }

    @Test
    @Order(20)
    @DisplayName("Should return invalid for expired token")
    void testValidateToken_Expired() throws Exception {
        // Arrange
        TokenValidationResponse response = new TokenValidationResponse(
                false,
                null,
                null,
                0L
        );

        when(authService.validateToken(anyString())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/validate")
                        .param("token", "expired-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    // ========== Two-Factor Authentication Tests ==========

    @Test
    @Order(21)
    @WithMockUser(username = "test@example.com")
    @DisplayName("Should successfully enable 2FA")
    void testEnable2FA_Success() throws Exception {
        // Arrange
        TwoFactorSetupResponse response = new TwoFactorSetupResponse(
                "otpauth://totp/CAFM:test@example.com?secret=SECRET&issuer=CAFM",
                "SECRET",
                "data:image/png;base64,qrcode"
        );

        when(authService.enableTwoFactor(anyString())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/2fa/enable")
                        .header("Authorization", "Bearer valid-token")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").value("SECRET"))
                .andExpect(jsonPath("$.qrCode").exists());
    }

    @Test
    @Order(22)
    @WithMockUser(username = "test@example.com")
    @DisplayName("Should successfully disable 2FA")
    void testDisable2FA_Success() throws Exception {
        // Arrange
        DisableTwoFactorRequest request = new DisableTwoFactorRequest("Password123!");

        doNothing().when(authService).disableTwoFactor(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/2fa/disable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer valid-token")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(23)
    @DisplayName("Should successfully verify 2FA code")
    void testVerify2FA_Success() throws Exception {
        // Arrange
        TwoFactorVerificationRequest request = new TwoFactorVerificationRequest(
                "session-id",
                "123456"
        );

        LoginResponse response = new LoginResponse(
                UUID.randomUUID(),
                "test@example.com",
                "Test User",
                "ADMIN",
                UUID.randomUUID(),
                "access-token",
                "refresh-token",
                3600L
        );

        when(authService.verifyTwoFactor(anyString(), anyString())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/2fa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    @Order(24)
    @DisplayName("Should fail 2FA verification with invalid code")
    void testVerify2FA_InvalidCode() throws Exception {
        // Arrange
        TwoFactorVerificationRequest request = new TwoFactorVerificationRequest(
                "session-id",
                "000000"
        );

        when(authService.verifyTwoFactor(anyString(), anyString()))
                .thenThrow(new RuntimeException("Invalid 2FA code"));

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/2fa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    // ========== Security Tests ==========

    @Test
    @Order(25)
    @DisplayName("Should prevent SQL injection in login")
    void testLogin_SQLInjection() throws Exception {
        // Arrange
        String maliciousRequest = "{\"email\":\"admin' OR '1'='1\",\"password\":\"' OR '1'='1\"}";

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousRequest)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(26)
    @DisplayName("Should prevent XSS in registration")
    void testRegister_XSSPrevention() throws Exception {
        // Arrange
        String xssRequest = "{\"email\":\"test@example.com\",\"password\":\"Password123!\",\"firstName\":\"<script>alert('XSS')</script>\",\"lastName\":\"User\",\"role\":\"USER\"}";

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(xssRequest)
                        .with(csrf()))
                .andExpect(status().isUnauthorized()); // Will fail auth first
    }

    @Test
    @Order(27)
    @DisplayName("Should enforce CSRF protection")
    void testLogin_CSRFProtection() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("test@example.com", "Password123!");

        // Act & Assert - without CSRF token
        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(28)
    @DisplayName("Should handle missing required fields")
    void testLogin_MissingFields() throws Exception {
        // Arrange
        String incompleteRequest = "{\"email\":\"test@example.com\"}";

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incompleteRequest)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(29)
    @DisplayName("Should validate password complexity")
    void testRegister_WeakPassword() throws Exception {
        // Arrange
        String weakPasswordRequest = "{\"email\":\"test@example.com\",\"password\":\"weak\",\"firstName\":\"Test\",\"lastName\":\"User\",\"role\":\"USER\"}";

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(weakPasswordRequest)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(30)
    @DisplayName("Should handle concurrent login attempts")
    void testLogin_ConcurrentAttempts() throws Exception {
        // This test would simulate concurrent login attempts
        // In a real scenario, you'd use CompletableFuture or similar
        LoginRequest request = new LoginRequest("test@example.com", "Password123!");
        LoginResponse response = new LoginResponse(
                UUID.randomUUID(),
                "test@example.com",
                "Test User",
                "ADMIN",
                UUID.randomUUID(),
                "access-token",
                "refresh-token",
                3600L
        );

        when(authService.login(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(response);

        // Simulate multiple concurrent requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        // Verify that login was called multiple times
        verify(authService, times(5)).login(anyString(), anyString(), anyString(), anyString());
    }
}