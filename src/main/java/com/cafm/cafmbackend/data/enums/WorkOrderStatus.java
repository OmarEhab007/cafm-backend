package com.cafm.cafmbackend.data.enums;

/**
 * Work Order Status enumeration for tracking work order lifecycle.
 * Maps to PostgreSQL enum type 'work_order_status'.
 */
public enum WorkOrderStatus {
    DRAFT("Draft", "Work order created but not yet submitted"),
    PENDING("Pending", "Awaiting assignment or approval"),
    ASSIGNED("Assigned", "Assigned to technician but not started"),
    IN_PROGRESS("In Progress", "Work is currently being performed"),
    ON_HOLD("On Hold", "Work temporarily suspended"),
    COMPLETED("Completed", "Work finished and awaiting verification"),
    CANCELLED("Cancelled", "Work order cancelled"),
    VERIFIED("Verified", "Work completed and verified by supervisor");
    
    private final String displayName;
    private final String description;
    
    WorkOrderStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if work order can be edited in this status
     */
    public boolean isEditable() {
        return this == DRAFT || this == PENDING || this == ASSIGNED;
    }
    
    /**
     * Check if work order is in active state
     */
    public boolean isActive() {
        return this == ASSIGNED || this == IN_PROGRESS || this == ON_HOLD;
    }
    
    /**
     * Check if work order is in final state
     */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED || this == VERIFIED;
    }
    
    /**
     * Check if technician can work on this status
     */
    public boolean canStartWork() {
        return this == ASSIGNED || this == ON_HOLD;
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
    public static WorkOrderStatus fromDatabaseValue(String value) {
        if (value == null) return null;
        return valueOf(value.toUpperCase());
    }
}