package com.cafm.cafmbackend.infrastructure.persistence.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreRemove;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base entity with soft delete capability.
 * 
 * Explanation:
 * - Purpose: Extends BaseEntity to add soft delete functionality matching our database design
 * - Pattern: Soft delete pattern prevents data loss while maintaining referential integrity
 * - Java 23: Ready for pattern matching and enhanced switch expressions in queries
 * - Architecture: Implements soft delete at entity level, transparent to service layer
 * - Standards: Follows database V8 migration soft delete implementation
 */
@MappedSuperclass
public abstract class SoftDeletableEntity extends BaseEntity {
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @Column(name = "deleted_by")
    private UUID deletedBy;
    
    @Column(name = "deletion_reason")
    private String deletionReason;
    
    // Constructor
    protected SoftDeletableEntity() {
        super();
    }
    
    // Getters and Setters
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
    
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
    
    public UUID getDeletedBy() {
        return deletedBy;
    }
    
    public void setDeletedBy(UUID deletedBy) {
        this.deletedBy = deletedBy;
    }
    
    public String getDeletionReason() {
        return deletionReason;
    }
    
    public void setDeletionReason(String deletionReason) {
        this.deletionReason = deletionReason;
    }
    
    // Business methods
    
    /**
     * Check if entity is deleted
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }
    
    /**
     * Check if entity is active (not deleted)
     */
    public boolean isActive() {
        return deletedAt == null;
    }
    
    /**
     * Soft delete the entity
     */
    public void softDelete(UUID deletedBy, String reason) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
        this.deletionReason = reason;
    }
    
    /**
     * Soft delete the entity without reason
     */
    public void softDelete(UUID deletedBy) {
        softDelete(deletedBy, null);
    }
    
    /**
     * Restore a soft-deleted entity
     */
    public void restore() {
        this.deletedAt = null;
        this.deletedBy = null;
        this.deletionReason = null;
    }
    
    /**
     * Check if entity can be restored (within 30 days as per business rules)
     */
    public boolean canBeRestored() {
        if (!isDeleted()) {
            return false;
        }
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return deletedAt.isAfter(thirtyDaysAgo);
    }
    
    /**
     * Check if entity should be purged (after 90 days as per business rules)
     */
    public boolean shouldBePurged() {
        if (!isDeleted()) {
            return false;
        }
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        return deletedAt.isBefore(ninetyDaysAgo);
    }
    
    @PreRemove
    protected void onRemove() {
        // Prevent hard delete if not soft deleted
        if (!isDeleted()) {
            throw new IllegalStateException(
                "Cannot hard delete entity that is not soft deleted. Use softDelete() first."
            );
        }
        // Only allow hard delete if purge time has passed
        if (!shouldBePurged()) {
            throw new IllegalStateException(
                "Cannot hard delete entity. Retention period (90 days) has not passed."
            );
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s[deleted=%s, deletedAt=%s]",
                super.toString(), isDeleted(), deletedAt);
    }
}