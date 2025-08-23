package com.cafm.cafmbackend.validation.validator;

import com.cafm.cafmbackend.validation.constraint.ValidArabicText;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for Arabic text fields.
 * 
 * Business Rule: Ensures text contains valid Arabic characters
 * Pattern: Supports Arabic letters, numbers, and common punctuation
 */
public class ArabicTextValidator implements ConstraintValidator<ValidArabicText, String> {
    
    // Arabic Unicode blocks
    private static final String ARABIC_PATTERN = "^[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\s\\d\\p{Punct}]+$";
    
    private boolean allowEmpty;
    private boolean allowMixed;
    
    @Override
    public void initialize(ValidArabicText constraintAnnotation) {
        this.allowEmpty = constraintAnnotation.allowEmpty();
        this.allowMixed = constraintAnnotation.allowMixed();
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        if (value.isEmpty()) {
            return allowEmpty;
        }
        
        // If mixed content is allowed, just check if it contains some Arabic
        if (allowMixed) {
            return containsArabic(value);
        }
        
        // Otherwise, ensure it's purely Arabic (with allowed punctuation and digits)
        return value.matches(ARABIC_PATTERN);
    }
    
    /**
     * Check if string contains at least one Arabic character
     */
    private boolean containsArabic(String text) {
        for (char c : text.toCharArray()) {
            if ((c >= 0x0600 && c <= 0x06FF) || 
                (c >= 0x0750 && c <= 0x077F) || 
                (c >= 0x08A0 && c <= 0x08FF)) {
                return true;
            }
        }
        return false;
    }
}