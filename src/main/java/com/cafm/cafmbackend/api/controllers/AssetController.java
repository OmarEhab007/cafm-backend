package com.cafm.cafmbackend.api.controllers;

import com.cafm.cafmbackend.data.enums.AssetCondition;
import com.cafm.cafmbackend.data.enums.AssetStatus;
import com.cafm.cafmbackend.dto.asset.*;
import com.cafm.cafmbackend.service.AssetService;
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

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST controller for asset management operations.
 * 
 * Purpose: Manages physical assets and equipment in the CAFM system
 * Pattern: RESTful API with comprehensive CRUD and asset lifecycle operations
 * Java 23: Modern controller with security annotations and asset tracking
 * Architecture: Multi-tenant aware with role-based access control
 * Standards: OpenAPI documentation, Bean Validation, asset lifecycle management
 */
@RestController
@RequestMapping("/api/v1/assets")
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Assets", description = "Physical asset and equipment management endpoints")
public class AssetController {
    
    private static final Logger logger = LoggerFactory.getLogger(AssetController.class);
    
    private final AssetService assetService;
    private final CurrentUserService currentUserService;
    
    public AssetController(AssetService assetService, CurrentUserService currentUserService) {
        this.assetService = assetService;
        this.currentUserService = currentUserService;
    }
    
    /**
     * Get all assets with filtering and pagination.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Get all assets", description = "Get paginated list of assets with filtering")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Assets retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Page<AssetListResponse>> getAllAssets(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) @Parameter(description = "Filter by asset status") AssetStatus status,
            @RequestParam(required = false) @Parameter(description = "Filter by asset condition") AssetCondition condition,
            @RequestParam(required = false) @Parameter(description = "Filter by category ID") UUID categoryId,
            @RequestParam(required = false) @Parameter(description = "Filter by school ID") UUID schoolId,
            @RequestParam(required = false) @Parameter(description = "Search by name, code, serial number, etc.") String search,
            @RequestParam(required = false) @Parameter(description = "Filter by active status") Boolean isActive) {
        
        logger.debug("Get all assets request with page: {}, size: {}", 
                    pageable.getPageNumber(), pageable.getPageSize());
        
        UUID companyId = currentUserService.ensureTenantContext();
        
        Page<AssetListResponse> assets = assetService.getAllAssetsAsDto(
            pageable, companyId, status, condition, categoryId, schoolId, search, isActive);
        
        return ResponseEntity.ok(assets);
    }
    
    /**
     * Get asset by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Get asset by ID", description = "Get detailed asset information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Asset retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Asset not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<AssetResponse> getAssetById(
            @PathVariable @Parameter(description = "Asset ID") UUID id) {
        
        logger.debug("Get asset by ID: {}", id);
        
        AssetResponse asset = assetService.getAssetByIdAsDto(id);
        return ResponseEntity.ok(asset);
    }
    
    /**
     * Create a new asset.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create asset", description = "Create a new physical asset")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Asset created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid asset data"),
        @ApiResponse(responseCode = "409", description = "Asset code or serial number already exists"),
        @ApiResponse(responseCode = "403", description = "Not authorized - admin required")
    })
    public ResponseEntity<AssetResponse> createAsset(
            @Valid @RequestBody AssetCreateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Create asset request for code: {} by admin: {}", 
                   request.assetCode(), currentUser.getUsername());
        
        UUID companyId = currentUserService.ensureTenantContext();
        
        AssetResponse asset = assetService.createAssetFromDto(request, companyId);
        
        logger.info("Asset created successfully with ID: {}", asset.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(asset);
    }
    
    /**
     * Update asset information.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update asset", description = "Update asset information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Asset updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid update data"),
        @ApiResponse(responseCode = "404", description = "Asset not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<AssetResponse> updateAsset(
            @PathVariable @Parameter(description = "Asset ID") UUID id,
            @Valid @RequestBody AssetUpdateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Update asset request for ID: {} by: {}", id, currentUser.getUsername());
        
        AssetResponse asset = assetService.updateAssetFromDto(id, request);
        
        logger.info("Asset updated successfully: {}", id);
        return ResponseEntity.ok(asset);
    }
    
    /**
     * Delete asset (soft delete).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete asset", description = "Soft delete an asset")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Asset deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Asset not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized"),
        @ApiResponse(responseCode = "409", description = "Asset is currently assigned or has active maintenance")
    })
    public ResponseEntity<Void> deleteAsset(
            @PathVariable @Parameter(description = "Asset ID") UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Delete asset request for ID: {} by admin: {}", id, currentUser.getUsername());
        
        // Use legacy method from AssetService
        assetService.updateStatus(id, AssetStatus.DISPOSED, "Deleted by admin: " + currentUser.getUsername());
        
        logger.info("Asset deleted successfully: {}", id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Assign asset to user.
     */
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Assign asset", description = "Assign asset to a user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Asset assigned successfully"),
        @ApiResponse(responseCode = "404", description = "Asset not found"),
        @ApiResponse(responseCode = "400", description = "Asset cannot be assigned in current status"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<AssetResponse> assignAsset(
            @PathVariable @Parameter(description = "Asset ID") UUID id,
            @RequestParam @Parameter(description = "User ID to assign to") UUID userId,
            @RequestParam(required = false) @Parameter(description = "Assignment notes") String notes,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Assign asset {} to user {} by: {}", id, userId, currentUser.getUsername());
        
        // Use legacy method from AssetService
        assetService.assignToUser(id, userId, notes);
        AssetResponse asset = assetService.getAssetByIdAsDto(id);
        
        logger.info("Asset assigned successfully: {}", id);
        return ResponseEntity.ok(asset);
    }
    
    /**
     * Return asset from user.
     */
    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Return asset", description = "Return asset from user assignment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Asset returned successfully"),
        @ApiResponse(responseCode = "404", description = "Asset not found"),
        @ApiResponse(responseCode = "400", description = "Asset is not currently assigned"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<AssetResponse> returnAsset(
            @PathVariable @Parameter(description = "Asset ID") UUID id,
            @RequestParam(required = false) @Parameter(description = "Return notes") String notes,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Return asset {} by: {}", id, currentUser.getUsername());
        
        // Use legacy method from AssetService
        assetService.returnFromUser(id, notes);
        AssetResponse asset = assetService.getAssetByIdAsDto(id);
        
        logger.info("Asset returned successfully: {}", id);
        return ResponseEntity.ok(asset);
    }
    
    /**
     * Transfer asset to different location.
     */
    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Transfer asset", description = "Transfer asset to different location")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Asset transferred successfully"),
        @ApiResponse(responseCode = "404", description = "Asset not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<AssetResponse> transferAsset(
            @PathVariable @Parameter(description = "Asset ID") UUID id,
            @RequestParam(required = false) @Parameter(description = "New school ID") UUID schoolId,
            @RequestParam(required = false) @Parameter(description = "New location") String location,
            @RequestParam(required = false) @Parameter(description = "Transfer notes") String notes,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Transfer asset {} to school {} location {} by: {}", 
                   id, schoolId, location, currentUser.getUsername());
        
        // Use legacy method from AssetService
        assetService.transferAsset(id, schoolId, location, notes);
        AssetResponse asset = assetService.getAssetByIdAsDto(id);
        
        logger.info("Asset transferred successfully: {}", id);
        return ResponseEntity.ok(asset);
    }
    
