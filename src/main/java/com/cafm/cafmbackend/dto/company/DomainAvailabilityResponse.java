package com.cafm.cafmbackend.dto.company;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Domain availability response DTO.
 * 
 * Purpose: Reports domain and subdomain availability for company registration
 * Pattern: Validation response with detailed availability information
 * Java 23: Record with comprehensive domain check results
 * Architecture: Multi-tenant domain validation for unique company identification
 */
@Schema(description = "Domain and subdomain availability check response")
public record DomainAvailabilityResponse(
    @Schema(description = "Checked domain", example = "acme.com")
    String domain,
    
    @Schema(description = "Is domain available", example = "true")
    Boolean isDomainAvailable,
    
    @Schema(description = "Domain availability message", example = "Domain is available")
    String domainMessage,
    
    @Schema(description = "Checked subdomain", example = "acme")
    String subdomain,
    
    @Schema(description = "Is subdomain available", example = "false")
    Boolean isSubdomainAvailable,
    
    @Schema(description = "Subdomain availability message", example = "Subdomain is already taken")
    String subdomainMessage,
    
    @Schema(description = "Overall availability (both domain and subdomain)", example = "false")
    Boolean isAvailable,
    
    @Schema(description = "List of alternative subdomains if requested one is taken")
    List<String> suggestedSubdomains,
    
    @Schema(description = "Validation errors or warnings")
    List<String> validationMessages
) {
    
    /**
     * Create a response for available domain and subdomain.
     */
    public static DomainAvailabilityResponse available(String domain, String subdomain) {
        return new DomainAvailabilityResponse(
            domain,
            true,
            "Domain is available",
            subdomain,
            true,
            "Subdomain is available",
            true,
            List.of(),
            List.of()
        );
    }
    
    /**
     * Create a response for unavailable domain.
     */
    public static DomainAvailabilityResponse domainUnavailable(String domain, String subdomain) {
        return new DomainAvailabilityResponse(
            domain,
            false,
            "Domain is already registered to another company",
            subdomain,
            null,
            null,
            false,
            List.of(),
            List.of("Domain is already in use")
        );
    }
    
    /**
     * Create a response for unavailable subdomain with suggestions.
     */
    public static DomainAvailabilityResponse subdomainUnavailable(
            String domain, String subdomain, List<String> suggestions) {
        return new DomainAvailabilityResponse(
            domain,
            domain != null ? true : null,
            domain != null ? "Domain is available" : null,
            subdomain,
            false,
            "Subdomain is already taken",
            false,
            suggestions,
            List.of("Subdomain is already in use")
        );
    }
    
    /**
     * Create a response for invalid domain format.
     */
    public static DomainAvailabilityResponse invalidDomain(String domain, String subdomain, String error) {
        return new DomainAvailabilityResponse(
            domain,
            false,
            "Domain format is invalid",
            subdomain,
            null,
            null,
            false,
            List.of(),
            List.of(error)
        );
    }
    
    /**
     * Create a response for invalid subdomain format.
     */
    public static DomainAvailabilityResponse invalidSubdomain(String domain, String subdomain, String error) {
        return new DomainAvailabilityResponse(
            domain,
            domain != null ? true : null,
            domain != null ? "Domain is available" : null,
            subdomain,
            false,
            "Subdomain format is invalid",
            false,
            List.of(),
            List.of(error)
        );
    }
    
    /**
     * Create a response when neither domain nor subdomain is provided.
     */
    public static DomainAvailabilityResponse nothingToCheck() {
        return new DomainAvailabilityResponse(
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            List.of(),
            List.of("Either domain or subdomain must be provided for checking")
        );
    }
    
    /**
     * Check if both domain and subdomain are available.
     */
    public boolean areBothAvailable() {
        return Boolean.TRUE.equals(isDomainAvailable) && Boolean.TRUE.equals(isSubdomainAvailable);
    }
    
    /**
     * Get the primary reason for unavailability.
     */
    public String getUnavailabilityReason() {
        if (Boolean.FALSE.equals(isDomainAvailable) && Boolean.FALSE.equals(isSubdomainAvailable)) {
            return "Both domain and subdomain are unavailable";
        } else if (Boolean.FALSE.equals(isDomainAvailable)) {
            return "Domain is unavailable";
        } else if (Boolean.FALSE.equals(isSubdomainAvailable)) {
            return "Subdomain is unavailable";
        } else if (!validationMessages.isEmpty()) {
            return validationMessages.get(0);
        }
        return null;
    }
}