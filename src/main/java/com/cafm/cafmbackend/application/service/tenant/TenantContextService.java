package com.cafm.cafmbackend.application.service.tenant;

import com.cafm.cafmbackend.infrastructure.persistence.entity.Company;
import com.cafm.cafmbackend.infrastructure.persistence.repository.CompanyRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing tenant context in multi-tenant application.
 * 
 * Architecture: Multi-tenant context management with Row-Level Security
 * Pattern: Thread-local tenant context with database session integration
 * Java 23: Modern exception handling and validation patterns
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS)
public class TenantContextService {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantContextService.class);
    
    private static final String DEFAULT_SYSTEM_COMPANY_ID = "00000000-0000-0000-0000-000000000001";
    private static final String TENANT_CONTEXT_KEY = "app.current_company_id";
    
    private static final ThreadLocal<UUID> currentTenantId = new ThreadLocal<>();
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private final CompanyRepository companyRepository;
    
    @Autowired
    public TenantContextService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }
    
    // ========== Core Tenant Context Operations ==========
    
    /**
     * Set the current tenant context for the current thread
     */
    public void setCurrentTenant(UUID companyId) {
        if (companyId == null) {
            logger.warn("Attempted to set null tenant ID, using system default");
            companyId = UUID.fromString(DEFAULT_SYSTEM_COMPANY_ID);
        }
        
        logger.debug("Setting tenant context to: {}", companyId);
        
        currentTenantId.set(companyId);
        
        // Set database-level tenant context for RLS policies
        setDatabaseTenantContext(companyId);
    }
    
    /**
     * Get the current tenant ID from thread-local context
     */
    public UUID getCurrentTenant() {
        UUID tenantId = currentTenantId.get();
        if (tenantId == null) {
            logger.debug("No tenant context found, using system default");
            tenantId = UUID.fromString(DEFAULT_SYSTEM_COMPANY_ID);
            setCurrentTenant(tenantId);
        }
        return tenantId;
    }
    
    /**
     * Get the current tenant company entity
     */
    public Optional<Company> getCurrentTenantCompany() {
        UUID tenantId = getCurrentTenant();
        return companyRepository.findById(tenantId);
    }
    
    /**
     * Clear the tenant context for the current thread
     */
    public void clearTenantContext() {
        logger.debug("Clearing tenant context");
        currentTenantId.remove();
        clearDatabaseTenantContext();
    }
    
    /**
     * Check if tenant context is set
     */
    public boolean hasTenantContext() {
        return currentTenantId.get() != null;
    }
    
    /**
     * Check if current tenant is the system default
     */
    public boolean isSystemTenant() {
        UUID tenantId = getCurrentTenant();
        return DEFAULT_SYSTEM_COMPANY_ID.equals(tenantId.toString());
    }
    
    // ========== Tenant Validation ==========
    
    /**
     * Validate if a tenant is active and accessible
     */
    public boolean validateTenantAccess(UUID companyId) {
        if (companyId == null) {
            return false;
        }
        
        try {
            Optional<Company> company = companyRepository.findById(companyId);
            if (company.isEmpty()) {
                logger.warn("Tenant not found: {}", companyId);
                return false;
            }
            
            Company companyEntity = company.get();
            boolean isAccessible = companyEntity.isAccessible();
            
            if (!isAccessible) {
                logger.warn("Tenant is not accessible: {} (status: {}, active: {})", 
                           companyId, companyEntity.getStatus(), companyEntity.getIsActive());
            }
            
            return isAccessible;
            
        } catch (Exception e) {
            logger.error("Error validating tenant access for {}: {}", companyId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Validate current tenant access
     */
    public boolean validateCurrentTenantAccess() {
        return validateTenantAccess(getCurrentTenant());
    }
    
    /**
     * Switch to a different tenant with validation
     */
    public boolean switchTenant(UUID newTenantId) {
        if (!validateTenantAccess(newTenantId)) {
            logger.warn("Cannot switch to invalid tenant: {}", newTenantId);
            return false;
        }
        
        UUID previousTenant = getCurrentTenant();
        setCurrentTenant(newTenantId);
        
        logger.info("Switched tenant from {} to {}", previousTenant, newTenantId);
        return true;
    }
    
    // ========== Database Context Management ==========
    
    /**
     * Set database-level tenant context for RLS policies
     */
    private void setDatabaseTenantContext(UUID companyId) {
        try {
            entityManager.createNativeQuery(
                "SELECT set_config(?, ?, false)"
            )
            .setParameter(1, TENANT_CONTEXT_KEY)
            .setParameter(2, companyId.toString())
            .getSingleResult();
            
            logger.debug("Set database tenant context to: {}", companyId);
            
        } catch (Exception e) {
            logger.error("Failed to set database tenant context: {}", e.getMessage());
            // Don't throw - fall back to application-level filtering
        }
    }
    
    /**
     * Clear database-level tenant context
     */
    private void clearDatabaseTenantContext() {
        try {
            entityManager.createNativeQuery(
                "SELECT set_config(?, ?, false)"
            )
            .setParameter(1, TENANT_CONTEXT_KEY)
            .setParameter(2, "")
            .getSingleResult();
            
            logger.debug("Cleared database tenant context");
            
        } catch (Exception e) {
            logger.error("Failed to clear database tenant context: {}", e.getMessage());
        }
    }
    
    /**
     * Get database-level tenant context
     */
    public UUID getDatabaseTenantContext() {
        try {
            Object result = entityManager.createNativeQuery(
                "SELECT current_setting(?, true)"
            )
            .setParameter(1, TENANT_CONTEXT_KEY)
            .getSingleResult();
            
            if (result != null && !result.toString().isEmpty()) {
                return UUID.fromString(result.toString());
            }
        } catch (Exception e) {
            logger.debug("No database tenant context found: {}", e.getMessage());
        }
        
        return null;
    }
    
    // ========== Tenant Context Utilities ==========
    
    /**
     * Execute code block with specific tenant context
     */
    public <T> T executeWithTenant(UUID tenantId, TenantOperation<T> operation) {
        UUID previousTenant = hasTenantContext() ? getCurrentTenant() : null;
        
        try {
            setCurrentTenant(tenantId);
            return operation.execute();
        } finally {
            if (previousTenant != null) {
                setCurrentTenant(previousTenant);
            } else {
                clearTenantContext();
            }
        }
    }
    
    /**
     * Execute code block with system tenant context
     */
    public <T> T executeWithSystemTenant(TenantOperation<T> operation) {
        return executeWithTenant(UUID.fromString(DEFAULT_SYSTEM_COMPANY_ID), operation);
    }
    
    /**
     * Check if a resource belongs to current tenant
     */
    public boolean belongsToCurrentTenant(UUID resourceCompanyId) {
        if (resourceCompanyId == null) {
            return false;
        }
        
        UUID currentTenant = getCurrentTenant();
        return currentTenant.equals(resourceCompanyId);
    }
    
    /**
     * Ensure tenant context is set, using system default if not
     */
    public UUID ensureTenantContext() {
        if (!hasTenantContext()) {
            setCurrentTenant(UUID.fromString(DEFAULT_SYSTEM_COMPANY_ID));
        }
        return getCurrentTenant();
    }
    
    /**
     * Get tenant information summary
     */
    public TenantInfo getTenantInfo() {
        UUID tenantId = getCurrentTenant();
        Optional<Company> company = getCurrentTenantCompany();
        
        return new TenantInfo(
            tenantId,
            company.map(Company::getName).orElse("Unknown"),
            company.map(Company::getStatus).orElse(null),
            company.map(Company::getSubscriptionPlan).orElse(null),
            isSystemTenant(),
            validateCurrentTenantAccess()
        );
    }
    
    // ========== Helper Classes ==========
    
    /**
     * Functional interface for tenant operations
     */
    @FunctionalInterface
    public interface TenantOperation<T> {
        T execute();
    }
    
    /**
     * Tenant information record
     */
    public record TenantInfo(
        UUID tenantId,
        String companyName,
        com.cafm.cafmbackend.shared.enums.CompanyStatus status,
        com.cafm.cafmbackend.shared.enums.SubscriptionPlan subscriptionPlan,
        boolean isSystemTenant,
        boolean isAccessible
    ) {}
}