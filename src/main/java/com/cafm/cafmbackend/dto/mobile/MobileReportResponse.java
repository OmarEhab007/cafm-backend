package com.cafm.cafmbackend.dto.mobile;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Mobile report submission response DTO.
 * 
 * Purpose: Provides response data for mobile report submissions
 * Pattern: Immutable record with comprehensive response structure
 * Java 23: Uses records for DTOs with automatic serialization
 * Architecture: API layer response DTO for mobile consumption
 * Standards: JSON property mapping, structured response format
 */
public record MobileReportResponse(
    @JsonProperty("report_id")
    String reportId,
    
    @JsonProperty("report_number")
    String reportNumber,
    
    @JsonProperty("client_id")
    String clientId,
    
    @JsonProperty("status")
    String status,
    
    @JsonProperty("submission_status")
    SubmissionStatus submissionStatus,
    
    @JsonProperty("created_at")
    LocalDateTime createdAt,
    
    @JsonProperty("estimated_completion")
    LocalDateTime estimatedCompletion,
    
    @JsonProperty("priority_adjusted")
    Boolean priorityAdjusted,
    
    @JsonProperty("auto_assigned")
    Boolean autoAssigned,
    
    @JsonProperty("work_order_created")
    Boolean workOrderCreated,
    
    @JsonProperty("work_order_id")
    String workOrderId,
    
    @JsonProperty("assigned_technicians")
    List<AssignedTechnician> assignedTechnicians,
    
    @JsonProperty("photo_upload_results")
    List<PhotoUploadResult> photoUploadResults,
    
    @JsonProperty("validation_warnings")
    List<ValidationWarning> validationWarnings,
    
    @JsonProperty("sync_info")
    SyncInfo syncInfo,
    
    @JsonProperty("next_actions")
    List<NextAction> nextActions,
    
    @JsonProperty("metadata")
    Map<String, Object> metadata
) {
    
    public enum SubmissionStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        FAILED,
        REQUIRES_APPROVAL,
        DUPLICATE_DETECTED
    }
    
    /**
     * Information about assigned technician.
     */
    public record AssignedTechnician(
        @JsonProperty("technician_id")
        String technicianId,
        
        @JsonProperty("name")
        String name,
        
        @JsonProperty("phone")
        String phone,
        
        @JsonProperty("specialization")
        String specialization,
        
        @JsonProperty("availability")
        String availability,
        
        @JsonProperty("estimated_arrival")
        LocalDateTime estimatedArrival
    ) {}
    
    /**
     * Photo upload result.
     */
    public record PhotoUploadResult(
        @JsonProperty("photo_id")
        String photoId,
        
        @JsonProperty("original_filename")
        String originalFilename,
        
        @JsonProperty("upload_status")
        String uploadStatus,
        
        @JsonProperty("file_url")
        String fileUrl,
        
        @JsonProperty("thumbnail_url")
        String thumbnailUrl,
        
        @JsonProperty("compressed_size")
        Long compressedSize,
        
        @JsonProperty("error_message")
        String errorMessage
    ) {}
    
    /**
     * Validation warning.
     */
    public record ValidationWarning(
        @JsonProperty("field")
        String field,
        
        @JsonProperty("warning_type")
        String warningType,
        
        @JsonProperty("message")
        String message,
        
        @JsonProperty("severity")
        String severity,
        
        @JsonProperty("suggestion")
        String suggestion
    ) {}
    
    /**
     * Sync information.
     */
    public record SyncInfo(
        @JsonProperty("server_version")
        Long serverVersion,
        
        @JsonProperty("sync_token")
        String syncToken,
        
        @JsonProperty("conflicts_detected")
        Boolean conflictsDetected,
        
        @JsonProperty("requires_sync")
        Boolean requiresSync,
        
        @JsonProperty("next_sync_recommended")
        LocalDateTime nextSyncRecommended
    ) {}
    
    /**
     * Next action recommendation.
     */
    public record NextAction(
        @JsonProperty("action_type")
        String actionType,
        
        @JsonProperty("title")
        String title,
        
        @JsonProperty("description")
        String description,
        
        @JsonProperty("priority")
        String priority,
        
        @JsonProperty("due_date")
        LocalDateTime dueDate,
        
        @JsonProperty("action_url")
        String actionUrl
    ) {}
}