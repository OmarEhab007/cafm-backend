package com.cafm.cafmbackend.security;

import com.cafm.cafmbackend.data.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT token provider for generating and validating JWT tokens.
 * 
 * Security Features:
 * - Enforces minimum 64-character secret key
 * - No default secrets allowed in production
 * - Automatic secure secret generation if needed
 * - Uses HMAC-SHA256 for token signing
 */
@Component
public class JwtTokenProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final int MINIMUM_SECRET_LENGTH = 64;
    private static final String DEFAULT_SECRET_PREFIX = "default";
    private static final String WEAK_SECRET_PREFIX = "changeme";
    
    @Value("${jwt.secret:#{null}}")
    private String jwtSecret;
    
    @Value("${spring.profiles.active:default}")
    private String activeProfile;
    
    @Value("${jwt.expiration:3600000}") // 1 hour default
    private Long jwtExpirationMs;
    
    @Value("${jwt.refresh-expiration:86400000}") // 24 hours default
    private Long refreshExpirationMs;
    
    private SecretKey signingKey;
    
    /**
     * Initialize and validate JWT secret on startup
     */
    @PostConstruct
    public void init() {
        validateAndInitializeSecret();
    }
    
    /**
     * Validates JWT secret and initializes signing key
     * @throws IllegalStateException if secret is invalid in production
     */
    private void validateAndInitializeSecret() {
        // Check if secret is provided
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            if (isProductionProfile()) {
                throw new IllegalStateException(
                    "JWT secret is required in production. Please set jwt.secret environment variable with a secure 64+ character secret."
                );
            } else {
                // Generate secure secret for development
                jwtSecret = generateSecureSecret();
                logger.warn("Generated temporary JWT secret for development. This is NOT suitable for production!");
            }
        }
        
        // Validate secret strength
        if (jwtSecret.length() < MINIMUM_SECRET_LENGTH) {
            String errorMsg = String.format(
                "JWT secret must be at least %d characters long. Current length: %d",
                MINIMUM_SECRET_LENGTH, jwtSecret.length()
            );
            if (isProductionProfile()) {
                throw new IllegalStateException(errorMsg);
            } else {
                logger.warn(errorMsg + " - Extending secret for development environment.");
                jwtSecret = extendSecret(jwtSecret);
            }
        }
        
        // Check for weak/default secrets
        String lowerSecret = jwtSecret.toLowerCase();
        if (lowerSecret.contains(DEFAULT_SECRET_PREFIX) || 
            lowerSecret.contains(WEAK_SECRET_PREFIX) ||
            lowerSecret.contains("password") ||
            lowerSecret.contains("secret") ||
            lowerSecret.contains("123456") ||
            isWeakSecret(jwtSecret)) {
            
            if (isProductionProfile()) {
                throw new IllegalStateException(
                    "Weak or default JWT secret detected. Please use a cryptographically secure secret in production."
                );
            } else {
                logger.warn("Weak JWT secret detected in development environment. This is NOT suitable for production!");
            }
        }
        
        // Initialize signing key
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        
        logger.info("JWT token provider initialized successfully with {} character secret", jwtSecret.length());
    }
    
    /**
     * Check if running in production profile
     */
    private boolean isProductionProfile() {
        return activeProfile != null && 
               (activeProfile.contains("prod") || 
                activeProfile.contains("production") ||
                activeProfile.contains("prd"));
    }
    
    /**
     * Check if secret is weak based on patterns
     */
    private boolean isWeakSecret(String secret) {
        // Check for repetitive patterns
        if (secret.matches("^(.)\\1+$")) {
            return true; // All same character
        }
        
        // Check for sequential patterns
        boolean isSequential = true;
        for (int i = 1; i < Math.min(secret.length(), 10); i++) {
            if (secret.charAt(i) != secret.charAt(i-1) + 1) {
                isSequential = false;
                break;
            }
        }
        
        return isSequential;
    }
    
    /**
     * Generate a cryptographically secure secret
     */
    private String generateSecureSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    /**
     * Extend a short secret to meet minimum length requirement (dev only)
     */
    private String extendSecret(String shortSecret) {
        StringBuilder extended = new StringBuilder(shortSecret);
        SecureRandom random = new SecureRandom();
        
        while (extended.length() < MINIMUM_SECRET_LENGTH) {
            byte[] bytes = new byte[16];
            random.nextBytes(bytes);
            extended.append(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        }
        
        return extended.substring(0, MINIMUM_SECRET_LENGTH);
    }
    
    /**
     * Get the signing key for JWT operations
     */
    private SecretKey getSigningKey() {
        if (signingKey == null) {
            throw new IllegalStateException("JWT signing key not initialized");
        }
        return signingKey;
    }
    
    /**
     * Generate access token from username.
     */
    public String generateAccessToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }
    
    /**
     * Generate access token with user details.
     */
    public String generateAccessTokenWithClaims(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("userType", user.getUserType().name());
        if (user.getCompany() != null) {
            claims.put("companyId", user.getCompany().getId().toString());
            claims.put("companyName", user.getCompany().getName());
        }
        claims.put("roles", user.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .toList());
        
        return Jwts.builder()
            .claims(claims)
            .subject(user.getEmail())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }
    
    /**
     * Generate refresh token from username.
     */
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationMs);
        
        return Jwts.builder()
            .subject(username)
            .issuedAt(now)
            .expiration(expiryDate)
            .claim("type", "refresh")
            .signWith(getSigningKey())
            .compact();
    }
    
    /**
     * Generate refresh token with user ID.
     */
    public String generateRefreshTokenWithUserId(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationMs);
        
        return Jwts.builder()
            .subject(user.getEmail())
            .issuedAt(now)
            .expiration(expiryDate)
            .claim("type", "refresh")
            .claim("userId", user.getId().toString())
            .signWith(getSigningKey())
            .compact();
    }
    
    /**
     * Get username from token.
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        return claims.getSubject();
    }
    
    /**
     * Validate token.
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(authToken);
            return true;
        } catch (SecurityException ex) {
            logger.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty");
        }
        return false;
    }
    
    /**
     * Get expiration time in seconds.
     */
    public Long getExpirationTime(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            Date expiration = claims.getExpiration();
            long now = System.currentTimeMillis();
            return (expiration.getTime() - now) / 1000; // Convert to seconds
        } catch (Exception e) {
            return 0L;
        }
    }
    
    /**
     * Get claims from token.
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (Exception e) {
            logger.error("Could not extract claims from token", e);
            return null;
        }
    }
    
    /**
     * Get user ID from token.
     */
    public UUID getUserIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims != null && claims.containsKey("userId")) {
                return UUID.fromString(claims.get("userId", String.class));
            }
            return null;
        } catch (Exception e) {
            logger.error("Could not extract user ID from token", e);
            return null;
        }
    }
    
    /**
     * Get company ID from token.
     */
    public UUID getCompanyIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims != null && claims.containsKey("companyId")) {
                return UUID.fromString(claims.get("companyId", String.class));
            }
            return null;
        } catch (Exception e) {
            logger.error("Could not extract company ID from token", e);
            return null;
        }
    }
}