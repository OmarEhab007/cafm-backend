package com.cafm.cafmbackend.dto.damage;

import com.cafm.cafmbackend.infrastructure.persistence.entity.DamageCount.DamageCountStatus;
import com.cafm.cafmbackend.infrastructure.persistence.entity.DamageCount.PriorityLevel;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Simplified DamageCount response that matches actual entity fields.
 * This replaces the complex DamageCountResponse with realistic data from the entity.
 */
public record DamageCountSimplifiedResponse(
    // Core identifiers
    UUID id,
    UUID schoolId,
    String schoolName,
    UUID supervisorId,
    String supervisorName,
    UUID companyId,
    
    // Status and priority
    DamageCountStatus status,
    PriorityLevel priority,
    
    // Core data from entity
    Map<String, Integer> itemCounts,
    Map<String, List<String>> sectionPhotos,
    Integer totalItemsCount,
    
    // Cost information
    BigDecimal estimatedRepairCost,
    String repairNotes,
    
    // Timestamps
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime submittedAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime reviewedAt,
    
    UUID reviewedBy,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt
) {
    /**
     * Create a response for list views (minimal data)
     */
    public static DamageCountSimplifiedResponse forList(
        UUID id,
        String schoolName,
        DamageCountStatus status,
        PriorityLevel priority,
        Integer totalItemsCount,
        LocalDateTime createdAt
    ) {
        return new DamageCountSimplifiedResponse(
            id, null, schoolName, null, null, null,
            status, priority,
            null, null, totalItemsCount,
            null, null,
            null, null, null,
            createdAt, null
        );
    }
    
    /**
     * Builder for creating responses from entities
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private UUID id;
        private UUID schoolId;
        private String schoolName;
        private UUID supervisorId;
        private String supervisorName;
        private UUID companyId;
        private DamageCountStatus status;
        private PriorityLevel priority;
        private Map<String, Integer> itemCounts;
        private Map<String, List<String>> sectionPhotos;
        private Integer totalItemsCount;
        private BigDecimal estimatedRepairCost;
        private String repairNotes;
        private LocalDateTime submittedAt;
        private LocalDateTime reviewedAt;
        private UUID reviewedBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public Builder id(UUID id) {
            this.id = id;
            return this;
        }
        
        public Builder schoolId(UUID schoolId) {
            this.schoolId = schoolId;
            return this;
        }
        
        public Builder schoolName(String schoolName) {
            this.schoolName = schoolName;
            return this;
        }
        
        public Builder supervisorId(UUID supervisorId) {
            this.supervisorId = supervisorId;
            return this;
        }
        
        public Builder supervisorName(String supervisorName) {
            this.supervisorName = supervisorName;
            return this;
        }
        
        public Builder companyId(UUID companyId) {
            this.companyId = companyId;
            return this;
        }
        
        public Builder status(DamageCountStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder priority(PriorityLevel priority) {
            this.priority = priority;
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
        
        public Builder totalItemsCount(Integer totalItemsCount) {
            this.totalItemsCount = totalItemsCount;
            return this;
        }
        
        public Builder estimatedRepairCost(BigDecimal estimatedRepairCost) {
            this.estimatedRepairCost = estimatedRepairCost;
            return this;
        }
        
        public Builder repairNotes(String repairNotes) {
            this.repairNotes = repairNotes;
            return this;
        }
        
        public Builder submittedAt(LocalDateTime submittedAt) {
            this.submittedAt = submittedAt;
            return this;
        }
        
        public Builder reviewedAt(LocalDateTime reviewedAt) {
            this.reviewedAt = reviewedAt;
            return this;
        }
        
        public Builder reviewedBy(UUID reviewedBy) {
            this.reviewedBy = reviewedBy;
            return this;
        }
        
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public DamageCountSimplifiedResponse build() {
            return new DamageCountSimplifiedResponse(
                id, schoolId, schoolName, supervisorId, supervisorName, companyId,
                status, priority,
                itemCounts, sectionPhotos, totalItemsCount,
                estimatedRepairCost, repairNotes,
                submittedAt, reviewedAt, reviewedBy,
                createdAt, updatedAt
            );
        }
    }
}