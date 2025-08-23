package com.cafm.cafmbackend.service.security;

import com.cafm.cafmbackend.data.entity.User;
import com.cafm.cafmbackend.data.enums.UserStatus;
import com.cafm.cafmbackend.data.enums.UserType;
import com.cafm.cafmbackend.data.repository.TenantAwareUserRepository;
import com.cafm.cafmbackend.service.tenant.TenantContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SECURITY ENHANCEMENT: Secure User Service with Tenant Isolation
 * 
 * Purpose: Provides secure user operations with automatic tenant enforcement
 * Pattern: Service layer with embedded tenant security validation  
 * Java 23: Modern service patterns with enhanced security checks
 * Architecture: Security-first business service for user operations
 * Standards: All user operations require tenant validation, comprehensive audit logging
 * 
 * CRITICAL SECURITY FEATURES:
 * - Automatic tenant context validation on all operations
 * - Prevents cross-tenant user access at service layer
 * - Comprehensive audit logging for security events
 * - Fail-safe design - denies access when tenant context is unclear
 */
@Service
@Transactional(readOnly = true)
public class SecureUserService {
    
    private static final Logger logger = LoggerFactory.getLogger(SecureUserService.class);
    
    private final TenantAwareUserRepository userRepository;
    private final TenantContextService tenantContextService;
    
    @Autowired
    public SecureUserService(TenantAwareUserRepository userRepository, 
                           TenantContextService tenantContextService) {
        this.userRepository = userRepository;
        this.tenantContextService = tenantContextService;
    }
    
    // ========== SECURE USER LOOKUP METHODS ==========
    
    /**
     * SECURITY: Find user by email with automatic tenant isolation
     * Purpose: Prevents cross-tenant access, requires tenant context
     */
    public Optional<User> findByEmail(String email) {
        UUID tenantId = getCurrentTenantIdOrThrow();
        
        logger.debug("Secure lookup: Finding user by email for tenant {}", tenantId);
        Optional<User> user = userRepository.findByEmailIgnoreCaseAndCompanyId(email, tenantId);
        
        if (user.isPresent()) {
            logger.debug("Secure lookup: Found user {} for tenant {}", user.get().getId(), tenantId);
        } else {
            logger.debug("Secure lookup: No user found with email for tenant {}", tenantId);
        }
        
        return user;
    }
    
    /**
     * SECURITY: Find user by username with automatic tenant isolation
     */
    public Optional<User> findByUsername(String username) {
        UUID tenantId = getCurrentTenantIdOrThrow();
        
        logger.debug("Secure lookup: Finding user by username for tenant {}", tenantId);
        Optional<User> user = userRepository.findByUsernameIgnoreCaseAndCompanyId(username, tenantId);
        
        if (user.isPresent()) {
            logger.debug("Secure lookup: Found user {} for tenant {}", user.get().getId(), tenantId);
        } else {
            logger.debug("Secure lookup: No user found with username for tenant {}", tenantId);
        }
        
        return user;
    }
    
    /**
     * SECURITY: Find user by email OR username for authentication
     * Critical method used during login - must be tenant-safe
     */
    public Optional<User> findByEmailOrUsername(String identifier) {
        UUID tenantId = getCurrentTenantIdOrThrow();
        
        logger.info("Secure authentication lookup: Finding user by identifier for tenant {}", tenantId);
        Optional<User> user = userRepository.findByEmailOrUsernameAndCompanyId(identifier, tenantId);
        
        if (user.isPresent()) {
            logger.info("Secure authentication lookup: Found user {} for tenant {}", user.get().getId(), tenantId);
            // Additional security: verify user is active and not locked
            User foundUser = user.get();
            if (foundUser.getIsLocked() || !foundUser.getIsActive()) {
                logger.warn("Security: Found user {} but account is locked/inactive", foundUser.getId());
            }
        } else {
            logger.warn("Security: Authentication attempt with unknown identifier for tenant {}", tenantId);
        }
        
        return user;
    }
    
    /**
     * SECURITY: Find user by employee ID with tenant isolation
     */
    public Optional<User> findByEmployeeId(String employeeId) {
        UUID tenantId = getCurrentTenantIdOrThrow();
        
        logger.debug("Secure lookup: Finding user by employee ID for tenant {}", tenantId);
        return userRepository.findByEmployeeIdAndCompanyId(employeeId, tenantId);
    }
    
    // ========== SECURE EXISTENCE CHECKS ==========
    
