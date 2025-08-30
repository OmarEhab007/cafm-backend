package com.cafm.cafmbackend.infrastructure.persistence.repository;

import com.cafm.cafmbackend.infrastructure.persistence.entity.WorkOrderTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WorkOrderTask entity.
 */
@Repository
public interface WorkOrderTaskRepository extends JpaRepository<WorkOrderTask, UUID> {
    
    // ========== Basic Queries ==========
    
    List<WorkOrderTask> findByWorkOrderId(UUID workOrderId);
    
    List<WorkOrderTask> findByWorkOrderIdOrderByTaskNumber(UUID workOrderId);
    
    Optional<WorkOrderTask> findByWorkOrderIdAndTaskNumber(UUID workOrderId, Integer taskNumber);
    
    // ========== Status Queries ==========
    
    List<WorkOrderTask> findByWorkOrderIdAndStatus(UUID workOrderId, String status);
    
    @Query("SELECT wot FROM WorkOrderTask wot WHERE wot.workOrder.id = :workOrderId " +
           "AND wot.isMandatory = true AND wot.status != 'completed'")
    List<WorkOrderTask> findIncompleteMandatoryTasks(@Param("workOrderId") UUID workOrderId);
    
    @Query("SELECT COUNT(wot) FROM WorkOrderTask wot WHERE wot.workOrder.id = :workOrderId " +
           "AND wot.status = :status")
    long countByWorkOrderIdAndStatus(@Param("workOrderId") UUID workOrderId, @Param("status") String status);
    
    // ========== Assignment Queries ==========
    
    List<WorkOrderTask> findByAssignedToId(UUID userId);
    
    @Query("SELECT wot FROM WorkOrderTask wot WHERE wot.assignedTo.id = :userId " +
           "AND wot.status IN ('pending', 'in_progress') ORDER BY wot.workOrder.priority ASC")
    List<WorkOrderTask> findActiveTasksByUser(@Param("userId") UUID userId);
    
    // ========== Completion Queries ==========
    
    @Query("SELECT wot FROM WorkOrderTask wot WHERE wot.completedBy.id = :userId " +
           "AND wot.completedAt BETWEEN :startDate AND :endDate")
    List<WorkOrderTask> findCompletedByUserInPeriod(@Param("userId") UUID userId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(wot) FROM WorkOrderTask wot WHERE wot.workOrder.id = :workOrderId " +
           "AND wot.completedAt IS NOT NULL")
    long countCompletedTasks(@Param("workOrderId") UUID workOrderId);
    
    @Query("SELECT COUNT(wot) FROM WorkOrderTask wot WHERE wot.workOrder.id = :workOrderId")
    long countTotalTasks(@Param("workOrderId") UUID workOrderId);
    
    // ========== Statistics ==========
    
    @Query("SELECT AVG(wot.actualHours) FROM WorkOrderTask wot " +
           "WHERE wot.workOrder.id = :workOrderId AND wot.actualHours IS NOT NULL")
    Double getAverageActualHours(@Param("workOrderId") UUID workOrderId);
    
    @Query("SELECT SUM(wot.actualHours) FROM WorkOrderTask wot " +
           "WHERE wot.workOrder.id = :workOrderId")
    Double getTotalActualHours(@Param("workOrderId") UUID workOrderId);
    
    @Query("SELECT SUM(wot.estimatedHours) FROM WorkOrderTask wot " +
           "WHERE wot.workOrder.id = :workOrderId")
    Double getTotalEstimatedHours(@Param("workOrderId") UUID workOrderId);
    
    // ========== Update Operations ==========
    
    @Modifying
    @Query("UPDATE WorkOrderTask wot SET wot.status = :status, wot.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE wot.id = :taskId")
    int updateTaskStatus(@Param("taskId") UUID taskId, @Param("status") String status);
    
    @Modifying
    @Query("UPDATE WorkOrderTask wot SET wot.status = 'completed', " +
           "wot.completedAt = CURRENT_TIMESTAMP, wot.completedBy = :userId " +
           "WHERE wot.id = :taskId")
    int markAsCompleted(@Param("taskId") UUID taskId, @Param("userId") UUID userId);
    
    // ========== Bulk Operations ==========
    
    @Modifying
    @Query("DELETE FROM WorkOrderTask wot WHERE wot.workOrder.id = :workOrderId")
    int deleteByWorkOrderId(@Param("workOrderId") UUID workOrderId);
    
    @Modifying
    @Query("UPDATE WorkOrderTask wot SET wot.status = 'pending' " +
           "WHERE wot.workOrder.id = :workOrderId AND wot.status != 'completed'")
    int resetIncompleteTasks(@Param("workOrderId") UUID workOrderId);
}