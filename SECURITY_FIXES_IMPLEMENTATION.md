# CAFM Backend Security Fixes Implementation Guide

## 🔴 CRITICAL FIX 1: Enable CSRF Protection

### File: `/src/main/java/com/cafm/cafmbackend/config/SecurityConfig.java`

Replace line 109 with proper CSRF configuration:

```java
// REMOVE THIS LINE:
// .csrf(AbstractHttpConfigurer::disable)

// ADD THIS CONFIGURATION:
.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
    .ignoringRequestMatchers(
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/v1/auth/forgot-password"
    )
)
```

### Create new class: `SpaCsrfTokenRequestHandler.java`

```java
package com.cafm.cafmbackend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * Custom CSRF token handler for SPA applications.
 * Handles both cookie-based and header-based CSRF tokens.
 */
public class SpaCsrfTokenRequestHandler extends XorCsrfTokenRequestAttributeHandler {
    
    private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();
    
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, 
                      Supplier<CsrfToken> csrfToken) {
        // Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection
        this.delegate.handle(request, response, csrfToken);
    }
    
    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        // Check header first (for AJAX requests)
        String headerToken = request.getHeader("X-CSRF-TOKEN");
        if (StringUtils.hasText(headerToken)) {
            return headerToken;
        }
        
        // Fall back to parameter (for form submissions)
        String paramToken = request.getParameter("_csrf");
        if (StringUtils.hasText(paramToken)) {
            return paramToken;
        }
        
        // Use default resolution
        return this.delegate.resolveCsrfTokenValue(request, csrfToken);
    }
}
```

---

## 🔴 CRITICAL FIX 2: Remove Hardcoded Secrets

### File: `/src/main/resources/application.yml`

Replace ALL default values with environment variable enforcement:

```yaml
# BEFORE (INSECURE):
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-for-jwt-tokens-change-this-in-production}

# AFTER (SECURE):
jwt:
  secret: ${JWT_SECRET}  # No default - will fail if not provided
  
# Apply same pattern to all credentials:
datasource:
  password: ${DB_PASSWORD}  # Remove default
  
redis:
  password: ${REDIS_PASSWORD}  # Remove default
  
minio:
  access-key: ${MINIO_ACCESS_KEY}  # Remove default
  secret-key: ${MINIO_SECRET_KEY}  # Remove default
```

### Add startup validation: `SecretValidationConfig.java`

```java
package com.cafm.cafmbackend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.regex.Pattern;

/**
 * Validates critical security configuration at startup.
 * Ensures no default or weak secrets in production.
 */
@Configuration
@Profile("!test")  // Skip validation in tests
public class SecretValidationConfig {
    
    private static final Pattern WEAK_SECRET_PATTERN = Pattern.compile(
        ".*(password|secret|changeme|default|123456|admin).*", 
        Pattern.CASE_INSENSITIVE
    );
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${spring.profiles.active:}")
    private String activeProfile;
    
    @PostConstruct
    public void validateSecrets() {
        // Only enforce in production
        if (isProduction()) {
            validateJwtSecret();
        }
    }
    
    private void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException(
                "JWT secret is required. Set JWT_SECRET environment variable."
            );
        }
        
        if (jwtSecret.length() < 64) {
            throw new IllegalStateException(
                "JWT secret must be at least 64 characters. Current: " + jwtSecret.length()
            );
        }
        
        if (WEAK_SECRET_PATTERN.matcher(jwtSecret).matches()) {
            throw new IllegalStateException(
                "JWT secret appears to be weak or default. Use a cryptographically secure secret."
            );
        }
    }
    
    private boolean isProduction() {
        return activeProfile != null && 
               (activeProfile.contains("prod") || activeProfile.contains("production"));
    }
}
```

---

## 🟠 HIGH FIX 1: Implement Comprehensive Rate Limiting

### Create: `RateLimitAspect.java`

```java
package com.cafm.cafmbackend.security.aspect;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting aspect for protecting sensitive endpoints.
 * Uses token bucket algorithm for flexible rate limiting.
 */
@Aspect
@Component
public class RateLimitAspect {
    
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        String key = getKey(request, rateLimit.keyType());
        
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(rateLimit));
        
        if (bucket.tryConsume(1)) {
            return joinPoint.proceed();
        } else {
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Rate limit exceeded. Please try again later."
            );
        }
    }
    
    private Bucket createBucket(RateLimit rateLimit) {
        Bandwidth limit = Bandwidth.classic(
            rateLimit.capacity(),
            Refill.intervally(rateLimit.tokens(), Duration.ofMinutes(rateLimit.minutes()))
        );
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
    
    private String getKey(HttpServletRequest request, RateLimitKeyType keyType) {
        return switch (keyType) {
            case IP -> request.getRemoteAddr();
            case USER -> getUsername();
            case SESSION -> request.getSession().getId();
            case GLOBAL -> "global";
        };
    }
    
    private String getUsername() {
        // Extract from security context
        return "anonymous"; // Implement actual extraction
    }
    
    private HttpServletRequest getCurrentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
            .getRequest();
    }
}
```

