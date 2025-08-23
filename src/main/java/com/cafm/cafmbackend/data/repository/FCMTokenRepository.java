package com.cafm.cafmbackend.data.repository;

import com.cafm.cafmbackend.data.entity.FCMToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for FCMToken entities managing device tokens for push notifications.
 */
@Repository
public interface FCMTokenRepository extends JpaRepository<FCMToken, UUID>, 
                                           JpaSpecificationExecutor<FCMToken> {

    /**
     * Find token by token string.
     */
    Optional<FCMToken> findByToken(String token);

    /**
     * Find all active tokens for a user.
     */
    List<FCMToken> findByUserIdAndActive(UUID userId, Boolean active);

    /**
     * Find all tokens for a user.
     */
    List<FCMToken> findByUserId(UUID userId);

    /**
     * Find token by user and device.
     */
    Optional<FCMToken> findByUserIdAndDeviceId(UUID userId, String deviceId);

    /**
     * Find all active tokens for a company.
     */
    @Query("SELECT f FROM FCMToken f WHERE f.company.id = :companyId " +
           "AND f.active = true")
    List<FCMToken> findActiveTokensByCompany(@Param("companyId") UUID companyId);

    /**
     * Find tokens by platform.
     */
    Page<FCMToken> findByPlatformAndActive(String platform, 
                                            Boolean active, 
                                            Pageable pageable);


    /**
     * Find active tokens for user.
     */
    @Query("SELECT f FROM FCMToken f WHERE f.user.id = :userId " +
           "AND f.active = true")
    List<FCMToken> findActiveTokens(@Param("userId") UUID userId);

    /**
     * Count active tokens by platform for a company.
     */
    @Query("SELECT f.platform, COUNT(f) FROM FCMToken f " +
           "WHERE f.company.id = :companyId " +
           "AND f.active = true " +
           "GROUP BY f.platform")
    List<Object[]> countActiveTokensByPlatform(@Param("companyId") UUID companyId);

    /**
     * Check if user has any active tokens.
     */
    @Query("SELECT COUNT(f) > 0 FROM FCMToken f " +
           "WHERE f.user.id = :userId " +
           "AND f.active = true")
    boolean hasActiveTokens(@Param("userId") UUID userId);

    /**
     * Find users with active tokens in a company.
     */
    @Query("SELECT DISTINCT f.user.id FROM FCMToken f " +
           "WHERE f.company.id = :companyId " +
           "AND f.active = true")
    List<UUID> findUsersWithActiveTokens(@Param("companyId") UUID companyId);


    /**
     * Deactivate all tokens for a user.
     */
    @Modifying
    @Query("UPDATE FCMToken f SET f.active = false " +
           "WHERE f.user.id = :userId")
    int deactivateUserTokens(@Param("userId") UUID userId);


    /**
     * Count active tokens for a user.
     */
    @Query("SELECT COUNT(f) FROM FCMToken f WHERE f.user.id = :userId " +
           "AND f.active = true")
    long countActiveTokensByUserId(@Param("userId") UUID userId);
}