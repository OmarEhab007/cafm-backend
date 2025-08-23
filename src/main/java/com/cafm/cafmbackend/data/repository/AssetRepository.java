package com.cafm.cafmbackend.data.repository;

import com.cafm.cafmbackend.data.entity.Asset;
import com.cafm.cafmbackend.data.enums.AssetStatus;
import com.cafm.cafmbackend.data.enums.AssetCondition;
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
 * Repository for Asset entity with tenant-aware queries.
 */
@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID>, JpaSpecificationExecutor<Asset> {
    
    // ========== Basic Queries ==========
    
    Optional<Asset> findByIdAndCompany_Id(UUID id, UUID companyId);
    
    Optional<Asset> findByAssetCodeAndCompany_Id(String assetCode, UUID companyId);
    
    Optional<Asset> findBySerialNumberAndCompany_Id(String serialNumber, UUID companyId);
    
    boolean existsByAssetCodeAndCompany_Id(String assetCode, UUID companyId);
    
    boolean existsBySerialNumberAndCompany_Id(String serialNumber, UUID companyId);
    
    Page<Asset> findByCompany_Id(UUID companyId, Pageable pageable);
    
    // ========== Status Queries ==========
    
    List<Asset> findByCompany_IdAndStatus(UUID companyId, AssetStatus status);
    
    List<Asset> findByCompany_IdAndCondition(UUID companyId, AssetCondition condition);
    
    @Query("SELECT a FROM Asset a WHERE a.company.id = :companyId " +
           "AND a.status IN :statuses")
    Page<Asset> findByCompanyAndStatuses(@Param("companyId") UUID companyId,
                                        @Param("statuses") List<AssetStatus> statuses,
                                        Pageable pageable);
    
    @Query("SELECT COUNT(a) FROM Asset a WHERE a.company.id = :companyId " +
           "AND a.status = :status")
    long countByCompanyAndStatus(@Param("companyId") UUID companyId, 
                                @Param("status") AssetStatus status);
    
    // ========== Category Queries ==========
    
    List<Asset> findByCategoryId(UUID categoryId);
    
    List<Asset> findByCategoryIdAndStatus(UUID categoryId, AssetStatus status);
    
    @Query("SELECT a FROM Asset a WHERE a.category.id IN :categoryIds")
    List<Asset> findByCategoryIds(@Param("categoryIds") List<UUID> categoryIds);
    
    // ========== Location Queries ==========
    
    // Location is a String field, not a reference to another entity
    // List<Asset> findByCurrentLocationId(UUID locationId);
    
    List<Asset> findByAssignedToId(UUID userId);
    
    @Query("SELECT a FROM Asset a WHERE a.company.id = :companyId " +
           "AND a.assignedTo.id = :userId AND a.status = 'IN_USE'")
    List<Asset> findActiveAssignmentsByUser(@Param("companyId") UUID companyId,
                                           @Param("userId") UUID userId);
    
    // Location is a String field, not a reference to another entity
    // @Query("SELECT a FROM Asset a WHERE a.currentLocation.id = :locationId " +
    //        "AND a.status IN ('AVAILABLE', 'IN_USE')")
    // List<Asset> findActiveAssetsByLocation(@Param("locationId") UUID locationId);
    
    // ========== Warranty Queries ==========
    
    @Query("SELECT a FROM Asset a WHERE a.company.id = :companyId " +
           "AND a.warrantyEndDate >= :currentDate AND a.warrantyEndDate <= :futureDate")
    List<Asset> findExpiringWarranties(@Param("companyId") UUID companyId,
                                      @Param("currentDate") LocalDate currentDate,
                                      @Param("futureDate") LocalDate futureDate);
    
    @Query("SELECT a FROM Asset a WHERE a.company.id = :companyId " +
           "AND a.warrantyEndDate < :currentDate AND a.status != 'DISPOSED'")
    List<Asset> findExpiredWarranties(@Param("companyId") UUID companyId,
                                     @Param("currentDate") LocalDate currentDate);
    
    // ========== Maintenance Queries ==========
    
    @Query("SELECT a FROM Asset a WHERE a.company.id = :companyId " +
           "AND a.nextMaintenanceDate <= :targetDate " +
           "AND a.status IN ('IN_USE', 'AVAILABLE')")
    List<Asset> findAssetsDueForMaintenance(@Param("companyId") UUID companyId,
                                           @Param("targetDate") LocalDate targetDate);
    
    @Query("SELECT a FROM Asset a WHERE a.company.id = :companyId " +
           "AND a.lastMaintenanceDate < :thresholdDate " +
           "AND a.status = 'IN_USE'")
    List<Asset> findOverdueMaintenance(@Param("companyId") UUID companyId,
                                      @Param("thresholdDate") LocalDate thresholdDate);
    
    // ========== Depreciation Queries ==========
    
    @Query("SELECT a FROM Asset a WHERE a.company.id = :companyId " +
           "AND a.disposalDate IS NULL AND a.purchaseCost IS NOT NULL " +
           "AND a.purchaseDate <= :asOfDate")
    List<Asset> findDepreciableAssets(@Param("companyId") UUID companyId,
                                     @Param("asOfDate") LocalDate asOfDate);
    
    @Query("SELECT SUM(a.purchaseCost) FROM Asset a WHERE a.company.id = :companyId " +
           "AND a.status != 'DISPOSED'")
    BigDecimal getTotalAssetValue(@Param("companyId") UUID companyId);
    
    @Query("SELECT SUM(a.currentValue) FROM Asset a WHERE a.company.id = :companyId " +
           "AND a.status != 'DISPOSED' AND a.currentValue IS NOT NULL")
    BigDecimal getTotalCurrentValue(@Param("companyId") UUID companyId);
    
    // ========== Life Cycle Queries ==========
    
    // Life expectancy field does not exist in Asset entity
    // @Query("SELECT a FROM Asset a WHERE a.company.id = :companyId " +
    //        "AND a.lifeExpectancyYears IS NOT NULL " +
    //        "AND FUNCTION('AGE', CURRENT_DATE, a.purchaseDate) >= CAST(a.lifeExpectancyYears || ' years' AS INTERVAL)")
    // List<Asset> findAssetsAtEndOfLife(@Param("companyId") UUID companyId);
    
    @Query("SELECT a FROM Asset a WHERE a.company.id = :companyId " +
           "AND a.purchaseDate BETWEEN :startDate AND :endDate")
    List<Asset> findAssetsPurchasedInPeriod(@Param("companyId") UUID companyId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);
    
    @Query("SELECT a FROM Asset a WHERE a.company.id = :companyId " +
           "AND a.disposalDate BETWEEN :startDate AND :endDate")
    List<Asset> findAssetsDisposedInPeriod(@Param("companyId") UUID companyId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
    
    // ========== Cost Analysis ==========
    
    @Query("SELECT a.category.name, SUM(a.purchaseCost), COUNT(a) " +
           "FROM Asset a WHERE a.company.id = :companyId " +
           "GROUP BY a.category.name")
    List<Object[]> getAssetValueByCategory(@Param("companyId") UUID companyId);
    
    @Query("SELECT a.location, SUM(a.purchaseCost), COUNT(a) " +
           "FROM Asset a WHERE a.company.id = :companyId " +
           "AND a.location IS NOT NULL " +
           "GROUP BY a.location")
    List<Object[]> getAssetValueByLocation(@Param("companyId") UUID companyId);
    
    @Query("SELECT AVG(a.purchaseCost) FROM Asset a " +
           "WHERE a.category.id = :categoryId AND a.purchaseCost IS NOT NULL")
    BigDecimal getAverageCostByCategory(@Param("categoryId") UUID categoryId);
    
    // ========== Statistics ==========
    
    @Query("SELECT COUNT(a) FROM Asset a WHERE a.company.id = :companyId")
    long countTotalAssets(@Param("companyId") UUID companyId);
    
    @Query("SELECT a.status, COUNT(a) FROM Asset a WHERE a.company.id = :companyId " +
           "GROUP BY a.status")
    List<Object[]> getAssetCountByStatus(@Param("companyId") UUID companyId);
    
    @Query("SELECT a.condition, COUNT(a) FROM Asset a WHERE a.company.id = :companyId " +
           "GROUP BY a.condition")
    List<Object[]> getAssetCountByCondition(@Param("companyId") UUID companyId);
    
    @Query("SELECT YEAR(a.purchaseDate), COUNT(a), SUM(a.purchaseCost) " +
           "FROM Asset a WHERE a.company.id = :companyId " +
           "GROUP BY YEAR(a.purchaseDate) ORDER BY YEAR(a.purchaseDate) DESC")
    List<Object[]> getAssetAcquisitionTrend(@Param("companyId") UUID companyId);
    
    // ========== Updates ==========
    
    @Modifying
    @Query("UPDATE Asset a SET a.status = :newStatus, a.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE a.id = :assetId AND a.company.id = :companyId")
    int updateStatus(@Param("assetId") UUID assetId,
                    @Param("companyId") UUID companyId,
                    @Param("newStatus") AssetStatus newStatus);
    
    @Modifying
    @Query("UPDATE Asset a SET a.assignedTo.id = :userId, a.status = 'IN_USE', " +
           "a.updatedAt = CURRENT_TIMESTAMP WHERE a.id = :assetId")
    int assignToUser(@Param("assetId") UUID assetId, @Param("userId") UUID userId);
    
    @Modifying
    @Query("UPDATE Asset a SET a.location = :location, " +
           "a.updatedAt = CURRENT_TIMESTAMP WHERE a.id = :assetId")
    int updateLocation(@Param("assetId") UUID assetId, @Param("location") String location);
    
    // ========== School Queries ==========
    
    /**
     * Count assets by school ID
     */
    long countBySchoolIdAndIsActiveTrue(UUID schoolId);
    
    /**
     * Count assets needing maintenance by school ID
     */
    @Query("SELECT COUNT(a) FROM Asset a WHERE a.school.id = :schoolId " +
           "AND (a.condition IN :poorConditions OR a.nextMaintenanceDate < CURRENT_DATE) " +
           "AND a.isActive = true")
    long countAssetsNeedingMaintenanceBySchoolId(@Param("schoolId") UUID schoolId, @Param("poorConditions") List<AssetCondition> poorConditions);
    
    // ========== Search ==========
    
    @Query("SELECT a FROM Asset a WHERE a.company.id = :companyId " +
           "AND (LOWER(a.assetCode) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(a.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(a.serialNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(a.manufacturer) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(a.model) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Asset> searchAssets(@Param("companyId") UUID companyId,
                           @Param("searchTerm") String searchTerm,
                           Pageable pageable);
    
    // ========== Barcode ==========
    
    Optional<Asset> findByBarcodeAndCompany_Id(String barcode, UUID companyId);
    
    // ========== Alternative Method Names (For Service Compatibility) ==========
    
    // Alternative naming patterns for compatibility with AssetService
    default boolean existsByAssetCodeAndCompanyId(String assetCode, UUID companyId) {
        return existsByAssetCodeAndCompany_Id(assetCode, companyId);
    }
    
    default boolean existsBySerialNumberAndCompanyId(String serialNumber, UUID companyId) {
        return existsBySerialNumberAndCompany_Id(serialNumber, companyId);
    }
    
    default Optional<Asset> findByAssetCodeAndCompanyId(String assetCode, UUID companyId) {
        return findByAssetCodeAndCompany_Id(assetCode, companyId);
    }
    
    default Optional<Asset> findBySerialNumberAndCompanyId(String serialNumber, UUID companyId) {
        return findBySerialNumberAndCompany_Id(serialNumber, companyId);
    }
    
    default Page<Asset> findByCompanyId(UUID companyId, Pageable pageable) {
        return findByCompany_Id(companyId, pageable);
    }
    
    default List<Asset> findByCompanyIdAndStatus(UUID companyId, AssetStatus status) {
        return findByCompany_IdAndStatus(companyId, status);
    }
    
    default List<Asset> findByCompanyIdAndCondition(UUID companyId, AssetCondition condition) {
        return findByCompany_IdAndCondition(companyId, condition);
    }
}