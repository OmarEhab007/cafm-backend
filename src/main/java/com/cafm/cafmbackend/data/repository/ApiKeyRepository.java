package com.cafm.cafmbackend.data.repository;

import com.cafm.cafmbackend.data.entity.ApiKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * Repository for API key management.
 * 
 * Purpose: Data access for API key operations
 * Pattern: Spring Data JPA repository
 * Java 23: Ready for virtual threads in queries
 * Architecture: Data layer repository
 * Standards: Secure API key queries
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    
    /**
     * Find API key by prefix and active status
     */
    Optional<ApiKey> findByKeyPrefixAndIsActiveTrue(String keyPrefix);
    
    /**
     * Find API key by name
     */
    Optional<ApiKey> findByKeyName(String keyName);
    
    /**
     * Check if key name exists
     */
    boolean existsByKeyName(String keyName);
    
    /**
     * Find all keys for a company
     */
    Page<ApiKey> findByCompany_Id(UUID companyId, Pageable pageable);
    
    // Compatibility method
    default Page<ApiKey> findByCompanyId(UUID companyId, Pageable pageable) {
        return findByCompany_Id(companyId, pageable);
    }
    
    /**
     * Find active keys for a company
     */
    List<ApiKey> findByCompany_IdAndIsActiveTrue(UUID companyId);
    
    // Compatibility method
    default List<ApiKey> findByCompanyIdAndIsActiveTrue(UUID companyId) {
        return findByCompany_IdAndIsActiveTrue(companyId);
    }
    
    /**
     * Find expired keys
     */
    @Query("""
        SELECT ak FROM ApiKey ak 
        WHERE ak.expiresAt IS NOT NULL 
        AND ak.expiresAt < :now 
        AND ak.isActive = true
        """)
    List<ApiKey> findExpiredKeys(@Param("now") LocalDateTime now);
    
    /**
     * Find unused keys (not used in last N days)
     */
    @Query("""
        SELECT ak FROM ApiKey ak 
        WHERE ak.lastUsedAt IS NULL 
        OR ak.lastUsedAt < :cutoffDate
        """)
    List<ApiKey> findUnusedKeys(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Update last used timestamp
     */
    @Modifying
    @Query("""
        UPDATE ApiKey ak 
        SET ak.lastUsedAt = :now, 
            ak.lastUsedIp = :ip, 
            ak.usageCount = ak.usageCount + 1 
        WHERE ak.id = :keyId
        """)
    void updateUsage(@Param("keyId") UUID keyId, 
                    @Param("now") LocalDateTime now, 
                    @Param("ip") String ip);
    
    /**
     * Revoke API key
     */
    @Modifying
    @Query("""
        UPDATE ApiKey ak 
        SET ak.isActive = false, 
            ak.revokedAt = :now, 
            ak.revokedByUserId = :userId, 
            ak.revokeReason = :reason 
        WHERE ak.id = :keyId
        """)
    void revokeKey(@Param("keyId") UUID keyId, 
                  @Param("now") LocalDateTime now, 
                  @Param("userId") UUID userId, 
                  @Param("reason") String reason);
    
    /**
     * Deactivate expired keys
     */
    @Modifying
    @Query("""
        UPDATE ApiKey ak 
        SET ak.isActive = false 
        WHERE ak.expiresAt IS NOT NULL 
        AND ak.expiresAt < :now 
        AND ak.isActive = true
        """)
    int deactivateExpiredKeys(@Param("now") LocalDateTime now);
    
    /**
     * Get usage statistics for a company
     */
    @Query("""
        SELECT COUNT(ak) as totalKeys,
               COUNT(CASE WHEN ak.isActive = true THEN 1 END) as activeKeys,
               COUNT(CASE WHEN ak.revokedAt IS NOT NULL THEN 1 END) as revokedKeys,
               SUM(ak.usageCount) as totalUsage
        FROM ApiKey ak 
        WHERE ak.company.id = :companyId
        """)
    Object getUsageStatistics(@Param("companyId") UUID companyId);
    
    /**
     * Find keys by scope
     */
    @Query("""
        SELECT ak FROM ApiKey ak 
        JOIN ak.scopes scope 
        WHERE scope = :scope 
        AND ak.isActive = true
        """)
    List<ApiKey> findByScope(@Param("scope") String scope);
    
    /**
     * Find keys created by user
     */
    List<ApiKey> findByCreatedByUserId(UUID userId);
    
    /**
     * Delete old revoked keys
     */
    @Modifying
    @Query("""
        DELETE FROM ApiKey ak 
        WHERE ak.revokedAt IS NOT NULL 
        AND ak.revokedAt < :cutoffDate
        """)
    int deleteOldRevokedKeys(@Param("cutoffDate") LocalDateTime cutoffDate);
}