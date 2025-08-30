package com.cafm.cafmbackend.infrastructure.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA converter for PostgreSQL JSONB arrays to Java List.
 * 
 * Architecture: Handles conversion between JSONB array columns and Java Lists
 * Pattern: Generic converter for array-type JSONB data
 */
@Converter
public class JsonArrayConverter implements AttributeConverter<List<Object>, String> {
    
    private static final Logger log = LoggerFactory.getLogger(JsonArrayConverter.class);
    private final ObjectMapper objectMapper;
    
    public JsonArrayConverter() {
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String convertToDatabaseColumn(List<Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert list to JSON: {}", e.getMessage());
            throw new IllegalArgumentException("Error converting list to JSON", e);
        }
    }
    
    @Override
    public List<Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty() || "[]".equals(dbData)) {
            return new ArrayList<>();
        }
        
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<Object>>() {});
        } catch (IOException e) {
            log.error("Failed to convert JSON to list: {}", e.getMessage());
            throw new IllegalArgumentException("Error converting JSON to list", e);
        }
    }
}