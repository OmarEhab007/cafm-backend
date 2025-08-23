# Performance Optimization Guide

## Overview

This document details the performance optimizations implemented in the CAFM Backend and provides guidelines for maintaining and improving system performance.

## Performance Architecture

```
┌─────────────┐     ┌──────────┐     ┌─────────────┐
│   Client    │────▶│   CDN    │────▶│Load Balancer│
└─────────────┘     └──────────┘     └─────────────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    │                        │                        │
              ┌─────▼─────┐           ┌─────▼─────┐           ┌─────▼─────┐
              │   App 1   │           │   App 2   │           │   App 3   │
              └─────┬─────┘           └─────┬─────┘           └─────┬─────┘
                    │                        │                        │
         ┌──────────┼────────────────────────┼────────────────────────┼──────────┐
         │          │                        │                        │          │
    ┌────▼────┐ ┌───▼───┐          ┌────────▼────────┐          ┌───▼───┐ ┌────▼────┐
    │  Redis  │ │ Redis │          │   PostgreSQL    │          │  S3   │ │  MinIO  │
    │ (Cache) │ │(Queue)│          │   (Primary)     │          │(Files)│ │ (Files) │
    └─────────┘ └───────┘          └─────────────────┘          └───────┘ └─────────┘
```

## Caching Strategy

### Redis Cache Configuration

#### Cache Regions and TTLs
```java
@Configuration
public class RedisCacheConfig {
    
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> builder
            .withCacheConfiguration("users",
                defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration("companies",
                defaultConfig.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration("reports",
                defaultConfig.entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration("workOrders",
                defaultConfig.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration("assets",
                defaultConfig.entryTtl(Duration.ofHours(2)))
            .withCacheConfiguration("schools",
                defaultConfig.entryTtl(Duration.ofHours(4)))
            .withCacheConfiguration("statistics",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));
    }
}
```

### Cache Usage Patterns

#### 1. Entity Caching
```java
@Cacheable(value = "users", key = "#id", unless = "#result == null")
public User findById(UUID id) {
    return userRepository.findById(id).orElse(null);
}

@CacheEvict(value = "users", key = "#user.id")
public User update(User user) {
    return userRepository.save(user);
}

@CacheEvict(value = "users", allEntries = true)
public void bulkUpdate(List<User> users) {
    userRepository.saveAll(users);
}
```

#### 2. Query Result Caching
```java
@Cacheable(value = "reports", 
           key = "'company-' + #companyId + '-status-' + #status + '-page-' + #page")
public Page<Report> findByCompanyAndStatus(UUID companyId, String status, int page) {
    return reportRepository.findByCompanyIdAndStatus(companyId, status, 
                                                     PageRequest.of(page, 20));
}
```

#### 3. Statistics Caching
```java
@Cacheable(value = "statistics", key = "'dashboard-' + #companyId")
public DashboardStats getDashboardStats(UUID companyId) {
    // Expensive aggregation queries
    return calculateStats(companyId);
}
```

### Cache Warming

```java
@Component
public class CacheWarmer {
    
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmCache() {
        // Preload frequently accessed data
        companyService.findAll().forEach(company -> {
            userService.findByCompanyId(company.getId());
            assetService.findByCompanyId(company.getId());
        });
    }
}
```

### Cache Monitoring

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "cache,metrics"
  metrics:
    cache:
      instrument: true
```

Monitor cache metrics:
- Cache hit ratio: `cache.gets{result=hit} / cache.gets`
- Cache miss ratio: `cache.gets{result=miss} / cache.gets`
- Cache eviction count: `cache.evictions`
- Cache put time: `cache.puts`

## Database Optimization

### Connection Pooling (HikariCP)

```yaml
spring:
  datasource:
    hikari:
      pool-name: CAFM-HikariPool
      maximum-pool-size: 50
      minimum-idle: 10
      idle-timeout: 300000
      connection-timeout: 20000
      max-lifetime: 1200000
      leak-detection-threshold: 60000
      connection-test-query: SELECT 1
      validation-timeout: 5000
