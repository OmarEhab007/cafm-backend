package com.cafm.cafmbackend.service;

import com.cafm.cafmbackend.data.entity.RefreshToken;
import com.cafm.cafmbackend.data.entity.User;
import com.cafm.cafmbackend.data.repository.RefreshTokenRepository;
import com.cafm.cafmbackend.data.repository.UserRepository;
import com.cafm.cafmbackend.dto.auth.LoginRequest;
import com.cafm.cafmbackend.dto.auth.LoginResponse;
import com.cafm.cafmbackend.exception.InvalidCredentialsException;
import com.cafm.cafmbackend.security.JwtTokenProvider;
import com.cafm.cafmbackend.security.service.EnhancedLoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling authentication operations.
 * 
 * Security Features:
 * - Integration with rate limiting
 * - Account lockout after failed attempts
 * - Secure token generation
 * - Audit logging for security events
 */
@Service
@Transactional
public class AuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EnhancedLoginAttemptService loginAttemptService;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${jwt.refresh-expiration:86400000}")
    private Long refreshTokenDurationMs;
    
    @Autowired
    public AuthenticationService(AuthenticationManager authenticationManager,
                                JwtTokenProvider jwtTokenProvider,
                                UserRepository userRepository,
                                RefreshTokenRepository refreshTokenRepository,
                                EnhancedLoginAttemptService loginAttemptService,
                                PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.loginAttemptService = loginAttemptService;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Authenticate user and generate tokens.
     */
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String username = request.email();
        String ipAddress = loginAttemptService.getClientIP(httpRequest);
        
        logger.info("Login attempt for user: {} from IP: {}", username, ipAddress);
        
        // Check if account is blocked due to too many attempts
        EnhancedLoginAttemptService.LoginAttemptResult attemptResult = 
            loginAttemptService.checkLoginAttempt(username, ipAddress);
        
        if (attemptResult.blocked()) {
            logger.warn("Login blocked for user: {} - Reason: {}", username, attemptResult.reason());
            throw new LockedException("Account is temporarily locked: " + attemptResult.reason());
        }
        
        try {
            // Attempt authentication
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.password())
            );
            
            // Get authenticated user
            User user = (User) authentication.getPrincipal();
            
            // Check if user account is active
            if (!user.isEnabled()) {
                loginAttemptService.loginFailed(username, ipAddress);
                throw new DisabledException("Account is disabled");
            }
            
            if (!user.isAccountNonLocked()) {
                loginAttemptService.loginFailed(username, ipAddress);
                throw new LockedException("Account is locked");
            }
            
            // Authentication successful - reset attempts
            loginAttemptService.loginSucceeded(username, ipAddress);
            
            // Generate tokens
            String accessToken = jwtTokenProvider.generateAccessTokenWithClaims(user);
            String refreshToken = createRefreshToken(user);
            
            // Update last login
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(ipAddress);
            userRepository.save(user);
            
            // Log successful login
            logger.info("Successful login for user: {} from IP: {}", username, ipAddress);
            
            return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationTime(accessToken))
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userType(user.getUserType())
                .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                .companyName(user.getCompany() != null ? user.getCompany().getName() : null)
                .loginTime(LocalDateTime.now())
                .lastLoginTime(user.getLastLoginAt())
                .build();
            
        } catch (BadCredentialsException e) {
            // Record failed attempt
            loginAttemptService.loginFailed(username, ipAddress);
            
            // Get remaining attempts
            int remainingAttempts = loginAttemptService.getRemainingAttempts(username);
            
            logger.warn("Failed login for user: {} from IP: {} - Remaining attempts: {}", 
                       username, ipAddress, remainingAttempts);
            
            String message = remainingAttempts > 0 ? 
                String.format("Invalid credentials. %d attempts remaining", remainingAttempts) :
                "Account locked due to too many failed attempts";
            
            throw new InvalidCredentialsException(message);
            
        } catch (DisabledException | LockedException e) {
            // Record failed attempt for disabled/locked accounts
            loginAttemptService.loginFailed(username, ipAddress);
            throw e;
            
        } catch (AuthenticationException e) {
            // Record failed attempt for other authentication failures
            loginAttemptService.loginFailed(username, ipAddress);
            logger.error("Authentication failed for user: {} from IP: {}", username, ipAddress, e);
            throw new InvalidCredentialsException("Authentication failed");
        }
    }
    
    /**
     * Create and store refresh token.
     */
    private String createRefreshToken(User user) {
        // Generate token
        String token = jwtTokenProvider.generateRefreshTokenWithUserId(user);
        
        // Calculate expiry
        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(refreshTokenDurationMs / 1000);
        
        // Store in database
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(token);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(expiryDate);
        
        refreshTokenRepository.save(refreshToken);
        
        return token;
    }
    
    /**
     * Refresh access token using refresh token.
     */
    @Transactional
    public LoginResponse refreshToken(String refreshToken, HttpServletRequest httpRequest) {
        logger.info("Token refresh attempt");
        
        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }
        
        // Find stored refresh token
        Optional<RefreshToken> storedToken = refreshTokenRepository.findByTokenHash(refreshToken);
        if (storedToken.isEmpty()) {
            throw new InvalidCredentialsException("Refresh token not found");
        }
        
        RefreshToken token = storedToken.get();
        
        // Check if expired
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new InvalidCredentialsException("Refresh token expired");
        }
        
        // Check if revoked
        if (token.isRevoked()) {
            throw new InvalidCredentialsException("Refresh token has been revoked");
        }
        
        User user = token.getUser();
        
        // Check if user is still active
        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            throw new DisabledException("Account is not active");
        }
        
        // Generate new access token
        String newAccessToken = jwtTokenProvider.generateAccessTokenWithClaims(user);
        
        // Update refresh token usage
        token.setLastUsedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
        
        String ipAddress = loginAttemptService.getClientIP(httpRequest);
        logger.info("Token refreshed for user: {} from IP: {}", user.getEmail(), ipAddress);
        
        return LoginResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken) // Return same refresh token
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getExpirationTime(newAccessToken))
            .userId(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .userType(user.getUserType())
            .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
            .companyName(user.getCompany() != null ? user.getCompany().getName() : null)
            .loginTime(LocalDateTime.now())
            .lastLoginTime(user.getLastLoginAt())
            .build();
    }
    
    /**
     * Logout user by revoking tokens.
     */
    @Transactional
    public void logout(String refreshToken, HttpServletRequest httpRequest) {
        if (refreshToken == null) {
            return;
        }
        
        Optional<RefreshToken> storedToken = refreshTokenRepository.findByTokenHash(refreshToken);
        if (storedToken.isPresent()) {
            RefreshToken token = storedToken.get();
            token.revoke("User logout");
            refreshTokenRepository.save(token);
            
            String ipAddress = loginAttemptService.getClientIP(httpRequest);
            logger.info("User logged out: {} from IP: {}", token.getUser().getEmail(), ipAddress);
        }
    }
    
    /**
     * Revoke all refresh tokens for a user.
     */
    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        refreshTokenRepository.revokeAllTokensForUser(user, LocalDateTime.now(), "All tokens revoked by admin");
        logger.info("All tokens revoked for user: {}", user.getEmail());
    }
    
    /**
     * Clean up expired refresh tokens.
     */
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        if (deleted > 0) {
            logger.info("Cleaned up {} expired refresh tokens", deleted);
        }
    }
}