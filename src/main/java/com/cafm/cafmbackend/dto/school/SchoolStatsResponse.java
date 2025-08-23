package com.cafm.cafmbackend.dto.school;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

/**
 * School statistics response DTO.
 * 
 * Purpose: Provides comprehensive school metrics and analytics
 * Pattern: Aggregated data response with calculated statistics
 * Java 23: Record with computed metrics for dashboard displays
 * Architecture: Multi-tenant aware school analytics
 */
@Schema(description = "School statistics and analytics")
public record SchoolStatsResponse(
    // Overall Counts
    @Schema(description = "Total number of schools", example = "15")
    Long totalSchools,
    
    @Schema(description = "Active schools count", example = "14")
    Long activeSchools,
    
    @Schema(description = "Inactive schools count", example = "1")
    Long inactiveSchools,
    
    // By Type
    @Schema(description = "Primary schools count", example = "8")
    Long primarySchools,
    
    @Schema(description = "Intermediate schools count", example = "4")
    Long intermediateSchools,
    
    @Schema(description = "Secondary schools count", example = "2")
    Long secondarySchools,
    
    @Schema(description = "High schools count", example = "1")
    Long highSchools,
    
    @Schema(description = "Kindergarten count", example = "0")
    Long kindergartens,
    
    @Schema(description = "Universities count", example = "0")
    Long universities,
    
    // By Gender
    @Schema(description = "Boys schools count", example = "6")
    Long boysSchools,
    
    @Schema(description = "Girls schools count", example = "5")
    Long girlsSchools,
    
    @Schema(description = "Mixed schools count", example = "4")
    Long mixedSchools,
    
    // Maintenance Metrics
    @Schema(description = "Average maintenance score", example = "78.5")
    BigDecimal averageMaintenanceScore,
    
    @Schema(description = "Minimum maintenance score", example = "45")
    Integer minMaintenanceScore,
    
    @Schema(description = "Maximum maintenance score", example = "95")
    Integer maxMaintenanceScore,
    
    @Schema(description = "Schools with excellent maintenance (90+)", example = "3")
    Long excellentMaintenanceSchools,
    
    @Schema(description = "Schools with good maintenance (75-89)", example = "8")
    Long goodMaintenanceSchools,
    
    @Schema(description = "Schools with fair maintenance (60-74)", example = "2")
    Long fairMaintenanceSchools,
    
    @Schema(description = "Schools with poor maintenance (40-59)", example = "1")
    Long poorMaintenanceSchools,
    
    @Schema(description = "Schools with critical maintenance (<40)", example = "1")
    Long criticalMaintenanceSchools,
    
    // Activity Levels
    @Schema(description = "High activity schools", example = "5")
    Long highActivitySchools,
    
    @Schema(description = "Medium activity schools", example = "8")
    Long mediumActivitySchools,
    
    @Schema(description = "Low activity schools", example = "2")
    Long lowActivitySchools,
    
    // Geographic Distribution
    @Schema(description = "Schools by city breakdown")
    Map<String, Long> schoolsByCity,
    
    @Schema(description = "Schools with GPS coordinates", example = "12")
    Long schoolsWithCoordinates,
    
    @Schema(description = "Schools without GPS coordinates", example = "3")
    Long schoolsWithoutCoordinates,
    
    // Workload Statistics
    @Schema(description = "Total pending reports across all schools", example = "45")
    Long totalPendingReports,
    
    @Schema(description = "Total active work orders across all schools", example = "32")
    Long totalActiveWorkOrders,
    
    @Schema(description = "Average reports per school", example = "3.0")
    BigDecimal averageReportsPerSchool,
    
    @Schema(description = "Schools with heavy workload (>10 pending reports)", example = "2")
    Long schoolsWithHeavyWorkload,
    
    @Schema(description = "Schools needing attention", example = "4")
    Long schoolsNeedingAttention,
    
    // Supervisor Assignment
    @Schema(description = "Schools with assigned supervisors", example = "13")
    Long schoolsWithSupervisors,
    
    @Schema(description = "Unassigned schools", example = "2")
    Long unassignedSchools,
    
    @Schema(description = "Average supervisors per school", example = "1.5")
    BigDecimal averageSupervisorsPerSchool,
    
    // Trends (if available)
    @Schema(description = "School count trend vs previous period", example = "5.0")
    BigDecimal schoolCountTrend,
    
    @Schema(description = "Average maintenance score trend", example = "-2.5")
    BigDecimal maintenanceScoreTrend,
    
    @Schema(description = "Overall trend direction", example = "improving")
    String overallTrend
) {
    
    /**
     * Calculate percentage of schools with good or excellent maintenance.
     */
    public BigDecimal getGoodMaintenancePercentage() {
        if (totalSchools == null || totalSchools == 0) {
            return BigDecimal.ZERO;
        }
        
        long goodSchools = (excellentMaintenanceSchools != null ? excellentMaintenanceSchools : 0) +
                          (goodMaintenanceSchools != null ? goodMaintenanceSchools : 0);
        
        return BigDecimal.valueOf(goodSchools)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalSchools), 1, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Calculate percentage of schools needing attention.
     */
    public BigDecimal getAttentionRequiredPercentage() {
        if (totalSchools == null || totalSchools == 0 || schoolsNeedingAttention == null) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(schoolsNeedingAttention)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalSchools), 1, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Get overall health status based on maintenance metrics.
     */
    public String getOverallHealthStatus() {
        BigDecimal goodPercentage = getGoodMaintenancePercentage();
        
        if (goodPercentage.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "EXCELLENT";
        } else if (goodPercentage.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return "GOOD";
        } else if (goodPercentage.compareTo(BigDecimal.valueOf(40)) >= 0) {
            return "FAIR";
        } else {
            return "NEEDS_IMPROVEMENT";
        }
    }
    
    /**
     * Check if there are capacity issues.
     */
    public boolean hasCapacityIssues() {
        return (schoolsWithHeavyWorkload != null && schoolsWithHeavyWorkload > 0) ||
               (unassignedSchools != null && unassignedSchools > 0);
    }
    
    /**
     * Get the most common school type.
     */
    public String getDominantSchoolType() {
        long primary = primarySchools != null ? primarySchools : 0;
        long intermediate = intermediateSchools != null ? intermediateSchools : 0;
        long secondary = secondarySchools != null ? secondarySchools : 0;
        long high = highSchools != null ? highSchools : 0;
        long kinder = kindergartens != null ? kindergartens : 0;
        long uni = universities != null ? universities : 0;
        
        long max = Math.max(primary, Math.max(intermediate, Math.max(secondary, 
                   Math.max(high, Math.max(kinder, uni)))));
        
        if (max == primary) return "PRIMARY";
        if (max == intermediate) return "INTERMEDIATE";
        if (max == secondary) return "SECONDARY";
        if (max == high) return "HIGH_SCHOOL";
        if (max == kinder) return "KINDERGARTEN";
        if (max == uni) return "UNIVERSITY";
        
        return "MIXED";
    }
    
    /**
     * Calculate supervisor coverage percentage.
     */
    public BigDecimal getSupervisorCoveragePercentage() {
        if (totalSchools == null || totalSchools == 0 || schoolsWithSupervisors == null) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(schoolsWithSupervisors)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalSchools), 1, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Get priority action items based on statistics.
     */
    public java.util.List<String> getPriorityActions() {
        java.util.List<String> actions = new java.util.ArrayList<>();
        
        if (unassignedSchools != null && unassignedSchools > 0) {
            actions.add("Assign supervisors to " + unassignedSchools + " unassigned schools");
        }
        
        if (criticalMaintenanceSchools != null && criticalMaintenanceSchools > 0) {
            actions.add("Address critical maintenance issues in " + criticalMaintenanceSchools + " schools");
        }
        
        if (schoolsWithHeavyWorkload != null && schoolsWithHeavyWorkload > 0) {
            actions.add("Reduce workload for " + schoolsWithHeavyWorkload + " overloaded schools");
        }
        
        if (schoolsWithoutCoordinates != null && schoolsWithoutCoordinates > 0) {
            actions.add("Add GPS coordinates for " + schoolsWithoutCoordinates + " schools");
        }
        
        return actions;
    }
}