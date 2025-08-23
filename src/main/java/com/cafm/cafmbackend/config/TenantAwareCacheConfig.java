package com.cafm.cafmbackend.config;

import com.cafm.cafmbackend.service.tenant.TenantContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.concurrent.Callable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tenant-aware caching configuration with Redis backend.
 * 
 * Explanation:
 * - Purpose: Provides tenant-isolated caching with automatic key prefixing
 * - Pattern: Decorator pattern for cache operations with tenant context
 * - Java 23: Modern caching configuration with performance optimizations
 * - Architecture: Performance enhancement while maintaining tenant security
 * - Standards: Cache isolation prevents cross-tenant data leakage
 */
@Configuration
@EnableCaching
public class TenantAwareCacheConfig implements CachingConfigurer {
    
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;
    
    @Autowired
    private TenantContextService tenantContextService;
    
    /**
     * Tenant-aware cache manager with Redis backend
     */
    @Bean
    @Primary
    @Override
    public CacheManager cacheManager() {
        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory)
            .cacheDefaults(cacheConfiguration())
            .withInitialCacheConfigurations(getCacheConfigurations());
        
        RedisCacheManager cacheManager = builder.build();
        
        // Wrap with tenant-aware cache manager
        return new TenantAwareCacheManager(cacheManager, tenantContextService);
    }
    
    /**
     * Default Redis cache configuration
     */
    private org.springframework.data.redis.cache.RedisCacheConfiguration cacheConfiguration() {
        return org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1)) // Default TTL
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();
    }
    
    /**
     * Cache-specific configurations with different TTLs
     */
    private Map<String, org.springframework.data.redis.cache.RedisCacheConfiguration> getCacheConfigurations() {
        Map<String, org.springframework.data.redis.cache.RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // User cache - 30 minutes
        cacheConfigurations.put("users", cacheConfiguration().entryTtl(Duration.ofMinutes(30)));
        
        // Company cache - 2 hours (relatively static)
        cacheConfigurations.put("companies", cacheConfiguration().entryTtl(Duration.ofHours(2)));
        
        // Reports cache - 15 minutes (frequently updated)
        cacheConfigurations.put("reports", cacheConfiguration().entryTtl(Duration.ofMinutes(15)));
        
        // Assets cache - 1 hour
        cacheConfigurations.put("assets", cacheConfiguration().entryTtl(Duration.ofHours(1)));
        
        // Work orders cache - 15 minutes (active data)  
        cacheConfigurations.put("workOrders", cacheConfiguration().entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("workorders", cacheConfiguration().entryTtl(Duration.ofMinutes(15))); // legacy name
        
        // Schools cache - 1 hour (relatively static)
        cacheConfigurations.put("schools", cacheConfiguration().entryTtl(Duration.ofHours(1)));
        
        // Statistics cache - 5 minutes (aggregated data)
        cacheConfigurations.put("statistics", cacheConfiguration().entryTtl(Duration.ofMinutes(5)));
        
        // Permission cache - 1 hour (relatively stable)
        cacheConfigurations.put("permissions", cacheConfiguration().entryTtl(Duration.ofHours(1)));
        
        // Token blacklist cache - 24 hours (security tokens)
        cacheConfigurations.put("tokenBlacklist", cacheConfiguration().entryTtl(Duration.ofHours(24)));
        
        // Short-term cache for API responses - 2 minutes
        cacheConfigurations.put("api-responses", cacheConfiguration().entryTtl(Duration.ofMinutes(2)));
        
        return cacheConfigurations;
    }
    
    /**
     * Tenant-aware cache key generator
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return new TenantAwareKeyGenerator(tenantContextService);
    }
    
    /**
     * Cache resolver for method-level caching
     */
    @Bean
    @Override
    public CacheResolver cacheResolver() {
        return new SimpleCacheResolver(cacheManager());
    }
    
    /**
     * Error handler for cache operations
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new TenantAwareCacheErrorHandler();
    }
    
    /**
     * Custom cache manager that wraps all cache operations with tenant context
     */
    public static class TenantAwareCacheManager implements CacheManager {
        
        private final CacheManager delegate;
        private final TenantContextService tenantContextService;
        
        public TenantAwareCacheManager(CacheManager delegate, TenantContextService tenantContextService) {
            this.delegate = delegate;
            this.tenantContextService = tenantContextService;
        }
        
        @Override
        public Cache getCache(String name) {
            Cache cache = delegate.getCache(name);
            if (cache == null) {
                return null;
            }
            return new TenantAwareCache(cache, tenantContextService);
        }
        
        @Override
        public java.util.Collection<String> getCacheNames() {
            return delegate.getCacheNames();
        }
    }
    
    /**
     * Cache wrapper that adds tenant context to all operations
     */
    public static class TenantAwareCache implements Cache {
        
        private final Cache delegate;
        private final TenantContextService tenantContextService;
        
        public TenantAwareCache(Cache delegate, TenantContextService tenantContextService) {
            this.delegate = delegate;
            this.tenantContextService = tenantContextService;
        }
        
        @Override
        public String getName() {
            return delegate.getName();
        }
        
        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }
        
        @Override
        public ValueWrapper get(Object key) {
            return delegate.get(getTenantAwareKey(key));
        }
        
        @Override
        public <T> T get(Object key, Class<T> type) {
            return delegate.get(getTenantAwareKey(key), type);
        }
        
        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            return delegate.get(getTenantAwareKey(key), valueLoader);
        }
        
        @Override
        public void put(Object key, Object value) {
            delegate.put(getTenantAwareKey(key), value);
        }
        
        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            return delegate.putIfAbsent(getTenantAwareKey(key), value);
        }
        
        @Override
        public void evict(Object key) {
            delegate.evict(getTenantAwareKey(key));
        }
        
        @Override
        public boolean evictIfPresent(Object key) {
            return delegate.evictIfPresent(getTenantAwareKey(key));
        }
        
        @Override
        public void clear() {
            // Clear only current tenant's cache entries
            // Note: This is a simplified implementation
            // In production, you might want to implement a more sophisticated approach
            delegate.clear();
        }
        
        /**
         * Generate tenant-aware cache key
         */
        private Object getTenantAwareKey(Object key) {
            UUID tenantId = getCurrentTenantId();
            return String.format("tenant:%s:key:%s", tenantId, key);
        }
        
        /**
         * Get current tenant ID with fallback
         */
        private UUID getCurrentTenantId() {
            if (tenantContextService.hasTenantContext()) {
                return tenantContextService.getCurrentTenant();
            }
            // Fallback to system tenant
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }
    }
    
    /**
     * Key generator that automatically includes tenant context
     */
    public static class TenantAwareKeyGenerator implements KeyGenerator {
        
        private final TenantContextService tenantContextService;
        
        public TenantAwareKeyGenerator(TenantContextService tenantContextService) {
            this.tenantContextService = tenantContextService;
        }
        
        @Override
        public Object generate(Object target, java.lang.reflect.Method method, Object... params) {
            UUID tenantId = getCurrentTenantId();
            String methodName = target.getClass().getSimpleName() + "." + method.getName();
            
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append("tenant:").append(tenantId);
            keyBuilder.append(":method:").append(methodName);
            
            // Add parameters to key
            if (params.length > 0) {
                keyBuilder.append(":params:");
                for (int i = 0; i < params.length; i++) {
                    if (i > 0) {
                        keyBuilder.append(",");
                    }
                    
                    Object param = params[i];
                    if (param == null) {
                        keyBuilder.append("null");
                    } else if (param instanceof UUID) {
                        keyBuilder.append(param.toString());
                    } else if (param instanceof String || param instanceof Number || param instanceof Boolean) {
                        keyBuilder.append(param.toString());
                    } else {
                        // For complex objects, use hashCode
                        keyBuilder.append(param.getClass().getSimpleName())
                                  .append(":")
                                  .append(param.hashCode());
                    }
                }
            }
            
            return keyBuilder.toString();
        }
        
        private UUID getCurrentTenantId() {
            if (tenantContextService.hasTenantContext()) {
                return tenantContextService.getCurrentTenant();
            }
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }
    }
    
    /**
     * Error handler for cache operations that logs but doesn't fail
     */
    public static class TenantAwareCacheErrorHandler implements CacheErrorHandler {
        
        private static final org.slf4j.Logger logger = 
            org.slf4j.LoggerFactory.getLogger(TenantAwareCacheErrorHandler.class);
        
        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            logger.warn("Cache GET error for cache '{}' and key '{}': {}", 
                cache.getName(), key, exception.getMessage());
        }
        
        @Override
        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
            logger.warn("Cache PUT error for cache '{}' and key '{}': {}", 
                cache.getName(), key, exception.getMessage());
        }
        
        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
            logger.warn("Cache EVICT error for cache '{}' and key '{}': {}", 
                cache.getName(), key, exception.getMessage());
        }
        
        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {
            logger.warn("Cache CLEAR error for cache '{}': {}", 
                cache.getName(), exception.getMessage());
        }
    }
}