package com.cafm.cafmbackend.infrastructure.persistence.repository;

import com.cafm.cafmbackend.infrastructure.persistence.entity.Report;
import com.cafm.cafmbackend.shared.enums.ReportPriority;
import com.cafm.cafmbackend.shared.enums.ReportStatus;
import com.cafm.cafmbackend.infrastructure.persistence.projection.ReportSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Report entity.
 * 
 * Architecture: Data access layer for work order management
 * Pattern: Repository pattern with workflow queries
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, UUID>, JpaSpecificationExecutor<Report> {
    
    // ========== Basic Queries ==========
    
    /**
     * Find report by report number
     */
    Optional<Report> findByReportNumber(String reportNumber);
    
    /**
     * Check if report number exists
     */
    boolean existsByReportNumber(String reportNumber);
    
    /**
     * Find reports by status
     */
    List<Report> findByStatus(ReportStatus status);
    
    /**
     * Find reports by priority
     */
    List<Report> findByPriority(ReportPriority priority);
    
    // ========== School-based Queries ==========
    
    /**
     * Find reports by school
     */
    @Query("SELECT r FROM Report r WHERE r.school.id = :schoolId AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    Page<Report> findBySchoolId(@Param("schoolId") UUID schoolId, Pageable pageable);
    
    /**
     * Find active reports by school
     */
    @Query("""
        SELECT r FROM Report r 
        WHERE r.school.id = :schoolId 
        AND r.status NOT IN :excludeStatuses 
        AND r.deletedAt IS NULL
        """)
    List<Report> findActiveReportsBySchool(@Param("schoolId") UUID schoolId, @Param("excludeStatuses") List<ReportStatus> excludeStatuses);
    
    // ========== Supervisor-based Queries ==========
    
    /**
     * Find reports by supervisor ID and date range
     */
    List<Report> findBySupervisorIdAndCreatedAtBetween(UUID supervisorId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find reports created by supervisor
     */
    @Query("SELECT r FROM Report r WHERE r.supervisor.id = :supervisorId AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    Page<Report> findBySupervisorId(@Param("supervisorId") UUID supervisorId, Pageable pageable);
    
    /**
     * Find reports assigned to user
     */
    @Query("SELECT r FROM Report r WHERE r.assignedTo.id = :userId AND r.deletedAt IS NULL ORDER BY r.scheduledDate ASC")
    Page<Report> findByAssignedTo(@Param("userId") UUID userId, Pageable pageable);
    
    // ========== Status-based Queries ==========
    
    /**
     * Find pending reports (awaiting action)
     */
    @Query("""
        SELECT r FROM Report r 
        WHERE r.status IN :pendingStatuses 
        AND r.deletedAt IS NULL 
        ORDER BY r.priority DESC, r.reportedDate ASC
        """)
    List<Report> findPendingReports(@Param("pendingStatuses") List<ReportStatus> pendingStatuses);
    
    /**
     * Find reports in progress
     */
    @Query("SELECT r FROM Report r WHERE r.status = :status AND r.deletedAt IS NULL")
    List<Report> findInProgressReports(@Param("status") ReportStatus status);
    
    /**
     * Find overdue reports
     */
    @Query("""
        SELECT r FROM Report r 
        WHERE r.scheduledDate < :today 
        AND r.status NOT IN :excludeStatuses 
        AND r.deletedAt IS NULL
        """)
    List<Report> findOverdueReports(@Param("today") LocalDate today, @Param("excludeStatuses") List<ReportStatus> excludeStatuses);
    
    // ========== Date-based Queries ==========
    
    /**
     * Find reports reported between dates
     */
    @Query("""
        SELECT r FROM Report r 
        WHERE r.reportedDate BETWEEN :startDate AND :endDate 
        AND r.deletedAt IS NULL
        """)
    List<Report> findReportsBetweenDates(@Param("startDate") LocalDate startDate, 
                                         @Param("endDate") LocalDate endDate);
    
    /**
     * Find reports scheduled for date
     */
    @Query("SELECT r FROM Report r WHERE r.scheduledDate = :date AND r.deletedAt IS NULL")
    List<Report> findByScheduledDate(@Param("date") LocalDate date);
    
    /**
     * Find reports completed between dates
     */
    @Query("""
        SELECT r FROM Report r 
        WHERE r.completedDate BETWEEN :startDate AND :endDate 
        AND r.status = :status 
        AND r.deletedAt IS NULL
        """)
    List<Report> findCompletedBetweenDates(@Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate,
                                           @Param("status") ReportStatus status);
    
    // ========== Priority and SLA Queries ==========
    
    /**
     * Find critical/urgent reports
     */
    @Query("""
        SELECT r FROM Report r 
        WHERE r.priority IN :highPriorities 
        AND r.status NOT IN :excludeStatuses 
        AND r.deletedAt IS NULL 
        ORDER BY r.priority DESC, r.reportedDate ASC
        """)
    List<Report> findHighPriorityReports(@Param("highPriorities") List<ReportPriority> highPriorities, @Param("excludeStatuses") List<ReportStatus> excludeStatuses);
    
    /**
     * Find reports breaching SLA
     */
    @Query(value = """
        SELECT * FROM reports r 
        WHERE r.deleted_at IS NULL 
        AND r.status NOT IN ('COMPLETED', 'CANCELLED')
        AND (
            (r.priority = 'CRITICAL' AND r.age_days > 1) OR
            (r.priority = 'URGENT' AND r.age_days > 2) OR
            (r.priority = 'HIGH' AND r.age_days > 7) OR
            (r.priority = 'MEDIUM' AND r.age_days > 14) OR
            (r.priority = 'LOW' AND r.age_days > 30)
        )
        """, nativeQuery = true)
    List<Report> findSLABreachReports();
    
    // ========== Count Queries ==========
    
    /**
     * Count reports by school ID
     */
    long countBySchoolIdAndDeletedAtIsNull(UUID schoolId);
    
    /**
     * Count reports by school ID and status
     */
    long countBySchoolIdAndStatusAndDeletedAtIsNull(UUID schoolId, ReportStatus status);
    
    // ========== Cost Queries ==========
    
    /**
     * Find reports by cost range
     */
    @Query("""
        SELECT r FROM Report r 
        WHERE r.estimatedCost BETWEEN :minCost AND :maxCost 
        AND r.deletedAt IS NULL
        """)
    List<Report> findByCostRange(@Param("minCost") BigDecimal minCost, 
                                 @Param("maxCost") BigDecimal maxCost);
    
    /**
     * Calculate total costs
     */
    @Query("""
        SELECT 
            SUM(r.estimatedCost) as totalEstimated,
            SUM(r.actualCost) as totalActual
        FROM Report r 
        WHERE r.school.id = :schoolId 
        AND r.status = :status 
        AND r.completedDate BETWEEN :startDate AND :endDate
        AND r.deletedAt IS NULL
        """)
    Object calculateCostsForSchool(@Param("schoolId") UUID schoolId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate,
                                   @Param("status") ReportStatus status);
    
    // ========== Projection Queries for Performance ==========
    
    /**
     * Find all reports with projection
     */
    Page<ReportSummaryProjection> findAllProjectedBy(Pageable pageable);
    
    /**
     * Find reports by status with projection
     */
    Page<ReportSummaryProjection> findByStatusAndDeletedAtIsNull(ReportStatus status, Pageable pageable, Class<ReportSummaryProjection> type);
    
    /**
     * Find reports by school with projection
     */
    @Query("""
        SELECT r FROM Report r 
        LEFT JOIN FETCH r.school 
        LEFT JOIN FETCH r.supervisor
        WHERE r.school.id = :schoolId 
        AND r.deletedAt IS NULL
        """)
    Page<ReportSummaryProjection> findBySchoolIdWithProjection(@Param("schoolId") UUID schoolId, Pageable pageable);
    
    /**
     * Find pending reports with projection
     */
    @Query("""
        SELECT r FROM Report r 
        LEFT JOIN FETCH r.school 
        LEFT JOIN FETCH r.supervisor
        WHERE r.status = :status 
        AND r.deletedAt IS NULL
        ORDER BY r.priority DESC, r.createdAt ASC
        """)
    Page<ReportSummaryProjection> findPendingReportsOptimized(@Param("status") ReportStatus status, Pageable pageable);
    
    // ========== Location-based Queries ==========
    
    /**
     * Find reports within radius of location (requires PostGIS)
     */
    @Query(value = """
        SELECT * FROM reports r
        WHERE r.deleted_at IS NULL
        AND r.latitude IS NOT NULL 
        AND r.longitude IS NOT NULL
        AND (
            6371 * acos(
                cos(radians(:latitude)) * cos(radians(r.latitude)) *
                cos(radians(r.longitude) - radians(:longitude)) +
                sin(radians(:latitude)) * sin(radians(r.latitude))
            )
        ) <= :radiusKm
        ORDER BY r.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Report> findNearbyReports(@Param("latitude") Double latitude,
                                   @Param("longitude") Double longitude,
                                   @Param("radiusKm") Double radiusKm,
                                   @Param("limit") int limit);
    
    // ========== Update Operations ==========
    
    /**
     * Update report status
     */
    @Modifying
    @Query("UPDATE Report r SET r.status = :status WHERE r.id = :reportId")
    void updateStatus(@Param("reportId") UUID reportId, @Param("status") ReportStatus status);
    
    /**
     * Assign report to user
     */
    @Modifying
    @Query("UPDATE Report r SET r.assignedTo.id = :userId WHERE r.id = :reportId")
    void assignToUser(@Param("reportId") UUID reportId, @Param("userId") UUID userId);
    
    /**
     * Update scheduled date
     */
    @Modifying
    @Query("UPDATE Report r SET r.scheduledDate = :date WHERE r.id = :reportId")
    void updateScheduledDate(@Param("reportId") UUID reportId, @Param("date") LocalDate date);
    
    /**
     * Complete report
     */
    @Modifying
    @Query("""
        UPDATE Report r 
        SET r.status = :status, 
            r.completedDate = :completedDate,
            r.actualCost = :actualCost
        WHERE r.id = :reportId
        """)
    void completeReport(@Param("reportId") UUID reportId, 
                       @Param("completedDate") LocalDate completedDate,
                       @Param("actualCost") BigDecimal actualCost,
                       @Param("status") ReportStatus status);
    
    // ========== Statistics Queries ==========
    
    /**
     * Count reports by status
     */
    @Query("SELECT r.status, COUNT(r) FROM Report r WHERE r.deletedAt IS NULL GROUP BY r.status")
    List<Object[]> countByStatus();
    
    /**
     * Count reports by priority
     */
    @Query("SELECT r.priority, COUNT(r) FROM Report r WHERE r.deletedAt IS NULL GROUP BY r.priority")
    List<Object[]> countByPriority();
    
    /**
     * Get report statistics for dashboard
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_reports,
            COUNT(CASE WHEN status = 'DRAFT' THEN 1 END) as draft_count,
            COUNT(CASE WHEN status = 'SUBMITTED' THEN 1 END) as submitted_count,
            COUNT(CASE WHEN status = 'IN_REVIEW' THEN 1 END) as in_review_count,
            COUNT(CASE WHEN status = 'APPROVED' THEN 1 END) as approved_count,
            COUNT(CASE WHEN status = 'IN_PROGRESS' THEN 1 END) as in_progress_count,
            COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_count,
            COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as cancelled_count,
            COUNT(CASE WHEN is_overdue = true THEN 1 END) as overdue_count,
            AVG(age_days) as avg_age_days,
            AVG(efficiency_score) as avg_efficiency
        FROM reports 
        WHERE deleted_at IS NULL
        """, nativeQuery = true)
    Object getReportStatistics();
    
    /**
     * Get completion rate by month
     */
    @Query(value = """
        SELECT 
            DATE_TRUNC('month', completed_date) as month,
            COUNT(*) as total_completed,
            AVG(EXTRACT(DAY FROM (completed_date - reported_date))) as avg_completion_days
        FROM reports 
        WHERE status = 'COMPLETED' 
        AND deleted_at IS NULL 
        AND completed_date >= :startDate
        GROUP BY DATE_TRUNC('month', completed_date)
        ORDER BY month
        """, nativeQuery = true)
    List<Object[]> getCompletionRateByMonth(@Param("startDate") LocalDate startDate);
    
    /**
     * Get top performing supervisors
     */
    @Query(value = """
        SELECT 
            u.id,
            u.full_name,
            COUNT(r.id) as reports_created,
            AVG(r.efficiency_score) as avg_efficiency,
            COUNT(CASE WHEN r.status = 'COMPLETED' THEN 1 END) as completed_count
        FROM reports r 
        JOIN users u ON r.supervisor_id = u.id
        WHERE r.deleted_at IS NULL 
        AND r.created_at >= :startDate
        GROUP BY u.id, u.full_name
        ORDER BY avg_efficiency DESC NULLS LAST
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> getTopPerformingSupervisors(@Param("startDate") LocalDate startDate, 
                                               @Param("limit") int limit);
}