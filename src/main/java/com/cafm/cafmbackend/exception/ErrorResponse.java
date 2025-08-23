package com.cafm.cafmbackend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

/**
 * Standardized error response structure for all API errors.
 * Provides consistent error format across all endpoints.
 * 
 * Architecture: Standardized API error response DTO
 * Pattern: Builder pattern with immutable record
 * Java 23: Modern record with JSON serialization and factory methods
 * Security: No internal system information exposed, sanitized messages only
 * 
 * @param timestamp When the error occurred (ISO 8601 format)
 * @param path The request path that caused the error
 * @param code Standardized error code for programmatic handling
 * @param message User-friendly error message (localized)
 * @param details List of detailed error information (e.g., validation errors)
 * @param correlationId Unique identifier for tracing requests (optional)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    LocalDateTime timestamp,
    String path,
    String code,
    String message,
    List<ValidationError> details,
    String correlationId
) {
    
    /**
     * Create a basic error response with minimal information.
     * 
     * @param path The request path
     * @param code The error code
     * @param message The error message
     * @return ErrorResponse instance
     */
    public static ErrorResponse of(String path, ErrorCode code, String message) {
        return new ErrorResponse(
            LocalDateTime.now(),
            path,
            code.getCode(),
            message,
            Collections.emptyList(),
            null
        );
    }
    
    /**
     * Create an error response with correlation ID for tracing.
     * 
     * @param path The request path
     * @param code The error code
     * @param message The error message
     * @param correlationId The correlation ID for request tracing
     * @return ErrorResponse instance
     */
    public static ErrorResponse of(String path, ErrorCode code, String message, String correlationId) {
        return new ErrorResponse(
            LocalDateTime.now(),
            path,
            code.getCode(),
            message,
            Collections.emptyList(),
            correlationId
        );
    }
    
    /**
     * Create an error response with validation details.
     * 
     * @param path The request path
     * @param code The error code
     * @param message The error message
     * @param details List of validation errors
     * @return ErrorResponse instance
     */
    public static ErrorResponse of(String path, ErrorCode code, String message, List<ValidationError> details) {
        return new ErrorResponse(
            LocalDateTime.now(),
            path,
            code.getCode(),
            message,
            details != null ? details : Collections.emptyList(),
            null
        );
    }
    
    /**
     * Create a complete error response with all information.
     * 
     * @param path The request path
     * @param code The error code
     * @param message The error message
     * @param details List of validation errors
     * @param correlationId The correlation ID for request tracing
     * @return ErrorResponse instance
     */
    public static ErrorResponse of(String path, ErrorCode code, String message, 
                                 List<ValidationError> details, String correlationId) {
        return new ErrorResponse(
            LocalDateTime.now(),
            path,
            code.getCode(),
            message,
            details != null ? details : Collections.emptyList(),
            correlationId
        );
    }
    
    /**
     * Create an internal server error response with minimal information.
     * Used for unexpected exceptions to prevent information disclosure.
     * 
     * @param path The request path
     * @param correlationId The correlation ID for server-side tracing
     * @return Sanitized error response for internal server errors
     */
    public static ErrorResponse internalServerError(String path, String correlationId) {
        return new ErrorResponse(
            LocalDateTime.now(),
            path,
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            "An unexpected error occurred. Please contact support if the problem persists.",
            Collections.emptyList(),
            correlationId
        );
    }
    
    /**
     * Create a validation error response from a list of field errors.
     * 
     * @param path The request path
     * @param validationErrors List of field-level validation errors
     * @return ErrorResponse with validation details
     */
    public static ErrorResponse validationError(String path, List<ValidationError> validationErrors) {
        String message = validationErrors.size() == 1 
            ? "Validation failed for field: " + validationErrors.get(0).field()
            : String.format("Validation failed for %d fields", validationErrors.size());
            
        return new ErrorResponse(
            LocalDateTime.now(),
            path,
            ErrorCode.VALIDATION_FAILED.getCode(),
            message,
            validationErrors,
            null
        );
    }
    
    /**
     * Create a tenant isolation violation error response.
     * 
     * @param path The request path
     * @param correlationId The correlation ID for security auditing
     * @return ErrorResponse for tenant violation
     */
    public static ErrorResponse tenantViolation(String path, String correlationId) {
        return new ErrorResponse(
            LocalDateTime.now(),
            path,
            ErrorCode.TENANT_ISOLATION_VIOLATION.getCode(),
            "Access denied: Invalid tenant context",
            Collections.emptyList(),
            correlationId
        );
    }
    
    /**
     * Create a resource not found error response.
     * 
     * @param path The request path
     * @param resourceType The type of resource that was not found
     * @return ErrorResponse for missing resource
     */
    public static ErrorResponse resourceNotFound(String path, String resourceType) {
        return new ErrorResponse(
            LocalDateTime.now(),
            path,
            ErrorCode.RESOURCE_NOT_FOUND.getCode(),
            String.format("%s not found", resourceType),
            Collections.emptyList(),
            null
        );
    }
}