package com.cafm.cafmbackend.exception;

import com.cafm.cafmbackend.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for the CAFM backend application.
 * Provides centralized error handling with standardized responses and security-conscious logging.
 * 
 * CRITICAL SECURITY FEATURES:
 * - Prevents internal stack trace exposure to clients
 * - Sanitizes error messages to prevent information disclosure  
 * - Implements correlation IDs for secure audit trails
 * - Enforces tenant isolation in error responses
 * - Provides localized error messages
 * 
 * Architecture: Centralized exception handling with @ControllerAdvice
 * Pattern: Global error handler with structured responses
 * Java 23: Modern exception handling with enhanced pattern matching
 * Security: NO sensitive information exposed in responses
 * Standards: Follows RFC 7807 Problem Details specification concepts
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    private final AuditService auditService;
    private final ErrorMessageResolver messageResolver;
    
    public GlobalExceptionHandler(AuditService auditService, ErrorMessageResolver messageResolver) {
        this.auditService = auditService;
        this.messageResolver = messageResolver;
    }
    
    // ============ AUTHENTICATION & AUTHORIZATION EXCEPTIONS ============
    
    /**
     * Handle authentication failures.
     * Returns 401 Unauthorized for authentication issues.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        // Log security event without exposing details
        logSecurityException("Authentication failed", ex, correlationId, path);
        
        String message = messageResolver.getMessage(
            "error.authentication.failed",
            "Authentication failed. Please check your credentials.",
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.of(path, ErrorCode.AUTHENTICATION_FAILED, message, correlationId);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * Handle authorization failures.
     * Returns 403 Forbidden for insufficient privileges.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        // Log security event for audit trail
        logSecurityException("Access denied", ex, correlationId, path);
        auditService.logSecurityEvent(
            com.cafm.cafmbackend.data.entity.AuditLog.AuditAction.ACCESS_DENIED, 
            String.format("Access denied at %s - Correlation ID: %s", path, correlationId), 
            request
        );
        
        String message = messageResolver.getMessage(
            "error.access.denied",
            "Access denied. You do not have sufficient privileges for this operation.",
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.of(path, ErrorCode.ACCESS_DENIED, message, correlationId);
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
    
    // ============ MULTI-TENANCY VIOLATIONS ============
    
    /**
     * Handle multi-tenant isolation violations.
     * CRITICAL: These are security violations and must be audited.
     */
    @ExceptionHandler(MultiTenantViolationException.class)
    public ResponseEntity<ErrorResponse> handleMultiTenantViolationException(
            MultiTenantViolationException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        // CRITICAL: Log security violation for monitoring
        logSecurityException("Multi-tenant violation", ex, correlationId, path);
        auditService.logSecurityEvent(
            com.cafm.cafmbackend.data.entity.AuditLog.AuditAction.TENANT_VIOLATION, 
            String.format("Tenant isolation violation at %s - %s - Correlation ID: %s", 
                path, buildTenantViolationContext(ex), correlationId), 
            request
        );
        
        String message = messageResolver.getMessage(
            "error.tenant.violation",
            "Access denied: Invalid tenant context",
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.tenantViolation(path, correlationId);
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
    
    // ============ VALIDATION EXCEPTIONS ============
    
    /**
     * Handle Spring validation failures from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        List<ValidationError> validationErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::convertFieldError)
            .collect(Collectors.toList());
        
        // Add global errors if any
        ex.getBindingResult().getGlobalErrors().forEach(globalError -> {
            ValidationError error = ValidationError.of("object", globalError.getDefaultMessage(), "GLOBAL_ERROR");
            validationErrors.add(error);
        });
        
        logValidationException("Method argument validation failed", ex, correlationId, path, validationErrors);
        
        String message = messageResolver.getMessage(
            "error.validation.failed",
            "Validation failed. Please check your input.",
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.of(path, ErrorCode.VALIDATION_FAILED, message, validationErrors, correlationId);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle Bean Validation constraint violations.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        List<ValidationError> validationErrors = ex.getConstraintViolations()
            .stream()
            .map(this::convertConstraintViolation)
            .collect(Collectors.toList());
        
        logValidationException("Constraint validation failed", ex, correlationId, path, validationErrors);
        
        String message = messageResolver.getMessage(
            "error.constraint.violation",
            "Input validation failed. Please check your data.",
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.of(path, ErrorCode.CONSTRAINT_VIOLATION, message, validationErrors, correlationId);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle custom validation exceptions.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        logValidationException("Custom validation failed", ex, correlationId, path, ex.getValidationErrors());
        
        String message = messageResolver.getMessage(
            "error.validation.custom",
            ex.getMessage(),
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.of(path, ErrorCode.VALIDATION_FAILED, message, ex.getValidationErrors(), correlationId);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    // ============ RESOURCE EXCEPTIONS ============
    
    /**
     * Handle resource not found exceptions.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        logBusinessException("Resource not found", ex, correlationId, path);
        
        String message = messageResolver.getMessage(
            "error.resource.not.found",
            "The requested resource was not found",
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.of(path, ErrorCode.RESOURCE_NOT_FOUND, message, correlationId);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Handle duplicate resource exceptions.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        logBusinessException("Duplicate resource", ex, correlationId, path);
        
        String message = messageResolver.getMessage(
            "error.resource.duplicate",
            "The resource already exists",
            getCurrentLocale()
        );
        
        // Create validation error for the duplicate field
        ValidationError validationError = ValidationError.of(
            ex.getFieldName(),
            ex.getFieldValue(),
            message,
            "DUPLICATE_RESOURCE"
        );
        
        ErrorResponse response = ErrorResponse.of(
            path, 
            ErrorCode.RESOURCE_ALREADY_EXISTS, 
            message, 
            List.of(validationError), 
            correlationId
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
    
    // ============ BUSINESS LOGIC EXCEPTIONS ============
    
    /**
     * Handle business logic violations.
     */
    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ErrorResponse> handleBusinessLogicException(
            BusinessLogicException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        logBusinessException("Business logic violation", ex, correlationId, path);
        
        String message = messageResolver.getMessage(
            "error.business.logic",
            ex.getMessage(),
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.of(path, ErrorCode.BUSINESS_RULE_VIOLATION, message, correlationId);
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // ============ FILE AND STORAGE EXCEPTIONS ============

    /**
     * Handle file upload size exceeded exceptions.
     */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        logRequestException("File size limit exceeded", ex, correlationId, path);
        
        String message = messageResolver.getMessage(
            "error.file.size.exceeded",
            "File size exceeds the maximum allowed limit",
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.of(path, ErrorCode.FILE_TOO_LARGE, message, correlationId);
        return new ResponseEntity<>(response, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    /**
     * Handle missing required request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        logRequestException("Missing request parameter", ex, correlationId, path);
        
        String message = messageResolver.getMessage(
            "error.parameter.missing",
            String.format("Required parameter '%s' is missing", ex.getParameterName()),
            getCurrentLocale()
        );
        
        ValidationError validationError = ValidationError.of(
            ex.getParameterName(),
            null,
            message,
            "MISSING_PARAMETER"
        );
        
        ErrorResponse response = ErrorResponse.of(
            path, 
            ErrorCode.MISSING_REQUIRED_FIELD, 
            message, 
            List.of(validationError), 
            correlationId
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle bind exceptions (form binding errors).
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        List<ValidationError> validationErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::convertFieldError)
            .collect(Collectors.toList());
        
        logValidationException("Binding validation failed", ex, correlationId, path, validationErrors);
        
        String message = messageResolver.getMessage(
            "error.binding.failed",
            "Form binding validation failed. Please check your input.",
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.of(path, ErrorCode.VALIDATION_FAILED, message, validationErrors, correlationId);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // ============ DATABASE AND SQL EXCEPTIONS ============

    /**
     * Handle SQL exceptions that might leak through other handlers.
     */
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorResponse> handleSQLException(
            SQLException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        logDatabaseException("SQL exception", ex, correlationId, path);
        
        // Don't expose SQL details to client for security
        String message = messageResolver.getMessage(
            "error.database.operation",
            "Database operation failed. Please try again.",
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.of(path, ErrorCode.DATABASE_ERROR, message, correlationId);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ============ TIMEOUT AND ASYNC EXCEPTIONS ============

    /**
     * Handle timeout exceptions.
     */
    @ExceptionHandler(java.util.concurrent.TimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeoutException(
            java.util.concurrent.TimeoutException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        logRequestException("Operation timeout", ex, correlationId, path);
        
        String message = messageResolver.getMessage(
            "error.operation.timeout",
            "The operation timed out. Please try again.",
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.of(path, ErrorCode.OPERATION_TIMEOUT, message, correlationId);
        return new ResponseEntity<>(response, HttpStatus.REQUEST_TIMEOUT);
    }

    /**
     * Handle illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        logRequestException("Illegal argument", ex, correlationId, path);
        
        String message = messageResolver.getMessage(
            "error.illegal.argument",
            "Invalid argument provided: " + ex.getMessage(),
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.of(path, ErrorCode.INVALID_FIELD_VALUE, message, correlationId);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle illegal state exceptions.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        logBusinessException("Illegal state", ex, correlationId, path);
        
        String message = messageResolver.getMessage(
            "error.illegal.state",
            "Operation not allowed in current state: " + ex.getMessage(),
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.of(path, ErrorCode.INVALID_OPERATION_STATE, message, correlationId);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    // ============ I/O AND NETWORKING EXCEPTIONS ============

    /**
     * Handle I/O exceptions (file operations, network issues, etc.).
     */
    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(
            java.io.IOException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        logRequestException("I/O exception", ex, correlationId, path);
        
        String message = messageResolver.getMessage(
            "error.io.operation",
            "I/O operation failed. Please try again.",
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.of(path, ErrorCode.IO_ERROR, message, correlationId);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ============ JSON PROCESSING EXCEPTIONS ============

    /**
     * Handle JSON processing exceptions.
     */
    @ExceptionHandler(com.fasterxml.jackson.core.JsonProcessingException.class)
    public ResponseEntity<ErrorResponse> handleJsonProcessingException(
            com.fasterxml.jackson.core.JsonProcessingException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        logRequestException("JSON processing error", ex, correlationId, path);
        
        String message = messageResolver.getMessage(
            "error.json.processing",
            "Invalid JSON format in request",
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.of(path, ErrorCode.INVALID_REQUEST_FORMAT, message, correlationId);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // ============ UNSUPPORTED OPERATION EXCEPTIONS ============

    /**
     * Handle unsupported operation exceptions.
     */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedOperationException(
            UnsupportedOperationException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        logRequestException("Unsupported operation", ex, correlationId, path);
        
        String message = messageResolver.getMessage(
            "error.operation.unsupported",
            "This operation is not supported",
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.of(path, ErrorCode.OPERATION_NOT_SUPPORTED, message, correlationId);
        return new ResponseEntity<>(response, HttpStatus.NOT_IMPLEMENTED);
    }
    
    // ============ DATA INTEGRITY EXCEPTIONS ============
    
    /**
     * Handle data integrity violations.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        logDatabaseException("Data integrity violation", ex, correlationId, path);
        
        // Analyze the exception to provide meaningful messages
        String message = analyzeDataIntegrityViolation(ex);
        ErrorCode errorCode = determineDataIntegrityErrorCode(ex);
        
        ErrorResponse response = ErrorResponse.of(path, errorCode, message, correlationId);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
    
    // ============ HTTP-SPECIFIC EXCEPTIONS ============
    
    /**
     * Handle invalid HTTP request format.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        logRequestException("Invalid request format", ex, correlationId, path);
        
        String message = messageResolver.getMessage(
            "error.request.format",
            "Invalid request format. Please check your JSON payload.",
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.of(path, ErrorCode.INVALID_REQUEST_FORMAT, message, correlationId);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle unsupported HTTP methods.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        logRequestException("Method not supported", ex, correlationId, path);
        
        String message = messageResolver.getMessage(
            "error.method.not.supported",
            String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod()),
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.of(path, ErrorCode.INVALID_REQUEST_FORMAT, message, correlationId);
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }
    
    // ============ ARGUMENT TYPE MISMATCHES ============
    
    /**
     * Handle method argument type mismatches.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        logRequestException("Argument type mismatch", ex, correlationId, path);
        
        String message = messageResolver.getMessage(
            "error.argument.type.mismatch",
            String.format("Invalid value for parameter '%s'. Expected type: %s", 
                ex.getName(), 
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"),
            getCurrentLocale()
        );
        
        ValidationError validationError = ValidationError.of(
            ex.getName(),
            ex.getValue(),
            message,
            "TYPE_MISMATCH"
        );
        
        ErrorResponse response = ErrorResponse.of(
            path, 
            ErrorCode.INVALID_FIELD_VALUE, 
            message, 
            List.of(validationError), 
            correlationId
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    // ============ GENERIC EXCEPTIONS ============
    
    /**
     * Handle all other exceptions as internal server errors.
     * CRITICAL: This prevents sensitive information disclosure.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String path = getRequestPath(request);
        
        // Log the full exception server-side for debugging
        logger.error("Internal server error - Correlation ID: {} - Path: {} - Exception: {}", 
            correlationId, path, ex.getClass().getSimpleName(), ex);
        
        // Audit critical system errors
        auditService.logSecurityEvent(
            com.cafm.cafmbackend.data.entity.AuditLog.AuditAction.ERROR, 
            String.format("Internal server error: %s at %s - Correlation ID: %s", 
                ex.getClass().getSimpleName(), path, correlationId), 
            request
        );
        
        // Return sanitized error response - NO internal details exposed
        String message = messageResolver.getMessage(
            "error.internal.server",
            "An unexpected error occurred. Please contact support if the problem persists.",
            getCurrentLocale()
        );
        
        ErrorResponse response = ErrorResponse.internalServerError(path, correlationId);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    // ============ PRIVATE HELPER METHODS ============
    
    /**
     * Generate a unique correlation ID for request tracing.
     */
    private String generateCorrelationId() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        return correlationId;
    }
    
    /**
     * Get the request path safely.
     */
    private String getRequestPath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "unknown";
    }
    
    /**
     * Get the current user ID from security context.
     */
    private String getCurrentUserId() {
        try {
            // Implementation would depend on your security context
            return "current-user"; // Placeholder
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Get the current locale for error messages.
     */
    private Locale getCurrentLocale() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String acceptLanguage = attributes.getRequest().getHeader("Accept-Language");
                if (acceptLanguage != null && acceptLanguage.startsWith("ar")) {
                    return new Locale("ar");
                }
            }
        } catch (Exception e) {
            logger.debug("Could not determine locale, using default", e);
        }
        return Locale.ENGLISH;
    }
    
    /**
     * Convert Spring field error to ValidationError.
     */
    private ValidationError convertFieldError(FieldError fieldError) {
        return ValidationError.of(
            fieldError.getField(),
            fieldError.getRejectedValue(),
            fieldError.getDefaultMessage(),
            fieldError.getCode()
        );
    }
    
    /**
     * Convert Bean Validation constraint violation to ValidationError.
     */
    private ValidationError convertConstraintViolation(ConstraintViolation<?> violation) {
        String fieldName = violation.getPropertyPath().toString();
        return ValidationError.of(
            fieldName,
            violation.getInvalidValue(),
            violation.getMessage(),
            violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()
        );
    }
    
    /**
     * Analyze data integrity violation to provide meaningful error messages.
     */
    private String analyzeDataIntegrityViolation(DataIntegrityViolationException ex) {
        String exceptionMessage = ex.getMessage();
        if (exceptionMessage == null) {
            return "Data integrity constraint violation";
        }
        
        // Check for common constraint violations
        String lowerMessage = exceptionMessage.toLowerCase();
        if (lowerMessage.contains("unique") || lowerMessage.contains("duplicate")) {
            return "The provided data conflicts with existing records";
        } else if (lowerMessage.contains("foreign key") || lowerMessage.contains("reference")) {
            return "The operation violates data relationships";
        } else if (lowerMessage.contains("not null")) {
            return "Required data is missing";
        } else {
            return "Data integrity constraint violation";
        }
    }
    
    /**
     * Determine appropriate error code for data integrity violations.
     */
    private ErrorCode determineDataIntegrityErrorCode(DataIntegrityViolationException ex) {
        String exceptionMessage = ex.getMessage();
        if (exceptionMessage == null) {
            return ErrorCode.DATA_INTEGRITY_VIOLATION;
        }
        
        String lowerMessage = exceptionMessage.toLowerCase();
        if (lowerMessage.contains("unique") || lowerMessage.contains("duplicate")) {
            return ErrorCode.UNIQUE_CONSTRAINT_VIOLATION;
        } else if (lowerMessage.contains("foreign key")) {
            return ErrorCode.FOREIGN_KEY_CONSTRAINT_VIOLATION;
        } else {
            return ErrorCode.DATA_INTEGRITY_VIOLATION;
        }
    }
    
    /**
     * Build context information for tenant violations.
     */
    private String buildTenantViolationContext(MultiTenantViolationException ex) {
        return String.format("UserTenant: %s, RequestedTenant: %s, Resource: %s/%s",
            ex.getUserTenantId(),
            ex.getRequestedTenantId(),
            ex.getResourceType(),
            ex.getResourceId()
        );
    }
    
    // ============ STRUCTURED LOGGING METHODS ============
    
    private void logSecurityException(String event, Exception ex, String correlationId, String path) {
        logger.warn("Security event: {} - Correlation ID: {} - Path: {} - Exception: {}", 
            event, correlationId, path, ex.getMessage());
    }
    
    private void logBusinessException(String event, Exception ex, String correlationId, String path) {
        logger.info("Business exception: {} - Correlation ID: {} - Path: {} - Message: {}", 
            event, correlationId, path, ex.getMessage());
    }
    
    private void logValidationException(String event, Exception ex, String correlationId, String path, List<ValidationError> errors) {
        logger.info("Validation exception: {} - Correlation ID: {} - Path: {} - Errors: {} - Message: {}", 
            event, correlationId, path, errors.size(), ex.getMessage());
    }
    
    private void logDatabaseException(String event, Exception ex, String correlationId, String path) {
        logger.warn("Database exception: {} - Correlation ID: {} - Path: {} - Type: {}", 
            event, correlationId, path, ex.getClass().getSimpleName());
    }
    
    private void logRequestException(String event, Exception ex, String correlationId, String path) {
        logger.info("Request exception: {} - Correlation ID: {} - Path: {} - Message: {}", 
            event, correlationId, path, ex.getMessage());
    }
}