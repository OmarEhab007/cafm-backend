package com.cafm.cafmbackend.exception;

/**
 * Standardized error codes for API responses.
 * Each error code corresponds to a specific category of error condition.
 * 
 * Architecture: Enumerated error codes for consistent API responses
 * Pattern: Error code standardization for client applications
 * Java 23: Uses modern enum features with enhanced string handling
 * Security: No sensitive information exposed in error codes
 */
public enum ErrorCode {
    
    // Authentication and Authorization (40x)
    AUTHENTICATION_FAILED("AUTHENTICATION_FAILED"),
    TOKEN_EXPIRED("TOKEN_EXPIRED"),
    TOKEN_INVALID("TOKEN_INVALID"),
    ACCESS_DENIED("ACCESS_DENIED"),
    INSUFFICIENT_PRIVILEGES("INSUFFICIENT_PRIVILEGES"),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED"),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED"),
    
    // Validation Errors (400)
    VALIDATION_FAILED("VALIDATION_FAILED"),
    INVALID_REQUEST_FORMAT("INVALID_REQUEST_FORMAT"),
    MISSING_REQUIRED_FIELD("MISSING_REQUIRED_FIELD"),
    INVALID_FIELD_VALUE("INVALID_FIELD_VALUE"),
    CONSTRAINT_VIOLATION("CONSTRAINT_VIOLATION"),
    
    // Resource Errors (404, 409)
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND"),
    USER_NOT_FOUND("USER_NOT_FOUND"),
    COMPANY_NOT_FOUND("COMPANY_NOT_FOUND"),
    SCHOOL_NOT_FOUND("SCHOOL_NOT_FOUND"),
    REPORT_NOT_FOUND("REPORT_NOT_FOUND"),
    WORK_ORDER_NOT_FOUND("WORK_ORDER_NOT_FOUND"),
    ASSET_NOT_FOUND("ASSET_NOT_FOUND"),
    RESOURCE_ALREADY_EXISTS("RESOURCE_ALREADY_EXISTS"),
    DUPLICATE_EMAIL("DUPLICATE_EMAIL"),
    DUPLICATE_USERNAME("DUPLICATE_USERNAME"),
    DUPLICATE_EMPLOYEE_ID("DUPLICATE_EMPLOYEE_ID"),
    
    // Multi-tenancy Violations (403)
    TENANT_ISOLATION_VIOLATION("TENANT_ISOLATION_VIOLATION"),
    CROSS_TENANT_ACCESS_DENIED("CROSS_TENANT_ACCESS_DENIED"),
    INVALID_TENANT_CONTEXT("INVALID_TENANT_CONTEXT"),
    
    // Business Logic Errors (422)
    BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION"),
    INVALID_OPERATION_STATE("INVALID_OPERATION_STATE"),
    WORKFLOW_CONSTRAINT_VIOLATION("WORKFLOW_CONSTRAINT_VIOLATION"),
    INSUFFICIENT_INVENTORY("INSUFFICIENT_INVENTORY"),
    INSUFFICIENT_STOCK("INSUFFICIENT_STOCK"),
    WORK_ORDER_ALREADY_ASSIGNED("WORK_ORDER_ALREADY_ASSIGNED"),
    INVALID_DATE_RANGE("INVALID_DATE_RANGE"),
    FEATURE_DISABLED("FEATURE_DISABLED"),
    NOT_IMPLEMENTED("NOT_IMPLEMENTED"),
    
    // Data Integrity (409)
    DATA_INTEGRITY_VIOLATION("DATA_INTEGRITY_VIOLATION"),
    FOREIGN_KEY_CONSTRAINT_VIOLATION("FOREIGN_KEY_CONSTRAINT_VIOLATION"),
    UNIQUE_CONSTRAINT_VIOLATION("UNIQUE_CONSTRAINT_VIOLATION"),
    CONCURRENT_MODIFICATION("CONCURRENT_MODIFICATION"),
    
    // Rate Limiting (429)
    RATE_LIMIT_EXCEEDED("RATE_LIMIT_EXCEEDED"),
    TOO_MANY_REQUESTS("TOO_MANY_REQUESTS"),
    API_QUOTA_EXCEEDED("API_QUOTA_EXCEEDED"),
    
    // External Service Errors (502, 503, 504)
    EXTERNAL_SERVICE_UNAVAILABLE("EXTERNAL_SERVICE_UNAVAILABLE"),
    UPSTREAM_SERVICE_ERROR("UPSTREAM_SERVICE_ERROR"),
    SERVICE_TIMEOUT("SERVICE_TIMEOUT"),
    
    // File and Media Errors (400, 413, 415)
    FILE_UPLOAD_FAILED("FILE_UPLOAD_FAILED"),
    INVALID_FILE_FORMAT("INVALID_FILE_FORMAT"),
    FILE_SIZE_EXCEEDED("FILE_SIZE_EXCEEDED"),
    FILE_TOO_LARGE("FILE_TOO_LARGE"),
    FILE_NOT_FOUND("FILE_NOT_FOUND"),
    
    // System Errors (500)
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR"),
    DATABASE_ERROR("DATABASE_ERROR"),
    CONFIGURATION_ERROR("CONFIGURATION_ERROR"),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE"),
    OPERATION_TIMEOUT("OPERATION_TIMEOUT"),
    IO_ERROR("IO_ERROR"),
    OPERATION_NOT_SUPPORTED("OPERATION_NOT_SUPPORTED"),
    
    // FCM and Notification Errors
    FCM_TOKEN_REGISTRATION_FAILED("FCM_TOKEN_REGISTRATION_FAILED");
    
    private final String code;
    
    ErrorCode(String code) {
        this.code = code;
    }
    
    /**
     * Get the string representation of the error code.
     * 
     * @return The error code as a string
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Get the error code by string value.
     * Useful for parsing and validation.
     * 
     * @param code The string code to match
     * @return The matching ErrorCode or null if not found
     */
    public static ErrorCode fromCode(String code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return code;
    }
}