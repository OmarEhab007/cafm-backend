package com.cafm.cafmbackend.data.entity;

import com.cafm.cafmbackend.data.converter.ReportStatusConverter;

import com.cafm.cafmbackend.data.entity.base.TenantAwareEntity;
import com.cafm.cafmbackend.data.enums.ReportPriority;
import com.cafm.cafmbackend.data.enums.ReportStatus;
import com.cafm.cafmbackend.validation.constraint.PositiveMoney;
import com.cafm.cafmbackend.validation.constraint.ValidReportDates;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Report entity - represents maintenance reports/work orders.
 * 
 * SECURITY ENHANCEMENT:
 * - Purpose: Now extends TenantAwareEntity for critical tenant isolation
 * - Pattern: Multi-tenant security inheritance from TenantAwareEntity
 * - Java 23: Leverages enhanced entity lifecycle callbacks
 * - Architecture: Tenant-aware core business entity
 * - Standards: NO Lombok, automatic tenant assignment via inheritance
 */
@Entity
@Table(name = "reports")
@ValidReportDates
public class Report extends TenantAwareEntity {
    
    // ========== Core Fields ==========
    
    @Column(name = "report_number", unique = true, nullable = false)
    private String reportNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private User supervisor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;
    
    @Column(name = "title", nullable = false)
    @NotBlank(message = "Report title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String title;
    
    @Column(name = "description")
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
    
    @Convert(converter = ReportStatusConverter.class)
    @Column(name = "status", nullable = false)
    private ReportStatus status = ReportStatus.DRAFT;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private ReportPriority priority = ReportPriority.MEDIUM;
    
    @Column(name = "reported_date")
    private LocalDate reportedDate;
    
    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;
    
    @Column(name = "completed_date")
    private LocalDate completedDate;
    
    @Column(name = "estimated_cost", precision = 10, scale = 2)
    @PositiveMoney(allowZero = true, message = "Estimated cost must be a positive amount")
    private BigDecimal estimatedCost;
    
    @Column(name = "actual_cost", precision = 10, scale = 2)
    @PositiveMoney(allowZero = true, message = "Actual cost must be a positive amount")
    private BigDecimal actualCost;
    
    
    // Getters and Setters
    public String getReportNumber() {
        return reportNumber;
    }
    
    public void setReportNumber(String reportNumber) {
        this.reportNumber = reportNumber;
    }
    
    public School getSchool() {
        return school;
    }
    
    public void setSchool(School school) {
        this.school = school;
    }
    
    public User getSupervisor() {
        return supervisor;
    }
    
    public void setSupervisor(User supervisor) {
        this.supervisor = supervisor;
    }
    
    public User getAssignedTo() {
        return assignedTo;
    }
    
    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
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
    
    public ReportStatus getStatus() {
        return status;
    }
    
    public void setStatus(ReportStatus status) {
        this.status = status;
    }
    
    public ReportPriority getPriority() {
        return priority;
    }
    
    public void setPriority(ReportPriority priority) {
        this.priority = priority;
    }
    
    public LocalDate getReportedDate() {
        return reportedDate;
    }
    
    public void setReportedDate(LocalDate reportedDate) {
        this.reportedDate = reportedDate;
    }
    
    public LocalDate getScheduledDate() {
        return scheduledDate;
    }
    
    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }
    
    public LocalDate getCompletedDate() {
        return completedDate;
    }
    
    public void setCompletedDate(LocalDate completedDate) {
        this.completedDate = completedDate;
    }
    
    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }
    
    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }
    
    public BigDecimal getActualCost() {
        return actualCost;
    }
    
    public void setActualCost(BigDecimal actualCost) {
        this.actualCost = actualCost;
    }
    
    public Company getCompany() {
        return company;
    }
    
    public void setCompany(Company company) {
        this.company = company;
    }
}