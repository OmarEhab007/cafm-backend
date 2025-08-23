package com.cafm.cafmbackend.validation.constraint;

import com.cafm.cafmbackend.validation.validator.PositiveMoneyValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation constraint for positive monetary amounts.
 * 
 * Usage: Apply to BigDecimal fields representing money
 * Business Rule: Ensures valid currency amounts
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PositiveMoneyValidator.class)
@Documented
public @interface PositiveMoney {
    
    String message() default "Amount must be a positive monetary value with maximum {maxDecimalPlaces} decimal places";
    
    boolean allowZero() default true;
    
    int maxDecimalPlaces() default 2;
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}