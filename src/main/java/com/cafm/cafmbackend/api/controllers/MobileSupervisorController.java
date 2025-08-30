package com.cafm.cafmbackend.api.controllers;

import com.cafm.cafmbackend.domain.services.MobileSyncService;
import com.cafm.cafmbackend.domain.services.MobileDashboardService;
import com.cafm.cafmbackend.domain.services.MobileReportService;
import com.cafm.cafmbackend.domain.services.SupervisorLocationService;
import com.cafm.cafmbackend.dto.mobile.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mobile-specific REST controller for supervisor operations.
 * 
 * Purpose: Provides mobile-optimized endpoints for supervisor mobile application
 * Pattern: REST controller with mobile-specific DTOs and optimizations
 * Java 23: Enhanced switch expressions and pattern matching for mobile operations
 * Architecture: API layer controller for mobile supervisor functionality
 * Standards: OpenAPI documentation, security annotations, mobile optimization patterns
 */
@RestController
@RequestMapping("/api/v1/mobile/supervisor")
@Tag(name = "Mobile Supervisor", description = "Mobile-optimized supervisor operations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('SUPERVISOR') or hasRole('ADMIN')")
public class MobileSupervisorController {
    
    private static final Logger logger = LoggerFactory.getLogger(MobileSupervisorController.class);
    
    private final MobileSyncService mobileSyncService;
    private final MobileDashboardService mobileDashboardService;
    private final MobileReportService mobileReportService;
    private final SupervisorLocationService locationService;
    
    public MobileSupervisorController(
            MobileSyncService mobileSyncService,
            MobileDashboardService mobileDashboardService,
            MobileReportService mobileReportService,
            SupervisorLocationService locationService) {
        this.mobileSyncService = mobileSyncService;
        this.mobileDashboardService = mobileDashboardService;
        this.mobileReportService = mobileReportService;
        this.locationService = locationService;
    }
    
