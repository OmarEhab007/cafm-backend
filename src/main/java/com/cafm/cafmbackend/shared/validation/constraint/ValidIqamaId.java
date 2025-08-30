package com.cafm.cafmbackend.shared.validation.constraint;

import com.cafm.cafmbackend.shared.validation.validator.IqamaIdValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation constraint for Saudi Iqama ID.
 * 
 * Usage: Apply to String fields that should contain valid Iqama IDs
 * Business Rule: Ensures proper format for Saudi residence permits
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IqamaIdValidator.class)
@Documented
public @interface ValidIqamaId {
    
    String message() default "Invalid Iqama ID format. Must be 10 digits starting with 1 or 2";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}