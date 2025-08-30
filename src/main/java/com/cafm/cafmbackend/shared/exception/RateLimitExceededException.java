package com.cafm.cafmbackend.shared.exception;

import java.time.LocalDateTime;

/**
 * Exception thrown when rate limits are exceeded.
 * 
 * Purpose: Signals rate limiting violations
 * Pattern: Security exception for rate limiting enforcement
 * Java 23: Enhanced exception with rate limiting context
 * Architecture: Rate limiting enforcement exception
 * Standards: Proper rate limit signaling with retry information
 */
public class RateLimitExceededException extends RuntimeException {

    private final String rateLimitKey;
    private final int currentCount;
    private final int maxCount;
    private final LocalDateTime resetTime;
    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, String rateLimitKey, int currentCount, int maxCount, 
                                    LocalDateTime resetTime, long retryAfterSeconds) {
        super(message);
        this.rateLimitKey = rateLimitKey;
        this.currentCount = currentCount;
        this.maxCount = maxCount;
        this.resetTime = resetTime;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getRateLimitKey() {
        return rateLimitKey;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public LocalDateTime getResetTime() {
        return resetTime;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public static RateLimitExceededException create(String rateLimitKey, int currentCount, int maxCount, 
                                                   LocalDateTime resetTime, long retryAfterSeconds) {
        String message = String.format("Rate limit exceeded for %s: %d/%d requests. Try again in %d seconds.", 
                                      rateLimitKey, currentCount, maxCount, retryAfterSeconds);
        return new RateLimitExceededException(message, rateLimitKey, currentCount, maxCount, resetTime, retryAfterSeconds);
    }

    public static RateLimitExceededException createForUser(String userId, int currentCount, int maxCount, long retryAfterSeconds) {
        LocalDateTime resetTime = LocalDateTime.now().plusSeconds(retryAfterSeconds);
        String rateLimitKey = "user:" + userId;
        String message = String.format("User rate limit exceeded: %d/%d requests. Try again in %d seconds.", 
                                      currentCount, maxCount, retryAfterSeconds);
        return new RateLimitExceededException(message, rateLimitKey, currentCount, maxCount, resetTime, retryAfterSeconds);
    }

    public static RateLimitExceededException createForEndpoint(String endpoint, String clientIp, int currentCount, int maxCount, long retryAfterSeconds) {
        LocalDateTime resetTime = LocalDateTime.now().plusSeconds(retryAfterSeconds);
        String rateLimitKey = "endpoint:" + endpoint + ":ip:" + clientIp;
        String message = String.format("Endpoint rate limit exceeded for %s: %d/%d requests. Try again in %d seconds.", 
                                      endpoint, currentCount, maxCount, retryAfterSeconds);
        return new RateLimitExceededException(message, rateLimitKey, currentCount, maxCount, resetTime, retryAfterSeconds);
    }
}