package com.cafm.cafmbackend.data.entity;

import com.cafm.cafmbackend.data.entity.base.SoftDeletableEntity;
import com.cafm.cafmbackend.data.enums.CompanyStatus;
import com.cafm.cafmbackend.data.enums.SubscriptionPlan;
import com.cafm.cafmbackend.data.converter.CompanyStatusConverter;
import com.cafm.cafmbackend.data.converter.SubscriptionPlanConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Company entity for multi-tenant support.
 * 
 * Architecture: Multi-tenant foundation entity with subscription management
 * Pattern: SaaS multi-tenancy with resource limits and feature toggles
 * Java 23: Modern record patterns and enhanced validation
 */
@Entity
@Table(name = "companies")
@NamedQueries({
    @NamedQuery(
        name = "Company.findActiveCompanies",
        query = "SELECT c FROM Company c WHERE c.status = com.cafm.cafmbackend.data.enums.CompanyStatus.ACTIVE AND c.isActive = true AND c.deletedAt IS NULL"
    ),
    @NamedQuery(
        name = "Company.findByDomain",
        query = "SELECT c FROM Company c WHERE c.domain = :domain AND c.deletedAt IS NULL"
    ),
    @NamedQuery(
        name = "Company.findBySubdomain",
        query = "SELECT c FROM Company c WHERE c.subdomain = :subdomain AND c.deletedAt IS NULL"
    )
})
public class Company extends SoftDeletableEntity {
    
    // ========== Basic Information ==========
    
    @Column(name = "name", nullable = false, length = 255)
    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name cannot exceed 255 characters")
    private String name;
    
    @Column(name = "display_name", length = 255)
    @Size(max = 255, message = "Display name cannot exceed 255 characters")
    private String displayName;
    
    @Column(name = "domain", unique = true, length = 255)
    @Pattern(regexp = "^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", 
             message = "Domain must be a valid domain format")
    private String domain;
    
    @Column(name = "subdomain", unique = true, length = 100)
    @Pattern(regexp = "^[a-z0-9-]+$", 
             message = "Subdomain can only contain lowercase letters, numbers, and hyphens")
    private String subdomain;
    
    // ========== Contact Information ==========
    
    @Column(name = "contact_email", length = 255)
    @Email(message = "Contact email must be valid")
    private String contactEmail;
    
    @Column(name = "contact_phone", length = 20)
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Contact phone must be valid")
    private String contactPhone;
    
    @Column(name = "primary_contact_name", length = 255)
    private String primaryContactName;
    
    // ========== Business Information ==========
    
    @Column(name = "industry", length = 100)
    private String industry;
    
    @Column(name = "country", length = 100)
    private String country = "Saudi Arabia";
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;
    
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    
    @Column(name = "tax_number", length = 50)
    private String taxNumber;
    
    @Column(name = "commercial_registration", length = 50)
    private String commercialRegistration;
    
    // ========== Localization Configuration ==========
    
    @Column(name = "timezone", length = 50)
    private String timezone = "Asia/Riyadh";
    
    @Column(name = "locale", length = 10)
    private String locale = "ar_SA";
    
    @Column(name = "currency", length = 3)
    private String currency = "SAR";
    
    // ========== Subscription & Limits ==========
    
    @Column(name = "subscription_plan", nullable = false, columnDefinition = "VARCHAR(50)")
    @NotNull(message = "Subscription plan is required")
    @Convert(converter = SubscriptionPlanConverter.class)
    @org.hibernate.annotations.ColumnTransformer(write = "?::subscription_plan")
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.FREE;
    
    @Column(name = "subscription_start_date")
    private LocalDate subscriptionStartDate;
    
    @Column(name = "subscription_end_date")
    private LocalDate subscriptionEndDate;
    
    @Column(name = "max_users", nullable = false)
    @Min(value = 1, message = "Max users must be at least 1")
    private Integer maxUsers = 10;
    
    @Column(name = "max_schools", nullable = false)
    @Min(value = 1, message = "Max schools must be at least 1")
    private Integer maxSchools = 5;
    
    @Column(name = "max_supervisors", nullable = false)
    @Min(value = 1, message = "Max supervisors must be at least 1")
    private Integer maxSupervisors = 3;
    
    @Column(name = "max_technicians", nullable = false)
    @Min(value = 1, message = "Max technicians must be at least 1")
    private Integer maxTechnicians = 15;
    
