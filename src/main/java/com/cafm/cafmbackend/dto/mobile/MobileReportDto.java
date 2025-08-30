package com.cafm.cafmbackend.dto.mobile;

import com.cafm.cafmbackend.shared.enums.ReportStatus;
import com.cafm.cafmbackend.shared.enums.Priority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Mobile-optimized Report DTO for field operations.
 * 
 * Purpose: Essential report data for mobile maintenance operations
 * Pattern: Mobile-first DTO optimized for offline work and field updates
 * Java 23: Record with nested records for efficient data structure
 * Architecture: Mobile layer DTO for maintenance report synchronization
 * Standards: Follows mobile sync patterns with conflict resolution support
 */
public record MobileReportDto(
    UUID id,
    String title,
    String description,
    ReportStatus status,
    Priority priority,
    UUID schoolId,
    String schoolName,
    UUID assetId,
    String assetName,
    UUID reportedById,
    String reportedByName,
    UUID assignedToId,
    String assignedToName,
    List<String> imageUrls,
    LocalDateTime reportedAt,
    LocalDateTime dueDate,
    LocalDateTime completedAt,
    LocalDateTime lastModified,
    Long version,
    MobileLocationDto location
) implements MobileSyncDto {
    
    @Override
    public String getEntityType() {
        return "REPORT";
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
        return switch (priority) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
            default -> 1;
        };
    }
    
    @Override
    public boolean shouldSync(UUID userId, String userRole) {
        return switch (userRole.toUpperCase()) {
            case "ADMIN" -> true;
            case "SUPERVISOR" -> assignedToId != null && assignedToId.equals(userId);
            case "TECHNICIAN" -> assignedToId != null && assignedToId.equals(userId);
            default -> false;
        };
    }
    
    /**
     * Mobile location data for field operations.
     */
    public record MobileLocationDto(
        String building,
        String floor,
        String room,
        String coordinates
    ) {}
}