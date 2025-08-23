# CAFM Backend - Production Monitoring Guide

## Overview

This guide provides comprehensive instructions for monitoring the CAFM Backend in production environments. The monitoring setup includes metrics collection, log aggregation, distributed tracing, and alerting.

## Architecture

### Monitoring Stack Components

- **Prometheus**: Metrics collection and storage
- **Grafana**: Visualization and dashboards
- **Loki**: Log aggregation and querying
- **Jaeger**: Distributed tracing
- **AlertManager**: Alert routing and management
- **Various Exporters**: System and application metrics

### Application Monitoring

- **Spring Boot Actuator**: Health checks and metrics endpoints
- **Micrometer**: Application metrics with Prometheus integration
- **Custom Health Indicators**: Database, Redis, MinIO connectivity
- **Structured Logging**: JSON format with correlation IDs

## Quick Start

### 1. Start Monitoring Stack

```bash
# Start the complete monitoring infrastructure
docker-compose -f docker-compose.monitoring.yml up -d

# Verify all services are running
docker-compose -f docker-compose.monitoring.yml ps
```

### 2. Access Monitoring Interfaces

| Service | URL | Default Credentials |
|---------|-----|-------------------|
| Grafana | http://localhost:3000 | admin / admin123 |
| Prometheus | http://localhost:9090 | None |
| AlertManager | http://localhost:9093 | None |
| Jaeger | http://localhost:16686 | None |
| Loki | http://localhost:3100 | None |

### 3. Import Dashboards

1. Open Grafana at http://localhost:3000
2. Navigate to "+" → Import
3. Upload `/monitoring/dashboards/cafm-backend-dashboard.json`
4. Configure Prometheus data source if needed

## Monitoring Endpoints

### Application Health Checks

| Endpoint | Purpose | Expected Response |
|----------|---------|------------------|
| `/actuator/health` | Overall application health | `{"status":"UP"}` |
| `/actuator/health/readiness` | Kubernetes readiness probe | `{"status":"UP"}` |
| `/actuator/health/liveness` | Kubernetes liveness probe | `{"status":"UP"}` |
| `/actuator/health/database` | Database connectivity | `{"status":"UP"}` |
| `/actuator/health/redis` | Redis connectivity | `{"status":"UP"}` |
| `/actuator/health/minio` | MinIO storage connectivity | `{"status":"UP"}` |

### Metrics Endpoints

| Endpoint | Purpose | Format |
|----------|---------|--------|
| `/actuator/prometheus` | Prometheus metrics | Text format |
| `/actuator/metrics` | Available metrics list | JSON |
| `/actuator/metrics/{name}` | Specific metric details | JSON |

### Information Endpoints

| Endpoint | Purpose | Access Level |
|----------|---------|-------------|
| `/actuator/info` | Application information | Public |
| `/actuator/env` | Environment properties | Authorized |
| `/actuator/configprops` | Configuration properties | Authorized |

## Key Metrics to Monitor

### Application Metrics

#### Business Logic Metrics
- `cafm_workorders_created_total` - Work orders created
- `cafm_workorders_completed_total` - Work orders completed  
- `cafm_reports_submitted_total` - Reports submitted
- `cafm_reports_approved_total` - Reports approved
- `cafm_auth_login_success_total` - Successful logins
- `cafm_auth_login_failed_total` - Failed login attempts

#### Performance Metrics
- `http_server_requests_duration_seconds` - API response times
- `cafm_db_query_duration_seconds` - Database query performance
- `cafm_files_upload_duration_seconds` - File upload performance

#### Security Metrics
- `cafm_security_unauthorized_access_total` - Unauthorized access attempts
- `cafm_tenant_isolation_violation_total` - Tenant isolation violations
- `cafm_security_rate_limit_exceeded_total` - Rate limit violations

### Infrastructure Metrics

#### JVM Metrics
- `jvm_memory_used_bytes` - JVM memory usage
- `jvm_memory_max_bytes` - JVM memory limits
- `jvm_threads_live_threads` - Active thread count
- `jvm_gc_pause_seconds` - Garbage collection times

#### System Metrics
- `system_cpu_usage` - CPU utilization
- `system_memory_usage` - Memory utilization
- `system_disk_usage` - Disk space usage

#### Database Metrics
- `hikaricp_connections_active` - Active database connections
- `hikaricp_connections_idle` - Idle database connections
- `postgresql_up` - Database availability

## Alert Thresholds

### Critical Alerts (Immediate Response Required)

| Alert | Threshold | Duration | Action Required |
|-------|-----------|----------|----------------|
| Application Down | `up == 0` | 1 minute | Immediate investigation |
| Database Down | `cafm_health_database_status == 0` | 30 seconds | Check database connectivity |
| Critical Memory Usage | Memory > 95% | 1 minute | Scale up or restart |
| High Error Rate | Error rate > 10% | 3 minutes | Check application logs |
| Security Violations | Any occurrence | Immediate | Security team alert |

