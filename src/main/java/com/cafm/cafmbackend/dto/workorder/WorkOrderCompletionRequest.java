package com.cafm.cafmbackend.dto.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Work order completion request DTO.
 * 
 * Purpose: Validates and transfers work order completion data
 * Pattern: Immutable record DTO with completion validation
 * Java 23: Record with completion-specific validation
 * Architecture: DTO for work order completion with cost tracking
 * Standards: Bean validation with completion requirements
 */
@Schema(description = "Work order completion request")
public record WorkOrderCompletionRequest(
    @NotBlank(message = "Completion notes are required")
    @Size(max = 2000, message = "Completion notes cannot exceed 2000 characters")
    @Schema(description = "Completion notes describing work performed", required = true)
    String completionNotes,
    
    @Schema(description = "Actual cost of work performed", example = "175.50")
    @DecimalMin(value = "0.0", message = "Actual cost cannot be negative")
    @DecimalMax(value = "99999.99", message = "Actual cost cannot exceed 99,999.99")
    @Digits(integer = 5, fraction = 2, message = "Actual cost must have at most 5 integer digits and 2 decimal places")
    BigDecimal actualCost,
    
    @Schema(description = "Work quality rating (1-5)", example = "4")
    @Min(value = 1, message = "Quality rating must be between 1 and 5")
    @Max(value = 5, message = "Quality rating must be between 1 and 5")
    Integer qualityRating,
    
    @Size(max = 1000, message = "Recommendations cannot exceed 1000 characters")
    @Schema(description = "Future maintenance recommendations")
    String recommendations,
    
    @Schema(description = "Work was completed satisfactorily", example = "true")
    Boolean workSatisfactory,
    
    @Schema(description = "Additional follow-up work required", example = "false")
    Boolean followUpRequired,
    
    @Size(max = 1000, message = "Follow-up notes cannot exceed 1000 characters")
    @Schema(description = "Follow-up work description")
    String followUpNotes
) {
    
    /**
     * Create basic completion request.
     */
    public static WorkOrderCompletionRequest basic(String completionNotes) {
        return new WorkOrderCompletionRequest(completionNotes, null, null, null, true, false, null);
    }
    
    /**
     * Create completion request with cost.
     */
    public static WorkOrderCompletionRequest withCost(String completionNotes, BigDecimal actualCost) {
        return new WorkOrderCompletionRequest(completionNotes, actualCost, null, null, true, false, null);
    }
    
    /**
     * Create completion request with follow-up.
     */
    public static WorkOrderCompletionRequest withFollowUp(String completionNotes, String followUpNotes) {
        return new WorkOrderCompletionRequest(completionNotes, null, null, null, true, true, followUpNotes);
    }
    
    /**
     * Create completion request with quality rating.
     */
    public static WorkOrderCompletionRequest withQuality(String completionNotes, Integer qualityRating, BigDecimal actualCost) {
        return new WorkOrderCompletionRequest(completionNotes, actualCost, qualityRating, null, true, false, null);
    }
    
    /**
     * Check if cost information is provided.
     */
    public boolean hasCostInfo() {
        return actualCost != null && actualCost.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if quality rating is provided.
     */
    public boolean hasQualityRating() {
        return qualityRating != null;
    }
    
    /**
     * Check if recommendations are provided.
     */
    public boolean hasRecommendations() {
        return recommendations != null && !recommendations.trim().isEmpty();
    }
    
    /**
     * Check if follow-up work is required.
     */
    public boolean needsFollowUp() {
        return followUpRequired != null && followUpRequired;
    }
    
    /**
     * Check if work was completed satisfactorily.
     */
    public boolean isSatisfactory() {
        return workSatisfactory == null || workSatisfactory; // Default to true if not specified
    }
    
    /**
     * Check if this is a high-cost completion.
     */
    public boolean isHighCost() {
        return hasCostInfo() && actualCost.compareTo(BigDecimal.valueOf(1000)) > 0;
    }
    
    /**
     * Check if this completion has quality issues.
     */
    public boolean hasQualityIssues() {
        return (qualityRating != null && qualityRating < 3) || !isSatisfactory();
    }
    
    /**
     * Get effective quality rating (default to 3 if not provided).
     */
    public int getEffectiveQualityRating() {
        return qualityRating != null ? qualityRating : 3;
    }
}