```

### Index Strategy

#### Performance Indexes (V111__Performance_Indexes.sql)
```sql
-- User queries
CREATE INDEX idx_users_email_lower ON users(LOWER(email)) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_company_active ON users(company_id, is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_type_status ON users(user_type, status) WHERE deleted_at IS NULL;

-- Report queries
CREATE INDEX idx_reports_company_status ON reports(company_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_reports_school_priority ON reports(school_id, priority) WHERE deleted_at IS NULL;
CREATE INDEX idx_reports_created_date ON reports(created_at DESC) WHERE deleted_at IS NULL;

-- Work order queries
CREATE INDEX idx_work_orders_assigned ON work_orders(assigned_to) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_orders_status_priority ON work_orders(status, priority) WHERE deleted_at IS NULL;
CREATE INDEX idx_work_orders_scheduled ON work_orders(scheduled_start, scheduled_end) WHERE deleted_at IS NULL;

-- Asset queries
CREATE INDEX idx_assets_school_category ON assets(school_id, category_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_assets_serial ON assets(serial_number) WHERE deleted_at IS NULL;

-- Full-text search
CREATE INDEX idx_reports_search ON reports USING gin(to_tsvector('english', title || ' ' || description));
CREATE INDEX idx_assets_search ON assets USING gin(to_tsvector('english', name || ' ' || COALESCE(description, '')));
```

### Query Optimization

#### 1. Use Projections for Read-Only Data
```java
public interface UserProjection {
    UUID getId();
    String getEmail();
    String getFirstName();
    String getLastName();
}

@Query("SELECT u.id as id, u.email as email, u.firstName as firstName, u.lastName as lastName " +
       "FROM User u WHERE u.companyId = :companyId")
List<UserProjection> findUsersProjection(@Param("companyId") UUID companyId);
```

#### 2. Batch Fetching
```java
@Entity
@BatchSize(size = 20)
public class Report {
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    private User createdBy;
    
    @OneToMany(mappedBy = "report")
    @BatchSize(size = 50)
    private List<ReportImage> images;
}
```

#### 3. Native Queries for Complex Operations
```java
@Query(value = """
    WITH report_stats AS (
        SELECT 
            DATE_TRUNC('day', created_at) as date,
            COUNT(*) as total,
            COUNT(*) FILTER (WHERE status = 'OPEN') as open,
            COUNT(*) FILTER (WHERE status = 'CLOSED') as closed
        FROM reports
        WHERE company_id = :companyId
            AND created_at >= :startDate
            AND deleted_at IS NULL
        GROUP BY DATE_TRUNC('day', created_at)
    )
    SELECT * FROM report_stats
    ORDER BY date DESC
    """, nativeQuery = true)
List<ReportStatistics> getDailyStatistics(@Param("companyId") UUID companyId, 
                                          @Param("startDate") LocalDateTime startDate);
```

#### 4. Avoiding N+1 Queries
```java
@EntityGraph(attributePaths = {"company", "createdBy", "assignedTo"})
@Query("SELECT w FROM WorkOrder w WHERE w.status = :status")
List<WorkOrder> findByStatusWithRelations(@Param("status") String status);
```

## Application-Level Optimization

### Virtual Threads (Java 23)

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
    
    @Bean(name = "ioTaskExecutor")
    public Executor ioTaskExecutor() {
        return new ThreadPoolTaskExecutor() {{
            setCorePoolSize(10);
            setMaxPoolSize(50);
            setQueueCapacity(100);
            setThreadNamePrefix("io-task-");
            setVirtualThreads(true); // Java 23 feature
            initialize();
        }};
    }
}
```

### Async Processing

```java
@Service
public class NotificationService {
    
    @Async("virtualThreadExecutor")
    public CompletableFuture<Void> sendNotification(Notification notification) {
        // Long-running I/O operation
        return CompletableFuture.completedFuture(null);
    }
    
    @Async("ioTaskExecutor")
    public void processReportImages(List<MultipartFile> images) {
        images.parallelStream()
            .forEach(this::optimizeAndStore);
    }
}
```

### Response Compression

```yaml
server:
  compression:
    enabled: true
    mime-types:
      - application/json
      - application/xml
      - text/html
      - text/xml
      - text/plain
      - application/javascript
      - text/css
    min-response-size: 1024
```

### Pagination and Limiting

```java
@GetMapping("/reports")
public Page<Report> getReports(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "100") int maxSize) {
    
    // Enforce maximum page size
    size = Math.min(size, maxSize);
    
    PageRequest pageRequest = PageRequest.of(page, size, 
                                            Sort.by("createdAt").descending());
    return reportService.findAll(pageRequest);
}
```

## API Performance

### Request/Response Optimization

#### 1. Field Filtering
```java
@GetMapping("/users/{id}")
public UserResponse getUser(@PathVariable UUID id,
                           @RequestParam(required = false) String fields) {
    User user = userService.findById(id);
    
    if (fields != null) {
        return userMapper.toPartialResponse(user, fields.split(","));
    }
    return userMapper.toFullResponse(user);
}
```

#### 2. Conditional Requests (ETags)
```java
@GetMapping("/assets/{id}")
public ResponseEntity<Asset> getAsset(@PathVariable UUID id,
                                     @RequestHeader(value = "If-None-Match", required = false) String etag) {
    Asset asset = assetService.findById(id);
    String currentEtag = generateEtag(asset);
    
    if (currentEtag.equals(etag)) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
    }
    
    return ResponseEntity.ok()
        .eTag(currentEtag)
        .body(asset);
}
```

#### 3. Bulk Operations
```java
@PostMapping("/reports/bulk")
@Transactional
public List<Report> createBulkReports(@RequestBody @Valid List<ReportCreateRequest> requests) {
    // Process in batches to avoid memory issues
    return Lists.partition(requests, 100).stream()
        .flatMap(batch -> reportService.createBatch(batch).stream())
        .collect(Collectors.toList());
}
```

### Rate Limiting Configuration

```java
@Configuration
public class RateLimitingConfig {
    
    @Bean
    public Supplier<BucketConfiguration> standardBucketSupplier() {
        Bandwidth limit = Bandwidth.classic(
            60,  // capacity
            Refill.intervally(60, Duration.ofMinutes(1))
        );
        return () -> BucketConfiguration.builder()
            .addLimit(limit)
            .build();
    }
    
    @Bean
    public Supplier<BucketConfiguration> premiumBucketSupplier() {
        Bandwidth limit = Bandwidth.classic(
            300,  // capacity
            Refill.intervally(300, Duration.ofMinutes(1))
        );
        return () -> BucketConfiguration.builder()
            .addLimit(limit)
            .build();
    }
}
```

## File Storage Optimization

### Image Optimization

```java
@Service
public class ImageOptimizationService {
    
    public CompletableFuture<String> optimizeAndUpload(MultipartFile file) {
        return CompletableFuture.supplyAsync(() -> {
            BufferedImage image = ImageIO.read(file.getInputStream());
            
            // Resize if too large
            if (image.getWidth() > 1920 || image.getHeight() > 1080) {
                image = resizeImage(image, 1920, 1080);
            }
            
            // Compress
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            
            // Upload to CDN
            return cdnService.upload(baos.toByteArray());
        });
    }
    
    private BufferedImage resizeImage(BufferedImage original, int maxWidth, int maxHeight) {
        // Maintain aspect ratio
        double scale = Math.min(
            (double) maxWidth / original.getWidth(),
            (double) maxHeight / original.getHeight()
        );
        
        int width = (int) (original.getWidth() * scale);
        int height = (int) (original.getHeight() * scale);
        
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, width, height, null);
        g2d.dispose();
        
        return resized;
    }
}
```

### CDN Integration

```yaml
cdn:
  enabled: true
  provider: cloudflare
  base-url: https://cdn.cafm.sa
  cache-control:
    images: max-age=31536000, immutable
    documents: max-age=86400
    reports: max-age=3600
```

## JVM Tuning

### Memory Configuration

```bash
# Heap Configuration
-Xms2g                          # Initial heap size
-Xmx4g                          # Maximum heap size
-XX:MaxMetaspaceSize=512m       # Maximum metaspace

# Garbage Collection (G1GC)
-XX:+UseG1GC                    # Use G1 garbage collector
-XX:MaxGCPauseMillis=200        # Target pause time
-XX:G1HeapRegionSize=16m        # Region size
-XX:InitiatingHeapOccupancyPercent=45  # Start GC at 45% heap usage

# GC Logging
-Xlog:gc*:file=/var/log/cafm/gc.log:time,uptime,level,tags:filecount=10,filesize=10M

# Performance
-XX:+AlwaysPreTouch             # Pre-touch memory pages
-XX:+UseStringDeduplication     # Deduplicate strings
-XX:+ParallelRefProcEnabled     # Parallel reference processing

# Diagnostics
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/cafm/
-XX:ErrorFile=/var/log/cafm/hs_err_pid%p.log
```

### Virtual Threads Tuning (Java 23)

```bash
# Virtual Thread Configuration
-Djdk.virtualThreadScheduler.parallelism=10
-Djdk.virtualThreadScheduler.maxPoolSize=256
-Djdk.virtualThreadScheduler.minRunnable=1
```

## Monitoring & Metrics

### Key Performance Indicators (KPIs)

| Metric | Target | Alert Threshold |
|--------|--------|-----------------|
| API Response Time (p95) | < 200ms | > 500ms |
| API Response Time (p99) | < 500ms | > 1000ms |
| Database Query Time (p95) | < 50ms | > 100ms |
| Cache Hit Ratio | > 80% | < 60% |
| Error Rate | < 0.1% | > 1% |
| CPU Usage | < 70% | > 85% |
| Memory Usage | < 80% | > 90% |
| Database Connections | < 80% of pool | > 90% of pool |

### Prometheus Metrics

```java
@Component
public class PerformanceMetrics {
    
    private final MeterRegistry registry;
    
    @PostConstruct
    public void init() {
        // Custom business metrics
        Gauge.builder("cache.hit.ratio", cacheManager, this::calculateHitRatio)
            .description("Cache hit ratio")
            .register(registry);
            
        Gauge.builder("db.pool.usage", dataSource, this::getPoolUsage)
            .description("Database connection pool usage")
            .register(registry);
    }
    
    @EventListener
    public void handleRequestProcessed(RequestProcessedEvent event) {
        registry.timer("api.request.duration")
            .record(event.getDuration(), TimeUnit.MILLISECONDS);
    }
}
```

### Performance Testing

#### Load Testing with JMeter
```xml
<ThreadGroup>
    <stringProp name="ThreadGroup.num_threads">100</stringProp>
    <stringProp name="ThreadGroup.ramp_time">30</stringProp>
    <stringProp name="ThreadGroup.duration">300</stringProp>
    
    <HTTPSamplerProxy>
        <stringProp name="HTTPSampler.path">/api/v1/reports</stringProp>
        <stringProp name="HTTPSampler.method">GET</stringProp>
        <HeaderManager>
            <collectionProp name="HeaderManager.headers">
                <elementProp name="Authorization">
                    <stringProp name="Header.value">Bearer ${token}</stringProp>
                </elementProp>
            </collectionProp>
        </HeaderManager>
    </HTTPSamplerProxy>
</ThreadGroup>
```

#### Stress Testing Script
```bash
#!/bin/bash
# stress-test.sh

# Gradual load increase
for users in 10 50 100 200 500 1000; do
    echo "Testing with $users concurrent users..."
    
    ab -n 10000 -c $users \
       -H "Authorization: Bearer $TOKEN" \
       https://api.cafm.sa/api/v1/reports
       
    sleep 30
done
```

## Optimization Checklist

### Database
- [ ] Indexes on frequently queried columns
- [ ] Compound indexes for multi-column queries
- [ ] Partial indexes for filtered queries
- [ ] VACUUM and ANALYZE regularly
- [ ] Connection pool properly sized
- [ ] Query execution plans reviewed
- [ ] Slow query log monitored
- [ ] Database statistics updated

### Caching
- [ ] Redis configured with appropriate memory limit
- [ ] Cache TTLs properly set
- [ ] Cache warming implemented
- [ ] Cache invalidation strategy defined
- [ ] Cache metrics monitored
- [ ] Cache key strategy documented

### Application
- [ ] Virtual threads enabled for I/O operations
- [ ] Async processing for long-running tasks
- [ ] Response compression enabled
- [ ] Pagination enforced
- [ ] N+1 queries eliminated
- [ ] Lazy loading properly configured
- [ ] Connection timeouts set
- [ ] Circuit breakers implemented

### Infrastructure
- [ ] CDN configured for static assets
- [ ] Load balancer health checks configured
- [ ] Auto-scaling rules defined
- [ ] Resource limits set
- [ ] Monitoring alerts configured
- [ ] Log aggregation setup
- [ ] Backup strategy implemented

## Performance Anti-Patterns to Avoid

1. **Fetching entire entities when only IDs needed**
2. **Using findAll() without pagination**
3. **Synchronous external API calls**
4. **Caching mutable objects**
5. **Excessive logging in production**
6. **Large transactions**
7. **Unbounded queues**
8. **Missing database indexes**
9. **Inefficient cache keys**
10. **Blocking I/O in request threads**

## Continuous Performance Improvement

### Performance Review Process

1. **Weekly Metrics Review**
   - Analyze performance dashboards
   - Identify degradation trends
   - Review slow query logs

2. **Monthly Performance Testing**
   - Run load tests
   - Compare with baseline
   - Update performance targets

3. **Quarterly Optimization Sprint**
   - Address identified bottlenecks
   - Implement new optimizations
   - Update documentation

### Tools for Performance Analysis

- **APM**: New Relic, Datadog, AppDynamics
- **Profiling**: JProfiler, YourKit, async-profiler
- **Load Testing**: JMeter, Gatling, K6
- **Database**: pgAdmin, DataGrip, explain.depesz.com
- **Monitoring**: Prometheus + Grafana, ELK Stack

---

For performance-related questions, contact the Performance Team at performance@cafm.sa