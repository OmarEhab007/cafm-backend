package com.cafm.cafmbackend.security.service;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced service for tracking login attempts with progressive delays and IP blocking.
 * 
 * Security Features:
 * - Progressive delays after failed attempts
 * - IP-based blocking after threshold
 * - Automatic cleanup of expired entries
 * - Configurable thresholds and durations
 */
@Service
public class EnhancedLoginAttemptService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedLoginAttemptService.class);
    
    // Configuration with secure defaults
    @Value("${security.login.max-attempts:3}")
    private int maxAttempts;
    
    @Value("${security.login.max-ip-attempts:10}")
    private int maxIpAttempts;
    
    @Value("${security.login.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;
    
    @Value("${security.login.ip-block-duration-hours:24}")
    private int ipBlockDurationHours;
    
    @Value("${security.login.attempt-window-minutes:15}")
    private int attemptWindowMinutes;
    
    @Value("${security.login.progressive-delay-enabled:true}")
    private boolean progressiveDelayEnabled;
    
    // Track failed attempts by username/email
    private final Map<String, LoginAttemptData> userAttemptsCache = new ConcurrentHashMap<>();
    
    // Track failed attempts by IP address
    private final Map<String, LoginAttemptData> ipAttemptsCache = new ConcurrentHashMap<>();
    
    // Track combined user+IP for more granular control
    private final Map<String, LoginAttemptData> combinedAttemptsCache = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        logger.info("Enhanced Login Attempt Service initialized with max attempts: {}, IP max attempts: {}", 
                   maxAttempts, maxIpAttempts);
    }
    
    /**
     * Record a successful login and reset attempts.
     */
    public void loginSucceeded(String username, String ipAddress) {
        logger.debug("Login succeeded for user: {} from IP: {}", username, ipAddress);
        
        userAttemptsCache.remove(username);
        combinedAttemptsCache.remove(getCombinedKey(username, ipAddress));
        
        // Don't reset IP attempts on single success to prevent distributed attacks
        LoginAttemptData ipData = ipAttemptsCache.get(ipAddress);
        if (ipData != null && ipData.attemptCount > 0) {
            // Reduce IP attempt count but don't clear completely
            ipAttemptsCache.put(ipAddress, new LoginAttemptData(
                Math.max(0, ipData.attemptCount - 1),
                ipData.firstAttemptTime,
                ipData.lockedUntil
            ));
        }
    }
    
    /**
     * Record a failed login attempt with progressive delay calculation.
     */
    public void loginFailed(String username, String ipAddress) {
        logger.warn("Login failed for user: {} from IP: {}", username, ipAddress);
        
        // Update user attempts
        updateAttemptData(userAttemptsCache, username, maxAttempts, lockoutDurationMinutes);
        
        // Update IP attempts
        updateAttemptData(ipAttemptsCache, ipAddress, maxIpAttempts, ipBlockDurationHours * 60);
        
        // Update combined attempts
        String combinedKey = getCombinedKey(username, ipAddress);
        updateAttemptData(combinedAttemptsCache, combinedKey, maxAttempts, lockoutDurationMinutes);
        
        // Log security event if thresholds exceeded
        LoginAttemptData userData = userAttemptsCache.get(username);
        LoginAttemptData ipData = ipAttemptsCache.get(ipAddress);
        
        if (userData != null && userData.attemptCount >= maxAttempts) {
            logger.error("SECURITY: Account locked due to {} failed login attempts: {}", 
                        userData.attemptCount, username);
        }
        
        if (ipData != null && ipData.attemptCount >= maxIpAttempts) {
            logger.error("SECURITY: IP blocked due to {} failed login attempts: {}", 
                        ipData.attemptCount, ipAddress);
        }
    }
    
    /**
     * Check if login should be blocked and get delay if progressive delay is enabled.
     */
    public LoginAttemptResult checkLoginAttempt(String username, String ipAddress) {
        // Check IP block first (highest priority)
        if (isIpBlocked(ipAddress)) {
            LocalDateTime ipLockExpiry = getLockExpirationTime(ipAttemptsCache, ipAddress);
            return new LoginAttemptResult(true, 0, "IP address is blocked", ipLockExpiry);
        }
        
        // Check user block
        if (isUserBlocked(username)) {
            LocalDateTime userLockExpiry = getLockExpirationTime(userAttemptsCache, username);
            long delaySeconds = getProgressiveDelay(username);
            return new LoginAttemptResult(true, delaySeconds, "Account is locked", userLockExpiry);
        }
        
        // Check combined block
        String combinedKey = getCombinedKey(username, ipAddress);
        if (isBlocked(combinedAttemptsCache, combinedKey, maxAttempts)) {
            LocalDateTime lockExpiry = getLockExpirationTime(combinedAttemptsCache, combinedKey);
            long delaySeconds = getProgressiveDelay(username);
            return new LoginAttemptResult(true, delaySeconds, "Too many attempts from this location", lockExpiry);
        }
        
        // Calculate progressive delay even if not blocked
        long delaySeconds = progressiveDelayEnabled ? getProgressiveDelay(username) : 0;
        
        return new LoginAttemptResult(false, delaySeconds, null, null);
    }
    
    /**
     * Get progressive delay in seconds based on attempt count.
     */
    private long getProgressiveDelay(String username) {
        if (!progressiveDelayEnabled) {
            return 0;
        }
        
        LoginAttemptData userData = userAttemptsCache.get(username);
        if (userData == null) {
            return 0;
        }
        
        // Progressive delay: 0s, 2s, 5s, 10s, 20s, 30s...
        return switch (userData.attemptCount) {
            case 0 -> 0;
            case 1 -> 0;
            case 2 -> 2;
            case 3 -> 5;
            case 4 -> 10;
            case 5 -> 20;
            default -> Math.min(30 + (userData.attemptCount - 6) * 10, 60); // Cap at 60 seconds
        };
    }
    
    /**
     * Check if user account is blocked.
     */
    public boolean isUserBlocked(String username) {
        return isBlocked(userAttemptsCache, username, maxAttempts);
    }
    
    /**
     * Check if IP address is blocked.
     */
    public boolean isIpBlocked(String ipAddress) {
        return isBlocked(ipAttemptsCache, ipAddress, maxIpAttempts);
    }
    
    /**
     * Generic method to check if a key is blocked.
     */
    private boolean isBlocked(Map<String, LoginAttemptData> cache, String key, int threshold) {
        LoginAttemptData attempts = cache.get(key);
        
        if (attempts == null) {
            return false;
        }
        
        // Check if locked
        if (attempts.lockedUntil != null) {
            if (attempts.lockedUntil.isAfter(LocalDateTime.now())) {
                return true;
            } else {
                // Lock expired, remove from cache
                cache.remove(key);
                return false;
            }
        }
        
        // Check if exceeded threshold
        return attempts.attemptCount >= threshold;
    }
    
    /**
     * Update attempt data for a key.
     */
    private void updateAttemptData(Map<String, LoginAttemptData> cache, String key, 
                                   int threshold, int lockoutMinutes) {
        LoginAttemptData attempts = cache.computeIfAbsent(key, 
            k -> new LoginAttemptData(0, LocalDateTime.now(), null));
        
        // Check if we're within the attempt window
        if (attempts.firstAttemptTime.isBefore(LocalDateTime.now().minus(attemptWindowMinutes, ChronoUnit.MINUTES))) {
            // Reset the counter if outside the window
            attempts = new LoginAttemptData(1, LocalDateTime.now(), null);
        } else {
            // Increment the counter
            LocalDateTime lockUntil = null;
            if (attempts.attemptCount + 1 >= threshold) {
                lockUntil = LocalDateTime.now().plus(lockoutMinutes, ChronoUnit.MINUTES);
            }
            
            attempts = new LoginAttemptData(
                attempts.attemptCount + 1,
                attempts.firstAttemptTime,
                lockUntil
            );
        }
        
        cache.put(key, attempts);
    }
    
    /**
     * Get remaining attempts before lockout.
     */
    public int getRemainingAttempts(String username) {
        LoginAttemptData attempts = userAttemptsCache.get(username);
        if (attempts == null) {
            return maxAttempts;
        }
        
        int remaining = maxAttempts - attempts.attemptCount;
        return Math.max(0, remaining);
    }
    
    /**
     * Get lock expiration time for a specific cache and key.
     */
    private LocalDateTime getLockExpirationTime(Map<String, LoginAttemptData> cache, String key) {
        LoginAttemptData attempts = cache.get(key);
        if (attempts != null && attempts.lockedUntil != null) {
            return attempts.lockedUntil;
        }
        return null;
    }
    
    /**
     * Reset login attempts for a specific user.
     */
    public void resetUserAttempts(String username) {
        logger.info("Resetting login attempts for user: {}", username);
        userAttemptsCache.remove(username);
        
        // Also clear combined entries for this user
        combinedAttemptsCache.entrySet().removeIf(entry -> 
            entry.getKey().startsWith(username + ":"));
    }
    
    /**
     * Reset login attempts for a specific IP.
     */
    public void resetIpAttempts(String ipAddress) {
        logger.info("Resetting login attempts for IP: {}", ipAddress);
        ipAttemptsCache.remove(ipAddress);
        
        // Also clear combined entries for this IP
        combinedAttemptsCache.entrySet().removeIf(entry -> 
            entry.getKey().endsWith(":" + ipAddress));
    }
    
    /**
     * Extract IP address from HTTP request.
     */
    public String getClientIP(HttpServletRequest request) {
        // Check for proxied requests
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
     * Generate combined key for user+IP tracking.
     */
    private String getCombinedKey(String username, String ipAddress) {
        return username + ":" + ipAddress;
    }
    
    /**
     * Clean up expired entries from all caches.
     * Scheduled to run every hour.
     */
    @Scheduled(fixedDelay = 3600000) // 1 hour
    public void cleanupExpiredEntries() {
        logger.debug("Running cleanup of expired login attempt entries");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowThreshold = now.minus(attemptWindowMinutes, ChronoUnit.MINUTES);
        
        // Clean user attempts
        cleanupCache(userAttemptsCache, windowThreshold, now);
        
        // Clean IP attempts
        cleanupCache(ipAttemptsCache, windowThreshold, now);
        
        // Clean combined attempts
        cleanupCache(combinedAttemptsCache, windowThreshold, now);
        
        logger.debug("Cleanup complete. User cache size: {}, IP cache size: {}, Combined cache size: {}",
                    userAttemptsCache.size(), ipAttemptsCache.size(), combinedAttemptsCache.size());
    }
    
    /**
     * Clean up a specific cache.
     */
    private void cleanupCache(Map<String, LoginAttemptData> cache, 
                              LocalDateTime windowThreshold, LocalDateTime now) {
        cache.entrySet().removeIf(entry -> {
            LoginAttemptData data = entry.getValue();
            
            // Remove if lock has expired
            if (data.lockedUntil != null && data.lockedUntil.isBefore(now)) {
                return true;
            }
            
            // Remove if outside attempt window and not locked
            if (data.lockedUntil == null && data.firstAttemptTime.isBefore(windowThreshold)) {
                return true;
            }
            
            return false;
        });
    }
    
    /**
     * Internal data structure for tracking login attempts.
     */
    private record LoginAttemptData(
        int attemptCount,
        LocalDateTime firstAttemptTime,
        LocalDateTime lockedUntil
    ) {}
    
    /**
     * Result of login attempt check.
     */
    public record LoginAttemptResult(
        boolean blocked,
        long delaySeconds,
        String reason,
        LocalDateTime lockExpiry
    ) {
        public Duration getRemainingLockTime() {
            if (lockExpiry == null) {
                return Duration.ZERO;
            }
            return Duration.between(LocalDateTime.now(), lockExpiry);
        }
    }
}