# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**CAFM Backend** - Spring Boot 3.3.x backend for School Maintenance Management System
- Multi-tenant architecture with company-based isolation
- RESTful API serving mobile and web applications
- Real-time maintenance tracking and work order management

## Technology Stack

- **Java 23** with preview features enabled
- **Spring Boot 3.3.3** with Spring Framework 6.x
- **PostgreSQL 15** as primary database
- **Redis 7** for caching
- **MinIO** for file storage
- **Maven** build tool
- **Docker Compose** for local development
- **After finish** test the app after finishing any component and make sure it's working fine

## Development Commands

### Quick Start
```bash
# Start infrastructure services
docker compose up -d

# Run application
mvn spring-boot:run

# Access application
# http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Testing
```bash
# Run all tests
mvn test

# Run tests with coverage
mvn clean test jacoco:report

# Run specific test class
mvn test -Dtest=ClassName

# Run specific test method
mvn test -Dtest=ClassName#methodName

# Integration tests only
mvn test -Dgroups=integration
```

### Database Management
```bash
# Run migrations
mvn flyway:migrate

# Check migration status
mvn flyway:info

# Validate migrations
mvn flyway:validate

# Clean database (CAUTION: removes all data)
mvn flyway:clean
```

### Build & Package
```bash
# Clean build
mvn clean compile

# Package JAR
mvn clean package

# Skip tests during build
mvn clean package -DskipTests

