package com.cafm.cafmbackend.shared.enums;

/**
 * User status enumeration matching database user_status enum.
 * 
 * Architecture: Maps to PostgreSQL ENUM type created in V3__Unified_User_Management.sql
 * Pattern: Used for user lifecycle management and access control
 */
public enum UserStatus {
    PENDING_VERIFICATION("PENDING_VERIFICATION", "Awaiting email/phone verification"),
    ACTIVE("ACTIVE", "Active and verified user"),
    INACTIVE("INACTIVE", "Temporarily inactive"),
    SUSPENDED("SUSPENDED", "Account suspended by admin"),
    LOCKED("LOCKED", "Account locked due to security reasons"),
    ARCHIVED("ARCHIVED", "Archived user account");
    
    private final String dbValue;
    private final String description;
    
    UserStatus(String dbValue, String description) {
        this.dbValue = dbValue;
        this.description = description;
    }
    
    public String getDbValue() {
        return dbValue;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if user can login with this status
     */
    public boolean canLogin() {
        return this == ACTIVE;
    }
    
    /**
     * Check if this is a temporary status
     */
    public boolean isTemporary() {
        return this == PENDING_VERIFICATION || this == INACTIVE;
    }
    
    /**
     * Check if this status requires admin intervention
     */
    public boolean requiresAdminAction() {
        return this == SUSPENDED || this == LOCKED;
    }
    
    /**
     * Get UserStatus from database value
     */
    public static UserStatus fromDbValue(String dbValue) {
        for (UserStatus status : values()) {
            if (status.dbValue.equals(dbValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown user status: " + dbValue);
    }
}