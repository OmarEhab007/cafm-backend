package com.cafm.cafmbackend.shared.exception;

/**
 * Exception thrown when external service calls fail.
 * 
 * Purpose: Signals external service integration failures
 * Pattern: Infrastructure exception for service dependencies
 * Java 23: Enhanced exception with service context
 * Architecture: External integration failure management
 * Standards: Proper service failure handling with retry context
 */
public class ExternalServiceException extends RuntimeException {

    private final String serviceName;
    private final String operation;
    private final int httpStatus;
    private final boolean retryable;

    public ExternalServiceException(String message, String serviceName, String operation, int httpStatus, boolean retryable) {
        super(message);
        this.serviceName = serviceName;
        this.operation = operation;
        this.httpStatus = httpStatus;
        this.retryable = retryable;
    }

    public ExternalServiceException(String message, Throwable cause, String serviceName, String operation, int httpStatus, boolean retryable) {
        super(message, cause);
        this.serviceName = serviceName;
        this.operation = operation;
        this.httpStatus = httpStatus;
        this.retryable = retryable;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getOperation() {
        return operation;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public static ExternalServiceException timeout(String serviceName, String operation) {
        String message = String.format("Timeout calling %s service for operation %s", serviceName, operation);
        return new ExternalServiceException(message, serviceName, operation, 0, true);
    }

    public static ExternalServiceException serviceUnavailable(String serviceName, String operation) {
        String message = String.format("%s service is unavailable for operation %s", serviceName, operation);
        return new ExternalServiceException(message, serviceName, operation, 503, true);
    }

    public static ExternalServiceException badRequest(String serviceName, String operation, String reason) {
        String message = String.format("Bad request to %s service for operation %s: %s", serviceName, operation, reason);
        return new ExternalServiceException(message, serviceName, operation, 400, false);
    }

    public static ExternalServiceException unauthorized(String serviceName, String operation) {
        String message = String.format("Unauthorized access to %s service for operation %s", serviceName, operation);
        return new ExternalServiceException(message, serviceName, operation, 401, false);
    }

    public static ExternalServiceException rateLimitExceeded(String serviceName, String operation) {
        String message = String.format("Rate limit exceeded for %s service operation %s", serviceName, operation);
        return new ExternalServiceException(message, serviceName, operation, 429, true);
    }
}