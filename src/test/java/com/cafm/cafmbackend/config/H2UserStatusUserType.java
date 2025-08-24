package com.cafm.cafmbackend.config;

import com.cafm.cafmbackend.data.enums.UserStatus;

/**
 * H2-compatible Hibernate UserType implementation for UserStatus enum.
 * 
 * Purpose: Maps UserStatus enum to VARCHAR in H2 for testing compatibility
 * Pattern: Concrete implementation of H2EnumUserType for UserStatus enum
 * Java 23: Type-safe enum handling with modern Java features
 * Architecture: Test infrastructure for user_status enum compatibility
 * Standards: Hibernate UserType implementation for H2 testing
 * 
 * This replaces PostgreSQL enum handling with simple VARCHAR storage in H2.
 */
public class H2UserStatusUserType extends H2EnumUserType<UserStatus> {
    
    public H2UserStatusUserType() {
        super(UserStatus.class);
    }
    
    @Override
    protected String convertToDatabaseValue(UserStatus enumValue) {
        return enumValue != null ? enumValue.getDbValue() : null;
    }
    
    @Override
    protected UserStatus convertToEnumValue(String databaseValue) {
        if (databaseValue == null || databaseValue.trim().isEmpty()) {
            return null;
        }
        
        try {
            return UserStatus.fromDbValue(databaseValue.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to convert database value '" + databaseValue + 
                "' to UserStatus enum", e);
        }
    }
}