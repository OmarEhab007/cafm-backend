package com.cafm.cafmbackend.dto.mobile;

import com.cafm.cafmbackend.shared.enums.AssetStatus;
import com.cafm.cafmbackend.shared.enums.AssetCondition;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mobile-optimized Asset DTO for maintenance operations.
 * 
 * Purpose: Essential asset information for field maintenance work
 * Pattern: Compact mobile DTO with critical asset data only
 * Java 23: Record-based DTO optimized for mobile bandwidth
 * Architecture: Mobile layer DTO for asset management synchronization
 * Standards: Implements mobile sync interface for offline capability
 */
public record MobileAssetDto(
    UUID id,
    String code,
    String name,
    String nameAr,
    String description,
    AssetStatus status,
    AssetCondition condition,
    UUID categoryId,
    String categoryName,
    UUID schoolId,
    String schoolName,
    String location,
    String serialNumber,
    String manufacturer,
    String model,
    Boolean isActive,
    LocalDateTime lastMaintenanceDate,
    LocalDateTime nextMaintenanceDate,
    LocalDateTime lastModified,
    Long version
) implements MobileSyncDto {
    
    @Override
    public String getEntityType() {
        return "ASSET";
    }
    
    @Override
    public UUID getEntityId() {
        return id;
    }
    
    @Override
    public LocalDateTime getLastModified() {
        return lastModified;
    }
    
    @Override
    public Long getVersion() {
        return version;
    }
    
    @Override
    public int getSyncPriority() {
        return switch (condition) {
            case BROKEN -> 5;
            case POOR -> 4;
            case FAIR -> 3;
            case GOOD -> 2;
            case EXCELLENT -> 1;
            default -> 2;
        };
    }
    
    @Override
    public boolean shouldSync(UUID userId, String userRole) {
        return switch (userRole.toUpperCase()) {
            case "ADMIN" -> true;
            case "SUPERVISOR", "TECHNICIAN" -> status == AssetStatus.IN_USE || 
                                                status == AssetStatus.MAINTENANCE_REQUIRED ||
                                                condition == AssetCondition.BROKEN ||
                                                condition == AssetCondition.POOR;
            default -> false;
        };
    }
    
    /**
     * Check if asset requires immediate attention.
     */
    public boolean requiresImmediateAttention() {
        return condition == AssetCondition.BROKEN || 
               status == AssetStatus.MAINTENANCE_REQUIRED;
    }
    
    /**
     * Check if asset maintenance is overdue.
     */
    public boolean isMaintenanceOverdue() {
        if (nextMaintenanceDate == null) return false;
        return LocalDateTime.now().isAfter(nextMaintenanceDate);
    }
}