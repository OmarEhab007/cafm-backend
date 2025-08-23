package com.cafm.cafmbackend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

/**
 * Security headers configuration.
 * 
 * Purpose: Configure security headers to protect against common attacks
 * Pattern: Spring Security headers configuration
 * Java 23: Enhanced security configuration
 * Architecture: Security layer configuration
 * Standards: OWASP security headers best practices
 */
@Configuration
public class SecurityHeadersConfig {
    
    @Value("${app.security.csp.enabled:true}")
    private boolean cspEnabled;
    
    @Value("${app.security.csp.report-uri:}")
    private String cspReportUri;
    
    @Value("${app.security.hsts.enabled:true}")
    private boolean hstsEnabled;
    
    @Value("${app.security.hsts.max-age:31536000}")
    private long hstsMaxAge;
    
    /**
     * Configure security headers
     */
    public void configureSecurityHeaders(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            // X-Content-Type-Options
            .contentTypeOptions(contentType -> {})
            
            // X-Frame-Options
            .frameOptions(frame -> frame.sameOrigin())
            
            // X-XSS-Protection
            .xssProtection(xss -> xss
                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
            
            // Referrer-Policy
            .referrerPolicy(referrer -> referrer
                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            
            // Permissions-Policy (formerly Feature-Policy)
            .permissionsPolicy(permissions -> permissions
                .policy("geolocation=(), microphone=(), camera=(), payment=(), usb=()"))
        );
        
        // Content-Security-Policy
        if (cspEnabled) {
            http.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(getContentSecurityPolicy()))
            );
        }
        
        // HTTP Strict Transport Security (HSTS)
        if (hstsEnabled) {
            http.headers(headers -> headers
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(hstsMaxAge)
                    .includeSubDomains(true)
                    .preload(true))
            );
        }
    }
    
    /**
     * Build Content Security Policy
     */
    private String getContentSecurityPolicy() {
        StringBuilder csp = new StringBuilder();
        
        // Default source
        csp.append("default-src 'self'; ");
        
        // Script sources
        csp.append("script-src 'self' 'unsafe-inline' 'unsafe-eval' ")
           .append("https://cdn.jsdelivr.net ")
           .append("https://cdnjs.cloudflare.com; ");
        
        // Style sources
        csp.append("style-src 'self' 'unsafe-inline' ")
           .append("https://fonts.googleapis.com ")
           .append("https://cdn.jsdelivr.net; ");
        
        // Image sources
        csp.append("img-src 'self' data: blob: ")
           .append("https://*.cloudinary.com ")
           .append("https://avatars.githubusercontent.com; ");
        
        // Font sources
        csp.append("font-src 'self' data: ")
           .append("https://fonts.gstatic.com; ");
        
        // Connect sources (API calls)
        csp.append("connect-src 'self' ")
           .append("https://api.github.com ")
           .append("wss://localhost:* ")
           .append("ws://localhost:*; ");
        
        // Frame sources
        csp.append("frame-src 'none'; ");
        
        // Object sources
        csp.append("object-src 'none'; ");
        
        // Media sources
        csp.append("media-src 'self'; ");
        
        // Child sources
        csp.append("child-src 'self'; ");
        
        // Form action
        csp.append("form-action 'self'; ");
        
        // Frame ancestors
        csp.append("frame-ancestors 'none'; ");
        
        // Base URI
        csp.append("base-uri 'self'; ");
        
        // Upgrade insecure requests
        csp.append("upgrade-insecure-requests; ");
        
        // Report URI
        if (cspReportUri != null && !cspReportUri.isEmpty()) {
            csp.append("report-uri ").append(cspReportUri).append("; ");
        }
        
        return csp.toString();
    }
    
    /**
     * Custom security headers filter for additional headers
     */
    @Bean
    public CustomSecurityHeadersFilter customSecurityHeadersFilter() {
        return new CustomSecurityHeadersFilter();
    }
    
    /**
     * Inner class for custom security headers
     */
    public static class CustomSecurityHeadersFilter extends org.springframework.web.filter.OncePerRequestFilter {
        
        @Override
        protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                      jakarta.servlet.http.HttpServletResponse response,
                                      jakarta.servlet.FilterChain filterChain) 
                                      throws jakarta.servlet.ServletException, java.io.IOException {
            
            // Add custom security headers
            response.setHeader("X-Permitted-Cross-Domain-Policies", "none");
            response.setHeader("X-Download-Options", "noopen");
            response.setHeader("X-DNS-Prefetch-Control", "off");
            
            // Remove server header
            response.setHeader("Server", "");
            
            // Add cache control for sensitive endpoints
            String path = request.getRequestURI();
            if (path.startsWith("/api/") && !path.contains("/public/")) {
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
            }
            
            filterChain.doFilter(request, response);
        }
    }
}