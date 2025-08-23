package com.cafm.cafmbackend.unit.service;

import com.cafm.cafmbackend.data.entity.Company;
import com.cafm.cafmbackend.data.entity.User;
import com.cafm.cafmbackend.data.enums.UserStatus;
import com.cafm.cafmbackend.data.enums.UserType;
import com.cafm.cafmbackend.data.repository.UserRepository;
import com.cafm.cafmbackend.dto.auth.*;
import com.cafm.cafmbackend.exception.ResourceNotFoundException;
import com.cafm.cafmbackend.security.JwtTokenProvider;
import com.cafm.cafmbackend.security.service.LoginAttemptService;
import com.cafm.cafmbackend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * Tests authentication logic, token generation, and security features.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private LoginAttemptService loginAttemptService;
    
    @InjectMocks
    private AuthService authService;
    
    private User testUser;
    private Company testCompany;
    private static final String TEST_EMAIL = "test@cafm.com";
    private static final String TEST_PASSWORD = "Test@123";
    private static final String ENCODED_PASSWORD = "$2a$10$encoded";
    private static final String ACCESS_TOKEN = "access.token.here";
    private static final String REFRESH_TOKEN = "refresh.token.here";
    
    @BeforeEach
    void setUp() {
        testCompany = new Company();
        testCompany.setId(UUID.randomUUID());
        testCompany.setName("Test Company");
        
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail(TEST_EMAIL);
        testUser.setPasswordHash(ENCODED_PASSWORD);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setUserType(UserType.ADMIN);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setIsActive(true);
        testUser.setIsLocked(false);
        testUser.setCompany(testCompany);
        testUser.setLastLoginAt(LocalDateTime.now().minusDays(1));
    }
    
    // ========== Login Tests ==========
    
    @Test
    @DisplayName("Should successfully login with valid credentials")
    void login_WithValidCredentials_ShouldReturnLoginResponse() {
        // Given
        when(loginAttemptService.isBlocked(TEST_EMAIL)).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtTokenProvider.generateAccessTokenWithClaims(testUser)).thenReturn(ACCESS_TOKEN);
        when(jwtTokenProvider.generateRefreshTokenWithUserId(testUser)).thenReturn(REFRESH_TOKEN);
        
        // When
        LoginResponse response = authService.login(TEST_EMAIL, TEST_PASSWORD, "127.0.0.1", "TestAgent");
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(response.email()).isEqualTo(TEST_EMAIL);
        assertThat(response.userId()).isEqualTo(testUser.getId());
        assertThat(response.userType()).isEqualTo(UserType.ADMIN);
        
        verify(loginAttemptService).loginSucceeded(TEST_EMAIL);
        verify(userRepository).save(testUser);
    }
    
    @Test
    @DisplayName("Should fail login with invalid password")
    void login_WithInvalidPassword_ShouldThrowBadCredentials() {
        // Given
        when(loginAttemptService.isBlocked(TEST_EMAIL)).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), eq(ENCODED_PASSWORD))).thenReturn(false);
        when(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).thenReturn(4);
        
        // When/Then
        assertThatThrownBy(() -> 
            authService.login(TEST_EMAIL, "wrongpassword", "127.0.0.1", "TestAgent")
        )
        .isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("4 attempts remaining");
        
        verify(loginAttemptService).loginFailed(TEST_EMAIL);
        verify(loginAttemptService, never()).loginSucceeded(any());
    }
    
    @Test
    @DisplayName("Should block login when account is locked due to failed attempts")
    void login_WithBlockedAccount_ShouldThrowLockedException() {
        // Given
        when(loginAttemptService.isBlocked(TEST_EMAIL)).thenReturn(true);
        
        // When/Then
        assertThatThrownBy(() -> 
            authService.login(TEST_EMAIL, TEST_PASSWORD, "127.0.0.1", "TestAgent")
        )
        .isInstanceOf(LockedException.class)
        .hasMessageContaining("temporarily locked");
        
        verify(userRepository, never()).findByEmailIgnoreCase(any());
    }
    
    @Test
    @DisplayName("Should fail login when user not found")
    void login_WithNonExistentUser_ShouldThrowException() {
        // Given
        when(loginAttemptService.isBlocked(TEST_EMAIL)).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.empty());
        when(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).thenReturn(4);
        
        // When/Then
        assertThatThrownBy(() -> 
            authService.login(TEST_EMAIL, TEST_PASSWORD, "127.0.0.1", "TestAgent")
        )
        .isInstanceOf(BadCredentialsException.class);
        
        verify(loginAttemptService).loginFailed(TEST_EMAIL);
    }
    
    @Test
    @DisplayName("Should fail login when user is inactive")
    void login_WithInactiveUser_ShouldThrowException() {
        // Given
        testUser.setIsActive(false);
        when(loginAttemptService.isBlocked(TEST_EMAIL)).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        
        // When/Then
        assertThatThrownBy(() -> 
            authService.login(TEST_EMAIL, TEST_PASSWORD, "127.0.0.1", "TestAgent")
        )
        .isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("Account is inactive");
    }
    
    @Test
    @DisplayName("Should fail login when user is locked by admin")
    void login_WithAdminLockedUser_ShouldThrowException() {
        // Given
        testUser.setIsLocked(true);
        when(loginAttemptService.isBlocked(TEST_EMAIL)).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        
        // When/Then
        assertThatThrownBy(() -> 
            authService.login(TEST_EMAIL, TEST_PASSWORD, "127.0.0.1", "TestAgent")
        )
        .isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("locked by administrator");
    }
    
    // ========== Registration Tests ==========
    
    @Test
    @DisplayName("Should successfully register new user")
    void register_WithValidData_ShouldCreateUser() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "newuser@cafm.com",
            "New",
            "User",
            UserType.SUPERVISOR,
            testCompany.getId(),
            "+1234567890",
            "EMP001"
        );
        
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        
        // When
        RegisterResponse response = authService.register(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.temporaryPassword()).isNotNull();
        assertThat(response.temporaryPassword()).hasSize(8);
        
        verify(userRepository).save(argThat(user -> 
            user.getEmail().equals(request.email()) &&
            user.getUserType() == UserType.SUPERVISOR &&
            user.getStatus() == UserStatus.ACTIVE &&
            user.getIsActive() &&
            !user.getEmailVerified()
        ));
    }
    
    @Test
    @DisplayName("Should fail registration with duplicate email")
    void register_WithDuplicateEmail_ShouldThrowException() {
        // Given
        RegisterRequest request = new RegisterRequest(
            TEST_EMAIL,
            "New",
            "User",
            UserType.SUPERVISOR,
            testCompany.getId(),
            null,
            null
        );
        
        when(userRepository.existsByEmailIgnoreCase(TEST_EMAIL)).thenReturn(true);
        
        // When/Then
        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email already exists");
        
        verify(userRepository, never()).save(any());
    }
    
    // ========== Token Refresh Tests ==========
    
    @Test
    @DisplayName("Should successfully refresh access token")
    void refreshToken_WithValidRefreshToken_ShouldReturnNewAccessToken() {
        // Given
        when(jwtTokenProvider.validateToken(REFRESH_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(REFRESH_TOKEN)).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessTokenWithClaims(testUser)).thenReturn("new.access.token");
        
        // When
        TokenRefreshResponse response = authService.refreshToken(REFRESH_TOKEN);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("new.access.token");
        assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
    }
    
    @Test
    @DisplayName("Should fail refresh with invalid token")
    void refreshToken_WithInvalidToken_ShouldThrowException() {
        // Given
        when(jwtTokenProvider.validateToken(REFRESH_TOKEN)).thenReturn(false);
        
        // When/Then
        assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid refresh token");
    }
    
    @Test
    @DisplayName("Should fail refresh when user not found")
    void refreshToken_WithNonExistentUser_ShouldThrowException() {
        // Given
        when(jwtTokenProvider.validateToken(REFRESH_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(REFRESH_TOKEN)).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");
    }
    
    // ========== Password Change Tests ==========
    
    @Test
    @DisplayName("Should successfully change password")
    void changePassword_WithCorrectCurrentPassword_ShouldUpdatePassword() {
        // Given
        String newPassword = "NewPassword@123";
        String encodedNewPassword = "$2a$10$newencoded";
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
        
        // When
        authService.changePassword(TEST_EMAIL, TEST_PASSWORD, newPassword);
        
        // Then
        verify(userRepository).save(argThat(user -> 
            user.getPasswordHash().equals(encodedNewPassword)
        ));
    }
    
    @Test
    @DisplayName("Should fail password change with incorrect current password")
    void changePassword_WithIncorrectCurrentPassword_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), eq(ENCODED_PASSWORD))).thenReturn(false);
        
        // When/Then
        assertThatThrownBy(() -> 
            authService.changePassword(TEST_EMAIL, "wrongpassword", "NewPassword@123")
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Current password is incorrect");
        
        verify(userRepository, never()).save(any());
    }
    
    // ========== Get Current User Tests ==========
    
    @Test
    @DisplayName("Should successfully get current user info")
    void getCurrentUser_WithValidUsername_ShouldReturnUserInfo() {
        // Given
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        
        // When
        CurrentUserResponse response = authService.getCurrentUser(TEST_EMAIL);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(testUser.getId());
        assertThat(response.email()).isEqualTo(TEST_EMAIL);
        assertThat(response.firstName()).isEqualTo("Test");
        assertThat(response.lastName()).isEqualTo("User");
        assertThat(response.userType()).isEqualTo(UserType.ADMIN);
        assertThat(response.companyId()).isEqualTo(testCompany.getId());
        assertThat(response.isActive()).isTrue();
        assertThat(response.isLocked()).isFalse();
    }
    
    @Test
    @DisplayName("Should fail to get current user when not found")
    void getCurrentUser_WithNonExistentUser_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> authService.getCurrentUser(TEST_EMAIL))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");
    }
    
    // ========== Token Validation Tests ==========
    
    @Test
    @DisplayName("Should validate token successfully")
    void validateToken_WithValidToken_ShouldReturnValidResponse() {
        // Given
        when(jwtTokenProvider.validateToken(ACCESS_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(ACCESS_TOKEN)).thenReturn(TEST_EMAIL);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.getExpirationTime(ACCESS_TOKEN)).thenReturn(3600L);
        
        // When
        TokenValidationResponse response = authService.validateToken(ACCESS_TOKEN);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.valid()).isTrue();
        assertThat(response.userId()).isEqualTo(testUser.getId());
        assertThat(response.email()).isEqualTo(TEST_EMAIL);
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }
    
    @Test
    @DisplayName("Should return invalid response for invalid token")
    void validateToken_WithInvalidToken_ShouldReturnInvalidResponse() {
        // Given
        when(jwtTokenProvider.validateToken(ACCESS_TOKEN)).thenReturn(false);
        
        // When
        TokenValidationResponse response = authService.validateToken(ACCESS_TOKEN);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.valid()).isFalse();
        assertThat(response.message()).contains("Invalid token");
    }
}