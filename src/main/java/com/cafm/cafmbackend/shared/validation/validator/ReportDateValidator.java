package com.cafm.cafmbackend.shared.validation.validator;

import com.cafm.cafmbackend.shared.validation.constraint.ValidReportDates;
import com.cafm.cafmbackend.infrastructure.persistence.entity.Report;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

/**
 * Validator for report date consistency.
 * 
 * Business Rule: Ensures logical date progression in reports
 * - Scheduled date must be after or equal to reported date
 * - Completed date must be after or equal to scheduled date
 */
public class ReportDateValidator implements ConstraintValidator<ValidReportDates, Report> {
    
    @Override
    public void initialize(ValidReportDates constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(Report report, ConstraintValidatorContext context) {
        if (report == null) {
            return true;
        }
        
        LocalDate reportedDate = report.getReportedDate();
        LocalDate scheduledDate = report.getScheduledDate();
        LocalDate completedDate = report.getCompletedDate();
        
        boolean valid = true;
        context.disableDefaultConstraintViolation();
        
        // Scheduled date must be after or equal to reported date
        if (reportedDate != null && scheduledDate != null) {
            if (scheduledDate.isBefore(reportedDate)) {
                context.buildConstraintViolationWithTemplate(
                    "Scheduled date cannot be before reported date"
                ).addPropertyNode("scheduledDate").addConstraintViolation();
                valid = false;
            }
        }
        
        // Completed date must be after or equal to scheduled date
        if (scheduledDate != null && completedDate != null) {
            if (completedDate.isBefore(scheduledDate)) {
                context.buildConstraintViolationWithTemplate(
                    "Completed date cannot be before scheduled date"
                ).addPropertyNode("completedDate").addConstraintViolation();
                valid = false;
            }
        }
        
        // Completed date must be after or equal to reported date
        if (reportedDate != null && completedDate != null) {
            if (completedDate.isBefore(reportedDate)) {
                context.buildConstraintViolationWithTemplate(
                    "Completed date cannot be before reported date"
                ).addPropertyNode("completedDate").addConstraintViolation();
                valid = false;
            }
        }
        
        return valid;
    }
}