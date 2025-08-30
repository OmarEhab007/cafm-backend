package com.cafm.cafmbackend.infrastructure.persistence.repository;

import com.cafm.cafmbackend.infrastructure.persistence.entity.InventoryTransaction;
import com.cafm.cafmbackend.shared.enums.InventoryTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for InventoryTransaction entity with tenant-aware queries.
 */
@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {
    
    // ========== Basic Queries ==========
    
    List<InventoryTransaction> findByItemId(UUID itemId);
    
    List<InventoryTransaction> findByItemIdOrderByTransactionDateDesc(UUID itemId);
    
    Page<InventoryTransaction> findByCompany_Id(UUID companyId, Pageable pageable);
    
    // Compatibility method
    default Page<InventoryTransaction> findByCompanyId(UUID companyId, Pageable pageable) {
        return findByCompany_Id(companyId, pageable);
    }
    
    List<InventoryTransaction> findByTransactionNumber(String transactionNumber);
    
    // ========== Transaction Type Queries ==========
    
    List<InventoryTransaction> findByItemIdAndTransactionType(UUID itemId, 
                                                              InventoryTransactionType transactionType);
    
    @Query("SELECT it FROM InventoryTransaction it WHERE it.company.id = :companyId " +
           "AND it.transactionType = :type ORDER BY it.transactionDate DESC")
    Page<InventoryTransaction> findByCompanyAndType(@Param("companyId") UUID companyId,
                                                    @Param("type") InventoryTransactionType type,
                                                    Pageable pageable);
    
    @Query("SELECT it FROM InventoryTransaction it WHERE it.item.id = :itemId " +
           "AND it.transactionType IN ('PURCHASE', 'RETURN_FROM_CUSTOMER')")
    List<InventoryTransaction> findInboundTransactions(@Param("itemId") UUID itemId);
    
    @Query("SELECT it FROM InventoryTransaction it WHERE it.item.id = :itemId " +
           "AND it.transactionType IN ('USAGE', 'DAMAGE', 'LOSS', 'RETURN_TO_SUPPLIER')")
    List<InventoryTransaction> findOutboundTransactions(@Param("itemId") UUID itemId);
    
    // ========== Date Range Queries ==========
    
    @Query("SELECT it FROM InventoryTransaction it WHERE it.item.id = :itemId " +
           "AND it.transactionDate BETWEEN :startDate AND :endDate")
    List<InventoryTransaction> findByItemAndDateRange(@Param("itemId") UUID itemId,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT it FROM InventoryTransaction it WHERE it.company.id = :companyId " +
           "AND it.transactionDate BETWEEN :startDate AND :endDate")
    Page<InventoryTransaction> findByCompanyAndDateRange(@Param("companyId") UUID companyId,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate,
                                                         Pageable pageable);
    
    // ========== User Queries ==========
    
    @Query("SELECT it FROM InventoryTransaction it WHERE it.createdByUser.id = :userId " +
           "ORDER BY it.transactionDate DESC")
    List<InventoryTransaction> findByCreatedByUser(@Param("userId") UUID userId);
    
    @Query("SELECT it FROM InventoryTransaction it WHERE it.createdByUser.id = :userId " +
           "AND it.transactionDate BETWEEN :startDate AND :endDate")
    List<InventoryTransaction> findByUserInPeriod(@Param("userId") UUID userId,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);
    
    // ========== Work Order Queries ==========
    
    List<InventoryTransaction> findByWorkOrderId(UUID workOrderId);
    
    @Query("SELECT it FROM InventoryTransaction it WHERE it.workOrder.id = :workOrderId " +
           "AND it.transactionType = 'USAGE'")
    List<InventoryTransaction> findUsageByWorkOrder(@Param("workOrderId") UUID workOrderId);
    
    @Query("SELECT SUM(it.totalCost) FROM InventoryTransaction it " +
           "WHERE it.workOrder.id = :workOrderId AND it.transactionType = 'USAGE'")
    BigDecimal getTotalCostByWorkOrder(@Param("workOrderId") UUID workOrderId);
    
    // ========== Reference Queries ==========
    
    List<InventoryTransaction> findByReferenceType(String referenceType);
    
    List<InventoryTransaction> findByReferenceId(UUID referenceId);
    
    @Query("SELECT it FROM InventoryTransaction it WHERE it.referenceType = :refType " +
           "AND it.referenceId = :refId")
    List<InventoryTransaction> findByReference(@Param("refType") String referenceType,
                                              @Param("refId") UUID referenceId);
    
    // ========== Stock Level Queries ==========
    
    @Query("SELECT it FROM InventoryTransaction it WHERE it.item.id = :itemId " +
           "AND it.stockAfter <= :threshold ORDER BY it.transactionDate DESC")
    List<InventoryTransaction> findLowStockTransactions(@Param("itemId") UUID itemId,
                                                        @Param("threshold") BigDecimal threshold);
    
    @Query("SELECT it.item.id, SUM(it.quantity) FROM InventoryTransaction it " +
           "WHERE it.company.id = :companyId AND it.transactionType = :type " +
           "AND it.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY it.item.id")
    List<Object[]> getQuantitySummaryByType(@Param("companyId") UUID companyId,
                                           @Param("type") InventoryTransactionType type,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
    
    // ========== Cost Analysis ==========
    
    @Query("SELECT SUM(it.totalCost) FROM InventoryTransaction it " +
           "WHERE it.item.id = :itemId AND it.transactionType = 'PURCHASE'")
    BigDecimal getTotalPurchaseCost(@Param("itemId") UUID itemId);
    
    @Query("SELECT AVG(it.unitCost) FROM InventoryTransaction it " +
           "WHERE it.item.id = :itemId AND it.transactionType = 'PURCHASE' " +
           "AND it.transactionDate >= :sinceDate")
    BigDecimal getAveragePurchasePrice(@Param("itemId") UUID itemId,
                                       @Param("sinceDate") LocalDateTime sinceDate);
    
    @Query("SELECT it.transactionType, SUM(it.totalCost) FROM InventoryTransaction it " +
           "WHERE it.company.id = :companyId " +
           "AND it.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY it.transactionType")
    List<Object[]> getCostByTransactionType(@Param("companyId") UUID companyId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
    
    // ========== Statistics ==========
    
    @Query("SELECT COUNT(it) FROM InventoryTransaction it " +
           "WHERE it.company.id = :companyId AND it.transactionType = :type")
    long countByCompanyAndType(@Param("companyId") UUID companyId,
                               @Param("type") InventoryTransactionType type);
    
    @Query("SELECT COUNT(DISTINCT it.item.id) FROM InventoryTransaction it " +
           "WHERE it.company.id = :companyId " +
           "AND it.transactionDate BETWEEN :startDate AND :endDate")
    long countActiveItems(@Param("companyId") UUID companyId,
                         @Param("startDate") LocalDateTime startDate,
                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT DATE(it.transactionDate), COUNT(it), SUM(it.quantity) " +
           "FROM InventoryTransaction it " +
           "WHERE it.company.id = :companyId " +
           "AND it.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(it.transactionDate)")
    List<Object[]> getDailyTransactionSummary(@Param("companyId") UUID companyId,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);
    
    // ========== Search ==========
    
    @Query("SELECT it FROM InventoryTransaction it WHERE it.company.id = :companyId " +
           "AND (LOWER(it.transactionNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(it.notes) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(it.item.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<InventoryTransaction> searchTransactions(@Param("companyId") UUID companyId,
                                                 @Param("searchTerm") String searchTerm,
                                                 Pageable pageable);
    
    // ========== Audit Trail ==========
    
    @Query("SELECT it FROM InventoryTransaction it WHERE it.item.id = :itemId " +
           "ORDER BY it.transactionDate DESC, it.createdAt DESC")
    List<InventoryTransaction> getItemAuditTrail(@Param("itemId") UUID itemId);
    
    @Query("SELECT it FROM InventoryTransaction it " +
           "WHERE it.stockBefore != it.stockAfter " +
           "AND ABS(it.stockBefore - it.stockAfter) != it.quantity " +
           "AND it.company.id = :companyId")
    List<InventoryTransaction> findDiscrepancies(@Param("companyId") UUID companyId);
}