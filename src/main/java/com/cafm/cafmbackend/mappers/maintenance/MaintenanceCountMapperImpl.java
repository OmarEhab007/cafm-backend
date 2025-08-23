package com.cafm.cafmbackend.mappers.maintenance;

import com.cafm.cafmbackend.mappers.config.BaseMapper;
import com.cafm.cafmbackend.mappers.config.MapperUtils;
import com.cafm.cafmbackend.data.entity.MaintenanceCount;
import com.cafm.cafmbackend.dto.maintenance.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapper implementation for MaintenanceCount entity and DTOs.
 * Handles complex JSONB field mappings.
 */
@Component
public class MaintenanceCountMapperImpl implements BaseMapper<MaintenanceCount, MaintenanceCountRequest, MaintenanceCountResponse> {
    
    @Override
    public MaintenanceCount toEntity(MaintenanceCountRequest request) {
        if (request == null) return null;
        
        MaintenanceCount entity = MaintenanceCount.builder()
            .schoolName("") // Will be set by service based on schoolId
            .status(MaintenanceCount.MaintenanceCountStatus.DRAFT)
            .build();
        
        // Map basic JSONB fields
        entity.setItemCounts(MapperUtils.copyMap(request.itemCounts()));
        entity.setYesNoWithCounts(MapperUtils.copyMap(request.itemRepairCounts()));
        
        // Initialize empty maps for JSONB fields not in request
        entity.setTextAnswers(new HashMap<>());
        entity.setYesNoAnswers(new HashMap<>());
        entity.setSurveyAnswers(new HashMap<>());
        entity.setSectionPhotos(new HashMap<>());
        entity.setFireSafetyExpiryDates(new HashMap<>());
        
        // Store notes in maintenanceNotes map
        Map<String, String> maintenanceNotes = new HashMap<>();
        if (request.notes() != null) {
            maintenanceNotes.put("general_notes", request.notes());
        }
        if (request.reportedBy() != null) {
            maintenanceNotes.put("reported_by", request.reportedBy());
        }
        entity.setMaintenanceNotes(maintenanceNotes);
        
        // Convert Object maps to String maps for fire safety fields
        entity.setFireSafetyAlarmPanelData(
            MapperUtils.objectMapToStringMap(request.safetyItems()));
        entity.setFireSafetyConditionOnlyData(
            MapperUtils.objectMapToStringMap(request.electricalItems()));
        
        return entity;
    }
    
    @Override
    public MaintenanceCountResponse toResponse(MaintenanceCount entity) {
        if (entity == null) return null;
        
        // Calculate section counts from item counts
        Map<String, Integer> sectionCounts = extractSectionCounts(entity);
        
        // Calculate totals
        int totalItems = calculateTotalItems(entity);
        int totalDamaged = calculateTotalDamaged(entity);
        
        // Convert String maps to Object maps for response
        Map<String, Object> electricalItems = 
            MapperUtils.stringMapToObjectMap(entity.getFireSafetyConditionOnlyData());
        Map<String, Object> safetyItems = 
            MapperUtils.stringMapToObjectMap(entity.getFireSafetyAlarmPanelData());
        
        // Extract notes from maintenanceNotes
        String notes = entity.getMaintenanceNotes() != null ? 
            entity.getMaintenanceNotes().get("general_notes") : null;
        
        // Create stats
        MaintenanceCountResponse.MaintenanceStats stats = calculateStats(entity, totalItems, totalDamaged);
        
        return new MaintenanceCountResponse(
            entity.getId(),
            entity.getSchool() != null ? entity.getSchool().getId() : null,
            entity.getSchoolName(),
            entity.getCompany() != null ? entity.getCompany().getId() : null,
            entity.getCompany() != null ? entity.getCompany().getName() : null,
            MapperUtils.dateTimeToDate(entity.getSubmittedAt()),
            MapperUtils.safeMap(entity.getItemCounts()),
            MapperUtils.safeMap(entity.getYesNoWithCounts()),
            sectionCounts,
            electricalItems,
            new HashMap<>(), // plumbingItems
            new HashMap<>(), // civilItems
            new HashMap<>(), // furnitureItems
            new HashMap<>(), // hvacItems
            safetyItems,
            new HashMap<>(), // customFields
            totalItems,
            totalDamaged,
            0, // totalRepaired
            notes,
            entity.getSupervisor() != null ? entity.getSupervisor().getFullName() : null,
            null, // verifiedBy
            false, // verified
            null, // verifiedAt
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            MapperUtils.uuidToString(entity.getCreatedBy()),
            MapperUtils.uuidToString(entity.getModifiedBy()),
            stats
        );
    }
    
