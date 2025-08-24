package com.cafm.cafmbackend.api.controllers;

import com.cafm.cafmbackend.domain.services.MobileSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for mobile synchronization and configuration.
 * 
 * Purpose: Provides APIs for mobile apps to sync offline data and get configuration
 * Pattern: RESTful API with comprehensive mobile support (sync, config, location, push)
 * Java 23: Uses virtual threads for I/O operations and modern Spring Boot patterns
 * Architecture: Controller layer handling mobile-specific operations with proper security
 * Standards: OpenAPI documentation, comprehensive validation, proper error handling
 */
@RestController
@RequestMapping("/api/v1/mobile")
@Tag(name = "Mobile Sync", description = "Mobile synchronization and configuration APIs")
@SecurityRequirement(name = "bearer-jwt")
public class MobileSyncController {
    
    private static final Logger logger = LoggerFactory.getLogger(MobileSyncController.class);
    
    private final MobileSyncService mobileSyncService;
    
    @Autowired
    public MobileSyncController(MobileSyncService mobileSyncService) {
        this.mobileSyncService = mobileSyncService;
    }
    
    /**
     * Get mobile app configuration.
     */
    @GetMapping("/config")
    @Operation(
        summary = "Get mobile configuration",
        description = "Retrieve mobile app configuration including feature flags and company settings"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<Map<String, Object>> getMobileConfig(
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.debug("Getting mobile config for user: {}", currentUser.getUsername());
        
        Map<String, Object> config = mobileSyncService.getMobileConfig(currentUser.getUsername());
        
        return ResponseEntity.ok(config);
    }
    
    /**
     * Check sync status.
     */
    @GetMapping("/sync/status")
    @Operation(
        summary = "Get sync status", 
        description = "Check current synchronization status and pending changes count"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<Map<String, Object>> getSyncStatus(
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.debug("Getting sync status for user: {}", currentUser.getUsername());
        
        Map<String, Object> status = mobileSyncService.getSyncStatus(currentUser.getUsername());
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Placeholder for sync endpoint.
     */
    @PostMapping("/sync")
    @Operation(
        summary = "Sync offline data",
        description = "Upload offline changes from mobile app and receive server changes"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<Map<String, Object>> syncData(
            @RequestBody Map<String, Object> syncRequest,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Sync request received for user: {}", currentUser.getUsername());
        
        Map<String, Object> response = mobileSyncService.processSyncRequest(
            currentUser.getUsername(), syncRequest);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Register a mobile device for push notifications and tracking.
     */
    @PostMapping("/device/register")
    @Operation(
        summary = "Register mobile device",
        description = "Register mobile device for push notifications and sync tracking"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<Map<String, Object>> registerDevice(
            @RequestBody Map<String, Object> deviceInfo,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Device registration request for user: {}", currentUser.getUsername());
        
        Map<String, Object> response = mobileSyncService.registerDevice(
            currentUser.getUsername(), deviceInfo);
        
        return ResponseEntity.ok(response);
    }
}