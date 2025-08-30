package com.cafm.cafmbackend.infrastructure.persistence.repository;

import com.cafm.cafmbackend.infrastructure.persistence.entity.WorkOrderAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for WorkOrderAttachment entity.
 */
@Repository
public interface WorkOrderAttachmentRepository extends JpaRepository<WorkOrderAttachment, UUID> {
    
    // ========== Basic Queries ==========
    
    List<WorkOrderAttachment> findByWorkOrderId(UUID workOrderId);
    
    List<WorkOrderAttachment> findByWorkOrderIdOrderByUploadedAtDesc(UUID workOrderId);
    
    List<WorkOrderAttachment> findByWorkOrderIdAndAttachmentType(UUID workOrderId, String attachmentType);
    
    // ========== File Type Queries ==========
    
    @Query("SELECT woa FROM WorkOrderAttachment woa WHERE woa.workOrder.id = :workOrderId " +
           "AND LOWER(woa.fileType) LIKE 'image/%'")
    List<WorkOrderAttachment> findImagesByWorkOrderId(@Param("workOrderId") UUID workOrderId);
    
    @Query("SELECT woa FROM WorkOrderAttachment woa WHERE woa.workOrder.id = :workOrderId " +
           "AND woa.fileType = 'application/pdf'")
    List<WorkOrderAttachment> findPdfsByWorkOrderId(@Param("workOrderId") UUID workOrderId);
    
    // ========== Attachment Type Queries ==========
    
    @Query("SELECT woa FROM WorkOrderAttachment woa WHERE woa.workOrder.id = :workOrderId " +
           "AND woa.attachmentType = 'before'")
    List<WorkOrderAttachment> findBeforePhotos(@Param("workOrderId") UUID workOrderId);
    
    @Query("SELECT woa FROM WorkOrderAttachment woa WHERE woa.workOrder.id = :workOrderId " +
           "AND woa.attachmentType = 'after'")
    List<WorkOrderAttachment> findAfterPhotos(@Param("workOrderId") UUID workOrderId);
    
    @Query("SELECT woa FROM WorkOrderAttachment woa WHERE woa.workOrder.id = :workOrderId " +
           "AND woa.attachmentType = 'invoice'")
    List<WorkOrderAttachment> findInvoices(@Param("workOrderId") UUID workOrderId);
    
    // ========== Upload Queries ==========
    
    List<WorkOrderAttachment> findByUploadedById(UUID userId);
    
    @Query("SELECT woa FROM WorkOrderAttachment woa WHERE woa.uploadedBy.id = :userId " +
           "AND woa.uploadedAt BETWEEN :startDate AND :endDate")
    List<WorkOrderAttachment> findByUploadedByInPeriod(@Param("userId") UUID userId,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);
    
    // ========== Statistics ==========
    
    @Query("SELECT COUNT(woa) FROM WorkOrderAttachment woa WHERE woa.workOrder.id = :workOrderId")
    long countByWorkOrderId(@Param("workOrderId") UUID workOrderId);
    
    @Query("SELECT COUNT(woa) FROM WorkOrderAttachment woa WHERE woa.workOrder.id = :workOrderId " +
           "AND woa.attachmentType = :attachmentType")
    long countByWorkOrderIdAndType(@Param("workOrderId") UUID workOrderId, 
                                  @Param("attachmentType") String attachmentType);
    
    @Query("SELECT SUM(woa.fileSize) FROM WorkOrderAttachment woa WHERE woa.workOrder.id = :workOrderId")
    Long getTotalFileSize(@Param("workOrderId") UUID workOrderId);
    
    @Query("SELECT AVG(woa.fileSize) FROM WorkOrderAttachment woa " +
           "WHERE woa.workOrder.company.id = :companyId")
    Double getAverageFileSize(@Param("companyId") UUID companyId);
    
    // ========== Company Queries ==========
    
    @Query("SELECT woa FROM WorkOrderAttachment woa WHERE woa.workOrder.company.id = :companyId " +
           "ORDER BY woa.uploadedAt DESC")
    List<WorkOrderAttachment> findRecentByCompany(@Param("companyId") UUID companyId);
    
    @Query("SELECT SUM(woa.fileSize) FROM WorkOrderAttachment woa " +
           "WHERE woa.workOrder.company.id = :companyId")
    Long getTotalStorageUsedByCompany(@Param("companyId") UUID companyId);
    
    // ========== Search ==========
    
    @Query("SELECT woa FROM WorkOrderAttachment woa WHERE woa.workOrder.id = :workOrderId " +
           "AND LOWER(woa.fileName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<WorkOrderAttachment> searchByFileName(@Param("workOrderId") UUID workOrderId, 
                                              @Param("searchTerm") String searchTerm);
}