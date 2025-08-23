package com.cafm.cafmbackend.unit.security;

import com.cafm.cafmbackend.data.entity.Company;
import com.cafm.cafmbackend.data.entity.User;
import com.cafm.cafmbackend.data.enums.UserType;
import com.cafm.cafmbackend.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider.
 * Tests JWT token generation, validation, and claims extraction.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    
    private static final String JWT_SECRET = "ThisIsAVeryLongSecretKeyForTestingPurposesOnlyAndShouldBeAtLeast256BitsLong123456789";
    private static final Long JWT_EXPIRATION_MS = 3600000L; // 1 hour
    private static final Long REFRESH_EXPIRATION_MS = 86400000L; // 24 hours
    
    private User testUser;
    private Company testCompany;
    
    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        
        // Inject test values using reflection
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", JWT_EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpirationMs", REFRESH_EXPIRATION_MS);
        
        // Setup test user
        testCompany = new Company();
        testCompany.setId(UUID.randomUUID());
        testCompany.setName("Test Company");
        
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@cafm.com");
        testUser.setUserType(UserType.ADMIN);
        testUser.setCompany(testCompany);
        
        // Mock authorities
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        ReflectionTestUtils.setField(testUser, "roles", new HashSet<>());
    }
    
    // ========== Token Generation Tests ==========
    
    @Test
    @DisplayName("Should generate access token with username")
    void generateAccessToken_WithUsername_ShouldCreateValidToken() {
        // When
        String token = jwtTokenProvider.generateAccessToken("test@cafm.com");
        
        // Then
        assertThat(token).isNotNull();
        assertThat(token).contains(".");
        
        // Validate token structure (header.payload.signature)
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
        
        // Verify claims
        Claims claims = extractClaims(token);
        assertThat(claims.getSubject()).isEqualTo("test@cafm.com");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }
    
    @Test
    @DisplayName("Should generate access token with user claims")
    void generateAccessTokenWithClaims_WithUser_ShouldIncludeUserDetails() {
        // When
        String token = jwtTokenProvider.generateAccessTokenWithClaims(testUser);
        
        // Then
        assertThat(token).isNotNull();
        
        Claims claims = extractClaims(token);
        assertThat(claims.getSubject()).isEqualTo(testUser.getEmail());
        assertThat(claims.get("userId")).isEqualTo(testUser.getId().toString());
        assertThat(claims.get("email")).isEqualTo(testUser.getEmail());
        assertThat(claims.get("userType")).isEqualTo(UserType.ADMIN.name());
        assertThat(claims.get("companyId")).isEqualTo(testCompany.getId().toString());
        assertThat(claims.get("companyName")).isEqualTo(testCompany.getName());
        assertThat(claims.get("roles")).isNotNull();
    }
    
    @Test
    @DisplayName("Should generate refresh token with username")
    void generateRefreshToken_WithUsername_ShouldCreateValidToken() {
        // When
        String token = jwtTokenProvider.generateRefreshToken("test@cafm.com");
        
        // Then
        assertThat(token).isNotNull();
        
        Claims claims = extractClaims(token);
        assertThat(claims.getSubject()).isEqualTo("test@cafm.com");
        assertThat(claims.get("type")).isEqualTo("refresh");
        
        // Verify refresh token has longer expiration
        long expirationTime = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(expirationTime).isGreaterThan(JWT_EXPIRATION_MS);
    }
    
    @Test
    @DisplayName("Should generate refresh token with user ID")
    void generateRefreshTokenWithUserId_WithUser_ShouldIncludeUserId() {
        // When
        String token = jwtTokenProvider.generateRefreshTokenWithUserId(testUser);
        
        // Then
        assertThat(token).isNotNull();
        
        Claims claims = extractClaims(token);
        assertThat(claims.getSubject()).isEqualTo(testUser.getEmail());
        assertThat(claims.get("type")).isEqualTo("refresh");
        assertThat(claims.get("userId")).isEqualTo(testUser.getId().toString());
    }
    
    // ========== Token Validation Tests ==========
    
    @Test
    @DisplayName("Should validate valid token")
    void validateToken_WithValidToken_ShouldReturnTrue() {
        // Given
        String token = jwtTokenProvider.generateAccessToken("test@cafm.com");
        
        // When
        boolean isValid = jwtTokenProvider.validateToken(token);
        
        // Then
        assertThat(isValid).isTrue();
    }
    
    @Test
    @DisplayName("Should reject malformed token")
    void validateToken_WithMalformedToken_ShouldReturnFalse() {
        // Given
        String malformedToken = "not.a.valid.token";
        
        // When
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("Should reject token with invalid signature")
    void validateToken_WithInvalidSignature_ShouldReturnFalse() {
        // Given
        String token = jwtTokenProvider.generateAccessToken("test@cafm.com");
        String tamperedToken = token.substring(0, token.lastIndexOf('.')) + ".invalidsignature";
        
        // When
        boolean isValid = jwtTokenProvider.validateToken(tamperedToken);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("Should reject expired token")
    void validateToken_WithExpiredToken_ShouldReturnFalse() {
        // Given - Create an expired token
        Date past = new Date(System.currentTimeMillis() - 10000); // 10 seconds ago
        String expiredToken = Jwts.builder()
            .subject("test@cafm.com")
            .issuedAt(new Date(System.currentTimeMillis() - 20000))
            .expiration(past)
            .signWith(getSigningKey())
            .compact();
        
        // When
        boolean isValid = jwtTokenProvider.validateToken(expiredToken);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("Should reject empty token")
    void validateToken_WithEmptyToken_ShouldReturnFalse() {
        // When
        boolean isValid = jwtTokenProvider.validateToken("");
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("Should reject null token")
    void validateToken_WithNullToken_ShouldReturnFalse() {
        // When
        boolean isValid = jwtTokenProvider.validateToken(null);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    // ========== Claims Extraction Tests ==========
    
    @Test
    @DisplayName("Should extract username from token")
    void getUsernameFromToken_WithValidToken_ShouldReturnUsername() {
        // Given
        String token = jwtTokenProvider.generateAccessToken("test@cafm.com");
        
        // When
        String username = jwtTokenProvider.getUsernameFromToken(token);
        
        // Then
        assertThat(username).isEqualTo("test@cafm.com");
    }
    
    @Test
    @DisplayName("Should extract user ID from token")
    void getUserIdFromToken_WithValidToken_ShouldReturnUserId() {
        // Given
        String token = jwtTokenProvider.generateAccessTokenWithClaims(testUser);
        
        // When
        UUID userId = jwtTokenProvider.getUserIdFromToken(token);
        
        // Then
        assertThat(userId).isEqualTo(testUser.getId());
    }
    
    @Test
    @DisplayName("Should return null for missing user ID")
    void getUserIdFromToken_WithoutUserIdClaim_ShouldReturnNull() {
        // Given
        String token = jwtTokenProvider.generateAccessToken("test@cafm.com");
        
        // When
        UUID userId = jwtTokenProvider.getUserIdFromToken(token);
        
        // Then
        assertThat(userId).isNull();
    }
    
    @Test
    @DisplayName("Should extract company ID from token")
    void getCompanyIdFromToken_WithValidToken_ShouldReturnCompanyId() {
        // Given
        String token = jwtTokenProvider.generateAccessTokenWithClaims(testUser);
        
        // When
        UUID companyId = jwtTokenProvider.getCompanyIdFromToken(token);
        
        // Then
        assertThat(companyId).isEqualTo(testCompany.getId());
    }
    
    @Test
    @DisplayName("Should return null for user without company")
    void getCompanyIdFromToken_WithoutCompany_ShouldReturnNull() {
        // Given
        testUser.setCompany(null);
        String token = jwtTokenProvider.generateAccessTokenWithClaims(testUser);
        
        // When
        UUID companyId = jwtTokenProvider.getCompanyIdFromToken(token);
        
        // Then
        assertThat(companyId).isNull();
    }
    
    @Test
    @DisplayName("Should get expiration time from token")
    void getExpirationTime_WithValidToken_ShouldReturnRemainingTime() {
        // Given
        String token = jwtTokenProvider.generateAccessToken("test@cafm.com");
        
        // When
        Long expirationTime = jwtTokenProvider.getExpirationTime(token);
        
        // Then
        assertThat(expirationTime).isNotNull();
        assertThat(expirationTime).isPositive();
        assertThat(expirationTime).isLessThanOrEqualTo(3600); // 1 hour in seconds
    }
    
    @Test
    @DisplayName("Should return 0 for expired token expiration time")
    void getExpirationTime_WithExpiredToken_ShouldReturnZero() {
        // Given - Create an expired token
        Date past = new Date(System.currentTimeMillis() - 10000);
        String expiredToken = Jwts.builder()
            .subject("test@cafm.com")
            .issuedAt(new Date(System.currentTimeMillis() - 20000))
            .expiration(past)
            .signWith(getSigningKey())
            .compact();
        
        // When
        Long expirationTime = jwtTokenProvider.getExpirationTime(expiredToken);
        
        // Then
        assertThat(expirationTime).isEqualTo(0L);
    }
    
    @Test
    @DisplayName("Should extract all claims from token")
    void getClaimsFromToken_WithValidToken_ShouldReturnAllClaims() {
        // Given
        String token = jwtTokenProvider.generateAccessTokenWithClaims(testUser);
        
        // When
        Claims claims = jwtTokenProvider.getClaimsFromToken(token);
        
        // Then
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(testUser.getEmail());
        assertThat(claims.get("userId")).isEqualTo(testUser.getId().toString());
        assertThat(claims.get("userType")).isEqualTo(UserType.ADMIN.name());
    }
    
    @Test
    @DisplayName("Should return null claims for invalid token")
    void getClaimsFromToken_WithInvalidToken_ShouldReturnNull() {
        // Given
        String invalidToken = "invalid.token.here";
        
        // When
        Claims claims = jwtTokenProvider.getClaimsFromToken(invalidToken);
        
        // Then
        assertThat(claims).isNull();
    }
    
    // ========== Edge Cases Tests ==========
    
    @Test
    @DisplayName("Should handle token with special characters in claims")
    void generateToken_WithSpecialCharacters_ShouldHandleCorrectly() {
        // Given
        testUser.setEmail("user+test@cafm-system.com");
        testCompany.setName("Test & Company Co.");
        
        // When
        String token = jwtTokenProvider.generateAccessTokenWithClaims(testUser);
        
        // Then
        assertThat(token).isNotNull();
        Claims claims = extractClaims(token);
        assertThat(claims.get("email")).isEqualTo("user+test@cafm-system.com");
        assertThat(claims.get("companyName")).isEqualTo("Test & Company Co.");
    }
    
    @Test
    @DisplayName("Should handle very long token expiration")
    void generateToken_WithLongExpiration_ShouldCreateValidToken() {
        // Given
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpirationMs", 31536000000L); // 1 year
        
        // When
        String token = jwtTokenProvider.generateRefreshToken("test@cafm.com");
        
        // Then
        assertThat(token).isNotNull();
        Claims claims = extractClaims(token);
        long expirationTime = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(expirationTime).isEqualTo(31536000000L);
    }
    
    // ========== Helper Methods ==========
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = JWT_SECRET.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    private Claims extractClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}