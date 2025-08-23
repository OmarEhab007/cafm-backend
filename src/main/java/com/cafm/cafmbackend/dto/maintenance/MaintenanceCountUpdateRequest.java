package com.cafm.cafmbackend.dto.maintenance;

import java.util.Map;

/**
 * Request DTO for partial updates to maintenance counts.
 * All fields are optional to support PATCH operations.
 */
public record MaintenanceCountUpdateRequest(
    Map<String, Integer> itemCounts,
    Map<String, Integer> itemRepairCounts,
    Map<String, Integer> sectionCounts,
    Map<String, Object> electricalItems,
    Map<String, Object> plumbingItems,
    Map<String, Object> civilItems,
    Map<String, Object> furnitureItems,
    Map<String, Object> hvacItems,
    Map<String, Object> safetyItems,
    Map<String, Object> customFields,
    Integer totalItems,
    Integer totalDamaged,
    Integer totalRepaired,
    String notes,
    String verifiedBy,
    Boolean verified
) {
    /**
     * Merge specific item counts into existing map.
     */
    public static MaintenanceCountUpdateRequest mergeItemCounts(
            Map<String, Integer> existingCounts,
            Map<String, Integer> newCounts) {
        Map<String, Integer> merged = new java.util.HashMap<>(existingCounts);
        merged.putAll(newCounts);
        
        return new MaintenanceCountUpdateRequest(
            merged,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
    
    /**
     * Update only verification status.
     */
    public static MaintenanceCountUpdateRequest verify(String verifiedBy) {
        return new MaintenanceCountUpdateRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            verifiedBy,
            true
        );
    }
}