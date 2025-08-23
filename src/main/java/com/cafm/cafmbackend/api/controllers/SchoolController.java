package com.cafm.cafmbackend.controller;

import com.cafm.cafmbackend.dto.school.*;
import com.cafm.cafmbackend.service.SchoolService;
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
 * REST controller for school facility management operations.
 * 
 * Purpose: Manages educational institutions in the CAFM system
 * Pattern: RESTful API with comprehensive CRUD and facility operations
 * Java 23: Modern controller with security annotations and spatial queries
 * Architecture: Multi-tenant aware with role-based access control
 * Standards: OpenAPI documentation, Bean Validation, comprehensive error handling
 */
@RestController
@RequestMapping("/api/v1/schools")
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Schools", description = "Educational facility management endpoints")
public class SchoolController {
    
    private static final Logger logger = LoggerFactory.getLogger(SchoolController.class);
    
    private final SchoolService schoolService;
    private final CurrentUserService currentUserService;
    
    public SchoolController(SchoolService schoolService, CurrentUserService currentUserService) {
        this.schoolService = schoolService;
        this.currentUserService = currentUserService;
    }
    
    /**
     * Get all schools with filtering and pagination.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Get all schools", description = "Get paginated list of schools with filtering")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Schools retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Page<SchoolListResponse>> getAllSchools(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) @Parameter(description = "Filter by school type") String type,
            @RequestParam(required = false) @Parameter(description = "Filter by gender type") String gender,
            @RequestParam(required = false) @Parameter(description = "Filter by city") String city,
            @RequestParam(required = false) @Parameter(description = "Search by name, code, or Arabic name") String search,
            @RequestParam(required = false) @Parameter(description = "Filter by active status") Boolean isActive) {
        
        logger.debug("Get all schools request with page: {}, size: {}", 
                    pageable.getPageNumber(), pageable.getPageSize());
        
        Page<SchoolListResponse> schools = schoolService.getAllSchoolsAsDto(
            pageable, type, gender, city, search, isActive);
        
        return ResponseEntity.ok(schools);
    }
    
    /**
     * Get school by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Get school by ID", description = "Get detailed school information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "School retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "School not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<SchoolResponse> getSchoolById(
            @PathVariable @Parameter(description = "School ID") UUID id) {
        
        logger.debug("Get school by ID: {}", id);
        
        SchoolResponse school = schoolService.getSchoolByIdAsDto(id);
        return ResponseEntity.ok(school);
    }
    
    /**
     * Create a new school.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create school", description = "Create a new educational facility")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "School created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid school data"),
        @ApiResponse(responseCode = "409", description = "School code already exists"),
        @ApiResponse(responseCode = "403", description = "Not authorized - admin required")
    })
    public ResponseEntity<SchoolResponse> createSchool(
            @Valid @RequestBody SchoolCreateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Create school request for code: {} by admin: {}", 
                   request.code(), currentUser.getUsername());
        
        UUID companyId = currentUserService.ensureTenantContext();
        
        SchoolResponse school = schoolService.createSchoolFromDto(request, companyId);
        
        logger.info("School created successfully with ID: {}", school.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(school);
    }
    
    /**
     * Update school information.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update school", description = "Update school information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "School updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid update data"),
        @ApiResponse(responseCode = "404", description = "School not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<SchoolResponse> updateSchool(
            @PathVariable @Parameter(description = "School ID") UUID id,
            @Valid @RequestBody SchoolUpdateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Update school request for ID: {} by: {}", id, currentUser.getUsername());
        
        SchoolResponse school = schoolService.updateSchoolFromDto(id, request);
        
        logger.info("School updated successfully: {}", id);
        return ResponseEntity.ok(school);
    }
    
    /**
     * Delete school (soft delete).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete school", description = "Soft delete a school")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "School deleted successfully"),
        @ApiResponse(responseCode = "404", description = "School not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized"),
        @ApiResponse(responseCode = "409", description = "School has active reports or work orders")
    })
    public ResponseEntity<Void> deleteSchool(
            @PathVariable @Parameter(description = "School ID") UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Delete school request for ID: {} by admin: {}", id, currentUser.getUsername());
        
        schoolService.deleteSchool(id);
        
        logger.info("School deleted successfully: {}", id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Activate school.
     */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate school", description = "Activate a school")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "School activated successfully"),
        @ApiResponse(responseCode = "404", description = "School not found"),
        @ApiResponse(responseCode = "400", description = "School already active"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<SchoolResponse> activateSchool(
            @PathVariable @Parameter(description = "School ID") UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Activate school request for ID: {} by admin: {}", id, currentUser.getUsername());
        
        schoolService.activateSchool(id);
        SchoolResponse school = schoolService.getSchoolByIdAsDto(id);
        
        logger.info("School activated successfully: {}", id);
        return ResponseEntity.ok(school);
    }
    
    /**
     * Deactivate school.
     */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate school", description = "Deactivate a school")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "School deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "School not found"),
        @ApiResponse(responseCode = "400", description = "School already inactive"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<SchoolResponse> deactivateSchool(
            @PathVariable @Parameter(description = "School ID") UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Deactivate school request for ID: {} by admin: {}", id, currentUser.getUsername());
        
        schoolService.deactivateSchool(id);
        SchoolResponse school = schoolService.getSchoolByIdAsDto(id);
        
        logger.info("School deactivated successfully: {}", id);
        return ResponseEntity.ok(school);
    }
    
    /**
     * Update school maintenance score.
     */
    @PutMapping("/{id}/maintenance-score")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Update maintenance score", description = "Update school maintenance score")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Maintenance score updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid score value"),
        @ApiResponse(responseCode = "404", description = "School not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<SchoolResponse> updateMaintenanceScore(
            @PathVariable @Parameter(description = "School ID") UUID id,
            @RequestParam @Parameter(description = "Maintenance score (0-100)") Integer score,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Update maintenance score for school: {} to {} by: {}", 
                   id, score, currentUser.getUsername());
        
