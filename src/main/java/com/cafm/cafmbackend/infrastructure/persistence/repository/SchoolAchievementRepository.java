package com.cafm.cafmbackend.infrastructure.persistence.repository;

import com.cafm.cafmbackend.infrastructure.persistence.entity.SchoolAchievement;
import com.cafm.cafmbackend.infrastructure.persistence.entity.SchoolAchievement.AchievementStatus;
import com.cafm.cafmbackend.infrastructure.persistence.entity.SchoolAchievement.AchievementType;
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
 * Repository for SchoolAchievement entities with support for achievement tracking.
 */
@Repository
public interface SchoolAchievementRepository extends JpaRepository<SchoolAchievement, UUID>, 
                                                    JpaSpecificationExecutor<SchoolAchievement> {

    /**
     * Find all achievements for a specific school.
     */
    Page<SchoolAchievement> findBySchoolIdAndDeletedAtIsNull(UUID schoolId, Pageable pageable);

    /**
     * Find all achievements for a specific supervisor.
     */
    Page<SchoolAchievement> findBySupervisorIdAndDeletedAtIsNull(UUID supervisorId, Pageable pageable);

    /**
     * Find all achievements by status.
     */
    Page<SchoolAchievement> findByStatusAndDeletedAtIsNull(AchievementStatus status, Pageable pageable);

    /**
     * Find all achievements by type.
     */
    Page<SchoolAchievement> findByAchievementTypeAndDeletedAtIsNull(AchievementType type, Pageable pageable);

    /**
     * Find all achievements for a company.
     */
    Page<SchoolAchievement> findByCompany_IdAndDeletedAtIsNull(UUID companyId, Pageable pageable);
    
    // Compatibility method
    default Page<SchoolAchievement> findByCompanyIdAndDeletedAtIsNull(UUID companyId, Pageable pageable) {
        return findByCompany_IdAndDeletedAtIsNull(companyId, pageable);
    }

    /**
     * Find achievements for a school and supervisor.
     */
    List<SchoolAchievement> findBySchoolIdAndSupervisorIdAndDeletedAtIsNull(UUID schoolId, UUID supervisorId);

    /**
     * Find latest achievement for a school.
     */
    Optional<SchoolAchievement> findFirstBySchoolIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID schoolId);

    /**
     * Find achievements created within a date range.
     */
    @Query("SELECT sa FROM SchoolAchievement sa WHERE sa.company.id = :companyId " +
           "AND sa.createdAt BETWEEN :startDate AND :endDate " +
           "AND sa.deletedAt IS NULL")
    List<SchoolAchievement> findByCompanyIdAndDateRange(@Param("companyId") UUID companyId,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Find achievements pending approval.
     */
    @Query("SELECT sa FROM SchoolAchievement sa WHERE sa.company.id = :companyId " +
           "AND sa.status = :status " +
           "AND sa.approvedAt IS NULL " +
           "AND sa.deletedAt IS NULL " +
           "ORDER BY sa.submittedAt ASC")
    List<SchoolAchievement> findPendingApproval(@Param("companyId") UUID companyId, @Param("status") AchievementStatus status);

    /**
     * Find achievements approved by a specific user.
     */
    @Query("SELECT sa FROM SchoolAchievement sa WHERE sa.approvedBy.id = :approverId " +
           "AND sa.deletedAt IS NULL")
    Page<SchoolAchievement> findByApproverId(@Param("approverId") UUID approverId, Pageable pageable);

    /**
     * Count achievements by status for a company.
     */
    @Query("SELECT sa.status, COUNT(sa) FROM SchoolAchievement sa " +
           "WHERE sa.company.id = :companyId AND sa.deletedAt IS NULL " +
           "GROUP BY sa.status")
    List<Object[]> countByStatusForCompany(@Param("companyId") UUID companyId);

    /**
     * Count achievements by type for a company.
     */
    @Query("SELECT sa.achievementType, COUNT(sa) FROM SchoolAchievement sa " +
           "WHERE sa.company.id = :companyId AND sa.deletedAt IS NULL " +
           "GROUP BY sa.achievementType")
    List<Object[]> countByTypeForCompany(@Param("companyId") UUID companyId);

    /**
     * Find achievements submitted within a specific period.
     */
    @Query("SELECT sa FROM SchoolAchievement sa WHERE sa.company.id = :companyId " +
           "AND sa.submittedAt BETWEEN :startDate AND :endDate " +
           "AND sa.deletedAt IS NULL")
    List<SchoolAchievement> findSubmittedInPeriod(@Param("companyId") UUID companyId,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Find achievements with photos.
     */
    @Query(value = "SELECT * FROM school_achievements " +
                   "WHERE company_id = :companyId " +
                   "AND jsonb_array_length(photos) > 0 " +
                   "AND deleted_at IS NULL", 
           nativeQuery = true)
    List<SchoolAchievement> findAchievementsWithPhotos(@Param("companyId") UUID companyId);

    /**
     * Find achievements by category.
     */
    @Query("SELECT sa FROM SchoolAchievement sa WHERE sa.company.id = :companyId " +
           "AND sa.category = :category " +
           "AND sa.deletedAt IS NULL")
    Page<SchoolAchievement> findByCategory(@Param("companyId") UUID companyId,
                                           @Param("category") String category,
                                           Pageable pageable);

    /**
     * Check if an achievement exists for a school in the current month.
     */
    @Query("SELECT COUNT(sa) > 0 FROM SchoolAchievement sa " +
           "WHERE sa.school.id = :schoolId " +
           "AND sa.achievementType = :type " +
           "AND EXTRACT(MONTH FROM sa.createdAt) = EXTRACT(MONTH FROM CURRENT_TIMESTAMP) " +
           "AND EXTRACT(YEAR FROM sa.createdAt) = EXTRACT(YEAR FROM CURRENT_TIMESTAMP) " +
           "AND sa.deletedAt IS NULL")
    boolean existsForSchoolInCurrentMonth(@Param("schoolId") UUID schoolId,
                                          @Param("type") AchievementType type);

    /**
     * Find rejected achievements.
     */
    @Query("SELECT sa FROM SchoolAchievement sa WHERE sa.company.id = :companyId " +
           "AND sa.status = :status " +
           "AND sa.deletedAt IS NULL " +
           "ORDER BY sa.approvedAt DESC")
    Page<SchoolAchievement> findRejectedAchievements(@Param("companyId") UUID companyId, @Param("status") AchievementStatus status, Pageable pageable);
}