package com.cafm.cafmbackend.validation.constraint;

import com.cafm.cafmbackend.validation.validator.StrongPasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation constraint for strong password requirements.
 * 
 * Enforces:
 * - Minimum 12 characters
 * - Complexity requirements (uppercase, lowercase, digit, special char)
 * - No common passwords
 * - No sequential or repeated patterns
 * - Sufficient entropy
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    
    String message() default "Password does not meet security requirements";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}