# Global Exception Handler System

## Overview

This comprehensive GlobalExceptionHandler system addresses critical security vulnerabilities by preventing internal error information disclosure while providing structured, localized error responses.

## Architecture

### Core Components

1. **GlobalExceptionHandler** - Central exception handling with @ControllerAdvice
2. **ErrorResponse** - Standardized error response DTO
3. **ErrorCode** - Enumerated error codes for consistent API responses
4. **ValidationError** - Field-level validation error details
5. **ErrorMessageResolver** - Multi-language error message support
6. **Custom Exceptions** - Domain-specific exceptions with context

### Security Features

- **No Stack Trace Exposure** - Internal exceptions never exposed to clients
- **Sanitized Error Messages** - Sensitive information filtered from responses
- **Correlation IDs** - Secure audit trails for request tracing
- **Tenant Isolation** - Multi-tenant security violation detection
- **Structured Logging** - Security events logged for monitoring

## Exception Hierarchy

```
RuntimeException
├── MultiTenantViolationException (Security)
├── BusinessLogicException (Domain Rules)
├── ValidationException (Custom Validation)
├── ResourceNotFoundException (Data Access)
└── DuplicateResourceException (Data Integrity)
```

## Error Response Format

All errors return consistent JSON structure:

```json
{
  "timestamp": "2024-08-20T10:30:00.000Z",
  "path": "/api/v1/resource",
  "code": "RESOURCE_NOT_FOUND",
  "message": "User-friendly localized message",
  "details": [
    {
      "field": "email",
      "value": "sanitized-value",
      "message": "Field-specific error message",
      "code": "VALIDATION_CODE"
    }
  ],
  "correlationId": "uuid-for-tracing"
}
```

## Supported Error Codes

### Authentication & Authorization (40x)
- `AUTHENTICATION_FAILED` - Invalid credentials
- `TOKEN_EXPIRED` - JWT token expired
- `ACCESS_DENIED` - Insufficient privileges
- `TENANT_ISOLATION_VIOLATION` - Cross-tenant access attempt

### Validation Errors (400)
- `VALIDATION_FAILED` - Field validation failures
- `CONSTRAINT_VIOLATION` - Bean validation violations
- `INVALID_REQUEST_FORMAT` - Malformed JSON/requests

### Resource Errors (404, 409)
- `RESOURCE_NOT_FOUND` - Requested resource missing
- `RESOURCE_ALREADY_EXISTS` - Duplicate resource creation
- `DATA_INTEGRITY_VIOLATION` - Database constraint violations

### Business Logic (422)
- `BUSINESS_RULE_VIOLATION` - Domain rule violations
- `INVALID_OPERATION_STATE` - Invalid entity state transitions
- `WORKFLOW_CONSTRAINT_VIOLATION` - Workflow rule violations

### System Errors (500)
- `INTERNAL_SERVER_ERROR` - Unexpected system errors (sanitized)

## Multi-Language Support

The system supports Arabic and English error messages:

```java
// English (default)
error.authentication.failed=Authentication failed. Please check your credentials.

// Arabic
error.authentication.failed=فشل في المصادقة. يرجى التحقق من بيانات الاعتماد
```

Language detection based on `Accept-Language` header:
- `ar` - Arabic messages
- `en` (default) - English messages

## Security Considerations

### Information Disclosure Prevention

1. **Generic Error Messages** - Internal exceptions show sanitized messages only
2. **Sensitive Field Filtering** - Password, token, secret fields never exposed
3. **Stack Trace Suppression** - Full stack traces only in server logs
4. **Correlation ID Tracking** - Secure request tracing without exposing internals

### Tenant Isolation

Multi-tenant violations trigger:
- Immediate security event logging
- Audit trail with correlation ID
- Generic "access denied" response to prevent information leakage

### Audit Logging

All security events are logged with:
- Correlation ID for tracing
- User context (when available)
- Tenant context
- Request details
- No sensitive data

## Usage Examples

