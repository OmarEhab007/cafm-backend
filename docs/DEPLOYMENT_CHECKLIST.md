# CAFM Backend - Production Deployment Checklist

## Pre-Deployment Checklist

### Environment Setup
- [ ] **Production server provisioned** with adequate resources (CPU: 4+ cores, RAM: 8+ GB, Disk: 100+ GB)
- [ ] **Java 23** installed and configured
- [ ] **Docker and Docker Compose** installed (if using containerized deployment)
- [ ] **PostgreSQL 15+** database server configured and accessible
- [ ] **Redis 7+** cache server configured and accessible
- [ ] **MinIO** or cloud storage service configured

### Security Configuration
- [ ] **JWT secret** generated and configured (minimum 256-bit)
- [ ] **Database credentials** secured (strong passwords, limited access)
- [ ] **Redis authentication** enabled and configured
- [ ] **MinIO access keys** generated and secured
- [ ] **SSL/TLS certificates** obtained and configured
- [ ] **Firewall rules** configured (only necessary ports open)
- [ ] **Network security groups** configured

### Application Configuration
- [ ] **Environment variables** set for production:
  ```bash
  SPRING_PROFILES_ACTIVE=prod
  DB_PASSWORD=<secure_password>
  JWT_SECRET=<256_bit_secret>
  REDIS_PASSWORD=<redis_password>
  MINIO_ACCESS_KEY=<minio_access_key>
  MINIO_SECRET_KEY=<minio_secret_key>
  ```
- [ ] **CORS origins** configured for production domains
- [ ] **Email SMTP** settings configured
- [ ] **Log directories** created with proper permissions
- [ ] **File upload limits** configured appropriately

### Database Setup
- [ ] **Database created** with proper encoding (UTF-8)
- [ ] **Database user** created with minimum required permissions
- [ ] **Flyway migrations** executed successfully
- [ ] **Connection pooling** configured (HikariCP settings)
- [ ] **Database backup** strategy implemented
- [ ] **Performance indexes** created

### Monitoring Setup
- [ ] **Monitoring stack** deployed (Prometheus, Grafana, Loki, Jaeger)
- [ ] **Grafana dashboards** imported and configured
- [ ] **Alert rules** configured and tested
- [ ] **Notification channels** configured (email, Slack, etc.)
- [ ] **Log aggregation** configured (Loki + Promtail)
- [ ] **Health check endpoints** accessible

## Deployment Process

### 1. Application Deployment
- [ ] **Stop existing application** (if updating)
- [ ] **Backup current version** (binaries and configuration)
- [ ] **Deploy new application binary**
- [ ] **Update configuration files**
- [ ] **Start application with production profile**
- [ ] **Verify application startup** (check logs for errors)

### 2. Health Verification
- [ ] **Application health check**: `curl -f http://localhost:8080/actuator/health`
- [ ] **Database connectivity**: Check `/actuator/health/database`
- [ ] **Redis connectivity**: Check `/actuator/health/redis`
- [ ] **MinIO connectivity**: Check `/actuator/health/minio`
- [ ] **Readiness probe**: Check `/actuator/health/readiness`
- [ ] **Liveness probe**: Check `/actuator/health/liveness`

### 3. Functionality Testing
- [ ] **Authentication endpoints** working
- [ ] **Key API endpoints** responding correctly
- [ ] **Database operations** functioning (CRUD operations)
- [ ] **File upload/download** working
- [ ] **Email notifications** sending
- [ ] **Multi-tenant isolation** enforced

### 4. Performance Verification
- [ ] **Response times** within acceptable limits (< 2s for 95th percentile)
- [ ] **Memory usage** stable and within limits
- [ ] **CPU usage** normal for expected load
- [ ] **Database connection pool** properly sized
- [ ] **JVM metrics** being collected

### 5. Security Verification
- [ ] **HTTPS** enforced (HTTP redirects to HTTPS)
- [ ] **Authentication** required for protected endpoints
- [ ] **Rate limiting** active and configured
- [ ] **CORS** properly configured
- [ ] **SQL injection** protections active
- [ ] **XSS protection** headers present

## Post-Deployment Verification

### Monitoring Setup Verification
- [ ] **Prometheus** collecting metrics from application
- [ ] **Grafana dashboards** displaying real-time data
- [ ] **Loki** receiving application logs
- [ ] **Jaeger** collecting distributed traces
- [ ] **AlertManager** configured and responsive
- [ ] **Notification channels** tested with test alerts

