package com.cafm.cafmbackend.data.converter;

import com.cafm.cafmbackend.data.enums.ReportStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for ReportStatus enum.
 * 
 * Purpose: Maps between Java enum names and database string values
 * Pattern: Custom AttributeConverter for enum mapping
 * Java 23: Standard JPA converter implementation
 * Architecture: Data layer converter for enum persistence
 * Standards: Handles null values and provides error logging
 */
@Converter(autoApply = true)
public class ReportStatusConverter implements AttributeConverter<ReportStatus, String> {

    @Override
    public String convertToDatabaseColumn(ReportStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDbValue();
    }

    @Override
    public ReportStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        
        // First, try to match by dbValue (preferred)
        for (ReportStatus status : ReportStatus.values()) {
            if (status.getDbValue().equals(dbData)) {
                return status;
            }
        }
        
        // Then, try to match by enum name (for backward compatibility with existing data)
        for (ReportStatus status : ReportStatus.values()) {
            if (status.name().equals(dbData)) {
                return status;
            }
        }
        
        throw new IllegalArgumentException("Unknown database value for ReportStatus: " + dbData);
    }
}