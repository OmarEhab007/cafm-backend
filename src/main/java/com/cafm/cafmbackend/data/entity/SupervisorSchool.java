package com.cafm.cafmbackend.data.entity;

import com.cafm.cafmbackend.data.entity.base.SoftDeletableEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * SupervisorSchool entity - junction table between supervisors and schools.
 * Placeholder implementation - to be completed
 */
@Entity
@Table(name = "supervisor_schools")
public class SupervisorSchool extends SoftDeletableEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private User supervisor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // Getters and Setters
    public User getSupervisor() {
        return supervisor;
    }
    
    public void setSupervisor(User supervisor) {
        this.supervisor = supervisor;
    }
    
    public School getSchool() {
        return school;
    }
    
    public void setSchool(School school) {
        this.school = school;
    }
    
    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }
    
    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}