### Warning Alerts (Monitor Closely)

| Alert | Threshold | Duration | Action Required |
|-------|-----------|----------|----------------|
| High Response Time | 95th percentile > 2s | 5 minutes | Performance investigation |
| High Memory Usage | Memory > 85% | 5 minutes | Monitor for escalation |
| High CPU Usage | CPU > 80% | 5 minutes | Check resource utilization |
| Database Slow Queries | Query time > 1s | 5 minutes | Database optimization |

## Log Analysis

### Log Levels and Locations

| Log Type | Location | Purpose |
|----------|----------|---------|
| Application | `/var/log/cafm-backend/application.log` | General application logs |
| Error | `/var/log/cafm-backend/application-error.log` | Error-specific logs |
| Security | `/var/log/cafm-backend/application-security.log` | Security events |
| Performance | `/var/log/cafm-backend/application-performance.log` | Performance metrics |

### Key Log Patterns to Monitor

#### Error Patterns
```bash
# Database connection errors
grep "Connection.*refused\|Connection.*timeout" /var/log/cafm-backend/application-error.log

# Authentication failures
grep "Authentication failed\|Invalid credentials" /var/log/cafm-backend/application-security.log

# Tenant isolation violations
grep "Tenant.*violation\|Cross-tenant" /var/log/cafm-backend/application-security.log
```

#### Performance Patterns
```bash
# Slow queries
grep "Query.*took.*ms" /var/log/cafm-backend/application-performance.log | awk '$NF > 1000'

# Memory pressure
grep "OutOfMemoryError\|Memory.*exhausted" /var/log/cafm-backend/application-error.log
```

### Correlation IDs

All logs include correlation IDs for request tracing:
```
[correlationId:abc123] [tenantId:company1] [userId:user456]
```

Use correlation IDs to trace requests across services:
```bash
grep "correlationId:abc123" /var/log/cafm-backend/*.log
```

## Troubleshooting Common Issues

### High Memory Usage

1. **Check JVM heap usage**:
   ```bash
   curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq .
   ```

2. **Analyze memory allocation**:
   - Review Grafana JVM memory dashboard
   - Check for memory leaks in heap dumps
   - Monitor garbage collection frequency

3. **Immediate actions**:
   - Increase JVM heap size: `-Xmx2g -Xms1g`
   - Enable G1 garbage collector: `-XX:+UseG1GC`
   - Generate heap dump: `jmap -dump:live,format=b,file=heap.hprof <pid>`

### High Database Connection Usage

1. **Check connection pool metrics**:
   ```bash
   curl -s http://localhost:8080/actuator/metrics/hikaricp.connections
   ```

2. **Investigate active connections**:
   ```sql
   SELECT count(*) FROM pg_stat_activity WHERE state = 'active';
   SELECT query, state, query_start FROM pg_stat_activity WHERE state != 'idle';
   ```

3. **Immediate actions**:
   - Increase connection pool size
   - Check for connection leaks
   - Optimize slow queries

### Application Not Responding

1. **Check application status**:
   ```bash
   curl -f http://localhost:8080/actuator/health || echo "Application down"
   ```

2. **Check system resources**:
   ```bash
   # CPU usage
   top -p $(pgrep -f cafm-backend)
   
   # Memory usage
   ps -p $(pgrep -f cafm-backend) -o pid,ppid,cmd,%mem,%cpu
   
   # Disk space
   df -h /var/log /tmp
   ```

3. **Check application logs**:
   ```bash
   tail -f /var/log/cafm-backend/application-error.log
   ```

### Database Performance Issues

1. **Monitor database metrics**:
   ```bash
   # Check Prometheus metrics
   curl -s "http://localhost:9090/api/v1/query?query=postgresql_locks_count"
   ```

2. **Analyze slow queries**:
   ```sql
   SELECT query, mean_time, calls, total_time 
   FROM pg_stat_statements 
   ORDER BY mean_time DESC LIMIT 10;
   ```

3. **Check connection pool**:
   ```bash
   curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active
   ```

## Performance Optimization

### JVM Tuning

#### Recommended JVM Parameters
```bash
# Memory settings
-Xms2g -Xmx4g
-XX:NewRatio=3
-XX:MaxMetaspaceSize=512m

# Garbage Collection (G1)
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=16m

# Monitoring and Debugging
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/cafm-backend/
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
-Xloggc:/var/log/cafm-backend/gc.log
```

### Database Optimization

#### Connection Pool Settings
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
      leak-detection-threshold: 60000
```

#### Query Optimization
- Monitor slow queries via pg_stat_statements
- Add appropriate indexes for frequent queries
- Use connection pooling efficiently
- Implement query result caching where appropriate

### Application Optimization

#### Cache Configuration
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1 hour
      cache-null-values: false
```

