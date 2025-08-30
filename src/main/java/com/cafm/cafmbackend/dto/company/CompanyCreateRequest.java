package com.cafm.cafmbackend.dto.company;

import com.cafm.cafmbackend.shared.enums.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Company creation request DTO.
 * 
 * Purpose: Validates and transfers company creation data
 * Pattern: Immutable record with comprehensive validation
 * Java 23: Record with Bean Validation annotations
 */
@Schema(description = "Company creation request")
public record CompanyCreateRequest(
    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name cannot exceed 255 characters")
    @Schema(description = "Company name", example = "Acme Corporation", required = true)
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
    
    @NotNull(message = "Country is required")
    @Schema(description = "Country", example = "Saudi Arabia", required = true)
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
    
    @NotNull(message = "Subscription plan is required")
    @Schema(description = "Subscription plan", required = true)
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
     * Get effective display name (falls back to name if not provided).
     */
    public String getEffectiveDisplayName() {
        return displayName != null && !displayName.trim().isEmpty() 
            ? displayName.trim() 
            : name;
    }
    
    /**
     * Get effective timezone (falls back to default if not provided).
     */
    public String getEffectiveTimezone() {
        return timezone != null && !timezone.trim().isEmpty() 
            ? timezone 
            : "Asia/Riyadh";
    }
    
    /**
     * Get effective locale (falls back to default if not provided).
     */
    public String getEffectiveLocale() {
        return locale != null && !locale.trim().isEmpty() 
            ? locale 
            : "ar_SA";
    }
    
    /**
     * Get effective currency (falls back to default if not provided).
     */
    public String getEffectiveCurrency() {
        return currency != null && !currency.trim().isEmpty() 
            ? currency 
            : "SAR";
    }
    
    /**
     * Get effective country (falls back to default if not provided).
     */
    public String getEffectiveCountry() {
        return country != null && !country.trim().isEmpty() 
            ? country 
            : "Saudi Arabia";
    }
}