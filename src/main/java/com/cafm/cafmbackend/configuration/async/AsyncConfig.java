package com.cafm.cafmbackend.configuration.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async execution configuration for optimal performance.
 * 
 * Purpose: Configure thread pools for async operations with proper sizing and rejection policies
 * Pattern: Spring async configuration with multiple executors for different workloads
 * Java 23: Supports both platform and virtual threads based on operation type
 * Architecture: Infrastructure layer providing async execution capabilities
 * Standards: Production-ready thread pool configuration with monitoring
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);
    
    @Value("${app.async.core-pool-size:10}")
    private int corePoolSize;
    
    @Value("${app.async.max-pool-size:50}")
    private int maxPoolSize;
    
    @Value("${app.async.queue-capacity:500}")
    private int queueCapacity;
    
    @Value("${app.async.thread-name-prefix:Async-}")
    private String threadNamePrefix;
    
    @Value("${app.async.await-termination-seconds:60}")
    private int awaitTerminationSeconds;
    
    /**
     * Default async executor for general purpose async operations.
     * Uses platform threads for CPU-bound operations.
     */
    @Bean(name = "taskExecutor")
    @Primary
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - number of threads to keep alive
        executor.setCorePoolSize(corePoolSize);
        
        // Maximum pool size - maximum threads that can be created
        executor.setMaxPoolSize(maxPoolSize);
        
        // Queue capacity - number of tasks to queue before creating new threads
        executor.setQueueCapacity(queueCapacity);
        
        // Thread naming for better debugging
        executor.setThreadNamePrefix(threadNamePrefix);
        
        // Rejection policy - caller runs the task when queue is full
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        
        // Thread keep-alive time (in seconds)
        executor.setKeepAliveSeconds(60);
        
        // Allow core threads to timeout
        executor.setAllowCoreThreadTimeOut(true);
        
        // Initialize the executor
        executor.initialize();
        
        logger.info("Configured default async executor with core pool size: {}, max pool size: {}, queue capacity: {}",
                   corePoolSize, maxPoolSize, queueCapacity);
        
        return executor;
    }
    
    /**
     * I/O intensive executor using virtual threads (Java 23).
     * Optimal for I/O-bound operations like network calls, file operations.
     */
    @Bean(name = "ioTaskExecutor")
    public Executor ioTaskExecutor() {
        // Use virtual threads for I/O operations (Java 23 feature)
        var executor = java.util.concurrent.Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual()
                .name("IO-VirtualThread-", 0)
                .factory()
        );
        
        logger.info("Configured I/O task executor with virtual threads");
        return executor;
    }
    
    /**
     * Heavy computation executor with fixed thread pool.
     * For CPU-intensive operations that should not use virtual threads.
     */
    @Bean(name = "computationExecutor")
    public Executor computationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Fixed size pool based on CPU cores
        int processors = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(processors);
        executor.setMaxPoolSize(processors * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Compute-");
        
        // Abort policy for computation tasks - fail fast
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        logger.info("Configured computation executor with {} threads", processors);
        return executor;
    }
    
    /**
     * Database operations executor with limited concurrency.
     * Prevents database connection pool exhaustion.
     */
    @Bean(name = "dbTaskExecutor")
    public Executor dbTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Limited threads to prevent DB connection exhaustion
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("DB-");
        
        // Caller runs policy for DB operations
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        logger.info("Configured database task executor with limited concurrency");
        return executor;
    }
    
    /**
     * Notification executor for email and push notifications.
     * Uses virtual threads for better scalability.
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        // Virtual threads for notification delivery
        var executor = java.util.concurrent.Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual()
                .name("Notification-VThread-", 0)
                .factory()
        );
        
        logger.info("Configured notification executor with virtual threads");
        return executor;
    }
    
    /**
     * Scheduled task executor for periodic operations.
     */
    @Bean(name = "scheduledExecutor")
    public Executor scheduledExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("Scheduled-");
        
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(10);
        
        executor.initialize();
        
        logger.info("Configured scheduled task executor");
        return executor;
    }
    
    /**
     * Exception handler for async operations.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable throwable, Method method, Object... params) {
                logger.error("Uncaught exception in async method '{}' with params: {}", 
                           method.getName(), params, throwable);
                
                // Could send alerts or notifications here
                // notificationService.sendAlert("Async operation failed: " + method.getName());
            }
        };
    }
    
    /**
     * Monitoring bean for thread pool metrics.
     */
    @Bean
    public ThreadPoolMonitor threadPoolMonitor() {
        return new ThreadPoolMonitor();
    }
    
    /**
     * Inner class for monitoring thread pool metrics.
     */
    public static class ThreadPoolMonitor {
        
        public void logStats(ThreadPoolTaskExecutor executor, String name) {
            logger.info("Thread Pool [{}] - Active: {}, Pool Size: {}, Queue Size: {}, Completed: {}",
                       name,
                       executor.getActiveCount(),
                       executor.getPoolSize(),
                       executor.getThreadPoolExecutor().getQueue().size(),
                       executor.getThreadPoolExecutor().getCompletedTaskCount());
        }
        
        public boolean isHealthy(ThreadPoolTaskExecutor executor) {
            var threadPool = executor.getThreadPoolExecutor();
            var queueSize = threadPool.getQueue().size();
            var activeCount = executor.getActiveCount();
            var maxPoolSize = executor.getMaxPoolSize();
            
            // Health check: queue not full and threads available
            return queueSize < executor.getQueueCapacity() * 0.9 &&
                   activeCount < maxPoolSize * 0.9;
        }
    }
}