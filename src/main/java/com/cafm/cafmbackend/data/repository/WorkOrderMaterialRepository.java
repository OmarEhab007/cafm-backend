package com.cafm.cafmbackend.data.repository;

import com.cafm.cafmbackend.data.entity.WorkOrderMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repository for WorkOrderMaterial entity.
 */
@Repository
public interface WorkOrderMaterialRepository extends JpaRepository<WorkOrderMaterial, UUID> {
    
    // ========== Basic Queries ==========
    
    List<WorkOrderMaterial> findByWorkOrderId(UUID workOrderId);
    
    List<WorkOrderMaterial> findByWorkOrderIdOrderByMaterialName(UUID workOrderId);
    
    List<WorkOrderMaterial> findByMaterialCode(String materialCode);
    
    // ========== Cost Queries ==========
    
    @Query("SELECT SUM(wom.totalCost) FROM WorkOrderMaterial wom WHERE wom.workOrder.id = :workOrderId")
    BigDecimal getTotalMaterialCost(@Param("workOrderId") UUID workOrderId);
    
    @Query("SELECT SUM(wom.quantity) FROM WorkOrderMaterial wom " +
           "WHERE wom.workOrder.id = :workOrderId AND wom.materialCode = :materialCode")
    BigDecimal getTotalQuantityByMaterial(@Param("workOrderId") UUID workOrderId, 
                                         @Param("materialCode") String materialCode);
    
    @Query("SELECT wom FROM WorkOrderMaterial wom WHERE wom.workOrder.id = :workOrderId " +
           "AND wom.totalCost > :minCost ORDER BY wom.totalCost DESC")
    List<WorkOrderMaterial> findHighCostMaterials(@Param("workOrderId") UUID workOrderId, 
                                                 @Param("minCost") BigDecimal minCost);
    
    // ========== Supplier Queries ==========
    
    List<WorkOrderMaterial> findBySupplier(String supplier);
    
    @Query("SELECT DISTINCT wom.supplier FROM WorkOrderMaterial wom " +
           "WHERE wom.workOrder.company.id = :companyId AND wom.supplier IS NOT NULL")
    List<String> findDistinctSuppliersByCompany(@Param("companyId") UUID companyId);
    
    @Query("SELECT SUM(wom.totalCost) FROM WorkOrderMaterial wom " +
           "WHERE wom.supplier = :supplier AND wom.workOrder.company.id = :companyId")
    BigDecimal getTotalCostBySupplier(@Param("supplier") String supplier, 
                                     @Param("companyId") UUID companyId);
    
    // ========== Material Analysis ==========
    
    @Query("SELECT wom.materialName, SUM(wom.quantity) as totalQty, SUM(wom.totalCost) as totalCost " +
           "FROM WorkOrderMaterial wom WHERE wom.workOrder.company.id = :companyId " +
           "GROUP BY wom.materialName ORDER BY totalCost DESC")
    List<Object[]> getMaterialUsageSummary(@Param("companyId") UUID companyId);
    
    @Query("SELECT COUNT(DISTINCT wom.materialCode) FROM WorkOrderMaterial wom " +
           "WHERE wom.workOrder.id = :workOrderId")
    long countUniqueMaterials(@Param("workOrderId") UUID workOrderId);
    
    // ========== Search ==========
    
    @Query("SELECT wom FROM WorkOrderMaterial wom WHERE wom.workOrder.company.id = :companyId " +
           "AND (LOWER(wom.materialName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(wom.materialCode) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<WorkOrderMaterial> searchMaterials(@Param("companyId") UUID companyId, 
                                           @Param("searchTerm") String searchTerm);
}