package com.cafm.cafmbackend.infrastructure.persistence.repository;

import com.cafm.cafmbackend.infrastructure.persistence.entity.WorkOrder;
import com.cafm.cafmbackend.shared.enums.WorkOrderStatus;
import com.cafm.cafmbackend.shared.enums.WorkOrderPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WorkOrder entity with tenant-aware queries.
 */
@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID>, JpaSpecificationExecutor<WorkOrder> {
    
    // ========== Basic Queries ==========
    
    Optional<WorkOrder> findByIdAndCompany_Id(UUID id, UUID companyId);
    
    Optional<WorkOrder> findByWorkOrderNumberAndCompany_Id(String workOrderNumber, UUID companyId);
    
    boolean existsByWorkOrderNumberAndCompany_Id(String workOrderNumber, UUID companyId);
    
    // ========== Company-scoped Queries ==========
    
    Page<WorkOrder> findByCompany_IdAndDeletedAtIsNull(UUID companyId, Pageable pageable);
    
    List<WorkOrder> findByCompany_IdAndStatus(UUID companyId, WorkOrderStatus status);
    
    List<WorkOrder> findByCompany_IdAndPriority(UUID companyId, WorkOrderPriority priority);
    
    // ========== Assignment Queries ==========
    
    /**
     * Find work orders by assigned user and date range
     */
    List<WorkOrder> findByAssignedToIdAndCreatedAtBetween(UUID assignedToId, LocalDateTime startDate, LocalDateTime endDate);
    
    Page<WorkOrder> findByAssignedToIdAndDeletedAtIsNull(UUID assignedToId, Pageable pageable);
    
    @Query("SELECT wo FROM WorkOrder wo WHERE wo.assignedTo.id = :userId AND wo.status IN :statuses AND wo.deletedAt IS NULL")
    Page<WorkOrder> findByAssignedToAndStatuses(@Param("userId") UUID userId, 
                                                @Param("statuses") List<WorkOrderStatus> statuses, 
                                                Pageable pageable);
    
    @Query("SELECT wo FROM WorkOrder wo WHERE wo.assignedBy.id = :userId AND wo.company.id = :companyId AND wo.deletedAt IS NULL")
    List<WorkOrder> findByAssignedByAndCompany_Id(@Param("userId") UUID userId, @Param("companyId") UUID companyId);
    
    @Query("SELECT COUNT(wo) FROM WorkOrder wo WHERE wo.assignedTo.id = :userId AND wo.status = :status")
    long countByAssignedToAndStatus(@Param("userId") UUID userId, @Param("status") WorkOrderStatus status);
    
    // ========== School Queries ==========
    
    Page<WorkOrder> findBySchoolIdAndDeletedAtIsNull(UUID schoolId, Pageable pageable);
    
    @Query("SELECT wo FROM WorkOrder wo WHERE wo.school.id = :schoolId AND wo.status IN :statuses")
    List<WorkOrder> findBySchoolIdAndStatuses(@Param("schoolId") UUID schoolId, 
                                             @Param("statuses") List<WorkOrderStatus> statuses);
    
    // ========== Location-based Queries ==========
    
    /**
     * Find work orders within radius of location
     */
    @Query(value = """
        SELECT * FROM work_orders wo
        WHERE wo.deleted_at IS NULL
        AND wo.latitude IS NOT NULL 
        AND wo.longitude IS NOT NULL
        AND (
            6371 * acos(
                cos(radians(:latitude)) * cos(radians(wo.latitude)) *
                cos(radians(wo.longitude) - radians(:longitude)) +
                sin(radians(:latitude)) * sin(radians(wo.latitude))
            )
        ) <= :radiusKm
        ORDER BY wo.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<WorkOrder> findNearbyWorkOrders(@Param("latitude") Double latitude,
                                         @Param("longitude") Double longitude,
                                         @Param("radiusKm") Double radiusKm,
                                         @Param("limit") int limit);
    
    // ========== Date-based Queries ==========
    
    @Query("SELECT wo FROM WorkOrder wo WHERE wo.company.id = :companyId " +
           "AND wo.scheduledStart BETWEEN :startDate AND :endDate AND wo.deletedAt IS NULL")
    List<WorkOrder> findByCompany_IdAndScheduledDateRange(@Param("companyId") UUID companyId,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT wo FROM WorkOrder wo WHERE wo.company.id = :companyId " +
           "AND wo.scheduledEnd < :currentDate AND wo.status NOT IN :excludeStatuses " +
           "AND wo.deletedAt IS NULL")
    List<WorkOrder> findOverdueWorkOrders(@Param("companyId") UUID companyId, 
                                         @Param("currentDate") LocalDateTime currentDate,
                                         @Param("excludeStatuses") List<WorkOrderStatus> excludeStatuses);
    
    @Query("SELECT wo FROM WorkOrder wo WHERE wo.company.id = :companyId " +
           "AND wo.actualEnd BETWEEN :startDate AND :endDate")
    List<WorkOrder> findCompletedInDateRange(@Param("companyId") UUID companyId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
    
    // ========== Status Updates ==========
    
    @Modifying
    @Query("UPDATE WorkOrder wo SET wo.status = :newStatus, wo.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE wo.id = :workOrderId AND wo.company.id = :companyId")
    int updateStatus(@Param("workOrderId") UUID workOrderId, 
                    @Param("companyId") UUID companyId,
                    @Param("newStatus") WorkOrderStatus newStatus);
    
    @Modifying
    @Query("UPDATE WorkOrder wo SET wo.completionPercentage = :percentage, wo.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE wo.id = :workOrderId")
    int updateCompletionPercentage(@Param("workOrderId") UUID workOrderId, 
                                  @Param("percentage") Integer percentage);
    
    // ========== Statistics Queries ==========
    
    @Query("SELECT COUNT(wo) FROM WorkOrder wo WHERE wo.company.id = :companyId AND wo.status = :status")
    long countByCompany_IdAndStatus(@Param("companyId") UUID companyId, @Param("status") WorkOrderStatus status);
    
    @Query("SELECT COUNT(wo) FROM WorkOrder wo WHERE wo.company.id = :companyId " +
           "AND wo.createdAt >= :startDate AND wo.deletedAt IS NULL")
    long countRecentWorkOrders(@Param("companyId") UUID companyId, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT AVG(wo.completionPercentage) FROM WorkOrder wo WHERE wo.company.id = :companyId " +
           "AND wo.status IN :includeStatuses")
    Double getAverageCompletionRate(@Param("companyId") UUID companyId, @Param("includeStatuses") List<WorkOrderStatus> includeStatuses);
    
    @Query("SELECT SUM(wo.totalCost) FROM WorkOrder wo WHERE wo.company.id = :companyId " +
           "AND wo.status = :status AND wo.actualEnd BETWEEN :startDate AND :endDate")
    BigDecimal getTotalCostInPeriod(@Param("companyId") UUID companyId,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   @Param("status") WorkOrderStatus status);
    
    // ========== Priority Queries ==========
    
    @Query("SELECT wo FROM WorkOrder wo WHERE wo.company.id = :companyId " +
           "AND wo.priority IN :highPriorities AND wo.status NOT IN :excludeStatuses " +
           "ORDER BY wo.priority ASC, wo.scheduledStart ASC")
    List<WorkOrder> findHighPriorityPending(@Param("companyId") UUID companyId,
                                           @Param("highPriorities") List<WorkOrderPriority> highPriorities,
                                           @Param("excludeStatuses") List<WorkOrderStatus> excludeStatuses);
    
    // ========== Report Integration ==========
    
    List<WorkOrder> findByReportId(UUID reportId);
    
    @Query("SELECT COUNT(wo) FROM WorkOrder wo WHERE wo.report.id = :reportId")
    long countByReportId(@Param("reportId") UUID reportId);
    
    // ========== Verification Queries ==========
    
    @Query("SELECT wo FROM WorkOrder wo WHERE wo.company.id = :companyId " +
           "AND wo.status = :status AND wo.verifiedBy IS NULL")
    List<WorkOrder> findUnverifiedCompleted(@Param("companyId") UUID companyId, @Param("status") WorkOrderStatus status);
    
    @Query("SELECT wo FROM WorkOrder wo WHERE wo.verifiedBy.id = :userId " +
           "AND wo.verifiedAt BETWEEN :startDate AND :endDate")
    List<WorkOrder> findVerifiedByUserInPeriod(@Param("userId") UUID userId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
    
    // ========== Bulk Operations ==========
    
    @Modifying
    @Query("UPDATE WorkOrder wo SET wo.deletedAt = CURRENT_TIMESTAMP, wo.deletedBy = :userId " +
           "WHERE wo.company.id = :companyId AND wo.status = :status " +
           "AND wo.createdAt < :beforeDate")
    int softDeleteOldCancelledOrders(@Param("companyId") UUID companyId,
                                    @Param("beforeDate") LocalDateTime beforeDate,
                                    @Param("userId") UUID userId,
                                    @Param("status") WorkOrderStatus status);
    
    // ========== Search ==========
    
    @Query("SELECT wo FROM WorkOrder wo WHERE wo.company.id = :companyId " +
           "AND (LOWER(wo.workOrderNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(wo.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(wo.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND wo.deletedAt IS NULL")
    Page<WorkOrder> searchWorkOrders(@Param("companyId") UUID companyId, 
                                    @Param("searchTerm") String searchTerm, 
                                    Pageable pageable);
    
    // ========== Alternative Method Names (For Service Compatibility) ==========
    
    // Alternative naming patterns for compatibility with services
    default Optional<WorkOrder> findByIdAndCompanyId(UUID id, UUID companyId) {
        return findByIdAndCompany_Id(id, companyId);
    }
    
    default Optional<WorkOrder> findByWorkOrderNumberAndCompanyId(String workOrderNumber, UUID companyId) {
        return findByWorkOrderNumberAndCompany_Id(workOrderNumber, companyId);
    }
    
    default boolean existsByWorkOrderNumberAndCompanyId(String workOrderNumber, UUID companyId) {
        return existsByWorkOrderNumberAndCompany_Id(workOrderNumber, companyId);
    }
    
    default Page<WorkOrder> findByCompanyIdAndDeletedAtIsNull(UUID companyId, Pageable pageable) {
        return findByCompany_IdAndDeletedAtIsNull(companyId, pageable);
    }
    
    default List<WorkOrder> findByCompanyIdAndStatus(UUID companyId, WorkOrderStatus status) {
        return findByCompany_IdAndStatus(companyId, status);
    }
    
    default List<WorkOrder> findByCompanyIdAndPriority(UUID companyId, WorkOrderPriority priority) {
        return findByCompany_IdAndPriority(companyId, priority);
    }
    
    default List<WorkOrder> findByAssignedByAndCompanyId(UUID userId, UUID companyId) {
        return findByAssignedByAndCompany_Id(userId, companyId);
    }
    
    default List<WorkOrder> findByCompanyIdAndScheduledDateRange(UUID companyId, LocalDateTime startDate, LocalDateTime endDate) {
        return findByCompany_IdAndScheduledDateRange(companyId, startDate, endDate);
    }
    
    default long countByCompanyIdAndStatus(UUID companyId, WorkOrderStatus status) {
        return countByCompany_IdAndStatus(companyId, status);
    }
}