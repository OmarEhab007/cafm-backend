package com.cafm.cafmbackend.config;

import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Validation configuration for the application.
 * 
 * Architecture: Configures Bean Validation with custom validators
 * Pattern: Centralized validation configuration
 */
@Configuration
public class ValidationConfig {
    
    /**
     * Message source for validation messages.
     * Supports internationalization (i18n) for Arabic/English.
     */
    @Bean("validationMessageSource")
    public MessageSource validationMessageSource() {
        ReloadableResourceBundleMessageSource messageSource = 
            new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages/validation");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(3600);
        return messageSource;
    }
    
    /**
     * Validator factory bean with custom message source.
     */
    @Bean
    public LocalValidatorFactoryBean validator(@Qualifier("validationMessageSource") MessageSource validationMessageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(validationMessageSource);
        return bean;
    }
    
    /**
     * Default validator bean.
     */
    @Bean
    public Validator getValidator(LocalValidatorFactoryBean factory) {
        return factory;
    }
}