### Load Testing (if applicable)
- [ ] **Baseline load test** executed successfully
- [ ] **Peak load scenarios** tested
- [ ] **Database performance** under load verified
- [ ] **Memory usage** stable under load
- [ ] **Response time degradation** within acceptable limits

### Documentation Updates
- [ ] **Deployment documentation** updated
- [ ] **Configuration changes** documented
- [ ] **Runbook updates** completed
- [ ] **Team notifications** sent about deployment
- [ ] **Change management** tickets updated

## Rollback Plan

### Preparation
- [ ] **Previous version backup** verified and accessible
- [ ] **Database backup** created before deployment
- [ ] **Configuration backup** available
- [ ] **Rollback procedure** documented and tested

### Rollback Triggers
Roll back immediately if:
- [ ] Application fails to start after 5 minutes
- [ ] Health checks fail consistently for 2 minutes
- [ ] Critical functionality is broken
- [ ] Security vulnerabilities are discovered
- [ ] Performance degradation > 50%
- [ ] Data corruption is detected

### Rollback Steps
1. [ ] **Stop current application**
2. [ ] **Restore previous application version**
3. [ ] **Restore previous configuration**
4. [ ] **Execute database rollback** (if schema changes were made)
5. [ ] **Restart application**
6. [ ] **Verify functionality**
7. [ ] **Notify stakeholders**

## Monitoring and Alerting Checklist

### Critical Alerts Configuration
- [ ] **Application down** alert (immediate notification)
- [ ] **Database connectivity** alert (30 second threshold)
- [ ] **High error rate** alert (> 10% for 3 minutes)
- [ ] **Memory exhaustion** alert (> 95% for 1 minute)
- [ ] **Security violations** alert (immediate notification)

### Dashboard Setup
- [ ] **System overview** dashboard configured
- [ ] **Application metrics** dashboard configured
- [ ] **Database performance** dashboard configured
- [ ] **Security monitoring** dashboard configured
- [ ] **Business metrics** dashboard configured

### Log Monitoring
- [ ] **Error log monitoring** configured
- [ ] **Security event monitoring** configured
- [ ] **Performance log monitoring** configured
- [ ] **Log retention policies** configured (30 days default)
- [ ] **Log rotation** configured

## Security Hardening

### Application Security
- [ ] **Default passwords** changed
- [ ] **Unnecessary endpoints** disabled
- [ ] **Security headers** configured
- [ ] **Input validation** enabled
- [ ] **Output encoding** implemented

### System Security
- [ ] **OS updates** applied
- [ ] **Security patches** installed
- [ ] **Unused services** disabled
- [ ] **File permissions** properly set
- [ ] **Log access** restricted

### Network Security
- [ ] **Firewall configured** (only required ports open)
- [ ] **VPN/bastion host** for admin access
- [ ] **Network segmentation** implemented
- [ ] **DDoS protection** enabled (if applicable)

## Performance Optimization

### JVM Tuning
- [ ] **Heap size** optimized for available memory
- [ ] **Garbage collector** configured (G1GC recommended)
- [ ] **JVM monitoring** enabled
- [ ] **Memory leak detection** configured

### Database Optimization
- [ ] **Connection pool** size optimized
- [ ] **Query performance** analyzed
- [ ] **Indexes** created for frequent queries
- [ ] **Database statistics** updated

### Caching Strategy
- [ ] **Redis cache** configured and tested
- [ ] **Cache hit rates** monitored
- [ ] **Cache eviction policies** configured
- [ ] **Cache warming** strategy implemented

## Team Handover

### Knowledge Transfer
- [ ] **Operations team** briefed on new deployment
- [ ] **Support team** updated on new features/changes
- [ ] **Monitoring team** informed of new metrics/alerts
- [ ] **Emergency contacts** updated

### Documentation Handover
- [ ] **Runbooks** updated and accessible
- [ ] **Troubleshooting guides** current
- [ ] **Configuration documentation** complete
- [ ] **Emergency procedures** documented

## Sign-off

### Technical Approval
- [ ] **Lead Developer** sign-off: _______________
- [ ] **DevOps Engineer** sign-off: _______________
- [ ] **Security Engineer** sign-off: _______________

### Business Approval
- [ ] **Product Owner** sign-off: _______________
- [ ] **Operations Manager** sign-off: _______________

### Final Verification
- [ ] **All checklist items** completed
- [ ] **Deployment successful** and verified
- [ ] **Monitoring active** and alerts configured
- [ ] **Team notified** of successful deployment
- [ ] **Documentation updated** and accessible

**Deployment Date**: _______________
**Deployed by**: _______________
**Version**: _______________