package com.cafm.cafmbackend.dto.mobile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mobile-optimized School DTO for synchronization.
 * 
 * Purpose: Lightweight school data transfer for mobile applications
 * Pattern: Mobile-specific DTO with minimal data and optimized serialization
 * Java 23: Record-based DTO with efficient memory usage
 * Architecture: Mobile layer DTO for offline-first synchronization
 * Standards: Follows mobile data optimization patterns for bandwidth efficiency
 */
public record MobileSchoolDto(
    UUID id,
    String code,
    String name,
    String nameAr,
    String district,
    BigDecimal latitude,
    BigDecimal longitude,
    String contactPhone,
    Boolean isActive,
    LocalDateTime lastModified,
    Long version // For optimistic locking and conflict resolution
) implements MobileSyncDto {
    
    @Override
    public String getEntityType() {
        return "SCHOOL";
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
}