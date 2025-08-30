package com.cafm.cafmbackend.shared.validation.constraint;

import com.cafm.cafmbackend.shared.validation.validator.ArabicTextValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation constraint for Arabic text.
 * 
 * Usage: Apply to String fields that should contain Arabic text
 * Business Rule: Ensures proper Arabic content for localized fields
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ArabicTextValidator.class)
@Documented
public @interface ValidArabicText {
    
    String message() default "Field must contain valid Arabic text";
    
    boolean allowEmpty() default false;
    
    boolean allowMixed() default false; // Allow mixed Arabic/English
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}