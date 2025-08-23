package com.cafm.cafmbackend.data.converter;

import com.cafm.cafmbackend.data.enums.UserStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for UserStatus enum to database value.
 * 
 * Purpose: Converts between Java UserStatus enum and PostgreSQL user_status enum type
 * Pattern: Simple string conversion for PostgreSQL enum compatibility
 * Java 23: Uses modern AttributeConverter interface
 * Architecture: Maps between Java enum and PostgreSQL enum type string representation
 * Standards: JPA AttributeConverter with PostgreSQL enum compatibility
 */
@Converter(autoApply = true)
public class UserStatusConverter implements AttributeConverter<UserStatus, String> {
    
    @Override
    public String convertToDatabaseColumn(UserStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDbValue();
    }
    
    @Override
    public UserStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return UserStatus.fromDbValue(dbData);
    }
}