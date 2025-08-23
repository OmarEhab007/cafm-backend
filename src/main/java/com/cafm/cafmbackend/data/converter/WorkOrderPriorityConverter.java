package com.cafm.cafmbackend.data.converter;

import com.cafm.cafmbackend.data.enums.WorkOrderPriority;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for WorkOrderPriority enum to database value.
 * 
 * Purpose: Maps between Java WorkOrderPriority enum and PostgreSQL work_order_priority type
 * Pattern: Explicit conversion using uppercase database values for consistency
 * Java 23: Compatible with pattern matching in future query enhancements
 * Architecture: Ensures proper enum handling at data layer
 * Standards: Follows JPA AttributeConverter pattern for type safety
 */
@Converter(autoApply = true)
public class WorkOrderPriorityConverter implements AttributeConverter<WorkOrderPriority, String> {
    
    @Override
    public String convertToDatabaseColumn(WorkOrderPriority attribute) {
        if (attribute == null) {
            return null;
        }
        // Convert to uppercase for database enum
        return attribute.name();
    }
    
    @Override
    public WorkOrderPriority convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        // Handle both uppercase and lowercase from database
        return WorkOrderPriority.valueOf(dbData.toUpperCase());
    }
}