### Custom Business Logic Exception

```java
// In service layer
if (workOrder.getStatus() != WorkOrderStatus.ASSIGNED) {
    throw BusinessLogicException.invalidState(
        "WorkOrder", 
        workOrder.getId().toString(), 
        workOrder.getStatus().toString(), 
        "ASSIGNED"
    );
}
```

### Multi-Tenant Security Violation

```java
// In repository/security layer  
if (!currentTenantId.equals(resource.getTenantId())) {
    throw MultiTenantViolationException.resourceAccess(
        currentTenantId, 
        resource.getTenantId(),
        "Report", 
        resource.getId().toString()
    );
}
```

### Custom Validation

```java
// In service layer
if (userRepository.existsByEmail(email)) {
    throw ValidationException.uniqueConstraint("email", email, "User");
}
```

## Integration Instructions

### 1. Enable Component Scanning

Ensure the exception package is scanned:

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.cafm.cafmbackend"})
public class CafmBackendApplication {
    // Application code
}
```

### 2. Configuration Required

The system requires:
- **AuditService** bean for security logging
- **MessageSource** bean for i18n support
- **Spring Security** for user context

### 3. Update Controllers

Remove manual exception handling from controllers - let GlobalExceptionHandler manage all errors:

```java
// Before (DON'T DO THIS)
@GetMapping("/{id}")
public ResponseEntity<?> getUser(@PathVariable UUID id) {
    try {
        User user = userService.findById(id);
        return ResponseEntity.ok(user);
    } catch (UserNotFoundException e) {
        return ResponseEntity.notFound().build();
    }
}

// After (CORRECT)
@GetMapping("/{id}")
public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
    User user = userService.findById(id); // Let GlobalExceptionHandler handle exceptions
    return ResponseEntity.ok(userMapper.toResponse(user));
}
```

### 4. Use Custom Exceptions

Replace generic exceptions with domain-specific ones:

```java
// Service layer example
public User findById(UUID id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
}
```

## Testing

### Unit Tests
- `GlobalExceptionHandlerTest` - Tests all exception scenarios
- `CustomExceptionTests` - Tests custom exception factory methods

### Integration Tests  
- `GlobalExceptionHandlerIntegrationTest` - End-to-end error handling

### Security Tests
Run tests to verify:
- No sensitive information exposure
- Proper correlation ID generation
- Security event logging
- Tenant isolation enforcement

## Monitoring & Alerting

Security events are logged with structured format:

```
SECURITY EVENT: TENANT_ISOLATION_VIOLATION - User: user123 - Path: /api/v1/reports/456 - Correlation: uuid-123 - Context: UserTenant: tenant1, RequestedTenant: tenant2
```

Set up alerts for:
- `TENANT_ISOLATION_VIOLATION` events
- High frequency of `AUTHENTICATION_FAILED` events  
- `INTERNAL_SERVER_ERROR` patterns

## Production Considerations

1. **Log Retention** - Ensure security logs are retained per compliance requirements
2. **Monitoring** - Set up dashboards for error patterns and security events
3. **Performance** - Async audit logging prevents blocking main request flow
4. **Scaling** - Correlation IDs enable distributed request tracing

## Migration from Existing Code

1. **Remove Try-Catch Blocks** - Let GlobalExceptionHandler manage exceptions
2. **Replace Generic Exceptions** - Use domain-specific custom exceptions
3. **Update Error Responses** - Remove manual error response creation
4. **Add Validation** - Use `@Valid` annotations and custom validators
5. **Test Thoroughly** - Verify no sensitive information exposure

## Compliance

This implementation addresses:
- **OWASP A09:2021** - Security Logging and Monitoring Failures
- **OWASP A01:2021** - Broken Access Control (tenant isolation)
- **OWASP A04:2021** - Insecure Design (secure error handling)

The system provides production-ready, security-conscious error handling that prevents information disclosure while maintaining excellent user experience through localized, meaningful error messages.