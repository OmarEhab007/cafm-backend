package com.cafm.cafmbackend.shared.exception;

import java.util.List;
import java.util.ArrayList;

/**
 * Exception thrown when business validation rules are violated.
 * 
 * Purpose: Provides structured error handling for business rule violations
 * Pattern: Domain-specific exception with validation error details
 * Java 23: Leverages enhanced exception handling and collections
 * Architecture: Part of exception hierarchy for business validation
 * Standards: Consistent validation error messaging and aggregation
 */
public class BusinessValidationException extends RuntimeException {
    
    private final List<String> validationErrors;
    private final String field;
    
    public BusinessValidationException(String message) {
        super(message);
        this.validationErrors = List.of(message);
        this.field = null;
    }
    
    public BusinessValidationException(String field, String message) {
        super(String.format("Validation failed for field '%s': %s", field, message));
        this.validationErrors = List.of(message);
        this.field = field;
    }
    
    public BusinessValidationException(List<String> validationErrors) {
        super("Multiple validation errors occurred: " + String.join(", ", validationErrors));
        this.validationErrors = new ArrayList<>(validationErrors);
        this.field = null;
    }
    
    public BusinessValidationException(String message, Throwable cause) {
        super(message, cause);
        this.validationErrors = List.of(message);
        this.field = null;
    }
    
    public List<String> getValidationErrors() {
        return validationErrors;
    }
    
    public String getField() {
        return field;
    }
    
    public boolean hasField() {
        return field != null;
    }
    
    public boolean hasMultipleErrors() {
        return validationErrors.size() > 1;
    }
}