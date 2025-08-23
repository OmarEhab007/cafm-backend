package com.cafm.cafmbackend.controller;

import com.cafm.cafmbackend.data.entity.Company;
import com.cafm.cafmbackend.data.entity.User;
import com.cafm.cafmbackend.dto.company.*;
import com.cafm.cafmbackend.service.CompanyService;
import com.cafm.cafmbackend.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for company (tenant) management operations.
 * 
 * Purpose: Manages multi-tenant companies in the CAFM system
 * Pattern: RESTful API with comprehensive CRUD and business operations
 * Java 23: Modern controller with security annotations and validation
 * Architecture: Multi-tenant aware with proper security boundaries
 */
@RestController
@RequestMapping("/api/v1/companies")
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Companies", description = "Multi-tenant company management endpoints")
public class CompanyController {
    
    private static final Logger logger = LoggerFactory.getLogger(CompanyController.class);
    
    private final CompanyService companyService;
    private final CurrentUserService currentUserService;
    
    public CompanyController(CompanyService companyService, CurrentUserService currentUserService) {
        this.companyService = companyService;
        this.currentUserService = currentUserService;
    }
    
    /**
     * Get all companies with pagination (super admin only).
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get all companies", description = "Get paginated list of all companies (super admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Companies retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not authorized - super admin required")
    })
    public ResponseEntity<Page<CompanyListResponse>> getAllCompanies(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) @Parameter(description = "Filter by company status") String status,
            @RequestParam(required = false) @Parameter(description = "Filter by subscription plan") String subscriptionPlan,
            @RequestParam(required = false) @Parameter(description = "Filter by country") String country,
            @RequestParam(required = false) @Parameter(description = "Search by name, domain, or email") String search) {
        
        logger.debug("Get all companies request with page: {}, size: {}", 
                    pageable.getPageNumber(), pageable.getPageSize());
        
        Page<CompanyListResponse> companies = companyService.getAllCompanies(
            pageable, status, subscriptionPlan, country, search);
        
        return ResponseEntity.ok(companies);
    }
    
    /**
     * Get company by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasAnyRole('ADMIN', 'SUPERVISOR') and @companyService.belongsToCompany(#id, authentication.principal.username))")
    @Operation(summary = "Get company by ID", description = "Get detailed company information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Company retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Company not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<CompanyResponse> getCompanyById(
            @PathVariable @Parameter(description = "Company ID") UUID id) {
        
        logger.debug("Get company by ID: {}", id);
        
        CompanyResponse company = companyService.getCompanyByIdAsDto(id);
        return ResponseEntity.ok(company);
    }
    
    /**
     * Create a new company (super admin only).
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create company", description = "Create a new company (super admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Company created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid company data"),
        @ApiResponse(responseCode = "409", description = "Domain or subdomain already exists"),
        @ApiResponse(responseCode = "403", description = "Not authorized - super admin required")
    })
    public ResponseEntity<CompanyResponse> createCompany(
            @Valid @RequestBody CompanyCreateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Create company request for name: {} by super admin: {}", 
                   request.name(), currentUser.getUsername());
        
        CompanyResponse company = companyService.createCompanyFromDto(request);
        
        logger.info("Company created successfully with ID: {}", company.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(company);
    }
    
    /**
     * Update company information.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and @companyService.belongsToCompany(#id, authentication.principal.username))")
    @Operation(summary = "Update company", description = "Update company information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Company updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid update data"),
        @ApiResponse(responseCode = "404", description = "Company not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized"),
        @ApiResponse(responseCode = "409", description = "Domain or subdomain conflict")
    })
    public ResponseEntity<CompanyResponse> updateCompany(
            @PathVariable @Parameter(description = "Company ID") UUID id,
            @Valid @RequestBody CompanyUpdateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Update company request for ID: {} by: {}", id, currentUser.getUsername());
        
        CompanyResponse company = companyService.updateCompanyFromDto(id, request);
        
        logger.info("Company updated successfully: {}", id);
        return ResponseEntity.ok(company);
    }
    
    /**
     * Delete company (soft delete - super admin only).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete company", description = "Soft delete a company (super admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Company deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Company not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized - super admin required"),
        @ApiResponse(responseCode = "409", description = "Company has active users or data")
    })
    public ResponseEntity<Void> deleteCompany(
            @PathVariable @Parameter(description = "Company ID") UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Delete company request for ID: {} by super admin: {}", id, currentUser.getUsername());
        
        User adminUser = currentUserService.getCurrentUser();
        UUID adminUserId = adminUser.getId();
        companyService.deleteCompany(id, adminUserId);
        
        logger.info("Company deleted successfully: {}", id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Activate company.
     */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Activate company", description = "Activate a company (super admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Company activated successfully"),
        @ApiResponse(responseCode = "404", description = "Company not found"),
        @ApiResponse(responseCode = "400", description = "Company already active"),
        @ApiResponse(responseCode = "403", description = "Not authorized - super admin required")
    })
    public ResponseEntity<CompanyResponse> activateCompany(
            @PathVariable @Parameter(description = "Company ID") UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Activate company request for ID: {} by super admin: {}", id, currentUser.getUsername());
        
        companyService.activateCompany(id);
        CompanyResponse company = companyService.getCompanyByIdAsDto(id);
        
        logger.info("Company activated successfully: {}", id);
        return ResponseEntity.ok(company);
    }
    
    /**
     * Deactivate company.
     */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Deactivate company", description = "Deactivate a company (super admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Company deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "Company not found"),
        @ApiResponse(responseCode = "400", description = "Company already inactive"),
        @ApiResponse(responseCode = "403", description = "Not authorized - super admin required")
    })
    public ResponseEntity<CompanyResponse> deactivateCompany(
            @PathVariable @Parameter(description = "Company ID") UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Deactivate company request for ID: {} by super admin: {}", id, currentUser.getUsername());
        
        CompanyResponse company = companyService.deactivateCompany(id);
        
        logger.info("Company deactivated successfully: {}", id);
        return ResponseEntity.ok(company);
    }
    
    /**
     * Update company subscription.
     */
    @PutMapping("/{id}/subscription")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update subscription", description = "Update company subscription plan and limits (super admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscription updated successfully"),
        @ApiResponse(responseCode = "404", description = "Company not found"),
        @ApiResponse(responseCode = "400", description = "Invalid subscription data"),
        @ApiResponse(responseCode = "403", description = "Not authorized - super admin required")
    })
    public ResponseEntity<CompanyResponse> updateSubscription(
            @PathVariable @Parameter(description = "Company ID") UUID id,
            @Valid @RequestBody CompanySubscriptionUpdateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Update subscription for company: {} by super admin: {}", id, currentUser.getUsername());
        
        CompanyResponse company = companyService.updateSubscription(id, request);
        
        logger.info("Subscription updated successfully for company: {}", id);
        return ResponseEntity.ok(company);
    }
    
    /**
     * Get company statistics.
     */
    @GetMapping("/{id}/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasAnyRole('ADMIN', 'SUPERVISOR') and @companyService.belongsToCompany(#id, authentication.principal.username))")
    @Operation(summary = "Get company statistics", description = "Get company usage statistics and metrics")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Company not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<CompanyStatsResponse> getCompanyStats(
            @PathVariable @Parameter(description = "Company ID") UUID id) {
        
        logger.debug("Get statistics for company: {}", id);
        
        CompanyStatsResponse stats = companyService.getCompanyStats(id);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get current company information (for authenticated user).
     */
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current company", description = "Get information about current user's company")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Company information retrieved"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "404", description = "User not associated with any company")
    })
    public ResponseEntity<CompanyResponse> getCurrentCompany(
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.debug("Get current company for user: {}", currentUser.getUsername());
        
        CompanyResponse company = companyService.getCurrentUserCompany(currentUser.getUsername());
        return ResponseEntity.ok(company);
    }
    
    /**
     * Check domain availability.
     */
    @GetMapping("/check-domain")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Check domain availability", description = "Check if domain or subdomain is available")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Domain availability checked"),
        @ApiResponse(responseCode = "403", description = "Not authorized - super admin required")
    })
    public ResponseEntity<DomainAvailabilityResponse> checkDomainAvailability(
            @RequestParam(required = false) @Parameter(description = "Domain to check") String domain,
            @RequestParam(required = false) @Parameter(description = "Subdomain to check") String subdomain) {
        
        logger.debug("Check domain availability - domain: {}, subdomain: {}", domain, subdomain);
        
        DomainAvailabilityResponse availability = companyService.checkDomainAvailability(domain, subdomain);
        return ResponseEntity.ok(availability);
    }
    
    /**
     * Get companies approaching resource limits.
     */
    @GetMapping("/alerts/resource-limits")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get resource limit alerts", description = "Get companies approaching their resource limits")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Alerts retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized - super admin required")
    })
    public ResponseEntity<Page<CompanyListResponse>> getResourceLimitAlerts(
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        
        logger.debug("Get companies approaching resource limits");
        
        Page<CompanyListResponse> companies = companyService.getCompaniesApproachingLimits(pageable);
        return ResponseEntity.ok(companies);
    }
    
    /**
     * Get companies with expiring subscriptions.
     */
    @GetMapping("/alerts/expiring-subscriptions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get expiring subscriptions", description = "Get companies with subscriptions expiring soon")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Alerts retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized - super admin required")
    })
    public ResponseEntity<Page<CompanyListResponse>> getExpiringSubscriptions(
            @PageableDefault(size = 50, sort = "subscriptionEndDate") Pageable pageable,
            @RequestParam(defaultValue = "30") @Parameter(description = "Days until expiration") int daysUntilExpiration) {
        
        logger.debug("Get companies with subscriptions expiring in {} days", daysUntilExpiration);
        
        Page<CompanyListResponse> companies = companyService.getCompaniesWithExpiringSubscriptions(
            pageable, daysUntilExpiration);
        return ResponseEntity.ok(companies);
    }
}