package com.cafm.cafmbackend.dto.company;

import com.cafm.cafmbackend.data.enums.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Company subscription update request DTO.
 * 
 * Purpose: Validates and transfers subscription plan changes
 * Pattern: Specialized DTO for subscription management operations
 * Java 23: Record with validation for subscription updates
 * Architecture: Domain-specific request for multi-tenant subscription control
 */
@Schema(description = "Company subscription update request")
public record CompanySubscriptionUpdateRequest(
    @NotNull(message = "Subscription plan is required")
    @Schema(description = "New subscription plan", required = true)
    SubscriptionPlan subscriptionPlan,
    
    @Future(message = "Subscription end date must be in the future")
    @Schema(description = "New subscription end date")
    LocalDate subscriptionEndDate,
    
    @Min(value = 1, message = "Max users must be at least 1")
    @Max(value = 10000, message = "Max users cannot exceed 10000")
    @Schema(description = "Maximum number of users", example = "100")
    Integer maxUsers,
    
    @Min(value = 1, message = "Max schools must be at least 1")
    @Max(value = 1000, message = "Max schools cannot exceed 1000")
    @Schema(description = "Maximum number of schools", example = "20")
    Integer maxSchools,
    
    @Min(value = 1, message = "Max supervisors must be at least 1")
    @Max(value = 1000, message = "Max supervisors cannot exceed 1000")
    @Schema(description = "Maximum number of supervisors", example = "50")
    Integer maxSupervisors,
    
    @Min(value = 1, message = "Max technicians must be at least 1")
    @Max(value = 5000, message = "Max technicians cannot exceed 5000")
    @Schema(description = "Maximum number of technicians", example = "100")
    Integer maxTechnicians,
    
    @Min(value = 1, message = "Max storage must be at least 1 GB")
    @Max(value = 10000, message = "Max storage cannot exceed 10TB")
    @Schema(description = "Maximum storage in GB", example = "500")
    Integer maxStorageGb,
    
    @Schema(description = "Auto-renewal enabled", example = "true")
    Boolean autoRenew,
    
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    @Schema(description = "Update notes or reason")
    String notes
) {
    
    /**
     * Check if any resource limits are being updated.
     */
    public boolean hasResourceLimitUpdates() {
        return maxUsers != null || maxSchools != null || 
               maxSupervisors != null || maxTechnicians != null || 
               maxStorageGb != null;
    }
    
    /**
     * Check if this is a plan upgrade (comparing by ordinal values).
     */
    public boolean isPlanUpgrade(SubscriptionPlan currentPlan) {
        if (subscriptionPlan == null || currentPlan == null) {
            return false;
        }
        return subscriptionPlan.ordinal() > currentPlan.ordinal();
    }
    
    /**
     * Check if this extends the subscription period.
     */
    public boolean extendsSubscription(LocalDate currentEndDate) {
        if (subscriptionEndDate == null) {
            return false;
        }
        return currentEndDate == null || subscriptionEndDate.isAfter(currentEndDate);
    }
}