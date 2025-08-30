package com.cafm.cafmbackend.shared.exception;

/**
 * Exception thrown when business logic rules are violated.
 * Represents application-specific constraints and workflow violations.
 * 
 * Architecture: Domain-specific exception for business rule enforcement
 * Pattern: Custom runtime exception with business context
 * Java 23: Modern exception handling with factory methods
 * Standards: Follows business logic layer exception patterns
 */
public class BusinessLogicException extends RuntimeException {
    
    private final String businessRule;
    private final String entityType;
    private final String entityId;
    
    /**
     * Create a business logic exception with basic information.
     * 
     * @param message The business rule violation description
     */
    public BusinessLogicException(String message) {
        super(message);
        this.businessRule = null;
        this.entityType = null;
        this.entityId = null;
    }
    
    /**
     * Create a business logic exception with business rule context.
     * 
     * @param message The violation description
     * @param businessRule The specific business rule that was violated
     */
    public BusinessLogicException(String message, String businessRule) {
        super(message);
        this.businessRule = businessRule;
        this.entityType = null;
        this.entityId = null;
    }
    
    /**
     * Create a business logic exception with full context.
     * 
     * @param message The violation description
     * @param businessRule The specific business rule that was violated
     * @param entityType The type of entity involved in the violation
     * @param entityId The ID of the entity involved
     */
    public BusinessLogicException(String message, String businessRule, String entityType, String entityId) {
        super(message);
        this.businessRule = businessRule;
        this.entityType = entityType;
        this.entityId = entityId;
    }
    
    /**
     * Create a business logic exception with cause.
     * 
     * @param message The violation description
     * @param cause The underlying cause
     */
    public BusinessLogicException(String message, Throwable cause) {
        super(message, cause);
        this.businessRule = null;
        this.entityType = null;
        this.entityId = null;
    }
    
    /**
     * Factory method for workflow state violations.
     * 
     * @param entityType The type of entity
     * @param entityId The entity identifier
     * @param currentState The current state
     * @param requiredState The required state for the operation
     * @return BusinessLogicException instance
     */
    public static BusinessLogicException invalidState(String entityType, String entityId, 
                                                     String currentState, String requiredState) {
        String message = String.format("%s '%s' is in state '%s' but operation requires state '%s'", 
            entityType, entityId, currentState, requiredState);
        return new BusinessLogicException(message, "INVALID_OPERATION_STATE", entityType, entityId);
    }
    
    /**
     * Factory method for work order assignment violations.
     * 
     * @param workOrderId The work order identifier
     * @param reason The reason assignment failed
     * @return BusinessLogicException instance
     */
    public static BusinessLogicException workOrderAssignmentFailed(String workOrderId, String reason) {
        String message = String.format("Cannot assign work order '%s': %s", workOrderId, reason);
        return new BusinessLogicException(message, "WORK_ORDER_ASSIGNMENT_FAILED", "WorkOrder", workOrderId);
    }
    
    /**
     * Factory method for inventory shortage violations.
     * 
     * @param itemId The inventory item identifier
     * @param requested The requested quantity
     * @param available The available quantity
     * @return BusinessLogicException instance
     */
    public static BusinessLogicException insufficientInventory(String itemId, int requested, int available) {
        String message = String.format("Insufficient inventory for item '%s': requested %d, available %d", 
            itemId, requested, available);
        return new BusinessLogicException(message, "INSUFFICIENT_INVENTORY", "InventoryItem", itemId);
    }
    
    /**
     * Factory method for date range violations.
     * 
     * @param startDate The start date
     * @param endDate The end date
     * @return BusinessLogicException instance
     */
    public static BusinessLogicException invalidDateRange(String startDate, String endDate) {
        String message = String.format("Invalid date range: start date '%s' must be before end date '%s'", 
            startDate, endDate);
        return new BusinessLogicException(message, "INVALID_DATE_RANGE");
    }
    
    /**
     * Factory method for workflow constraint violations.
     * 
     * @param workflow The workflow name
     * @param constraint The constraint that was violated
     * @param entityType The entity type
     * @param entityId The entity identifier
     * @return BusinessLogicException instance
     */
    public static BusinessLogicException workflowConstraintViolation(String workflow, String constraint, 
                                                                   String entityType, String entityId) {
        String message = String.format("Workflow '%s' constraint violated: %s for %s '%s'", 
            workflow, constraint, entityType, entityId);
        return new BusinessLogicException(message, "WORKFLOW_CONSTRAINT_VIOLATION", entityType, entityId);
    }
    
    /**
     * Factory method for authorization business rules.
     * 
     * @param operation The operation being attempted
     * @param reason The business reason for denial
     * @return BusinessLogicException instance
     */
    public static BusinessLogicException operationNotAllowed(String operation, String reason) {
        String message = String.format("Operation '%s' not allowed: %s", operation, reason);
        return new BusinessLogicException(message, "OPERATION_NOT_ALLOWED");
    }
    
    public String getBusinessRule() {
        return businessRule;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public String getEntityId() {
        return entityId;
    }
}