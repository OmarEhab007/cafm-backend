package com.cafm.cafmbackend.exception;

/**
 * Exception thrown when authentication credentials are invalid.
 */
public class InvalidCredentialsException extends RuntimeException {
    
    private final String username;
    private final int remainingAttempts;
    
    public InvalidCredentialsException(String message) {
        super(message);
        this.username = null;
        this.remainingAttempts = -1;
    }
    
    public InvalidCredentialsException(String message, String username) {
        super(message);
        this.username = username;
        this.remainingAttempts = -1;
    }
    
    public InvalidCredentialsException(String message, String username, int remainingAttempts) {
        super(message);
        this.username = username;
        this.remainingAttempts = remainingAttempts;
    }
    
    public String getUsername() {
        return username;
    }
    
    public int getRemainingAttempts() {
        return remainingAttempts;
    }
}