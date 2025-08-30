package com.cafm.cafmbackend.domain.services;

import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import com.cafm.cafmbackend.infrastructure.persistence.entity.Report;
import com.cafm.cafmbackend.infrastructure.persistence.entity.WorkOrder;
import com.cafm.cafmbackend.infrastructure.persistence.entity.SupervisorAttendance;
import com.cafm.cafmbackend.infrastructure.persistence.repository.UserRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.ReportRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.WorkOrderRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.SupervisorAttendanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating and tracking supervisor performance metrics.
 * 
 * Purpose: Calculate comprehensive KPIs and performance metrics for supervisors
 * Pattern: Aggregation service with caching for expensive metric calculations
 * Java 23: Uses stream operations and modern date/time APIs for metric computation
 * Architecture: Domain service aggregating data from multiple repositories
 * Standards: Provides standardized metrics for performance evaluation and reporting
 */
@Service
@Transactional(readOnly = true)
public class SupervisorMetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(SupervisorMetricsService.class);
    
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final WorkOrderRepository workOrderRepository;
    private final SupervisorAttendanceRepository attendanceRepository;
    
    @Autowired
    public SupervisorMetricsService(
            UserRepository userRepository,
            ReportRepository reportRepository,
            WorkOrderRepository workOrderRepository,
            SupervisorAttendanceRepository attendanceRepository) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.workOrderRepository = workOrderRepository;
        this.attendanceRepository = attendanceRepository;
    }
    
    /**
     * Calculate comprehensive performance metrics for a supervisor.
     */
    @Cacheable(value = "supervisor-metrics", key = "#username + ':' + #days")
    public Map<String, Object> calculatePerformanceMetrics(String username, Integer days) {
        logger.info("Calculating performance metrics for supervisor: {}, period: {} days", 
            username, days);
        
        try {
            User supervisor = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("Supervisor not found"));
            
            LocalDateTime startDate = LocalDateTime.now().minusDays(days);
            LocalDateTime endDate = LocalDateTime.now();
            
            // Calculate various metrics
            Map<String, Object> reportMetrics = calculateReportMetrics(supervisor, startDate, endDate);
            Map<String, Object> workOrderMetrics = calculateWorkOrderMetrics(supervisor, startDate, endDate);
            Map<String, Object> attendanceMetrics = calculateAttendanceMetrics(supervisor, startDate, endDate);
            Map<String, Object> efficiencyMetrics = calculateEfficiencyMetrics(supervisor, startDate, endDate);
            Map<String, Object> qualityMetrics = calculateQualityMetrics(supervisor, startDate, endDate);
            
            // Calculate overall performance score
            double performanceScore = calculateOverallPerformanceScore(
                reportMetrics, workOrderMetrics, attendanceMetrics, efficiencyMetrics, qualityMetrics);
            
            // Compile comprehensive metrics
            return Map.of(
                "period_days", days,
                "supervisor", Map.of(
                    "id", supervisor.getId().toString(),
                    "username", username,
                    "name", supervisor.getFullName() != null ? supervisor.getFullName() : username
                ),
                "reports", reportMetrics,
                "workOrders", workOrderMetrics,
                "attendance", attendanceMetrics,
                "efficiency", efficiencyMetrics,
                "quality", qualityMetrics,
                "overallScore", performanceScore,
                "rating", deriveRating(performanceScore),
                "timestamp", System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            logger.error("Error calculating metrics for supervisor: {}", username, e);
            throw new RuntimeException("Failed to calculate performance metrics", e);
        }
    }
    
    /**
     * Calculate report-related metrics.
     */
    private Map<String, Object> calculateReportMetrics(User supervisor, 
                                                       LocalDateTime startDate, 
                                                       LocalDateTime endDate) {
        try {
            // Get reports created by supervisor
            List<Report> reports = reportRepository.findBySupervisorIdAndCreatedAtBetween(
                supervisor.getId(), startDate, endDate);
            
            // Calculate metrics
            int totalReports = reports.size();
            long completedReports = reports.stream()
                .filter(r -> r.getStatus() != null && 
                           r.getStatus().toString().equals("COMPLETED"))
                .count();
            
            long pendingReports = reports.stream()
                .filter(r -> r.getStatus() != null && 
                           r.getStatus().toString().equals("PENDING"))
                .count();
            
            // Calculate average resolution time for completed reports
            double avgResolutionHours = reports.stream()
                .filter(r -> r.getStatus() != null && 
                           r.getStatus().toString().equals("COMPLETED") &&
                           r.getCompletedAt() != null)
                .mapToDouble(r -> Duration.between(r.getCreatedAt(), r.getCompletedAt()).toHours())
                .average()
                .orElse(0.0);
            
            // Group by priority
            Map<String, Long> byPriority = reports.stream()
                .collect(Collectors.groupingBy(
                    r -> r.getPriority() != null ? r.getPriority().toString() : "UNKNOWN",
                    Collectors.counting()
                ));
            
            return Map.of(
                "total", totalReports,
                "completed", completedReports,
                "pending", pendingReports,
                "completionRate", totalReports > 0 ? 
                    (double) completedReports / totalReports * 100 : 0.0,
                "averageResolutionHours", avgResolutionHours,
                "byPriority", byPriority,
                "dailyAverage", totalReports > 0 ? 
                    (double) totalReports / ChronoUnit.DAYS.between(startDate, endDate) : 0.0
            );
            
        } catch (Exception e) {
            logger.warn("Error calculating report metrics", e);
            return Map.of("error", "Unable to calculate report metrics");
        }
    }
    
    /**
     * Calculate work order metrics.
     */
    private Map<String, Object> calculateWorkOrderMetrics(User supervisor,
                                                          LocalDateTime startDate,
                                                          LocalDateTime endDate) {
        try {
            // Get work orders assigned to supervisor
            List<WorkOrder> workOrders = workOrderRepository.findByAssignedToIdAndCreatedAtBetween(
                supervisor.getId(), startDate, endDate);
            
            int totalWorkOrders = workOrders.size();
            long completedWorkOrders = workOrders.stream()
                .filter(wo -> wo.getStatus() != null && 
                            wo.getStatus().toString().equals("COMPLETED"))
                .count();
            
            long inProgressWorkOrders = workOrders.stream()
                .filter(wo -> wo.getStatus() != null && 
                            wo.getStatus().toString().equals("IN_PROGRESS"))
                .count();
            
            // Calculate average completion time
            double avgCompletionHours = workOrders.stream()
                .filter(wo -> wo.getStatus() != null && 
                            wo.getStatus().toString().equals("COMPLETED") &&
                            wo.getActualEnd() != null)
                .mapToDouble(wo -> Duration.between(wo.getCreatedAt(), wo.getActualEnd()).toHours())
                .average()
                .orElse(0.0);
            
            // Calculate on-time completion rate (assuming 48 hour SLA)
            long onTimeCompletions = workOrders.stream()
                .filter(wo -> wo.getStatus() != null && 
                            wo.getStatus().toString().equals("COMPLETED") &&
                            wo.getActualEnd() != null &&
                            Duration.between(wo.getCreatedAt(), wo.getActualEnd()).toHours() <= 48)
                .count();
            
            return Map.of(
                "total", totalWorkOrders,
                "completed", completedWorkOrders,
                "inProgress", inProgressWorkOrders,
                "completionRate", totalWorkOrders > 0 ? 
                    (double) completedWorkOrders / totalWorkOrders * 100 : 0.0,
                "averageCompletionHours", avgCompletionHours,
                "onTimeRate", completedWorkOrders > 0 ? 
                    (double) onTimeCompletions / completedWorkOrders * 100 : 0.0,
                "backlog", totalWorkOrders - completedWorkOrders - inProgressWorkOrders
            );
            
        } catch (Exception e) {
            logger.warn("Error calculating work order metrics", e);
            return Map.of("error", "Unable to calculate work order metrics");
        }
    }
    
    /**
     * Calculate attendance metrics.
     */
    private Map<String, Object> calculateAttendanceMetrics(User supervisor,
                                                          LocalDateTime startDate,
                                                          LocalDateTime endDate) {
        try {
            List<SupervisorAttendance> attendanceRecords = 
                attendanceRepository.findBySupervisorIdAndCheckInTimeBetween(
                    supervisor.getId(), startDate, endDate);
            
            int totalDays = (int) ChronoUnit.DAYS.between(startDate, endDate);
            int daysWorked = attendanceRecords.size();
            
            // Calculate total hours worked
            double totalHoursWorked = attendanceRecords.stream()
                .filter(a -> a.getCheckOutTime() != null)
                .mapToDouble(a -> Duration.between(a.getCheckInTime(), a.getCheckOutTime()).toMinutes() / 60.0)
                .sum();
            
            // Calculate average hours per day
            double avgHoursPerDay = daysWorked > 0 ? totalHoursWorked / daysWorked : 0.0;
            
            // Calculate punctuality (check-ins before 9 AM)
            long earlyCheckIns = attendanceRecords.stream()
                .filter(a -> a.getCheckInTime().getHour() < 9)
                .count();
            
            return Map.of(
                "totalDays", totalDays,
                "daysWorked", daysWorked,
                "attendanceRate", totalDays > 0 ? 
                    (double) daysWorked / totalDays * 100 : 0.0,
                "totalHoursWorked", totalHoursWorked,
                "averageHoursPerDay", avgHoursPerDay,
                "punctualityRate", daysWorked > 0 ? 
                    (double) earlyCheckIns / daysWorked * 100 : 0.0,
                "missedDays", totalDays - daysWorked
            );
            
        } catch (Exception e) {
            logger.warn("Error calculating attendance metrics", e);
            return Map.of("error", "Unable to calculate attendance metrics");
        }
    }
    
    /**
     * Calculate efficiency metrics.
     */
    private Map<String, Object> calculateEfficiencyMetrics(User supervisor,
                                                          LocalDateTime startDate,
                                                          LocalDateTime endDate) {
        try {
            // Get work data
            List<Report> reports = reportRepository.findBySupervisorIdAndCreatedAtBetween(
                supervisor.getId(), startDate, endDate);
            
            List<WorkOrder> workOrders = workOrderRepository.findByAssignedToIdAndCreatedAtBetween(
                supervisor.getId(), startDate, endDate);
            
            // Calculate task completion velocity
            int workingDays = (int) ChronoUnit.DAYS.between(startDate, endDate);
            double reportsPerDay = workingDays > 0 ? (double) reports.size() / workingDays : 0.0;
            double workOrdersPerDay = workingDays > 0 ? (double) workOrders.size() / workingDays : 0.0;
            
            // Calculate rework rate (simplified - tasks reopened)
            long reworkedTasks = workOrders.stream()
                .filter(wo -> wo.getUpdatedAt() != null && 
                            wo.getUpdatedAt().isAfter(wo.getCreatedAt().plusDays(2)))
                .count();
            
            double reworkRate = workOrders.size() > 0 ? 
                (double) reworkedTasks / workOrders.size() * 100 : 0.0;
            
            // Calculate response time (time to first action)
            double avgResponseHours = reports.stream()
                .filter(r -> r.getUpdatedAt() != null)
                .mapToDouble(r -> Duration.between(r.getCreatedAt(), r.getUpdatedAt()).toMinutes() / 60.0)
                .average()
                .orElse(0.0);
            
            return Map.of(
                "reportsPerDay", reportsPerDay,
                "workOrdersPerDay", workOrdersPerDay,
                "combinedProductivity", reportsPerDay + workOrdersPerDay,
                "reworkRate", reworkRate,
                "averageResponseHours", avgResponseHours,
                "efficiencyScore", calculateEfficiencyScore(
                    reportsPerDay, workOrdersPerDay, reworkRate, avgResponseHours)
            );
            
        } catch (Exception e) {
            logger.warn("Error calculating efficiency metrics", e);
            return Map.of("error", "Unable to calculate efficiency metrics");
        }
    }
    
    /**
     * Calculate quality metrics.
     */
    private Map<String, Object> calculateQualityMetrics(User supervisor,
                                                        LocalDateTime startDate,
                                                        LocalDateTime endDate) {
        try {
            List<Report> reports = reportRepository.findBySupervisorIdAndCreatedAtBetween(
                supervisor.getId(), startDate, endDate);
            
            // Calculate completeness score (reports with all required fields)
            long completeReports = reports.stream()
                .filter(r -> r.getDescription() != null && 
                           r.getPriority() != null &&
                           r.getStatus() != null)
                .count();
            
            double completenessRate = reports.size() > 0 ? 
                (double) completeReports / reports.size() * 100 : 0.0;
            
            // Calculate documentation quality (reports with detailed descriptions)
            long detailedReports = reports.stream()
                .filter(r -> r.getDescription() != null && r.getDescription().length() > 50)
                .count();
            
            double documentationQuality = reports.size() > 0 ? 
                (double) detailedReports / reports.size() * 100 : 0.0;
            
            // Simplified customer satisfaction (would come from feedback in production)
            double customerSatisfaction = 85.0 + (Math.random() * 10); // Mock value
            
            return Map.of(
                "completenessRate", completenessRate,
                "documentationQuality", documentationQuality,
                "customerSatisfaction", customerSatisfaction,
                "qualityScore", (completenessRate + documentationQuality + customerSatisfaction) / 3,
                "completeReports", completeReports,
                "detailedReports", detailedReports
            );
            
        } catch (Exception e) {
            logger.warn("Error calculating quality metrics", e);
            return Map.of("error", "Unable to calculate quality metrics");
        }
    }
    
    /**
     * Calculate overall performance score.
     */
    private double calculateOverallPerformanceScore(Map<String, Object> reportMetrics,
                                                   Map<String, Object> workOrderMetrics,
                                                   Map<String, Object> attendanceMetrics,
                                                   Map<String, Object> efficiencyMetrics,
                                                   Map<String, Object> qualityMetrics) {
        try {
            // Weight different metrics
            double reportScore = ((Number) reportMetrics.getOrDefault("completionRate", 0)).doubleValue() * 0.20;
            double workOrderScore = ((Number) workOrderMetrics.getOrDefault("completionRate", 0)).doubleValue() * 0.25;
            double attendanceScore = ((Number) attendanceMetrics.getOrDefault("attendanceRate", 0)).doubleValue() * 0.15;
            double efficiencyScore = ((Number) efficiencyMetrics.getOrDefault("efficiencyScore", 0)).doubleValue() * 0.20;
            double qualityScore = ((Number) qualityMetrics.getOrDefault("qualityScore", 0)).doubleValue() * 0.20;
            
            return reportScore + workOrderScore + attendanceScore + efficiencyScore + qualityScore;
            
        } catch (Exception e) {
            logger.warn("Error calculating overall score", e);
            return 0.0;
        }
    }
    
    /**
     * Calculate efficiency score based on multiple factors.
     */
    private double calculateEfficiencyScore(double reportsPerDay, double workOrdersPerDay,
                                           double reworkRate, double avgResponseHours) {
        // Normalize metrics and calculate weighted score
        double productivityScore = Math.min((reportsPerDay + workOrdersPerDay) * 10, 100);
        double reworkScore = Math.max(100 - reworkRate, 0);
        double responseScore = Math.max(100 - (avgResponseHours * 2), 0);
        
        return (productivityScore * 0.4 + reworkScore * 0.3 + responseScore * 0.3);
    }
    
    /**
     * Derive performance rating from score.
     */
    private String deriveRating(double score) {
        if (score >= 90) return "EXCELLENT";
        if (score >= 75) return "GOOD";
        if (score >= 60) return "SATISFACTORY";
        if (score >= 40) return "NEEDS_IMPROVEMENT";
        return "POOR";
    }
}