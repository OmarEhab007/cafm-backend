package com.cafm.cafmbackend.domain.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for resolving synchronization conflicts between mobile and server data.
 * 
 * Purpose: Handles data conflicts when mobile app syncs with server
 * Pattern: Strategy pattern for different conflict resolution approaches
 * Java 23: Uses pattern matching for conflict type handling and modern collections
 * Architecture: Domain service handling complex conflict resolution logic
 * Standards: Constructor injection, comprehensive logging, proper error handling
 */
@Service
public class SyncConflictResolver {
    
    private static final Logger logger = LoggerFactory.getLogger(SyncConflictResolver.class);
    
    /**
     * Conflict resolution strategies.
     */
    public enum ConflictResolution {
        SERVER_WINS,      // Server data takes precedence
        CLIENT_WINS,      // Client data takes precedence  
        MERGE,           // Attempt to merge compatible changes
        MANUAL           // Require manual resolution
    }
    
    /**
     * Represents a sync conflict between client and server data.
     */
    public record SyncConflict(
        String conflictId,
        String entityType,
        String entityId,
        Map<String, Object> clientData,
        Map<String, Object> serverData,
        LocalDateTime clientTimestamp,
        LocalDateTime serverTimestamp,
        ConflictType conflictType
    ) {}
    
    /**
     * Types of conflicts that can occur.
     */
    public enum ConflictType {
        UPDATE_CONFLICT,     // Both client and server updated same record
        DELETE_CONFLICT,     // One side deleted, other updated
        FIELD_CONFLICT,      // Specific field conflicts
        VERSION_CONFLICT     // Version mismatch
    }
    
    /**
     * Result of conflict resolution.
     */
    public record ConflictResolutionResult(
        String conflictId,
        ConflictResolution strategy,
        Map<String, Object> resolvedData,
        boolean requiresManualReview,
        String resolutionReason,
        List<String> warnings
    ) {}
    
    /**
     * Resolve a single conflict (public wrapper for resolveSingleConflict).
     */
    public ConflictResolutionResult resolveConflict(SyncConflict conflict, ConflictResolution strategy) {
        return resolveSingleConflict(conflict, strategy);
    }
    
    /**
     * Resolve sync conflicts using the specified strategy.
     */
    public List<ConflictResolutionResult> resolveConflicts(
            List<SyncConflict> conflicts, 
            ConflictResolution defaultStrategy) {
        
        logger.info("Resolving {} sync conflicts with strategy: {}", conflicts.size(), defaultStrategy);
        
        List<ConflictResolutionResult> results = new ArrayList<>();
        
        for (SyncConflict conflict : conflicts) {
            try {
                ConflictResolutionResult result = resolveSingleConflict(conflict, defaultStrategy);
                results.add(result);
                
                logger.debug("Resolved conflict {} using strategy: {}", 
                           conflict.conflictId(), result.strategy());
                           
            } catch (Exception e) {
                logger.error("Failed to resolve conflict: {}", conflict.conflictId(), e);
                
                // Create manual resolution result for failed conflicts
                ConflictResolutionResult failedResult = new ConflictResolutionResult(
                    conflict.conflictId(),
                    ConflictResolution.MANUAL,
                    conflict.serverData(), // Default to server data
                    true,
                    "Automatic resolution failed: " + e.getMessage(),
                    List.of("Requires manual intervention")
                );
                results.add(failedResult);
            }
        }
        
        logger.info("Conflict resolution completed: {} resolved, {} require manual review", 
                   results.size(), results.stream().mapToLong(r -> r.requiresManualReview() ? 1 : 0).sum());
        
        return results;
    }
    
    /**
     * Resolve a single conflict using the specified strategy.
     */
    private ConflictResolutionResult resolveSingleConflict(
            SyncConflict conflict, 
            ConflictResolution strategy) {
        
        logger.debug("Resolving conflict {} of type {} using strategy {}", 
                    conflict.conflictId(), conflict.conflictType(), strategy);
        
        return switch (strategy) {
            case SERVER_WINS -> resolveServerWins(conflict);
            case CLIENT_WINS -> resolveClientWins(conflict);
            case MERGE -> resolveMerge(conflict);
            case MANUAL -> resolveManual(conflict);
        };
    }
    
    /**
     * Server wins resolution strategy.
     */
    private ConflictResolutionResult resolveServerWins(SyncConflict conflict) {
        logger.debug("Applying SERVER_WINS strategy for conflict: {}", conflict.conflictId());
        
        List<String> warnings = new ArrayList<>();
        if (!conflict.clientData().isEmpty()) {
            warnings.add("Client changes discarded in favor of server data");
        }
        
        return new ConflictResolutionResult(
            conflict.conflictId(),
            ConflictResolution.SERVER_WINS,
            conflict.serverData(),
            false,
            "Server data took precedence",
            warnings
        );
    }
    
    /**
     * Client wins resolution strategy.
     */
    private ConflictResolutionResult resolveClientWins(SyncConflict conflict) {
        logger.debug("Applying CLIENT_WINS strategy for conflict: {}", conflict.conflictId());
        
        List<String> warnings = new ArrayList<>();
        warnings.add("Server changes overwritten with client data");
        
        return new ConflictResolutionResult(
            conflict.conflictId(),
            ConflictResolution.CLIENT_WINS,
            conflict.clientData(),
            false,
            "Client data took precedence",
            warnings
        );
    }
    