    @Column(name = "max_storage_gb", nullable = false)
    @Min(value = 1, message = "Max storage must be at least 1 GB")
    private Integer maxStorageGb = 5;
    
    // ========== Status & Configuration ==========
    
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(50)")
    @NotNull(message = "Company status is required")
    @Convert(converter = CompanyStatusConverter.class)
    @org.hibernate.annotations.ColumnTransformer(write = "?::company_status")
    private CompanyStatus status = CompanyStatus.PENDING_SETUP;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "settings", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String settings = "{}";
    
    @Column(name = "features", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String features = "{}";
    
    // ========== Security & Compliance ==========
    
    @Column(name = "data_classification", length = 50)
    private String dataClassification = "internal";
    
    @Column(name = "compliance_requirements", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String complianceRequirements = "{}";
    
    // ========== Relationships ==========
    
    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();
    
    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private Set<School> schools = new HashSet<>();
    
    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private Set<Report> reports = new HashSet<>();
    
    // ========== Constructors ==========
    
    public Company() {
        super();
    }
    
    public Company(String name, String domain, SubscriptionPlan subscriptionPlan) {
        this();
        this.name = name;
        this.domain = domain;
        this.subscriptionPlan = subscriptionPlan;
        this.subscriptionStartDate = LocalDate.now();
    }
    
    public Company(String name, String displayName, String subdomain) {
        this();
        this.name = name;
        this.displayName = displayName;
        this.subdomain = subdomain;
    }
    
    // ========== Business Methods ==========
    
    /**
     * Check if company is active and accessible
     */
    public boolean isAccessible() {
        return Boolean.TRUE.equals(isActive) && 
               status == CompanyStatus.ACTIVE && 
               !isDeleted();
    }
    
    /**
     * Check if subscription is active
     */
    public boolean hasActiveSubscription() {
        LocalDate now = LocalDate.now();
        return subscriptionEndDate == null || subscriptionEndDate.isAfter(now);
    }
    
    /**
     * Check if company is in trial period
     */
    public boolean isInTrial() {
        return status == CompanyStatus.TRIAL;
    }
    
    /**
     * Check if company has exceeded user limit
     */
    public boolean hasExceededUserLimit() {
        return users.size() >= maxUsers;
    }
    
    /**
     * Check if company has exceeded school limit
     */
    public boolean hasExceededSchoolLimit() {
        return schools.size() >= maxSchools;
    }
    
    /**
     * Get available user slots
     */
    public Integer getAvailableUserSlots() {
        return Math.max(0, maxUsers - users.size());
    }
    
    /**
     * Get available school slots
     */
    public Integer getAvailableSchoolSlots() {
        return Math.max(0, maxSchools - schools.size());
    }
    
    /**
     * Check if feature is enabled for this company
     */
    public boolean hasFeature(String featureName) {
        if (features == null || features.isEmpty()) return false;
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(features);
            JsonNode featureNode = rootNode.get(featureName);
            return featureNode != null && featureNode.asBoolean();
        } catch (Exception e) {
            // Log error and return false if JSON parsing fails
            return false;
        }
    }
    
    /**
     * Get display name (falls back to name if display name is null)
     */
    public String getDisplayName() {
        return displayName != null && !displayName.trim().isEmpty() ? displayName : name;
    }
    
    /**
     * Get full subdomain URL
     */
    public String getSubdomainUrl(String baseDomain) {
        if (subdomain == null) return null;
        return subdomain + "." + baseDomain;
    }
    
    /**
     * Activate company
     */
    public void activate() {
        this.status = CompanyStatus.ACTIVE;
        this.isActive = true;
    }
    
    /**
     * Suspend company
     */
    public void suspend() {
        this.status = CompanyStatus.SUSPENDED;
        this.isActive = false;
    }
    
    /**
     * Update subscription plan
     */
    public void updateSubscription(SubscriptionPlan newPlan, 
                                 LocalDate startDate, 
                                 LocalDate endDate,
                                 Integer users,
                                 Integer schools) {
        this.subscriptionPlan = newPlan;
        this.subscriptionStartDate = startDate;
        this.subscriptionEndDate = endDate;
        if (users != null) this.maxUsers = users;
        if (schools != null) this.maxSchools = schools;
    }
    
    /**
     * Get subscription status
     */
    public String getSubscriptionStatus() {
        if (!hasActiveSubscription()) {
            return "EXPIRED";
        }
        if (isInTrial()) {
            return "TRIAL";
        }
        return subscriptionPlan.name();
    }
    
    // ========== Getters and Setters ==========
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public String getSubdomain() {
        return subdomain;
    }
    
    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }
    
