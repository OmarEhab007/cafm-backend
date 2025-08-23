package com.cafm.cafmbackend.data.repository;

import com.cafm.cafmbackend.data.entity.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for InventoryItem entity with tenant-aware queries.
 */
@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID>, JpaSpecificationExecutor<InventoryItem> {
    
    // ========== Basic Queries ==========
    
    Optional<InventoryItem> findByIdAndCompany_Id(UUID id, UUID companyId);
    
    Optional<InventoryItem> findByItemCodeAndCompany_Id(String itemCode, UUID companyId);
    
    boolean existsByItemCodeAndCompany_Id(String itemCode, UUID companyId);
    
    @EntityGraph("InventoryItem.withCompanyAndCategory")
    Page<InventoryItem> findByCompany_IdAndIsActiveTrue(UUID companyId, Pageable pageable);
    
    // Method to get all items for statistics (without pagination)
    @Query("SELECT ii FROM InventoryItem ii JOIN FETCH ii.company LEFT JOIN FETCH ii.category WHERE ii.company.id = :companyId")
    List<InventoryItem> findAllByCompanyIdWithFetch(@Param("companyId") UUID companyId);
    
    // Compatibility methods
    default Optional<InventoryItem> findByIdAndCompanyId(UUID id, UUID companyId) {
        return findByIdAndCompany_Id(id, companyId);
    }
    
    default Optional<InventoryItem> findByItemCodeAndCompanyId(String itemCode, UUID companyId) {
        return findByItemCodeAndCompany_Id(itemCode, companyId);
    }
    
    default boolean existsByItemCodeAndCompanyId(String itemCode, UUID companyId) {
        return existsByItemCodeAndCompany_Id(itemCode, companyId);
    }
    
    default Page<InventoryItem> findByCompanyIdAndIsActiveTrue(UUID companyId, Pageable pageable) {
        return findByCompany_IdAndIsActiveTrue(companyId, pageable);
    }
    
    // ========== Category Queries ==========
    
    List<InventoryItem> findByCategoryId(UUID categoryId);
    
    List<InventoryItem> findByCategoryIdAndIsActiveTrue(UUID categoryId);
    
    @Query("SELECT ii FROM InventoryItem ii WHERE ii.category.id IN :categoryIds AND ii.isActive = true")
    List<InventoryItem> findByCategoryIds(@Param("categoryIds") List<UUID> categoryIds);
    
    // ========== Stock Level Queries ==========
    
    @Query("SELECT ii FROM InventoryItem ii WHERE ii.company.id = :companyId " +
           "AND ii.currentStock <= ii.minimumStock AND ii.isActive = true")
    List<InventoryItem> findLowStockItems(@Param("companyId") UUID companyId);
    
    @Query("SELECT ii FROM InventoryItem ii WHERE ii.company.id = :companyId " +
           "AND ii.currentStock = 0 AND ii.isActive = true")
    List<InventoryItem> findOutOfStockItems(@Param("companyId") UUID companyId);
    
    @Query("SELECT ii FROM InventoryItem ii WHERE ii.company.id = :companyId " +
           "AND ii.currentStock > ii.maximumStock AND ii.maximumStock IS NOT NULL AND ii.isActive = true")
    List<InventoryItem> findOverstockedItems(@Param("companyId") UUID companyId);
    
    @Query("SELECT ii FROM InventoryItem ii WHERE ii.company.id = :companyId " +
           "AND ii.currentStock <= ii.reorderLevel AND ii.reorderLevel IS NOT NULL AND ii.isActive = true")
    List<InventoryItem> findItemsNeedingReorder(@Param("companyId") UUID companyId);
    
    // ========== Stock Updates ==========
    
    @Modifying
    @Query("UPDATE InventoryItem ii SET ii.currentStock = ii.currentStock + :quantity " +
           "WHERE ii.id = :itemId AND ii.company.id = :companyId")
    int increaseStock(@Param("itemId") UUID itemId, @Param("companyId") UUID companyId, 
                     @Param("quantity") BigDecimal quantity);
    
    @Modifying
    @Query("UPDATE InventoryItem ii SET ii.currentStock = ii.currentStock - :quantity " +
           "WHERE ii.id = :itemId AND ii.company.id = :companyId AND ii.currentStock >= :quantity")
    int decreaseStock(@Param("itemId") UUID itemId, @Param("companyId") UUID companyId, 
                     @Param("quantity") BigDecimal quantity);
    
    @Modifying
    @Query("UPDATE InventoryItem ii SET ii.currentStock = :newLevel " +
           "WHERE ii.id = :itemId AND ii.company.id = :companyId")
    int adjustStock(@Param("itemId") UUID itemId, @Param("companyId") UUID companyId, 
                   @Param("newLevel") BigDecimal newLevel);
    
    // ========== Cost Queries ==========
    
    @Query("SELECT SUM(ii.currentStock * ii.averageCost) FROM InventoryItem ii " +
           "WHERE ii.company.id = :companyId AND ii.isActive = true")
    BigDecimal getTotalInventoryValue(@Param("companyId") UUID companyId);
    
    @Query("SELECT SUM(ii.currentStock * ii.averageCost) FROM InventoryItem ii " +
           "WHERE ii.category.id = :categoryId AND ii.isActive = true")
    BigDecimal getTotalValueByCategory(@Param("categoryId") UUID categoryId);
    
    @Query("SELECT ii FROM InventoryItem ii JOIN FETCH ii.company LEFT JOIN FETCH ii.category WHERE ii.company.id = :companyId " +
           "ORDER BY (ii.currentStock * ii.averageCost) DESC")
    List<InventoryItem> findHighValueItems(@Param("companyId") UUID companyId, Pageable pageable);
    
    // ========== Location Queries ==========
    
    List<InventoryItem> findByWarehouseLocation(String warehouseLocation);
    
    @Query("SELECT DISTINCT ii.warehouseLocation FROM InventoryItem ii " +
           "WHERE ii.company.id = :companyId AND ii.warehouseLocation IS NOT NULL")
    List<String> findDistinctWarehouseLocations(@Param("companyId") UUID companyId);
    
    @Query("SELECT ii FROM InventoryItem ii WHERE ii.company.id = :companyId " +
           "AND ii.warehouseLocation = :location AND ii.binNumber = :binNumber")
    List<InventoryItem> findByLocationAndBin(@Param("companyId") UUID companyId,
                                            @Param("location") String location,
                                            @Param("binNumber") String binNumber);
    
    // ========== Brand/Model Queries ==========
    
    List<InventoryItem> findByBrand(String brand);
    
    List<InventoryItem> findByBrandAndModel(String brand, String model);
    
    @Query("SELECT DISTINCT ii.brand FROM InventoryItem ii " +
           "WHERE ii.company.id = :companyId AND ii.brand IS NOT NULL ORDER BY ii.brand")
    List<String> findDistinctBrands(@Param("companyId") UUID companyId);
    
    // ========== Statistics ==========
    
    @Query("SELECT COUNT(ii) FROM InventoryItem ii WHERE ii.company.id = :companyId AND ii.isActive = true")
    long countActiveItems(@Param("companyId") UUID companyId);
    
    @Query("SELECT COUNT(ii) FROM InventoryItem ii WHERE ii.company.id = :companyId " +
           "AND ii.currentStock <= ii.minimumStock AND ii.isActive = true")
    long countLowStockItems(@Param("companyId") UUID companyId);
    
    @Query("SELECT AVG(ii.currentStock) FROM InventoryItem ii " +
           "WHERE ii.company.id = :companyId AND ii.isActive = true")
    Double getAverageStockLevel(@Param("companyId") UUID companyId);
    
    // ========== Search ==========
    
    @Query("SELECT ii FROM InventoryItem ii WHERE ii.company.id = :companyId " +
           "AND (LOWER(ii.itemCode) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(ii.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(ii.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND ii.isActive = true")
    Page<InventoryItem> searchItems(@Param("companyId") UUID companyId, 
                                   @Param("searchTerm") String searchTerm, 
                                   Pageable pageable);
    
    // ========== ABC Analysis ==========
    
    @Query(value = "WITH item_values AS (" +
           "SELECT id, (current_stock * average_cost) as total_value " +
           "FROM inventory_items WHERE company_id = :companyId AND is_active = true), " +
           "ranked_items AS (" +
           "SELECT id, total_value, " +
           "SUM(total_value) OVER (ORDER BY total_value DESC) as running_total, " +
           "SUM(total_value) OVER () as grand_total " +
           "FROM item_values) " +
           "SELECT ii.* FROM inventory_items ii " +
           "JOIN ranked_items ri ON ii.id = ri.id " +
           "WHERE ri.running_total <= ri.grand_total * :percentage",
           nativeQuery = true)
    List<InventoryItem> findTopValueItems(@Param("companyId") UUID companyId, 
                                         @Param("percentage") double percentage);
}