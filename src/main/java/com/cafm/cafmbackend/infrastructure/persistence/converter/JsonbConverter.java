package com.cafm.cafmbackend.infrastructure.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * JPA converter for PostgreSQL JSONB type to Java Map.
 * 
 * Architecture: Handles conversion between JSONB columns and Java objects
 * Pattern: Attribute converter pattern for transparent serialization
 * Java 23: Ready for pattern matching in error handling
 */
@Converter
public class JsonbConverter implements AttributeConverter<Map<String, Object>, String> {
    
    private static final Logger log = LoggerFactory.getLogger(JsonbConverter.class);
    private final ObjectMapper objectMapper;
    
    public JsonbConverter() {
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }
        
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert map to JSON: {}", e.getMessage());
            throw new IllegalArgumentException("Error converting map to JSON", e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty() || "{}".equals(dbData)) {
            return Map.of();
        }
        
        try {
            return objectMapper.readValue(dbData, Map.class);
        } catch (IOException e) {
            log.error("Failed to convert JSON to map: {}", e.getMessage());
            throw new IllegalArgumentException("Error converting JSON to map", e);
        }
    }
}