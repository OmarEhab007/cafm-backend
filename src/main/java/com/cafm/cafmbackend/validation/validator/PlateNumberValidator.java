package com.cafm.cafmbackend.validation.validator;

import com.cafm.cafmbackend.validation.constraint.ValidPlateNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for Saudi vehicle plate numbers.
 * 
 * Business Rule: Validates Saudi Arabia vehicle plate number format
 * Pattern: 1-4 digits optionally followed by 1-3 English letters
 */
public class PlateNumberValidator implements ConstraintValidator<ValidPlateNumber, String> {
    
    // Saudi plate patterns:
    // Old format: 1-4 digits + 3 letters (e.g., "1234 ABC")
    // New format: 3 letters + 4 digits (e.g., "ABC 1234")
    private static final String OLD_PLATE_PATTERN = "^\\d{1,4}\\s*[A-Z]{3}$";
    private static final String NEW_PLATE_PATTERN = "^[A-Z]{3}\\s*\\d{4}$";
    private static final String DIGITS_ONLY_PATTERN = "^\\d{1,4}$";
    
    @Override
    public void initialize(ValidPlateNumber constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // Let @NotNull handle null validation
        }
        
        // Remove spaces and convert to uppercase for validation
        String normalized = value.replaceAll("\\s+", " ").trim().toUpperCase();
        
        // Check against all valid patterns
        return normalized.matches(OLD_PLATE_PATTERN) || 
               normalized.matches(NEW_PLATE_PATTERN) ||
               normalized.matches(DIGITS_ONLY_PATTERN);
    }
}