package com.cafm.cafmbackend.infrastructure.persistence.entity;

import com.cafm.cafmbackend.infrastructure.persistence.entity.base.BaseEntity;
import com.cafm.cafmbackend.shared.enums.TechnicianSpecialization;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Objects;

/**
 * SupervisorTechnician entity - represents the assignment relationship between supervisors and technicians.
 * 
 * Architecture: Junction table for many-to-many relationship with additional metadata
 * Pattern: Association entity with business logic
 * Java 23: Uses pattern matching and modern validation
 */
@Entity
@Table(name = "supervisor_technicians")
@NamedQueries({
    @NamedQuery(
        name = "SupervisorTechnician.findActiveAssignments",
        query = "SELECT st FROM SupervisorTechnician st WHERE st.isActive = true"
    ),
    @NamedQuery(
        name = "SupervisorTechnician.findByTechnician",
        query = "SELECT st FROM SupervisorTechnician st WHERE st.technician.id = :technicianId AND st.isActive = true"
    )
})
public class SupervisorTechnician extends BaseEntity {
    
    // ========== Multi-Tenant Relationship ==========
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    @NotNull(message = "Company is required")
    private Company company;
    
    // ========== Relationship Fields ==========
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supervisor_id", nullable = false)
    @NotNull(message = "Supervisor is required")
    private User supervisor;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id", nullable = false)
    @NotNull(message = "Technician is required")
    private User technician;
    
    // ========== Assignment Details ==========
    