    public String getContactEmail() {
        return contactEmail;
    }
    
    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
    
    public String getContactPhone() {
        return contactPhone;
    }
    
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
    
    public String getPrimaryContactName() {
        return primaryContactName;
    }
    
    public void setPrimaryContactName(String primaryContactName) {
        this.primaryContactName = primaryContactName;
    }
    
    public String getIndustry() {
        return industry;
    }
    
    public void setIndustry(String industry) {
        this.industry = industry;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getPostalCode() {
        return postalCode;
    }
    
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }
    
    public String getTaxNumber() {
        return taxNumber;
    }
    
    public void setTaxNumber(String taxNumber) {
        this.taxNumber = taxNumber;
    }
    
    public String getCommercialRegistration() {
        return commercialRegistration;
    }
    
    public void setCommercialRegistration(String commercialRegistration) {
        this.commercialRegistration = commercialRegistration;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public String getLocale() {
        return locale;
    }
    
    public void setLocale(String locale) {
        this.locale = locale;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public SubscriptionPlan getSubscriptionPlan() {
        return subscriptionPlan;
    }
    
    public void setSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }
    
    public LocalDate getSubscriptionStartDate() {
        return subscriptionStartDate;
    }
    
    public void setSubscriptionStartDate(LocalDate subscriptionStartDate) {
        this.subscriptionStartDate = subscriptionStartDate;
    }
    
    public LocalDate getSubscriptionEndDate() {
        return subscriptionEndDate;
    }
    
    public void setSubscriptionEndDate(LocalDate subscriptionEndDate) {
        this.subscriptionEndDate = subscriptionEndDate;
    }
    
    public Integer getMaxUsers() {
        return maxUsers;
    }
    
    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }
    
    public Integer getMaxSchools() {
        return maxSchools;
    }
    
    public void setMaxSchools(Integer maxSchools) {
        this.maxSchools = maxSchools;
    }
    
    public Integer getMaxSupervisors() {
        return maxSupervisors;
    }
    
    public void setMaxSupervisors(Integer maxSupervisors) {
        this.maxSupervisors = maxSupervisors;
    }
    
    public Integer getMaxTechnicians() {
        return maxTechnicians;
    }
    
    public void setMaxTechnicians(Integer maxTechnicians) {
        this.maxTechnicians = maxTechnicians;
    }
    
    public Integer getMaxStorageGb() {
        return maxStorageGb;
    }
    
    public void setMaxStorageGb(Integer maxStorageGb) {
        this.maxStorageGb = maxStorageGb;
    }
    
    public CompanyStatus getStatus() {
        return status;
    }
    
    public void setStatus(CompanyStatus status) {
        this.status = status;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public String getSettings() {
        return settings;
    }
    
    public void setSettings(String settings) {
        this.settings = settings;
    }
    
    public String getFeatures() {
        return features;
    }
    
    public void setFeatures(String features) {
        this.features = features;
    }
    
    public String getDataClassification() {
        return dataClassification;
    }
    
    public void setDataClassification(String dataClassification) {
        this.dataClassification = dataClassification;
    }
    
    public String getComplianceRequirements() {
        return complianceRequirements;
    }
    
    public void setComplianceRequirements(String complianceRequirements) {
        this.complianceRequirements = complianceRequirements;
    }
    
    public Set<User> getUsers() {
        return users;
    }
    
    public void setUsers(Set<User> users) {
        this.users = users;
    }
    
    public Set<School> getSchools() {
        return schools;
    }
    
    public void setSchools(Set<School> schools) {
        this.schools = schools;
    }
    
    public Set<Report> getReports() {
        return reports;
    }
    
    public void setReports(Set<Report> reports) {
        this.reports = reports;
    }
    
    // ========== toString, equals, hashCode ==========
    
    @Override
    public String toString() {
        return String.format("Company[id=%s, name=%s, domain=%s, status=%s, plan=%s]",
            getId(), name, domain, status, subscriptionPlan);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Company company)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(name, company.name) && 
               Objects.equals(domain, company.domain);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, domain);
    }
}