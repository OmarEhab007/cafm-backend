package com.cafm.cafmbackend.shared.exception;

import java.util.List;
import java.util.Collections;

/**
 * Exception thrown when custom validation rules are violated.
 * Complements standard Bean Validation with application-specific validation logic.
 * 
 * Architecture: Custom validation exception with detailed error information
 * Pattern: Exception with structured validation error details
 * Java 23: Modern exception handling with factory methods
 * Standards: Integrates with Spring validation framework
 */
public class ValidationException extends RuntimeException {
    
    private final List<ValidationError> validationErrors;
    private final String fieldName;
    private final Object fieldValue;
    
    /**
     * Create a validation exception with basic message.
     * 
     * @param message The validation error message
     */
    public ValidationException(String message) {
        super(message);
        this.validationErrors = Collections.emptyList();
        this.fieldName = null;
        this.fieldValue = null;
    }
    
    /**
     * Create a validation exception with field context.
     * 
     * @param message The validation error message
     * @param fieldName The name of the field that failed validation
     * @param fieldValue The value that failed validation
     */
    public ValidationException(String message, String fieldName, Object fieldValue) {
        super(message);
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.validationErrors = List.of(ValidationError.of(fieldName, message));
    }
    
    /**
     * Create a validation exception with multiple validation errors.
     * 
     * @param message The overall validation error message
     * @param validationErrors List of specific field validation errors
     */
    public ValidationException(String message, List<ValidationError> validationErrors) {
        super(message);
        this.validationErrors = validationErrors != null ? validationErrors : Collections.emptyList();
        this.fieldName = null;
        this.fieldValue = null;
    }
    
    /**
     * Create a validation exception with cause.
     * 
     * @param message The validation error message
     * @param cause The underlying cause
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.validationErrors = Collections.emptyList();
        this.fieldName = null;
        this.fieldValue = null;
    }
    
    /**
     * Factory method for single field validation failures.
     * 
     * @param fieldName The field that failed validation
     * @param fieldValue The invalid value
     * @param message The validation message
     * @return ValidationException instance
     */
    public static ValidationException fieldValidation(String fieldName, Object fieldValue, String message) {
        return new ValidationException(
            String.format("Validation failed for field '%s': %s", fieldName, message),
            fieldName, 
            fieldValue
        );
    }
    
    /**
     * Factory method for cross-field validation failures.
     * 
     * @param fields The fields involved in the validation
     * @param message The validation message
     * @return ValidationException instance
     */
    public static ValidationException crossFieldValidation(List<String> fields, String message) {
        String fieldNames = String.join(", ", fields);
        String fullMessage = String.format("Cross-field validation failed for fields [%s]: %s", fieldNames, message);
        
        List<ValidationError> errors = fields.stream()
            .map(field -> ValidationError.of(field, message, "CROSS_FIELD_VALIDATION"))
            .toList();
            
        return new ValidationException(fullMessage, errors);
    }
    
    /**
     * Factory method for business validation failures.
     * 
     * @param fieldName The field being validated
     * @param businessRule The business rule that was violated
     * @param message The validation message
     * @return ValidationException instance
     */
    public static ValidationException businessRule(String fieldName, String businessRule, String message) {
        ValidationError error = ValidationError.of(fieldName, message, businessRule);
        return new ValidationException(
            String.format("Business validation failed for field '%s': %s", fieldName, message),
            List.of(error)
        );
    }
    
    /**
     * Factory method for entity state validation failures.
     * 
     * @param entityType The type of entity being validated
     * @param entityId The entity identifier
     * @param currentState The current state
     * @param expectedState The expected state
     * @return ValidationException instance
     */
    public static ValidationException entityState(String entityType, String entityId, 
                                                 String currentState, String expectedState) {
        String message = String.format("%s '%s' is in state '%s' but expected '%s'", 
            entityType, entityId, currentState, expectedState);
        ValidationError error = ValidationError.of("state", currentState, "INVALID_ENTITY_STATE");
        return new ValidationException(message, List.of(error));
    }
    
    /**
     * Factory method for unique constraint validation failures.
     * 
     * @param fieldName The field that must be unique
     * @param fieldValue The duplicate value
     * @param entityType The type of entity
     * @return ValidationException instance
     */
    public static ValidationException uniqueConstraint(String fieldName, Object fieldValue, String entityType) {
        String message = String.format("%s with %s '%s' already exists", entityType, fieldName, fieldValue);
        ValidationError error = ValidationError.of(fieldName, fieldValue, message, "UNIQUE_CONSTRAINT_VIOLATION");
        return new ValidationException(message, List.of(error));
    }
    
    /**
     * Factory method for format validation failures.
     * 
     * @param fieldName The field with invalid format
     * @param fieldValue The invalid value
     * @param expectedFormat The expected format description
     * @return ValidationException instance
     */
    public static ValidationException invalidFormat(String fieldName, Object fieldValue, String expectedFormat) {
        String message = String.format("Invalid format for field '%s'. Expected: %s", fieldName, expectedFormat);
        ValidationError error = ValidationError.of(fieldName, fieldValue, message, "INVALID_FORMAT");
        return new ValidationException(message, List.of(error));
    }
    
    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public Object getFieldValue() {
        return fieldValue;
    }
    
    /**
     * Check if this exception has multiple validation errors.
     * 
     * @return true if multiple errors exist
     */
    public boolean hasMultipleErrors() {
        return validationErrors.size() > 1;
    }
}