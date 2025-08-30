package com.cafm.cafmbackend.shared.exception;

import java.util.UUID;

/**
 * Exception thrown when a resource conflict occurs.
 * 
 * Purpose: Signals resource state conflicts like duplicate creation or concurrent modification
 * Pattern: Domain-specific exception for resource state management
 * Java 23: Enhanced exception with resource context
 * Architecture: Resource conflict management exception
 * Standards: Clear conflict identification with resolution hints
 */
public class ResourceConflictException extends RuntimeException {

    private final String resourceType;
    private final UUID resourceId;
    private final String conflictType;

    public ResourceConflictException(String message, String resourceType, UUID resourceId, String conflictType) {
        super(message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.conflictType = conflictType;
    }

    public ResourceConflictException(String message, Throwable cause, String resourceType, UUID resourceId, String conflictType) {
        super(message, cause);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.conflictType = conflictType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getConflictType() {
        return conflictType;
    }

    public static ResourceConflictException duplicateResource(String resourceType, String identifier) {
        String message = String.format("%s already exists with identifier: %s", resourceType, identifier);
        return new ResourceConflictException(message, resourceType, null, "DUPLICATE");
    }

    public static ResourceConflictException concurrentModification(String resourceType, UUID resourceId) {
        String message = String.format("%s %s was modified by another process", resourceType, resourceId);
        return new ResourceConflictException(message, resourceType, resourceId, "CONCURRENT_MODIFICATION");
    }

    public static ResourceConflictException invalidState(String resourceType, UUID resourceId, String currentState, String requiredState) {
        String message = String.format("%s %s is in state %s but requires state %s", 
                                      resourceType, resourceId, currentState, requiredState);
        return new ResourceConflictException(message, resourceType, resourceId, "INVALID_STATE");
    }

    public static ResourceConflictException dependencyConflict(String resourceType, UUID resourceId, String dependency) {
        String message = String.format("%s %s cannot be modified due to dependency on %s", 
                                      resourceType, resourceId, dependency);
        return new ResourceConflictException(message, resourceType, resourceId, "DEPENDENCY_CONFLICT");
    }
}