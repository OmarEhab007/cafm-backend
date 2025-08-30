package com.cafm.cafmbackend.infrastructure.persistence.entity;


import com.cafm.cafmbackend.infrastructure.persistence.entity.base.TenantAwareEntity;
import com.cafm.cafmbackend.shared.enums.WorkOrderStatus;
import com.cafm.cafmbackend.shared.enums.WorkOrderPriority;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Work Order entity for tracking maintenance tasks.
 * 
 * SECURITY ENHANCEMENT:
 * - Purpose: Now extends TenantAwareEntity for critical work order isolation
 * - Pattern: Multi-tenant security via inherited company validation
 * - Java 23: Enhanced JPA lifecycle callbacks for tenant enforcement
 * - Architecture: Core tenant-aware business entity
 * - Standards: NO Lombok, automatic tenant isolation at entity level
 */
@Entity
@Table(name = "work_orders")
@NamedQueries({
    @NamedQuery(
        name = "WorkOrder.findByCompanyAndStatus",
        query = "SELECT wo FROM WorkOrder wo WHERE wo.company.id = :companyId AND wo.status = :status AND wo.deletedAt IS NULL"
    ),
    @NamedQuery(
        name = "WorkOrder.findOverdue",
        query = "SELECT wo FROM WorkOrder wo WHERE wo.company.id = :companyId AND wo.scheduledEnd < :currentDate AND wo.status NOT IN ('COMPLETED', 'CANCELLED', 'VERIFIED') AND wo.deletedAt IS NULL"
    ),
    @NamedQuery(
        name = "WorkOrder.findByAssignee",
        query = "SELECT wo FROM WorkOrder wo WHERE wo.assignedTo.id = :userId AND wo.status IN :statuses AND wo.deletedAt IS NULL"
    )
})
public class WorkOrder extends TenantAwareEntity {
    
    @Column(name = "work_order_number", nullable = false, length = 50)
    @NotBlank(message = "Work order number is required")
    @Size(max = 50, message = "Work order number cannot exceed 50 characters")
    private String workOrderNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private Report report;
    
    // ========== Assignment ==========
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;
    
    @Column(name = "assignment_date")
    private LocalDateTime assignmentDate;
    
    // ========== Work Details ==========
    
