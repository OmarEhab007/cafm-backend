package com.cafm.cafmbackend.data.converter;

import com.cafm.cafmbackend.data.enums.SubscriptionPlan;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for SubscriptionPlan enum to database value.
 * 
 * Purpose: Converts between Java SubscriptionPlan enum and PostgreSQL subscription_plan enum type
 * Pattern: Simple string conversion for PostgreSQL enum compatibility
 * Java 23: Uses modern AttributeConverter interface
 * Architecture: Maps between Java enum and PostgreSQL enum type string representation
 * Standards: JPA AttributeConverter with PostgreSQL enum compatibility
 */
@Converter(autoApply = true)
public class SubscriptionPlanConverter implements AttributeConverter<SubscriptionPlan, String> {
    
    @Override
    public String convertToDatabaseColumn(SubscriptionPlan attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDbValue();
    }
    
    @Override
    public SubscriptionPlan convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return SubscriptionPlan.fromDbValue(dbData);
    }
}