package com.cafm.cafmbackend.security.filter;

import com.cafm.cafmbackend.data.entity.ApiKey;
import com.cafm.cafmbackend.data.repository.ApiKeyRepository;
//import com.cafm.cafmbackend.security.TenantContext;
import com.cafm.cafmbackend.service.tenant.TenantContextService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Filter for API key authentication.
 * 
 * Purpose: Authenticate external clients using API keys
 * Pattern: Servlet filter for API key validation
 * Java 23: Efficient string processing
 * Architecture: Security layer filter
 * Standards: API key in X-API-Key header
 */
@Component
@Order(2)
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";
    
    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantContextService tenantContextService;
    
    @Autowired
    public ApiKeyAuthenticationFilter(@Lazy ApiKeyRepository apiKeyRepository,
                                    PasswordEncoder passwordEncoder,
                                    TenantContextService tenantContextService) {
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantContextService = tenantContextService;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // Check if request already has authentication
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Extract API key from header
        String apiKeyValue = request.getHeader(API_KEY_HEADER);
        
        if (apiKeyValue != null && !apiKeyValue.isEmpty()) {
            authenticateWithApiKey(apiKeyValue, request);
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Authenticate request using API key
     */
    private void authenticateWithApiKey(String apiKeyValue, HttpServletRequest request) {
        try {
            // Extract key prefix (first 8 characters)
            if (apiKeyValue.length() < 8) {
                logger.warn("Invalid API key format from IP: {}", getClientIP(request));
                return;
            }
            
            String keyPrefix = apiKeyValue.substring(0, 8);
            
            // Find API key by prefix
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyPrefixAndIsActiveTrue(keyPrefix);
            
            if (apiKeyOpt.isEmpty()) {
                logger.warn("API key not found for prefix: {} from IP: {}", keyPrefix, getClientIP(request));
                return;
            }
            
            ApiKey apiKey = apiKeyOpt.get();
            
            // Verify the full key
            if (!passwordEncoder.matches(apiKeyValue, apiKey.getKeyHash())) {
                logger.warn("Invalid API key for prefix: {} from IP: {}", keyPrefix, getClientIP(request));
                return;
            }
            
            // Check if key is valid
            if (!apiKey.isValid()) {
                logger.warn("API key is invalid (expired/revoked) for key: {} from IP: {}", 
                    apiKey.getKeyName(), getClientIP(request));
                return;
            }
            
            // Check IP restriction
            String clientIp = getClientIP(request);
            if (!apiKey.isIpAllowed(clientIp)) {
                logger.warn("IP not allowed for API key: {} from IP: {}", 
                    apiKey.getKeyName(), clientIp);
                return;
            }
            
            // Check scopes for the endpoint
            String requestPath = request.getRequestURI();
            if (!hasRequiredScope(apiKey, requestPath)) {
                logger.warn("Insufficient scope for API key: {} accessing: {}", 
                    apiKey.getKeyName(), requestPath);
                return;
            }
            
            // Set tenant context
            tenantContextService.setCurrentTenant(apiKey.getCompany().getId());
            
            // Create authentication
            List<SimpleGrantedAuthority> authorities = apiKey.getScopes().stream()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .collect(Collectors.toList());
            
            // Add API key role
            authorities.add(new SimpleGrantedAuthority("ROLE_API_KEY"));
            
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    "apikey:" + apiKey.getKeyName(),
                    null,
                    authorities
                );
            
            authentication.setDetails(apiKey);
            
            // Set authentication in context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Record usage
            apiKey.recordUsage(clientIp);
            apiKeyRepository.save(apiKey);
            
            logger.info("API key authentication successful for key: {} from IP: {}", 
                apiKey.getKeyName(), clientIp);
            
        } catch (Exception e) {
            logger.error("Error during API key authentication", e);
        }
    }
    
    /**
     * Check if API key has required scope for endpoint
     */
    private boolean hasRequiredScope(ApiKey apiKey, String requestPath) {
        // Define scope requirements per endpoint pattern
        if (requestPath.startsWith("/api/v1/reports")) {
            return apiKey.hasScope("reports:read") || apiKey.hasScope("reports:write");
        }
        if (requestPath.startsWith("/api/v1/users")) {
            return apiKey.hasScope("users:read") || apiKey.hasScope("users:write");
        }
        if (requestPath.startsWith("/api/v1/schools")) {
            return apiKey.hasScope("schools:read") || apiKey.hasScope("schools:write");
        }
        if (requestPath.startsWith("/api/v1/work-orders")) {
            return apiKey.hasScope("workorders:read") || apiKey.hasScope("workorders:write");
        }
        
        // Default: require at least read scope
        return apiKey.hasScope("read") || apiKey.hasScope("write") || apiKey.hasScope("admin");
    }
    
    /**
     * Get client IP address
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}