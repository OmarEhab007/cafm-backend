package com.cafm.cafmbackend.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Performance and load tests for CAFM Backend APIs.
 * Tests response times, throughput, and system behavior under load.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Tag("performance")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Performance & Load Tests")
class PerformanceLoadTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("cafm_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final int LIGHT_LOAD = 10;
    private static final int MEDIUM_LOAD = 50;
    private static final int HEAVY_LOAD = 100;
    private static final Duration ACCEPTABLE_RESPONSE_TIME = Duration.ofMillis(200);
    private static final Duration MAX_RESPONSE_TIME = Duration.ofMillis(1000);

    // ========== Response Time Tests ==========

    @Test
    @Order(1)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should respond to GET requests within acceptable time")
    void testResponseTime_GetRequests() throws Exception {
        // Warm up
        mockMvc.perform(get("/api/v1/users/profile")).andExpect(status().isOk());

        // Measure response times
        List<Duration> responseTimes = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            Instant start = Instant.now();
            mockMvc.perform(get("/api/v1/users/profile"))
                    .andExpect(status().isOk());
            Duration responseTime = Duration.between(start, Instant.now());
            responseTimes.add(responseTime);
        }

        // Calculate statistics
        Duration avgResponseTime = calculateAverage(responseTimes);
        Duration maxResponseTime = Collections.max(responseTimes);
        Duration minResponseTime = Collections.min(responseTimes);

        // Assertions
        assertTrue(avgResponseTime.compareTo(ACCEPTABLE_RESPONSE_TIME) <= 0,
                "Average response time " + avgResponseTime.toMillis() + "ms exceeds acceptable " + ACCEPTABLE_RESPONSE_TIME.toMillis() + "ms");
        assertTrue(maxResponseTime.compareTo(MAX_RESPONSE_TIME) <= 0,
                "Max response time " + maxResponseTime.toMillis() + "ms exceeds maximum " + MAX_RESPONSE_TIME.toMillis() + "ms");

        System.out.println("GET Response Times - Avg: " + avgResponseTime.toMillis() + 
                          "ms, Max: " + maxResponseTime.toMillis() + 
                          "ms, Min: " + minResponseTime.toMillis() + "ms");
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle POST requests efficiently")
    void testResponseTime_PostRequests() throws Exception {
        List<Duration> responseTimes = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            String userRequest = createUserRequest(i);
            
            Instant start = Instant.now();
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userRequest)
                            .with(csrf()))
                    .andExpect(status().isCreated());
            Duration responseTime = Duration.between(start, Instant.now());
            responseTimes.add(responseTime);
        }

        Duration avgResponseTime = calculateAverage(responseTimes);
        assertTrue(avgResponseTime.compareTo(Duration.ofMillis(300)) <= 0,
                "POST average response time exceeds 300ms");
    }

    // ========== Concurrent User Tests ==========

    @Test
    @Order(3)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle light concurrent load (10 users)")
    void testConcurrentUsers_LightLoad() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(LIGHT_LOAD);
        CountDownLatch latch = new CountDownLatch(LIGHT_LOAD);
        List<Future<TestResult>> futures = new ArrayList<>();

        for (int i = 0; i < LIGHT_LOAD; i++) {
            final int userId = i;
            Future<TestResult> future = executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await(); // Wait for all threads to be ready
                    
                    Instant start = Instant.now();
                    MvcResult result = mockMvc.perform(get("/api/v1/users/profile"))
                            .andReturn();
                    Duration responseTime = Duration.between(start, Instant.now());
                    
                    return new TestResult(
                            result.getResponse().getStatus() == 200,
                            responseTime,
                            userId
                    );
                } catch (Exception e) {
                    return new TestResult(false, Duration.ZERO, userId);
                }
            });
            futures.add(future);
        }

        // Collect results
        List<TestResult> results = new ArrayList<>();
        for (Future<TestResult> future : futures) {
            results.add(future.get(10, TimeUnit.SECONDS));
        }

        executor.shutdown();

        // Analyze results
        long successCount = results.stream().filter(r -> r.success).count();
        double successRate = (successCount * 100.0) / LIGHT_LOAD;
        Duration avgResponseTime = calculateAverageFromResults(results);

        assertTrue(successRate >= 95, "Success rate " + successRate + "% is below 95%");
        assertTrue(avgResponseTime.compareTo(Duration.ofMillis(500)) <= 0,
                "Average response time under light load exceeds 500ms");

        System.out.println("Light Load Test - Success Rate: " + successRate + 
                          "%, Avg Response: " + avgResponseTime.toMillis() + "ms");
    }

    @Test
    @Order(4)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle medium concurrent load (50 users)")
    void testConcurrentUsers_MediumLoad() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(MEDIUM_LOAD);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(MEDIUM_LOAD);
        List<Future<TestResult>> futures = new ArrayList<>();

        for (int i = 0; i < MEDIUM_LOAD; i++) {
            final int userId = i;
            Future<TestResult> future = executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for start signal
                    
                    Instant start = Instant.now();
                    MvcResult result = mockMvc.perform(get("/api/v1/users")
                            .param("page", "0")
                            .param("size", "10"))
                            .andReturn();
                    Duration responseTime = Duration.between(start, Instant.now());
                    
                    completeLatch.countDown();
                    return new TestResult(
                            result.getResponse().getStatus() == 200,
                            responseTime,
                            userId
                    );
                } catch (Exception e) {
                    completeLatch.countDown();
                    return new TestResult(false, Duration.ZERO, userId);
                }
            });
            futures.add(future);
        }

        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for completion
        assertTrue(completeLatch.await(30, TimeUnit.SECONDS), "Not all requests completed within 30 seconds");

        // Collect results
        List<TestResult> results = new ArrayList<>();
        for (Future<TestResult> future : futures) {
            results.add(future.get());
        }

        executor.shutdown();

        // Analyze results
        long successCount = results.stream().filter(r -> r.success).count();
        double successRate = (successCount * 100.0) / MEDIUM_LOAD;
        Duration avgResponseTime = calculateAverageFromResults(results);
        Duration maxResponseTime = results.stream()
                .map(r -> r.responseTime)
                .max(Duration::compareTo)
                .orElse(Duration.ZERO);

        assertTrue(successRate >= 90, "Success rate " + successRate + "% is below 90%");
        assertTrue(avgResponseTime.compareTo(Duration.ofSeconds(1)) <= 0,
                "Average response time under medium load exceeds 1 second");

        System.out.println("Medium Load Test - Success Rate: " + successRate + 
                          "%, Avg Response: " + avgResponseTime.toMillis() + 
                          "ms, Max Response: " + maxResponseTime.toMillis() + "ms");
    }

    // ========== Stress Tests ==========

    @Test
    @Order(5)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle heavy concurrent load (100 users)")
    void testConcurrentUsers_HeavyLoad() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(HEAVY_LOAD);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<TestResult>> futures = new ArrayList<>();

        for (int i = 0; i < HEAVY_LOAD; i++) {
            final int userId = i;
            Future<TestResult> future = executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    // Mix of different operations
                    String operation = getRandomOperation(userId);
                    Instant start = Instant.now();
                    MvcResult result = performOperation(operation, userId);
                    Duration responseTime = Duration.between(start, Instant.now());
                    
                    return new TestResult(
                            result.getResponse().getStatus() < 500,
                            responseTime,
                            userId
                    );
                } catch (Exception e) {
                    return new TestResult(false, Duration.ZERO, userId);
                }
            });
            futures.add(future);
        }

        startLatch.countDown();

        // Collect results with timeout
        List<TestResult> results = new ArrayList<>();
        for (Future<TestResult> future : futures) {
            try {
                results.add(future.get(60, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                results.add(new TestResult(false, Duration.ofSeconds(60), -1));
            }
        }

        executor.shutdown();

        // Analyze results
        long successCount = results.stream().filter(r -> r.success).count();
        double successRate = (successCount * 100.0) / HEAVY_LOAD;
        Duration avgResponseTime = calculateAverageFromResults(results);

        assertTrue(successRate >= 80, "Success rate " + successRate + "% is below 80% under heavy load");
        
        System.out.println("Heavy Load Test - Success Rate: " + successRate + 
                          "%, Avg Response: " + avgResponseTime.toMillis() + "ms");
    }

    // ========== Database Connection Pool Tests ==========

    @Test
    @Order(6)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should efficiently use database connection pool")
    void testDatabaseConnectionPool() throws Exception {
        int concurrentRequests = 30;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        List<Future<TestResult>> futures = new ArrayList<>();

        for (int i = 0; i < concurrentRequests; i++) {
            final int requestId = i;
            Future<TestResult> future = executor.submit(() -> {
                try {
                    // Database-heavy operation
                    Instant start = Instant.now();
                    MvcResult result = mockMvc.perform(get("/api/v1/reports")
                            .param("page", "0")
                            .param("size", "100"))
                            .andReturn();
                    Duration responseTime = Duration.between(start, Instant.now());
                    
                    return new TestResult(
                            result.getResponse().getStatus() == 200,
                            responseTime,
                            requestId
                    );
                } catch (Exception e) {
                    return new TestResult(false, Duration.ZERO, requestId);
                }
            });
            futures.add(future);
        }

        // Collect results
        List<TestResult> results = new ArrayList<>();
        for (Future<TestResult> future : futures) {
            results.add(future.get(30, TimeUnit.SECONDS));
        }

        executor.shutdown();

        // All requests should succeed without connection pool exhaustion
        long successCount = results.stream().filter(r -> r.success).count();
        assertEquals(concurrentRequests, successCount, "Some requests failed due to connection pool issues");
    }

    // ========== Memory & Resource Tests ==========

    @Test
    @Order(7)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle large payload efficiently")
    void testLargePayload() throws Exception {
        // Create large report with many details
        String largePayload = createLargeReportPayload();
        
        Instant start = Instant.now();
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(largePayload)
                        .with(csrf()))
                .andExpect(status().isCreated());
        Duration responseTime = Duration.between(start, Instant.now());
        
        assertTrue(responseTime.compareTo(Duration.ofSeconds(2)) <= 0,
                "Large payload processing exceeds 2 seconds");
    }

    @Test
    @Order(8)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle pagination efficiently")
    void testPaginationPerformance() throws Exception {
        List<Duration> responseTimes = new ArrayList<>();
        
        // Test different page sizes
        int[] pageSizes = {10, 50, 100, 200};
        
        for (int pageSize : pageSizes) {
            Instant start = Instant.now();
            mockMvc.perform(get("/api/v1/users")
                            .param("page", "0")
                            .param("size", String.valueOf(pageSize)))
                    .andExpect(status().isOk());
            Duration responseTime = Duration.between(start, Instant.now());
            responseTimes.add(responseTime);
            
            System.out.println("Page size " + pageSize + ": " + responseTime.toMillis() + "ms");
        }
        
        // Response time should not grow linearly with page size
        Duration smallPageTime = responseTimes.get(0);
        Duration largePageTime = responseTimes.get(responseTimes.size() - 1);
        
        assertTrue(largePageTime.toMillis() < smallPageTime.toMillis() * 10,
                "Response time grows too much with page size");
    }

    // ========== Throughput Tests ==========

    @Test
    @Order(9)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should maintain acceptable throughput")
    void testThroughput() throws Exception {
        int totalRequests = 100;
        Instant startTime = Instant.now();
        
        for (int i = 0; i < totalRequests; i++) {
            mockMvc.perform(get("/api/v1/users/profile"))
                    .andExpect(status().isOk());
        }
        
        Duration totalTime = Duration.between(startTime, Instant.now());
        double throughput = (totalRequests * 1000.0) / totalTime.toMillis(); // requests per second
        
        assertTrue(throughput >= 10, "Throughput " + throughput + " req/s is below minimum 10 req/s");
        
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " requests/second");
    }

    // ========== Cache Performance Tests ==========

    @Test
    @Order(10)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should demonstrate cache effectiveness")
    void testCachePerformance() throws Exception {
        String endpoint = "/api/v1/companies/" + UUID.randomUUID();
        
        // First request (cache miss)
        Instant start1 = Instant.now();
        mockMvc.perform(get(endpoint))
                .andExpect(status().isOk());
        Duration firstRequestTime = Duration.between(start1, Instant.now());
        
        // Subsequent requests (cache hits)
        List<Duration> cachedRequestTimes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Instant start = Instant.now();
            mockMvc.perform(get(endpoint))
                    .andExpect(status().isOk());
            Duration responseTime = Duration.between(start, Instant.now());
            cachedRequestTimes.add(responseTime);
        }
        
        Duration avgCachedTime = calculateAverage(cachedRequestTimes);
        
        // Cached requests should be significantly faster
        assertTrue(avgCachedTime.toMillis() < firstRequestTime.toMillis() * 0.5,
                "Cache is not providing expected performance improvement");
        
        System.out.println("Cache Performance - First request: " + firstRequestTime.toMillis() + 
                          "ms, Avg cached: " + avgCachedTime.toMillis() + "ms");
    }

    // ========== Helper Methods ==========

    private Duration calculateAverage(List<Duration> durations) {
        long totalMillis = durations.stream()
                .mapToLong(Duration::toMillis)
                .sum();
        return Duration.ofMillis(totalMillis / durations.size());
    }

    private Duration calculateAverageFromResults(List<TestResult> results) {
        long totalMillis = results.stream()
                .mapToLong(r -> r.responseTime.toMillis())
                .sum();
        return Duration.ofMillis(totalMillis / results.size());
    }

    private String createUserRequest(int index) {
        return String.format("""
                {
                    "email": "perftest%d@example.com",
                    "password": "Password123!",
                    "firstName": "Perf",
                    "lastName": "Test%d",
                    "userType": "TECHNICIAN",
                    "companyId": "%s"
                }
                """, index, index, UUID.randomUUID());
    }

    private String createLargeReportPayload() {
        StringBuilder description = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            description.append("This is line ").append(i).append(" of a very detailed report description. ");
        }
        
        return String.format("""
                {
                    "title": "Large Performance Test Report",
                    "description": "%s",
                    "priority": "HIGH",
                    "category": "HVAC",
                    "schoolId": "%s",
                    "location": "Building A, Multiple Rooms",
                    "attachments": [%s]
                }
                """, 
                description.toString(),
                UUID.randomUUID(),
                IntStream.range(0, 20)
                    .mapToObj(i -> "\"image" + i + ".jpg\"")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("")
        );
    }

    private String getRandomOperation(int userId) {
        String[] operations = {"GET_USERS", "GET_REPORTS", "GET_COMPANIES", "GET_PROFILE"};
        return operations[userId % operations.length];
    }

    private MvcResult performOperation(String operation, int userId) throws Exception {
        return switch (operation) {
            case "GET_USERS" -> mockMvc.perform(get("/api/v1/users")
                    .param("page", "0")
                    .param("size", "10"))
                    .andReturn();
            case "GET_REPORTS" -> mockMvc.perform(get("/api/v1/reports")
                    .param("page", "0")
                    .param("size", "10"))
                    .andReturn();
            case "GET_COMPANIES" -> mockMvc.perform(get("/api/v1/companies")
                    .param("page", "0")
                    .param("size", "10"))
                    .andReturn();
            default -> mockMvc.perform(get("/api/v1/users/profile"))
                    .andReturn();
        };
    }

    // Inner class for test results
    private static class TestResult {
        final boolean success;
        final Duration responseTime;
        final int userId;

        TestResult(boolean success, Duration responseTime, int userId) {
            this.success = success;
            this.responseTime = responseTime;
            this.userId = userId;
        }
    }
}