package com.cafm.cafmbackend.infrastructure.persistence.repository.base;

import com.cafm.cafmbackend.infrastructure.persistence.entity.base.TenantAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Base repository interface for tenant-aware entities.
 * 
 * Explanation:
 * - Purpose: Provides tenant-safe repository operations with automatic filtering
 * - Pattern: Repository pattern with tenant isolation built-in
 * - Java 23: Uses modern Spring Data JPA features with specification support
 * - Architecture: Ensures all data access respects tenant boundaries
 * - Standards: Prevents accidental cross-tenant data access at repository level
 */
@NoRepositoryBean
public interface TenantAwareRepository<T extends TenantAware, ID> 
    extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
    
    // ========== Tenant-Safe Query Methods ==========
    
    /**
     * Find entity by ID with tenant validation
     * @param id Entity ID
     * @param companyId Tenant ID to validate against
     * @return Optional entity if found and belongs to tenant
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.company.id = :companyId AND e.deletedAt IS NULL")
    Optional<T> findByIdAndCompanyId(@Param("id") ID id, @Param("companyId") UUID companyId);
    
    /**
     * Find all entities for a specific tenant
     * @param companyId Tenant ID
     * @return List of entities belonging to the tenant
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.company.id = :companyId AND e.deletedAt IS NULL ORDER BY e.createdAt DESC")
    List<T> findAllByCompanyId(@Param("companyId") UUID companyId);
    
    /**
     * Find all entities for a specific tenant with pagination
     * @param companyId Tenant ID
     * @param pageable Pagination parameters
     * @return Page of entities belonging to the tenant
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.company.id = :companyId AND e.deletedAt IS NULL")
    Page<T> findAllByCompanyId(@Param("companyId") UUID companyId, Pageable pageable);
    
    /**
     * Find all active entities for a specific tenant
     * @param companyId Tenant ID
     * @return List of active entities belonging to the tenant
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.company.id = :companyId AND e.deletedAt IS NULL")
    List<T> findActiveByCompanyId(@Param("companyId") UUID companyId);
    
    /**
     * Count entities for a specific tenant
     * @param companyId Tenant ID
     * @return Number of entities belonging to the tenant
     */
    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.company.id = :companyId AND e.deletedAt IS NULL")
    long countByCompanyId(@Param("companyId") UUID companyId);
    
    /**
     * Check if entity exists for a specific tenant
     * @param id Entity ID
     * @param companyId Tenant ID
     * @return true if entity exists and belongs to tenant
     */
    @Query("SELECT COUNT(e) > 0 FROM #{#entityName} e WHERE e.id = :id AND e.company.id = :companyId AND e.deletedAt IS NULL")
    boolean existsByIdAndCompanyId(@Param("id") ID id, @Param("companyId") UUID companyId);
    
    // ========== Tenant-Safe Bulk Operations ==========
    
    /**
     * Soft delete entity by ID with tenant validation
     * @param id Entity ID
     * @param companyId Tenant ID to validate against
     * @param deletedBy User ID performing the deletion
     * @param reason Deletion reason
     * @return Number of entities updated
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deletedAt = CURRENT_TIMESTAMP, e.deletedBy = :deletedBy, e.deletionReason = :reason " +
           "WHERE e.id = :id AND e.company.id = :companyId AND e.deletedAt IS NULL")
    int softDeleteByIdAndCompanyId(@Param("id") ID id, @Param("companyId") UUID companyId, 
                                  @Param("deletedBy") UUID deletedBy, @Param("reason") String reason);
    
    /**
     * Soft delete multiple entities by IDs with tenant validation
     * @param ids List of entity IDs
     * @param companyId Tenant ID to validate against
     * @param deletedBy User ID performing the deletion
     * @param reason Deletion reason
     * @return Number of entities updated
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deletedAt = CURRENT_TIMESTAMP, e.deletedBy = :deletedBy, e.deletionReason = :reason " +
           "WHERE e.id IN :ids AND e.company.id = :companyId AND e.deletedAt IS NULL")
    int softDeleteByIdsAndCompanyId(@Param("ids") List<ID> ids, @Param("companyId") UUID companyId,
                                   @Param("deletedBy") UUID deletedBy, @Param("reason") String reason);
    
    /**
     * Restore soft-deleted entity by ID with tenant validation
     * @param id Entity ID
     * @param companyId Tenant ID to validate against
     * @return Number of entities updated
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deletedAt = NULL, e.deletedBy = NULL, e.deletionReason = NULL " +
           "WHERE e.id = :id AND e.company.id = :companyId AND e.deletedAt IS NOT NULL")
    int restoreByIdAndCompanyId(@Param("id") ID id, @Param("companyId") UUID companyId);
    
    // ========== Advanced Tenant Query Methods ==========
    
    /**
     * Find entities created between dates for a specific tenant
     * @param companyId Tenant ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of entities created in the date range
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.company.id = :companyId AND e.deletedAt IS NULL " +
           "AND e.createdAt >= :startDate AND e.createdAt <= :endDate ORDER BY e.createdAt DESC")
    List<T> findByCompanyIdAndCreatedAtBetween(@Param("companyId") UUID companyId,
                                              @Param("startDate") java.time.LocalDateTime startDate,
                                              @Param("endDate") java.time.LocalDateTime endDate);
    
    /**
     * Find recently updated entities for a specific tenant
     * @param companyId Tenant ID
     * @param since DateTime to search from
     * @return List of recently updated entities
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.company.id = :companyId AND e.deletedAt IS NULL " +
           "AND e.updatedAt >= :since ORDER BY e.updatedAt DESC")
    List<T> findRecentlyUpdatedByCompanyId(@Param("companyId") UUID companyId,
                                          @Param("since") java.time.LocalDateTime since);
    
    /**
     * Find soft-deleted entities for a specific tenant (for admin recovery)
     * @param companyId Tenant ID
     * @return List of soft-deleted entities
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.company.id = :companyId AND e.deletedAt IS NOT NULL " +
           "ORDER BY e.deletedAt DESC")
    List<T> findDeletedByCompanyId(@Param("companyId") UUID companyId);
    
    /**
     * Find entities that can be restored (within 30 days) for a specific tenant
     * @param companyId Tenant ID
     * @return List of restorable entities
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.company.id = :companyId AND e.deletedAt IS NOT NULL " +
           "AND e.deletedAt > :cutoffDate ORDER BY e.deletedAt DESC")
    List<T> findRestorableByCompanyId(@Param("companyId") UUID companyId,
                                     @Param("cutoffDate") java.time.LocalDateTime cutoffDate);
    
    /**
     * Find entities that should be purged (older than 90 days) for a specific tenant
     * @param companyId Tenant ID
     * @return List of entities eligible for purging
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.company.id = :companyId AND e.deletedAt IS NOT NULL " +
           "AND e.deletedAt < :cutoffDate ORDER BY e.deletedAt ASC")
    List<T> findPurgeCandidatesByCompanyId(@Param("companyId") UUID companyId,
                                          @Param("cutoffDate") java.time.LocalDateTime cutoffDate);
    
    // ========== Tenant Statistics Methods ==========
    
    /**
     * Get entity count statistics for a tenant
     * @param companyId Tenant ID
     * @return Statistics object with counts
     */
    @Query("SELECT new com.cafm.cafmbackend.dto.common.TenantEntityStats(" +
           "COUNT(CASE WHEN e.deletedAt IS NULL THEN 1 END), " +
           "COUNT(CASE WHEN e.deletedAt IS NOT NULL THEN 1 END), " +
           "COUNT(*)) " +
           "FROM #{#entityName} e WHERE e.company.id = :companyId")
    TenantEntityStats getStatsByCompanyId(@Param("companyId") UUID companyId);
    
    // ========== Security Validation Methods ==========
    
    /**
     * Validate that all provided IDs belong to the specified tenant
     * @param ids List of entity IDs to validate
     * @param companyId Tenant ID
     * @return true if all entities belong to the tenant
     */
    @Query("SELECT COUNT(e) = :expectedCount FROM #{#entityName} e " +
           "WHERE e.id IN :ids AND e.company.id = :companyId")
    boolean validateAllIdsBelongToTenant(@Param("ids") List<ID> ids, 
                                        @Param("companyId") UUID companyId,
                                        @Param("expectedCount") long expectedCount);
    
    /**
     * Find IDs that don't belong to the specified tenant (for security validation)
     * @param ids List of entity IDs to check
     * @param companyId Tenant ID
     * @return List of IDs that don't belong to the tenant
     */
    @Query("SELECT :allIds WHERE :allIds NOT IN " +
           "(SELECT e.id FROM #{#entityName} e WHERE e.id IN :allIds AND e.company.id = :companyId)")
    List<ID> findIdsNotBelongingToTenant(@Param("allIds") List<ID> ids, @Param("companyId") UUID companyId);
    
    // ========== Helper Record for Statistics ==========
    
    /**
     * Record for tenant entity statistics
     */
    record TenantEntityStats(
        long activeCount,
        long deletedCount,
        long totalCount
    ) {}
}