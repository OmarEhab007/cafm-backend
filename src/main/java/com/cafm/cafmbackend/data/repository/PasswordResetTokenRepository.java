package com.cafm.cafmbackend.data.repository;

import com.cafm.cafmbackend.data.entity.PasswordResetToken;
import com.cafm.cafmbackend.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for password reset token operations.
 * 
 * Purpose: Data access layer for password reset token management
 * Pattern: Spring Data JPA repository with custom queries
 * Java 23: Ready for virtual thread execution of queries
 * Architecture: Follows repository pattern for data access
 * Standards: Implements secure token lookup and cleanup
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    
    /**
     * Find a valid token by its hash
     */
    Optional<PasswordResetToken> findByTokenHashAndUsedFalseAndExpiresAtAfter(
        String tokenHash, LocalDateTime now);
    
    /**
     * Find the latest valid token for a user
     */
    Optional<PasswordResetToken> findFirstByUserAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
        User user, LocalDateTime now);
    
    /**
     * Count valid tokens for a user (for rate limiting)
     */
    long countByUserAndCreatedAtAfterAndUsedFalse(User user, LocalDateTime since);
    
    /**
     * Invalidate all unused tokens for a user
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.user = :user AND t.used = false")
    void invalidateAllTokensForUser(@Param("user") User user);
    
    /**
     * Delete expired tokens (for cleanup)
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :cutoff")
    int deleteExpiredTokens(@Param("cutoff") LocalDateTime cutoff);
    
    /**
     * Delete used tokens older than specified date (for cleanup)
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.used = true AND t.usedAt < :cutoff")
    int deleteUsedTokens(@Param("cutoff") LocalDateTime cutoff);
}