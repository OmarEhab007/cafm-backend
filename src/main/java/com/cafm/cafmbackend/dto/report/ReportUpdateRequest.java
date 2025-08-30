package com.cafm.cafmbackend.dto.report;

import com.cafm.cafmbackend.shared.enums.ReportPriority;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO for supervisors to update draft or rejected reports.
 * All fields are optional - only provided fields will be updated.
 */
public record ReportUpdateRequest(
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    String title,
    
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    String description,
    
    ReportPriority priority,
    
    @Size(max = 50, message = "Category cannot exceed 50 characters")
    String category,
    
    // Location details
    @Size(max = 255, message = "Location cannot exceed 255 characters")
    String location,
    String building,
    String floor,
    String roomNumber,
    
    // Damage assessment
    @Size(max = 500, message = "Damage assessment cannot exceed 500 characters")
    String damageAssessment,
    
    // Safety concerns
    Boolean isUrgent,
    Boolean isSafetyHazard,
    
    // Photos to add (existing photos remain unless explicitly removed)
    List<String> addPhotoUrls,
    
    // Photo IDs to remove
    List<String> removePhotoIds,
    
    // Additional notes
    String notes,
    
    // Contact person
    String contactName,
    String contactPhone
) {
    /**
     * Check if this update has any changes
     */
    public boolean hasChanges() {
        return title != null ||
               description != null ||
               priority != null ||
               category != null ||
               location != null ||
               building != null ||
               floor != null ||
               roomNumber != null ||
               damageAssessment != null ||
               isUrgent != null ||
               isSafetyHazard != null ||
               (addPhotoUrls != null && !addPhotoUrls.isEmpty()) ||
               (removePhotoIds != null && !removePhotoIds.isEmpty()) ||
               notes != null ||
               contactName != null ||
               contactPhone != null;
    }
    
    /**
     * Create update request for priority change only
     */
    public static ReportUpdateRequest priorityUpdate(ReportPriority newPriority) {
        return new ReportUpdateRequest(
            null, null, newPriority, null,
            null, null, null, null, null,
            null, null, null, null, null,
            null, null
        );
    }
    
    /**
     * Create update request for adding photos
     */
    public static ReportUpdateRequest addPhotos(List<String> photoUrls) {
        return new ReportUpdateRequest(
            null, null, null, null,
            null, null, null, null, null,
            null, null, photoUrls, null, null,
            null, null
        );
    }
    
    /**
     * Create update request for safety escalation
     */
    public static ReportUpdateRequest escalateToSafety(String damageAssessment) {
        return new ReportUpdateRequest(
            null, null, ReportPriority.URGENT, null,
            null, null, null, null, damageAssessment,
            true, true, null, null, null,
            null, null
        );
    }
}