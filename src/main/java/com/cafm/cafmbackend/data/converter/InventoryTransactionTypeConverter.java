package com.cafm.cafmbackend.data.converter;

import com.cafm.cafmbackend.data.enums.InventoryTransactionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for InventoryTransactionType enum to database value.
 * 
 * Purpose: Maps between Java InventoryTransactionType enum and PostgreSQL inventory_transaction_type
 * Pattern: Explicit conversion using uppercase database values for consistency
 * Java 23: Compatible with pattern matching in future query enhancements
 * Architecture: Ensures proper enum handling at data layer
 * Standards: Follows JPA AttributeConverter pattern for type safety
 */
@Converter(autoApply = true)
public class InventoryTransactionTypeConverter implements AttributeConverter<InventoryTransactionType, String> {
    
    @Override
    public String convertToDatabaseColumn(InventoryTransactionType attribute) {
        if (attribute == null) {
            return null;
        }
        // Convert to uppercase for database enum
        return attribute.name();
    }
    
    @Override
    public InventoryTransactionType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        // Handle both uppercase and lowercase from database
        return InventoryTransactionType.valueOf(dbData.toUpperCase());
    }
}