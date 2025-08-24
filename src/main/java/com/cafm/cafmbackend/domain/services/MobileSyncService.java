package com.cafm.cafmbackend.domain.services;

import com.cafm.cafmbackend.data.entity.User;
import com.cafm.cafmbackend.data.repository.UserRepository;
import com.cafm.cafmbackend.domain.services.SyncConflictResolver.ConflictResolution;
import com.cafm.cafmbackend.domain.services.SyncConflictResolver.ConflictResolutionResult;
import com.cafm.cafmbackend.domain.services.SyncConflictResolver.SyncConflict;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for handling mobile synchronization operations.
 * 
 * Purpose: Provides business logic for mobile app sync, config, and device management
 * Pattern: Domain service handling complex mobile sync operations
 * Java 23: Uses modern collections and pattern matching for sync logic
 * Architecture: Domain layer service with proper transaction boundaries
 * Standards: Constructor injection, proper logging, comprehensive error handling
 */
@Service
@Transactional
public class MobileSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(MobileSyncService.class);
    
    private final UserRepository userRepository;
    private final SyncConflictResolver conflictResolver;
    
    @Autowired
    public MobileSyncService(UserRepository userRepository, SyncConflictResolver conflictResolver) {
        this.userRepository = userRepository;
        this.conflictResolver = conflictResolver;
    }
    
    /**
     * Get mobile application configuration for a user.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMobileConfig(String userEmail) {
        logger.debug("Getting mobile config for user: {}", userEmail);
        
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
        
        Map<String, Object> config = new HashMap<>();
        config.put("app_version", "1.0.0");
        config.put("api_version", "v1");
        config.put("server_time", LocalDateTime.now());
        
        // Feature flags based on user role
        Map<String, Boolean> features = new HashMap<>();
        features.put("offline_sync", true);
        features.put("location_tracking", true);
        features.put("push_notifications", true);
        features.put("photo_upload", true);
        features.put("barcode_scanning", "TECHNICIAN".equals(user.getUserType().name()));
        
        config.put("features", features);
        
        // Sync configuration
        Map<String, Object> syncConfig = new HashMap<>();
        syncConfig.put("sync_interval_minutes", 15);
        syncConfig.put("max_offline_days", 7);
        syncConfig.put("batch_size", 50);
        syncConfig.put("conflict_resolution", "server_wins");
        
        config.put("sync_config", syncConfig);
        
        // Company-specific settings
        if (user.getCompanyId() != null) {
            Map<String, Object> companyConfig = new HashMap<>();
            companyConfig.put("company_id", user.getCompanyId());
            companyConfig.put("timezone", "UTC");
            companyConfig.put("date_format", "yyyy-MM-dd");
            config.put("company_config", companyConfig);
        }
        
        logger.debug("Mobile config generated for user: {} with {} features", 
                    userEmail, features.size());
        
        return config;
    }
    
    /**
     * Get synchronization status for a user.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSyncStatus(String userEmail) {
        logger.debug("Getting sync status for user: {}", userEmail);
        
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
        
        Map<String, Object> status = new HashMap<>();
        status.put("user_id", user.getId());
        status.put("last_sync_at", null); // Will be populated from sync_logs table
        status.put("last_sync_status", "NEVER");
        status.put("pending_conflicts", 0);
        status.put("pending_changes", 0);
        status.put("server_time", LocalDateTime.now());
        
        // Sync statistics
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total_syncs", 0);
        stats.put("successful_syncs", 0);
        stats.put("failed_syncs", 0);
        stats.put("conflicts_resolved", 0);
        
        status.put("sync_statistics", stats);
        
        logger.debug("Sync status retrieved for user: {}", userEmail);
        
        return status;
    }
    
    /**
     * Process sync request from mobile client.
     */
    public Map<String, Object> processSyncRequest(String userEmail, Map<String, Object> syncRequest) {
        logger.info("Processing sync request for user: {}", userEmail);
        
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
        
        // Extract sync data from request
        @SuppressWarnings("unchecked")
        Map<String, Object> clientData = (Map<String, Object>) syncRequest.getOrDefault("data", Map.of());
        String clientTimestampStr = (String) syncRequest.get("client_timestamp");
        String syncId = (String) syncRequest.getOrDefault("sync_id", UUID.randomUUID().toString());
        ConflictResolution strategy = ConflictResolution.valueOf(
            (String) syncRequest.getOrDefault("conflict_strategy", "SERVER_WINS"));
        
        LocalDateTime clientTimestamp = clientTimestampStr != null ? 
            LocalDateTime.parse(clientTimestampStr) : LocalDateTime.now();
        
        logger.debug("Sync request: ID={}, client_timestamp={}, data_size={}, strategy={}", 
                    syncId, clientTimestamp, clientData.size(), strategy);
        
        // Process each entity in the client data
        List<SyncConflict> allConflicts = new ArrayList<>();
        Map<String, Object> processedData = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : clientData.entrySet()) {
            String entityType = entry.getKey();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> entities = (Map<String, Object>) entry.getValue();
            
            Map<String, Object> processedEntities = processEntities(
                entityType, entities, clientTimestamp, allConflicts, user);
            
            processedData.put(entityType, processedEntities);
        }
        
        // Resolve conflicts
        List<ConflictResolutionResult> conflictResults = List.of();
        if (!allConflicts.isEmpty()) {
            conflictResults = conflictResolver.resolveConflicts(allConflicts, strategy);
            logger.info("Resolved {} conflicts for sync: {}", conflictResults.size(), syncId);
        }
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("sync_id", syncId);
        response.put("status", "SUCCESS");
        response.put("server_timestamp", LocalDateTime.now());
        response.put("records_processed", clientData.size());
        response.put("conflicts_count", allConflicts.size());
        response.put("conflicts_resolved", conflictResults.size());
        
        if (!conflictResults.isEmpty()) {
            response.put("conflict_resolutions", conflictResults.stream()
                .map(this::convertConflictResultToMap)
                .toList());
        }
        
        // Server changes to send back to client
        Map<String, Object> serverChanges = new HashMap<>();
        serverChanges.put("work_orders", Map.of());
        serverChanges.put("reports", Map.of());
        serverChanges.put("assets", Map.of());
        
        response.put("server_changes", serverChanges);
        
        // Next sync recommendations
        Map<String, Object> nextSync = new HashMap<>();
        nextSync.put("recommended_at", LocalDateTime.now().plusMinutes(15));
        nextSync.put("priority", allConflicts.isEmpty() ? "NORMAL" : "HIGH");
        
        response.put("next_sync", nextSync);
        
        String message = allConflicts.isEmpty() ? 
            "Sync completed successfully" : 
            String.format("Sync completed with %d conflicts resolved", allConflicts.size());
        response.put("message", message);
        
        logger.info("Sync request processed successfully for user: {} with ID: {}", 
                   userEmail, syncId);
        
        return response;
    }
    
    /**
     * Process entities and detect conflicts.
     */
    private Map<String, Object> processEntities(
            String entityType, 
            Map<String, Object> entities, 
            LocalDateTime clientTimestamp,
            List<SyncConflict> allConflicts,
            User user) {
        
        Map<String, Object> processed = new HashMap<>();
        
        for (Map.Entry<String, Object> entityEntry : entities.entrySet()) {
            String entityId = entityEntry.getKey();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> clientEntityData = (Map<String, Object>) entityEntry.getValue();
            
            // Simulate server data (in real implementation, fetch from database)
            Map<String, Object> serverEntityData = simulateServerData(entityType, entityId);
            
            // Detect conflicts
            List<SyncConflict> conflicts = conflictResolver.detectConflicts(
                entityType, entityId, clientEntityData, serverEntityData, 
                clientTimestamp, LocalDateTime.now());
            
            allConflicts.addAll(conflicts);
            
            // For now, just use client data (conflicts will be resolved separately)
            processed.put(entityId, clientEntityData);
        }
        
        return processed;
    }
    
    /**
     * Simulate server data for conflict detection (placeholder).
     */
    private Map<String, Object> simulateServerData(String entityType, String entityId) {
        // In real implementation, this would fetch from database
        Map<String, Object> serverData = new HashMap<>();
        serverData.put("id", entityId);
        serverData.put("version", 1);
        serverData.put("updated_at", LocalDateTime.now().minusMinutes(30));
        serverData.put("status", "IN_PROGRESS");
        
        return serverData;
    }
    
    /**
     * Convert conflict resolution result to map for JSON response.
     */
    private Map<String, Object> convertConflictResultToMap(ConflictResolutionResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("conflict_id", result.conflictId());
        map.put("strategy", result.strategy().name());
        map.put("resolved_data", result.resolvedData());
        map.put("requires_manual_review", result.requiresManualReview());
        map.put("resolution_reason", result.resolutionReason());
        map.put("warnings", result.warnings());
        
        return map;
    }
    
    /**
     * Register or update mobile device for a user.
     */
    public Map<String, Object> registerDevice(String userEmail, Map<String, Object> deviceInfo) {
        logger.info("Registering device for user: {}", userEmail);
        
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
        
        String deviceId = (String) deviceInfo.get("device_id");
        String deviceType = (String) deviceInfo.get("device_type");
        String appVersion = (String) deviceInfo.get("app_version");
        
        logger.debug("Device registration: ID={}, type={}, version={}", 
                    deviceId, deviceType, appVersion);
        
        // Device registration logic (placeholder)
        Map<String, Object> response = new HashMap<>();
        response.put("device_id", deviceId);
        response.put("registration_status", "SUCCESS");
        response.put("registered_at", LocalDateTime.now());
        response.put("user_id", user.getId());
        
        logger.info("Device registered successfully for user: {}, device: {}", 
                   userEmail, deviceId);
        
        return response;
    }
}