# Build Docker image
docker build -t cafm-backend:latest .
```

## Architecture & Key Patterns

### Package Structure
```
src/main/java/com/example/cafmbackend/
├── api/              # REST controllers, DTOs, mappers
│   ├── controllers/  # REST endpoints
│   ├── dtos/        # Request/Response DTOs (records)
│   └── mappers/     # MapStruct mappers
├── data/            # Data layer
│   ├── entities/    # JPA entities
│   ├── repositories/# Spring Data repositories
│   └── specifications/ # JPA Specifications
├── domain/          # Business logic
│   ├── services/    # Domain services
│   └── events/      # Domain events
├── app/             # Application configuration
│   ├── config/      # Spring configuration
│   └── security/    # Security configuration
└── validation/      # Custom validators
```

### Multi-Tenancy Architecture
- Company-based tenant isolation via `TenantContext`
- Automatic tenant filtering in repositories
- Security filters enforce tenant boundaries
- All entities extend `BaseEntity` with tenant tracking

### Entity Hierarchy
```
BaseEntity (id, createdAt, updatedAt, deletedAt, companyId)
├── User (abstract)
│   ├── Admin
│   ├── Supervisor
│   └── Technician
├── Asset
├── Report → WorkOrder → Task
└── InventoryItem → InventoryTransaction
```

### Security Configuration
- JWT authentication with refresh tokens
- Role-based access: ADMIN, SUPERVISOR, TECHNICIAN
- Method-level security with `@PreAuthorize`
- Tenant isolation enforced at repository level

## Database Schema

### Key Tables
- `users` - Polymorphic user table with role discrimination
- `companies` - Tenant organizations
- `assets` - Physical assets requiring maintenance
- `reports` - Maintenance issues reported
- `work_orders` - Assigned maintenance work
- `tasks` - Individual work items within orders
- `inventory_items` - Stock management
- `inventory_transactions` - Stock movement history

### Migration Strategy
- Flyway migrations in `src/main/resources/db/migration/`
- Naming: `V{version}__{description}.sql`
- Rollback scripts: `U{version}__{description}.sql`
- Never modify existing migrations

## API Patterns

### Standard Endpoints
```java
GET    /api/v1/{resource}          // List with pagination
GET    /api/v1/{resource}/{id}     // Get single
POST   /api/v1/{resource}          // Create
PUT    /api/v1/{resource}/{id}     // Full update
PATCH  /api/v1/{resource}/{id}     // Partial update
DELETE /api/v1/{resource}/{id}     // Soft delete
```

### Response Structure
```json
{
  "data": {},
  "status": "SUCCESS|ERROR",
  "message": "Optional message",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### Error Response
```json
{
  "timestamp": "2024-01-01T00:00:00Z",
  "path": "/api/v1/resource",
  "code": "RESOURCE_NOT_FOUND",
  "message": "User-friendly message",
  "details": []
}
```

## Testing Guidelines

### Test Structure
```
src/test/java/
├── unit/           # Pure unit tests
├── integration/    # @SpringBootTest tests
├── slice/          # @WebMvcTest, @DataJpaTest
└── fixtures/       # Test data builders
```

### Test Patterns
- Use `@DisplayName` for readable test names
- Arrange-Act-Assert pattern
- Testcontainers for database tests
- MockMvc for controller tests
- Test data builders for complex objects

## Key Services & Components

### File Storage Service
- MinIO for local development
- Cloudinary integration available
- Automatic image optimization
- Virus scanning before storage

### Notification Service
- FCM integration for mobile push
- Email notifications via SMTP
- In-app notifications stored in DB

### Reporting Service
- Export to Excel/PDF
- Scheduled report generation
- Custom report templates

### Audit Service
- Automatic audit logging
- Change tracking for entities
- Compliance reporting

## Environment Variables

### Required for Development
```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=cafm_db
DB_USERNAME=cafm_user
DB_PASSWORD=cafm_pass

REDIS_HOST=localhost
REDIS_PORT=6379

JWT_SECRET=your-secret-key
JWT_EXPIRATION=3600000

MINIO_URL=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
```

## Common Development Tasks

### Adding a New Entity
1. Create entity in `data/entities/` extending `BaseEntity`
2. Create repository in `data/repositories/`
3. Add Flyway migration script
4. Create DTOs in `api/dtos/`
5. Create MapStruct mapper in `api/mappers/`
6. Implement service in `domain/services/`
7. Create controller in `api/controllers/`
8. Write tests for all layers

### Adding a New API Endpoint
1. Define DTO records with validation
2. Create mapper interface with MapStruct
3. Implement service method with `@Transactional`
4. Add controller method with proper security
5. Document with OpenAPI annotations
6. Write integration and unit tests

### Implementing Business Logic
1. Place in domain service, not controller
2. Use specifications for complex queries
3. Emit domain events for cross-cutting concerns
4. Handle errors with custom exceptions
5. Add proper logging and metrics

## Performance Considerations

### Database Optimization
- Use projections for read-only queries
- Implement pagination for list endpoints
- Add indexes for frequently queried columns
- Use `@EntityGraph` to prevent N+1 queries
- Cache frequently accessed data with Redis

### API Performance
- Use virtual threads for I/O operations
- Implement response compression
- Add ETags for cache validation
- Use async processing for long tasks
- Rate limiting per tenant

## Security Best Practices

### Authentication & Authorization
- Never expose internal IDs in URLs
- Validate tenant context on every request
- Use method-level security annotations
- Implement rate limiting per user
- Log security events for audit

### Data Protection
- Encrypt sensitive data at rest
- Use HTTPS in production
- Sanitize all user inputs
- Implement CORS properly
- Never log passwords or tokens

## Debugging & Troubleshooting

### Logging
```properties
# Enable SQL logging
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Enable Spring Security debugging
logging.level.org.springframework.security=DEBUG

# Application-specific logging
logging.level.com.example.cafmbackend=DEBUG
```

### Common Issues
- **Tenant Context Missing**: Check `TenantFilter` is applied
- **Lazy Loading Exception**: Use `@Transactional` or eager fetch
- **Migration Failed**: Check migration version conflicts
- **Test Isolation**: Use `@DirtiesContext` if needed
- **Redis Connection**: Ensure Docker container is running

## Code Quality Standards

### Mandatory Rules
- Constructor injection only (no field injection)
- Java 23 features by default (records, pattern matching)
- No Lombok on JPA entities
- Bean Validation on all DTOs
- Complete test coverage for new code
- Flyway migrations for all schema changes

### Testing Requirements
- Unit tests with JUnit 5 and Mockito
- Slice tests for controllers and repositories
- Integration tests with Testcontainers
- Minimum 80% code coverage
- Test both success and failure scenarios

### Code Review Checklist
- [ ] Security annotations present
- [ ] Transaction boundaries correct
- [ ] Error handling implemented
- [ ] Tests cover edge cases
- [ ] API documented with OpenAPI
- [ ] No hardcoded values
- [ ] Logging at appropriate levels
- [ ] Performance impact considered