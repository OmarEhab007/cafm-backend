package com.cafm.cafmbackend.configuration.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.ZoneOffset;
import java.util.TimeZone;

/**
 * Jackson configuration for JSON serialization/deserialization.
 * 
 * Purpose: Fixes critical BigDecimal serialization issues and configures optimal JSON handling
 * Pattern: Centralized configuration with explicit settings for BigDecimal and datetime handling
 * Java 23: Leverages modern Jackson features with proper BigDecimal support
 * Architecture: Global configuration affecting all REST endpoints
 * Standards: Comprehensive JSON configuration addressing serialization edge cases
 */
@Configuration
public class JacksonConfig {
    
    /**
     * Primary ObjectMapper bean with comprehensive configuration.
     * 
     * Key fixes:
     * - USE_BIG_DECIMAL_FOR_FLOATS: Prevents BigDecimal serialization errors
     * - Proper numeric handling for GPS coordinates and financial fields
     * - UTC timezone handling for consistency
     * - Optimized settings for REST API usage
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // ========== CRITICAL BIGDECIMAL FIXES ==========
        
        // Fix for BigDecimal JSON serialization/deserialization
        mapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
        mapper.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);
        mapper.configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, false);
        
        // ========== SERIALIZATION CONFIGURATION ==========
        
        // Date/Time handling
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
        mapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
        
        // Number handling
        mapper.configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, true);
        mapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        
        // Property inclusion
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, true);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        
        // ========== DESERIALIZATION CONFIGURATION ==========
        
        // Ignore unknown properties (flexibility for API evolution)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
        
        // Handle missing/empty values gracefully
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        
        // ========== MODULES ==========
        
        // Java 8 time support
        mapper.registerModule(new JavaTimeModule());
        
        // Parameter names support for records
        mapper.registerModule(new ParameterNamesModule());
        
        // ========== TIMEZONE CONFIGURATION ==========
        
        // Set UTC as default timezone for consistent datetime handling
        mapper.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
        
        // ========== PERFORMANCE OPTIMIZATIONS ==========
        
        // Disable features that impact performance
        mapper.configure(DeserializationFeature.EAGER_DESERIALIZER_FETCH, true);
        mapper.configure(SerializationFeature.CLOSE_CLOSEABLE, false);
        
        return mapper;
    }
}