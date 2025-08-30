package com.cafm.cafmbackend.dto.company;

import com.cafm.cafmbackend.shared.enums.CompanyStatus;
import com.cafm.cafmbackend.shared.enums.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Company response DTO.
 * 
 * Purpose: Represents company information for API responses
 * Pattern: Immutable record for thread-safe data transfer
 * Java 23: Record with validation annotations and OpenAPI documentation
 */
@Schema(description = "Company information response")
public record CompanyResponse(
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
    
    @Schema(description = "Contact phone", example = "+966501234567")
    String contactPhone,
    
    @Schema(description = "Primary contact name", example = "John Doe")
    String primaryContactName,
    
    @Schema(description = "Industry", example = "Education")
    String industry,
    
    @Schema(description = "Country", example = "Saudi Arabia")
    String country,
    
    @Schema(description = "City", example = "Riyadh")
    String city,
    
    @Schema(description = "Address")
    String address,
    
    @Schema(description = "Postal code", example = "12345")
    String postalCode,
    
    @Schema(description = "Tax number")
    String taxNumber,
    
    @Schema(description = "Commercial registration number")
    String commercialRegistration,
    
    @Schema(description = "Timezone", example = "Asia/Riyadh")
    String timezone,
    
    @Schema(description = "Locale", example = "ar_SA")
    String locale,
    
    @Schema(description = "Currency", example = "SAR")
    String currency,
    
    @Schema(description = "Subscription plan")
    SubscriptionPlan subscriptionPlan,
    
    @Schema(description = "Subscription start date")
    LocalDate subscriptionStartDate,
    
    @Schema(description = "Subscription end date")
    LocalDate subscriptionEndDate,
    
    @Schema(description = "Maximum number of users", example = "50")
    Integer maxUsers,
    
    @Schema(description = "Maximum number of schools", example = "10")
    Integer maxSchools,
    
    @Schema(description = "Maximum number of supervisors", example = "20")
    Integer maxSupervisors,
    
    @Schema(description = "Maximum number of technicians", example = "30")
    Integer maxTechnicians,
    
    @Schema(description = "Maximum storage in GB", example = "100")
    Integer maxStorageGb,
    
    @Schema(description = "Company status")
    CompanyStatus status,
    
    @Schema(description = "Is company active", example = "true")
    Boolean isActive,
    
    @Schema(description = "Company settings (JSON)")
    String settings,
    
    @Schema(description = "Company features (JSON)")
    String features,
    
    @Schema(description = "Data classification", example = "internal")
    String dataClassification,
    
    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt,
    
    @Schema(description = "Last update timestamp")
    LocalDateTime updatedAt,
    
    @Schema(description = "Current user count", example = "25")
    Long currentUserCount,
    
    @Schema(description = "Current school count", example = "5")
    Long currentSchoolCount,
    
    @Schema(description = "Is subscription active", example = "true")
    Boolean isSubscriptionActive
) {
    

    
    /**
     * Check if company has active subscription.
     */
    public boolean hasActiveSubscription() {
        return Boolean.TRUE.equals(isSubscriptionActive) && 
               subscriptionEndDate != null && 
               subscriptionEndDate.isAfter(LocalDate.now());
    }
    
    /**
     * Get subscription days remaining.
     */
    public long getDaysUntilExpiry() {
        if (subscriptionEndDate == null) {
            return 0;
        }
        return LocalDate.now().until(subscriptionEndDate, java.time.temporal.ChronoUnit.DAYS);
    }
}