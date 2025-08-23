package com.cafm.cafmbackend.dto.maintenance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating or updating maintenance counts.
 * Handles complex JSONB fields as Java Maps.
 */
public record MaintenanceCountRequest(
    @NotNull(message = "School ID is required")
    UUID schoolId,
    
    @NotNull(message = "Company ID is required")
    UUID companyId,
    
    @NotNull(message = "Report date is required")
    LocalDate reportDate,
    
    @NotNull(message = "Item counts are required")
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
    
    @Positive(message = "Total items must be positive")
    Integer totalItems,
    
    @Positive(message = "Total damaged must be positive")
    Integer totalDamaged,
    
    @Positive(message = "Total repaired must be positive")
    Integer totalRepaired,
    
    String notes,
    
    String reportedBy,
    
    String verifiedBy,
    
    Boolean verified
) {
    /**
     * Creates a request with default empty maps for optional JSONB fields.
     */
    public static MaintenanceCountRequest withDefaults(
            UUID schoolId,
            UUID companyId,
            LocalDate reportDate,
            Map<String, Integer> itemCounts) {
        return new MaintenanceCountRequest(
            schoolId,
            companyId,
            reportDate,
            itemCounts,
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            0,
            0,
            0,
            null,
            null,
            null,
            false
        );
    }
    
    /**
     * Builder pattern for complex object construction.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private UUID schoolId;
        private UUID companyId;
        private LocalDate reportDate;
        private Map<String, Integer> itemCounts = Map.of();
        private Map<String, Integer> itemRepairCounts = Map.of();
        private Map<String, Integer> sectionCounts = Map.of();
        private Map<String, Object> electricalItems = Map.of();
        private Map<String, Object> plumbingItems = Map.of();
        private Map<String, Object> civilItems = Map.of();
        private Map<String, Object> furnitureItems = Map.of();
        private Map<String, Object> hvacItems = Map.of();
        private Map<String, Object> safetyItems = Map.of();
        private Map<String, Object> customFields = Map.of();
        private Integer totalItems = 0;
        private Integer totalDamaged = 0;
        private Integer totalRepaired = 0;
        private String notes;
        private String reportedBy;
        private String verifiedBy;
        private Boolean verified = false;
        
        public Builder schoolId(UUID schoolId) {
            this.schoolId = schoolId;
            return this;
        }
        
        public Builder companyId(UUID companyId) {
            this.companyId = companyId;
            return this;
        }
        
        public Builder reportDate(LocalDate reportDate) {
            this.reportDate = reportDate;
            return this;
        }
        
        public Builder itemCounts(Map<String, Integer> itemCounts) {
            this.itemCounts = itemCounts;
            return this;
        }
        
        public Builder itemRepairCounts(Map<String, Integer> itemRepairCounts) {
            this.itemRepairCounts = itemRepairCounts;
            return this;
        }
        
        public Builder sectionCounts(Map<String, Integer> sectionCounts) {
            this.sectionCounts = sectionCounts;
            return this;
        }
        
        public Builder electricalItems(Map<String, Object> electricalItems) {
            this.electricalItems = electricalItems;
            return this;
        }
        
        public Builder plumbingItems(Map<String, Object> plumbingItems) {
            this.plumbingItems = plumbingItems;
            return this;
        }
        
        public Builder civilItems(Map<String, Object> civilItems) {
            this.civilItems = civilItems;
            return this;
        }
        
        public Builder furnitureItems(Map<String, Object> furnitureItems) {
            this.furnitureItems = furnitureItems;
            return this;
        }
        
        public Builder hvacItems(Map<String, Object> hvacItems) {
            this.hvacItems = hvacItems;
            return this;
        }
        
        public Builder safetyItems(Map<String, Object> safetyItems) {
            this.safetyItems = safetyItems;
            return this;
        }
        
        public Builder customFields(Map<String, Object> customFields) {
            this.customFields = customFields;
            return this;
        }
        
        public Builder totalItems(Integer totalItems) {
            this.totalItems = totalItems;
            return this;
        }
        
        public Builder totalDamaged(Integer totalDamaged) {
            this.totalDamaged = totalDamaged;
            return this;
        }
        
        public Builder totalRepaired(Integer totalRepaired) {
            this.totalRepaired = totalRepaired;
            return this;
        }
        
        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }
        
        public Builder reportedBy(String reportedBy) {
            this.reportedBy = reportedBy;
            return this;
        }
        
        public Builder verifiedBy(String verifiedBy) {
            this.verifiedBy = verifiedBy;
            return this;
        }
        
        public Builder verified(Boolean verified) {
            this.verified = verified;
            return this;
        }
        
        public MaintenanceCountRequest build() {
            return new MaintenanceCountRequest(
                schoolId,
                companyId,
                reportDate,
                itemCounts,
                itemRepairCounts,
                sectionCounts,
                electricalItems,
                plumbingItems,
                civilItems,
                furnitureItems,
                hvacItems,
                safetyItems,
                customFields,
                totalItems,
                totalDamaged,
                totalRepaired,
                notes,
                reportedBy,
                verifiedBy,
                verified
            );
        }
    }
}