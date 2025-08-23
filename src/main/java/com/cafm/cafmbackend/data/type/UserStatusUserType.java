package com.cafm.cafmbackend.data.type;

import com.cafm.cafmbackend.data.enums.UserStatus;

/**
 * Hibernate UserType implementation for UserStatus enum with PostgreSQL enum support.
 * 
 * Purpose: Maps UserStatus enum to PostgreSQL user_status enum without @ColumnTransformer
 * Pattern: Concrete implementation of PostgreSQLEnumUserType for UserStatus enum
 * Java 23: Type-safe enum handling with modern Java features
 * Architecture: Database type mapping for user_status PostgreSQL enum
 * Standards: Hibernate UserType implementation following best practices
 * 
 * This replaces the problematic @ColumnTransformer(write = "?::user_status") approach
 * that was causing transaction commit failures.
 */
public class UserStatusUserType extends PostgreSQLEnumUserType<UserStatus> {
    
    public UserStatusUserType() {
        super(UserStatus.class, "user_status");
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