### Apply to sensitive endpoints:

```java
@PostMapping("/forgot-password")
@RateLimit(capacity = 3, tokens = 3, minutes = 15, keyType = RateLimitKeyType.IP)
public ResponseEntity<PasswordResetResponse> forgotPassword(...) {
    // Implementation
}

@PostMapping("/login")
@RateLimit(capacity = 5, tokens = 5, minutes = 1, keyType = RateLimitKeyType.IP)
public ResponseEntity<LoginResponse> login(...) {
    // Implementation
}
```

---

## 🟠 HIGH FIX 2: Add Security Headers

### Create: `SecurityHeadersFilter.java`

```java
package com.cafm.cafmbackend.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds comprehensive security headers to all responses.
 * Implements defense-in-depth with multiple security layers.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response,
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        
        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");
        
        // Enable XSS protection
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Force HTTPS
        if (isProduction()) {
            response.setHeader("Strict-Transport-Security", 
                             "max-age=31536000; includeSubDomains; preload");
        }
        
        // Content Security Policy
        String csp = "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                    "font-src 'self' https://fonts.gstatic.com; " +
                    "img-src 'self' data: https:; " +
                    "connect-src 'self' https://api.cafm.sa wss://api.cafm.sa; " +
                    "frame-ancestors 'none'; " +
                    "base-uri 'self'; " +
                    "form-action 'self'";
        response.setHeader("Content-Security-Policy", csp);
        
        // Referrer Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions Policy (formerly Feature Policy)
        response.setHeader("Permissions-Policy", 
            "geolocation=(), microphone=(), camera=(), payment=()");
        
        // Cache Control for sensitive content
        if (request.getRequestURI().startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean isProduction() {
        String profile = System.getProperty("spring.profiles.active", "");
        return profile.contains("prod") || profile.contains("production");
    }
}
```

---

## 🟡 MEDIUM FIX 1: Enhanced File Upload Security

### Update: `FileUploadService.java`

```java
package com.cafm.cafmbackend.service;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

/**
 * Enhanced file upload service with security validations.
 */
@Service
public class SecureFileUploadService {
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "xls", "xlsx"
    );
    
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif",
        "application/pdf", 
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );
    
    private final Tika tika = new Tika();
    
    public void validateFile(MultipartFile file) throws SecurityException {
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new SecurityException("File size exceeds maximum allowed size of 10MB");
        }
        
        // Check file extension
        String filename = file.getOriginalFilename();
        if (filename == null || !hasAllowedExtension(filename)) {
            throw new SecurityException("File type not allowed");
        }
        
        // Check MIME type from content
        try {
            String detectedMimeType = tika.detect(file.getBytes());
            if (!ALLOWED_MIME_TYPES.contains(detectedMimeType)) {
                throw new SecurityException("File content type not allowed: " + detectedMimeType);
            }
            
            // Verify MIME type matches extension
            String declaredMimeType = file.getContentType();
            if (!detectedMimeType.equals(declaredMimeType)) {
                throw new SecurityException("File content does not match declared type");
            }
        } catch (IOException e) {
            throw new SecurityException("Failed to validate file content", e);
        }
        
        // Scan for malware (integrate with ClamAV or similar)
        scanForMalware(file);
        
        // Sanitize filename
        String sanitizedFilename = sanitizeFilename(filename);
        
        // Additional checks for specific file types
        if (detectedMimeType.startsWith("image/")) {
            validateImage(file);
        }
    }
    
    private boolean hasAllowedExtension(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }
    
    private void scanForMalware(MultipartFile file) {
        // Integrate with antivirus service
        // Example: ClamAV integration
    }
    
    private String sanitizeFilename(String filename) {
        // Remove path traversal attempts
        filename = filename.replaceAll("\\.\\./", "");
        filename = filename.replaceAll("/", "");
        filename = filename.replaceAll("\\\\", "");
        
        // Remove special characters
        filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Limit length
        if (filename.length() > 255) {
            filename = filename.substring(0, 255);
        }
        
        return filename;
    }
    
    private void validateImage(MultipartFile file) {
        // Additional image-specific validations
        // Check for embedded scripts in SVG
        // Verify image dimensions
        // Check for EXIF data that might contain sensitive info
    }
}
```

---

## 🟡 MEDIUM FIX 2: Input Sanitization Layer

### Create: `InputSanitizer.java`

