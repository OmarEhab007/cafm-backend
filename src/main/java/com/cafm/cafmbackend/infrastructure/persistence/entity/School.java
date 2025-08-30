package com.cafm.cafmbackend.infrastructure.persistence.entity;

import com.cafm.cafmbackend.infrastructure.persistence.entity.base.TenantAwareEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * School entity - represents educational institutions.
 * 
 * SECURITY ENHANCEMENT:
 * - Purpose: Now extends TenantAwareEntity for critical school isolation
 * - Pattern: Multi-tenant security via inherited company validation
 * - Java 23: Enhanced JPA entity with automatic tenant assignment
 * - Architecture: Tenant-aware educational institution entity
 * - Standards: NO Lombok, inherits tenant security from base entity
 */
@Entity
@Table(name = "schools")
public class School extends TenantAwareEntity {
    
    // ========== Core Fields ==========
    
    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "name_ar")
    private String nameAr;
    
    @Column(name = "type", length = 50)
    private String type;
    
    @Column(name = "gender", length = 20)
    private String gender;
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;
    
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "maintenance_score")
    private Integer maintenanceScore;

    @Column(name = "activity_level")
    private String activityLevel;
    
    // ========== Relationships ==========
    
    @OneToMany(mappedBy = "school", fetch = FetchType.LAZY)
    private Set<Asset> assets = new HashSet<>();
    
    @OneToMany(mappedBy = "school", fetch = FetchType.LAZY)
    private Set<WorkOrder> workOrders = new HashSet<>();
    
    @OneToMany(mappedBy = "school", fetch = FetchType.LAZY)
    private Set<Report> reports = new HashSet<>();
    
    // Getters and Setters
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getNameAr() {
        return nameAr;
    }
    
    public void setNameAr(String nameAr) {
        this.nameAr = nameAr;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
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
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getMaintenanceScore() {
        return maintenanceScore;
    }

    public void setMaintenanceScore(Integer maintenanceScore) {
        this.maintenanceScore = maintenanceScore;
    }

    public String getActivityLevel() {
        return activityLevel;
    }
    public void setActivityLevel(String activityLevel) {
        this.activityLevel = activityLevel;
    }
    
    public Company getCompany() {
        return company;
    }
    
    public void setCompany(Company company) {
        this.company = company;
    }
    
    /**
     * Check if school belongs to a specific company
     */
    public boolean belongsToCompany(UUID companyId) {
        return company != null && Objects.equals(company.getId(), companyId);
    }
    
    // ========== Relationship Getters and Setters ==========
    
    public Set<Asset> getAssets() {
        return assets;
    }
    
    public void setAssets(Set<Asset> assets) {
        this.assets = assets;
    }
    
    public Set<WorkOrder> getWorkOrders() {
        return workOrders;
    }
    
    public void setWorkOrders(Set<WorkOrder> workOrders) {
        this.workOrders = workOrders;
    }
    
    public Set<Report> getReports() {
        return reports;
    }
    
    public void setReports(Set<Report> reports) {
        this.reports = reports;
    }

}