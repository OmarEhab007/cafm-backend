package com.cafm.cafmbackend.security.filter;

import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import com.cafm.cafmbackend.security.JwtTokenProvider;
import com.cafm.cafmbackend.application.service.tenant.TenantContextService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Security filter to extract and set tenant context from HTTP requests.
 * 
 * Architecture: Multi-tenant security filter with JWT integration
 * Pattern: Servlet filter for cross-cutting tenant concerns
 * Java 23: Modern exception handling and validation
 * Order: Runs after JwtAuthenticationFilter to use established authentication
 */
@Component
@Order(2) // Run after JWT authentication
public class TenantSecurityFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantSecurityFilter.class);
    
    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String COMPANY_HEADER = "X-Company-ID";
    private static final String SUBDOMAIN_HEADER = "X-Subdomain";
    
    private final TenantContextService tenantContextService;
    private final JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    public TenantSecurityFilter(TenantContextService tenantContextService,
                               JwtTokenProvider jwtTokenProvider) {
        this.tenantContextService = tenantContextService;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                   @NonNull HttpServletResponse response, 
                                   @NonNull FilterChain filterChain) 
            throws ServletException, IOException {
        
        try {
            // Extract tenant context from various sources
            UUID tenantId = extractTenantFromRequest(request);
            
            if (tenantId != null) {
                // Validate tenant access before setting context
                if (tenantContextService.validateTenantAccess(tenantId)) {
                    tenantContextService.setCurrentTenant(tenantId);
                    logger.debug("Set tenant context from request: {}", tenantId);
                } else {
                    logger.warn("Invalid tenant ID in request: {}", tenantId);
                    // Continue with system default tenant
                    tenantContextService.ensureTenantContext();
                }
            } else {
                // Ensure system default tenant is set
                tenantContextService.ensureTenantContext();
                logger.debug("No tenant context in request, using system default");
            }
            
            // Continue with the filter chain
            filterChain.doFilter(request, response);
            
            // Add tenant information to response headers (optional)
            addTenantHeaders(response);
            
        } catch (Exception e) {
            logger.error("Error in tenant security filter", e);
            // Ensure system default tenant and continue
            tenantContextService.ensureTenantContext();
            filterChain.doFilter(request, response);
            
        } finally {
            // Clean up tenant context after request
            tenantContextService.clearTenantContext();
        }
    }
    
    /**
     * Extract tenant ID from various sources in the HTTP request
     * 
     * Security: Only JWT token is trusted in production
     * Headers and parameters are disabled to prevent tenant bypass attacks
     */
    private UUID extractTenantFromRequest(HttpServletRequest request) {
        UUID tenantId = null;
        
        // 1. ALWAYS try to get from JWT token claims (ONLY trusted source)
        tenantId = extractTenantFromJwt();
        if (tenantId != null) {
            logger.debug("Extracted tenant from JWT: {}", tenantId);
            return tenantId;
        }
        
        // 2. Try direct JWT token extraction as fallback
        tenantId = extractTenantFromJwtToken(request);
        if (tenantId != null) {
            logger.debug("Extracted tenant from JWT token directly: {}", tenantId);
            return tenantId;
        }
        
        // 3. Try to get from subdomain (safe for multi-tenant routing)
        tenantId = extractTenantFromSubdomain(request);
        if (tenantId != null) {
            logger.debug("Extracted tenant from subdomain: {}", tenantId);
            return tenantId;
        }
        
        // Headers and parameters are DISABLED for security
        // These methods are now blocked to prevent tenant bypass attacks
        
        logger.debug("No tenant context found in request");
        return null;
    }
    
    /**
     * Extract tenant ID from JWT token claims
     */
    private UUID extractTenantFromJwt() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            
            // Extract company ID from authenticated user
            if (authentication.getPrincipal() instanceof User user) {
                if (user.getCompany() != null) {
                    return user.getCompany().getId();
                }
            }
            
            return null;
            
        } catch (Exception e) {
            logger.debug("Could not extract tenant from JWT: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract tenant ID from JWT token string directly
     */
    private UUID extractTenantFromJwtToken(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                UUID companyId = jwtTokenProvider.getCompanyIdFromToken(token);
                if (companyId != null) {
                    logger.debug("Extracted company ID from JWT token: {}", companyId);
                    return companyId;
                }
            }
            return null;
        } catch (Exception e) {
            logger.debug("Could not extract tenant from JWT token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract tenant ID from HTTP headers - DISABLED FOR SECURITY
     * 
     * This method is retained for backwards compatibility but always returns null
     * to prevent tenant bypass attacks via header manipulation.
     * 
     * @deprecated Headers should not be used for tenant identification
     */
    @Deprecated
    private UUID extractTenantFromHeaders(HttpServletRequest request) {
        // Log any attempts to use headers for tenant override
        String tenantHeader = request.getHeader(TENANT_HEADER);
        String companyHeader = request.getHeader(COMPANY_HEADER);
        
        if (tenantHeader != null || companyHeader != null) {
            logger.warn("SECURITY: Attempted tenant override via headers detected from IP: {} User-Agent: {}",
                       request.getRemoteAddr(), request.getHeader("User-Agent"));
            
            // Log more details for security monitoring
            logger.warn("Attempted override - Tenant Header: {}, Company Header: {}, Request URI: {}",
                       tenantHeader, companyHeader, request.getRequestURI());
        }
        
        // Always return null - headers are not trusted for tenant identification
        return null;
    }
    
    /**
     * Extract tenant ID from subdomain
     */
    private UUID extractTenantFromSubdomain(HttpServletRequest request) {
        try {
            String serverName = request.getServerName();
            if (serverName == null) {
                return null;
            }
            
            // Parse subdomain (e.g., company1.cafm.example.com -> company1)
            String[] parts = serverName.split("\\.");
            if (parts.length < 3) {
                return null; // No subdomain
            }
            
            String subdomain = parts[0];
            if ("www".equals(subdomain) || "api".equals(subdomain)) {
                return null; // Skip common subdomains
            }
            
            // TODO: Look up company by subdomain when needed
            // This requires a service call to resolve subdomain to company ID
            /*
            Optional<Company> company = companyRepository.findBySubdomainAndDeletedAtIsNull(subdomain);
            if (company.isPresent()) {
                return company.get().getId();
            }
            */
            
            return null;
            
        } catch (Exception e) {
            logger.debug("Could not extract tenant from subdomain: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract tenant ID from request parameter - COMPLETELY DISABLED
     * 
     * This method is completely disabled to prevent tenant bypass attacks.
     * Request parameters must NEVER be used for tenant identification.
     * 
     * @deprecated Never use request parameters for tenant identification
     */
    @Deprecated
    private UUID extractTenantFromParameter(HttpServletRequest request) {
        // Log any attempts to use parameters for tenant override
        String tenantParam = request.getParameter("tenant_id");
        String companyParam = request.getParameter("company_id");
        
        if (tenantParam != null || companyParam != null) {
            // This is a potential security breach attempt
            logger.error("SECURITY ALERT: Attempted tenant override via request parameters from IP: {} User-Agent: {}",
                        request.getRemoteAddr(), request.getHeader("User-Agent"));
            
            logger.error("Attack details - Tenant Param: {}, Company Param: {}, Request URI: {}, Method: {}",
                        tenantParam, companyParam, request.getRequestURI(), request.getMethod());
            
            // Consider triggering security alerts or blocking the IP
            // You might want to integrate with your security monitoring system here
        }
        
        // ALWAYS return null - parameters are NEVER trusted for tenant identification
        return null;
    }
    
    /**
     * Should the filter be applied to this request?
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip tenant filtering for public endpoints
        return path.startsWith("/public/") ||
               path.startsWith("/health") ||
               path.startsWith("/actuator/") ||
               path.equals("/favicon.ico") ||
               path.startsWith("/swagger-") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/webjars/") ||
               // Skip auth endpoints that don't require tenant context
               path.equals("/api/v1/auth/login") ||
               path.equals("/api/v1/auth/refresh") ||
               path.equals("/api/v1/auth/forgot-password") ||
               path.equals("/api/v1/auth/reset-password") ||
               path.equals("/api/v1/auth/verify-email");
    }
    
    /**
     * Add tenant information to response headers
     */
    private void addTenantHeaders(HttpServletResponse response) {
        try {
            if (tenantContextService.hasTenantContext()) {
                UUID currentTenant = tenantContextService.getCurrentTenant();
                response.setHeader(TENANT_HEADER, currentTenant.toString());
                
                // Add additional tenant info for debugging
                TenantContextService.TenantInfo tenantInfo = tenantContextService.getTenantInfo();
                response.setHeader("X-Tenant-Name", tenantInfo.companyName());
                response.setHeader("X-Tenant-Status", tenantInfo.status() != null ? tenantInfo.status().name() : "UNKNOWN");
            }
        } catch (Exception e) {
            logger.debug("Could not add tenant headers to response: {}", e.getMessage());
        }
    }
}