package com.cafm.cafmbackend.data.converter;

import com.cafm.cafmbackend.data.enums.UserType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for UserType enum to database value.
 * 
 * Architecture: Maps between Java enum and PostgreSQL enum type
 * Pattern: Explicit conversion for database compatibility
 */
@Converter(autoApply = true)
public class UserTypeConverter implements AttributeConverter<UserType, String> {
    
    @Override
    public String convertToDatabaseColumn(UserType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDbValue();
    }
    
    @Override
    public UserType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return UserType.fromDbValue(dbData);
    }
}