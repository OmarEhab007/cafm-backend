package com.cafm.cafmbackend.service;

import com.cafm.cafmbackend.data.entity.User;
import com.cafm.cafmbackend.data.repository.UserRepository;
import com.cafm.cafmbackend.exception.ResourceNotFoundException;
import com.cafm.cafmbackend.service.tenant.TenantContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing current user context and tenant information.
 * 
 * Purpose: Provides centralized access to current user and tenant information
 * Pattern: Service layer with security context integration
 * Java 23: Uses Optional and modern patterns for null safety
 * Architecture: Bridges security context with tenant context
 * Standards: Thread-safe and transaction-aware implementation
 */
@Service
@Transactional(readOnly = true)
public class CurrentUserService {
    
    private static final Logger logger = LoggerFactory.getLogger(CurrentUserService.class);
    
    private final UserRepository userRepository;
    private final TenantContextService tenantContextService;
    
    public CurrentUserService(UserRepository userRepository, 
                            TenantContextService tenantContextService) {
        this.userRepository = userRepository;
        this.tenantContextService = tenantContextService;
    }
    
    /**
     * Get the current authenticated user from security context
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }
        
        String username;
        
        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else if (authentication.getPrincipal() instanceof String) {
            username = (String) authentication.getPrincipal();
        } else {
            throw new IllegalStateException("Unable to extract username from authentication");
        }
        
        final String finalUsername = username;
        return userRepository.findByEmail(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + finalUsername));
    }
    
    /**
     * Get the current user's ID
     */
    public UUID getCurrentUserId() {
        User currentUser = getCurrentUser();
        return currentUser.getId();
    }
    
    /**
     * Get the current user's company ID
     */
    public UUID getCurrentUserCompanyId() {
        User currentUser = getCurrentUser();
        
        if (currentUser.getCompany() == null) {
            logger.warn("Current user {} has no company assigned", currentUser.getEmail());
            return tenantContextService.getCurrentTenant(); // Fall back to tenant context
        }
        
        return currentUser.getCompany().getId();
    }
    
    /**
     * Get company ID from UserDetails (for use in controllers)
     */
    public UUID getCompanyIdFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("UserDetails cannot be null");
        }
        
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userDetails.getUsername()));
        
        if (user.getCompany() == null) {
            logger.warn("User {} has no company assigned", user.getEmail());
            return tenantContextService.getCurrentTenant(); // Fall back to tenant context
        }
        
        return user.getCompany().getId();
    }
    
    /**
     * Ensure tenant context is set based on current user
     */
    public UUID ensureTenantContext() {
        try {
            UUID companyId = getCurrentUserCompanyId();
            tenantContextService.setCurrentTenant(companyId);
            return companyId;
        } catch (Exception e) {
            logger.warn("Could not set tenant context from current user: {}", e.getMessage());
            return tenantContextService.ensureTenantContext();
        }
    }
    
    /**
     * Ensure tenant context is set from UserDetails
     */
    public UUID ensureTenantContext(UserDetails userDetails) {
        try {
            UUID companyId = getCompanyIdFromUserDetails(userDetails);
            tenantContextService.setCurrentTenant(companyId);
            return companyId;
        } catch (Exception e) {
            logger.warn("Could not set tenant context from user details: {}", e.getMessage());
            return tenantContextService.ensureTenantContext();
        }
    }
    
    /**
     * Check if current user belongs to a specific company
     */
    public boolean belongsToCompany(UUID companyId) {
        try {
            UUID userCompanyId = getCurrentUserCompanyId();
            return userCompanyId.equals(companyId);
        } catch (Exception e) {
            logger.error("Error checking company membership: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if current user is a system admin
     */
    public boolean isSystemAdmin() {
        try {
            User currentUser = getCurrentUser();
            return currentUser.getUserType() == com.cafm.cafmbackend.data.enums.UserType.ADMIN 
                && currentUser.getRoles().stream()
                    .anyMatch(role -> "SUPER_ADMIN".equals(role.getName()) || "SYSTEM_ADMIN".equals(role.getName()));
        } catch (Exception e) {
            logger.error("Error checking system admin status: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get current tenant ID (alias for getCurrentUserCompanyId)
     */
    public UUID getCurrentTenantId() {
        return getCurrentUserCompanyId();
    }
    
    /**
     * Get current user's email
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        
        return null;
    }
    
    /**
     * Check if there is an authenticated user
     */
    public boolean hasAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
            && !"anonymousUser".equals(authentication.getPrincipal());
    }
}