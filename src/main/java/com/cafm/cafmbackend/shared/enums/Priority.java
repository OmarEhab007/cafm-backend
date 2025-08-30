package com.cafm.cafmbackend.shared.enums;

/**
 * General Priority enumeration for reports, tasks, and other items.
 * 
 * Purpose: Unified priority system across different entity types
 * Pattern: Enum with metadata and utility methods for priority management
 * Java 23: Uses enhanced switch expressions for priority calculations
 * Architecture: Data enum providing consistent priority definitions
 * Standards: Implements priority scoring and color coding for UI consistency
 */
public enum Priority {
    EMERGENCY("Emergency", "Immediate attention required", 1, "#FF0000", 4),
    HIGH("High", "Urgent - complete within 24 hours", 2, "#FF8C00", 24),
    MEDIUM("Medium", "Standard priority - complete within 3 days", 3, "#FFD700", 72),
    LOW("Low", "Non-urgent - complete within 7 days", 4, "#32CD32", 168),
    INFO("Info", "Informational only - no action required", 5, "#4169E1", null);
    
    private final String displayName;
    private final String description;
    private final int sortOrder;
    private final String colorCode;
    private final Integer responseTimeHours;
    
    Priority(String displayName, String description, int sortOrder, String colorCode, Integer responseTimeHours) {
        this.displayName = displayName;
        this.description = description;
        this.sortOrder = sortOrder;
        this.colorCode = colorCode;
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
    
    public String getColorCode() {
        return colorCode;
    }
    
    public Integer getResponseTimeHours() {
        return responseTimeHours;
    }
    
    /**
     * Check if this is critical priority requiring immediate attention
     */
    public boolean isCritical() {
        return this == EMERGENCY || this == HIGH;
    }
    
    /**
     * Check if this requires action
     */
    public boolean requiresAction() {
        return this != INFO;
    }
    
    /**
     * Get urgency score for sorting (lower = more urgent)
     */
    public int getUrgencyScore() {
        return sortOrder;
    }
    
    /**
     * Get icon name for UI display
     */
    public String getIconName() {
        return switch (this) {
            case EMERGENCY -> "alert-triangle";
            case HIGH -> "alert-circle";
            case MEDIUM -> "clock";
            case LOW -> "chevron-down";
            case INFO -> "info";
        };
    }
    
    /**
     * Get escalation priority (next level up)
     */
    public Priority escalate() {
        return switch (this) {
            case LOW -> MEDIUM;
            case MEDIUM -> HIGH;
            case HIGH, EMERGENCY -> EMERGENCY;
            case INFO -> LOW;
        };
    }
    
    /**
     * Create priority from string value
     */
    public static Priority fromString(String value) {
        if (value == null || value.isBlank()) return LOW;
        
        return switch (value.toUpperCase().trim()) {
            case "EMERGENCY", "URGENT", "CRITICAL" -> EMERGENCY;
            case "HIGH", "IMPORTANT" -> HIGH;
            case "MEDIUM", "NORMAL", "STANDARD" -> MEDIUM;
            case "LOW", "MINOR" -> LOW;
            case "INFO", "INFORMATIONAL", "NOTE" -> INFO;
            default -> {
                try {
                    yield valueOf(value.toUpperCase());
                } catch (IllegalArgumentException e) {
                    yield LOW; // Default fallback
                }
            }
        };
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
    public static Priority fromDatabaseValue(String value) {
        if (value == null) return LOW;
        return valueOf(value.toUpperCase());
    }
}