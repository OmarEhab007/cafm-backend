package com.cafm.cafmbackend.infrastructure.persistence.entity;


import com.cafm.cafmbackend.infrastructure.persistence.entity.base.TenantAwareEntity;
import com.cafm.cafmbackend.shared.enums.ReportPriority;
import com.cafm.cafmbackend.shared.enums.ReportStatus;
import com.cafm.cafmbackend.shared.validation.constraint.PositiveMoney;
import com.cafm.cafmbackend.shared.validation.constraint.ValidReportDates;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by")
    private User reportedBy;
    
    @Column(name = "title", nullable = false)
    @NotBlank(message = "Report title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String title;
    
    @Column(name = "description")
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
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
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "estimated_cost", precision = 10, scale = 2)
    @PositiveMoney(allowZero = true, message = "Estimated cost must be a positive amount")
    private BigDecimal estimatedCost;
    
    @Column(name = "actual_cost", precision = 10, scale = 2)
    @PositiveMoney(allowZero = true, message = "Actual cost must be a positive amount")
    private BigDecimal actualCost;
    
    // ========== Location Fields ==========
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
    @Column(name = "location_accuracy")
    private Double locationAccuracy;
    
    @Column(name = "location_address")
    @Size(max = 500, message = "Location address cannot exceed 500 characters")
    private String locationAddress;
    
    @Column(name = "location_timestamp")
    private LocalDateTime locationTimestamp;
    
    
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
    
    public User getReportedBy() {
        return reportedBy;
    }
    
    public void setReportedBy(User reportedBy) {
        this.reportedBy = reportedBy;
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
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
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
    
    public Double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
    public Double getLocationAccuracy() {
        return locationAccuracy;
    }
    
    public void setLocationAccuracy(Double locationAccuracy) {
        this.locationAccuracy = locationAccuracy;
    }
    
    public String getLocationAddress() {
        return locationAddress;
    }
    
    public void setLocationAddress(String locationAddress) {
        this.locationAddress = locationAddress;
    }
    
    public LocalDateTime getLocationTimestamp() {
        return locationTimestamp;
    }
    
    public void setLocationTimestamp(LocalDateTime locationTimestamp) {
        this.locationTimestamp = locationTimestamp;
    }
    
    public Company getCompany() {
        return company;
    }
    
    public void setCompany(Company company) {
        this.company = company;
    }
}