```java
package com.cafm.cafmbackend.security;

import org.owasp.encoder.Encode;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Comprehensive input sanitization for preventing injection attacks.
 */
@Component
public class InputSanitizer {
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "('.+--)|(--)|(\\|\\|)|(\\*\\|)|(%7C)"
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "<script[^>]*>.*?</script>|<iframe[^>]*>.*?</iframe>|javascript:|on\\w+\\s*=",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Sanitize input for SQL queries
     */
    public String sanitizeForSql(String input) {
        if (input == null) return null;
        
        // Remove SQL injection attempts
        input = SQL_INJECTION_PATTERN.matcher(input).replaceAll("");
        
        // Escape single quotes
        input = input.replace("'", "''");
        
        // Remove null bytes
        input = input.replace("\0", "");
        
        return input;
    }
    
    /**
     * Sanitize input for HTML output
     */
    public String sanitizeForHtml(String input) {
        if (input == null) return null;
        
        // Use OWASP encoder
        return Encode.forHtml(input);
    }
    
    /**
     * Sanitize input for JavaScript context
     */
    public String sanitizeForJavaScript(String input) {
        if (input == null) return null;
        
        return Encode.forJavaScript(input);
    }
    
    /**
     * Sanitize search terms for LIKE queries
     */
    public String sanitizeSearchTerm(String searchTerm) {
        if (searchTerm == null) return null;
        
        // Escape SQL wildcard characters
        searchTerm = searchTerm.replace("%", "\\%");
        searchTerm = searchTerm.replace("_", "\\_");
        searchTerm = searchTerm.replace("[", "\\[");
        searchTerm = searchTerm.replace("]", "\\]");
        
        // Remove potential XSS
        searchTerm = XSS_PATTERN.matcher(searchTerm).replaceAll("");
        
        // Limit length to prevent ReDoS
        if (searchTerm.length() > 100) {
            searchTerm = searchTerm.substring(0, 100);
        }
        
        return searchTerm;
    }
    
    /**
     * Validate and sanitize email
     */
    public String sanitizeEmail(String email) {
        if (email == null) return null;
        
        email = email.trim().toLowerCase();
        
        // Basic email validation
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        return email;
    }
    
    /**
     * Sanitize file paths
     */
    public String sanitizeFilePath(String path) {
        if (path == null) return null;
        
        // Remove path traversal attempts
        path = path.replaceAll("\\.\\./", "");
        path = path.replaceAll("/\\.\\.", "");
        path = path.replaceAll("\\.\\\\", "");
        
        // Remove null bytes
        path = path.replace("\0", "");
        
        return path;
    }
}
```

---

## Testing Security Fixes

### Create: `SecurityFixesTest.java`

```java
package com.cafm.cafmbackend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityFixesTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testCsrfProtection() throws Exception {
        // Should fail without CSRF token
        mockMvc.perform(post("/api/v1/users")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void testSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Content-Security-Policy"));
    }
    
    @Test
    void testRateLimiting() throws Exception {
        // Test rate limiting on login endpoint
        for (int i = 0; i < 6; i++) {
            var result = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType("application/json")
                    .content("{\"email\":\"test@test.com\",\"password\":\"wrong\"}"));
            
            if (i < 5) {
                result.andExpect(status().isUnauthorized());
            } else {
                result.andExpect(status().isTooManyRequests());
            }
        }
    }
}
```

---

## Deployment Checklist

### Before deploying these fixes:

1. **Test in staging environment** with production-like configuration
2. **Update environment variables** to remove any default values
3. **Generate new JWT secrets** using cryptographically secure methods:
   ```bash
   openssl rand -base64 64
   ```
4. **Update API documentation** to include CSRF token requirements
5. **Test frontend integration** with new CSRF tokens
6. **Monitor rate limiting** to ensure legitimate users aren't blocked
7. **Review security headers** compatibility with your frontend
8. **Run penetration testing** after implementing fixes
9. **Update security documentation** for the team
10. **Set up security monitoring** and alerting

---

## Monitoring & Alerting

### Add security event monitoring:

```java
@EventListener
public void handleSecurityEvent(AbstractAuthenticationEvent event) {
    if (event instanceof AbstractAuthenticationFailureEvent) {
        // Log failed authentication
        securityLogger.warn("Authentication failed: {}", event);
        
        // Alert on multiple failures
        if (getRecentFailures() > 10) {
            alertingService.sendSecurityAlert("Multiple authentication failures detected");
        }
    }
}
```

---

## Regular Security Tasks

### Weekly:
- Review authentication logs for anomalies
- Check rate limiting effectiveness
- Monitor file upload patterns

### Monthly:
- Update dependencies for security patches
- Review and rotate API keys
- Audit user permissions

### Quarterly:
- Penetration testing
- Security training for developers
- Review and update security policies

---

*Implementation guide prepared: 2025-08-22*  
*Estimated implementation time: 2-3 days for critical fixes, 1 week for all fixes*