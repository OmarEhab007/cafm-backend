package com.cafm.cafmbackend.shared.enums;

/**
 * Report priority enumeration for work order urgency.
 * 
 * Architecture: Maps to database report_priority enum
 * Pattern: Used for SLA management and scheduling
 */
public enum ReportPriority {
    LOW("low", "Low priority", 30),
    MEDIUM("medium", "Medium priority", 14),
    HIGH("high", "High priority", 7),
    URGENT("urgent", "Urgent priority", 2),
    CRITICAL("critical", "Critical priority", 1);
    
    private final String dbValue;
    private final String description;
    private final int slaInDays;
    
    ReportPriority(String dbValue, String description, int slaInDays) {
        this.dbValue = dbValue;
        this.description = description;
        this.slaInDays = slaInDays;
    }
    
    public String getDbValue() {
        return dbValue;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getSlaInDays() {
        return slaInDays;
    }
    
    /**
     * Check if this priority requires immediate attention
     */
    public boolean isImmediate() {
        return this == URGENT || this == CRITICAL;
    }
}