    /**
     * Update entity from update request.
     */
    public void updateEntity(MaintenanceCountUpdateRequest request, MaintenanceCount entity) {
        if (request == null || entity == null) return;
        
        if (request.itemCounts() != null) {
            entity.setItemCounts(new HashMap<>(request.itemCounts()));
        }
        if (request.itemRepairCounts() != null) {
            entity.setYesNoWithCounts(new HashMap<>(request.itemRepairCounts()));
        }
        
        // Update electrical items
        if (request.electricalItems() != null) {
            entity.setFireSafetyConditionOnlyData(
                MapperUtils.objectMapToStringMap(request.electricalItems()));
        }
        
        // Update safety items
        if (request.safetyItems() != null) {
            entity.setFireSafetyAlarmPanelData(
                MapperUtils.objectMapToStringMap(request.safetyItems()));
        }
        
        // Update notes
        if (request.notes() != null) {
            if (entity.getMaintenanceNotes() == null) {
                entity.setMaintenanceNotes(new HashMap<>());
            }
            entity.getMaintenanceNotes().put("general_notes", request.notes());
        }
        
        // Update verification
        if (request.verified() != null && request.verified()) {
            if (request.verifiedBy() != null && entity.getMaintenanceNotes() != null) {
                entity.getMaintenanceNotes().put("verified_by", request.verifiedBy());
            }
        }
    }
    
    private Map<String, Integer> extractSectionCounts(MaintenanceCount entity) {
        Map<String, Integer> sectionCounts = new HashMap<>();
        if (entity.getItemCounts() != null) {
            entity.getItemCounts().forEach((key, value) -> {
                String section = key.contains("_") ? key.split("_")[0] : "general";
                sectionCounts.merge(section, value, Integer::sum);
            });
        }
        return sectionCounts;
    }
    
    private int calculateTotalItems(MaintenanceCount entity) {
        if (entity.getItemCounts() == null) return 0;
        return entity.getItemCounts().values().stream()
            .mapToInt(Integer::intValue)
            .sum();
    }
    
    private int calculateTotalDamaged(MaintenanceCount entity) {
        if (entity.getYesNoWithCounts() == null) return 0;
        return entity.getYesNoWithCounts().values().stream()
            .filter(v -> v > 0)
            .mapToInt(Integer::intValue)
            .sum();
    }
    
    private MaintenanceCountResponse.MaintenanceStats calculateStats(
            MaintenanceCount entity, int totalItems, int totalDamaged) {
        
        Double damagePercentage = totalItems > 0 ? (totalDamaged * 100.0 / totalItems) : 0.0;
        
        // Find top damaged items
        Map<String, Integer> topDamagedItems = new HashMap<>();
        if (entity.getYesNoWithCounts() != null) {
            entity.getYesNoWithCounts().entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> topDamagedItems.put(e.getKey(), e.getValue()));
        }
        
        return new MaintenanceCountResponse.MaintenanceStats(
            damagePercentage,
            0.0, // repairPercentage
            totalDamaged, // pendingRepairs
            topDamagedItems,
            new HashMap<>() // criticalItems
        );
    }
}