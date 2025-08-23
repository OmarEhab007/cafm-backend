package com.cafm.cafmbackend.data.enums;

/**
 * Company status enumeration for multi-tenant architecture.
 * 
 * Architecture: Status lifecycle for company/tenant management
 * Pattern: State machine pattern for subscription lifecycle
 */
public enum CompanyStatus {
    
    /**
     * Company is active and fully operational
     */
    ACTIVE("ACTIVE", "Active", "Company is fully operational"),
    
    /**
     * Company account is inactive but not deleted
     */
    INACTIVE("INACTIVE", "Inactive", "Company account is temporarily disabled"),
    
    /**
     * Company is suspended due to policy violation or non-payment
     */
    SUSPENDED("SUSPENDED", "Suspended", "Company is suspended - limited access"),
    
    /**
     * Company is in trial period
     */
    TRIAL("TRIAL", "Trial", "Company is in trial period with limited features"),
    
    /**
     * Company setup is not yet complete
     */
    PENDING_SETUP("PENDING_SETUP", "Pending Setup", "Company registration incomplete");
    
    private final String dbValue;
    private final String displayName;
    private final String description;
    
    CompanyStatus(String dbValue, String displayName, String description) {
        this.dbValue = dbValue;
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDbValue() {
        return dbValue;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if status allows full access
     */
    public boolean allowsFullAccess() {
        return this == ACTIVE;
    }
    
    /**
     * Check if status allows limited access
     */
    public boolean allowsLimitedAccess() {
        return this == ACTIVE || this == TRIAL;
    }
    
    /**
     * Check if status blocks access
     */
    public boolean blocksAccess() {
        return this == SUSPENDED || this == INACTIVE;
    }
    
    /**
     * Check if status requires setup completion
     */
    public boolean requiresSetup() {
        return this == PENDING_SETUP;
    }
    
    /**
     * Get next valid statuses from current status
     */
    public CompanyStatus[] getValidTransitions() {
        return switch (this) {
            case PENDING_SETUP -> new CompanyStatus[]{ACTIVE, TRIAL, INACTIVE};
            case TRIAL -> new CompanyStatus[]{ACTIVE, SUSPENDED, INACTIVE};
            case ACTIVE -> new CompanyStatus[]{SUSPENDED, INACTIVE};
            case SUSPENDED -> new CompanyStatus[]{ACTIVE, INACTIVE};
            case INACTIVE -> new CompanyStatus[]{ACTIVE, TRIAL};
        };
    }
    
    /**
     * Check if transition to another status is valid
     */
    public boolean canTransitionTo(CompanyStatus newStatus) {
        CompanyStatus[] validTransitions = getValidTransitions();
        for (CompanyStatus validStatus : validTransitions) {
            if (validStatus == newStatus) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get CompanyStatus from database value
     */
    public static CompanyStatus fromDbValue(String dbValue) {
        for (CompanyStatus status : values()) {
            if (status.dbValue.equals(dbValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown company status: " + dbValue);
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}