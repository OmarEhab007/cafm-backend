package com.cafm.cafmbackend.data.repository;

import com.cafm.cafmbackend.data.entity.AssetMaintenance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for AssetMaintenance entity.
 */
@Repository
public interface AssetMaintenanceRepository extends JpaRepository<AssetMaintenance, UUID> {
    
    // ========== Basic Queries ==========
    
    List<AssetMaintenance> findByAssetId(UUID assetId);
    
    List<AssetMaintenance> findByAssetIdOrderByMaintenanceDateDesc(UUID assetId);
    
    Page<AssetMaintenance> findByAssetId(UUID assetId, Pageable pageable);
    
    // ========== Status Queries ==========
    
    // Status field does not exist in AssetMaintenance entity
    // List<AssetMaintenance> findByAssetIdAndStatus(UUID assetId, String status);
    
    // Status field does not exist in AssetMaintenance entity
    // @Query("SELECT am FROM AssetMaintenance am WHERE am.asset.id = :assetId " +
    //        "AND am.status IN :statuses")
    // List<AssetMaintenance> findByAssetAndStatuses(@Param("assetId") UUID assetId,
    //                                               @Param("statuses") List<String> statuses);
    
    // Status field does not exist in AssetMaintenance entity
    // @Query("SELECT am FROM AssetMaintenance am WHERE am.asset.company.id = :companyId " +
    //        "AND am.status = :status")
    // Page<AssetMaintenance> findByCompanyAndStatus(@Param("companyId") UUID companyId,
    //                                               @Param("status") String status,
    //                                               Pageable pageable);
    
    // ========== Scheduled Maintenance ==========
    
    @Query("SELECT am FROM AssetMaintenance am WHERE am.asset.company.id = :companyId " +
           "AND am.maintenanceDate BETWEEN :startDate AND :endDate")
    List<AssetMaintenance> findMaintenanceInPeriod(@Param("companyId") UUID companyId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);
    
    @Query("SELECT am FROM AssetMaintenance am WHERE am.asset.company.id = :companyId " +
           "AND am.nextMaintenanceDate <= :targetDate " +
           "ORDER BY am.nextMaintenanceDate ASC")
    List<AssetMaintenance> findUpcomingMaintenance(@Param("companyId") UUID companyId,
                                                  @Param("targetDate") LocalDate targetDate);
    
    @Query("SELECT am FROM AssetMaintenance am WHERE am.asset.company.id = :companyId " +
           "AND am.nextMaintenanceDate < :currentDate")
    List<AssetMaintenance> findOverdueMaintenance(@Param("companyId") UUID companyId,
                                                 @Param("currentDate") LocalDate currentDate);
    
    // ========== Performed By Queries ==========
    
    List<AssetMaintenance> findByPerformedById(UUID technicianId);
    
    @Query("SELECT am FROM AssetMaintenance am WHERE am.performedBy.id = :technicianId " +
           "AND am.maintenanceDate BETWEEN :startDate AND :endDate")
    List<AssetMaintenance> findByTechnicianInPeriod(@Param("technicianId") UUID technicianId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);
    
    @Query("SELECT am.performedBy.id, COUNT(am), SUM(am.totalCost) " +
           "FROM AssetMaintenance am " +
           "WHERE am.asset.company.id = :companyId " +
           "AND am.maintenanceDate BETWEEN :startDate AND :endDate " +
           "GROUP BY am.performedBy.id")
    List<Object[]> getTechnicianPerformanceSummary(@Param("companyId") UUID companyId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);
    
    // ========== Maintenance Type Queries ==========
    
    List<AssetMaintenance> findByAssetIdAndMaintenanceType(UUID assetId, String maintenanceType);
    
    @Query("SELECT am FROM AssetMaintenance am WHERE am.asset.company.id = :companyId " +
           "AND am.maintenanceType = :type")
    Page<AssetMaintenance> findByCompanyAndType(@Param("companyId") UUID companyId,
                                               @Param("type") String type,
                                               Pageable pageable);
    
    @Query("SELECT am.maintenanceType, COUNT(am), SUM(am.totalCost) " +
           "FROM AssetMaintenance am " +
           "WHERE am.asset.company.id = :companyId " +
           "AND am.maintenanceDate BETWEEN :startDate AND :endDate " +
           "GROUP BY am.maintenanceType")
    List<Object[]> getMaintenanceTypeSummary(@Param("companyId") UUID companyId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);
    
    // ========== Cost Analysis ==========
    
    @Query("SELECT SUM(am.totalCost) FROM AssetMaintenance am " +
           "WHERE am.asset.id = :assetId AND am.totalCost IS NOT NULL")
    BigDecimal getTotalMaintenanceCost(@Param("assetId") UUID assetId);
    
