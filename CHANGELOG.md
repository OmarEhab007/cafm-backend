# Changelog

All notable changes to the CAFM Backend project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- GitHub Actions CI/CD workflows for automated testing and deployment
- Comprehensive .gitignore file with platform-specific and tool-specific exclusions
- CHANGELOG.md for version tracking
- Multi-tenant architecture with company-based isolation
- JWT authentication with refresh token support
- Role-based access control (ADMIN, SUPERVISOR, TECHNICIAN)
- Asset management system for tracking school equipment
- Work order management with task assignment
- Inventory tracking with transaction history
- File upload service with MinIO integration
- Push notification service via Firebase Cloud Messaging
- Audit logging for compliance and tracking
- Redis caching for improved performance
- Flyway database migrations
- OpenAPI/Swagger documentation
- Comprehensive test suite with JUnit 5 and Testcontainers
- Docker Compose setup for local development
- Performance monitoring with Micrometer metrics

### Changed
- Upgraded to Java 23 with preview features
- Migrated to Spring Boot 3.3.3
- Updated all dependencies to latest stable versions
- Improved error handling with centralized exception management
- Enhanced security configuration with method-level authorization

### Fixed
- SQL injection vulnerabilities in custom queries
- N+1 query issues in entity relationships
- Memory leaks in file upload processing
- Race conditions in concurrent work order updates
- CORS configuration for cross-origin requests

### Security
- Implemented rate limiting per tenant
- Added input validation for all API endpoints
- Encrypted sensitive data at rest
- Secured file uploads with virus scanning
- Implemented OWASP security best practices

## [0.9.0] - 2024-08-23

### Added
- Initial backend implementation migrated from Supabase
- Core domain models and entities
- RESTful API endpoints for all major features
- PostgreSQL database schema
- Basic authentication and authorization
- File storage service
- Email notification service
- Excel/PDF report generation
- Docker containerization

### Known Issues
- Performance optimization needed for large datasets
- Some edge cases in work order state transitions
- Memory usage spikes during bulk operations
- Incomplete localization for Arabic language

## [0.8.0] - 2024-08-09

### Added
- Project initialization with Spring Initializr
- Basic project structure following Clean Architecture
- Maven build configuration
- Development environment setup
- Initial database design
- Core dependencies and libraries

### Documentation
- README.md with project overview
- CONTRIBUTING.md with contribution guidelines
- API documentation structure
- Development setup instructions
- Architecture decision records

## Versioning Strategy

This project follows Semantic Versioning (SemVer):

- **MAJOR** version (X.0.0): Incompatible API changes
- **MINOR** version (0.X.0): New functionality in a backward-compatible manner
- **PATCH** version (0.0.X): Backward-compatible bug fixes

### Pre-release versions:
- Alpha: X.Y.Z-alpha.N (feature complete, not thoroughly tested)
- Beta: X.Y.Z-beta.N (feature complete, tested, may have known issues)
- Release Candidate: X.Y.Z-rc.N (potential final release)

### Version Tagging:
```bash
# Create a new version tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# Create a pre-release tag
git tag -a v1.0.0-beta.1 -m "Beta release 1.0.0-beta.1"
git push origin v1.0.0-beta.1
```

## Release Process

1. Update version in `pom.xml`
2. Update CHANGELOG.md with release notes
3. Commit changes: `git commit -m "chore: prepare release vX.Y.Z"`
4. Create tag: `git tag -a vX.Y.Z -m "Release version X.Y.Z"`
5. Push changes and tags: `git push && git push --tags`
6. GitHub Actions will automatically:
   - Run tests
   - Build artifacts
   - Deploy to staging
   - Create GitHub release
   - Deploy to production (after approval)

## Migration Notes

### Migrating from 0.8.x to 0.9.x
- Database schema changes require running new Flyway migrations
- JWT secret key must be regenerated for security
- Redis cache must be cleared after upgrade
- Update environment variables as per .env.example

### Migrating from 0.9.x to 1.0.x (Future)
- Breaking API changes will be documented here
- Database migration scripts will be provided
- Backward compatibility layer for deprecation period

[Unreleased]: https://github.com/yourusername/cafm-backend/compare/v0.9.0...HEAD
[0.9.0]: https://github.com/yourusername/cafm-backend/compare/v0.8.0...v0.9.0
[0.8.0]: https://github.com/yourusername/cafm-backend/releases/tag/v0.8.0