    /**
     * Synchronize mobile data with server.
     */
    @PostMapping("/sync")
    @Operation(
        summary = "Synchronize mobile data", 
        description = "Handles bi-directional sync between mobile app and server with conflict resolution"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Synchronization completed successfully"),
        @ApiResponse(responseCode = "409", description = "Conflicts detected - manual resolution required"),
        @ApiResponse(responseCode = "400", description = "Invalid sync request"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<MobileSyncResponse> syncData(
            @Valid @RequestBody MobileSyncRequest syncRequest,
            Authentication authentication) {
        
        logger.info("Mobile sync request from user: {}, device: {}", 
                   authentication.getName(), syncRequest.deviceId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate sync request
            if (syncRequest.deviceId() == null) {
                logger.warn("Sync request missing device ID from user: {}", authentication.getName());
                return ResponseEntity.badRequest().build();
            }
            
            // Process sync request
            MobileSyncResponse response = mobileSyncService.processSyncRequest(
                authentication.getName(), syncRequest);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Mobile sync completed for user: {} in {}ms, conflicts: {}, errors: {}", 
                       authentication.getName(), duration, 
                       response.conflicts() != null ? response.conflicts().size() : 0,
                       response.errors() != null ? response.errors().size() : 0);
            
            // Return appropriate status based on sync result
            return switch (response.syncStatus()) {
                case SUCCESS -> ResponseEntity.ok(response);
                case PARTIAL_SUCCESS -> ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
                case CONFLICTS_PENDING -> ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                case FAILED -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            };
            
        } catch (Exception e) {
            logger.error("Mobile sync failed for user: {}", authentication.getName(), e);
            
            MobileSyncResponse errorResponse = new MobileSyncResponse(
                UUID.randomUUID().toString(),
                MobileSyncResponse.SyncStatus.FAILED,
                new MobileSyncResponse.ServerChanges(List.of(), List.of(), List.of(), 0),
                List.of(),
                List.of(new MobileSyncResponse.SyncError(null, null, "SYNC_FAILED", e.getMessage(), Map.of())),
                LocalDateTime.now(),
                null,
                new MobileSyncResponse.SyncStatistics(0, 0, 0, 0, 1, System.currentTimeMillis() - startTime, 0),
                Map.of()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get mobile dashboard data optimized for mobile display.
     */
    @GetMapping("/dashboard")
    @Operation(
        summary = "Get mobile dashboard", 
        description = "Returns mobile-optimized dashboard data for supervisor"
    )
    @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully")
    public ResponseEntity<Map<String, Object>> getMobileDashboard(
            @Parameter(description = "Include detailed metrics") 
            @RequestParam(defaultValue = "false") boolean detailed,
            Authentication authentication) {
        
        logger.debug("Mobile dashboard request from user: {}, detailed: {}", 
                    authentication.getName(), detailed);
        
        try {
            Map<String, Object> dashboard = mobileDashboardService.getSupervisorDashboard(
                authentication.getName(), detailed);
            
            logger.debug("Mobile dashboard data size: {} items for user: {}", 
                        dashboard.size(), authentication.getName());
            
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            logger.error("Failed to get mobile dashboard for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get mobile-optimized report list.
     */
    @GetMapping("/reports")
    @Operation(
        summary = "Get mobile reports", 
        description = "Returns mobile-optimized report list with filtering and pagination"
    )
    @ApiResponse(responseCode = "200", description = "Reports retrieved successfully")
    public ResponseEntity<List<MobileReportDto>> getMobileReports(
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by priority") 
            @RequestParam(required = false) String priority,
            @Parameter(description = "Filter by school")
            @RequestParam(required = false) UUID schoolId,
            @Parameter(description = "Only assigned to me")
            @RequestParam(defaultValue = "false") boolean myReports,
            @Parameter(description = "Page size for mobile optimization")
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {
        
        logger.debug("Mobile reports request: status={}, priority={}, schoolId={}, myReports={}, limit={}", 
                    status, priority, schoolId, myReports, limit);
        
        try {
            List<MobileReportDto> reports = mobileReportService.getMobileReports(
                authentication.getName(), status, priority, schoolId, myReports, limit);
            
            logger.debug("Retrieved {} mobile reports for user: {}", 
                        reports.size(), authentication.getName());
            
            return ResponseEntity.ok(reports);
            
        } catch (Exception e) {
            logger.error("Failed to get mobile reports for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Update supervisor location for tracking and optimization.
     */
    @PostMapping("/location")
    @Operation(
        summary = "Update supervisor location", 
        description = "Updates supervisor's current location for routing and assignment optimization"
    )
    @ApiResponse(responseCode = "200", description = "Location updated successfully")
    public ResponseEntity<Map<String, Object>> updateLocation(
            @Valid @RequestBody SupervisorLocationUpdateRequest request,
            Authentication authentication) {
        
        logger.debug("Location update from user: {}, lat: {}, lng: {}", 
                    authentication.getName(), request.latitude(), request.longitude());
        
        try {
            Map<String, Object> result = locationService.updateSupervisorLocation(
                authentication.getName(), request);
            
            logger.debug("Location updated successfully for user: {}", authentication.getName());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to update location for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get mobile app configuration.
     */
    @GetMapping("/config")
    @Operation(
        summary = "Get mobile configuration", 
        description = "Returns mobile app configuration based on user role and permissions"
    )
    @ApiResponse(responseCode = "200", description = "Configuration retrieved successfully")
    public ResponseEntity<Map<String, Object>> getMobileConfig(Authentication authentication) {
        
        logger.debug("Mobile config request from user: {}", authentication.getName());
        
        try {
            Map<String, Object> config = mobileSyncService.getMobileConfig(authentication.getName());
            
            logger.debug("Mobile config generated for user: {}", authentication.getName());
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            logger.error("Failed to get mobile config for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get synchronization status.
     */
    @GetMapping("/sync/status")
    @Operation(
        summary = "Get sync status", 
        description = "Returns current synchronization status and statistics"
    )
    @ApiResponse(responseCode = "200", description = "Sync status retrieved successfully")
    public ResponseEntity<Map<String, Object>> getSyncStatus(Authentication authentication) {
        
        logger.debug("Sync status request from user: {}", authentication.getName());
        
        try {
            Map<String, Object> status = mobileSyncService.getSyncStatus(authentication.getName());
            
            logger.debug("Sync status retrieved for user: {}", authentication.getName());
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("Failed to get sync status for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Register mobile device.
     */
    @PostMapping("/device/register")
    @Operation(
        summary = "Register mobile device", 
        description = "Registers a mobile device for push notifications and sync optimization"
    )
    @ApiResponse(responseCode = "200", description = "Device registered successfully")
    public ResponseEntity<Map<String, Object>> registerDevice(
            @Valid @RequestBody MobileDeviceRegistrationRequest request,
            Authentication authentication) {
        
        logger.info("Device registration from user: {}, device: {}, platform: {}", 
                   authentication.getName(), request.deviceId(), request.platform());
        
        try {
            Map<String, Object> deviceInfo = Map.of(
                "device_id", request.deviceId(),
                "device_type", request.platform(),
                "app_version", request.appVersion(),
                "fcm_token", request.fcmToken(),
                "device_info", request.deviceInfo()
            );
            
            Map<String, Object> result = mobileSyncService.registerDevice(
                authentication.getName(), deviceInfo);
            
            logger.info("Device registered successfully for user: {}, device: {}", 
                       authentication.getName(), request.deviceId());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to register device for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Mobile device registration request DTO.
     */
    public record MobileDeviceRegistrationRequest(
        @Parameter(description = "Unique device identifier") 
        String deviceId,
        @Parameter(description = "Platform (iOS/Android)") 
        String platform,
        @Parameter(description = "App version") 
        String appVersion,
        @Parameter(description = "FCM token for push notifications") 
        String fcmToken,
        @Parameter(description = "Additional device information") 
        Map<String, Object> deviceInfo
    ) {}
    
    /**
     * Supervisor location update request DTO.
     */
    public record SupervisorLocationUpdateRequest(
        @Parameter(description = "Latitude coordinate") 
        double latitude,
        @Parameter(description = "Longitude coordinate") 
        double longitude,
        @Parameter(description = "Location accuracy in meters") 
        Double accuracy,
        @Parameter(description = "Current school/location ID") 
        UUID currentLocationId,
        @Parameter(description = "Timestamp of location update") 
        LocalDateTime timestamp
    ) {}
}