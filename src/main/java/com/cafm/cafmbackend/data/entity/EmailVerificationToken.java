package com.cafm.cafmbackend.data.entity;

import com.cafm.cafmbackend.data.entity.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Entity for managing email verification tokens.
 * 
 * Purpose: Secure email verification functionality with time-limited tokens
 * Pattern: Token-based verification for email address confirmation
 * Java 23: Uses records for DTOs in service layer
 * Architecture: Extends BaseEntity for consistent audit tracking
 * Standards: Follows best practices for email verification
 */
@Entity
@Table(name = "email_verification_tokens", indexes = {
    @Index(name = "idx_email_token_hash", columnList = "token_hash"),
    @Index(name = "idx_email_expires", columnList = "expires_at"),
    @Index(name = "idx_email_user", columnList = "user_id")
})
public class EmailVerificationToken extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;
    
    @Column(name = "token_hash", nullable = false, unique = true)
    @NotNull(message = "Token hash is required")
    private String tokenHash;
    
    @Column(name = "email", nullable = false)
    @NotNull(message = "Email is required")
    private String email;
    
    @Column(name = "expires_at", nullable = false)
    @NotNull(message = "Expiration time is required")
    private LocalDateTime expiresAt;
    
    @Column(name = "verified")
    private boolean verified = false;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    // Constructors
    public EmailVerificationToken() {
        super();
    }
    
    public EmailVerificationToken(User user, String tokenHash, String email, LocalDateTime expiresAt, String ipAddress) {
        this();
        this.user = user;
        this.tokenHash = tokenHash;
        this.email = email;
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
     * Check if token is valid (not verified and not expired)
     */
    public boolean isValid() {
        return !verified && !isExpired();
    }
    
    /**
     * Mark token as verified
     */
    public void markAsVerified() {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
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
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public boolean isVerified() {
        return verified;
    }
    
    public void setVerified(boolean verified) {
        this.verified = verified;
    }
    
    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }
    
    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}