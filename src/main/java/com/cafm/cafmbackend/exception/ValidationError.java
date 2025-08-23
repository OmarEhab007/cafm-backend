package com.cafm.cafmbackend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a field-level validation error.
 * Used in validation failure responses to provide detailed feedback.
 * 
 * Architecture: Value object for validation error details
 * Pattern: DTO record with JSON serialization
 * Java 23: Modern record with optional fields using JsonInclude
 * Security: No sensitive information exposed, only validation details
 * 
 * @param field The name of the field that failed validation
 * @param value The rejected value (sanitized for security)
 * @param message The validation error message
 * @param code The specific validation error code
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationError(
    String field,
    Object value,
    String message,
    String code
) {
    
    /**
     * Create a basic validation error with field and message.
     * 
     * @param field The field name
     * @param message The error message
     * @return ValidationError instance
     */
    public static ValidationError of(String field, String message) {
        return new ValidationError(field, null, message, null);
    }
    
    /**
     * Create a validation error with field, message, and error code.
     * 
     * @param field The field name
     * @param message The error message
     * @param code The error code
     * @return ValidationError instance
     */
    public static ValidationError of(String field, String message, String code) {
        return new ValidationError(field, null, message, code);
    }
    
    /**
     * Create a full validation error with all details.
     * Value is sanitized to prevent information disclosure.
     * 
     * @param field The field name
     * @param value The rejected value (will be sanitized)
     * @param message The error message
     * @param code The error code
     * @return ValidationError instance
     */
    public static ValidationError of(String field, Object value, String message, String code) {
        // Sanitize sensitive values
        Object sanitizedValue = sanitizeValue(field, value);
        return new ValidationError(field, sanitizedValue, message, code);
    }
    
    /**
     * Sanitize field values to prevent sensitive information disclosure.
     * 
     * @param field The field name
     * @param value The original value
     * @return Sanitized value safe for client exposure
     */
    private static Object sanitizeValue(String field, Object value) {
        if (value == null) {
            return null;
        }
        
        String fieldLower = field.toLowerCase();
        
        // Hide sensitive field values completely
        if (fieldLower.contains("password") || 
            fieldLower.contains("secret") || 
            fieldLower.contains("token") ||
            fieldLower.contains("key") ||
            fieldLower.contains("pin") ||
            fieldLower.contains("otp")) {
            return "[HIDDEN]";
        }
        
        // Truncate long values to prevent payload bloat
        String stringValue = value.toString();
        if (stringValue.length() > 100) {
            return stringValue.substring(0, 97) + "...";
        }
        
        return value;
    }
}