package com.cafm.cafmbackend.infrastructure.persistence.repository;

import com.cafm.cafmbackend.infrastructure.persistence.entity.RefreshToken;
import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RefreshTokenRepository for managing JWT refresh tokens.
 * 
 * Purpose: Provides data access layer for refresh token operations
 * Pattern: Spring Data JPA repository with custom queries for token management
 * Java 23: Uses modern JPA features with optional query methods
 * Architecture: Data layer repository following CAFM backend patterns
 * Standards: Implements tenant-aware queries and soft delete support
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    
    /**
     * Find refresh token by token hash
     * Used for token validation during refresh operations
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    
    /**
     * Find refresh token by token hash and ensure it's not revoked
     * Primary method for validating refresh tokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash AND rt.revoked = false")
    Optional<RefreshToken> findByTokenHashAndNotRevoked(@Param("tokenHash") String tokenHash);
    
    /**
     * Find all refresh tokens for a specific user
     * Used for token management and cleanup
     */
    List<RefreshToken> findByUser(User user);
    
    /**
     * Find all non-revoked refresh tokens for a user
     * Used to check active sessions
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false")
    List<RefreshToken> findByUserAndNotRevoked(@Param("user") User user);
    
    /**
     * Find all refresh tokens for a user by user ID
     * Optimized query that doesn't require loading full User entity
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId")
    List<RefreshToken> findByUserId(@Param("userId") UUID userId);
    
    /**
     * Find expired tokens that need cleanup
     * Used by scheduled tasks to clean up old tokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.expiresAt < :now")
    List<RefreshToken> findExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * Find tokens that expired before a specific date
     * Used for bulk cleanup operations
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.expiresAt < :cutoffDate")
    List<RefreshToken> findTokensExpiredBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Revoke all tokens for a specific user
     * Used when user logs out from all devices or account is compromised
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :revokedAt, rt.revokedReason = :reason WHERE rt.user = :user AND rt.revoked = false")
    int revokeAllTokensForUser(@Param("user") User user, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") String reason);
    
    /**
     * Revoke all tokens for a user by user ID
     * More efficient when you only have the user ID
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :revokedAt, rt.revokedReason = :reason WHERE rt.user.id = :userId AND rt.revoked = false")
    int revokeAllTokensForUserId(@Param("userId") UUID userId, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") String reason);
    
    /**
     * Revoke a specific token by token hash
     * Used for single device logout
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :revokedAt, rt.revokedReason = :reason WHERE rt.tokenHash = :tokenHash")
    int revokeTokenByHash(@Param("tokenHash") String tokenHash, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") String reason);
    
    /**
     * Delete expired tokens (hard delete)
     * Used for cleanup to reduce database size
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoffDate")
    int deleteExpiredTokens(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Delete revoked tokens older than specified date
     * Used for cleanup of old revoked tokens
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true AND rt.revokedAt < :cutoffDate")
    int deleteOldRevokedTokens(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Count active tokens for a user
     * Used to limit number of concurrent sessions
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.expiresAt > :now")
    long countActiveTokensForUser(@Param("user") User user, @Param("now") LocalDateTime now);
    
    /**
     * Find tokens by IP address for security monitoring
     * Used to track login locations
     */
    List<RefreshToken> findByIpAddress(String ipAddress);
    
    /**
     * Find tokens by user agent for device tracking
     * Used to identify different devices/browsers
     */
    List<RefreshToken> findByUserAgent(String userAgent);
    
    /**
     * Check if token exists and is valid (not expired and not revoked)
     * Efficient boolean check without loading full entity
     */
    @Query("SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash AND rt.revoked = false AND rt.expiresAt > :now")
    boolean existsValidToken(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);
    
    /**
     * Find the most recent refresh token for a user
     * Used to get the latest session information
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user ORDER BY rt.createdAt DESC")
    List<RefreshToken> findMostRecentForUser(@Param("user") User user);
    
    /**
     * Clean up tokens for security - revoke all tokens except the current one
     * Used when implementing "logout other devices" functionality
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :revokedAt, rt.revokedReason = :reason WHERE rt.user = :user AND rt.tokenHash != :currentTokenHash AND rt.revoked = false")
    int revokeAllOtherTokensForUser(@Param("user") User user, @Param("currentTokenHash") String currentTokenHash, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") String reason);
}