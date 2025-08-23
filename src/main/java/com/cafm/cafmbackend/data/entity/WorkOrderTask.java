package com.cafm.cafmbackend.data.entity;

import com.cafm.cafmbackend.data.entity.base.TenantAwareEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Work Order Task entity for tracking individual tasks within a work order.
 * Represents checklist items or subtasks.
 */
@Entity
@Table(name = "work_order_tasks")
public class WorkOrderTask extends TenantAwareEntity {
    // SECURITY: Multi-tenant task isolation via TenantAwareEntity
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    @NotNull(message = "Work order is required")
    private WorkOrder workOrder;
    
    @Column(name = "task_number", nullable = false)
    @NotNull(message = "Task number is required")
    @Min(value = 1, message = "Task number must be positive")
    private Integer taskNumber;
    
    @Column(name = "title", nullable = false, length = 255)
    @NotBlank(message = "Task title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "status", length = 30)
    @Size(max = 30, message = "Status cannot exceed 30 characters")
    private String status = "pending";
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;
    
    @Column(name = "estimated_hours", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Estimated hours cannot be negative")
    private BigDecimal estimatedHours;
    
    @Column(name = "actual_hours", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Actual hours cannot be negative")
    private BigDecimal actualHours;
    
    @Column(name = "is_mandatory")
    private Boolean isMandatory = false;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private User completedBy;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    // ========== Constructors ==========
    
    public WorkOrderTask() {
        super();
    }
    
    public WorkOrderTask(WorkOrder workOrder, Integer taskNumber, String title) {
        this();
        this.workOrder = workOrder;
        this.taskNumber = taskNumber;
        this.title = title;
    }
    
    // ========== Business Methods ==========
    
    /**
     * Mark task as completed
     */
    public void complete(User user, String notes) {
        this.status = "completed";
        this.completedAt = LocalDateTime.now();
        this.completedBy = user;
        if (notes != null) {
            this.notes = notes;
        }
    }
    
    /**
     * Mark task as in progress
     */
    public void startWork(User user) {
        this.status = "in_progress";
        if (this.assignedTo == null) {
            this.assignedTo = user;
        }
    }
    
    /**
     * Check if task is completed
     */
    public boolean isCompleted() {
        return "completed".equals(status) || completedAt != null;
    }
    
    /**
     * Check if task is in progress
     */
    public boolean isInProgress() {
        return "in_progress".equals(status);
    }
    
    /**
     * Check if task is pending
     */
    public boolean isPending() {
        return "pending".equals(status);
    }
    
    /**
     * Reset task to pending
     */
    public void reset() {
        this.status = "pending";
        this.completedAt = null;
        this.completedBy = null;
        this.actualHours = null;
    }
    
    /**
     * Calculate time variance (actual vs estimated)
     */
    public BigDecimal calculateTimeVariance() {
        if (estimatedHours == null || actualHours == null) {
            return BigDecimal.ZERO;
        }
        return actualHours.subtract(estimatedHours);
    }
    
    /**
     * Get completion percentage based on actual vs estimated hours
     */
    public Integer getCompletionPercentage() {
        if (isCompleted()) return 100;
        if (estimatedHours == null || estimatedHours.compareTo(BigDecimal.ZERO) == 0) {
            return isInProgress() ? 50 : 0;
        }
        if (actualHours == null) return 0;
        
        BigDecimal percentage = actualHours.divide(estimatedHours, 2, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal(100));
        return Math.min(percentage.intValue(), 99); // Cap at 99% until completed
    }
    
    // ========== Getters and Setters ==========
    
    public WorkOrder getWorkOrder() {
        return workOrder;
    }
    
    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }
    
    public Integer getTaskNumber() {
        return taskNumber;
    }
    
    public void setTaskNumber(Integer taskNumber) {
        this.taskNumber = taskNumber;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public User getAssignedTo() {
        return assignedTo;
    }
    
    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }
    
    public BigDecimal getEstimatedHours() {
        return estimatedHours;
    }
    
    public void setEstimatedHours(BigDecimal estimatedHours) {
        this.estimatedHours = estimatedHours;
    }
    
    public BigDecimal getActualHours() {
        return actualHours;
    }
    
    public void setActualHours(BigDecimal actualHours) {
        this.actualHours = actualHours;
    }
    
    public Boolean getIsMandatory() {
        return isMandatory;
    }
    
    public void setIsMandatory(Boolean isMandatory) {
        this.isMandatory = isMandatory;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public User getCompletedBy() {
        return completedBy;
    }
    
    public void setCompletedBy(User completedBy) {
        this.completedBy = completedBy;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    // ========== toString, equals, hashCode ==========
    
    @Override
    public String toString() {
        return String.format("WorkOrderTask[id=%s, workOrder=%s, taskNumber=%d, title=%s, status=%s]",
            getId(), workOrder != null ? workOrder.getWorkOrderNumber() : "null", 
            taskNumber, title, status);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkOrderTask)) return false;
        if (!super.equals(o)) return false;
        WorkOrderTask that = (WorkOrderTask) o;
        return Objects.equals(workOrder, that.workOrder) &&
               Objects.equals(taskNumber, that.taskNumber);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), workOrder, taskNumber);
    }
}