        schoolService.updateMaintenanceScore(id, score);
        SchoolResponse school = schoolService.getSchoolByIdAsDto(id);
        
        logger.info("Maintenance score updated successfully for school: {}", id);
        return ResponseEntity.ok(school);
    }
    
    /**
     * Get school statistics.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get school statistics", description = "Get comprehensive school statistics and metrics")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized - admin required")
    })
    public ResponseEntity<SchoolStatsResponse> getSchoolStatistics() {
        
        logger.debug("Get school statistics request");
        
        UUID companyId = currentUserService.ensureTenantContext();
        
        SchoolStatsResponse stats = schoolService.getSchoolsStatsAsDto(companyId);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get schools by type.
     */
    @GetMapping("/by-type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Get schools by type", description = "Get schools filtered by education type")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Schools retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid school type"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Page<SchoolListResponse>> getSchoolsByType(
            @PathVariable @Parameter(description = "School type", 
                example = "PRIMARY", 
                schema = @Schema(allowableValues = {"PRIMARY", "INTERMEDIATE", "SECONDARY", "HIGH_SCHOOL", "KINDERGARTEN", "UNIVERSITY"})) 
            String type,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        
        logger.debug("Get schools by type: {}", type);
        
        Page<SchoolListResponse> schools = schoolService.getAllSchoolsAsDto(
            pageable, type, null, null, null, null);
        
        return ResponseEntity.ok(schools);
    }
    
    /**
     * Get schools by city.
     */
    @GetMapping("/by-city/{city}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Get schools by city", description = "Get schools in a specific city")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Schools retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Page<SchoolListResponse>> getSchoolsByCity(
            @PathVariable @Parameter(description = "City name") String city,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        
        logger.debug("Get schools by city: {}", city);
        
        Page<SchoolListResponse> schools = schoolService.getAllSchoolsAsDto(
            pageable, null, null, city, null, null);
        
        return ResponseEntity.ok(schools);
    }
    
    /**
     * Search schools.
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Search schools", description = "Search schools by name, code, or Arabic name")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Page<SchoolListResponse>> searchSchools(
            @RequestParam @Parameter(description = "Search term") String q,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        
        logger.debug("Search schools with query: {}", q);
        
        Page<SchoolListResponse> schools = schoolService.getAllSchoolsAsDto(
            pageable, null, null, null, q, null);
        
        return ResponseEntity.ok(schools);
    }
    
    /**
     * Get schools needing attention.
     */
    @GetMapping("/attention-required")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Get schools needing attention", description = "Get schools with low maintenance scores or high workloads")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Schools retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Page<SchoolListResponse>> getSchoolsNeedingAttention(
            @PageableDefault(size = 20, sort = "maintenanceScore") Pageable pageable) {
        
        logger.debug("Get schools needing attention");
        
        // Get all schools and let the frontend filter by needsAttention()
        Page<SchoolListResponse> schools = schoolService.getAllSchoolsAsDto(
            pageable, null, null, null, null, true);
        
        return ResponseEntity.ok(schools);
    }
    
    /**
     * Check school code availability.
     */
    @GetMapping("/check-code")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Check code availability", description = "Check if school code is available")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Code availability checked"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Boolean> checkCodeAvailability(
            @RequestParam @Parameter(description = "School code to check") String code) {
        
        logger.debug("Check school code availability: {}", code);
        
        boolean isAvailable = schoolService.isCodeAvailable(code);
        return ResponseEntity.ok(isAvailable);
    }
}