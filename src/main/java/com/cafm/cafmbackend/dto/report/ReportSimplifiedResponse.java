package com.cafm.cafmbackend.dto.report;

import com.cafm.cafmbackend.shared.enums.ReportPriority;
import com.cafm.cafmbackend.shared.enums.ReportStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Simplified Report response that matches actual Report entity fields.
 * This replaces complex response DTOs with realistic data from the entity.
 */
public record ReportSimplifiedResponse(
    // Core identifiers
    UUID id,
    String reportNumber,
    
    // Basic information
    String title,
    String description,
    ReportStatus status,
    ReportPriority priority,
    
    // School information
    UUID schoolId,
    String schoolName,
    
    // User information
    UUID supervisorId,
    String supervisorName,
    UUID assignedToId,
    String assignedToName,
    
    // Company information
    UUID companyId,
    
    // Dates
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate reportedDate,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate scheduledDate,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate completedDate,
    
    // Costs
    BigDecimal estimatedCost,
    BigDecimal actualCost,
    
    // Timestamps
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt
) {
    /**
     * Create a response for list views (minimal data)
     */
    public static ReportSimplifiedResponse forList(
        UUID id,
        String reportNumber,
        String title,
        ReportStatus status,
        ReportPriority priority,
        String schoolName,
        LocalDateTime createdAt
    ) {
        return new ReportSimplifiedResponse(
            id, reportNumber,
            title, null, status, priority,
            null, schoolName,
            null, null, null, null,
            null,
            null, null, null,
            null, null,
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
        private String reportNumber;
        private String title;
        private String description;
        private ReportStatus status;
        private ReportPriority priority;
        private UUID schoolId;
        private String schoolName;
        private UUID supervisorId;
        private String supervisorName;
        private UUID assignedToId;
        private String assignedToName;
        private UUID companyId;
        private LocalDate reportedDate;
        private LocalDate scheduledDate;
        private LocalDate completedDate;
        private BigDecimal estimatedCost;
        private BigDecimal actualCost;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public Builder id(UUID id) {
            this.id = id;
            return this;
        }
        
        public Builder reportNumber(String reportNumber) {
            this.reportNumber = reportNumber;
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
        
        public Builder status(ReportStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder priority(ReportPriority priority) {
            this.priority = priority;
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
        
        public Builder reportedDate(LocalDate reportedDate) {
            this.reportedDate = reportedDate;
            return this;
        }
        
        public Builder scheduledDate(LocalDate scheduledDate) {
            this.scheduledDate = scheduledDate;
            return this;
        }
        
        public Builder completedDate(LocalDate completedDate) {
            this.completedDate = completedDate;
            return this;
        }
        
        public Builder estimatedCost(BigDecimal estimatedCost) {
            this.estimatedCost = estimatedCost;
            return this;
        }
        
        public Builder actualCost(BigDecimal actualCost) {
            this.actualCost = actualCost;
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
        
        public ReportSimplifiedResponse build() {
            return new ReportSimplifiedResponse(
                id, reportNumber,
                title, description, status, priority,
                schoolId, schoolName,
                supervisorId, supervisorName, assignedToId, assignedToName,
                companyId,
                reportedDate, scheduledDate, completedDate,
                estimatedCost, actualCost,
                createdAt, updatedAt
            );
        }
    }
}