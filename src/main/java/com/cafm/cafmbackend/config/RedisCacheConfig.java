package com.cafm.cafmbackend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration for performance optimization.
 * 
 * Purpose: Configure Redis caching with proper TTL and serialization
 * Pattern: Spring Cache abstraction with Redis backend
 * Java 23: Uses modern Duration API and type-safe configuration
 * Architecture: Centralized cache configuration for all services
 * Standards: Follows Spring Boot caching best practices
 */
// @Configuration - Disabled in favor of TenantAwareCacheConfig for multi-tenant security
// @ConditionalOnProperty(name = "cafm.cache.use-tenant-aware", havingValue = "false", matchIfMissing = false)
public class RedisCacheConfig implements CachingConfigurer {
    
    @Value("${spring.cache.redis.time-to-live:3600000}")
    private long defaultTtl;
    
    @Value("${spring.cache.redis.cache-null-values:false}")
    private boolean cacheNullValues;
    
    @Value("${spring.cache.redis.use-key-prefix:true}")
    private boolean useKeyPrefix;
    
    @Value("${spring.cache.redis.key-prefix:cafm:}")
    private String keyPrefix;
    
    /**
     * Configure object mapper for Redis serialization
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        
        // Configure polymorphic type handling for proper deserialization
        mapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        
        return mapper;
    }
    
    /**
     * Configure Redis cache manager with custom TTL for different cache regions
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMillis(defaultTtl))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper())));
        
        if (!cacheNullValues) {
            defaultConfig = defaultConfig.disableCachingNullValues();
        }
        
        if (useKeyPrefix) {
            defaultConfig = defaultConfig.prefixCacheNameWith(keyPrefix);
        }
        
        // Create cache manager with default configuration
        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .transactionAware();
        
        // Configure specific cache regions with custom TTL
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // User cache - 30 minutes
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // Company cache - 1 hour
        cacheConfigurations.put("companies", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // School cache - 1 hour
        cacheConfigurations.put("schools", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Asset cache - 30 minutes
        cacheConfigurations.put("assets", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // Report cache - 15 minutes
        cacheConfigurations.put("reports", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // Work order cache - 15 minutes
        cacheConfigurations.put("workOrders", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // Statistics cache - 5 minutes
        cacheConfigurations.put("statistics", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Permission cache - 1 hour
        cacheConfigurations.put("permissions", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Token blacklist - 24 hours
        cacheConfigurations.put("tokenBlacklist", defaultConfig.entryTtl(Duration.ofHours(24)));
        
        return builder.withInitialCacheConfigurations(cacheConfigurations).build();
    }
    
    /**
     * Customize cache manager for specific cache behaviors
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> builder
            .disableCreateOnMissingCache()
            .enableStatistics();
    }
    
    /**
     * Custom key generator for complex cache keys
     */
    @Bean("customKeyGenerator")
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName());
            sb.append(".");
            sb.append(method.getName());
            for (Object param : params) {
                sb.append(".");
                if (param != null) {
                    sb.append(param.toString());
                } else {
                    sb.append("null");
                }
            }
            return sb.toString();
        };
    }
    
    /**
     * Tenant-aware key generator for multi-tenant caching
     */
    @Bean("tenantAwareKeyGenerator")
    public KeyGenerator tenantAwareKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            
            // Get tenant context if available
            String tenantId = getTenantId();
            if (tenantId != null) {
                sb.append(tenantId);
                sb.append(":");
            }
            
            sb.append(target.getClass().getSimpleName());
            sb.append(".");
            sb.append(method.getName());
            for (Object param : params) {
                sb.append(".");
                if (param != null) {
                    sb.append(param.toString());
                } else {
                    sb.append("null");
                }
            }
            return sb.toString();
        };
    }
    
    /**
     * Get current tenant ID from context
     */
    private String getTenantId() {
        // This would typically get the tenant from TenantContext
        // For now, return null to use global cache
        return null;
    }
}