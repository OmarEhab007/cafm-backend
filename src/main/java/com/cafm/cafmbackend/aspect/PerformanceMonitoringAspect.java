package com.cafm.cafmbackend.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Aspect for monitoring performance of critical methods.
 * 
 * Purpose: Track execution time, identify bottlenecks, and alert on slow operations
 * Pattern: AOP cross-cutting concern for performance monitoring
 * Java 23: Uses record patterns for performance data
 * Architecture: Aspect layer for non-invasive monitoring
 * Standards: Integrates with Micrometer for metrics collection
 */
@Aspect
@Component
public class PerformanceMonitoringAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);
    
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, MethodPerformanceStats> performanceStats;
    
    @Value("${app.performance.slow-method-threshold-ms:1000}")
    private long slowMethodThresholdMs;
    
    @Value("${app.performance.monitoring.enabled:true}")
    private boolean monitoringEnabled;
    
    @Value("${app.performance.log-slow-queries:true}")
    private boolean logSlowQueries;
    
    public PerformanceMonitoringAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.performanceStats = new ConcurrentHashMap<>();
    }
    
    /**
     * Custom annotation for marking methods to monitor.
     */
    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MonitorPerformance {
        String value() default "";
        boolean logArgs() default false;
        boolean logResult() default false;
    }
    
    /**
     * Pointcut for all repository methods (database operations).
     */
    @Pointcut("execution(* com.cafm.cafmbackend.data.repository..*(..))")
    public void repositoryMethods() {}
    
    /**
     * Pointcut for all service methods (business logic).
     */
    @Pointcut("execution(* com.cafm.cafmbackend.service..*(..))")
    public void serviceMethods() {}
    
    /**
     * Pointcut for all controller methods (API endpoints).
     */
    @Pointcut("execution(* com.cafm.cafmbackend.api.controllers..*(..))")
    public void controllerMethods() {}
    
    /**
     * Pointcut for methods annotated with @MonitorPerformance.
     */
    @Pointcut("@annotation(monitorPerformance)")
    public void monitoredMethods(MonitorPerformance monitorPerformance) {}
    
    /**
     * Monitor repository method performance (database queries).
     */
    @Around("repositoryMethods()")
    public Object monitorRepositoryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!monitoringEnabled) {
            return joinPoint.proceed();
        }
        
        String methodName = joinPoint.getSignature().toShortString();
        Timer.Sample sample = Timer.start(meterRegistry);
        
        long startTime = System.currentTimeMillis();
        Object result = null;
        boolean success = false;
        
        try {
            result = joinPoint.proceed();
            success = true;
            return result;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record metrics
            sample.stop(Timer.builder("db.query.duration")
                    .tag("method", methodName)
                    .tag("status", success ? "success" : "failure")
                    .description("Database query execution time")
                    .register(meterRegistry));
            
            // Update stats
            updateStats("repository", methodName, executionTime, success);
            
            // Log slow queries
            if (logSlowQueries && executionTime > slowMethodThresholdMs) {
                logger.warn("SLOW QUERY detected: {} took {} ms", methodName, executionTime);
                
                // Log query details if available
                if (result instanceof java.util.Collection<?> collection) {
                    logger.warn("Query returned {} results", collection.size());
                }
            }
        }
    }
    
    /**
     * Monitor service method performance (business logic).
     */
    @Around("serviceMethods()")
    public Object monitorServicePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!monitoringEnabled) {
            return joinPoint.proceed();
        }
        
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;
        
        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();
        
        Object result = null;
        boolean success = false;
        
        try {
            result = joinPoint.proceed();
            success = true;
            return result;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record metrics
            sample.stop(Timer.builder("service.method.duration")
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("status", success ? "success" : "failure")
                    .description("Service method execution time")
                    .register(meterRegistry));
            
            // Update stats
            updateStats("service", fullMethodName, executionTime, success);
            
            // Log slow methods
            if (executionTime > slowMethodThresholdMs * 2) {
                logger.warn("SLOW SERVICE METHOD: {} took {} ms", fullMethodName, executionTime);
            }
        }
    }
    
    /**
     * Monitor controller method performance (API endpoints).
     */
    @Around("controllerMethods()")
    public Object monitorControllerPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!monitoringEnabled) {
            return joinPoint.proceed();
        }
        
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String endpoint = className + "." + methodName;
        
        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();
        
        Object result = null;
        boolean success = false;
        
        try {
            result = joinPoint.proceed();
            success = true;
            return result;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record metrics
            sample.stop(Timer.builder("http.request.duration")
                    .tag("controller", className)
                    .tag("method", methodName)
                    .tag("status", success ? "success" : "failure")
                    .description("API endpoint execution time")
                    .register(meterRegistry));
            
            // Update stats
            updateStats("controller", endpoint, executionTime, success);
            
            // Log slow endpoints
            if (executionTime > slowMethodThresholdMs) {
                logger.warn("SLOW API ENDPOINT: {} took {} ms", endpoint, executionTime);
            }
            
            // Alert on very slow endpoints
            if (executionTime > slowMethodThresholdMs * 5) {
                logger.error("CRITICAL: API endpoint {} took {} ms - possible performance issue!", 
                           endpoint, executionTime);
                // Could trigger alerts here
            }
        }
    }
    
    /**
     * Monitor methods with custom @MonitorPerformance annotation.
     */
    @Around("monitoredMethods(monitorPerformance)")
    public Object monitorCustomPerformance(ProceedingJoinPoint joinPoint, 
                                          MonitorPerformance monitorPerformance) throws Throwable {
        if (!monitoringEnabled) {
            return joinPoint.proceed();
        }
        
        String methodName = monitorPerformance.value().isEmpty() 
            ? joinPoint.getSignature().toShortString() 
            : monitorPerformance.value();
        
        long startTime = System.currentTimeMillis();
        
        // Log method arguments if requested
        if (monitorPerformance.logArgs()) {
            logger.debug("Executing {} with args: {}", methodName, joinPoint.getArgs());
        }
        
        Object result = null;
        boolean success = false;
        
        try {
            result = joinPoint.proceed();
            success = true;
            
            // Log result if requested
            if (monitorPerformance.logResult()) {
                logger.debug("Method {} returned: {}", methodName, result);
            }
            
            return result;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record custom metric
            meterRegistry.timer("custom.method.duration", "method", methodName)
                        .record(executionTime, java.util.concurrent.TimeUnit.MILLISECONDS);
            
            logger.info("Method {} executed in {} ms", methodName, executionTime);
        }
    }
    
    /**
     * Update performance statistics for a method.
     */
    private void updateStats(String type, String methodName, long executionTime, boolean success) {
        String key = type + ":" + methodName;
        
        performanceStats.compute(key, (k, stats) -> {
            if (stats == null) {
                stats = new MethodPerformanceStats(methodName, type);
            }
            stats.recordExecution(executionTime, success);
            return stats;
        });
        
        // Check if method is consistently slow
        MethodPerformanceStats stats = performanceStats.get(key);
        if (stats != null && stats.getAverageTime() > slowMethodThresholdMs && stats.getCallCount() > 10) {
            logger.warn("Performance Alert: {} consistently slow - Avg: {} ms, Count: {}", 
                       methodName, stats.getAverageTime(), stats.getCallCount());
        }
    }
    
    /**
     * Get performance statistics for analysis.
     */
    public ConcurrentHashMap<String, MethodPerformanceStats> getPerformanceStats() {
        return new ConcurrentHashMap<>(performanceStats);
    }
    
    /**
     * Reset performance statistics.
     */
    public void resetStats() {
        performanceStats.clear();
        logger.info("Performance statistics reset");
    }
    
    /**
     * Inner class for tracking method performance statistics.
     */
    public static class MethodPerformanceStats {
        private final String methodName;
        private final String type;
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong callCount = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        private volatile long minTime = Long.MAX_VALUE;
        private volatile long maxTime = 0;
        private volatile long lastExecutionTime = 0;
        
        public MethodPerformanceStats(String methodName, String type) {
            this.methodName = methodName;
            this.type = type;
        }
        
        public void recordExecution(long executionTime, boolean success) {
            totalTime.addAndGet(executionTime);
            callCount.incrementAndGet();
            
            if (!success) {
                errorCount.incrementAndGet();
            }
            
            lastExecutionTime = executionTime;
            
            // Update min/max
            if (executionTime < minTime) {
                minTime = executionTime;
            }
            if (executionTime > maxTime) {
                maxTime = executionTime;
            }
        }
        
        public long getAverageTime() {
            long count = callCount.get();
            return count > 0 ? totalTime.get() / count : 0;
        }
        
        public long getCallCount() {
            return callCount.get();
        }
        
        public long getErrorCount() {
            return errorCount.get();
        }
        
        public double getErrorRate() {
            long count = callCount.get();
            return count > 0 ? (double) errorCount.get() / count * 100 : 0;
        }
        
        public long getMinTime() {
            return minTime == Long.MAX_VALUE ? 0 : minTime;
        }
        
        public long getMaxTime() {
            return maxTime;
        }
        
        public long getLastExecutionTime() {
            return lastExecutionTime;
        }
        
        @Override
        public String toString() {
            return String.format("%s [%s] - Calls: %d, Avg: %d ms, Min: %d ms, Max: %d ms, Errors: %d (%.2f%%)",
                methodName, type, callCount.get(), getAverageTime(), 
                getMinTime(), maxTime, errorCount.get(), getErrorRate());
        }
    }
}