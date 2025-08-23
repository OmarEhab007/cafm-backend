package com.cafm.cafmbackend.data.type;

import com.cafm.cafmbackend.data.enums.UserType;

/**
 * Hibernate UserType implementation for UserType enum with PostgreSQL enum support.
 * 
 * Purpose: Maps UserType enum to PostgreSQL user_type enum without @ColumnTransformer
 * Pattern: Concrete implementation of PostgreSQLEnumUserType for UserType enum
 * Java 23: Type-safe enum handling with modern Java features
 * Architecture: Database type mapping for user_type PostgreSQL enum
 * Standards: Hibernate UserType implementation following best practices
 * 
 * This replaces the problematic @ColumnTransformer(write = "?::user_type") approach
 * that was causing transaction commit failures.
 */
public class UserTypeUserType extends PostgreSQLEnumUserType<UserType> {
    
    public UserTypeUserType() {
        super(UserType.class, "user_type");
    }
    
    @Override
    protected String convertToDatabaseValue(UserType enumValue) {
        return enumValue != null ? enumValue.getDbValue() : null;
    }
    
    @Override
    protected UserType convertToEnumValue(String databaseValue) {
        if (databaseValue == null || databaseValue.trim().isEmpty()) {
            return null;
        }
        
        try {
            return UserType.fromDbValue(databaseValue.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to convert database value '" + databaseValue + 
                "' to UserType enum", e);
        }
    }
}