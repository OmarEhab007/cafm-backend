package com.cafm.cafmbackend.infrastructure.persistence.entity;

import com.cafm.cafmbackend.infrastructure.persistence.entity.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Objects;

/**
 * AdminSupervisor entity - represents the assignment relationship between admins and supervisors.
 * 
 * Architecture: Junction table for many-to-many relationship with management metadata
 * Pattern: Association entity with hierarchical business logic
 * Java 23: Uses pattern matching and modern validation
 */
@Entity
@Table(name = "admin_supervisors")
@NamedQueries({
    @NamedQuery(
        name = "AdminSupervisor.findActiveAssignments",
        query = "SELECT aso FROM AdminSupervisor aso WHERE aso.isActive = true"
    ),
    @NamedQuery(
        name = "AdminSupervisor.findBySupervisor",
        query = "SELECT aso FROM AdminSupervisor aso WHERE aso.supervisor.id = :supervisorId AND aso.isActive = true"
    )
})
public class AdminSupervisor extends BaseEntity {
    
    // ========== Multi-Tenant Relationship ==========
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    @NotNull(message = "Company is required")
    private Company company;
    
    // ========== Relationship Fields ==========
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    @NotNull(message = "Admin is required")
    private User admin;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supervisor_id", nullable = false)
    @NotNull(message = "Supervisor is required")
    private User supervisor;
    
    // ========== Assignment Details ==========
    
