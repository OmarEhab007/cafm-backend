package com.cafm.cafmbackend.infrastructure.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * JPA converter for PostgreSQL INET type to Java InetAddress.
 * 
 * Architecture: Handles conversion between PostgreSQL INET type and Java InetAddress
 * Pattern: Type-safe conversion for network addresses
 */
@Converter
public class InetAddressConverter implements AttributeConverter<InetAddress, String> {
    
    private static final Logger log = LoggerFactory.getLogger(InetAddressConverter.class);
    
    @Override
    public String convertToDatabaseColumn(InetAddress attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getHostAddress();
    }
    
    @Override
    public InetAddress convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        
        try {
            return InetAddress.getByName(dbData);
        } catch (UnknownHostException e) {
            log.error("Failed to convert string to InetAddress: {}", e.getMessage());
            return null;
        }
    }
}