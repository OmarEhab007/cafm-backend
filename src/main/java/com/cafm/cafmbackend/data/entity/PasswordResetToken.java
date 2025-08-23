package com.cafm.cafmbackend.data.entity;

import com.cafm.cafmbackend.data.entity.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Entity for managing password reset tokens.
 * 
 * Purpose: Secure password reset functionality with time-limited tokens
 * Pattern: Token-based authentication for sensitive operations
 * Java 23: Uses records for DTOs in service layer
 * Architecture: Extends BaseEntity for consistent audit tracking
 * Standards: Follows OWASP guidelines for secure password reset
 */
@Entity
@Table(name = "password_reset_tokens", indexes = {
    @Index(name = "idx_reset_token_hash", columnList = "token_hash"),
    @Index(name = "idx_reset_expires", columnList = "expires_at"),
    @Index(name = "idx_reset_user", columnList = "user_id")
})
public class PasswordResetToken extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;
    
    @Column(name = "token_hash", nullable = false, unique = true)
    @NotNull(message = "Token hash is required")
    private String tokenHash;
    
    @Column(name = "expires_at", nullable = false)
    @NotNull(message = "Expiration time is required")
    private LocalDateTime expiresAt;
    
    @Column(name = "used")
    private boolean used = false;
    
    @Column(name = "used_at")
    private LocalDateTime usedAt;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    // Constructors
    public PasswordResetToken() {
        super();
    }
    
    public PasswordResetToken(User user, String tokenHash, LocalDateTime expiresAt, String ipAddress) {
        this();
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.ipAddress = ipAddress;
    }
    
    // Business Methods
    
    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Check if token is valid (not used and not expired)
     */
    public boolean isValid() {
        return !used && !isExpired();
    }
    
    /**
     * Mark token as used
     */
    public void markAsUsed() {
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getTokenHash() {
        return tokenHash;
    }
    
    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public boolean isUsed() {
        return used;
    }
    
    public void setUsed(boolean used) {
        this.used = used;
    }
    
    public LocalDateTime getUsedAt() {
        return usedAt;
    }
    
    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}