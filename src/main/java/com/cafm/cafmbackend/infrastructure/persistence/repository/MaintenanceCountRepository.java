package com.cafm.cafmbackend.infrastructure.persistence.repository;

import com.cafm.cafmbackend.infrastructure.persistence.entity.MaintenanceCount;
import com.cafm.cafmbackend.infrastructure.persistence.entity.MaintenanceCount.MaintenanceCountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MaintenanceCount entities with support for complex JSONB queries.
 */
@Repository
public interface MaintenanceCountRepository extends JpaRepository<MaintenanceCount, UUID>, 
                                                   JpaSpecificationExecutor<MaintenanceCount> {

    /**
     * Find all maintenance counts for a specific school.
     */
    Page<MaintenanceCount> findBySchoolIdAndDeletedAtIsNull(UUID schoolId, Pageable pageable);

    /**
     * Find all maintenance counts for a specific supervisor.
     */
    Page<MaintenanceCount> findBySupervisorIdAndDeletedAtIsNull(UUID supervisorId, Pageable pageable);

    /**
     * Find all maintenance counts by status.
     */
    Page<MaintenanceCount> findByStatusAndDeletedAtIsNull(MaintenanceCountStatus status, Pageable pageable);

    /**
     * Find all maintenance counts for a company.
     */
    Page<MaintenanceCount> findByCompany_IdAndDeletedAtIsNull(UUID companyId, Pageable pageable);
    
    // Compatibility method
    default Page<MaintenanceCount> findByCompanyIdAndDeletedAtIsNull(UUID companyId, Pageable pageable) {
        return findByCompany_IdAndDeletedAtIsNull(companyId, pageable);
    }

    /**
     * Find maintenance counts for a school and supervisor.
     */
    List<MaintenanceCount> findBySchoolIdAndSupervisorIdAndDeletedAtIsNull(UUID schoolId, UUID supervisorId);

    /**
     * Find latest maintenance count for a school.
     */
    Optional<MaintenanceCount> findFirstBySchoolIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID schoolId);

    /**
     * Find maintenance counts created within a date range.
     */
    @Query("SELECT m FROM MaintenanceCount m WHERE m.company.id = :companyId " +
           "AND m.createdAt BETWEEN :startDate AND :endDate " +
           "AND m.deletedAt IS NULL")
    List<MaintenanceCount> findByCompanyIdAndDateRange(@Param("companyId") UUID companyId,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Find maintenance counts with specific item counts using JSONB query.
     */
    @Query(value = "SELECT * FROM maintenance_counts " +
                   "WHERE company_id = :companyId " +
                   "AND item_counts @> :itemFilter::jsonb " +
                   "AND deleted_at IS NULL", 
           nativeQuery = true)
    List<MaintenanceCount> findByItemCountsContaining(@Param("companyId") UUID companyId,
                                                      @Param("itemFilter") String itemFilter);

    /**
     * Find maintenance counts with photos in specific sections.
     */
//    @Query(value = "SELECT * FROM maintenance_counts " +
//                   "WHERE company_id = :companyId " +
//                   "AND section_photos ? :section " +
//                   "AND deleted_at IS NULL",
//           nativeQuery = true)
//    List<MaintenanceCount> findBySectionPhotosContainingSection(@Param("companyId") UUID companyId,
//                                                                @Param("section") String section);

    /**
     * Count maintenance counts by status for a company.
     */
    @Query("SELECT m.status, COUNT(m) FROM MaintenanceCount m " +
           "WHERE m.company.id = :companyId AND m.deletedAt IS NULL " +
           "GROUP BY m.status")
    List<Object[]> countByStatusForCompany(@Param("companyId") UUID companyId);

    /**
     * Find maintenance counts submitted within a specific period.
     */
    @Query("SELECT m FROM MaintenanceCount m WHERE m.company.id = :companyId " +
           "AND m.submittedAt BETWEEN :startDate AND :endDate " +
           "AND m.deletedAt IS NULL")
    List<MaintenanceCount> findSubmittedInPeriod(@Param("companyId") UUID companyId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Check if a maintenance count exists for a school in the current month.
     */
    @Query("SELECT COUNT(m) > 0 FROM MaintenanceCount m " +
           "WHERE m.school.id = :schoolId " +
           "AND EXTRACT(MONTH FROM m.createdAt) = EXTRACT(MONTH FROM CURRENT_TIMESTAMP) " +
           "AND EXTRACT(YEAR FROM m.createdAt) = EXTRACT(YEAR FROM CURRENT_TIMESTAMP) " +
           "AND m.deletedAt IS NULL")
    boolean existsForSchoolInCurrentMonth(@Param("schoolId") UUID schoolId);

    /**
     * Find all draft maintenance counts older than specified days.
     */
    @Query("SELECT m FROM MaintenanceCount m WHERE m.status = :status " +
           "AND m.createdAt < :cutoffDate " +
           "AND m.deletedAt IS NULL")
    List<MaintenanceCount> findOldDrafts(@Param("status") MaintenanceCountStatus status, @Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Search maintenance counts with text in various JSONB fields.
     */
    @Query(value = "SELECT * FROM maintenance_counts " +
                   "WHERE company_id = :companyId " +
                   "AND (text_answers::text ILIKE :searchTerm " +
                   "OR maintenance_notes::text ILIKE :searchTerm " +
                   "OR survey_answers::text ILIKE :searchTerm) " +
                   "AND deleted_at IS NULL", 
           nativeQuery = true)
    Page<MaintenanceCount> searchInJsonFields(@Param("companyId") UUID companyId,
                                              @Param("searchTerm") String searchTerm,
                                              Pageable pageable);
}