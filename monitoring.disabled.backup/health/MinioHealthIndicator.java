package com.cafm.cafmbackend.monitoring.health;

import io.minio.MinioClient;
import io.minio.BucketExistsArgs;
import io.minio.ListBucketsResponse;
import io.minio.messages.Bucket;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * MinIO Health Indicator for CAFM Backend
 * 
 * Purpose: Monitors MinIO connectivity, bucket availability, and storage health
 * Pattern: Spring Boot HealthIndicator implementation for MinIO file storage
 * Java 23: Uses modern exception handling and record types
 * Architecture: Part of monitoring layer providing health checks for file storage
 * Standards: Implements Spring Boot Actuator health check standards
 */
@Component("minio")
public class MinioHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(MinioHealthIndicator.class);
    
    private final MinioClient minioClient;
    private final MeterRegistry meterRegistry;
    private final Timer healthCheckTimer;
    
    @Value("${app.minio.bucket.files:cafm-files}")
    private String filesBucket;
    
    @Value("${app.minio.bucket.images:cafm-images}")
    private String imagesBucket;

    public MinioHealthIndicator(MinioClient minioClient, MeterRegistry meterRegistry) {
        this.minioClient = minioClient;
        this.meterRegistry = meterRegistry;
        this.healthCheckTimer = Timer.builder("cafm.health.minio.check")
            .description("MinIO health check execution time")
            .register(meterRegistry);
    }

    @Override
    public Health health() {
        return healthCheckTimer.recordCallable(this::performHealthCheck);
    }

    private Health performHealthCheck() {
        Instant startTime = Instant.now();
        
        try {
            // Test basic connectivity by listing buckets
            ListBucketsResponse response = minioClient.listBuckets();
            List<Bucket> buckets = response.buckets();
            
            // Check required buckets exist
            BucketStatus filesBucketStatus = checkBucket(filesBucket);
            BucketStatus imagesBucketStatus = checkBucket(imagesBucket);
            
            Duration responseTime = Duration.between(startTime, Instant.now());
            
            // Determine health status based on bucket availability and response time
            Health.Builder healthBuilder;
            if (!filesBucketStatus.exists() || !imagesBucketStatus.exists()) {
                healthBuilder = Health.down()
                    .withDetail("error", "Required buckets not available");
            } else if (responseTime.toMillis() > 3000) {
                healthBuilder = Health.down()
                    .withDetail("error", "MinIO response time too slow");
            } else if (responseTime.toMillis() > 1000) {
                healthBuilder = Health.up()
                    .withDetail("warning", "MinIO response time is slow");
            } else {
                healthBuilder = Health.up();
            }

            return healthBuilder
                .withDetail("connectivity", "OK")
                .withDetail("response_time_ms", responseTime.toMillis())
                .withDetail("total_buckets", buckets.size())
                .withDetail("files_bucket", filesBucketStatus.toMap())
                .withDetail("images_bucket", imagesBucketStatus.toMap())
                .withDetail("available_buckets", buckets.stream()
                    .map(bucket -> bucket.name())
                    .toList())
                .withDetail("last_check", Instant.now())
                .build();

        } catch (Exception e) {
            logger.error("MinIO health check failed", e);
            
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("error_type", e.getClass().getSimpleName())
                .withDetail("timestamp", Instant.now())
                .build();
        }
    }

    private BucketStatus checkBucket(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
            
            return new BucketStatus(bucketName, exists, null);
        } catch (Exception e) {
            logger.warn("Failed to check bucket: {}", bucketName, e);
            return new BucketStatus(bucketName, false, e.getMessage());
        }
    }

    /**
     * Bucket status record
     */
    private record BucketStatus(
        String name,
        boolean exists,
        String error
    ) {
        public java.util.Map<String, Object> toMap() {
            var map = new java.util.HashMap<String, Object>();
            map.put("name", name);
            map.put("exists", exists);
            if (error != null) {
                map.put("error", error);
            }
            return map;
        }
    }
}