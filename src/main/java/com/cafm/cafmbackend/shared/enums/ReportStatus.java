package com.cafm.cafmbackend.shared.enums;

/**
 * Report status enumeration for work order lifecycle.
 * 
 * Architecture: Maps to database report_status enum
 * Pattern: State machine for report workflow
 */
public enum ReportStatus {
    DRAFT("draft", "Initial creation state"),
    SUBMITTED("submitted", "Submitted for review"),
    IN_REVIEW("in_review", "Under review"),
    APPROVED("approved", "Approved for work"),
    REJECTED("rejected", "Rejected, needs revision"),
    IN_PROGRESS("in_progress", "Work in progress"),
    PENDING("pending", "On hold"),
    COMPLETED("completed", "Work completed"),
    RESOLVED("resolved", "Issue resolved"),
    CLOSED("closed", "Report closed"),
    CANCELLED("cancelled", "Cancelled"),
    LATE("late", "Past due date"),
    LATE_COMPLETED("late_completed", "Completed after due date");
    
    private final String dbValue;
    private final String description;
    
    ReportStatus(String dbValue, String description) {
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
     * Check if report can be edited in this status
     */
    public boolean isEditable() {
        return this == DRAFT || this == REJECTED;
    }
    
    /**
     * Check if report is in active work state
     */
    public boolean isActive() {
        return this == IN_PROGRESS || this == PENDING || this == LATE;
    }
    
    /**
     * Check if report is in final state
     */
    public boolean isFinal() {
        return this == COMPLETED || this == RESOLVED || this == CLOSED || 
               this == CANCELLED || this == LATE_COMPLETED;
    }
    
    /**
     * Check if report is overdue
     */
    public boolean isOverdue() {
        return this == LATE || this == LATE_COMPLETED;
    }
}