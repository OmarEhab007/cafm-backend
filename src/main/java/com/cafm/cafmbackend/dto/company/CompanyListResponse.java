package com.cafm.cafmbackend.dto.company;

import com.cafm.cafmbackend.shared.enums.CompanyStatus;
import com.cafm.cafmbackend.shared.enums.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Company list response DTO for paginated results.
 * 
 * Purpose: Lightweight company representation for list views
 * Pattern: Simplified record with essential fields only
 * Java 23: Record optimized for memory efficiency in large datasets
 */
@Schema(description = "Company list item response")
public record CompanyListResponse(
    @Schema(description = "Company ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID id,
    
    @Schema(description = "Company name", example = "Acme Corporation")
    String name,
    
    @Schema(description = "Display name", example = "Acme Corp")
    String displayName,
    
    @Schema(description = "Company domain", example = "acme.com")
    String domain,
    
    @Schema(description = "Company subdomain", example = "acme")
    String subdomain,
    
    @Schema(description = "Contact email", example = "contact@acme.com")
    String contactEmail,
    
    @Schema(description = "Industry", example = "Education")
    String industry,
    
    @Schema(description = "Country", example = "Saudi Arabia")
    String country,
    
    @Schema(description = "City", example = "Riyadh")
    String city,
    
    @Schema(description = "Subscription plan")
    SubscriptionPlan subscriptionPlan,
    
    @Schema(description = "Subscription end date")
    LocalDate subscriptionEndDate,
    
    @Schema(description = "Company status")
    CompanyStatus status,
    
    @Schema(description = "Is company active", example = "true")
    Boolean isActive,
    
    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt,
    
    @Schema(description = "Current user count", example = "25")
    Long currentUserCount,
    
    @Schema(description = "Current school count", example = "5")
    Long currentSchoolCount,
    
    @Schema(description = "Maximum number of users", example = "50")
    Integer maxUsers,
    
    @Schema(description = "Maximum number of schools", example = "10")
    Integer maxSchools,
    
    @Schema(description = "Is subscription active", example = "true")
    Boolean isSubscriptionActive
) {
    
    /**
     * Get display name with fallback to name.
     */
    public String getEffectiveDisplayName() {
        return displayName != null && !displayName.trim().isEmpty() 
            ? displayName 
            : name;
    }
    
    /**
     * Check if subscription is expiring soon (within 30 days).
     */
    public boolean isSubscriptionExpiringSoon() {
        if (subscriptionEndDate == null) {
            return false;
        }
        LocalDate in30Days = LocalDate.now().plusDays(30);
        return subscriptionEndDate.isBefore(in30Days) && subscriptionEndDate.isAfter(LocalDate.now());
    }
    
    /**
     * Check if subscription has expired.
     */
    public boolean isSubscriptionExpired() {
        return subscriptionEndDate != null && subscriptionEndDate.isBefore(LocalDate.now());
    }
    
    /**
     * Get user utilization percentage.
     */
    public int getUserUtilizationPercentage() {
        if (maxUsers == null || maxUsers == 0 || currentUserCount == null) {
            return 0;
        }
        return (int) Math.round((currentUserCount.doubleValue() / maxUsers) * 100);
    }
    
    /**
     * Get school utilization percentage.
     */
    public int getSchoolUtilizationPercentage() {
        if (maxSchools == null || maxSchools == 0 || currentSchoolCount == null) {
            return 0;
        }
        return (int) Math.round((currentSchoolCount.doubleValue() / maxSchools) * 100);
    }
    
    /**
     * Check if resource limits are being approached (>80% utilization).
     */
    public boolean isApproachingLimits() {
        return getUserUtilizationPercentage() > 80 || getSchoolUtilizationPercentage() > 80;
    }
}