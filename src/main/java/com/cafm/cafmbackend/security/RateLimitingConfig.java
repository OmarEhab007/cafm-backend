package com.cafm.cafmbackend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Rate limiting configuration using Bucket4j with Redis backend.
 * 
 * Purpose: Protect API from abuse and ensure fair usage
 * Pattern: Token bucket algorithm with distributed state
 * Java 23: Ready for virtual threads in rate limit checks
 * Architecture: Security layer with Redis-backed rate limiting
 * Standards: Configurable limits per API tier
 */
@Configuration
public class RateLimitingConfig {
    
    @Value("${app.rate-limit.default-requests-per-minute:60}")
    private int defaultRequestsPerMinute;
    
    @Value("${app.rate-limit.premium-requests-per-minute:300}")
    private int premiumRequestsPerMinute;
    
    @Value("${app.rate-limit.burst-capacity:10}")
    private int burstCapacity;
    
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.data.redis.password:}")
    private String redisPassword;
    
    /**
     * Configure Redis client for rate limiting
     */
    @Bean
    public RedisClient redisClientForRateLimiting() {
        RedisURI.Builder builder = RedisURI.builder()
            .withHost(redisHost)
            .withPort(redisPort);
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            builder.withPassword(redisPassword.toCharArray());
        }
        
        return RedisClient.create(builder.build());
    }
    
    /**
     * Configure Redis connection for rate limiting
     */
    @Bean
    public StatefulRedisConnection<String, byte[]> redisConnection(RedisClient redisClient) {
        RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);
        return redisClient.connect(codec);
    }
    
    /**
     * Configure distributed proxy manager for rate limiting buckets
     */
    @Bean
    public LettuceBasedProxyManager<String> proxyManager(StatefulRedisConnection<String, byte[]> connection) {
        return LettuceBasedProxyManager.builderFor(connection)
            .withExpirationStrategy(
                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10))
            )
            .build();
    }
    
    /**
     * Create rate limit configuration for standard users
     */
    @Bean
    public Supplier<io.github.bucket4j.BucketConfiguration> standardBucketSupplier() {
        Bandwidth limit = Bandwidth.classic(
            defaultRequestsPerMinute + burstCapacity,
            Refill.intervally(defaultRequestsPerMinute, Duration.ofMinutes(1))
        );
        
        return () -> io.github.bucket4j.BucketConfiguration.builder()
            .addLimit(limit)
            .build();
    }
    
    /**
     * Create rate limit configuration for premium users
     */
    @Bean
    public Supplier<io.github.bucket4j.BucketConfiguration> premiumBucketSupplier() {
        Bandwidth limit = Bandwidth.classic(
            premiumRequestsPerMinute + burstCapacity,
            Refill.intervally(premiumRequestsPerMinute, Duration.ofMinutes(1))
        );
        
        return () -> io.github.bucket4j.BucketConfiguration.builder()
            .addLimit(limit)
            .build();
    }
    
    /**
     * Create rate limit configuration for public endpoints
     */
    @Bean
    public Supplier<io.github.bucket4j.BucketConfiguration> publicBucketSupplier() {
        // More restrictive limits for public endpoints
        Bandwidth limit = Bandwidth.classic(
            20,
            Refill.intervally(20, Duration.ofMinutes(1))
        );
        
        return () -> io.github.bucket4j.BucketConfiguration.builder()
            .addLimit(limit)
            .build();
    }
    
    /**
     * Rate limit tiers enumeration
     */
    public enum RateLimitTier {
        PUBLIC(20),
        STANDARD(60),
        PREMIUM(300),
        UNLIMITED(Integer.MAX_VALUE);
        
        private final int requestsPerMinute;
        
        RateLimitTier(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }
        
        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }
    }
}