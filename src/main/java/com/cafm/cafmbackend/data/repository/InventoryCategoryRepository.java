package com.cafm.cafmbackend.data.repository;

import com.cafm.cafmbackend.data.entity.InventoryCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for InventoryCategory entity with tenant-aware queries.
 */
@Repository
public interface InventoryCategoryRepository extends JpaRepository<InventoryCategory, UUID> {
    
    // ========== Basic Queries ==========
    
    Optional<InventoryCategory> findByIdAndCompanyId(UUID id, UUID companyId);
    
    Optional<InventoryCategory> findByNameAndCompanyId(String name, UUID companyId);
    
    boolean existsByNameAndCompanyId(String name, UUID companyId);
    
    Page<InventoryCategory> findByCompanyIdAndIsActiveTrue(UUID companyId, Pageable pageable);
    
    List<InventoryCategory> findByCompanyIdAndIsActiveTrueOrderByName(UUID companyId);
    
    // ========== Parent Category Queries ==========
    
    List<InventoryCategory> findByParentCategoryId(UUID parentCategoryId);
    
    List<InventoryCategory> findByParentCategoryIdAndIsActiveTrue(UUID parentCategoryId);
    
    @Query("SELECT ic FROM InventoryCategory ic WHERE ic.company.id = :companyId " +
           "AND ic.parentCategory IS NULL AND ic.isActive = true")
    List<InventoryCategory> findRootCategories(@Param("companyId") UUID companyId);
    
    @Query("SELECT ic FROM InventoryCategory ic WHERE ic.parentCategory.id = :parentId " +
           "AND ic.isActive = true ORDER BY ic.name")
    List<InventoryCategory> findChildCategories(@Param("parentId") UUID parentId);
    
    // ========== Hierarchy Queries ==========
    
    @Query(value = "WITH RECURSIVE category_tree AS (" +
           "SELECT id, name, parent_category_id, 0 as level FROM inventory_categories " +
           "WHERE id = :categoryId " +
           "UNION ALL " +
           "SELECT c.id, c.name, c.parent_category_id, ct.level + 1 " +
           "FROM inventory_categories c " +
           "JOIN category_tree ct ON c.parent_category_id = ct.id) " +
           "SELECT * FROM category_tree ORDER BY level",
           nativeQuery = true)
    List<Object[]> getCategoryHierarchy(@Param("categoryId") UUID categoryId);
    
    @Query("SELECT COUNT(ic) FROM InventoryCategory ic WHERE ic.parentCategory.id = :categoryId")
    long countChildCategories(@Param("categoryId") UUID categoryId);
    
    // ========== Stock Tracking Queries ==========
    
    @Query("SELECT DISTINCT ic FROM InventoryCategory ic " +
           "JOIN ic.items ii WHERE ic.company.id = :companyId " +
           "AND ii.minimumStock IS NOT NULL AND ic.isActive = true")
    List<InventoryCategory> findCategoriesWithMinimumStock(@Param("companyId") UUID companyId);
    
    // ========== Statistics ==========
    
    @Query("SELECT COUNT(ic) FROM InventoryCategory ic WHERE ic.company.id = :companyId " +
           "AND ic.isActive = true")
    long countActiveCategories(@Param("companyId") UUID companyId);
    
    @Query("SELECT ic.parentCategory.name, COUNT(ic) FROM InventoryCategory ic " +
           "WHERE ic.company.id = :companyId AND ic.parentCategory IS NOT NULL " +
           "GROUP BY ic.parentCategory.name")
    List<Object[]> getCategoryDistribution(@Param("companyId") UUID companyId);
    
    @Query("SELECT ic FROM InventoryCategory ic " +
           "LEFT JOIN InventoryItem ii ON ii.category.id = ic.id " +
           "WHERE ic.company.id = :companyId " +
           "GROUP BY ic.id " +
           "ORDER BY COUNT(ii) DESC")
    List<InventoryCategory> findMostUsedCategories(@Param("companyId") UUID companyId, Pageable pageable);
    
    // ========== Search ==========
    
    @Query("SELECT ic FROM InventoryCategory ic WHERE ic.company.id = :companyId " +
           "AND (LOWER(ic.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(ic.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND ic.isActive = true")
    Page<InventoryCategory> searchCategories(@Param("companyId") UUID companyId, 
                                            @Param("searchTerm") String searchTerm, 
                                            Pageable pageable);
    
    // ========== Validation ==========
    
    @Query("SELECT CASE WHEN COUNT(ii) > 0 THEN true ELSE false END " +
           "FROM InventoryItem ii WHERE ii.category.id = :categoryId")
    boolean hasAssociatedItems(@Param("categoryId") UUID categoryId);
    
}