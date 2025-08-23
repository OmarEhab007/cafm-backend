package com.cafm.cafmbackend.dto.report;

import com.cafm.cafmbackend.data.enums.ReportPriority;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

/**
 * DTO for supervisors to create maintenance reports from mobile app.
 * Focuses on essential information needed to report an issue.
 */
public record ReportCreateRequest(
    @NotNull(message = "School ID is required")
    UUID schoolId,
    
    @NotNull(message = "Supervisor ID is required")
    UUID supervisorId,
    
    @NotNull(message = "Company ID is required")
    UUID companyId,
    
    @NotBlank(message = "Report title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    String title,
    
    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    String description,
    
    @NotNull(message = "Priority is required")
    ReportPriority priority,
    
    // Category of maintenance (electrical, plumbing, hvac, general, etc.)
    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category cannot exceed 50 characters")
    String category,
    
    // Specific location within school
    @Size(max = 255, message = "Location cannot exceed 255 characters")
    String location,
    
    // Building/Floor information
    String building,
    String floor,
    String roomNumber,
    
    // Damage assessment
    @Size(max = 500, message = "Damage assessment cannot exceed 500 characters")
    String damageAssessment,
    
    // Safety concerns
    Boolean isUrgent,
    Boolean isSafetyHazard,
    
    // Photos as base64 strings or URLs (mobile app will upload to Cloudinary)
    List<String> photoUrls,
    
    // Additional notes
    String notes,
    
    // Contact person at school (if different from supervisor)
    String contactName,
    String contactPhone
) {
    /**
     * Constructor with minimal required fields for quick reporting
     */
    public ReportCreateRequest(
        UUID schoolId,
        UUID supervisorId,
        UUID companyId,
        String title,
        String description,
        ReportPriority priority,
        String category
    ) {
        this(
            schoolId, supervisorId, companyId, title, description, priority, category,
            null, null, null, null, null,
            false, false, null, null, null, null
        );
    }
    
    /**
     * Factory method for urgent safety reports
     */
    public static ReportCreateRequest urgentSafetyReport(
        UUID schoolId,
        UUID supervisorId,
        UUID companyId,
        String title,
        String description,
        String category,
        String location
    ) {
        return new ReportCreateRequest(
            schoolId, supervisorId, companyId, title, description, ReportPriority.URGENT, category,
            location, null, null, null, null,
            true, true, null, null, null, null
        );
    }
    
    /**
     * Validate that urgent reports have location details
     */
    public boolean isValid() {
        if (Boolean.TRUE.equals(isUrgent) || Boolean.TRUE.equals(isSafetyHazard)) {
            return location != null && !location.isBlank();
        }
        return true;
    }
}