    @Column(name = "assigned_date", nullable = false)
    @NotNull(message = "Assignment date is required")
    private LocalDate assignedDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "is_active", nullable = false)
    @NotNull(message = "Active status is required")
    private Boolean isActive = true;
    
    // ========== Management Details ==========
    
    @Column(name = "region", length = 100)
    private String region; // Geographic region managed
    
    @Column(name = "authority_level")
    @Min(value = 1, message = "Authority level must be at least 1")
    @Max(value = 5, message = "Authority level cannot exceed 5")
    private Integer authorityLevel = 3; // 1 = highest authority, 5 = lowest
    
    @Column(name = "max_schools_oversight")
    @Min(value = 1, message = "Max schools oversight must be at least 1")
    private Integer maxSchoolsOversight = 50; // Maximum schools this supervisor can oversee
    
    // ========== Performance Tracking ==========
    
    @Column(name = "supervisors_managed")
    @Min(value = 0, message = "Supervisors managed cannot be negative")
    private Integer supervisorsManaged = 1; // This supervisor plus any sub-supervisors
    
    @Column(name = "total_schools_covered")
    @Min(value = 0, message = "Total schools covered cannot be negative")
    private Integer totalSchoolsCovered = 0;
    
    @Column(name = "total_technicians_covered")
    @Min(value = 0, message = "Total technicians covered cannot be negative")
    private Integer techniciansCovered = 0;
    
    @Column(name = "efficiency_rating")
    @DecimalMin(value = "0.00", message = "Efficiency rating cannot be negative")
    @DecimalMax(value = "100.00", message = "Efficiency rating cannot exceed 100.00")
    private Double efficiencyRating;
    
    // ========== Assignment Metadata ==========
    
    @Column(name = "assignment_notes")
    @Size(max = 1000, message = "Assignment notes cannot exceed 1000 characters")
    private String assignmentNotes;
    
    @Column(name = "assigned_by_user_id")
    private java.util.UUID assignedByUserId; // Super admin who created the assignment
    
    @Column(name = "is_primary_admin")
    private Boolean isPrimaryAdmin = true; // Whether this is the supervisor's primary admin
    
    // ========== Constructors ==========
    
    public AdminSupervisor() {
        super();
    }
    
    public AdminSupervisor(User admin, User supervisor, LocalDate assignedDate) {
        this();
        this.admin = admin;
        this.supervisor = supervisor;
        this.assignedDate = assignedDate;
    }
    
    // ========== Business Methods ==========
    
    /**
     * Calculate supervisor coverage ratio (schools covered vs max capacity)
     */
    public double getCoverageRatio() {
        if (maxSchoolsOversight == null || maxSchoolsOversight == 0) {
            return 0.0;
        }
        return (totalSchoolsCovered != null ? totalSchoolsCovered.doubleValue() : 0.0) / 
               maxSchoolsOversight.doubleValue();
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
     * Check if supervisor is at capacity
     */
    public boolean isAtCapacity() {
        return totalSchoolsCovered != null && maxSchoolsOversight != null && 
               totalSchoolsCovered >= maxSchoolsOversight;
    }
    
    /**
     * Check if supervisor can take on more schools
     */
    public boolean canTakeMoreSchools(int additionalSchools) {
        if (maxSchoolsOversight == null) {
            return true; // No limit set
        }
        
        int currentSchools = totalSchoolsCovered != null ? totalSchoolsCovered : 0;
        return (currentSchools + additionalSchools) <= maxSchoolsOversight;
    }
    
    /**
     * Get assignment duration in days
     */
    public long getAssignmentDurationDays() {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        return java.time.temporal.ChronoUnit.DAYS.between(assignedDate, end);
    }
    
    /**
     * Check if this supervisor has high authority
     */
    public boolean hasHighAuthority() {
        return authorityLevel != null && authorityLevel <= 2;
    }
    
    /**
     * Calculate technician to supervisor ratio
     */
    public double getTechnicianToSupervisorRatio() {
        if (supervisorsManaged == null || supervisorsManaged == 0) {
            return 0.0;
        }
        return (techniciansCovered != null ? techniciansCovered.doubleValue() : 0.0) / 
               supervisorsManaged.doubleValue();
    }
    
    /**
     * Update coverage statistics
     */
    public void updateCoverageStats(int schools, int technicians) {
        this.totalSchoolsCovered = schools;
        this.techniciansCovered = technicians;
    }
    
    /**
     * Check if this is a senior management assignment
     */
    public boolean isSeniorManagement() {
        return hasHighAuthority() && 
               (maxSchoolsOversight != null && maxSchoolsOversight > 30);
    }
    
    // ========== Getters and Setters ==========
    
    public Company getCompany() {
        return company;
    }
    
    public void setCompany(Company company) {
        this.company = company;
    }
    
    public User getAdmin() {
        return admin;
    }
    
    public void setAdmin(User admin) {
        this.admin = admin;
    }
    
    public User getSupervisor() {
        return supervisor;
    }
    
    public void setSupervisor(User supervisor) {
        this.supervisor = supervisor;
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
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public Integer getAuthorityLevel() {
        return authorityLevel;
    }
    
    public void setAuthorityLevel(Integer authorityLevel) {
        this.authorityLevel = authorityLevel;
    }
    
    public Integer getMaxSchoolsOversight() {
        return maxSchoolsOversight;
    }
    
    public void setMaxSchoolsOversight(Integer maxSchoolsOversight) {
        this.maxSchoolsOversight = maxSchoolsOversight;
    }
    
    public Integer getSupervisorsManaged() {
        return supervisorsManaged;
    }
    
    public void setSupervisorsManaged(Integer supervisorsManaged) {
        this.supervisorsManaged = supervisorsManaged;
    }
    
    public Integer getTotalSchoolsCovered() {
        return totalSchoolsCovered;
    }
    
    public void setTotalSchoolsCovered(Integer totalSchoolsCovered) {
        this.totalSchoolsCovered = totalSchoolsCovered;
    }
    
    public Integer getTechniciansCovered() {
        return techniciansCovered;
    }
    
    public void setTechniciansCovered(Integer techniciansCovered) {
        this.techniciansCovered = techniciansCovered;
    }
    
    public Double getEfficiencyRating() {
        return efficiencyRating;
    }
    
    public void setEfficiencyRating(Double efficiencyRating) {
        this.efficiencyRating = efficiencyRating;
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
    
    public Boolean getIsPrimaryAdmin() {
        return isPrimaryAdmin;
    }
    
    public void setIsPrimaryAdmin(Boolean isPrimaryAdmin) {
        this.isPrimaryAdmin = isPrimaryAdmin;
    }
    
    // ========== toString, equals, hashCode ==========
    
    @Override
    public String toString() {
        return String.format("AdminSupervisor[id=%s, admin=%s, supervisor=%s, active=%s, region=%s]",
            getId(), 
            admin != null ? admin.getUsername() : null,
            supervisor != null ? supervisor.getUsername() : null,
            isActive, region);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdminSupervisor that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(admin, that.admin) && 
               Objects.equals(supervisor, that.supervisor) &&
               Objects.equals(assignedDate, that.assignedDate);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), admin, supervisor, assignedDate);
    }
}