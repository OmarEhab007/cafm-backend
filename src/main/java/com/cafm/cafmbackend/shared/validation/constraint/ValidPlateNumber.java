package com.cafm.cafmbackend.shared.validation.constraint;

import com.cafm.cafmbackend.shared.validation.validator.PlateNumberValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation constraint for Saudi vehicle plate numbers.
 * 
 * Usage: Apply to String fields that should contain valid plate numbers
 * Business Rule: Ensures proper format for Saudi vehicle registration
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PlateNumberValidator.class)
@Documented
public @interface ValidPlateNumber {
    
    String message() default "Invalid Saudi plate number format";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}