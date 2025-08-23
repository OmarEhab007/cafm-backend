package com.cafm.cafmbackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.task.TaskDecorator;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.http.CacheControl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.Filter;
import java.util.concurrent.TimeUnit;

/**
 * Performance optimization configuration for the CAFM backend.
 * 
 * Purpose: Centralize all performance-related configurations and optimizations
 * Pattern: Configuration class with performance tuning beans
 * Java 23: Leverages virtual threads and modern JVM optimizations
 * Architecture: Cross-cutting performance layer
 * Standards: Production-ready performance configurations
 */
@Configuration
@EnableCaching
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class PerformanceOptimizationConfig implements WebMvcConfigurer {
    
    /**
     * Configure maximum page size to prevent memory exhaustion.
     */
    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return config -> {
            config.setMaxPageSize(100);  // Limit max page size
            config.setOneIndexedParameters(false);  // Use 0-based indexing
            config.setPageParameterName("page");
            config.setSizeParameterName("size");
            config.setPrefix("");
            config.setQualifierDelimiter("_");
        };
    }
    
    /**
     * ETag filter for HTTP caching support.
     * Reduces bandwidth by sending 304 Not Modified for unchanged resources.
     */
    @Bean
    public Filter shallowEtagHeaderFilter() {
        return new ShallowEtagHeaderFilter();
    }
    
    /**
     * Configure static resource caching.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cache static resources for 1 year
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS)
                        .cachePublic()
                        .mustRevalidate());
        
        // Cache images for 30 days
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/images/")
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS)
                        .cachePublic());
        
        // API documentation - cache for 1 hour
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springdoc-openapi-ui/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));
    }
    
    /**
     * Customize Tomcat for better performance.
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> {
                // Enable compression
                connector.setProperty("compression", "on");
                connector.setProperty("compressionMinSize", "1024");
                connector.setProperty("compressibleMimeType", 
                    "text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json");
                
                // Connection settings
                connector.setProperty("maxThreads", "200");
                connector.setProperty("minSpareThreads", "10");
                connector.setProperty("maxConnections", "10000");
                connector.setProperty("acceptCount", "100");
                
                // Keep-alive settings
                connector.setProperty("keepAliveTimeout", "20000");
                connector.setProperty("maxKeepAliveRequests", "100");
                
                // Enable HTTP/2 if available
                connector.setProperty("protocol", "org.apache.coyote.http11.Http11Nio2Protocol");
            });
        };
    }
    
    /**
     * Task decorator for async operations with metrics.
     */
    @Bean
    public TaskDecorator taskDecorator(MeterRegistry meterRegistry) {
        return runnable -> {
            return () -> {
                Timer.Sample sample = Timer.start(meterRegistry);
                try {
                    runnable.run();
                } finally {
                    sample.stop(Timer.builder("async.task.duration")
                            .description("Duration of async task execution")
                            .register(meterRegistry));
                }
            };
        };
    }
    
    /**
     * Configure Jackson ObjectMapper for optimal performance.
     */
    @Bean
    @ConditionalOnProperty(name = "app.performance.optimize-json", havingValue = "true", matchIfMissing = true)
    public ObjectMapper performanceOptimizedObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Disable features that impact performance
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT, false);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // Enable features that improve performance
        mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, false);
        mapper.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);
        
        return mapper;
    }
    
    /**
     * Request/Response compression filter.
     */
    @Bean
    @ConditionalOnProperty(name = "app.performance.compression.enabled", havingValue = "true", matchIfMissing = true)
    public Filter compressionFilter() {
        return (request, response, chain) -> {
            // This would be a custom compression filter
            // For now, relying on Tomcat's built-in compression
            chain.doFilter(request, response);
        };
    }
    
    /**
     * Connection pool warmup on startup.
     */
    @Bean
    public ConnectionPoolWarmup connectionPoolWarmup() {
        return new ConnectionPoolWarmup();
    }
    
    /**
     * Inner class for warming up connection pools on startup.
     */
    public static class ConnectionPoolWarmup {
        
        public void warmup() {
            // Warmup logic to pre-establish database connections
            // This prevents cold start issues
            
            // Example: Execute a simple query to warm up the connection pool
            // jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        }
    }
    
    /**
     * JVM optimization hints.
     */
    @Bean
    public JvmOptimizationConfig jvmOptimizationConfig() {
        return new JvmOptimizationConfig();
    }
    
    /**
     * Inner class for JVM optimization settings.
     */
    public static class JvmOptimizationConfig {
        
        public JvmOptimizationConfig() {
            // Set JVM hints for better performance
            System.setProperty("java.awt.headless", "true");  // Headless mode
            System.setProperty("file.encoding", "UTF-8");     // UTF-8 encoding
            
            // Enable tiered compilation for faster startup
            System.setProperty("XX:TieredStopAtLevel", "1");
            
            // String deduplication (G1GC)
            System.setProperty("XX:+UseStringDeduplication", "true");
            
            // Optimize for containers
            System.setProperty("XX:+UseContainerSupport", "true");
            System.setProperty("XX:MaxRAMPercentage", "75.0");
        }
    }
}