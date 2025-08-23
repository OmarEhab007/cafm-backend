package com.cafm.cafmbackend.data.entity.base;

import com.cafm.cafmbackend.data.entity.Company;
import com.cafm.cafmbackend.data.repository.CompanyRepository;
import com.cafm.cafmbackend.service.tenant.TenantContextService;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * JPA Entity Listener for automatic tenant assignment and validation.
 * 
 * Explanation:
 * - Purpose: Provides automatic tenant context management for all tenant-aware entities
 * - Pattern: Observer pattern via JPA lifecycle callbacks with dependency injection
 * - Java 23: Modern exception handling and logging patterns
 * - Architecture: Cross-cutting concern for tenant security enforcement
 * - Standards: Centralized tenant validation logic with comprehensive audit logging
 */
@Component
@Configurable
public class TenantEntityListener {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantEntityListener.class);
    private static final String SYSTEM_TENANT_ID = "00000000-0000-0000-0000-000000000001";
    
    @Autowired
    private TenantContextService tenantContextService;
    
    @Autowired
    private CompanyRepository companyRepository;
    
    /**
     * Pre-persist callback for tenant assignment and validation
     */
    @PrePersist
    public void prePersist(Object entity) {
        if (!(entity instanceof TenantAware tenantEntity)) {
            return; // Not a tenant-aware entity
        }
        
        logger.debug("Pre-persist tenant validation for entity: {}", entity.getClass().getSimpleName());
        
        try {
            // Ensure tenant assignment
            ensureTenantAssignment(tenantEntity);
            
            // Validate tenant context
            validateTenantOperation(tenantEntity, "CREATE");
            
            // Log security operation
            logSecurityOperation(tenantEntity, "CREATE", "SUCCESS");
            
        } catch (Exception e) {
            logSecurityOperation(tenantEntity, "CREATE", "FAILED: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Pre-update callback for tenant validation
     */
    @PreUpdate
    public void preUpdate(Object entity) {
        if (!(entity instanceof TenantAware tenantEntity)) {
            return;
        }
        
        logger.debug("Pre-update tenant validation for entity: {}", entity.getClass().getSimpleName());
        
        try {
            // Validate tenant context for update
            validateTenantOperation(tenantEntity, "UPDATE");
            
            // Ensure company assignment hasn't been tampered with
            validateCompanyIntegrity(tenantEntity);
            
            logSecurityOperation(tenantEntity, "UPDATE", "SUCCESS");
            
        } catch (Exception e) {
            logSecurityOperation(tenantEntity, "UPDATE", "FAILED: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Pre-remove callback for tenant validation
     */
    @PreRemove
    public void preRemove(Object entity) {
        if (!(entity instanceof TenantAware tenantEntity)) {
            return;
        }
        
        logger.debug("Pre-remove tenant validation for entity: {}", entity.getClass().getSimpleName());
        
        try {
            // Validate tenant context for deletion
            validateTenantOperation(tenantEntity, "DELETE");
            
            logSecurityOperation(tenantEntity, "DELETE", "SUCCESS");
            
        } catch (Exception e) {
            logSecurityOperation(tenantEntity, "DELETE", "FAILED: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Post-load callback for tenant validation on read operations
     */
    @PostLoad
    public void postLoad(Object entity) {
        if (!(entity instanceof TenantAware tenantEntity)) {
            return;
        }
        
        // Skip validation if no tenant context (system operations)
        if (tenantContextService == null || !tenantContextService.hasTenantContext()) {
            return;
        }
        
        try {
            // Validate that loaded entity belongs to current tenant
            validateTenantAccess(tenantEntity);
            
            logger.debug("Post-load tenant validation passed for entity: {} (tenant: {})",
                entity.getClass().getSimpleName(), tenantEntity.getCompanyId());
                
        } catch (Exception e) {
            logger.error("Post-load tenant validation failed for entity: {} - {}",
                entity.getClass().getSimpleName(), e.getMessage());
                
            // Log but don't throw - RLS should have prevented this
            logSecurityOperation(tenantEntity, "READ", "VALIDATION_FAILED: " + e.getMessage());
        }
    }
    
    // ========== Private Helper Methods ==========
    
    /**
     * Ensure the entity has a valid tenant assignment
     */
    private void ensureTenantAssignment(TenantAware entity) {
        if (entity.getCompany() != null) {
            return; // Already assigned
        }
        
        // Get current tenant context
        if (tenantContextService == null) {
            throw new IllegalStateException("TenantContextService not available for entity assignment");
        }
        
        if (!tenantContextService.hasTenantContext()) {
            throw new IllegalStateException(
                "Cannot persist tenant-aware entity without tenant context. " +
                "Ensure tenant context is set before persisting entities."
            );
        }
        
        UUID currentTenantId = tenantContextService.getCurrentTenant();
        
        // Get the company entity
        Company company = companyRepository.findById(currentTenantId)
            .orElseThrow(() -> new IllegalStateException(
                "Current tenant company not found: " + currentTenantId
            ));
        
        // Assign company to entity
        entity.setCompany(company);
        
        logger.debug("Auto-assigned entity to tenant: {} ({})", 
            company.getName(), currentTenantId);
    }
    
    /**
     * Validate tenant operation is allowed
     */
    private void validateTenantOperation(TenantAware entity, String operation) {
        if (entity.getCompany() == null) {
            throw new SecurityException(
                "Tenant violation: Cannot " + operation + " entity without company assignment"
            );
        }
        
        // Skip validation for system operations
        if (tenantContextService == null || !tenantContextService.hasTenantContext()) {
            logger.debug("Skipping tenant validation for system operation: {}", operation);
            return;
        }
        
        UUID currentTenantId = tenantContextService.getCurrentTenant();
        UUID entityTenantId = entity.getCompanyId();
        
        // Allow system tenant to perform any operation
        if (SYSTEM_TENANT_ID.equals(currentTenantId.toString())) {
            logger.debug("System tenant operation allowed: {}", operation);
            return;
        }
        
        // Validate tenant match
        if (!currentTenantId.equals(entityTenantId)) {
            String errorMsg = String.format(
                "Tenant isolation violation: Tenant %s attempting %s on entity belonging to tenant %s",
                currentTenantId, operation, entityTenantId
            );
            
            logger.error(errorMsg);
            throw new SecurityException(errorMsg);
        }
        
        // Validate tenant is still active
        if (!tenantContextService.validateTenantAccess(currentTenantId)) {
            String errorMsg = String.format(
                "Tenant access violation: Tenant %s is not active or accessible",
                currentTenantId
            );
            
            logger.error(errorMsg);
            throw new SecurityException(errorMsg);
        }
        
        logger.debug("Tenant operation validated: {} by tenant {}", operation, currentTenantId);
    }
    
    /**
     * Validate tenant access for read operations
     */
    private void validateTenantAccess(TenantAware entity) {
        if (entity.getCompany() == null) {
            logger.warn("Loaded entity has no company assignment - possible data integrity issue");
            return;
        }
        
        UUID currentTenantId = tenantContextService.getCurrentTenant();
        UUID entityTenantId = entity.getCompanyId();
        
        // Allow system tenant to read anything
        if (SYSTEM_TENANT_ID.equals(currentTenantId.toString())) {
            return;
        }
        
        // Validate tenant match
        if (!currentTenantId.equals(entityTenantId)) {
            String errorMsg = String.format(
                "Tenant data leak detected: Tenant %s loaded entity belonging to tenant %s",
                currentTenantId, entityTenantId
            );
            
            logger.error(errorMsg);
            // Note: We log but don't throw here as RLS should prevent this
            // If we reach here, there's likely a configuration issue with RLS
        }
    }
    
    /**
     * Validate that company assignment hasn't been tampered with
     */
    private void validateCompanyIntegrity(TenantAware entity) {
        if (entity.getCompany() == null) {
            throw new SecurityException("Tenant violation: Company assignment cannot be null during update");
        }
        
        // Additional integrity checks can be added here
        // For example, verify that the company exists and is active
        UUID companyId = entity.getCompanyId();
        
        if (!companyRepository.existsById(companyId)) {
            throw new SecurityException("Tenant violation: Referenced company does not exist: " + companyId);
        }
    }
    
    /**
     * Log security operations for audit trail
     */
    private void logSecurityOperation(TenantAware entity, String operation, String result) {
        try {
            UUID currentTenant = tenantContextService != null && tenantContextService.hasTenantContext()
                ? tenantContextService.getCurrentTenant()
                : null;
                
            UUID entityTenant = entity.getCompanyId();
            
            // Use structured logging for security audit
            logger.info("SECURITY_AUDIT: operation={}, entity={}, entityTenant={}, currentTenant={}, result={}",
                operation,
                entity.getClass().getSimpleName(),
                entityTenant,
                currentTenant,
                result
            );
            
        } catch (Exception e) {
            // Don't fail the operation due to logging issues
            logger.error("Failed to log security operation: {}", e.getMessage());
        }
    }
}