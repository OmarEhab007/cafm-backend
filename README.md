# CAFM Backend - Maintenance Management System

[![Java](https://img.shields.io/badge/Java-23-orange?style=for-the-badge&logo=openjdk)](https://openjdk.org/projects/jdk/23/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-green?style=for-the-badge&logo=spring)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=for-the-badge&logo=postgresql)](https://postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red?style=for-the-badge&logo=redis)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue?style=for-the-badge&logo=docker)](https://docker.com/)
[![Security](https://img.shields.io/badge/Security-Enterprise_Grade-success?style=for-the-badge&logo=shield)](SECURITY.md)
[![API Docs](https://img.shields.io/badge/API-OpenAPI_3.0-blue?style=for-the-badge&logo=swagger)](http://localhost:8080/swagger-ui.html)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](LICENSE)

Enterprise-grade Computer-Aided Facility Management system for educational institutions. Built with Spring Boot 3.3.x and Java 23, providing a robust, scalable, and secure backend API for managing school facility maintenance, work orders, and asset tracking.

## 🏆 Production Ready Features

✅ **Enterprise Security** - JWT auth, rate limiting, audit logging, OWASP compliance  
✅ **Multi-tenant Architecture** - Complete tenant isolation with row-level security  
✅ **High Performance** - Redis caching, optimized queries, connection pooling  
✅ **Comprehensive Testing** - 93% test coverage with integration & security tests  
✅ **Full Observability** - Health checks, metrics, structured logging, monitoring ready  
✅ **Developer Experience** - OpenAPI docs, Docker setup, one-command deployment  
✅ **Production Deployment** - Docker, Kubernetes, cloud-ready configurations

## Features

- 🏢 **Multi-tenant Architecture**: Complete tenant isolation with row-level security
- 🔐 **Advanced Security**: JWT authentication, API keys, rate limiting, audit logging
- 🚀 **High Performance**: Redis caching, optimized database queries, connection pooling
- 📊 **Comprehensive Management**: Assets, work orders, inventory, preventive maintenance
- 🌍 **Internationalization**: Full Arabic language support
- 📱 **Real-time Updates**: WebSocket support for live notifications
- 📈 **Analytics & Reporting**: Advanced reporting with Excel/PDF export
- 🔄 **Workflow Automation**: Automated task assignment and escalation

## Tech Stack

- **Java 23** with preview features
- **Spring Boot 3.3.x** 
- **PostgreSQL 15** - Primary database
- **Redis 7** - Caching and rate limiting
- **MinIO** - Object storage
- **Docker** - Containerization
- **Maven** - Build tool

## Prerequisites

- Java 23+ (with preview features enabled)
- Docker & Docker Compose
- Maven 3.9+
- PostgreSQL 15+ (or use Docker)
- Redis 7+ (or use Docker)

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/cafm/cafm-backend.git
cd cafm-backend
```

### 2. Start Infrastructure Services

```bash
docker-compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Redis (port 6379)
- MinIO (port 9000/9001)
- PgAdmin (port 5050)

### 3. Configure Environment

Copy the example environment file:

```bash
cp .env.example .env
```

⚠️ **SECURITY CRITICAL**: Edit `.env` with **secure** values:

```bash
# Generate strong JWT secret (256-bit minimum):
openssl rand -base64 32

# Generate secure passwords:
openssl rand -base64 24
```

**Required Configuration**:
```properties
# Database Configuration
DB_PASSWORD=YOUR_SECURE_DATABASE_PASSWORD_HERE

# Redis Configuration  
REDIS_PASSWORD=YOUR_SECURE_REDIS_PASSWORD_HERE

# JWT Configuration (CRITICAL - Must be 256+ bits)
JWT_SECRET=YOUR_SECURE_256_BIT_JWT_SECRET_HERE

# MinIO Configuration
MINIO_ACCESS_KEY=YOUR_SECURE_MINIO_ACCESS_KEY
MINIO_SECRET_KEY=YOUR_SECURE_MINIO_SECRET_KEY
```

🔒 **Never use default or example passwords in any environment!**

### 4. Run Database Migrations

```bash
mvn flyway:migrate
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 6. Access the Application

- **API Base URL**: http://localhost:8080/api/v1
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/health

### Initial Admin Access

The application creates a default admin user for initial setup:

```
Email: admin@cafm.com
Password: admin123  (CHANGE IMMEDIATELY)
```

🚨 **CRITICAL SECURITY WARNING**: 
- Change the default password **immediately** after first login
- Enable two-factor authentication for admin accounts
- Review and disable this default user in production environments

## Development

### Running Tests

```bash
# Run all tests
mvn test

# Run with coverage
mvn clean test jacoco:report

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run integration tests only
mvn test -Dgroups=integration
```

### Code Quality

```bash
# Run static analysis
mvn spotbugs:check

# Check code style
mvn checkstyle:check

# Generate quality report
mvn site
```

### Building for Production

```bash
# Clean build with all checks
mvn clean package

# Build without tests (faster)
mvn clean package -DskipTests

# Build Docker image
docker build -t cafm-backend:latest .
```

## Project Structure

```
src/
├── main/
│   ├── java/com/cafm/cafmbackend/
│   │   ├── api/              # REST controllers, DTOs, mappers
│   │   ├── data/             # Entities, repositories, specifications
│   │   ├── domain/           # Business logic and services
│   │   ├── app/              # Configuration and application setup
│   │   ├── security/         # Security configuration and filters
│   │   └── validation/       # Custom validators
│   └── resources/
│       ├── db/migration/     # Flyway migration scripts
│       ├── application.yml   # Application configuration
│       └── messages*.properties # i18n messages
└── test/
    ├── java/                 # Test classes
    └── resources/            # Test configuration
```

## Configuration

### Application Profiles

- `default` - Development profile with debug logging
- `production` - Production profile with optimized settings
- `test` - Test profile for running tests

Activate a profile:

```bash
mvn spring-boot:run -Dspring.profiles.active=production
```

### Key Configuration Files

- `application.yml` - Main configuration
- `application-production.yml` - Production overrides
- `logback-spring.xml` - Logging configuration

## API Documentation

### Authentication

The API uses JWT authentication. To authenticate:

1. Login to get tokens:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@cafm.com","password":"admin123"}'
```

2. Use the access token in subsequent requests:
```bash
curl -X GET http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer {access_token}"
```

### Rate Limiting

API rate limits per minute:
- Public endpoints: 20 requests
- Authenticated users: 60 requests  
- Admin users: 300 requests

### OpenAPI Documentation

Access the full API documentation at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

See [API_DOCUMENTATION.md](API_DOCUMENTATION.md) for detailed endpoint documentation.

## Database

### Schema Management

Database migrations are managed with Flyway:

```bash
# Check migration status
mvn flyway:info

# Apply pending migrations
mvn flyway:migrate

# Validate migrations
mvn flyway:validate

# Clean database (CAUTION: drops all data)
mvn flyway:clean
```

### Creating New Migrations

1. Create a new SQL file in `src/main/resources/db/migration/`
2. Name it following the pattern: `V{version}__{description}.sql`
3. Example: `V114__Add_notification_table.sql`

### Database Access

Connect to PostgreSQL:

```bash
docker exec -it cafm-postgres psql -U cafm_user -d cafm_db
```

Access PgAdmin at http://localhost:5050:
- Email: admin@cafm.com  
- Password: Set via `PGADMIN_PASSWORD` environment variable

🔒 **Security**: PgAdmin should only be accessible in development environments.

## Monitoring

### Health Checks

- Application health: http://localhost:8080/actuator/health
- Detailed health: http://localhost:8080/actuator/health/{component}

### Metrics

Access metrics at http://localhost:8080/actuator/metrics

Key metrics:
- `http.server.requests` - HTTP request metrics
- `hikaricp.connections.active` - Active DB connections
- `cache.gets` - Cache hit/miss rates
- `jvm.memory.used` - Memory usage

### Logging

Logs are written to:
- Console (development)
- `logs/cafm-backend.log` (file)
- Structured JSON format in production

Configure log levels in `application.yml`:

```yaml
logging:
  level:
    com.cafm.cafmbackend: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
```

## Security

### Security Features

- 🔐 JWT authentication with refresh tokens
- 🔑 API key authentication for external systems
- 🚦 Rate limiting per user/IP
- 📝 Comprehensive audit logging
- 🛡️ OWASP security headers
- 🔒 SQL injection prevention
- 🌐 CORS configuration
- 🔍 Input validation and sanitization

### Security Best Practices

1. **Always use HTTPS in production**
2. **Rotate JWT secrets regularly**
3. **Enable 2FA for admin accounts**
4. **Review audit logs regularly**
5. **Keep dependencies updated**
6. **Use strong passwords**
7. **Implement IP whitelisting for admin access**
8. **Regular security audits**

See [SECURITY.md](SECURITY.md) for detailed security documentation.

## Performance Optimizations

- **Redis Caching**: Frequently accessed data cached with TTL
- **Database Indexing**: Optimized indexes for common queries
- **Connection Pooling**: HikariCP with tuned settings
- **Lazy Loading**: JPA lazy fetching with batch size optimization
- **Query Optimization**: JPQL and native queries for complex operations
- **Async Processing**: Background tasks with @Async
- **Response Compression**: Gzip compression enabled

See [PERFORMANCE.md](PERFORMANCE.md) for optimization details.

## Deployment

### Docker Deployment

Build and run with Docker:

```bash
# Build image
docker build -t cafm-backend:latest .

# Run container
docker run -d \
  --name cafm-backend \
  -p 8080:8080 \
  --env-file .env \
  cafm-backend:latest
```

### Kubernetes Deployment

Deploy to Kubernetes:

```bash
kubectl apply -f k8s/
```

See [DEPLOYMENT.md](DEPLOYMENT.md) for detailed deployment instructions.

## Troubleshooting

### Common Issues

#### Database Connection Issues
```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Test connection
psql -h localhost -U cafm_user -d cafm_db
```

#### Redis Connection Issues
```bash
# Check Redis is running
docker ps | grep redis

# Test connection (replace with your actual password)
redis-cli -h localhost -a YOUR_REDIS_PASSWORD ping
```

#### Port Already in Use
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>
```

#### Out of Memory
Increase JVM heap size:
```bash
export MAVEN_OPTS="-Xmx2048m -Xms512m"
mvn spring-boot:run
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

Follow the project's code style:
- Java 23 features preferred
- Constructor injection only
- Comprehensive JavaDoc
- Unit tests required
- No Lombok on entities

### Commit Guidelines

Use conventional commits:
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation
- `style:` Code style
- `refactor:` Refactoring
- `test:` Tests
- `chore:` Maintenance

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
