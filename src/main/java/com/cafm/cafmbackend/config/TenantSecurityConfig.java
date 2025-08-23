package com.cafm.cafmbackend.config;

import com.cafm.cafmbackend.security.filter.EnhancedTenantSecurityFilter;
import com.cafm.cafmbackend.service.audit.TenantSecurityAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Central configuration for tenant security integration.
 * 
 * CRITICAL SECURITY CONFIGURATION:
 * - Purpose: Integrates all tenant security components into Spring Security chain
 * - Pattern: Configuration class with comprehensive security filter chain
 * - Java 23: Modern Spring Security 6.x configuration patterns
 * - Architecture: Central integration point for all tenant security features
 * - Standards: Production-ready security configuration with full tenant isolation
 */
@Configuration
// @EnableWebSecurity // Removed to avoid duplicate security configuration
@EnableAspectJAutoProxy
public class TenantSecurityConfig {
    
    @Autowired
    private EnhancedTenantSecurityFilter enhancedTenantSecurityFilter;
    
    @Autowired
    private TenantSecurityAuditService auditService;
    
    /**
     * Configure security filter chain with tenant security
     * Commented out to avoid duplicate SecurityFilterChain bean - configuration is handled in SecurityConfig
     */
    // @Bean
    // public SecurityFilterChain tenantSecurityFilterChain(HttpSecurity http) throws Exception {
    //     return http
    //         // Add enhanced tenant security filter after authentication
    //         .addFilterAfter(enhancedTenantSecurityFilter, UsernamePasswordAuthenticationFilter.class)
    //         
    //         // Configure request authorization with tenant awareness
    //         .authorizeHttpRequests(authz -> authz
    //             .requestMatchers("/api/v1/auth/**").permitAll()
    //             .requestMatchers("/api/v1/public/**").permitAll()
    //             .requestMatchers("/health", "/actuator/**").permitAll()
    //             .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
    //             .anyRequest().authenticated()
    //         )
    //         
    //         // Session management
    //         .sessionManagement(session -> session
    //             .maximumSessions(1)
    //             .maxSessionsPreventsLogin(false)
    //         )
    //         
    //         .build();
    // }
}