package com.cafm.cafmbackend.shared.enums;

/**
 * Asset Status enumeration for tracking asset lifecycle.
 * Represents the operational status of company assets.
 */
public enum AssetStatus {
    ACTIVE("Active", "Asset is in active use", "#28a745"),
    IN_USE("In Use", "Asset is currently being used", "#28a745"),
    MAINTENANCE("Under Maintenance", "Asset is being serviced or repaired", "#ffc107"),
    MAINTENANCE_REQUIRED("Maintenance Required", "Asset needs scheduled maintenance", "#ff9800"),
    RETIRED("Retired", "Asset is no longer in service but retained", "#6c757d"),
    DISPOSED("Disposed", "Asset has been sold or discarded", "#dc3545"),
    LOST("Lost", "Asset cannot be located", "#dc3545"),
    RESERVED("Reserved", "Asset is reserved for future use", "#17a2b8"),
    DAMAGED("Damaged", "Asset is damaged but repairable", "#fd7e14");
    
    private final String displayName;
    private final String description;
    private final String colorCode;
    
    AssetStatus(String displayName, String description, String colorCode) {
        this.displayName = displayName;
        this.description = description;
        this.colorCode = colorCode;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getColorCode() {
        return colorCode;
    }
    
    /**
     * Check if asset is operational
     */
    public boolean isOperational() {
        return this == ACTIVE || this == RESERVED;
    }
    
    /**
     * Check if asset needs attention
     */
    public boolean needsAttention() {
        return this == MAINTENANCE || this == DAMAGED;
    }
    
    /**
     * Check if asset is end-of-life
     */
    public boolean isEndOfLife() {
        return this == RETIRED || this == DISPOSED || this == LOST;
    }
    
    /**
     * Check if asset can be assigned
     */
    public boolean canBeAssigned() {
        return this == ACTIVE || this == RESERVED;
    }
    
    /**
     * Check if asset can be maintained
     */
    public boolean canBeMaintained() {
        return this == ACTIVE || this == DAMAGED;
    }
    
    /**
     * Get icon name for UI
     */
    public String getIconName() {
        switch (this) {
            case ACTIVE:
                return "check-circle";
            case MAINTENANCE:
                return "tool";
            case RETIRED:
                return "archive";
            case DISPOSED:
                return "trash-2";
            case LOST:
                return "help-circle";
            case RESERVED:
                return "bookmark";
            case DAMAGED:
                return "alert-triangle";
            default:
                return "package";
        }
    }
}