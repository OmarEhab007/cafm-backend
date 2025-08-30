package com.cafm.cafmbackend.shared.exception;

import java.util.UUID;

/**
 * Exception thrown when a requested entity cannot be found.
 * 
 * Purpose: Provides structured error handling for missing entities
 * Pattern: Domain-specific exception with meaningful error messages
 * Java 23: Leverages enhanced exception handling and pattern matching
 * Architecture: Part of exception hierarchy for business errors
 * Standards: Consistent error messaging and HTTP status mapping
 */
public class EntityNotFoundException extends RuntimeException {
    
    private final String entityType;
    private final Object identifier;
    
    public EntityNotFoundException(String entityType, Object identifier) {
        super(String.format("%s not found with identifier: %s", entityType, identifier));
        this.entityType = entityType;
        this.identifier = identifier;
    }
    
    public EntityNotFoundException(String entityType, UUID id) {
        this(entityType, (Object) id);
    }
    
    public EntityNotFoundException(String entityType, String identifier) {
        this(entityType, (Object) identifier);
    }
    
    public EntityNotFoundException(String message) {
        super(message);
        this.entityType = "Unknown";
        this.identifier = "Unknown";
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public Object getIdentifier() {
        return identifier;
    }
}