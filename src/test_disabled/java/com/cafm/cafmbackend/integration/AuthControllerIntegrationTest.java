package com.cafm.cafmbackend.integration;

import com.cafm.cafmbackend.data.entity.Company;
import com.cafm.cafmbackend.data.entity.User;
import com.cafm.cafmbackend.data.enums.UserStatus;
import com.cafm.cafmbackend.data.enums.UserType;
import com.cafm.cafmbackend.data.repository.CompanyRepository;
import com.cafm.cafmbackend.data.repository.UserRepository;
import com.cafm.cafmbackend.dto.auth.*;
import com.cafm.cafmbackend.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Integration tests for Authentication endpoints.
 * Tests the complete authentication flow with real HTTP requests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CompanyRepository companyRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    private Company testCompany;
    private User testUser;
    private User adminUser;
    
    private static final String AUTH_BASE_URL = "/api/v1/auth";
    private static final String TEST_EMAIL = "test@cafm.com";
    private static final String TEST_PASSWORD = "Test@123";
    private static final String ADMIN_EMAIL = "admin@cafm.com";
    private static final String ADMIN_PASSWORD = "Admin@123";
    
    @BeforeEach
    void setUp() {
        // Clear existing data
        userRepository.deleteAll();
        companyRepository.deleteAll();
        
        // Create test company
        testCompany = new Company();
        testCompany.setName("Test Company");
        testCompany.setStatus(com.cafm.cafmbackend.data.enums.CompanyStatus.ACTIVE);
        testCompany = companyRepository.save(testCompany);
        
        // Create test user
        testUser = new User();
        testUser.setEmail(TEST_EMAIL);
        testUser.setUsername("testuser");
        testUser.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setUserType(UserType.SUPERVISOR);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setIsActive(true);
        testUser.setIsLocked(false);
        testUser.setEmailVerified(true);
        testUser.setCompany(testCompany);
        testUser = userRepository.save(testUser);
        
        // Create admin user
        adminUser = new User();
        adminUser.setEmail(ADMIN_EMAIL);
        adminUser.setUsername("adminuser");
        adminUser.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setUserType(UserType.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setIsActive(true);
        adminUser.setIsLocked(false);
        adminUser.setEmailVerified(true);
        adminUser.setCompany(testCompany);
        adminUser = userRepository.save(adminUser);
    }
    
    // ========== Login Tests ==========
    
    @Test
    @DisplayName("Should successfully login with valid credentials")
    void login_WithValidCredentials_ShouldReturn200AndTokens() throws Exception {
        // Given
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        
        // When & Then
        MvcResult result = mockMvc.perform(post(AUTH_BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.email").value(TEST_EMAIL))
            .andExpect(jsonPath("$.userType").value("SUPERVISOR"))
            .andExpect(jsonPath("$.companyId").value(testCompany.getId().toString()))
            .andReturn();
        
        String response = result.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);
        
        // Verify tokens are valid
        assertThat(jwtTokenProvider.validateToken(loginResponse.accessToken())).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(loginResponse.accessToken())).isEqualTo(TEST_EMAIL);
    }
    
    @Test
    @DisplayName("Should fail login with invalid password")
    void login_WithInvalidPassword_ShouldReturn401() throws Exception {
        // Given
        LoginRequest request = new LoginRequest(TEST_EMAIL, "WrongPassword");
        
        // When & Then
        mockMvc.perform(post(AUTH_BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    @DisplayName("Should fail login with non-existent email")
    void login_WithNonExistentEmail_ShouldReturn401() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("nonexistent@cafm.com", TEST_PASSWORD);
        
        // When & Then
        mockMvc.perform(post(AUTH_BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("Should fail login when user is locked")
    void login_WithLockedUser_ShouldReturn401() throws Exception {
        // Given
        testUser.setIsLocked(true);
        userRepository.save(testUser);
        
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        
        // When & Then
        mockMvc.perform(post(AUTH_BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("locked")));
    }
    
    @Test
    @DisplayName("Should fail login when user is inactive")
    void login_WithInactiveUser_ShouldReturn401() throws Exception {
        // Given
        testUser.setIsActive(false);
        userRepository.save(testUser);
        
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        
        // When & Then
        mockMvc.perform(post(AUTH_BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("inactive")));
    }
    
    // ========== Registration Tests ==========
    
    @Test
    @DisplayName("Admin should successfully register new user")
    void register_AsAdmin_ShouldReturn201AndUserDetails() throws Exception {
        // Given
        String adminToken = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        
        RegisterRequest request = new RegisterRequest(
            "newuser@cafm.com",
            "New",
            "User",
            UserType.TECHNICIAN,
            testCompany.getId(),
            "+1234567890",
            "EMP001"
        );
        
        // When & Then
        MvcResult result = mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("newuser@cafm.com"))
            .andExpect(jsonPath("$.temporaryPassword").isNotEmpty())
            .andExpect(jsonPath("$.message").value("User registered successfully"))
            .andReturn();
        
        // Verify user was created
        assertThat(userRepository.existsByEmailIgnoreCase("newuser@cafm.com")).isTrue();
    }
    
    @Test
    @DisplayName("Non-admin should not be able to register users")
    void register_AsNonAdmin_ShouldReturn403() throws Exception {
        // Given
        String userToken = loginAndGetToken(TEST_EMAIL, TEST_PASSWORD);
        
        RegisterRequest request = new RegisterRequest(
            "newuser@cafm.com",
            "New",
            "User",
            UserType.TECHNICIAN,
            testCompany.getId(),
            null,
            null
        );
        
        // When & Then
        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("Should fail registration with duplicate email")
    void register_WithDuplicateEmail_ShouldReturn400() throws Exception {
        // Given
        String adminToken = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        
        RegisterRequest request = new RegisterRequest(
            TEST_EMAIL, // Existing email
            "Duplicate",
            "User",
            UserType.TECHNICIAN,
            testCompany.getId(),
            null,
            null
        );
        
        // When & Then
        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already exists")));
    }
    
    // ========== Token Refresh Tests ==========
    
    @Test
    @DisplayName("Should successfully refresh access token")
    void refreshToken_WithValidRefreshToken_ShouldReturnNewAccessToken() throws Exception {
        // Given - Login first to get refresh token
        String refreshToken = loginAndGetRefreshToken(TEST_EMAIL, TEST_PASSWORD);
        
        TokenRefreshRequest request = new TokenRefreshRequest(refreshToken);
        
        // When & Then
        MvcResult result = mockMvc.perform(post(AUTH_BASE_URL + "/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").value(refreshToken))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andReturn();
        
        String response = result.getResponse().getContentAsString();
        TokenRefreshResponse refreshResponse = objectMapper.readValue(response, TokenRefreshResponse.class);
        
        // Verify new access token is valid
        assertThat(jwtTokenProvider.validateToken(refreshResponse.accessToken())).isTrue();
    }
    
    @Test
    @DisplayName("Should fail refresh with invalid token")
    void refreshToken_WithInvalidToken_ShouldReturn401() throws Exception {
        // Given
        TokenRefreshRequest request = new TokenRefreshRequest("invalid.refresh.token");
        
        // When & Then
        mockMvc.perform(post(AUTH_BASE_URL + "/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isUnauthorized());
    }
    
    // ========== Logout Tests ==========
    
    @Test
    @DisplayName("Should successfully logout authenticated user")
    void logout_WithValidToken_ShouldReturn200() throws Exception {
        // Given
        String accessToken = loginAndGetToken(TEST_EMAIL, TEST_PASSWORD);
        
        // When & Then
        mockMvc.perform(post(AUTH_BASE_URL + "/logout")
                .header("Authorization", "Bearer " + accessToken))
            .andDo(print())
            .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("Should fail logout without authentication")
    void logout_WithoutToken_ShouldReturn401() throws Exception {
        // When & Then
        mockMvc.perform(post(AUTH_BASE_URL + "/logout"))
            .andDo(print())
            .andExpect(status().isUnauthorized());
    }
    
    // ========== Current User Tests ==========
    
    @Test
    @DisplayName("Should get current user information")
    void getCurrentUser_WithValidToken_ShouldReturnUserInfo() throws Exception {
        // Given
        String accessToken = loginAndGetToken(TEST_EMAIL, TEST_PASSWORD);
        
        // When & Then
        mockMvc.perform(get(AUTH_BASE_URL + "/current-user")
                .header("Authorization", "Bearer " + accessToken))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(TEST_EMAIL))
            .andExpect(jsonPath("$.firstName").value("Test"))
            .andExpect(jsonPath("$.lastName").value("User"))
            .andExpect(jsonPath("$.userType").value("SUPERVISOR"))
            .andExpect(jsonPath("$.companyId").value(testCompany.getId().toString()));
    }
    
    @Test
    @DisplayName("Should fail to get current user without authentication")
    void getCurrentUser_WithoutToken_ShouldReturn401() throws Exception {
        // When & Then
        mockMvc.perform(get(AUTH_BASE_URL + "/current-user"))
            .andDo(print())
            .andExpect(status().isUnauthorized());
    }
    
    // ========== Change Password Tests ==========
    
//    @Test
//    @DisplayName("Should successfully change password")
//    void changePassword_WithCorrectCurrentPassword_ShouldReturn200() throws Exception {
//        // Given
//        String accessToken = loginAndGetToken(TEST_EMAIL, TEST_PASSWORD);
//
//        ChangePasswordRequest request = new ChangePasswordRequest(
//            TEST_PASSWORD,
//            "NewPassword@123"
//        );
//
//        // When & Then
//        mockMvc.perform(post(AUTH_BASE_URL + "/change-password")
//                .header("Authorization", "Bearer " + accessToken)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//            .andDo(print())
//            .andExpect(status().isOk())
//            .andExpect(jsonPath("$.message").value("Password changed successfully"));
//
//        // Verify new password works
//        User updatedUser = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
//        assertThat(passwordEncoder.matches("NewPassword@123", updatedUser.getPasswordHash())).isTrue();
//    }
    
//    @Test
//    @DisplayName("Should fail password change with incorrect current password")
//    void changePassword_WithIncorrectCurrentPassword_ShouldReturn400() throws Exception {
//        // Given
//        String accessToken = loginAndGetToken(TEST_EMAIL, TEST_PASSWORD);
//
//        ChangePasswordRequest request = new ChangePasswordRequest(
//            "WrongCurrentPassword",
//            "NewPassword@123"
//        );
//
//        // When & Then
//        mockMvc.perform(post(AUTH_BASE_URL + "/change-password")
//                .header("Authorization", "Bearer " + accessToken)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//            .andDo(print())
//            .andExpect(status().isBadRequest())
//            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("incorrect")));
//    }
//
    // ========== Forgot Password Tests ==========
    
    @Test
    @DisplayName("Should request password reset successfully")
    void forgotPassword_WithValidEmail_ShouldReturn200() throws Exception {
        // Given
        PasswordResetRequest request = new PasswordResetRequest(TEST_EMAIL);
        
        // When & Then
        mockMvc.perform(post(AUTH_BASE_URL + "/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.success").value(true));
    }
    
    @Test
    @DisplayName("Should return success even for non-existent email (security)")
    void forgotPassword_WithNonExistentEmail_ShouldReturn200() throws Exception {
        // Given
        PasswordResetRequest request = new PasswordResetRequest("nonexistent@cafm.com");
        
        // When & Then - Should return success to prevent email enumeration
        mockMvc.perform(post(AUTH_BASE_URL + "/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
    
    // ========== Helper Methods ==========
    
    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest(email, password);
        
        MvcResult result = mockMvc.perform(post(AUTH_BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn();
        
        String response = result.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);
        
        return loginResponse.accessToken();
    }
    
    private String loginAndGetRefreshToken(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest(email, password);
        
        MvcResult result = mockMvc.perform(post(AUTH_BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn();
        
        String response = result.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);
        
        return loginResponse.refreshToken();
    }
}