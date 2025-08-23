package com.cafm.cafmbackend.data.converter;

import com.cafm.cafmbackend.data.enums.WorkOrderStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for WorkOrderStatus enum to database value.
 * 
 * Purpose: Maps between Java WorkOrderStatus enum and PostgreSQL work_order_status type
 * Pattern: Explicit conversion using uppercase database values for consistency
 * Java 23: Compatible with pattern matching in future query enhancements
 * Architecture: Ensures proper enum handling at data layer
 * Standards: Follows JPA AttributeConverter pattern for type safety
 */
@Converter(autoApply = true)
public class WorkOrderStatusConverter implements AttributeConverter<WorkOrderStatus, String> {
    
    @Override
    public String convertToDatabaseColumn(WorkOrderStatus attribute) {
        if (attribute == null) {
            return null;
        }
        // Convert to uppercase for database enum
        return attribute.name();
    }
    
    @Override
    public WorkOrderStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        // Handle both uppercase and lowercase from database
        return WorkOrderStatus.valueOf(dbData.toUpperCase());
    }
}