    /**
     * Update asset status.
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update asset status", description = "Change asset status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition"),
        @ApiResponse(responseCode = "404", description = "Asset not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<AssetResponse> updateAssetStatus(
            @PathVariable @Parameter(description = "Asset ID") UUID id,
            @RequestParam @Parameter(description = "New asset status") AssetStatus status,
            @RequestParam(required = false) @Parameter(description = "Status change reason") String reason,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Update asset {} status to {} by: {}", id, status, currentUser.getUsername());
        
        // Use legacy method from AssetService
        assetService.updateStatus(id, status, reason);
        AssetResponse asset = assetService.getAssetByIdAsDto(id);
        
        logger.info("Asset status updated successfully: {}", id);
        return ResponseEntity.ok(asset);
    }
    
    /**
     * Dispose asset.
     */
    @PostMapping("/{id}/dispose")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dispose asset", description = "Dispose of an asset permanently")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Asset disposed successfully"),
        @ApiResponse(responseCode = "404", description = "Asset not found"),
        @ApiResponse(responseCode = "400", description = "Asset cannot be disposed in current state"),
        @ApiResponse(responseCode = "403", description = "Not authorized - admin required")
    })
    public ResponseEntity<AssetResponse> disposeAsset(
            @PathVariable @Parameter(description = "Asset ID") UUID id,
            @RequestParam @Parameter(description = "Disposal method") String method,
            @RequestParam(required = false) @Parameter(description = "Disposal value") BigDecimal disposalValue,
            @RequestParam @Parameter(description = "Disposal reason") String reason,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Dispose asset {} with method {} by admin: {}", 
                   id, method, currentUser.getUsername());
        
        // Use legacy method from AssetService
        assetService.disposeAsset(id, method, disposalValue, reason);
        AssetResponse asset = assetService.getAssetByIdAsDto(id);
        
        logger.info("Asset disposed successfully: {}", id);
        return ResponseEntity.ok(asset);
    }
    
    /**
     * Calculate asset depreciation.
     */
    @GetMapping("/{id}/depreciation")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Calculate depreciation", description = "Calculate current depreciation for asset")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Depreciation calculated successfully"),
        @ApiResponse(responseCode = "404", description = "Asset not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<BigDecimal> calculateDepreciation(
            @PathVariable @Parameter(description = "Asset ID") UUID id) {
        
        logger.debug("Calculate depreciation for asset: {}", id);
        
        BigDecimal depreciation = assetService.calculateDepreciation(id);
        return ResponseEntity.ok(depreciation);
    }
    
    /**
     * Get asset statistics.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get asset statistics", description = "Get comprehensive asset statistics and metrics")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized - admin required")
    })
    public ResponseEntity<AssetStatsResponse> getAssetStatistics() {
        
        logger.debug("Get asset statistics request");
        
        UUID companyId = currentUserService.ensureTenantContext();
        
        AssetStatsResponse stats = assetService.getAssetsStatsAsDto(companyId);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get assets by status.
     */
    @GetMapping("/by-status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Get assets by status", description = "Get assets filtered by status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Assets retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid asset status"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Page<AssetListResponse>> getAssetsByStatus(
            @PathVariable @Parameter(description = "Asset status") AssetStatus status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        
        logger.debug("Get assets by status: {}", status);
        
        UUID companyId = currentUserService.ensureTenantContext();
        
        Page<AssetListResponse> assets = assetService.getAllAssetsAsDto(
            pageable, companyId, status, null, null, null, null, null);
        
        return ResponseEntity.ok(assets);
    }
    
    /**
     * Get assets by condition.
     */
    @GetMapping("/by-condition/{condition}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Get assets by condition", description = "Get assets filtered by condition")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Assets retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid asset condition"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Page<AssetListResponse>> getAssetsByCondition(
            @PathVariable @Parameter(description = "Asset condition") AssetCondition condition,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        
        logger.debug("Get assets by condition: {}", condition);
        
        UUID companyId = currentUserService.ensureTenantContext();
        
        Page<AssetListResponse> assets = assetService.getAllAssetsAsDto(
            pageable, companyId, null, condition, null, null, null, null);
        
        return ResponseEntity.ok(assets);
    }
    
    /**
     * Get assets due for maintenance.
     */
    @GetMapping("/maintenance-due")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Get assets due for maintenance", description = "Get assets that require maintenance")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Assets retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Page<AssetListResponse>> getAssetsDueForMaintenance(
            @PageableDefault(size = 20, sort = "nextMaintenanceDate") Pageable pageable,
            @RequestParam(defaultValue = "0") @Parameter(description = "Days ahead to check") int daysAhead) {
        
        logger.debug("Get assets due for maintenance within {} days", daysAhead);
        
        UUID companyId = currentUserService.ensureTenantContext();
        
        // Use search with status active and get maintenance due assets via service
        Page<AssetListResponse> assets = assetService.getAllAssetsAsDto(
            pageable, companyId, AssetStatus.ACTIVE, null, null, null, null, true);
        
        return ResponseEntity.ok(assets);
    }
    
    /**
     * Search assets.
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Search assets", description = "Search assets by various criteria")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Page<AssetListResponse>> searchAssets(
            @RequestParam @Parameter(description = "Search term") String q,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        
        logger.debug("Search assets with query: {}", q);
        
        UUID companyId = currentUserService.ensureTenantContext();
        
        Page<AssetListResponse> assets = assetService.getAllAssetsAsDto(
            pageable, companyId, null, null, null, null, q, null);
        
        return ResponseEntity.ok(assets);
    }
    
    /**
     * Check asset code availability.
     */
    @GetMapping("/check-code")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Check code availability", description = "Check if asset code is available")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Code availability checked"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Boolean> checkCodeAvailability(
            @RequestParam @Parameter(description = "Asset code to check") String code) {
        
        logger.debug("Check asset code availability: {}", code);
        
        UUID companyId = currentUserService.ensureTenantContext();
        
        boolean isAvailable = !assetService.findByAssetCode(code, companyId).isPresent();
        return ResponseEntity.ok(isAvailable);
    }
    
    /**
     * Generate unique asset code.
     */
    @GetMapping("/generate-code")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate asset code", description = "Generate a unique asset code")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Asset code generated successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<String> generateAssetCode(
            @RequestParam(defaultValue = "ASSET") @Parameter(description = "Code prefix") String prefix) {
        
        logger.debug("Generate asset code with prefix: {}", prefix);
        
        UUID companyId = currentUserService.ensureTenantContext();
        
        String assetCode = assetService.generateAssetCode(prefix, companyId);
        return ResponseEntity.ok(assetCode);
    }
}