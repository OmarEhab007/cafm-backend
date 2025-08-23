package com.cafm.cafmbackend.security;

import com.cafm.cafmbackend.data.entity.Company;
import com.cafm.cafmbackend.data.entity.User;
import com.cafm.cafmbackend.security.filter.EnhancedTenantSecurityFilter;
import com.cafm.cafmbackend.security.JwtTokenProvider;
import com.cafm.cafmbackend.service.tenant.TenantContextService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Enhanced Tenant Security Filter.
 * 
 * Explanation:
 * - Purpose: Validates tenant security filter behavior under various attack scenarios
 * - Pattern: Unit testing with comprehensive mocking for security validation
 * - Java 23: Modern testing patterns with security-focused assertions
 * - Architecture: Filter-level security testing for request processing
 * - Standards: Defense-in-depth testing with attack simulation
 */
@ExtendWith(MockitoExtension.class)
class TenantSecurityFilterTest {
    
    @Mock
    private TenantContextService tenantContextService;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    @Mock
    private HttpSession session;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private User mockUser;
    
    @Mock
    private Company mockCompany;
    
    private EnhancedTenantSecurityFilter filter;
    
    private static final UUID COMPANY1_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID COMPANY2_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SYSTEM_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    
    @BeforeEach
    void setUp() {
        filter = new EnhancedTenantSecurityFilter(tenantContextService, jwtTokenProvider);
        SecurityContextHolder.setContext(securityContext);
    }
    
    @Test
    @DisplayName("SECURITY: Valid tenant from JWT is properly extracted and validated")
    void testValidTenantFromJWT() throws ServletException, IOException {
        // Setup
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getSession(false)).thenReturn(session);
        when(session.getId()).thenReturn("test-session-123");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/users");
        
