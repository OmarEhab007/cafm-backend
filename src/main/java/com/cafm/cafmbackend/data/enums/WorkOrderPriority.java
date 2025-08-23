package com.cafm.cafmbackend.data.enums;

/**
 * Work Order Priority enumeration for scheduling and resource allocation.
 * Maps to PostgreSQL enum type 'work_order_priority'.
 */
public enum WorkOrderPriority {
    EMERGENCY("Emergency", "Immediate response required", 1, 4),
    HIGH("High", "Urgent - complete within 24 hours", 2, 24),
    MEDIUM("Medium", "Standard priority - complete within 3 days", 3, 72),
    LOW("Low", "Non-urgent - complete within 7 days", 4, 168),
    SCHEDULED("Scheduled", "Planned maintenance - as per schedule", 5, null);
    
    private final String displayName;
    private final String description;
    private final int sortOrder;
    private final Integer responseTimeHours;
    
    WorkOrderPriority(String displayName, String description, int sortOrder, Integer responseTimeHours) {
        this.displayName = displayName;
        this.description = description;
        this.sortOrder = sortOrder;
        this.responseTimeHours = responseTimeHours;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getSortOrder() {
        return sortOrder;
    }
    
    public Integer getResponseTimeHours() {
        return responseTimeHours;
    }
    
    /**
     * Check if this is critical priority
     */
    public boolean isCritical() {
        return this == EMERGENCY || this == HIGH;
    }
    
    /**
     * Get color code for UI display
     */
    public String getColorCode() {
        switch (this) {
            case EMERGENCY:
                return "#FF0000"; // Red
            case HIGH:
                return "#FF8C00"; // Dark Orange
            case MEDIUM:
                return "#FFD700"; // Gold
            case LOW:
                return "#32CD32"; // Lime Green
            case SCHEDULED:
                return "#4169E1"; // Royal Blue
            default:
                return "#808080"; // Gray
        }
    }
    
    /**
     * Get database value for PostgreSQL enum
     */
    public String toDatabaseValue() {
        return name().toLowerCase();
    }
    
    /**
     * Parse from database value
     */
    public static WorkOrderPriority fromDatabaseValue(String value) {
        if (value == null) return null;
        return valueOf(value.toUpperCase());
    }
}