package com.cafm.cafmbackend.dto.damage;

import com.cafm.cafmbackend.data.entity.DamageCount.PriorityLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating or updating damage counts.
 */
public record DamageCountRequest(
    @NotNull(message = "School ID is required")
    UUID schoolId,
    
    @NotNull(message = "Company ID is required")
    UUID companyId,
    
    @NotNull(message = "Report date is required")
    LocalDate reportDate,
    
    @NotNull(message = "Item counts are required")
    Map<String, Integer> itemCounts,
    
    Map<String, List<String>> sectionPhotos,
    
    @NotNull(message = "Section is required")
    String section,
    
    String location,
    
    String description,
    
    @NotNull(message = "Priority is required")
    PriorityLevel priority,
    
    @PositiveOrZero(message = "Total items must be zero or positive")
    Integer totalItems,
    
    @PositiveOrZero(message = "Total damaged must be zero or positive")
    Integer totalDamaged,
    
    @PositiveOrZero(message = "Repair cost must be zero or positive")
    BigDecimal estimatedRepairCost,
    
    String reportedBy,
    
    String verifiedBy,
    
    Boolean requiresUrgentAction,
    
    Boolean safetyHazard,
    
    LocalDateTime inspectionDate,
    
    String inspectorName,
    
    String notes
) {
    /**
     * Creates a basic damage count request.
     */
    public static DamageCountRequest basic(
            UUID schoolId,
            UUID companyId,
            String section,
            Map<String, Integer> itemCounts,
            PriorityLevel priority) {
        return new DamageCountRequest(
            schoolId,
            companyId,
            LocalDate.now(),
            itemCounts,
            Map.of(),
            section,
            null,
            null,
            priority,
            0,
            0,
            BigDecimal.ZERO,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
    }
    
    /**
     * Builder for complex damage count requests.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private UUID schoolId;
        private UUID companyId;
        private LocalDate reportDate = LocalDate.now();
        private Map<String, Integer> itemCounts = Map.of();
        private Map<String, List<String>> sectionPhotos = Map.of();
        private String section;
        private String location;
        private String description;
        private PriorityLevel priority = PriorityLevel.MEDIUM;
        private Integer totalItems = 0;
        private Integer totalDamaged = 0;
        private BigDecimal estimatedRepairCost = BigDecimal.ZERO;
        private String reportedBy;
        private String verifiedBy;
        private Boolean requiresUrgentAction = false;
        private Boolean safetyHazard = false;
        private LocalDateTime inspectionDate;
        private String inspectorName;
        private String notes;
        
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
        
        public Builder sectionPhotos(Map<String, List<String>> sectionPhotos) {
            this.sectionPhotos = sectionPhotos;
            return this;
        }
        
        public Builder section(String section) {
            this.section = section;
            return this;
        }
        
        public Builder location(String location) {
            this.location = location;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder priority(PriorityLevel priority) {
            this.priority = priority;
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
        
        public Builder estimatedRepairCost(BigDecimal estimatedRepairCost) {
            this.estimatedRepairCost = estimatedRepairCost;
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
        
        public Builder requiresUrgentAction(Boolean requiresUrgentAction) {
            this.requiresUrgentAction = requiresUrgentAction;
            return this;
        }
        
        public Builder safetyHazard(Boolean safetyHazard) {
            this.safetyHazard = safetyHazard;
            return this;
        }
        
        public Builder inspectionDate(LocalDateTime inspectionDate) {
            this.inspectionDate = inspectionDate;
            return this;
        }
        
        public Builder inspectorName(String inspectorName) {
            this.inspectorName = inspectorName;
            return this;
        }
        
        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }
        
        public DamageCountRequest build() {
            return new DamageCountRequest(
                schoolId,
                companyId,
                reportDate,
                itemCounts,
                sectionPhotos,
                section,
                location,
                description,
                priority,
                totalItems,
                totalDamaged,
                estimatedRepairCost,
                reportedBy,
                verifiedBy,
                requiresUrgentAction,
                safetyHazard,
                inspectionDate,
                inspectorName,
                notes
            );
        }
    }
}