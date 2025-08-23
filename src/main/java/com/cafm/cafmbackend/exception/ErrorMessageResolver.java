package com.cafm.cafmbackend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Resolves error messages with internationalization support.
 * Provides localized error messages for Arabic and English users.
 * 
 * Architecture: Message resolution service with i18n support
 * Pattern: Service layer component for message localization
 * Java 23: Modern string handling and locale processing
 * Security: No sensitive information in messages, safe fallbacks
 * Standards: Spring MessageSource integration with validation messages
 */
@Component
public class ErrorMessageResolver {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorMessageResolver.class);
    
    private final MessageSource messageSource;
    
    public ErrorMessageResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }
    
    /**
     * Get localized error message with fallback.
     * 
     * @param key The message key to look up
     * @param defaultMessage Default message if key not found
     * @param locale The target locale
     * @param args Optional message arguments for parameterized messages
     * @return Localized error message
     */
    public String getMessage(String key, String defaultMessage, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(key, args, locale);
        } catch (NoSuchMessageException e) {
            logger.debug("Message key '{}' not found for locale '{}', using default message", key, locale);
            return defaultMessage;
        } catch (Exception e) {
            logger.warn("Error resolving message for key '{}' and locale '{}', using default", key, locale, e);
            return defaultMessage;
        }
    }
    
    /**
     * Get localized error message with default English fallback.
     * 
     * @param key The message key to look up
     * @param defaultMessage Default message if key not found
     * @param locale The target locale
     * @return Localized error message
     */
    public String getMessage(String key, String defaultMessage, Locale locale) {
        return getMessage(key, defaultMessage, locale, (Object[]) null);
    }
    
    /**
     * Get validation error message with field context.
     * 
     * @param fieldName The field name that failed validation
     * @param key The validation message key
     * @param defaultMessage Default message if key not found
     * @param locale The target locale
     * @return Localized validation message
     */
    public String getValidationMessage(String fieldName, String key, String defaultMessage, Locale locale) {
        // First try field-specific message
        String fieldSpecificKey = String.format("validation.%s.%s", fieldName, key);
        try {
            return messageSource.getMessage(fieldSpecificKey, null, locale);
        } catch (NoSuchMessageException e) {
            // Fall back to generic validation message
            return getMessage(key, defaultMessage, locale);
        }
    }
    
    /**
     * Get business rule error message with context.
     * 
     * @param businessRule The business rule that was violated
     * @param entityType The type of entity involved
     * @param locale The target locale
     * @param args Optional message arguments
     * @return Localized business rule message
     */
    public String getBusinessRuleMessage(String businessRule, String entityType, Locale locale, Object... args) {
        // Try business rule specific message
        String businessRuleKey = String.format("business.rule.%s.%s", entityType.toLowerCase(), businessRule.toLowerCase());
        String defaultMessage = String.format("Business rule violation: %s", businessRule);
        
        return getMessage(businessRuleKey, defaultMessage, locale, args);
    }
    
    /**
     * Get security error message (always sanitized).
     * 
     * @param securityEvent The type of security event
     * @param locale The target locale
     * @return Sanitized localized security message
     */
    public String getSecurityMessage(String securityEvent, Locale locale) {
        String key = String.format("security.%s", securityEvent.toLowerCase());
        
        // Default security messages are intentionally generic
        String defaultMessage = switch (securityEvent.toLowerCase()) {
            case "authentication_failed" -> "Authentication failed";
            case "access_denied" -> "Access denied";
            case "tenant_violation" -> "Access denied";
            case "token_expired" -> "Session expired";
            case "invalid_token" -> "Invalid session";
            default -> "Security error";
        };
        
        return getMessage(key, defaultMessage, locale);
    }
    
    /**
     * Get constraint violation message with enhanced context.
     * 
     * @param constraintType The type of constraint that was violated
     * @param fieldName The field name involved
     * @param locale The target locale
     * @param args Optional constraint parameters (e.g., min, max values)
     * @return Localized constraint message
     */
    public String getConstraintMessage(String constraintType, String fieldName, Locale locale, Object... args) {
        // Try constraint-specific message
        String constraintKey = String.format("constraint.%s", constraintType.toLowerCase());
        
        String defaultMessage = switch (constraintType.toLowerCase()) {
            case "notnull" -> String.format("Field '%s' is required", fieldName);
            case "notblank" -> String.format("Field '%s' cannot be blank", fieldName);
            case "size" -> String.format("Field '%s' size is invalid", fieldName);
            case "email" -> String.format("Field '%s' must be a valid email", fieldName);
            case "min" -> String.format("Field '%s' value is too small", fieldName);
            case "max" -> String.format("Field '%s' value is too large", fieldName);
            case "pattern" -> String.format("Field '%s' format is invalid", fieldName);
            default -> String.format("Field '%s' validation failed", fieldName);
        };
        
        return getMessage(constraintKey, defaultMessage, locale, args);
    }
    
    /**
     * Get HTTP error message for client-side errors.
     * 
     * @param httpStatus The HTTP status code
     * @param locale The target locale
     * @return Localized HTTP error message
     */
    public String getHttpErrorMessage(int httpStatus, Locale locale) {
        String key = String.format("http.error.%d", httpStatus);
        
        String defaultMessage = switch (httpStatus) {
            case 400 -> "Bad request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not found";
            case 405 -> "Method not allowed";
            case 409 -> "Conflict";
            case 422 -> "Unprocessable entity";
            case 429 -> "Too many requests";
            case 500 -> "Internal server error";
            case 502 -> "Bad gateway";
            case 503 -> "Service unavailable";
            case 504 -> "Gateway timeout";
            default -> "Error";
        };
        
        return getMessage(key, defaultMessage, locale);
    }
    
    /**
     * Detect if locale is Arabic for RTL text support.
     * 
     * @param locale The locale to check
     * @return true if Arabic locale
     */
    public boolean isArabicLocale(Locale locale) {
        return locale != null && "ar".equals(locale.getLanguage());
    }
    
    /**
     * Get safe locale with fallback to English.
     * 
     * @param locale The requested locale
     * @return Safe locale (English if null or invalid)
     */
    public Locale getSafeLocale(Locale locale) {
        if (locale == null) {
            return Locale.ENGLISH;
        }
        
        // Only support Arabic and English for now
        if ("ar".equals(locale.getLanguage())) {
            return new Locale("ar");
        }
        
        return Locale.ENGLISH;
    }
}