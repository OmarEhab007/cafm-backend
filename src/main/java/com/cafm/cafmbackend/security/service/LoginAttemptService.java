package com.cafm.cafmbackend.security.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking login attempts and implementing account lockout mechanism.
 * 
 * Architecture: Security service for brute force protection
 * Pattern: In-memory cache with automatic cleanup
 * Java 23: Using ConcurrentHashMap for thread safety
 */
@Service
public class LoginAttemptService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);
    
    @Value("${security.login.max-attempts:5}")
    private int maxAttempts;
    
    @Value("${security.login.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;
    
    @Value("${security.login.attempt-window-minutes:15}")
    private int attemptWindowMinutes;
    
    // Track failed attempts by email
    private final Map<String, LoginAttemptData> attemptsCache = new ConcurrentHashMap<>();
    
    /**
     * Record a successful login and reset attempts.
     */
    public void loginSucceeded(String key) {
        logger.debug("Login succeeded for: {}", key);
        attemptsCache.remove(key);
    }
    
    /**
     * Record a failed login attempt.
     */
    public void loginFailed(String key) {
        logger.warn("Login failed for: {}", key);
        
        LoginAttemptData attempts = attemptsCache.computeIfAbsent(key, 
            k -> new LoginAttemptData(0, LocalDateTime.now(), null));
        
        // Check if we're within the attempt window
        if (attempts.firstAttemptTime.isBefore(LocalDateTime.now().minus(attemptWindowMinutes, ChronoUnit.MINUTES))) {
            // Reset the counter if outside the window
            attempts = new LoginAttemptData(1, LocalDateTime.now(), null);
        } else {
            // Increment the counter
            attempts = new LoginAttemptData(
                attempts.attemptCount + 1,
                attempts.firstAttemptTime,
                attempts.attemptCount + 1 >= maxAttempts ? LocalDateTime.now() : attempts.lockedUntil
            );
        }
        
        attemptsCache.put(key, attempts);
        
        if (attempts.attemptCount >= maxAttempts) {
            logger.error("Account locked due to {} failed login attempts: {}", maxAttempts, key);
        }
    }
    
    /**
     * Check if account is blocked due to too many failed attempts.
     */
    public boolean isBlocked(String key) {
        LoginAttemptData attempts = attemptsCache.get(key);
        
        if (attempts == null) {
            return false;
        }
        
        // Check if locked
        if (attempts.lockedUntil != null) {
            if (attempts.lockedUntil.isAfter(LocalDateTime.now())) {
                logger.warn("Account is locked until {}: {}", attempts.lockedUntil, key);
                return true;
            } else {
                // Lock expired, remove from cache
                attemptsCache.remove(key);
                return false;
            }
        }
        
        // Check if exceeded max attempts
        if (attempts.attemptCount >= maxAttempts) {
            // Set lock time if not already set
            LocalDateTime lockUntil = LocalDateTime.now().plus(lockoutDurationMinutes, ChronoUnit.MINUTES);
            attemptsCache.put(key, new LoginAttemptData(
                attempts.attemptCount,
                attempts.firstAttemptTime,
                lockUntil
            ));
            logger.warn("Account locked until {} due to {} failed attempts: {}", 
                       lockUntil, attempts.attemptCount, key);
            return true;
        }
        
        return false;
    }
    
    /**
     * Get remaining attempts before lockout.
     */
    public int getRemainingAttempts(String key) {
        LoginAttemptData attempts = attemptsCache.get(key);
        if (attempts == null) {
            return maxAttempts;
        }
        
        int remaining = maxAttempts - attempts.attemptCount;
        return Math.max(0, remaining);
    }
    
    /**
     * Get lock expiration time.
     */
    public LocalDateTime getLockExpirationTime(String key) {
        LoginAttemptData attempts = attemptsCache.get(key);
        if (attempts != null && attempts.lockedUntil != null) {
            return attempts.lockedUntil;
        }
        return null;
    }
    
    /**
     * Reset login attempts for a specific key.
     */
    public void resetAttempts(String key) {
        logger.info("Resetting login attempts for: {}", key);
        attemptsCache.remove(key);
    }
    
    /**
     * Clean up expired entries from cache.
     * Should be called periodically by a scheduled task.
     */
    public void cleanupExpiredEntries() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowThreshold = now.minus(attemptWindowMinutes, ChronoUnit.MINUTES);
        
        attemptsCache.entrySet().removeIf(entry -> {
            LoginAttemptData data = entry.getValue();
            
            // Remove if lock has expired
            if (data.lockedUntil != null && data.lockedUntil.isBefore(now)) {
                logger.debug("Removing expired lock for: {}", entry.getKey());
                return true;
            }
            
            // Remove if outside attempt window and not locked
            if (data.lockedUntil == null && data.firstAttemptTime.isBefore(windowThreshold)) {
                logger.debug("Removing stale attempt record for: {}", entry.getKey());
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
}