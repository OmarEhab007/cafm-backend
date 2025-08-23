package com.cafm.cafmbackend.security.filter;

import com.cafm.cafmbackend.security.RateLimitingConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Rate limiting filter to prevent API abuse.
 * 
 * Purpose: Enforce rate limits per user/IP to prevent abuse
 * Pattern: Servlet filter with token bucket algorithm
 * Java 23: Efficient rate limit checks
 * Architecture: Security layer filter
 * Standards: Returns 429 Too Many Requests when limit exceeded
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    
    private final ProxyManager<String> proxyManager;
    private final Supplier<BucketConfiguration> standardBucketSupplier;
    private final Supplier<BucketConfiguration> premiumBucketSupplier;
    private final Supplier<BucketConfiguration> publicBucketSupplier;
    
    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;
    
    @Value("${app.rate-limit.per-ip:false}")
    private boolean rateLimitPerIp;
    
    @Autowired
    public RateLimitingFilter(ProxyManager<String> proxyManager,
                             Supplier<BucketConfiguration> standardBucketSupplier,
                             Supplier<BucketConfiguration> premiumBucketSupplier,
                             Supplier<BucketConfiguration> publicBucketSupplier) {
        this.proxyManager = proxyManager;
        this.standardBucketSupplier = standardBucketSupplier;
        this.premiumBucketSupplier = premiumBucketSupplier;
        this.publicBucketSupplier = publicBucketSupplier;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // Skip rate limiting if disabled
        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Skip rate limiting for static resources
        String path = request.getRequestURI();
        if (isStaticResource(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Get bucket key based on user or IP
        String bucketKey = getBucketKey(request);
        
        // Get rate limit tier
        RateLimitingConfig.RateLimitTier tier = getRateLimitTier(request);
        
        // Get or create bucket
        Bucket bucket = proxyManager.builder()
            .build(bucketKey, getBucketSupplier(tier));
        
        // Try to consume token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            // Add rate limit headers
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            response.addHeader("X-Rate-Limit-Limit", String.valueOf(tier.getRequestsPerMinute()));
            
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            long waitForRefill = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.getWriter().write("Rate limit exceeded. Please retry after " + waitForRefill + " seconds");
            
            logger.warn("Rate limit exceeded for key: {} on path: {}", bucketKey, path);
        }
    }
    
    /**
     * Generate bucket key based on user or IP
     */
    private String getBucketKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getPrincipal())) {
            // Use username for authenticated users
            return "rate_limit:user:" + authentication.getName();
        } else if (rateLimitPerIp) {
            // Use IP address for anonymous users
            return "rate_limit:ip:" + getClientIP(request);
        } else {
            // Global rate limit for all anonymous users
            return "rate_limit:anonymous";
        }
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
    
    /**
     * Determine rate limit tier based on user role or endpoint
     */
    private RateLimitingConfig.RateLimitTier getRateLimitTier(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Check if public endpoint
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/auth/") || path.startsWith("/api/v1/public/")) {
            return RateLimitingConfig.RateLimitTier.PUBLIC;
        }
        
        // Check user authorities
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            if (isAdmin) {
                return RateLimitingConfig.RateLimitTier.PREMIUM;
            }
            
            return RateLimitingConfig.RateLimitTier.STANDARD;
        }
        
        return RateLimitingConfig.RateLimitTier.PUBLIC;
    }
    
    /**
     * Get bucket supplier based on tier
     */
    private Supplier<BucketConfiguration> getBucketSupplier(RateLimitingConfig.RateLimitTier tier) {
        return switch (tier) {
            case PREMIUM -> premiumBucketSupplier;
            case STANDARD -> standardBucketSupplier;
            case PUBLIC, UNLIMITED -> publicBucketSupplier;
        };
    }
    
    /**
     * Check if path is for static resource
     */
    private boolean isStaticResource(String path) {
        return path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.endsWith(".css") ||
               path.endsWith(".js") ||
               path.endsWith(".png") ||
               path.endsWith(".jpg") ||
               path.endsWith(".ico");
    }
}