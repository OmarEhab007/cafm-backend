package com.cafm.cafmbackend.data.entity.base;

import com.cafm.cafmbackend.data.entity.Company;
import com.cafm.cafmbackend.service.tenant.TenantContextService;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

/**
 * Abstract base entity that provides tenant awareness for multi-tenant entities.
 * 
 * Explanation:
 * - Purpose: Extends SoftDeletableEntity with tenant isolation capabilities
 * - Pattern: Template pattern for tenant-aware entity inheritance with EntityListener delegation
 * - Java 23: Uses modern JPA 3.1 features and lifecycle callbacks without field injection
 * - Architecture: Core abstraction for multi-tenant data layer with proper dependency injection
 * - Standards: Automatic tenant assignment and validation via TenantEntityListener (no field injection)
 */
@MappedSuperclass
@EntityListeners({TenantEntityListener.class})
public abstract class TenantAwareEntity extends SoftDeletableEntity implements TenantAware {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantAwareEntity.class);
    
    /**
     * Company/tenant relationship - lazy loaded for performance
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    @NotNull(message = "Company is required for tenant isolation")
    protected Company company;
    
    /**
     * Default constructor for JPA
     */
    protected TenantAwareEntity() {
        super();
    }
    
    /**
     * Constructor with company assignment
     * @param company Company to assign to this entity
     */
    protected TenantAwareEntity(Company company) {
        super();
        this.company = company;
    }
    
    // ========== TenantAware Implementation ==========
    
    @Override
    public Company getCompany() {
        return company;
    }
    
    @Override
    public void setCompany(Company company) {
        this.company = company;
    }
    
    // ========== JPA Lifecycle Callbacks ==========
    // Note: All lifecycle callbacks are now handled by TenantEntityListener
    // This eliminates field injection and centralizes tenant logic
    
    /**
     * Pre-persist callback - delegates to parent only
     */
    @PrePersist
    protected void onPrePersist() {
        super.onCreate(); // Call parent lifecycle - tenant handling in listener
    }
    
    /**
     * Pre-update callback - delegates to parent only
     */
    @PreUpdate
    protected void onPreUpdate() {
        super.onUpdate(); // Call parent lifecycle - tenant handling in listener
    }
    
    /**
     * Pre-remove callback - delegates to parent only
     */
    @PreRemove
    protected void onPreRemove() {
        super.onRemove(); // Call parent lifecycle - tenant handling in listener
    }
    
    // ========== Security Helper Methods ==========
    // Note: These methods provide basic tenant information without requiring service injection
    
    /**
     * Check if this entity belongs to the system tenant
     * @return true if system tenant entity
     */
    public boolean isSystemTenantEntity() {
        return company != null && 
               "00000000-0000-0000-0000-000000000001".equals(company.getId().toString());
    }
    
    // ========== Enhanced toString with tenant info ==========
    
    @Override
    public String toString() {
        return String.format("%s[tenant=%s]",
            super.toString(),
            company != null ? company.getId() : "null"
        );
    }
    
    // ========== Equals and HashCode (tenant-aware) ==========
    
    /**
     * Tenant-aware equals that considers company assignment
     */
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        
        // Additional tenant check for extra security
        if (obj instanceof TenantAware other) {
            UUID thisCompanyId = this.getCompanyId();
            UUID otherCompanyId = other.getCompanyId();
            
            // Both must have the same tenant or both be null
            if (thisCompanyId == null && otherCompanyId == null) {
                return true;
            }
            
            return thisCompanyId != null && thisCompanyId.equals(otherCompanyId);
        }
        
        return true; // Non-tenant entities are considered equal if base equals passes
    }
}