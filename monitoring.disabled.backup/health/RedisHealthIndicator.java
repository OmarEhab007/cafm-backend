package com.cafm.cafmbackend.monitoring.health;

import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

/**
 * Redis Health Indicator for CAFM Backend
 * 
 * Purpose: Monitors Redis connectivity, performance, and memory usage
 * Pattern: Spring Boot HealthIndicator implementation for Redis monitoring
 * Java 23: Uses modern exception handling and pattern matching
 * Architecture: Part of monitoring layer providing health checks for Redis cache
 * Standards: Implements Spring Boot Actuator health check standards
 */
@Component("redis")
public class RedisHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(RedisHealthIndicator.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;
    private final Timer healthCheckTimer;
    
    // Test key for health checks
    private static final String HEALTH_CHECK_KEY = "cafm:health:check";
    private static final String HEALTH_CHECK_VALUE = "healthy";

    public RedisHealthIndicator(RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
        this.healthCheckTimer = Timer.builder("cafm.health.redis.check")
            .description("Redis health check execution time")
            .register(meterRegistry);
    }

    @Override
    public Health health() {
        return healthCheckTimer.recordCallable(this::performHealthCheck);
    }

    private Health performHealthCheck() {
        Instant startTime = Instant.now();
        
        try {
            // Test basic connectivity with ping
            String pong = redisTemplate.execute((RedisCallback<String>) connection -> {
                return connection.ping();
            });
            
            if (!"PONG".equals(pong)) {
                return Health.down()
                    .withDetail("error", "Redis ping failed, received: " + pong)
                    .withDetail("timestamp", Instant.now())
                    .build();
            }

            // Test read/write operations
            redisTemplate.opsForValue().set(HEALTH_CHECK_KEY, HEALTH_CHECK_VALUE, Duration.ofMinutes(1));
            String retrievedValue = (String) redisTemplate.opsForValue().get(HEALTH_CHECK_KEY);
            
            if (!HEALTH_CHECK_VALUE.equals(retrievedValue)) {
                return Health.down()
                    .withDetail("error", "Redis read/write test failed")
                    .withDetail("expected", HEALTH_CHECK_VALUE)
                    .withDetail("actual", retrievedValue)
                    .withDetail("timestamp", Instant.now())
                    .build();
            }

            // Clean up test key
            redisTemplate.delete(HEALTH_CHECK_KEY);

            // Get Redis server information
            RedisInfo redisInfo = getRedisInfo();
            Duration responseTime = Duration.between(startTime, Instant.now());
            
            // Check response time threshold
            Health.Builder healthBuilder;
            if (responseTime.toMillis() > 2000) {
                healthBuilder = Health.down()
                    .withDetail("error", "Redis response time too slow");
            } else if (responseTime.toMillis() > 500) {
                healthBuilder = Health.up()
                    .withDetail("warning", "Redis response time is slow");
            } else {
                healthBuilder = Health.up();
            }

            // Add detailed information
            return healthBuilder
                .withDetail("ping", "PONG")
                .withDetail("response_time_ms", responseTime.toMillis())
                .withDetail("server_version", redisInfo.version())
                .withDetail("used_memory", redisInfo.usedMemory())
                .withDetail("used_memory_human", redisInfo.usedMemoryHuman())
                .withDetail("max_memory", redisInfo.maxMemory())
                .withDetail("connected_clients", redisInfo.connectedClients())
                .withDetail("total_connections_received", redisInfo.totalConnectionsReceived())
                .withDetail("keyspace_hits", redisInfo.keyspaceHits())
                .withDetail("keyspace_misses", redisInfo.keyspaceMisses())
                .withDetail("uptime_in_seconds", redisInfo.uptimeInSeconds())
                .withDetail("last_check", Instant.now())
                .build();

        } catch (Exception e) {
            logger.error("Redis health check failed", e);
            
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("error_type", e.getClass().getSimpleName())
                .withDetail("timestamp", Instant.now())
                .build();
        }
    }

    private RedisInfo getRedisInfo() {
        try {
            Properties info = redisTemplate.execute((RedisCallback<Properties>) connection -> {
                return connection.info();
            });

            if (info == null) {
                return new RedisInfo("unknown", "0", "0", "0", "0", "0", "0", "0", "0");
            }

            return new RedisInfo(
                info.getProperty("redis_version", "unknown"),
                info.getProperty("used_memory", "0"),
                info.getProperty("used_memory_human", "0"),
                info.getProperty("maxmemory", "0"),
                info.getProperty("connected_clients", "0"),
                info.getProperty("total_connections_received", "0"),
                info.getProperty("keyspace_hits", "0"),
                info.getProperty("keyspace_misses", "0"),
                info.getProperty("uptime_in_seconds", "0")
            );
        } catch (Exception e) {
            logger.warn("Failed to get Redis info", e);
            return new RedisInfo("unknown", "0", "0", "0", "0", "0", "0", "0", "0");
        }
    }

    /**
     * Redis information record
     */
    private record RedisInfo(
        String version,
        String usedMemory,
        String usedMemoryHuman,
        String maxMemory,
        String connectedClients,
        String totalConnectionsReceived,
        String keyspaceHits,
        String keyspaceMisses,
        String uptimeInSeconds
    ) {}
}