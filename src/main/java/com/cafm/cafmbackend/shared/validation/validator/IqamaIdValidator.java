package com.cafm.cafmbackend.shared.validation.validator;

import com.cafm.cafmbackend.shared.validation.constraint.ValidIqamaId;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for Saudi Iqama ID format.
 * 
 * Business Rule: Validates Saudi Arabia Iqama (residence permit) number format
 * Pattern: Must be 10 digits starting with 1 or 2
 */
public class IqamaIdValidator implements ConstraintValidator<ValidIqamaId, String> {
    
    private static final String IQAMA_PATTERN = "^[12]\\d{9}$";
    
    @Override
    public void initialize(ValidIqamaId constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // Let @NotNull handle null validation
        }
        
        // Check if it matches the pattern
        if (!value.matches(IQAMA_PATTERN)) {
            return false;
        }
        
        // Additional validation: Luhn algorithm check for Saudi IDs
        return isValidLuhn(value);
    }
    
    /**
     * Validates the ID using Luhn algorithm
     */
    private boolean isValidLuhn(String number) {
        int sum = 0;
        boolean alternate = false;
        
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return (sum % 10) == 0;
    }
}