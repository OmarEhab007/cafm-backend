package com.cafm.cafmbackend.infrastructure.persistence.converter;

import jakarta.persistence.AttributeConverter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

/**
 * Base converter for PostgreSQL enum types.
 * 
 * Purpose: Provides base functionality for converting between Java enums and PostgreSQL enum types
 * Pattern: Template method pattern for enum conversion
 * Java 23: Uses modern generics and type system
 * Architecture: Data conversion layer for PostgreSQL specific types
 * Standards: JPA AttributeConverter with PostgreSQL PGobject handling
 */
public abstract class PostgreSQLEnumConverter<T extends Enum<T>> implements AttributeConverter<T, Object> {
    
    private final String pgEnumType;
    
    protected PostgreSQLEnumConverter(String pgEnumType) {
        this.pgEnumType = pgEnumType;
    }
    
    protected abstract String toDatabaseValue(T attribute);
    protected abstract T fromDatabaseValue(String dbData);
    
    @Override
    public Object convertToDatabaseColumn(T attribute) {
        if (attribute == null) {
            return null;
        }
        
        try {
            PGobject pgObject = new PGobject();
            pgObject.setType(pgEnumType);
            pgObject.setValue(toDatabaseValue(attribute));
            return pgObject;
        } catch (SQLException e) {
            throw new IllegalStateException("Error converting enum to PGobject", e);
        }
    }
    
    @Override
    public T convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return null;
        }
        
        String value;
        if (dbData instanceof PGobject pgObject) {
            value = pgObject.getValue();
        } else if (dbData instanceof String str) {
            value = str;
        } else {
            throw new IllegalArgumentException("Unable to convert database object of type " + 
                dbData.getClass().getName() + " to enum");
        }
        
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        return fromDatabaseValue(value);
    }
}