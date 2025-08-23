package com.cafm.cafmbackend.mappers.report;

import com.cafm.cafmbackend.mappers.config.BaseMapper;
import com.cafm.cafmbackend.data.entity.Report;
import com.cafm.cafmbackend.data.enums.ReportPriority;
import com.cafm.cafmbackend.data.enums.ReportStatus;
import com.cafm.cafmbackend.dto.report.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper implementation for Report entity and DTOs.
 * Note: Report entity has simplified structure compared to DTOs.
 * Many DTO fields are not present in the entity and would need
 * to be stored in separate tables or JSONB columns.
 */
@Component
public class ReportMapperImpl implements BaseMapper<Report, ReportCreateRequest, ReportDetailResponse> {
    
    @Override
    public Report toEntity(ReportCreateRequest request) {
        if (request == null) return null;
        
        Report report = new Report();
        
        // Basic fields
        report.setTitle(request.title());
        report.setDescription(request.description());
        report.setPriority(request.priority());
        
        // Initial status
        report.setStatus(ReportStatus.DRAFT);
        report.setReportedDate(java.time.LocalDate.now());
        
        // Note: Many request fields (category, location, building, floor, roomNumber, 
        // damageAssessment, isUrgent, isSafetyHazard, photoUrls, notes, contactName, 
        // contactPhone) are not present in the Report entity.
        // These would need to be stored in a separate table or JSONB column.
        
        return report;
    }
    
    @Override
    public ReportDetailResponse toResponse(Report entity) {
        if (entity == null) return null;
        
        // Note: Many response fields are not available in the Report entity.
        // This mapper returns null for fields that don't exist in the entity.
        // A complete implementation would need to fetch data from related tables.
        
        return new ReportDetailResponse(
            // Identifiers
            entity.getId(),
            entity.getReportNumber(),
            
            // Basic Information
            entity.getTitle(),
            entity.getDescription(),
            entity.getStatus(),
            entity.getPriority(),
            null, // category - not in entity
            
            // Location Details
            null, // location - not in entity
            null, // building - not in entity
            null, // floor - not in entity
            null, // roomNumber - not in entity
            
            // School Information
            entity.getSchool() != null ? entity.getSchool().getId() : null,
            entity.getSchool() != null ? entity.getSchool().getName() : null,
            entity.getSchool() != null ? entity.getSchool().getCode() : null,
            entity.getSchool() != null ? entity.getSchool().getAddress() : null,
            entity.getSchool() != null ? entity.getSchool().getCity() : null, // Using city as district
            
            // Supervisor Information
            entity.getSupervisor() != null ? entity.getSupervisor().getId() : null,
            entity.getSupervisor() != null ? entity.getSupervisor().getFullName() : null,
            entity.getSupervisor() != null ? entity.getSupervisor().getEmail() : null,
            entity.getSupervisor() != null ? entity.getSupervisor().getPhone() : null,
            
            // Assignment Information
            entity.getAssignedTo() != null ? entity.getAssignedTo().getId() : null,
            entity.getAssignedTo() != null ? entity.getAssignedTo().getFullName() : null,
            entity.getAssignedTo() != null ? entity.getAssignedTo().getEmail() : null,
            entity.getAssignedTo() != null ? entity.getAssignedTo().getPhone() : null,
            entity.getAssignedTo() != null && entity.getAssignedTo().getSpecialization() != null ? entity.getAssignedTo().getSpecialization().toString() : null,
            
            // Damage Assessment
            null, // damageAssessment - not in entity
            null, // isUrgent - not in entity
            null, // isSafetyHazard - not in entity
            
            // Photos and Attachments
            List.of(), // photos - would need to fetch from attachments table
            List.of(), // documents - would need to fetch from attachments table
            
            // Dates
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getReportedDate() != null ? entity.getReportedDate().atStartOfDay() : null,
            entity.getScheduledDate() != null ? entity.getScheduledDate().atStartOfDay() : null,
            entity.getCompletedDate() != null ? entity.getCompletedDate().atStartOfDay() : null,
            
            // Cost Information
            entity.getEstimatedCost(),
            entity.getActualCost(),
            null, // costNotes - not in entity
            
            // Work Order Information
            null, // workOrder - would need to fetch from work_orders table
            
            // Review Information
            null, // reviewedBy - not in entity
            null, // reviewedAt - not in entity
            null, // reviewNotes - not in entity
            null, // rejectionReason - not in entity
            
            // Completion Information
            null, // completionNotes - not in entity
            null, // verifiedBy - not in entity
            null, // verifiedAt - not in entity
            
            // Contact Information
            null, // contactName - not in entity
            null, // contactPhone - not in entity
            
            // Additional Information
            null, // notes - not in entity
            List.of(), // tags - not in entity
            
            // Timeline/History
            List.of(), // activities - would need to fetch from audit log
            
            // Statistics
            calculateDaysOpen(entity),
            isOverdue(entity),
            0 // commentsCount - would need to fetch from comments table
        );
    }
    
    /**
     * Convert to list response for overview.
     * Note: This is a simpler DTO that should be created for list views.
     */
    public ReportListResponse toListResponse(Report entity) {
        if (entity == null) return null;
        
        // ReportListResponse constructor needs many more fields than originally expected
        // For now, returning null. A proper ReportListResponse DTO should be created
        // with fewer fields for list views.
        return null;
    }
    
    /**
     * Update entity from update request.
     */
    public void updateEntity(ReportUpdateRequest request, Report entity) {
        if (request == null || entity == null) return;
        
        // Basic fields
        if (request.title() != null) {
            entity.setTitle(request.title());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.priority() != null) {
            entity.setPriority(request.priority());
        }
        
        // Note: Many update request fields don't exist in the Report entity
    }
    
    /**
     * Update entity from review request.
     */
    public void reviewEntity(ReportReviewRequest request, Report entity) {
        if (request == null || entity == null) return;
        
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        
        if (request.newPriority() != null && Boolean.TRUE.equals(request.overridePriority())) {
            try {
                entity.setPriority(ReportPriority.valueOf(request.newPriority().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Handle invalid priority
            }
        }
        
        if (request.estimatedCost() != null) {
            entity.setEstimatedCost(request.estimatedCost());
        }
        
        if (request.scheduledDate() != null) {
            entity.setScheduledDate(request.scheduledDate());
        }
        
        // Note: Review notes and other fields would need to be stored elsewhere
    }
    
    /**
     * Calculate days since report was created
     */
    private Integer calculateDaysOpen(Report entity) {
        if (entity.getCreatedAt() == null) return 0;
        if (entity.getStatus() == ReportStatus.COMPLETED || entity.getStatus() == ReportStatus.CANCELLED) {
            if (entity.getCompletedDate() != null) {
                return (int) java.time.temporal.ChronoUnit.DAYS.between(
                    entity.getCreatedAt().toLocalDate(),
                    entity.getCompletedDate()
                );
            }
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(
            entity.getCreatedAt().toLocalDate(),
            java.time.LocalDate.now()
        );
    }
    
    /**
     * Check if report is overdue based on scheduled date
     */
    private Boolean isOverdue(Report entity) {
        if (entity.getScheduledDate() == null) return false;
        if (entity.getStatus() == ReportStatus.COMPLETED || entity.getStatus() == ReportStatus.CANCELLED) {
            return false;
        }
        return entity.getScheduledDate().isBefore(java.time.LocalDate.now());
    }
}