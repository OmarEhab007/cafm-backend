package com.cafm.cafmbackend.shared.enums;

/**
 * User type enumeration matching database user_type enum.
 * 
 * Architecture: Maps to PostgreSQL ENUM type created in V3__Unified_User_Management.sql
 * Pattern: Used for role-based access control and business logic branching
 */
public enum UserType {
    VIEWER("VIEWER", "Read-only access"),
    TECHNICIAN("TECHNICIAN", "Technician who performs maintenance work"),
    SUPERVISOR("SUPERVISOR", "Supervisor who manages schools and reports"),
    ADMIN("ADMIN", "Administrator with system management access"),
    SUPER_ADMIN("SUPER_ADMIN", "Super administrator with full system access");
    
    private final String dbValue;
    private final String description;
    
    UserType(String dbValue, String description) {
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
     * Check if this type has admin privileges
     */
    public boolean isAdmin() {
        return this == ADMIN || this == SUPER_ADMIN;
    }
    
    /**
     * Check if this type can manage reports
     */
    public boolean canManageReports() {
        return this == SUPERVISOR || isAdmin();
    }
    
    /**
     * Check if this type can perform maintenance
     */
    public boolean canPerformMaintenance() {
        return this == TECHNICIAN || this == SUPERVISOR || isAdmin();
    }
    
    /**
     * Get UserType from database value
     */
    public static UserType fromDbValue(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        // Try exact match first
        for (UserType type : values()) {
            if (type.dbValue.equals(dbValue)) {
                return type;
            }
        }
        // Try case-insensitive match for backward compatibility
        for (UserType type : values()) {
            if (type.dbValue.equalsIgnoreCase(dbValue)) {
                return type;
            }
        }
        // Try matching by enum name
        try {
            return UserType.valueOf(dbValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown user type: " + dbValue);
        }
    }
}