    @Column(name = "title", nullable = false, length = 255)
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "category", length = 50)
    @Size(max = 50, message = "Category cannot exceed 50 characters")
    private String category;
    
    @Column(name = "priority")
    @Enumerated(EnumType.STRING)
    private WorkOrderPriority priority = WorkOrderPriority.MEDIUM;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private WorkOrderStatus status = WorkOrderStatus.PENDING;
    
    // ========== Schedule ==========
    
    @Column(name = "scheduled_start")
    private LocalDateTime scheduledStart;
    
    @Column(name = "scheduled_end")
    private LocalDateTime scheduledEnd;
    
    @Column(name = "actual_start")
    private LocalDateTime actualStart;
    
    @Column(name = "actual_end")
    private LocalDateTime actualEnd;
    
    // ========== Location ==========
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;
    
    @Column(name = "location_details", columnDefinition = "TEXT")
    private String locationDetails;
    
    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;
    
    // ========== Cost Tracking ==========
    
    @Column(name = "estimated_hours", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Estimated hours cannot be negative")
    private BigDecimal estimatedHours;
    
    @Column(name = "actual_hours", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Actual hours cannot be negative")
    private BigDecimal actualHours;
    
    @Column(name = "labor_cost", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Labor cost cannot be negative")
    private BigDecimal laborCost;
    
    @Column(name = "material_cost", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Material cost cannot be negative")
    private BigDecimal materialCost;
    
    @Column(name = "other_cost", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Other cost cannot be negative")
    private BigDecimal otherCost;
    
    @Column(name = "total_cost", precision = 10, scale = 2, insertable = false, updatable = false)
    private BigDecimal totalCost;
    
    // ========== Completion ==========
    
    @Column(name = "completion_percentage")
    @Min(value = 0, message = "Completion percentage cannot be less than 0")
    @Max(value = 100, message = "Completion percentage cannot exceed 100")
    private Integer completionPercentage = 0;
    
    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;
    
    @Column(name = "signature_url", columnDefinition = "TEXT")
    private String signatureUrl;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    // ========== Relationships ==========
    
    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<WorkOrderTask> tasks = new HashSet<>();
    
    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<WorkOrderMaterial> materials = new HashSet<>();
    
    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<WorkOrderAttachment> attachments = new HashSet<>();
    
    // ========== Constructors ==========
    
    public WorkOrder() {
        super();
    }
    
    public WorkOrder(Company company, String workOrderNumber, String title) {
        this();
        this.company = company;
        this.workOrderNumber = workOrderNumber;
        this.title = title;
    }
    
    // ========== Business Methods ==========
    
    /**
     * Start work on this order
     */
    public void startWork(User technician) {
        if (status != WorkOrderStatus.ASSIGNED && status != WorkOrderStatus.ON_HOLD) {
            throw new IllegalStateException("Cannot start work on order with status: " + status);
        }
        this.status = WorkOrderStatus.IN_PROGRESS;
        this.actualStart = LocalDateTime.now();
        if (this.assignedTo == null) {
            this.assignedTo = technician;
            this.assignmentDate = LocalDateTime.now();
        }
    }
    
    /**
     * Complete the work order
     */
    public void complete(String notes) {
        if (status != WorkOrderStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only complete in-progress work orders");
        }
        this.status = WorkOrderStatus.COMPLETED;
        this.actualEnd = LocalDateTime.now();
        this.completionNotes = notes;
        this.completionPercentage = 100;
    }
    
    /**
     * Put work order on hold
     */
    public void putOnHold(String reason) {
        if (!status.isActive()) {
            throw new IllegalStateException("Can only hold active work orders");
        }
        this.status = WorkOrderStatus.ON_HOLD;
        if (this.completionNotes != null) {
            this.completionNotes += "\nOn Hold: " + reason;
        } else {
            this.completionNotes = "On Hold: " + reason;
        }
    }
    
    /**
     * Cancel work order
     */
    public void cancel(String reason) {
        if (status.isFinal()) {
            throw new IllegalStateException("Cannot cancel finalized work order");
        }
        this.status = WorkOrderStatus.CANCELLED;
        this.completionNotes = "Cancelled: " + reason;
    }
    
    /**
     * Verify completed work
     */
    public void verify(User supervisor) {
        if (status != WorkOrderStatus.COMPLETED) {
            throw new IllegalStateException("Can only verify completed work orders");
        }
        this.status = WorkOrderStatus.VERIFIED;
        this.verifiedBy = supervisor;
        this.verifiedAt = LocalDateTime.now();
    }
    
    /**
     * Calculate total cost
     */
    public BigDecimal calculateTotalCost() {
        BigDecimal total = BigDecimal.ZERO;
        if (laborCost != null) total = total.add(laborCost);
        if (materialCost != null) total = total.add(materialCost);
        if (otherCost != null) total = total.add(otherCost);
        return total;
    }
    
    /**
     * Check if work order is overdue
     */
    public boolean isOverdue() {
        if (status.isFinal() || scheduledEnd == null) {
            return false;
        }
        return scheduledEnd.isBefore(LocalDateTime.now());
    }
    
    /**
     * Calculate actual duration in hours
     */
    public BigDecimal calculateActualDuration() {
        if (actualStart == null || actualEnd == null) {
            return BigDecimal.ZERO;
        }
        long minutes = java.time.Duration.between(actualStart, actualEnd).toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Add a task to this work order
     */
    public void addTask(WorkOrderTask task) {
        tasks.add(task);
        task.setWorkOrder(this);
    }
    
    /**
     * Add material to this work order
     */
    public void addMaterial(WorkOrderMaterial material) {
        materials.add(material);
        material.setWorkOrder(this);
        // Recalculate material cost
        this.materialCost = materials.stream()
            .map(WorkOrderMaterial::getTotalCost)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Add attachment to this work order
     */
    public void addAttachment(WorkOrderAttachment attachment) {
        attachments.add(attachment);
        attachment.setWorkOrder(this);
    }
    
    /**
     * Get progress status text
     */
    public String getProgressStatus() {
        if (completionPercentage == null) return "Not Started";
        if (completionPercentage == 0) return "Not Started";
        if (completionPercentage < 25) return "Just Started";
        if (completionPercentage < 50) return "In Progress";
        if (completionPercentage < 75) return "Half Done";
        if (completionPercentage < 100) return "Almost Complete";
        return "Completed";
    }
    
    // ========== Getters and Setters ==========
    
    // Company getter/setter inherited from TenantAwareEntity
    
    public String getWorkOrderNumber() {
        return workOrderNumber;
    }
    
    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }
    
    public Report getReport() {
        return report;
    }
    
    public void setReport(Report report) {
        this.report = report;
    }
    
    public User getAssignedTo() {
        return assignedTo;
    }
    
    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }
    
    public User getAssignedBy() {
        return assignedBy;
    }
    
    public void setAssignedBy(User assignedBy) {
        this.assignedBy = assignedBy;
    }
    
    public LocalDateTime getAssignmentDate() {
        return assignmentDate;
    }
    
    public void setAssignmentDate(LocalDateTime assignmentDate) {
        this.assignmentDate = assignmentDate;
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
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public WorkOrderPriority getPriority() {
        return priority;
    }
    
    public void setPriority(WorkOrderPriority priority) {
        this.priority = priority;
    }
    
    public WorkOrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(WorkOrderStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getScheduledStart() {
        return scheduledStart;
    }
    
    public void setScheduledStart(LocalDateTime scheduledStart) {
        this.scheduledStart = scheduledStart;
    }
    
    public LocalDateTime getScheduledEnd() {
        return scheduledEnd;
    }
    
    public void setScheduledEnd(LocalDateTime scheduledEnd) {
        this.scheduledEnd = scheduledEnd;
    }
    
    public LocalDateTime getActualStart() {
        return actualStart;
    }
    
    public void setActualStart(LocalDateTime actualStart) {
        this.actualStart = actualStart;
    }
    
    public LocalDateTime getActualEnd() {
        return actualEnd;
    }
    
    public void setActualEnd(LocalDateTime actualEnd) {
        this.actualEnd = actualEnd;
    }
    
    public School getSchool() {
        return school;
    }
    
    public void setSchool(School school) {
        this.school = school;
    }
    
    public String getLocationDetails() {
        return locationDetails;
    }
    
    public void setLocationDetails(String locationDetails) {
        this.locationDetails = locationDetails;
    }
    
    public BigDecimal getLatitude() {
        return latitude;
    }
    
    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }
    
    public BigDecimal getLongitude() {
        return longitude;
    }
    
    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
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
    
    public BigDecimal getLaborCost() {
        return laborCost;
    }
    
    public void setLaborCost(BigDecimal laborCost) {
        this.laborCost = laborCost;
    }
    
    public BigDecimal getMaterialCost() {
        return materialCost;
    }
    
    public void setMaterialCost(BigDecimal materialCost) {
        this.materialCost = materialCost;
    }
    
    public BigDecimal getOtherCost() {
        return otherCost;
    }
    
    public void setOtherCost(BigDecimal otherCost) {
        this.otherCost = otherCost;
    }
    
    public BigDecimal getTotalCost() {
        return totalCost;
    }
    
    public Integer getCompletionPercentage() {
        return completionPercentage;
    }
    
    public void setCompletionPercentage(Integer completionPercentage) {
        this.completionPercentage = completionPercentage;
    }
    
    public String getCompletionNotes() {
        return completionNotes;
    }
    
    public void setCompletionNotes(String completionNotes) {
        this.completionNotes = completionNotes;
    }
    
    public String getSignatureUrl() {
        return signatureUrl;
    }
    
    public void setSignatureUrl(String signatureUrl) {
        this.signatureUrl = signatureUrl;
    }
    
    public User getVerifiedBy() {
        return verifiedBy;
    }
    
    public void setVerifiedBy(User verifiedBy) {
        this.verifiedBy = verifiedBy;
    }
    
    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }
    
    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
    
    public Set<WorkOrderTask> getTasks() {
        return tasks;
    }
    
    public void setTasks(Set<WorkOrderTask> tasks) {
        this.tasks = tasks;
    }
    
    public Set<WorkOrderMaterial> getMaterials() {
        return materials;
    }
    
    public void setMaterials(Set<WorkOrderMaterial> materials) {
        this.materials = materials;
    }
    
    public Set<WorkOrderAttachment> getAttachments() {
        return attachments;
    }
    
    public void setAttachments(Set<WorkOrderAttachment> attachments) {
        this.attachments = attachments;
    }
    
    // ========== toString, equals, hashCode ==========
    
    @Override
    public String toString() {
        return String.format("WorkOrder[id=%s, number=%s, title=%s, status=%s, priority=%s]",
            getId(), workOrderNumber, title, status, priority);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkOrder)) return false;
        if (!super.equals(o)) return false;
        WorkOrder workOrder = (WorkOrder) o;
        return Objects.equals(company, workOrder.company) &&
               Objects.equals(workOrderNumber, workOrder.workOrderNumber);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), company, workOrderNumber);
    }
}