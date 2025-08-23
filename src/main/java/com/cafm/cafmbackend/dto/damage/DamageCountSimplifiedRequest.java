package com.cafm.cafmbackend.dto.damage;

import com.cafm.cafmbackend.data.entity.DamageCount.PriorityLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Simplified request DTO for creating damage counts.
 * Only includes fields that actually exist in the DamageCount entity.
 */
public record DamageCountSimplifiedRequest(
    @NotNull(message = "School ID is required")
    UUID schoolId,
    
    @NotNull(message = "Supervisor ID is required") 
    UUID supervisorId,
    
    @NotNull(message = "Company ID is required")
    UUID companyId,
    
    @NotNull(message = "Item counts are required")
    Map<String, Integer> itemCounts,
    
    Map<String, List<String>> sectionPhotos,
    
    PriorityLevel priority,
    
    @PositiveOrZero(message = "Estimated repair cost must be zero or positive")
    BigDecimal estimatedRepairCost,
    
    String repairNotes
) {
    /**
     * Create a basic damage count request with minimal fields.
     */
    public static DamageCountSimplifiedRequest basic(
            UUID schoolId,
            UUID supervisorId,
            UUID companyId,
            Map<String, Integer> itemCounts) {
        return new DamageCountSimplifiedRequest(
            schoolId,
            supervisorId,
            companyId,
            itemCounts,
            Map.of(),
            PriorityLevel.MEDIUM,
            BigDecimal.ZERO,
            null
        );
    }
}