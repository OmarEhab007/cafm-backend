package com.cafm.cafmbackend.data.repository;

import com.cafm.cafmbackend.data.entity.AssetCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AssetCategory entity with tenant-aware queries.
 */
@Repository
public interface AssetCategoryRepository extends JpaRepository<AssetCategory, UUID> {
    
    // ========== Basic Queries ==========
    
    Optional<AssetCategory> findByIdAndCompany_Id(UUID id, UUID companyId);
    
    Optional<AssetCategory> findByNameAndCompany_Id(String name, UUID companyId);
    
    boolean existsByNameAndCompany_Id(String name, UUID companyId);
    
    Page<AssetCategory> findByCompany_IdAndIsActiveTrue(UUID companyId, Pageable pageable);
    
    List<AssetCategory> findByCompany_IdAndIsActiveTrueOrderByName(UUID companyId);
    
    // ========== Alternative Method Names (For Service Compatibility) ==========
    
    // Alternative naming patterns for compatibility with AssetService
    default Optional<AssetCategory> findByIdAndCompanyId(UUID id, UUID companyId) {
        return findByIdAndCompany_Id(id, companyId);
    }
    
    default Optional<AssetCategory> findByNameAndCompanyId(String name, UUID companyId) {
        return findByNameAndCompany_Id(name, companyId);
    }
    
    default boolean existsByNameAndCompanyId(String name, UUID companyId) {
        return existsByNameAndCompany_Id(name, companyId);
    }
    
    default Page<AssetCategory> findByCompanyIdAndIsActiveTrue(UUID companyId, Pageable pageable) {
        return findByCompany_IdAndIsActiveTrue(companyId, pageable);
    }
    
    default List<AssetCategory> findByCompanyIdAndIsActiveTrueOrderByName(UUID companyId) {
        return findByCompany_IdAndIsActiveTrueOrderByName(companyId);
    }
    
    // ========== Parent Category Queries ==========
    // AssetCategory does not have a parent category field
    
    // ========== Depreciation Queries ==========
    
    @Query("SELECT ac FROM AssetCategory ac WHERE ac.company.id = :companyId " +
           "AND ac.depreciationRate IS NOT NULL AND ac.isActive = true")
    List<AssetCategory> findCategoriesWithDepreciation(@Param("companyId") UUID companyId);
    
    @Query("SELECT ac FROM AssetCategory ac WHERE ac.company.id = :companyId " +
           "AND ac.usefulLifeYears IS NOT NULL AND ac.usefulLifeYears <= :years")
    List<AssetCategory> findByMaxUsefulLife(@Param("companyId") UUID companyId,
                                           @Param("years") Integer years);
    
    // ========== Maintenance Queries ==========
    // AssetCategory does not have maintenance interval fields
    
    // ========== Asset Count Queries ==========
    
    @Query("SELECT ac, COUNT(a) FROM AssetCategory ac " +
           "LEFT JOIN Asset a ON a.category.id = ac.id " +
           "WHERE ac.company.id = :companyId " +
           "GROUP BY ac.id ORDER BY COUNT(a) DESC")
    List<Object[]> findCategoriesWithAssetCount(@Param("companyId") UUID companyId);
    
    @Query("SELECT COUNT(a) FROM Asset a WHERE a.category.id = :categoryId")
    long countAssetsInCategory(@Param("categoryId") UUID categoryId);
    
    @Query("SELECT COUNT(a) FROM Asset a WHERE a.category.id = :categoryId " +
           "AND a.status = 'IN_USE'")
    long countActiveAssetsInCategory(@Param("categoryId") UUID categoryId);
    
    // ========== Value Analysis ==========
    
    @Query("SELECT SUM(a.purchaseCost) FROM Asset a " +
           "WHERE a.category.id = :categoryId")
    BigDecimal getTotalAssetValue(@Param("categoryId") UUID categoryId);
    
    @Query("SELECT SUM(a.currentValue) FROM Asset a " +
           "WHERE a.category.id = :categoryId AND a.currentValue IS NOT NULL")
    BigDecimal getTotalCurrentValue(@Param("categoryId") UUID categoryId);
    
    @Query("SELECT ac.name, SUM(a.purchaseCost), SUM(a.currentValue), COUNT(a) " +
           "FROM AssetCategory ac " +
           "LEFT JOIN Asset a ON a.category.id = ac.id " +
           "WHERE ac.company.id = :companyId " +
           "GROUP BY ac.id, ac.name " +
           "HAVING COUNT(a) > 0 " +
           "ORDER BY SUM(a.purchaseCost) DESC")
    List<Object[]> getCategoryValueAnalysis(@Param("companyId") UUID companyId);
    
    // ========== Hierarchy Queries ==========
    // AssetCategory does not have a parent category field
    
    // ========== Insurance Queries ==========
    // AssetCategory does not have insurance or warranty fields
    
    // ========== Statistics ==========
    
    @Query("SELECT COUNT(ac) FROM AssetCategory ac WHERE ac.company.id = :companyId " +
           "AND ac.isActive = true")
    long countActiveCategories(@Param("companyId") UUID companyId);
    
    // Depreciation method field does not exist
    // @Query("SELECT ac.depreciationMethod, COUNT(ac) FROM AssetCategory ac " +
    //        "WHERE ac.company.id = :companyId AND ac.depreciationMethod IS NOT NULL " +
    //        "GROUP BY ac.depreciationMethod")
    // List<Object[]> getDepreciationMethodDistribution(@Param("companyId") UUID companyId);
    
    @Query("SELECT AVG(ac.usefulLifeYears) FROM AssetCategory ac " +
           "WHERE ac.company.id = :companyId AND ac.usefulLifeYears IS NOT NULL")
    Double getAverageUsefulLife(@Param("companyId") UUID companyId);
    
    // ========== Search ==========
    
    @Query("SELECT ac FROM AssetCategory ac WHERE ac.company.id = :companyId " +
           "AND (LOWER(ac.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(ac.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND ac.isActive = true")
    Page<AssetCategory> searchCategories(@Param("companyId") UUID companyId,
                                        @Param("searchTerm") String searchTerm,
                                        Pageable pageable);
    
    // ========== Validation ==========
    
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM Asset a WHERE a.category.id = :categoryId")
    boolean hasAssociatedAssets(@Param("categoryId") UUID categoryId);
    
    @Query("SELECT ac FROM AssetCategory ac WHERE ac.company.id = :companyId " +
           "AND ac.isActive = true AND ac.id != :excludeId " +
           "AND UPPER(ac.name) = UPPER(:name)")
    List<AssetCategory> findDuplicateName(@Param("companyId") UUID companyId,
                                         @Param("name") String name,
                                         @Param("excludeId") UUID excludeId);
}