    /**
     * SECURITY: Check if email exists within current tenant only
     * Used for user registration validation
     */
    public boolean existsByEmail(String email) {
        UUID tenantId = getCurrentTenantIdOrThrow();
        
        logger.debug("Secure check: Verifying email existence for tenant {}", tenantId);
        boolean exists = userRepository.existsByEmailIgnoreCaseAndCompanyId(email, tenantId);
        
        if (exists) {
            logger.debug("Secure check: Email already exists for tenant {}", tenantId);
        }
        
        return exists;
    }
    
    /**
     * SECURITY: Check if username exists within current tenant only
     */
    public boolean existsByUsername(String username) {
        UUID tenantId = getCurrentTenantIdOrThrow();
        
        logger.debug("Secure check: Verifying username existence for tenant {}", tenantId);
        boolean exists = userRepository.existsByUsernameIgnoreCaseAndCompanyId(username, tenantId);
        
        if (exists) {
            logger.debug("Secure check: Username already exists for tenant {}", tenantId);
        }
        
        return exists;
    }
    
    /**
     * SECURITY: Check if employee ID exists within current tenant only
     */
    public boolean existsByEmployeeId(String employeeId) {
        UUID tenantId = getCurrentTenantIdOrThrow();
        
        logger.debug("Secure check: Verifying employee ID existence for tenant {}", tenantId);
        return userRepository.existsByEmployeeIdAndCompanyId(employeeId, tenantId);
    }
    
    // ========== SECURE USER QUERIES ==========
    
    /**
     * SECURITY: Find users by type within current tenant only
     */
    public List<User> findByUserType(UserType userType) {
        UUID tenantId = getCurrentTenantIdOrThrow();
        
        logger.debug("Secure query: Finding users by type {} for tenant {}", userType, tenantId);
        return userRepository.findByUserTypeAndCompanyId(userType, tenantId);
    }
    
    /**
     * SECURITY: Find active users by type within current tenant only
     */
    public List<User> findActiveByUserType(UserType userType) {
        UUID tenantId = getCurrentTenantIdOrThrow();
        
        logger.debug("Secure query: Finding active users by type {} for tenant {}", userType, tenantId);
        return userRepository.findActiveByUserTypeAndCompanyId(userType, UserStatus.ACTIVE, tenantId);
    }
    
    // ========== SECURITY VALIDATION METHODS ==========
    
    /**
     * CRITICAL SECURITY: Validate that a user belongs to current tenant
     * Must be called before any user-related operations
     */
    public boolean validateUserAccess(UUID userId) {
        UUID tenantId = getCurrentTenantIdOrThrow();
        
        boolean hasAccess = userRepository.validateUserBelongsToTenant(userId, tenantId);
        
        if (!hasAccess) {
            logger.error("SECURITY VIOLATION: Attempted access to user {} from tenant {} - ACCESS DENIED", 
                        userId, tenantId);
        } else {
            logger.debug("Security validation: User {} access granted for tenant {}", userId, tenantId);
        }
        
        return hasAccess;
    }
    
    /**
     * CRITICAL SECURITY: Validate multiple users belong to current tenant
     */
    public boolean validateUsersAccess(List<UUID> userIds) {
        UUID tenantId = getCurrentTenantIdOrThrow();
        
        boolean hasAccess = userRepository.validateAllUsersBelongToTenant(userIds, tenantId, userIds.size());
        
        if (!hasAccess) {
            logger.error("SECURITY VIOLATION: Attempted bulk access to users from tenant {} - ACCESS DENIED", tenantId);
        } else {
            logger.debug("Security validation: Bulk user access granted for tenant {}", tenantId);
        }
        
        return hasAccess;
    }
    
    // ========== TENANT STATISTICS ==========
    
    /**
     * Get user count by status within current tenant
     */
    public long countByStatus(UserStatus status) {
        UUID tenantId = getCurrentTenantIdOrThrow();
        return userRepository.countByStatusAndCompanyId(status, tenantId);
    }
    
    /**
     * Get user count by type within current tenant
     */
    public long countByUserType(UserType userType) {
        UUID tenantId = getCurrentTenantIdOrThrow();
        return userRepository.countByUserTypeAndCompanyId(userType, tenantId);
    }
    
    // ========== PRIVATE HELPER METHODS ==========
    
    /**
     * Get current tenant ID or throw security exception
     * Ensures all operations have tenant context
     */
    private UUID getCurrentTenantIdOrThrow() {
        if (!tenantContextService.hasTenantContext()) {
            logger.error("SECURITY ERROR: Attempted user operation without tenant context");
            throw new SecurityException("User operations require tenant context");
        }
        
        UUID tenantId = tenantContextService.getCurrentTenant();
        if (tenantId == null) {
            logger.error("SECURITY ERROR: Tenant context exists but tenant ID is null");
            throw new SecurityException("Invalid tenant context");
        }
        
        return tenantId;
    }
}