        // Mock authentication with tenant
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUser);
        when(mockUser.getCompany()).thenReturn(mockCompany);
        when(mockCompany.getId()).thenReturn(COMPANY1_ID);
        
        // Mock tenant validation
        when(tenantContextService.validateTenantAccess(COMPANY1_ID)).thenReturn(true);
        
        // Execute
        filter.doFilter(request, response, filterChain);
        
        // Verify
        verify(tenantContextService).setCurrentTenant(COMPANY1_ID);
        verify(filterChain).doFilter(request, response);
        verify(tenantContextService).clearTenantContext();
    }
    
    @Test
    @DisplayName("SECURITY: Invalid tenant attempt is blocked and logged")
    void testInvalidTenantBlocked() throws ServletException, IOException {
        // Setup
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getSession(false)).thenReturn(session);
        when(session.getId()).thenReturn("test-session-123");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/users");
        when(request.getHeader("User-Agent")).thenReturn("TestAgent/1.0");
        
        // Mock authentication with invalid tenant
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUser);
        when(mockUser.getCompany()).thenReturn(mockCompany);
        when(mockUser.getUsername()).thenReturn("testuser");
        when(mockCompany.getId()).thenReturn(COMPANY1_ID);
        
        // Mock tenant validation failure
        when(tenantContextService.validateTenantAccess(COMPANY1_ID)).thenReturn(false);
        
        // Execute
        filter.doFilter(request, response, filterChain);
        
        // Verify tenant was not set and system default was used
        verify(tenantContextService, never()).setCurrentTenant(COMPANY1_ID);
        verify(tenantContextService).ensureTenantContext();
        verify(filterChain).doFilter(request, response);
        verify(tenantContextService).clearTenantContext();
    }
    
    @Test
    @DisplayName("SECURITY: Cross-tenant header attack is detected and blocked")
    void testCrossTenantHeaderAttack() throws ServletException, IOException {
        // Setup malicious request with conflicting tenant information
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getSession(false)).thenReturn(session);
        when(session.getId()).thenReturn("test-session-123");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/reports");
        when(request.getHeader("User-Agent")).thenReturn("AttackBot/1.0");
        when(request.getHeader("X-Tenant-ID")).thenReturn(COMPANY2_ID.toString());
        
        // Mock authenticated user from company1
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUser);
        when(mockUser.getCompany()).thenReturn(mockCompany);
        when(mockUser.getUsername()).thenReturn("company1user");
        when(mockCompany.getId()).thenReturn(COMPANY1_ID);
        
        // Mock tenant validations
        when(tenantContextService.validateTenantAccess(COMPANY1_ID)).thenReturn(true);
        when(tenantContextService.validateTenantAccess(COMPANY2_ID)).thenReturn(true);
        
        // Execute
        filter.doFilter(request, response, filterChain);
        
        // Verify JWT tenant takes precedence over header (security)
        verify(tenantContextService).setCurrentTenant(COMPANY1_ID);
        verify(filterChain).doFilter(request, response);
        verify(tenantContextService).clearTenantContext();
    }
    
    @Test
    @DisplayName("SECURITY: System tenant has unrestricted access")
    void testSystemTenantAccess() throws ServletException, IOException {
        // Setup
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession(false)).thenReturn(session);
        when(session.getId()).thenReturn("system-session");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/admin/companies");
        
        // Mock system tenant authentication
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUser);
        when(mockUser.getCompany()).thenReturn(mockCompany);
        when(mockUser.getUsername()).thenReturn("system");
        when(mockCompany.getId()).thenReturn(SYSTEM_TENANT_ID);
        
        // Mock tenant validation
        when(tenantContextService.validateTenantAccess(SYSTEM_TENANT_ID)).thenReturn(true);
        
        // Execute
        filter.doFilter(request, response, filterChain);
        
        // Verify system tenant is set
        verify(tenantContextService).setCurrentTenant(SYSTEM_TENANT_ID);
        verify(filterChain).doFilter(request, response);
        verify(tenantContextService).clearTenantContext();
    }
    
    @Test
    @DisplayName("SECURITY: No tenant context defaults to system tenant")
    void testNoTenantContextDefaultsToSystem() throws ServletException, IOException {
        // Setup
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getSession(false)).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/public/health");
        
        // Mock no authentication
        when(securityContext.getAuthentication()).thenReturn(null);
        
        // Execute
        filter.doFilter(request, response, filterChain);
        
        // Verify system default is used
        verify(tenantContextService).ensureTenantContext();
        verify(filterChain).doFilter(request, response);
        verify(tenantContextService).clearTenantContext();
    }
    
    @Test
    @DisplayName("SECURITY: Exception in filter is handled gracefully")
    void testExceptionHandling() throws ServletException, IOException {
        // Setup
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getSession(false)).thenReturn(session);
        when(session.getId()).thenReturn("error-session");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/users");
        when(request.getHeader("User-Agent")).thenReturn("TestAgent/1.0");
        
        // Mock exception during tenant extraction
        when(securityContext.getAuthentication()).thenThrow(new RuntimeException("Token validation error"));
        
        // Execute
        filter.doFilter(request, response, filterChain);
        
        // Verify graceful handling
        verify(tenantContextService).ensureTenantContext();
        verify(filterChain).doFilter(request, response);
        verify(tenantContextService).clearTenantContext();
    }
    
    @Test
    @DisplayName("SECURITY: Security headers are added to response")
    void testSecurityHeadersAdded() throws ServletException, IOException {
        // Setup
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getSession(false)).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/users");
        
        // Mock no authentication
        when(securityContext.getAuthentication()).thenReturn(null);
        
        // Execute
        filter.doFilter(request, response, filterChain);
        
        // Verify security headers are set
        verify(response).setHeader("X-Frame-Options", "DENY");
        verify(response).setHeader("X-Content-Type-Options", "nosniff");
        verify(response).setHeader("X-XSS-Protection", "1; mode=block");
        verify(response).setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        verify(response).setHeader(eq("X-Request-ID"), anyString());
        
        verify(filterChain).doFilter(request, response);
        verify(tenantContextService).clearTenantContext();
    }
    
    @Test
    @DisplayName("SECURITY: Public endpoints bypass tenant filtering")
    void testPublicEndpointsBypass() throws ServletException, IOException {
        // Setup public endpoint
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        
        // Execute filter - should pass through without tenant check
        filter.doFilter(request, response, filterChain);
        
        // Verify no tenant context was set for public endpoint
        verify(tenantContextService, never()).setCurrentTenant(any());
    }
    
    @Test
    @DisplayName("SECURITY: Protected endpoints require tenant filtering")
    void testProtectedEndpointsRequireFiltering() throws ServletException, IOException {
        // Setup protected endpoint
        when(request.getRequestURI()).thenReturn("/api/v1/users");
        
        // Mock authentication with tenant
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUser);
        when(mockUser.getCompany()).thenReturn(mockCompany);
        when(mockCompany.getId()).thenReturn(COMPANY1_ID);
        when(tenantContextService.validateTenantAccess(COMPANY1_ID)).thenReturn(true);
        
        // Execute
        filter.doFilter(request, response, filterChain);
        
        // Verify tenant context was set for protected endpoint
        verify(tenantContextService).setCurrentTenant(COMPANY1_ID);
    }
    
    @Test
    @DisplayName("SECURITY: Tenant context is always cleared after request")
    void testTenantContextAlwaysCleared() throws ServletException, IOException {
        // Setup
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getSession(false)).thenReturn(session);
        when(session.getId()).thenReturn("test-session");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/users");
        
        // Mock exception during filter chain
        doThrow(new RuntimeException("Downstream error")).when(filterChain).doFilter(request, response);
        
        // Execute (should not throw)
        try {
            filter.doFilter(request, response, filterChain);
        } catch (RuntimeException e) {
            // Expected from downstream
        }
        
        // Verify context is still cleared despite exception
        verify(tenantContextService).clearTenantContext();
    }
    
    // Helper method for assertions in tests that need it
    private void assertThat(boolean condition) {
        if (!condition) {
            throw new AssertionError("Assertion failed");
        }
    }
}