    /**
     * Merge resolution strategy - attempt to merge compatible changes.
     */
    private ConflictResolutionResult resolveMerge(SyncConflict conflict) {
        logger.debug("Applying MERGE strategy for conflict: {}", conflict.conflictId());
        
        Map<String, Object> mergedData = new HashMap<>(conflict.serverData());
        List<String> warnings = new ArrayList<>();
        
        // Attempt to merge non-conflicting fields
        for (Map.Entry<String, Object> entry : conflict.clientData().entrySet()) {
            String field = entry.getKey();
            Object clientValue = entry.getValue();
            Object serverValue = conflict.serverData().get(field);
            
            if (canMergeField(field, clientValue, serverValue)) {
                mergedData.put(field, clientValue);
                logger.debug("Merged field '{}' from client data", field);
            } else if (!Objects.equals(clientValue, serverValue)) {
                warnings.add("Field conflict for '" + field + "' - using server value");
                logger.debug("Field conflict for '{}': client={}, server={}", 
                           field, clientValue, serverValue);
            }
        }
        
        // Special handling for timestamps - use most recent
        if (conflict.clientTimestamp() != null && conflict.serverTimestamp() != null) {
            if (conflict.clientTimestamp().isAfter(conflict.serverTimestamp())) {
                mergedData.put("updated_at", conflict.clientTimestamp());
            }
        }
        
        boolean requiresReview = !warnings.isEmpty();
        
        return new ConflictResolutionResult(
            conflict.conflictId(),
            ConflictResolution.MERGE,
            mergedData,
            requiresReview,
            "Data merged with " + warnings.size() + " field conflicts",
            warnings
        );
    }
    
    /**
     * Manual resolution strategy - flag for human intervention.
     */
    private ConflictResolutionResult resolveManual(SyncConflict conflict) {
        logger.debug("Marking conflict for manual resolution: {}", conflict.conflictId());
        
        List<String> warnings = List.of(
            "Conflict requires manual resolution",
            "Both client and server data preserved for review"
        );
        
        // Preserve both datasets for manual review
        Map<String, Object> manualData = new HashMap<>();
        manualData.put("client_data", conflict.clientData());
        manualData.put("server_data", conflict.serverData());
        manualData.put("conflict_type", conflict.conflictType().name());
        
        return new ConflictResolutionResult(
            conflict.conflictId(),
            ConflictResolution.MANUAL,
            manualData,
            true,
            "Flagged for manual resolution",
            warnings
        );
    }
    
    /**
     * Determine if a field can be safely merged.
     */
    private boolean canMergeField(String field, Object clientValue, Object serverValue) {
        // Don't merge critical fields
        Set<String> criticalFields = Set.of("id", "created_at", "deleted_at", "version");
        if (criticalFields.contains(field)) {
            return false;
        }
        
        // If server value is null/empty, client can win
        if (serverValue == null || (serverValue instanceof String str && str.trim().isEmpty())) {
            return clientValue != null;
        }
        
        // If client value is null/empty, keep server
        if (clientValue == null || (clientValue instanceof String str && str.trim().isEmpty())) {
            return false;
        }
        
        // For status fields, be more conservative
        if (field.toLowerCase().contains("status") && !Objects.equals(clientValue, serverValue)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Detect conflicts between client and server data.
     */
    public List<SyncConflict> detectConflicts(
            String entityType,
            String entityId, 
            Map<String, Object> clientData,
            Map<String, Object> serverData,
            LocalDateTime clientTimestamp,
            LocalDateTime serverTimestamp) {
        
        logger.debug("Detecting conflicts for entity: {} {}", entityType, entityId);
        
        List<SyncConflict> conflicts = new ArrayList<>();
        
        // Check for version conflicts
        Object clientVersion = clientData.get("version");
        Object serverVersion = serverData.get("version");
        
        if (clientVersion != null && serverVersion != null && 
            !Objects.equals(clientVersion, serverVersion)) {
            
            String conflictId = UUID.randomUUID().toString();
            SyncConflict conflict = new SyncConflict(
                conflictId,
                entityType,
                entityId,
                clientData,
                serverData,
                clientTimestamp,
                serverTimestamp,
                ConflictType.VERSION_CONFLICT
            );
            conflicts.add(conflict);
            
            logger.debug("Version conflict detected: client={}, server={}", 
                        clientVersion, serverVersion);
        }
        
        // Check for field-level conflicts
        Set<String> allFields = new HashSet<>();
        allFields.addAll(clientData.keySet());
        allFields.addAll(serverData.keySet());
        
        boolean hasFieldConflicts = false;
        for (String field : allFields) {
            Object clientValue = clientData.get(field);
            Object serverValue = serverData.get(field);
            
            if (!Objects.equals(clientValue, serverValue)) {
                hasFieldConflicts = true;
                logger.debug("Field conflict detected for '{}': client={}, server={}", 
                           field, clientValue, serverValue);
            }
        }
        
        if (hasFieldConflicts && conflicts.isEmpty()) {
            String conflictId = UUID.randomUUID().toString();
            SyncConflict conflict = new SyncConflict(
                conflictId,
                entityType,
                entityId,
                clientData,
                serverData,
                clientTimestamp,
                serverTimestamp,
                ConflictType.FIELD_CONFLICT
            );
            conflicts.add(conflict);
        }
        
        logger.debug("Conflict detection completed: {} conflicts found", conflicts.size());
        
        return conflicts;
    }
}