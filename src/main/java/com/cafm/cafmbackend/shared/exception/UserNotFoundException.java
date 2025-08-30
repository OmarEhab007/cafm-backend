package com.cafm.cafmbackend.shared.exception;

import java.util.UUID;

/**
 * Exception thrown when a user is not found.
 */
public class UserNotFoundException extends RuntimeException {
    
    private final UUID userId;
    private final String identifier;
    
    public UserNotFoundException(UUID userId) {
        super("User not found with ID: " + userId);
        this.userId = userId;
        this.identifier = null;
    }
    
    public UserNotFoundException(String identifier) {
        super("User not found with identifier: " + identifier);
        this.userId = null;
        this.identifier = identifier;
    }
    
    public UserNotFoundException(String message, UUID userId) {
        super(message);
        this.userId = userId;
        this.identifier = null;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public String getIdentifier() {
        return identifier;
    }
}