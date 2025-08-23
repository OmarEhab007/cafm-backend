package com.cafm.cafmbackend.dto.damage;

import com.cafm.cafmbackend.data.entity.DamageCount.PriorityLevel;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for damage count data.
 */
public record DamageCountResponse(
    UUID id,
    
    UUID schoolId,
    String schoolName,
    
    UUID companyId,
    String companyName,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate reportDate,
    
    Map<String, Integer> itemCounts,
    Map<String, List<String>> sectionPhotos,
    
    String section,
    String location,
    String description,
    
    PriorityLevel priority,
    
    Integer totalItems,
    Integer totalDamaged,
    
    BigDecimal estimatedRepairCost,
    BigDecimal actualRepairCost,
    
    String reportedBy,
    String verifiedBy,
    Boolean verified,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime verifiedAt,
    
    Boolean requiresUrgentAction,
    Boolean safetyHazard,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime inspectionDate,
    String inspectorName,
    
    String notes,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt,
    
    String createdBy,
    String updatedBy,
    
    DamageAnalytics analytics
) {
    /**
     * Analytics and insights for damage data.
     */
    public record DamageAnalytics(
        Double damageRate,
        String severityLevel,
        Integer daysOverdue,
        List<String> affectedAreas,
        Map<String, Integer> priorityDistribution,
        BigDecimal totalEstimatedCost,
        Boolean criticalStatus
    ) {
        public static DamageAnalytics calculate(
                DamageCountResponse response) {
            
            double damageRate = response.totalItems() > 0
                ? (response.totalDamaged() * 100.0) / response.totalItems()
                : 0.0;
            
            String severityLevel = determineSeverity(
                response.priority(),
                response.safetyHazard(),
                response.requiresUrgentAction()
            );
            
            Integer daysOverdue = calculateDaysOverdue(
                response.reportDate(),
                response.priority()
            );
            
            List<String> affectedAreas = response.sectionPhotos().keySet()
                .stream()
                .filter(section -> !response.sectionPhotos().get(section).isEmpty())
                .toList();
            
            Map<String, Integer> priorityDistribution = calculatePriorityDistribution(
                response.itemCounts(),
                response.priority()
            );
            
            boolean criticalStatus = response.safetyHazard() || 
                                   response.requiresUrgentAction() ||
                                   response.priority() == PriorityLevel.CRITICAL ||
                                   response.priority() == PriorityLevel.URGENT;
            
            return new DamageAnalytics(
                damageRate,
                severityLevel,
                daysOverdue,
                affectedAreas,
                priorityDistribution,
                response.estimatedRepairCost(),
                criticalStatus
            );
        }
        
        private static String determineSeverity(
                PriorityLevel priority,
                Boolean safetyHazard,
                Boolean requiresUrgentAction) {
            
            if (safetyHazard) return "CRITICAL_SAFETY";
            if (requiresUrgentAction) return "URGENT";
            
            return switch (priority) {
                case CRITICAL, URGENT -> "HIGH";
                case HIGH -> "MODERATE_HIGH";
                case MEDIUM -> "MODERATE";
                case LOW -> "LOW";
            };
        }
        
        private static Integer calculateDaysOverdue(
                LocalDate reportDate,
                PriorityLevel priority) {
            
            LocalDate now = LocalDate.now();
            long daysSinceReport = java.time.temporal.ChronoUnit.DAYS.between(reportDate, now);
            
            int expectedDays = switch (priority) {
                case CRITICAL -> 1;
                case URGENT -> 3;
                case HIGH -> 7;
                case MEDIUM -> 14;
                case LOW -> 30;
            };
            
            return Math.max(0, (int)daysSinceReport - expectedDays);
        }
        
        private static Map<String, Integer> calculatePriorityDistribution(
                Map<String, Integer> itemCounts,
                PriorityLevel defaultPriority) {
            
            // Group items by estimated priority based on damage count
            return itemCounts.entrySet().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    entry -> {
                        int count = entry.getValue();
                        if (count > 10) return "CRITICAL";
                        if (count > 5) return "HIGH";
                        if (count > 2) return "MEDIUM";
                        return "LOW";
                    },
                    java.util.stream.Collectors.summingInt(Map.Entry::getValue)
                ));
        }
    }
}