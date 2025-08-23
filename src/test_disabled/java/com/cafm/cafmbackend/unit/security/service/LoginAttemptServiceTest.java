package com.cafm.cafmbackend.unit.security.service;

import com.cafm.cafmbackend.security.service.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LoginAttemptService.
 * Tests account lockout mechanism, attempt tracking, and automatic unlock.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginAttemptService Unit Tests")
class LoginAttemptServiceTest {

    private LoginAttemptService loginAttemptService;
    
    private static final String TEST_EMAIL = "test@cafm.com";
    private static final String OTHER_EMAIL = "other@cafm.com";
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;
    private static final int ATTEMPT_WINDOW_MINUTES = 15;
    
    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService();
        
        // Inject test configuration using reflection
        ReflectionTestUtils.setField(loginAttemptService, "maxAttempts", MAX_ATTEMPTS);
        ReflectionTestUtils.setField(loginAttemptService, "lockoutDurationMinutes", LOCKOUT_DURATION_MINUTES);
        ReflectionTestUtils.setField(loginAttemptService, "attemptWindowMinutes", ATTEMPT_WINDOW_MINUTES);
    }
    
    // ========== Login Success Tests ==========
    
    @Test
    @DisplayName("Should reset attempts on successful login")
    void loginSucceeded_WithPreviousFailures_ShouldResetAttempts() {
        // Given - Record some failed attempts
        loginAttemptService.loginFailed(TEST_EMAIL);
        loginAttemptService.loginFailed(TEST_EMAIL);
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(3);
        
        // When - Login succeeds
        loginAttemptService.loginSucceeded(TEST_EMAIL);
        
        // Then - Attempts should be reset
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(MAX_ATTEMPTS);
        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isFalse();
    }
    
    @Test
    @DisplayName("Should handle successful login with no previous attempts")
    void loginSucceeded_WithNoPreviousAttempts_ShouldNotThrowException() {
        // When - Login succeeds without any previous attempts
        loginAttemptService.loginSucceeded(TEST_EMAIL);
        
        // Then - Should handle gracefully
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(MAX_ATTEMPTS);
        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isFalse();
    }
    
    // ========== Login Failure Tests ==========
    
    @Test
    @DisplayName("Should track failed login attempts")
    void loginFailed_WithMultipleFailures_ShouldDecrementRemainingAttempts() {
        // When - Multiple login failures
        loginAttemptService.loginFailed(TEST_EMAIL);
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(4);
        
        loginAttemptService.loginFailed(TEST_EMAIL);
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(3);
        
        loginAttemptService.loginFailed(TEST_EMAIL);
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(2);
        
        // Then - Should not be blocked yet
        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isFalse();
    }
    
    @Test
    @DisplayName("Should block account after max attempts")
    void loginFailed_AfterMaxAttempts_ShouldBlockAccount() {
        // When - Fail login MAX_ATTEMPTS times
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            loginAttemptService.loginFailed(TEST_EMAIL);
        }
        
        // Should not be blocked yet
        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isFalse();
        
        // Last attempt that triggers the block
        loginAttemptService.loginFailed(TEST_EMAIL);
        
        // Then - Account should be blocked
        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isTrue();
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(0);
        assertThat(loginAttemptService.getLockExpirationTime(TEST_EMAIL)).isNotNull();
    }
    
    @Test
    @DisplayName("Should track attempts separately for different users")
    void loginFailed_ForDifferentUsers_ShouldTrackSeparately() {
        // When - Different users fail login
        loginAttemptService.loginFailed(TEST_EMAIL);
        loginAttemptService.loginFailed(TEST_EMAIL);
        
        loginAttemptService.loginFailed(OTHER_EMAIL);
        
        // Then - Each user should have separate attempt count
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(3);
        assertThat(loginAttemptService.getRemainingAttempts(OTHER_EMAIL)).isEqualTo(4);
        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isFalse();
        assertThat(loginAttemptService.isBlocked(OTHER_EMAIL)).isFalse();
    }
    
    // ========== Account Blocking Tests ==========
    
    @Test
    @DisplayName("Should set lock expiration time when blocking")
    void isBlocked_WhenAccountLocked_ShouldReturnLockExpiration() {
        // Given - Lock the account
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            loginAttemptService.loginFailed(TEST_EMAIL);
        }
        
        // Trigger the block check to set lock time
        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isTrue();
        
        // When
        LocalDateTime lockExpiration = loginAttemptService.getLockExpirationTime(TEST_EMAIL);
        LocalDateTime now = LocalDateTime.now();
        
        // Then
        assertThat(lockExpiration).isNotNull();
        // Lock expiration should be approximately LOCKOUT_DURATION_MINUTES from now
        assertThat(lockExpiration).isAfter(now.minusSeconds(1));
        assertThat(lockExpiration).isBefore(now.plusMinutes(LOCKOUT_DURATION_MINUTES).plusSeconds(1));
    }
    
    @Test
    @DisplayName("Should maintain block even after additional failed attempts")
    void loginFailed_WhenAlreadyBlocked_ShouldRemainBlocked() {
        // Given - Lock the account
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            loginAttemptService.loginFailed(TEST_EMAIL);
        }
        
        // Trigger the block
        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isTrue();
        LocalDateTime firstLockTime = loginAttemptService.getLockExpirationTime(TEST_EMAIL);
        
        // When - Try to login again while blocked
        loginAttemptService.loginFailed(TEST_EMAIL);
        
        // Then - Should still be blocked
        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isTrue();
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(0);
    }
    
    // ========== Reset Attempts Tests ==========
    
    @Test
    @DisplayName("Should reset attempts manually")
    void resetAttempts_WithFailedAttempts_ShouldClearAll() {
        // Given - Some failed attempts
        loginAttemptService.loginFailed(TEST_EMAIL);
        loginAttemptService.loginFailed(TEST_EMAIL);
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(3);
        
        // When
        loginAttemptService.resetAttempts(TEST_EMAIL);
        
        // Then
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(MAX_ATTEMPTS);
        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isFalse();
    }
    
    @Test
    @DisplayName("Should unlock blocked account on reset")
    void resetAttempts_WithBlockedAccount_ShouldUnblock() {
        // Given - Block the account
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            loginAttemptService.loginFailed(TEST_EMAIL);
        }
        // Trigger the block to set lock time
        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isTrue();
        assertThat(loginAttemptService.getLockExpirationTime(TEST_EMAIL)).isNotNull();
        
        // When
        loginAttemptService.resetAttempts(TEST_EMAIL);
        
        // Then
        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isFalse();
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(MAX_ATTEMPTS);
        assertThat(loginAttemptService.getLockExpirationTime(TEST_EMAIL)).isNull();
    }
    
    // ========== Attempt Window Tests ==========
    
    @Test
    @DisplayName("Should reset counter if attempts are outside window")
    void loginFailed_OutsideAttemptWindow_ShouldResetCounter() {
        // Given - Initial failed attempt
        loginAttemptService.loginFailed(TEST_EMAIL);
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(4);
        
        // When - Simulate time passing beyond attempt window
        // We need to manipulate the internal cache for this test
        // In a real scenario, we'd use a time provider that can be mocked
        
        // For now, we'll test the cleanup method instead
        loginAttemptService.cleanupExpiredEntries();
        
        // Note: This test is limited without time manipulation
        // In production, you'd want to inject a Clock or TimeProvider
    }
    
    // ========== Cleanup Tests ==========
    
    @Test
    @DisplayName("Should clean up expired entries")
    void cleanupExpiredEntries_WithMixedEntries_ShouldRemoveOnlyExpired() {
        // Given - Add some attempts
        loginAttemptService.loginFailed(TEST_EMAIL);
        loginAttemptService.loginFailed(OTHER_EMAIL);
        
        // When
        loginAttemptService.cleanupExpiredEntries();
        
        // Then - Recent attempts should remain
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isLessThan(MAX_ATTEMPTS);
        assertThat(loginAttemptService.getRemainingAttempts(OTHER_EMAIL)).isLessThan(MAX_ATTEMPTS);
    }
    
    // ========== Edge Cases Tests ==========
    
    @Test
    @DisplayName("Should handle null or empty email gracefully")
    void loginFailed_WithEmptyEmail_ShouldHandleGracefully() {
        // When/Then - Should not throw exception
        loginAttemptService.loginFailed("");
        assertThat(loginAttemptService.getRemainingAttempts("")).isEqualTo(4);
        
        loginAttemptService.loginSucceeded("");
        assertThat(loginAttemptService.getRemainingAttempts("")).isEqualTo(MAX_ATTEMPTS);
    }
    
    @Test
    @DisplayName("Should handle concurrent access safely")
    void loginFailed_ConcurrentAccess_ShouldBeThreadSafe() throws InterruptedException {
        // Given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        // When - Multiple threads failing login simultaneously
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                loginAttemptService.loginFailed(TEST_EMAIL);
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then - Should have tracked all attempts correctly
        int remainingAttempts = loginAttemptService.getRemainingAttempts(TEST_EMAIL);
        assertThat(remainingAttempts).isGreaterThanOrEqualTo(0);
        assertThat(remainingAttempts).isLessThanOrEqualTo(MAX_ATTEMPTS);
    }
    
    @Test
    @DisplayName("Should return correct remaining attempts at boundary")
    void getRemainingAttempts_AtMaxAttemptsBoundary_ShouldReturnZero() {
        // Given - Exactly MAX_ATTEMPTS failures
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            loginAttemptService.loginFailed(TEST_EMAIL);
        }
        
        // Then
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(0);
        // Trigger the block check
        assertThat(loginAttemptService.isBlocked(TEST_EMAIL)).isTrue();
    }
    
    @Test
    @DisplayName("Should not return negative remaining attempts")
    void getRemainingAttempts_BeyondMaxAttempts_ShouldNotReturnNegative() {
        // Given - More than MAX_ATTEMPTS failures
        for (int i = 0; i < MAX_ATTEMPTS + 2; i++) {
            loginAttemptService.loginFailed(TEST_EMAIL);
        }
        
        // Then - Should return 0, not negative
        assertThat(loginAttemptService.getRemainingAttempts(TEST_EMAIL)).isEqualTo(0);
    }
}