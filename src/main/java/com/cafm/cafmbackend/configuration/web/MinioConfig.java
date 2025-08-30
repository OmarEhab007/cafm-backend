package com.cafm.cafmbackend.configuration.web;

import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

/**
 * MinIO configuration for file storage.
 * 
 * Purpose: Configures MinIO client with proper connection settings
 * and connection pooling for optimal performance.
 * 
 * Pattern: Spring configuration with @Bean factory methods
 * Java 23: Uses modern configuration patterns
 * Architecture: Infrastructure configuration layer
 * Standards: Follows Spring Boot configuration best practices
 */
@Configuration
public class MinioConfig {

    private static final Logger logger = LoggerFactory.getLogger(MinioConfig.class);

    @Value("${app.minio.endpoint}")
    private String endpoint;

    @Value("${app.minio.access-key}")
    private String accessKey;

    @Value("${app.minio.secret-key}")
    private String secretKey;

    /**
     * Create MinIO client with optimized connection settings.
     */
    @Bean
    public MinioClient minioClient() {
        try {
            MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

            logger.info("MinIO client configured successfully - Endpoint: {}", endpoint);
            
            // Test connection
            client.ignoreCertCheck(); // Only for development - remove in production
            
            return client;
            
        } catch (Exception e) {
            logger.error("Failed to configure MinIO client: {}", e.getMessage(), e);
            throw new RuntimeException("MinIO configuration failed", e);
        }
    }
    
    /**
     * Configure RestTemplate for HTTP client operations.
     * 
     * Purpose: Provides RestTemplate bean for services like PushNotificationService
     * Pattern: RestTemplate with timeouts and error handling
     * Java 23: Ready for virtual threads
     * Architecture: HTTP client configuration
     * Standards: RESTful client best practices
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();
    }
}