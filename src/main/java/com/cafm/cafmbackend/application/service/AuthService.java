package com.cafm.cafmbackend.application.service;

import com.cafm.cafmbackend.infrastructure.persistence.entity.EmailVerificationToken;
import com.cafm.cafmbackend.infrastructure.persistence.entity.PasswordResetToken;
import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import com.cafm.cafmbackend.shared.enums.UserStatus;
import com.cafm.cafmbackend.infrastructure.persistence.repository.EmailVerificationTokenRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.PasswordResetTokenRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.UserRepository;
import com.cafm.cafmbackend.dto.auth.*;
import com.cafm.cafmbackend.shared.exception.ResourceNotFoundException;
import com.cafm.cafmbackend.security.JwtTokenProvider;
import com.cafm.cafmbackend.security.service.LoginAttemptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Service for authentication operations.
 * Handles login, registration, password reset, and token management.
 */
@Service
@Transactional
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginAttemptService loginAttemptService;
    private final EmailService emailService;
    
    @Value("${app.password-reset.token-validity-hours:1}")
    private int passwordResetTokenValidityHours;
    
    @Value("${app.password-reset.max-attempts-per-day:5}")
    private int maxPasswordResetAttemptsPerDay;
    
    @Value("${app.email-verification.token-validity-hours:24}")
    private int emailVerificationTokenValidityHours;
    
    @Value("${app.email-verification.max-attempts-per-day:3}")
    private int maxEmailVerificationAttemptsPerDay;
    
    public AuthService(UserRepository userRepository,
                      PasswordResetTokenRepository passwordResetTokenRepository,
                      EmailVerificationTokenRepository emailVerificationTokenRepository,
                      PasswordEncoder passwordEncoder,
                      JwtTokenProvider jwtTokenProvider,
                      LoginAttemptService loginAttemptService,
                      EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.loginAttemptService = loginAttemptService;
        this.emailService = emailService;
    }
    
    /**
     * Authenticate user and generate tokens.
     */
    public LoginResponse login(String email, String password, String ipAddress, String userAgent) {
        logger.info("Login attempt for email: {}", email);
        
        // Check if account is blocked due to too many failed attempts
        if (loginAttemptService.isBlocked(email)) {
            throw new LockedException("Account is temporarily locked due to too many failed login attempts. Please try again later.");
        }
        
        try {
            // Get user
            User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    loginAttemptService.loginFailed(email);
                    int remainingAttempts = loginAttemptService.getRemainingAttempts(email);
                    if (remainingAttempts > 0) {
                        throw new BadCredentialsException(
                            String.format("Invalid credentials. %d attempts remaining.", remainingAttempts)
                        );
                    } else {
                        throw new LockedException("Account has been locked due to too many failed attempts.");
                    }
                });
            
            // Verify password
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                loginAttemptService.loginFailed(email);
                int remainingAttempts = loginAttemptService.getRemainingAttempts(email);
                if (remainingAttempts > 0) {
                    throw new BadCredentialsException(
                        String.format("Invalid credentials. %d attempts remaining.", remainingAttempts)
                    );
                } else {
                    throw new LockedException("Account has been locked due to too many failed attempts.");
                }
            }
            
            // Check if user is active
            if (!user.isActive()) {
                throw new BadCredentialsException("Account is inactive");
            }
            
            if (user.getIsLocked() != null && user.getIsLocked()) {
                throw new BadCredentialsException("Account is locked by administrator");
            }
            
            // Generate tokens with user details
            String accessToken = jwtTokenProvider.generateAccessTokenWithClaims(user);
            String refreshToken = jwtTokenProvider.generateRefreshTokenWithUserId(user);
            
            // Reset login attempts on successful login
            loginAttemptService.loginSucceeded(email);
            
            // Update last login
            userRepository.updateLastLogin(user.getId(), LocalDateTime.now());
            
            // Build response
            return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L) // 1 hour
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userType(user.getUserType())
                .roles(getUserRoles(user))
                .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                .companyName(user.getCompany() != null ? user.getCompany().getName() : null)
                .sessionId(UUID.randomUUID().toString())
                .loginTime(LocalDateTime.now())
                .lastLoginTime(user.getLastLoginAt())
                .isFirstLogin(user.getLastLoginAt() == null)
                .mustChangePassword(false)
                .twoFactorEnabled(false)
                .build();
                
        } catch (BadCredentialsException | LockedException e) {
            logger.warn("Failed login attempt for email: {} - {}", email, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Register a new user (admin only).
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        logger.info("Registering new user: {}", request.email());
        
        // Check if email exists
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        // Create user
        User user = new User();
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setUserType(request.userType());
        user.setPhone(request.phone());
        user.setEmployeeId(request.employeeId());
        
        // Generate temporary password
        String temporaryPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        
        // Set defaults
        user.setStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        user.setIsLocked(false);
        user.setEmailVerified(false);
        
        // Save user
        user = userRepository.save(user);
        
        // Send email verification
        sendEmailVerification(user);
        
        // Send welcome email with temporary password
        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName(), temporaryPassword);
        
        logger.info("User registered successfully: {}", user.getId());
        
        return new RegisterResponse(
            user.getId(),
            user.getEmail(),
            temporaryPassword,
            "User registered successfully. Verification email sent."
        );
    }
    
    /**
     * Refresh access token.
     */
    public TokenRefreshResponse refreshToken(String refreshToken) {
        logger.debug("Refreshing token");
        
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        String newAccessToken = jwtTokenProvider.generateAccessTokenWithClaims(user);
        
        return new TokenRefreshResponse(
            newAccessToken,
            refreshToken,
            3600L
        );
    }
    
    /**
     * Logout user.
     */
    public void logout(String username, String token) {
        logger.info("Logout for user: {}", username);
        // In a stateless JWT system, logout is typically handled client-side
        // Here we could blacklist the token if needed
    }
    
    /**
     * Request password reset.
     */
    @Transactional
    public PasswordResetResponse requestPasswordReset(String email) {
        logger.info("Password reset requested for: {}", email);
        
        userRepository.findByEmail(email).ifPresent(user -> {
            try {
                // Check rate limit
                LocalDateTime since = LocalDateTime.now().minusDays(1);
                long recentAttempts = passwordResetTokenRepository.countByUserAndCreatedAtAfterAndUsedFalse(user, since);
                
                if (recentAttempts >= maxPasswordResetAttemptsPerDay) {
                    logger.warn("Password reset rate limit exceeded for user: {}", email);
                    return;
                }
                
                // Invalidate any existing tokens
                passwordResetTokenRepository.invalidateAllTokensForUser(user);
                
                // Generate new token
                String resetToken = UUID.randomUUID().toString();
                String tokenHash = passwordEncoder.encode(resetToken);
                LocalDateTime expiresAt = LocalDateTime.now().plusHours(passwordResetTokenValidityHours);
                
                // Save token
                PasswordResetToken passwordResetToken = new PasswordResetToken(
                    user, tokenHash, expiresAt, null
                );
                passwordResetTokenRepository.save(passwordResetToken);
                
                // Send email
                emailService.sendPasswordResetEmail(
                    user.getEmail(), 
                    user.getFullName(), 
                    resetToken
                );
                
                logger.info("Password reset token created for user: {}", user.getId());
                
            } catch (Exception e) {
                logger.error("Error processing password reset for user: {}", email, e);
            }
        });
        
        // Always return success to prevent email enumeration
        return PasswordResetResponse.successResponse();
    }
    
    /**
     * Reset password with token.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        logger.info("Resetting password with token");
        
        // Hash the token to find it in database
        String tokenHash = passwordEncoder.encode(token);
        
        // Find valid token
        PasswordResetToken resetToken = passwordResetTokenRepository
            .findByTokenHashAndUsedFalseAndExpiresAtAfter(tokenHash, LocalDateTime.now())
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));
        
        // Get user
        User user = resetToken.getUser();
        
        // Validate new password
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Mark token as used
        resetToken.markAsUsed();
        passwordResetTokenRepository.save(resetToken);
        
        // Invalidate all other tokens for this user
        passwordResetTokenRepository.invalidateAllTokensForUser(user);
        
        logger.info("Password reset successful for user: {}", user.getId());
    }
    
    /**
     * Change password.
     */
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        logger.info("Changing password for user: {}", username);
        
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        logger.info("Password changed successfully for user: {}", username);
    }
    
    /**
     * Verify email with token.
     */
    @Transactional
    public VerifyEmailResponse verifyEmail(String token) {
        logger.info("Verifying email with token");
        
        try {
            // Hash the token to find it in database
            String tokenHash = passwordEncoder.encode(token);
            
            // Find valid token
            EmailVerificationToken verificationToken = emailVerificationTokenRepository
                .findByTokenHashAndVerifiedFalseAndExpiresAtAfter(tokenHash, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification token"));
            
            // Get user
            User user = verificationToken.getUser();
            
            // Mark email as verified
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(LocalDateTime.now());
            userRepository.save(user);
            
            // Mark token as verified
            verificationToken.markAsVerified();
            emailVerificationTokenRepository.save(verificationToken);
            
            // Invalidate all other verification tokens for this user
            emailVerificationTokenRepository.invalidateAllTokensForUser(user, LocalDateTime.now());
            
            logger.info("Email verified successfully for user: {}", user.getId());
            
            return VerifyEmailResponse.success(
                user.getId(),
                user.getEmail()
            );
            
        } catch (IllegalArgumentException e) {
            logger.warn("Email verification failed: {}", e.getMessage());
            return VerifyEmailResponse.error(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during email verification", e);
            return VerifyEmailResponse.error("Email verification failed. Please try again.");
        }
    }
    
    /**
     * Request email verification resend.
     */
    @Transactional
    public VerifyEmailResponse resendVerificationEmail(String email) {
        logger.info("Resending verification email for: {}", email);
        
        userRepository.findByEmail(email).ifPresent(user -> {
            try {
                // Check if already verified
                if (Boolean.TRUE.equals(user.getEmailVerified())) {
                    logger.info("Email already verified for user: {}", email);
                    return;
                }
                
                // Check rate limit
                LocalDateTime since = LocalDateTime.now().minusDays(1);
                long recentAttempts = emailVerificationTokenRepository.countByUserAndCreatedAtAfterAndVerifiedFalse(user, since);
                
                if (recentAttempts >= maxEmailVerificationAttemptsPerDay) {
                    logger.warn("Email verification rate limit exceeded for user: {}", email);
                    return;
                }
                
                // Send new verification email
                sendEmailVerification(user);
                
                logger.info("Verification email resent for user: {}", user.getId());
                
            } catch (Exception e) {
                logger.error("Error resending verification email for user: {}", email, e);
            }
        });
        
        // Always return success to prevent email enumeration
        return new VerifyEmailResponse(
            null,
            null,
            true,
            "If the email exists and is unverified, a verification link has been sent."
        );
    }
    
    /**
     * Get current user information.
     */
    public CurrentUserResponse getCurrentUser(String username) {
        logger.debug("Getting current user info for: {}", username);
        
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return new CurrentUserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getFullName(),
            user.getUserType(),
            user.getCompany() != null ? user.getCompany().getId() : null,
            user.getCompany() != null ? user.getCompany().getName() : null,
            getUserRoles(user),
            user.getEmailVerified() != null && user.getEmailVerified(),
            user.getIsActive() != null && user.getIsActive(),
            user.getIsLocked() != null && user.getIsLocked()
        );
    }
    
    /**
     * Validate token.
     */
    public TokenValidationResponse validateToken(String token) {
        logger.debug("Validating token");
        
        if (!jwtTokenProvider.validateToken(token)) {
            return TokenValidationResponse.invalid("Invalid token");
        }
        
        String username = jwtTokenProvider.getUsernameFromToken(token);
        User user = userRepository.findByEmail(username)
            .orElse(null);
        
        if (user == null) {
            return TokenValidationResponse.invalid("User not found");
        }
        
        Long expiresIn = jwtTokenProvider.getExpirationTime(token);
        
        return TokenValidationResponse.valid(user.getId(), user.getEmail(), expiresIn);
    }
    
    /**
     * Enable two-factor authentication.
     */
    @Transactional
    public TwoFactorSetupResponse enableTwoFactor(String username) {
        logger.info("Enabling 2FA for user: {}", username);
        
        // This would typically involve generating TOTP secret and QR code
        // For now, this is a placeholder
        
        return new TwoFactorSetupResponse(
            "SECRET123",
            "https://example.com/qr",
            "MANUAL-ENTRY-KEY",
            new String[]{"BACKUP1", "BACKUP2", "BACKUP3"}
        );
    }
    
    /**
     * Disable two-factor authentication.
     */
    @Transactional
    public void disableTwoFactor(String username, String password) {
        logger.info("Disabling 2FA for user: {}", username);
        
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Verify password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid password");
        }
        
        // Disable 2FA (placeholder)
        logger.info("2FA disabled for user: {}", username);
    }
    
    /**
     * Verify two-factor code.
     */
    public LoginResponse verifyTwoFactor(String sessionId, String code) {
        logger.info("Verifying 2FA code for session: {}", sessionId);
        
        // This would typically involve verifying TOTP code
        // For now, this is a placeholder that throws an error
        
        throw new UnsupportedOperationException("2FA verification not implemented");
    }
    
    // ========== Helper Methods ==========
    
    private Set<String> getUserRoles(User user) {
        Set<String> roles = new HashSet<>();
        // Add user type as role (Spring Security will add ROLE_ prefix automatically)
        roles.add(user.getUserType().name());
        
        // Add any additional roles from user's role collection if they exist
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            user.getRoles().forEach(role -> {
                String roleName = role.getName();
                // Remove ROLE_ prefix if it exists to avoid duplication
                if (roleName.startsWith("ROLE_")) {
                    roleName = roleName.substring(5);
                }
                roles.add(roleName);
            });
        }
        
        return roles;
    }
    
    private String generateTemporaryPassword() {
        // Generate a random 8-character password
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return password.toString();
    }
    
    /**
     * Send email verification to user.
     */
    private void sendEmailVerification(User user) {
        try {
            // Invalidate any existing tokens
            emailVerificationTokenRepository.invalidateAllTokensForUser(user, LocalDateTime.now());
            
            // Generate new token
            String verificationToken = UUID.randomUUID().toString();
            String tokenHash = passwordEncoder.encode(verificationToken);
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(emailVerificationTokenValidityHours);
            
            // Save token
            EmailVerificationToken emailVerificationToken = new EmailVerificationToken(
                user, tokenHash, user.getEmail(), expiresAt, null
            );
            emailVerificationTokenRepository.save(emailVerificationToken);
            
            // Send email
            emailService.sendVerificationEmail(
                user.getEmail(),
                user.getFullName(),
                verificationToken
            );
            
            logger.info("Email verification token created for user: {}", user.getId());
            
        } catch (Exception e) {
            logger.error("Error sending verification email for user: {}", user.getId(), e);
            // Don't throw exception to avoid blocking registration
        }
    }
}