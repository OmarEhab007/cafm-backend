package com.cafm.cafmbackend.dto.workorder;

import com.cafm.cafmbackend.data.enums.WorkOrderPriority;
import com.cafm.cafmbackend.data.enums.WorkOrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Simplified WorkOrder response that matches actual WorkOrder entity fields.
 */
public record WorkOrderSimplifiedResponse(
    // Core identifiers
    UUID id,
    String workOrderNumber,
    
    // Basic information
    String title,
    String description,
    WorkOrderStatus status,
    WorkOrderPriority priority,
    
    // References
    UUID reportId,
    UUID schoolId,
    String schoolName,
    UUID assignedToId,
    String assignedToName,
    UUID companyId,
    
    // Schedule
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime scheduledStart,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime scheduledEnd,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime actualStart,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime actualEnd,
    
    // Progress
    Integer completionPercentage,
    Double actualHours,
    
    // Verification
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime verifiedAt,
    UUID verifiedBy,
    
    // Instructions
    String instructions,
    String completionNotes,
    
    // Timestamps
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt
) {
    /**
     * Create a response for list views (minimal data)
     */
    public static WorkOrderSimplifiedResponse forList(
        UUID id,
        String workOrderNumber,
        String title,
        WorkOrderStatus status,
        WorkOrderPriority priority,
        String assignedToName,
        LocalDateTime scheduledStart
    ) {
        return new WorkOrderSimplifiedResponse(
            id, workOrderNumber,
            title, null, status, priority,
            null, null, null, null, assignedToName, null,
            scheduledStart, null, null, null,
            null, null,
            null, null,
            null, null,
            null, null
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
        private String workOrderNumber;
        private String title;
        private String description;
        private WorkOrderStatus status;
        private WorkOrderPriority priority;
        private UUID reportId;
        private UUID schoolId;
        private String schoolName;
        private UUID assignedToId;
        private String assignedToName;
        private UUID companyId;
        private LocalDateTime scheduledStart;
        private LocalDateTime scheduledEnd;
        private LocalDateTime actualStart;
        private LocalDateTime actualEnd;
        private Integer completionPercentage;
        private Double actualHours;
        private LocalDateTime verifiedAt;
        private UUID verifiedBy;
        private String instructions;
        private String completionNotes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public Builder id(UUID id) {
            this.id = id;
            return this;
        }
        
        public Builder workOrderNumber(String workOrderNumber) {
            this.workOrderNumber = workOrderNumber;
            return this;
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder status(WorkOrderStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder priority(WorkOrderPriority priority) {
            this.priority = priority;
            return this;
        }
        
        public Builder reportId(UUID reportId) {
            this.reportId = reportId;
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
        
        public Builder assignedToId(UUID assignedToId) {
            this.assignedToId = assignedToId;
            return this;
        }
        
        public Builder assignedToName(String assignedToName) {
            this.assignedToName = assignedToName;
            return this;
        }
        
        public Builder companyId(UUID companyId) {
            this.companyId = companyId;
            return this;
        }
        
        public Builder scheduledStart(LocalDateTime scheduledStart) {
            this.scheduledStart = scheduledStart;
            return this;
        }
        
        public Builder scheduledEnd(LocalDateTime scheduledEnd) {
            this.scheduledEnd = scheduledEnd;
            return this;
        }
        
        public Builder actualStart(LocalDateTime actualStart) {
            this.actualStart = actualStart;
            return this;
        }
        
        public Builder actualEnd(LocalDateTime actualEnd) {
            this.actualEnd = actualEnd;
            return this;
        }
        
        public Builder completionPercentage(Integer completionPercentage) {
            this.completionPercentage = completionPercentage;
            return this;
        }
        
        public Builder actualHours(Double actualHours) {
            this.actualHours = actualHours;
            return this;
        }
        
        public Builder verifiedAt(LocalDateTime verifiedAt) {
            this.verifiedAt = verifiedAt;
            return this;
        }
        
        public Builder verifiedBy(UUID verifiedBy) {
            this.verifiedBy = verifiedBy;
            return this;
        }
        
        public Builder instructions(String instructions) {
            this.instructions = instructions;
            return this;
        }
        
        public Builder completionNotes(String completionNotes) {
            this.completionNotes = completionNotes;
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
        
        public WorkOrderSimplifiedResponse build() {
            return new WorkOrderSimplifiedResponse(
                id, workOrderNumber,
                title, description, status, priority,
                reportId, schoolId, schoolName, assignedToId, assignedToName, companyId,
                scheduledStart, scheduledEnd, actualStart, actualEnd,
                completionPercentage, actualHours,
                verifiedAt, verifiedBy,
                instructions, completionNotes,
                createdAt, updatedAt
            );
        }
    }
}