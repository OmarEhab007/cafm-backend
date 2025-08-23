package com.cafm.cafmbackend.data.repository;

import com.cafm.cafmbackend.data.entity.EmailVerificationToken;
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
 * Repository for email verification token operations.
 * 
 * Purpose: Data access layer for email verification token management
 * Pattern: Spring Data JPA repository with custom queries for token validation
 * Java 23: Ready for virtual thread execution of database queries
 * Architecture: Follows repository pattern in data layer
 * Standards: Implements secure token lookup with automatic cleanup
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    
    /**
     * Find a valid token by its hash
     */
    Optional<EmailVerificationToken> findByTokenHashAndVerifiedFalseAndExpiresAtAfter(
        String tokenHash, LocalDateTime now);
    
    /**
     * Find the latest valid token for a user
     */
    Optional<EmailVerificationToken> findFirstByUserAndVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
        User user, LocalDateTime now);
    
    /**
     * Find tokens by email address (for re-verification)
     */
    Optional<EmailVerificationToken> findFirstByEmailAndVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
        String email, LocalDateTime now);
    
    /**
     * Count valid tokens for a user (for rate limiting)
     */
    long countByUserAndCreatedAtAfterAndVerifiedFalse(User user, LocalDateTime since);
    
    /**
     * Invalidate all unverified tokens for a user
     */
    @Modifying
    @Query("UPDATE EmailVerificationToken t SET t.verified = true, t.verifiedAt = :now WHERE t.user = :user AND t.verified = false")
    void invalidateAllTokensForUser(@Param("user") User user, @Param("now") LocalDateTime now);
    
    /**
     * Delete expired tokens (for cleanup)
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :cutoff")
    int deleteExpiredTokens(@Param("cutoff") LocalDateTime cutoff);
    
    /**
     * Delete verified tokens older than specified date (for cleanup)
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.verified = true AND t.verifiedAt < :cutoff")
    int deleteVerifiedTokens(@Param("cutoff") LocalDateTime cutoff);
    
    /**
     * Check if email is already verified
     */
    @Query("SELECT COUNT(t) > 0 FROM EmailVerificationToken t WHERE t.email = :email AND t.verified = true")
    boolean existsByEmailAndVerifiedTrue(@Param("email") String email);
}