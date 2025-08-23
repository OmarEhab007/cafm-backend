package com.cafm.cafmbackend.security.filter;

import com.cafm.cafmbackend.security.service.EnhancedLoginAttemptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting filter specifically for authentication endpoints.
 * 
 * Security Features:
 * - Progressive delays after failed attempts
 * - IP-based blocking for distributed attacks
 * - Clear error messages with retry information
 * - Integration with login attempt tracking
 */
@Component
@Order(1) // Run before JWT authentication
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationRateLimitFilter.class);
    
    private final EnhancedLoginAttemptService loginAttemptService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public AuthenticationRateLimitFilter(EnhancedLoginAttemptService loginAttemptService,
                                        ObjectMapper objectMapper) {
        this.loginAttemptService = loginAttemptService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                   @NonNull HttpServletResponse response,
                                   @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        // Only apply to login endpoint
        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String ipAddress = loginAttemptService.getClientIP(request);
        String username = extractUsername(request);
        
        // Check if request should be blocked or delayed
        EnhancedLoginAttemptService.LoginAttemptResult attemptResult = 
            loginAttemptService.checkLoginAttempt(username != null ? username : ipAddress, ipAddress);
        
        if (attemptResult.blocked()) {
            // Request is blocked
            handleBlockedRequest(response, attemptResult);
            return;
        }
        
        if (attemptResult.delaySeconds() > 0) {
            // Apply progressive delay
            try {
                logger.info("Applying {} second delay for login attempt from IP: {}", 
                           attemptResult.delaySeconds(), ipAddress);
                TimeUnit.SECONDS.sleep(attemptResult.delaySeconds());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Delay interrupted", e);
            }
        }
        
        // Continue with the request
        filterChain.doFilter(request, response);
    }
    
    /**
     * Check if this is a login request.
     */
    private boolean isLoginRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        return "POST".equalsIgnoreCase(method) && 
               (path.equals("/api/v1/auth/login") || 
                path.equals("/api/v1/auth/signin") ||
                path.equals("/api/auth/login"));
    }
    
    /**
     * Extract username from login request.
     */
    private String extractUsername(HttpServletRequest request) {
        try {
            // Try to get from cached request body if available
            if (request instanceof CachedBodyHttpServletRequest cachedRequest) {
                String body = new String(cachedRequest.getCachedBody());
                Map<String, Object> loginData = objectMapper.readValue(body, Map.class);
                
                // Try different possible field names
                Object username = loginData.get("username");
                if (username == null) {
                    username = loginData.get("email");
                }
                if (username == null) {
                    username = loginData.get("login");
                }
                
                return username != null ? username.toString() : null;
            }
            
            // Fallback to request parameters
            String username = request.getParameter("username");
            if (username == null) {
                username = request.getParameter("email");
            }
            if (username == null) {
                username = request.getParameter("login");
            }
            
            return username;
            
        } catch (Exception e) {
            logger.debug("Could not extract username from request", e);
            return null;
        }
    }
    
    /**
     * Handle blocked request with appropriate error response.
     */
    private void handleBlockedRequest(HttpServletResponse response,
                                     EnhancedLoginAttemptService.LoginAttemptResult attemptResult) 
            throws IOException {
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        // Add retry-after header
        Duration remainingTime = attemptResult.getRemainingLockTime();
        if (remainingTime != null && !remainingTime.isZero()) {
            response.setHeader("Retry-After", String.valueOf(remainingTime.getSeconds()));
        }
        
        // Create error response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        errorResponse.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.put("error", "Too Many Requests");
        errorResponse.put("message", attemptResult.reason() != null ? 
                         attemptResult.reason() : "Too many login attempts");
        errorResponse.put("path", "/api/v1/auth/login");
        
        // Add additional details
        Map<String, Object> details = new HashMap<>();
        if (attemptResult.lockExpiry() != null) {
            details.put("lockExpiry", attemptResult.lockExpiry().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            details.put("remainingSeconds", remainingTime.getSeconds());
            details.put("remainingMinutes", remainingTime.toMinutes());
        }
        errorResponse.put("details", details);
        
        // Write response
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        
        // Log the blocked attempt
        logger.warn("Blocked login attempt: {}", attemptResult.reason());
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only apply to authentication endpoints
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/auth/") && 
               !path.startsWith("/api/auth/");
    }
}