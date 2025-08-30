package com.cafm.cafmbackend.shared.validation.validator;

import com.cafm.cafmbackend.shared.validation.constraint.PositiveMoney;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

/**
 * Validator for positive monetary amounts.
 * 
 * Business Rule: Ensures monetary values are positive and have valid precision
 * Pattern: Maximum 2 decimal places for currency amounts
 */
public class PositiveMoneyValidator implements ConstraintValidator<PositiveMoney, BigDecimal> {
    
    private boolean allowZero;
    private int maxDecimalPlaces;
    
    @Override
    public void initialize(PositiveMoney constraintAnnotation) {
        this.allowZero = constraintAnnotation.allowZero();
        this.maxDecimalPlaces = constraintAnnotation.maxDecimalPlaces();
    }
    
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Check if negative
        if (value.signum() < 0) {
            return false;
        }
        
        // Check if zero when not allowed
        if (!allowZero && value.signum() == 0) {
            return false;
        }
        
        // Check decimal places
        if (value.scale() > maxDecimalPlaces) {
            // Try to see if trailing zeros can be removed
            BigDecimal stripped = value.stripTrailingZeros();
            if (stripped.scale() > maxDecimalPlaces) {
                return false;
            }
        }
        
        return true;
    }
}