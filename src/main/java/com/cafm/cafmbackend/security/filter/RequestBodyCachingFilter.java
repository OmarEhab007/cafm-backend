package com.cafm.cafmbackend.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to cache request body for multiple reads.
 * 
 * Purpose: Prevents request body consumption issues in filter chain
 * Pattern: Wrapper pattern for HttpServletRequest
 * Java 23: Modern servlet filter implementation
 * Architecture: Cross-cutting concern for request processing
 * Standards: Spring Boot filter best practices
 */
@Component
@Order(0) // Run first in the filter chain
public class RequestBodyCachingFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestBodyCachingFilter.class);
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                   @NonNull HttpServletResponse response,
                                   @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        // Only wrap POST, PUT, PATCH requests with body
        if (isRequestWithBody(request)) {
            try {
                // Use our custom wrapper that properly caches the request body
                CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
                
                // Log the request for debugging
                String path = request.getRequestURI();
                String method = request.getMethod();
                int bodySize = cachedRequest.getCachedBody().length;
                
                logger.debug("Caching request body for {} {} - Body size: {} bytes", 
                           method, path, bodySize);
                
                if (logger.isTraceEnabled() && bodySize > 0 && bodySize < 10000) {
                    logger.trace("Request body content: {}", cachedRequest.getCachedBodyAsString());
                }
                
                filterChain.doFilter(cachedRequest, response);
            } catch (IOException e) {
                logger.error("Error caching request body", e);
                // If we can't cache the body, pass the original request
                filterChain.doFilter(request, response);
            }
        } else {
            // Pass through for GET, DELETE, etc.
            filterChain.doFilter(request, response);
        }
    }
    
    /**
     * Check if request method typically has a body.
     */
    private boolean isRequestWithBody(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method) ||
               "PUT".equalsIgnoreCase(method) ||
               "PATCH".equalsIgnoreCase(method);
    }
    
    /**
     * Skip filter for certain paths.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip for static resources and health checks
        return path.startsWith("/actuator/") ||
               path.startsWith("/health") ||
               path.equals("/favicon.ico") ||
               path.startsWith("/swagger-") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/webjars/");
    }
}