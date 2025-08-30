package com.cafm.cafmbackend.shared.validation.constraint;

import com.cafm.cafmbackend.shared.validation.validator.ReportDateValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation constraint for report date consistency.
 * 
 * Usage: Apply to Report entity class
 * Business Rule: Ensures logical date progression in work orders
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReportDateValidator.class)
@Documented
public @interface ValidReportDates {
    
    String message() default "Report dates are not in valid chronological order";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}