package com.cafm.cafmbackend.infrastructure.persistence.repository;

import com.cafm.cafmbackend.infrastructure.persistence.entity.DamageCount;
import com.cafm.cafmbackend.infrastructure.persistence.entity.DamageCount.DamageCountStatus;
import com.cafm.cafmbackend.infrastructure.persistence.entity.DamageCount.PriorityLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for DamageCount entities with support for damage tracking queries.
 */
@Repository
public interface DamageCountRepository extends JpaRepository<DamageCount, UUID>, 
                                              JpaSpecificationExecutor<DamageCount> {

    /**
     * Find all damage counts for a specific school.
     */
    Page<DamageCount> findBySchoolIdAndDeletedAtIsNull(UUID schoolId, Pageable pageable);

    /**
     * Find all damage counts for a specific supervisor.
     */
    Page<DamageCount> findBySupervisorIdAndDeletedAtIsNull(UUID supervisorId, Pageable pageable);

    /**
     * Find all damage counts by status.
     */
    Page<DamageCount> findByStatusAndDeletedAtIsNull(DamageCountStatus status, Pageable pageable);

    /**
     * Find all damage counts by priority.
     */
    Page<DamageCount> findByPriorityAndDeletedAtIsNull(PriorityLevel priority, Pageable pageable);

    /**
     * Find all damage counts for a company.
     */
    Page<DamageCount> findByCompany_IdAndDeletedAtIsNull(UUID companyId, Pageable pageable);
    
    // Compatibility method
    default Page<DamageCount> findByCompanyIdAndDeletedAtIsNull(UUID companyId, Pageable pageable) {
        return findByCompany_IdAndDeletedAtIsNull(companyId, pageable);
    }

    /**
     * Find damage counts for a school and supervisor.
     */
    List<DamageCount> findBySchoolIdAndSupervisorIdAndDeletedAtIsNull(UUID schoolId, UUID supervisorId);

    /**
     * Find latest damage count for a school.
     */
    Optional<DamageCount> findFirstBySchoolIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID schoolId);

    /**
     * Find damage counts created within a date range.
     */
    @Query("SELECT d FROM DamageCount d WHERE d.company.id = :companyId " +
           "AND d.createdAt BETWEEN :startDate AND :endDate " +
           "AND d.deletedAt IS NULL")
    List<DamageCount> findByCompanyIdAndDateRange(@Param("companyId") UUID companyId,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Find damage counts with repair cost above threshold.
     */
    @Query("SELECT d FROM DamageCount d WHERE d.company.id = :companyId " +
           "AND d.estimatedRepairCost >= :threshold " +
           "AND d.deletedAt IS NULL")
    List<DamageCount> findByRepairCostAboveThreshold(@Param("companyId") UUID companyId,
                                                     @Param("threshold") BigDecimal threshold);

    /**
     * Find damage counts with specific items using JSONB query.
     */
    @Query(value = "SELECT * FROM damage_counts " +
                   "WHERE company_id = :companyId " +
                   "AND item_counts @> :itemFilter::jsonb " +
                   "AND deleted_at IS NULL", 
           nativeQuery = true)
    List<DamageCount> findByItemCountsContaining(@Param("companyId") UUID companyId,
                                                 @Param("itemFilter") String itemFilter);

    /**
     * Count damage counts by status for a company.
     */
    @Query("SELECT d.status, COUNT(d) FROM DamageCount d " +
           "WHERE d.company.id = :companyId AND d.deletedAt IS NULL " +
           "GROUP BY d.status")
    List<Object[]> countByStatusForCompany(@Param("companyId") UUID companyId);

    /**
     * Count damage counts by priority for a company.
     */
    @Query("SELECT d.priority, COUNT(d) FROM DamageCount d " +
           "WHERE d.company.id = :companyId AND d.deletedAt IS NULL " +
           "GROUP BY d.priority")
    List<Object[]> countByPriorityForCompany(@Param("companyId") UUID companyId);

    /**
     * Find damage counts submitted within a specific period.
     */
    @Query("SELECT d FROM DamageCount d WHERE d.company.id = :companyId " +
           "AND d.submittedAt BETWEEN :startDate AND :endDate " +
           "AND d.deletedAt IS NULL")
    List<DamageCount> findSubmittedInPeriod(@Param("companyId") UUID companyId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate total repair cost for a company.
     */
    @Query("SELECT SUM(d.estimatedRepairCost) FROM DamageCount d " +
           "WHERE d.company.id = :companyId " +
           "AND d.status != :excludeStatus " +
           "AND d.deletedAt IS NULL")
    BigDecimal calculateTotalPendingRepairCost(@Param("companyId") UUID companyId, @Param("excludeStatus") DamageCountStatus excludeStatus);

    /**
     * Find high priority damage counts that are not completed.
     */
    @Query("SELECT d FROM DamageCount d WHERE d.company.id = :companyId " +
           "AND d.priority IN :highPriorities " +
           "AND d.status != :excludeStatus " +
           "AND d.deletedAt IS NULL " +
           "ORDER BY d.priority DESC, d.createdAt ASC")
    List<DamageCount> findHighPriorityPending(@Param("companyId") UUID companyId, @Param("highPriorities") List<PriorityLevel> highPriorities, @Param("excludeStatus") DamageCountStatus excludeStatus);

    /**
     * Check if a damage count exists for a school in the current month.
     */
    @Query("SELECT COUNT(d) > 0 FROM DamageCount d " +
           "WHERE d.school.id = :schoolId " +
           "AND EXTRACT(MONTH FROM d.createdAt) = EXTRACT(MONTH FROM CURRENT_TIMESTAMP) " +
           "AND EXTRACT(YEAR FROM d.createdAt) = EXTRACT(YEAR FROM CURRENT_TIMESTAMP) " +
           "AND d.deletedAt IS NULL")
    boolean existsForSchoolInCurrentMonth(@Param("schoolId") UUID schoolId);
}