    @Query("SELECT SUM(am.totalCost) FROM AssetMaintenance am " +
           "WHERE am.asset.company.id = :companyId " +
           "AND am.maintenanceDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalCostInPeriod(@Param("companyId") UUID companyId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);
    
    @Query("SELECT AVG(am.totalCost) FROM AssetMaintenance am " +
           "WHERE am.asset.category.id = :categoryId AND am.totalCost IS NOT NULL")
    BigDecimal getAverageCostByCategory(@Param("categoryId") UUID categoryId);
    
    @Query("SELECT am FROM AssetMaintenance am WHERE am.asset.company.id = :companyId " +
           "AND am.totalCost > :threshold ORDER BY am.totalCost DESC")
    List<AssetMaintenance> findHighCostMaintenance(@Param("companyId") UUID companyId,
                                                  @Param("threshold") BigDecimal threshold);
    
    // ========== Work Order Integration ==========
    
    List<AssetMaintenance> findByWorkOrderId(UUID workOrderId);
    
    @Query("SELECT COUNT(am) FROM AssetMaintenance am WHERE am.workOrder.id = :workOrderId")
    long countByWorkOrderId(@Param("workOrderId") UUID workOrderId);
    
    // ========== Next Maintenance ==========
    
    @Query("SELECT am FROM AssetMaintenance am WHERE am.asset.id = :assetId " +
           "AND am.nextMaintenanceDate IS NOT NULL AND am.nextMaintenanceDate <= :targetDate")
    List<AssetMaintenance> findDueForFollowUp(@Param("assetId") UUID assetId,
                                             @Param("targetDate") LocalDate targetDate);
    
    @Query("SELECT DISTINCT am.asset.id FROM AssetMaintenance am " +
           "WHERE am.asset.company.id = :companyId " +
           "AND am.nextMaintenanceDate <= :targetDate")
    List<UUID> findAssetsNeedingMaintenance(@Param("companyId") UUID companyId,
                                           @Param("targetDate") LocalDate targetDate);
    
    // ========== Statistics ==========
    
    // Status field does not exist in AssetMaintenance entity
    // @Query("SELECT COUNT(am) FROM AssetMaintenance am " +
    //        "WHERE am.asset.id = :assetId AND am.status = 'completed'")
    // long countCompletedMaintenanceForAsset(@Param("assetId") UUID assetId);
    
    // Status field does not exist in AssetMaintenance entity
    // @Query("SELECT COUNT(am) FROM AssetMaintenance am " +
    //        "WHERE am.asset.company.id = :companyId AND am.status = :status")
    // long countByCompanyAndStatus(@Param("companyId") UUID companyId,
    //                             @Param("status") String status);
    
    @Query("SELECT DATE(am.maintenanceDate), COUNT(am), SUM(am.totalCost) " +
           "FROM AssetMaintenance am " +
           "WHERE am.asset.company.id = :companyId " +
           "AND am.maintenanceDate BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(am.maintenanceDate)")
    List<Object[]> getDailyMaintenanceSummary(@Param("companyId") UUID companyId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);
    
    // Status and actualDate fields do not exist
    // @Query("SELECT AVG(FUNCTION('DAY', am.actualDate - am.scheduledDate)) " +
    //        "FROM AssetMaintenance am " +
    //        "WHERE am.asset.company.id = :companyId " +
    //        "AND am.status = 'completed' AND am.actualDate IS NOT NULL")
    // Double getAverageDelayDays(@Param("companyId") UUID companyId);
    
    // ========== Updates ==========
    
    // Status field does not exist in AssetMaintenance entity
    // @Modifying
    // @Query("UPDATE AssetMaintenance am SET am.status = :status, " +
    //        "am.updatedAt = CURRENT_TIMESTAMP WHERE am.id = :maintenanceId")
    // int updateStatus(@Param("maintenanceId") UUID maintenanceId,
    //                 @Param("status") String status);
    
    @Modifying
    @Query("UPDATE AssetMaintenance am SET " +
           "am.maintenanceDate = :maintenanceDate, am.totalCost = :totalCost, " +
           "am.performedBy.id = :technicianId, am.recommendations = :notes, " +
           "am.updatedAt = CURRENT_TIMESTAMP WHERE am.id = :maintenanceId")
    int markAsCompleted(@Param("maintenanceId") UUID maintenanceId,
                       @Param("maintenanceDate") LocalDate maintenanceDate,
                       @Param("totalCost") BigDecimal totalCost,
                       @Param("technicianId") UUID technicianId,
                       @Param("notes") String notes);
    
    // ========== Search ==========
    
    @Query("SELECT am FROM AssetMaintenance am WHERE am.asset.company.id = :companyId " +
           "AND (LOWER(am.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(am.recommendations) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(am.asset.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(am.asset.assetCode) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<AssetMaintenance> searchMaintenance(@Param("companyId") UUID companyId,
                                            @Param("searchTerm") String searchTerm,
                                            Pageable pageable);
    
    // ========== History ==========
    
    @Query("SELECT am FROM AssetMaintenance am WHERE am.asset.id = :assetId " +
           "ORDER BY am.maintenanceDate DESC")
    List<AssetMaintenance> getMaintenanceHistory(@Param("assetId") UUID assetId);
    
    @Query("SELECT am FROM AssetMaintenance am WHERE am.asset.id = :assetId " +
           "AND am.maintenanceDate = (SELECT MAX(am2.maintenanceDate) FROM AssetMaintenance am2 " +
           "WHERE am2.asset.id = :assetId)")
    AssetMaintenance findLastCompletedMaintenance(@Param("assetId") UUID assetId);
}