package com.cafm.cafmbackend.shared.enums;

/**
 * Enumeration for different types of notifications.
 * 
 * Purpose: Categorizes notification types for proper handling and display
 * Pattern: Simple enum with descriptive values for notification classification
 * Java 23: Standard enum pattern for type safety
 * Architecture: Data layer enum used across notification domain
 * Standards: Clear naming convention for notification categorization
 */
public enum NotificationType {
    
    /**
     * Work order related notifications.
     */
    WORK_ORDER_ASSIGNED("Work Order Assigned"),
    WORK_ORDER_COMPLETED("Work Order Completed"),
    WORK_ORDER_CANCELLED("Work Order Cancelled"),
    WORK_ORDER_OVERDUE("Work Order Overdue"),
    
    /**
     * Report related notifications.
     */
    REPORT_SUBMITTED("Report Submitted"),
    REPORT_STATUS_UPDATE("Report Status Updated"),
    REPORT_APPROVED("Report Approved"),
    REPORT_REJECTED("Report Rejected"),
    
    /**
     * Urgent and alert notifications.
     */
    URGENT_ALERT("Urgent Maintenance Alert"),
    SAFETY_ALERT("Safety Alert"),
    EMERGENCY_NOTIFICATION("Emergency Notification"),
    
    /**
     * System and administrative notifications.
     */
    SYSTEM_MAINTENANCE("System Maintenance"),
    DAILY_SUMMARY("Daily Summary"),
    WEEKLY_REPORT("Weekly Report"),
    REMINDER("Reminder"),
    
    /**
     * User and account notifications.
     */
    ACCOUNT_UPDATE("Account Update"),
    PASSWORD_RESET("Password Reset"),
    LOGIN_ALERT("Login Alert"),
    
    /**
     * General informational notifications.
     */
    GENERAL_INFO("General Information"),
    ANNOUNCEMENT("Announcement"),
    UPDATE_AVAILABLE("Update Available");
    
    private final String displayName;
    
    NotificationType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if this notification type is considered urgent.
     */
    public boolean isUrgent() {
        return switch (this) {
            case URGENT_ALERT, SAFETY_ALERT, EMERGENCY_NOTIFICATION, WORK_ORDER_OVERDUE -> true;
            default -> false;
        };
    }
    
    /**
     * Check if this notification type requires immediate action.
     */
    public boolean requiresAction() {
        return switch (this) {
            case WORK_ORDER_ASSIGNED, WORK_ORDER_OVERDUE, URGENT_ALERT, 
                 SAFETY_ALERT, EMERGENCY_NOTIFICATION, PASSWORD_RESET -> true;
            default -> false;
        };
    }
}