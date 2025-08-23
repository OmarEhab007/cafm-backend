package com.cafm.cafmbackend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT authentication entry point for handling unauthorized access.
 * Returns proper 401 responses with error details.
 * 
 * Architecture: Spring Security authentication error handler
 * Pattern: AuthenticationEntryPoint implementation
 * Java 23: Modern exception handling and response formatting
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        logger.error("Unauthorized access attempt: {}", authException.getMessage());
        
        // Check for specific error conditions
        String errorMessage = determineErrorMessage(request, authException);
        String errorCode = determineErrorCode(request);
        
        // Build error response
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now().toString());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("error", "Unauthorized");
        errorDetails.put("code", errorCode);
        errorDetails.put("message", errorMessage);
        errorDetails.put("status", HttpStatus.UNAUTHORIZED.value());
        
        // Add additional context if available
        Object tokenStatus = request.getAttribute("token_status");
        if (tokenStatus != null) {
            errorDetails.put("tokenStatus", tokenStatus);
        }
        
        // Set response properties
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        // Write response
        objectMapper.writeValue(response.getOutputStream(), errorDetails);
    }
    
    /**
     * Determine the appropriate error message based on the exception and request.
     */
    private String determineErrorMessage(HttpServletRequest request, AuthenticationException authException) {
        // Check if this is a public endpoint that shouldn't require authentication
        String path = request.getRequestURI();
        if (isPublicEndpoint(path)) {
            // For public endpoints, don't complain about missing/invalid auth headers
            return "Authentication is not required for this endpoint";
        }
        
        // Check for token expiration
        String tokenStatus = (String) request.getAttribute("token_status");
        if ("expired".equals(tokenStatus)) {
            return "JWT token has expired. Please login again.";
        }
        
        // Check if Authorization header is present
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isEmpty()) {
            return "Authorization header is missing. Please provide a valid JWT token.";
        }
        
        // Check if token format is correct
        if (!authHeader.startsWith("Bearer ")) {
            return "Invalid authorization header format. Expected 'Bearer <token>'";
        }
        
        // Check for specific exception messages
        String exceptionMessage = authException.getMessage();
        if (exceptionMessage != null) {
            if (exceptionMessage.contains("expired")) {
                return "Authentication token has expired";
            } else if (exceptionMessage.contains("invalid")) {
                return "Authentication token is invalid";
            } else if (exceptionMessage.contains("disabled")) {
                return "User account is disabled";
            } else if (exceptionMessage.contains("locked")) {
                return "User account is locked";
            }
        }
        
        // Default message
        return "Full authentication is required to access this resource";
    }
    
    /**
     * Determine the appropriate error code based on the request.
     */
    private String determineErrorCode(HttpServletRequest request) {
        // Check if this is a public endpoint
        String path = request.getRequestURI();
        if (isPublicEndpoint(path)) {
            return "PUBLIC_ENDPOINT";
        }
        
        String tokenStatus = (String) request.getAttribute("token_status");
        if ("expired".equals(tokenStatus)) {
            return "TOKEN_EXPIRED";
        }
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isEmpty()) {
            return "MISSING_AUTH_HEADER";
        }
        
        if (!authHeader.startsWith("Bearer ")) {
            return "INVALID_AUTH_FORMAT";
        }
        
        return "AUTHENTICATION_FAILED";
    }
    
    /**
     * Check if the given path is a public endpoint that doesn't require authentication.
     */
    private boolean isPublicEndpoint(String path) {
        return path.equals("/api/v1/auth/login") ||
               path.equals("/api/v1/auth/refresh") ||
               path.equals("/api/v1/auth/forgot-password") ||
               path.equals("/api/v1/auth/reset-password") ||
               path.startsWith("/api/v1/auth/verify-email") ||
               path.startsWith("/public/") ||
               path.startsWith("/health") ||
               path.startsWith("/actuator/") ||
               path.equals("/favicon.ico") ||
               path.equals("/swagger-ui.html") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/swagger-resources") ||
               path.startsWith("/webjars/");
    }
}