#### Thread Pool Tuning
```yaml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
    accept-count: 100
    max-connections: 10000
```

## Security Monitoring

### Key Security Metrics to Watch

1. **Authentication Failures**
   - Monitor login failure rates
   - Watch for brute force patterns
   - Track failed API key usage

2. **Authorization Violations**
   - Unauthorized access attempts
   - Privilege escalation attempts
   - Cross-tenant data access

3. **Suspicious Activity**
   - Unusual API usage patterns
   - High-frequency requests from single IPs
   - Access to sensitive endpoints

### Security Log Analysis

```bash
# Failed authentication attempts
grep "Authentication failed" /var/log/cafm-backend/application-security.log | wc -l

# Suspicious IP activity
grep "SECURITY" /var/log/cafm-backend/application-security.log | \
  awk '{print $7}' | sort | uniq -c | sort -nr

# Tenant isolation violations
grep "tenant.*violation" /var/log/cafm-backend/application-security.log
```

## Capacity Planning

### Growth Monitoring

Track these metrics for capacity planning:

1. **User Growth**
   - Active user count trends
   - Peak concurrent users
   - API request volume growth

2. **Data Growth**
   - Database size trends
   - File storage usage
   - Log volume growth

3. **Resource Utilization**
   - CPU usage trends
   - Memory consumption patterns
   - Database connection usage

### Scaling Recommendations

| Metric | Threshold | Action |
|--------|-----------|--------|
| CPU Usage | > 70% sustained | Add CPU cores or scale horizontally |
| Memory Usage | > 80% sustained | Increase RAM or optimize memory usage |
| DB Connections | > 80% of pool | Increase connection pool or add read replicas |
| Disk Usage | > 85% | Add storage or implement log rotation |
| API Response Time | > 1s average | Scale application instances |

## Maintenance Tasks

### Daily Tasks

1. **Check system health**
   ```bash
   # Application health
   curl -f http://localhost:8080/actuator/health
   
   # Monitor key metrics
   curl -s http://localhost:9090/api/v1/query?query=up{job=\"cafm-backend\"}
   ```

2. **Review error logs**
   ```bash
   # Check for new errors
   grep ERROR /var/log/cafm-backend/application-error.log | tail -20
   
   # Check security events
   grep SECURITY /var/log/cafm-backend/application-security.log | tail -10
   ```

3. **Monitor resource usage**
   - Check CPU and memory trends in Grafana
   - Review database performance metrics
   - Verify backup completion

### Weekly Tasks

1. **Performance review**
   - Analyze response time trends
   - Review slow query reports
   - Check garbage collection performance

2. **Capacity planning**
   - Review growth trends
   - Assess resource utilization
   - Plan for scaling needs

3. **Security audit**
   - Review authentication logs
   - Check for security alerts
   - Verify access patterns

### Monthly Tasks

1. **Full system review**
   - Performance optimization opportunities
   - Cost optimization analysis
   - Disaster recovery testing

2. **Update monitoring**
   - Review and update alert thresholds
   - Add new metrics as needed
   - Update dashboards

## Integration with CI/CD

### Deployment Health Checks

Add these checks to your deployment pipeline:

```bash
#!/bin/bash
# Health check script for deployment validation

# Wait for application to start
sleep 30

# Check application health
health_check() {
    curl -f http://localhost:8080/actuator/health/readiness || exit 1
    curl -f http://localhost:8080/actuator/health/liveness || exit 1
}

# Check key metrics are being reported
metrics_check() {
    curl -s http://localhost:8080/actuator/prometheus | grep -q "jvm_memory_used_bytes" || exit 1
    curl -s http://localhost:8080/actuator/prometheus | grep -q "http_server_requests_total" || exit 1
}

echo "Running post-deployment health checks..."
health_check && echo "✓ Health checks passed"
metrics_check && echo "✓ Metrics checks passed"
echo "Deployment validation complete"
```

### Monitoring as Code

Include monitoring configuration in your infrastructure as code:

```yaml
# Example Kubernetes deployment with monitoring
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cafm-backend
spec:
  template:
    metadata:
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "8080"
    spec:
      containers:
      - name: cafm-backend
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
```

## Support and Escalation

### Contact Information

| Level | Contact | Response Time |
|-------|---------|--------------|
| L1 Support | support@company.com | 15 minutes |
| L2 Engineering | engineering@company.com | 30 minutes |
| L3 Architecture | architecture@company.com | 1 hour |
| Emergency | +1-XXX-XXX-XXXX | Immediate |

### Escalation Criteria

- **Critical**: Application down, data loss, security breach
- **High**: Performance degradation, partial functionality loss
- **Medium**: Non-critical feature issues, monitoring alerts
- **Low**: Questions, enhancement requests

Remember to include correlation IDs and relevant metrics in all support requests for faster resolution.