    @Column(name = "assigned_date", nullable = false)
    @NotNull(message = "Assignment date is required")
    private LocalDate assignedDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "is_active", nullable = false)
    @NotNull(message = "Active status is required")
    private Boolean isActive = true;
    
    // ========== Specialization and Priority ==========
    
    @Enumerated(EnumType.STRING)
    @Column(name = "primary_specialization", length = 30)
    private TechnicianSpecialization primarySpecialization;
    
    @Column(name = "priority_level")
    @Min(value = 1, message = "Priority level must be at least 1")
    @Max(value = 10, message = "Priority level cannot exceed 10")
    private Integer priorityLevel = 5; // 1 = highest priority, 10 = lowest
    
    // ========== Performance Tracking ==========
    
    @Column(name = "tasks_assigned")
    @Min(value = 0, message = "Tasks assigned cannot be negative")
    private Integer tasksAssigned = 0;
    
    @Column(name = "tasks_completed")
    @Min(value = 0, message = "Tasks completed cannot be negative")
    private Integer tasksCompleted = 0;
    
    @Column(name = "avg_completion_time_hours")
    @DecimalMin(value = "0.00", message = "Average completion time cannot be negative")
    private Double avgCompletionTimeHours;
    
    // ========== Assignment Metadata ==========
    
    @Column(name = "assignment_notes")
    @Size(max = 1000, message = "Assignment notes cannot exceed 1000 characters")
    private String assignmentNotes;
    
    @Column(name = "assigned_by_user_id")
    private java.util.UUID assignedByUserId; // Admin who created the assignment
    
    // ========== Constructors ==========
    
    public SupervisorTechnician() {
        super();
    }
    
    public SupervisorTechnician(User supervisor, User technician, LocalDate assignedDate) {
        this();
        this.supervisor = supervisor;
        this.technician = technician;
        this.assignedDate = assignedDate;
        this.primarySpecialization = technician.getSpecialization(); // Use technician's specialization
    }
    
    // ========== Business Methods ==========
    
    /**
     * Calculate completion rate percentage
     */
    public double getCompletionRate() {
        if (tasksAssigned == null || tasksAssigned == 0) {
            return 0.0;
        }
        return (tasksCompleted != null ? tasksCompleted.doubleValue() : 0.0) / tasksAssigned.doubleValue() * 100.0;
    }
    
    /**
     * Check if assignment is currently active
     */
    public boolean isCurrentlyActive() {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        
        // Check if assignment has started
        if (assignedDate != null && assignedDate.isAfter(today)) {
            return false;
        }
        
        // Check if assignment has ended
        return endDate == null || !endDate.isBefore(today);
    }
    
    /**
     * End the assignment
     */
    public void endAssignment(LocalDate endDate) {
        this.endDate = endDate;
        this.isActive = false;
    }
    
    /**
     * Increment task counters
     */
    public void assignTask() {
        this.tasksAssigned = (this.tasksAssigned != null ? this.tasksAssigned : 0) + 1;
    }
    
    public void completeTask(double completionTimeHours) {
        this.tasksCompleted = (this.tasksCompleted != null ? this.tasksCompleted : 0) + 1;
        
        // Update average completion time
        if (this.avgCompletionTimeHours == null) {
            this.avgCompletionTimeHours = completionTimeHours;
        } else {
            // Calculate new average: (oldAvg * (n-1) + newValue) / n
            int n = this.tasksCompleted;
            this.avgCompletionTimeHours = (this.avgCompletionTimeHours * (n - 1) + completionTimeHours) / n;
        }
    }
    
    /**
     * Check if technician can handle the required specialization
     */
    public boolean canHandleSpecialization(TechnicianSpecialization required) {
        // Can handle if primary specialization matches or if it's general maintenance
        return primarySpecialization == required || 
               primarySpecialization == TechnicianSpecialization.GENERAL_MAINTENANCE ||
               required == TechnicianSpecialization.GENERAL_MAINTENANCE;
    }
    
    /**
     * Get assignment duration in days
     */
    public long getAssignmentDurationDays() {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        return java.time.temporal.ChronoUnit.DAYS.between(assignedDate, end);
    }
    
    /**
     * Check if this is a high priority assignment
     */
    public boolean isHighPriority() {
        return priorityLevel != null && priorityLevel <= 3;
    }
    
    // ========== Getters and Setters ==========
    
    public Company getCompany() {
        return company;
    }
    
    public void setCompany(Company company) {
        this.company = company;
    }
    
    public User getSupervisor() {
        return supervisor;
    }
    
    public void setSupervisor(User supervisor) {
        this.supervisor = supervisor;
    }
    
    public User getTechnician() {
        return technician;
    }
    
    public void setTechnician(User technician) {
        this.technician = technician;
        // Auto-set primary specialization from technician
        if (technician != null && this.primarySpecialization == null) {
            this.primarySpecialization = technician.getSpecialization();
        }
    }
    
    public LocalDate getAssignedDate() {
        return assignedDate;
    }
    
    public void setAssignedDate(LocalDate assignedDate) {
        this.assignedDate = assignedDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public TechnicianSpecialization getPrimarySpecialization() {
        return primarySpecialization;
    }
    
    public void setPrimarySpecialization(TechnicianSpecialization primarySpecialization) {
        this.primarySpecialization = primarySpecialization;
    }
    
    public Integer getPriorityLevel() {
        return priorityLevel;
    }
    
    public void setPriorityLevel(Integer priorityLevel) {
        this.priorityLevel = priorityLevel;
    }
    
    public Integer getTasksAssigned() {
        return tasksAssigned;
    }
    
    public void setTasksAssigned(Integer tasksAssigned) {
        this.tasksAssigned = tasksAssigned;
    }
    
    public Integer getTasksCompleted() {
        return tasksCompleted;
    }
    
    public void setTasksCompleted(Integer tasksCompleted) {
        this.tasksCompleted = tasksCompleted;
    }
    
    public Double getAvgCompletionTimeHours() {
        return avgCompletionTimeHours;
    }
    
    public void setAvgCompletionTimeHours(Double avgCompletionTimeHours) {
        this.avgCompletionTimeHours = avgCompletionTimeHours;
    }
    
    public String getAssignmentNotes() {
        return assignmentNotes;
    }
    
    public void setAssignmentNotes(String assignmentNotes) {
        this.assignmentNotes = assignmentNotes;
    }
    
    public java.util.UUID getAssignedByUserId() {
        return assignedByUserId;
    }
    
    public void setAssignedByUserId(java.util.UUID assignedByUserId) {
        this.assignedByUserId = assignedByUserId;
    }
    
    // ========== toString, equals, hashCode ==========
    
    @Override
    public String toString() {
        return String.format("SupervisorTechnician[id=%s, supervisor=%s, technician=%s, active=%s, specialization=%s]",
            getId(), 
            supervisor != null ? supervisor.getUsername() : null,
            technician != null ? technician.getUsername() : null,
            isActive, primarySpecialization);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SupervisorTechnician that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(supervisor, that.supervisor) && 
               Objects.equals(technician, that.technician) &&
               Objects.equals(assignedDate, that.assignedDate);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), supervisor, technician, assignedDate);
    }
}