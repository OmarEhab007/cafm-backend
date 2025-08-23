package com.cafm.cafmbackend.dto.maintenance;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for maintenance count data.
 * Includes all JSONB fields and metadata.
 */
public record MaintenanceCountResponse(
    UUID id,
    
    UUID schoolId,
    String schoolName,
    
    UUID companyId,
    String companyName,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate reportDate,
    
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
    String reportedBy,
    String verifiedBy,
    Boolean verified,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime verifiedAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt,
    
    String createdBy,
    String updatedBy,
    
    MaintenanceStats stats
) {
    /**
     * Calculated statistics for maintenance counts.
     */
    public record MaintenanceStats(
        Double damagePercentage,
        Double repairPercentage,
        Integer pendingRepairs,
        Map<String, Integer> topDamagedItems,
        Map<String, Integer> criticalItems
    ) {
        public static MaintenanceStats calculate(
                Integer totalItems,
                Integer totalDamaged,
                Integer totalRepaired,
                Map<String, Integer> itemCounts,
                Map<String, Integer> itemRepairCounts) {
            
            double damagePercentage = totalItems > 0 
                ? (totalDamaged * 100.0) / totalItems 
                : 0.0;
                
            double repairPercentage = totalDamaged > 0
                ? (totalRepaired * 100.0) / totalDamaged
                : 0.0;
                
            int pendingRepairs = Math.max(0, totalDamaged - totalRepaired);
            
            // Get top 5 damaged items
            Map<String, Integer> topDamagedItems = itemCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    java.util.LinkedHashMap::new
                ));
            
            // Items with damage > 50%
            Map<String, Integer> criticalItems = itemCounts.entrySet().stream()
                .filter(entry -> {
                    Integer repairs = itemRepairCounts.getOrDefault(entry.getKey(), 0);
                    return entry.getValue() > 0 && 
                           (entry.getValue() - repairs) > (entry.getValue() * 0.5);
                })
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
            
            return new MaintenanceStats(
                damagePercentage,
                repairPercentage,
                pendingRepairs,
                topDamagedItems,
                criticalItems
            );
        }
    }
}