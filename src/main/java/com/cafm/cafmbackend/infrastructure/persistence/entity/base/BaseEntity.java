package com.cafm.cafmbackend.infrastructure.persistence.entity.base;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Base entity class providing common fields for all entities.
 * 
 * Explanation:
 * - Purpose: Provides common audit fields (id, timestamps, audit user) for all entities
 * - Pattern: Template pattern for entity inheritance, following DRY principle
 * - Java 23: Uses records for better immutability where applicable in subclasses
 * - Architecture: Part of data layer, provides foundation for all domain entities
 * - Standards: NO Lombok as per requirements, manual getters/setters for full control
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;
    
    @LastModifiedBy
    @Column(name = "modified_by")
    private UUID modifiedBy;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    // Constructor
    protected BaseEntity() {
        // Default constructor for JPA
    }
    
    // Getters and Setters (No Lombok as per standards)
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public UUID getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
    
    public UUID getModifiedBy() {
        return modifiedBy;
    }
    
    public void setModifiedBy(UUID modifiedBy) {
        this.modifiedBy = modifiedBy;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // equals and hashCode based on id
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity that)) return false;
        return id != null && id.equals(that.getId());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    // toString excluding potentially sensitive data
    @Override
    public String toString() {
        return String.format("%s[id=%s, createdAt=%s, updatedAt=%s, version=%s]",
                this.getClass().getSimpleName(), id, createdAt, updatedAt, version);
    }
    
    /**
     * Check if entity is new (not persisted yet)
     */
    public boolean isNew() {
        return id == null;
    }
}