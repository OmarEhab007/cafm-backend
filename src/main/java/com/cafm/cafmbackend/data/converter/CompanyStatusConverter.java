package com.cafm.cafmbackend.data.converter;

import com.cafm.cafmbackend.data.enums.CompanyStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for CompanyStatus enum to database value.
 * 
 * Purpose: Converts between Java CompanyStatus enum and PostgreSQL company_status enum type
 * Pattern: Simple string conversion for PostgreSQL enum compatibility
 * Java 23: Uses modern AttributeConverter interface
 * Architecture: Maps between Java enum and PostgreSQL enum type string representation
 * Standards: JPA AttributeConverter with PostgreSQL enum compatibility
 */
@Converter(autoApply = true)
public class CompanyStatusConverter implements AttributeConverter<CompanyStatus, String> {
    
    @Override
    public String convertToDatabaseColumn(CompanyStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDbValue();
    }
    
    @Override
    public CompanyStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return CompanyStatus.fromDbValue(dbData);
    }
}