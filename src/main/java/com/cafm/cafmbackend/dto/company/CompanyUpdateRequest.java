package com.cafm.cafmbackend.dto.company;

import com.cafm.cafmbackend.shared.enums.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Company update request DTO.
 * 
 * Purpose: Validates and transfers company update data
 * Pattern: Immutable record with validation for partial updates
 * Java 23: Record with optional fields for PATCH-style updates
 */
@Schema(description = "Company update request")
public record CompanyUpdateRequest(
    @Size(max = 255, message = "Company name cannot exceed 255 characters")
    @Schema(description = "Company name", example = "Acme Corporation")
    String name,
    
    @Size(max = 255, message = "Display name cannot exceed 255 characters")
    @Schema(description = "Display name", example = "Acme Corp")
    String displayName,
    
    @Pattern(regexp = "^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", 
             message = "Domain must be a valid domain format")
    @Schema(description = "Company domain", example = "acme.com")
    String domain,
    
    @Pattern(regexp = "^[a-z0-9-]+$", 
             message = "Subdomain can only contain lowercase letters, numbers, and hyphens")
    @Size(max = 100, message = "Subdomain cannot exceed 100 characters")
    @Schema(description = "Company subdomain", example = "acme")
    String subdomain,
    
    @Email(message = "Contact email must be valid")
    @Schema(description = "Contact email", example = "contact@acme.com")
    String contactEmail,
    
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Contact phone must be valid")
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
    
    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    @Schema(description = "Postal code", example = "12345")
    String postalCode,
    
    @Size(max = 50, message = "Tax number cannot exceed 50 characters")
    @Schema(description = "Tax number")
    String taxNumber,
    
    @Size(max = 50, message = "Commercial registration cannot exceed 50 characters")
    @Schema(description = "Commercial registration number")
    String commercialRegistration,
    
    @Schema(description = "Timezone", example = "Asia/Riyadh")
    String timezone,
    
    @Schema(description = "Locale", example = "ar_SA")
    String locale,
    
    @Size(max = 3, message = "Currency code must be 3 characters")
    @Schema(description = "Currency", example = "SAR")
    String currency,
    
    @Schema(description = "Subscription plan")
    SubscriptionPlan subscriptionPlan,
    
    @Min(value = 1, message = "Max users must be at least 1")
    @Max(value = 10000, message = "Max users cannot exceed 10000")
    @Schema(description = "Maximum number of users", example = "50")
    Integer maxUsers,
    
    @Min(value = 1, message = "Max schools must be at least 1")
    @Max(value = 1000, message = "Max schools cannot exceed 1000")
    @Schema(description = "Maximum number of schools", example = "10")
    Integer maxSchools,
    
    @Min(value = 1, message = "Max supervisors must be at least 1")
    @Max(value = 1000, message = "Max supervisors cannot exceed 1000")
    @Schema(description = "Maximum number of supervisors", example = "20")
    Integer maxSupervisors,
    
    @Min(value = 1, message = "Max technicians must be at least 1")
    @Max(value = 5000, message = "Max technicians cannot exceed 5000")
    @Schema(description = "Maximum number of technicians", example = "30")
    Integer maxTechnicians,
    
    @Min(value = 1, message = "Max storage must be at least 1 GB")
    @Max(value = 10000, message = "Max storage cannot exceed 10TB")
    @Schema(description = "Maximum storage in GB", example = "100")
    Integer maxStorageGb,
    
    @Schema(description = "Company settings (JSON)")
    String settings,
    
    @Schema(description = "Data classification", example = "internal")
    String dataClassification
) {
    
    /**
     * Check if any field is provided for update.
     */
    public boolean hasUpdates() {
        return name != null || displayName != null || domain != null || subdomain != null ||
               contactEmail != null || contactPhone != null || primaryContactName != null ||
               industry != null || country != null || city != null || address != null ||
               postalCode != null || taxNumber != null || commercialRegistration != null ||
               timezone != null || locale != null || currency != null || subscriptionPlan != null ||
               maxUsers != null || maxSchools != null || maxSupervisors != null ||
               maxTechnicians != null || maxStorageGb != null || settings != null ||
               dataClassification != null;
    }
    
    /**
     * Check if this is a subscription-related update.
     */
    public boolean isSubscriptionUpdate() {
        return subscriptionPlan != null || maxUsers != null || maxSchools != null ||
               maxSupervisors != null || maxTechnicians != null || maxStorageGb != null;
    }
    
    /**
     * Check if this is a contact information update.
     */
    public boolean isContactUpdate() {
        return contactEmail != null || contactPhone != null || primaryContactName != null;
    }
    
    /**
     * Check if this is a location information update.
     */
    public boolean isLocationUpdate() {
        return country != null || city != null || address != null || postalCode != null ||
               timezone != null || locale != null || currency != null;
    }
}