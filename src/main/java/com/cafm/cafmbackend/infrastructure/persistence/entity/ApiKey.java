package com.cafm.cafmbackend.infrastructure.persistence.entity;

import com.cafm.cafmbackend.infrastructure.persistence.entity.base.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Entity representing API keys for external client authentication.
 * 
 * Purpose: Store API keys for external system integration
 * Pattern: Entity with secure key management
 * Java 23: Record-like getters/setters
 * Architecture: Data layer entity
 * Standards: Secure API key storage with hashing
 */
@Entity
@Table(name = "api_keys")
public class ApiKey extends BaseEntity {
    
    @Column(name = "key_name", nullable = false, unique = true)
    private String keyName;
    
    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;
    
    @Column(name = "key_prefix", nullable = false)
    private String keyPrefix;
    
    @Column(name = "description")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "last_used_ip")
    private String lastUsedIp;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "api_key_scopes", joinColumns = @JoinColumn(name = "api_key_id"))
    @Column(name = "scope")
    private Set<String> scopes;
    
    @Column(name = "rate_limit_tier")
    @Enumerated(EnumType.STRING)
    private RateLimitTier rateLimitTier = RateLimitTier.STANDARD;
    
    @Column(name = "allowed_ips", columnDefinition = "TEXT")
    private String allowedIps;
    
    @Column(name = "usage_count")
    private Long usageCount = 0L;
    
    @Column(name = "created_by_user_id")
    private UUID createdByUserId;
    
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    @Column(name = "revoked_by_user_id")
    private UUID revokedByUserId;
    
    @Column(name = "revoke_reason")
    private String revokeReason;
    
    // Constructors
    public ApiKey() {}
    
    public ApiKey(String keyName, String keyHash, String keyPrefix, Company company) {
        this.keyName = keyName;
        this.keyHash = keyHash;
        this.keyPrefix = keyPrefix;
        this.company = company;
        this.isActive = true;
        this.usageCount = 0L;
    }
    
    // Business methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isRevoked() {
        return revokedAt != null;
    }
    
    public boolean isValid() {
        return isActive && !isExpired() && !isRevoked();
    }
    
    public void recordUsage(String ipAddress) {
        this.lastUsedAt = LocalDateTime.now();
        this.lastUsedIp = ipAddress;
        this.usageCount++;
    }
    
    public void revoke(UUID userId, String reason) {
        this.isActive = false;
        this.revokedAt = LocalDateTime.now();
        this.revokedByUserId = userId;
        this.revokeReason = reason;
    }
    
    public boolean hasScope(String scope) {
        return scopes != null && scopes.contains(scope);
    }
    
    public boolean isIpAllowed(String ipAddress) {
        if (allowedIps == null || allowedIps.isEmpty()) {
            return true; // No IP restriction
        }
        
        String[] allowedIpList = allowedIps.split(",");
        for (String allowedIp : allowedIpList) {
            if (allowedIp.trim().equals(ipAddress) || 
                matchesIpPattern(ipAddress, allowedIp.trim())) {
                return true;
            }
        }
        return false;
    }
    
    private boolean matchesIpPattern(String ip, String pattern) {
        // Simple wildcard matching for IP patterns like 192.168.1.*
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return ip.startsWith(prefix);
        }
        return false;
    }
    
    // Getters and Setters
    public String getKeyName() { return keyName; }
    public void setKeyName(String keyName) { this.keyName = keyName; }
    
    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }
    
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    
    public String getLastUsedIp() { return lastUsedIp; }
    public void setLastUsedIp(String lastUsedIp) { this.lastUsedIp = lastUsedIp; }
    
    public Set<String> getScopes() { return scopes; }
    public void setScopes(Set<String> scopes) { this.scopes = scopes; }
    
    public RateLimitTier getRateLimitTier() { return rateLimitTier; }
    public void setRateLimitTier(RateLimitTier rateLimitTier) { this.rateLimitTier = rateLimitTier; }
    
    public String getAllowedIps() { return allowedIps; }
    public void setAllowedIps(String allowedIps) { this.allowedIps = allowedIps; }
    
    public Long getUsageCount() { return usageCount; }
    public void setUsageCount(Long usageCount) { this.usageCount = usageCount; }
    
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }
    
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    
    public UUID getRevokedByUserId() { return revokedByUserId; }
    public void setRevokedByUserId(UUID revokedByUserId) { this.revokedByUserId = revokedByUserId; }
    
    public String getRevokeReason() { return revokeReason; }
    public void setRevokeReason(String revokeReason) { this.revokeReason = revokeReason; }
    
    /**
     * Rate limit tier enumeration
     */
    public enum RateLimitTier {
        LOW(20),
        STANDARD(60),
        HIGH(300),
        UNLIMITED(Integer.MAX_VALUE);
        
        private final int requestsPerMinute;
        
        RateLimitTier(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }
        
        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }
    }
}