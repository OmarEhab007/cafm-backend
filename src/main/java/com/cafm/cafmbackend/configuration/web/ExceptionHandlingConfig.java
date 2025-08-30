package com.cafm.cafmbackend.configuration.web;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;
import java.util.Arrays;

/**
 * Configuration for exception handling and internationalization.
 * Ensures proper setup of error message resolution and localization.
 * 
 * Architecture: Spring configuration for exception handling system
 * Pattern: Configuration class with bean definitions
 * Java 23: Modern configuration with locale support
 * Security: Ensures proper error message sanitization
 * Standards: I18n configuration for multi-language error support
 */
@Configuration
public class ExceptionHandlingConfig {
    
    /**
     * Configure message source for error message internationalization.
     * Supports Arabic and English error messages.
     * 
     * @return MessageSource configured for validation and error messages
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        
        // Set base names for message bundles
        messageSource.setBasenames(
            "messages/validation",           // validation.properties, validation_ar.properties
            "messages/errors"                // errors.properties, errors_ar.properties (if created later)
        );
        
        // Set encoding to handle Arabic text properly
        messageSource.setDefaultEncoding("UTF-8");
        
        // Use code as default message if message not found
        messageSource.setUseCodeAsDefaultMessage(false);
        
        // Set default locale
        messageSource.setDefaultLocale(Locale.ENGLISH);
        
        // Enable caching for performance (disable in dev if needed)
        messageSource.setCacheSeconds(3600); // Cache for 1 hour
        
        return messageSource;
    }
    
    /**
     * Configure locale resolver to detect user language from Accept-Language header.
     * Supports Arabic (ar) and English (en) locales.
     * 
     * @return LocaleResolver that detects locale from HTTP headers
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        
        // Set supported locales
        localeResolver.setSupportedLocales(Arrays.asList(
            Locale.ENGLISH,          // en
            new Locale("ar")         // ar (Arabic)
        ));
        
        // Set default locale if none specified
        localeResolver.setDefaultLocale(Locale.ENGLISH);
        
        return localeResolver;
    }
}