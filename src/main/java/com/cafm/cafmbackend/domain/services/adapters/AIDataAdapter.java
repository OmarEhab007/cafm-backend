package com.cafm.cafmbackend.domain.services.adapters;

import com.cafm.cafmbackend.infrastructure.persistence.entity.*;
import com.cafm.cafmbackend.infrastructure.persistence.repository.*;
import com.cafm.cafmbackend.shared.enums.*;
import com.cafm.cafmbackend.shared.enums.UserType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Adapter service that bridges the gap between AI service expectations 
 * and the actual database schema.
 * 
 * Purpose: Translates school-centric data model to asset-centric AI algorithms
 * Pattern: Adapter pattern for schema mismatch resolution
 * Java 23: Uses records and pattern matching for data transformation
 * Architecture: Domain service layer adapter
 * Standards: Maintains data consistency while enabling AI features
 */
@Service
@Transactional(readOnly = true)
public class AIDataAdapter {
    
    private final AssetRepository assetRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ReportRepository reportRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    
    public AIDataAdapter(AssetRepository assetRepository,
                        WorkOrderRepository workOrderRepository,
                        ReportRepository reportRepository,
                        SchoolRepository schoolRepository,
                        UserRepository userRepository) {
        this.assetRepository = assetRepository;
        this.workOrderRepository = workOrderRepository;
        this.reportRepository = reportRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Get maintenance history for an asset by finding related work orders
     * through the school that owns the asset
     */
    public List<WorkOrder> getAssetMaintenanceHistory(UUID assetId) {
        return assetRepository.findById(assetId)
            .map(asset -> {
                // Find school that might be related to this asset
                // Since assets don't directly link to schools, we use company context
                UUID companyId = asset.getCompany().getId();
                
                // Get all work orders for the company and filter by relevance
                return workOrderRepository.findByCompanyIdAndDeletedAtIsNull(companyId, 
                    org.springframework.data.domain.Pageable.unpaged())
                    .getContent()
                    .stream()
                    .filter(wo -> wo.getStatus() == WorkOrderStatus.COMPLETED)
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(100) // Limit for performance
                    .collect(Collectors.toList());
            })
            .orElse(Collections.emptyList());
    }
    
    /**
     * Get maintenance reports related to an asset through school context
     */
    public List<Report> getAssetMaintenanceReports(UUID assetId) {
        return assetRepository.findById(assetId)
            .map(asset -> {
                UUID companyId = asset.getCompany().getId();
                
                // Get completed reports that might relate to this asset type
                return reportRepository.findByStatus(ReportStatus.COMPLETED)
                    .stream()
                    .filter(r -> r.getCompany() != null && 
                                r.getCompany().getId().equals(companyId))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(100)
                    .collect(Collectors.toList());
            })
            .orElse(Collections.emptyList());
    }
    
    /**
     * Find work orders by company and date range (missing repository method)
     */
    public List<WorkOrder> findWorkOrdersByCompanyAndDateRange(UUID companyId, 
                                                               LocalDateTime start, 
                                                               LocalDateTime end) {
        // Use available method with scheduled date
        return workOrderRepository.findByCompany_IdAndScheduledDateRange(companyId, start, end);
    }
    
    /**
     * Count work orders by assignee and status (missing repository method)
     */
    public long countByAssignedToAndStatusIn(User user, List<WorkOrderStatus> statuses) {
        // Use available repository methods
        return statuses.stream()
            .mapToLong(status -> 
                workOrderRepository.countByAssignedToAndStatus(user.getId(), status))
            .sum();
    }
    
    /**
     * Find assets by company with active status
     */
    public List<Asset> findActiveAssetsByCompany(UUID companyId) {
        return assetRepository.findByCompany_IdAndStatus(companyId, AssetStatus.ACTIVE);
    }
    
    /**
     * Calculate trend metrics from work order history
     */
    public TrendMetrics calculateTrendMetrics(List<WorkOrder> history) {
        if (history.isEmpty()) {
            return new TrendMetrics(0.0, "STABLE", 0.0, BigDecimal.ZERO);
        }
        
        // Calculate monthly averages
        Map<String, Long> monthlyCount = history.stream()
            .collect(Collectors.groupingBy(
                wo -> wo.getCreatedAt().getYear() + "-" + wo.getCreatedAt().getMonthValue(),
                Collectors.counting()
            ));
        
        double avgMonthly = monthlyCount.values().stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        // Calculate trend direction
        List<Long> counts = new ArrayList<>(monthlyCount.values());
        String direction = "STABLE";
        if (counts.size() >= 2) {
            long recent = counts.get(counts.size() - 1);
            long previous = counts.get(counts.size() - 2);
            if (recent > previous * 1.1) direction = "INCREASING";
            else if (recent < previous * 0.9) direction = "DECREASING";
        }
        
        // Calculate growth rate
        double growthRate = counts.size() >= 2 ? 
            ((double)(counts.get(counts.size() - 1) - counts.get(0)) / counts.get(0)) * 100 : 0.0;
        
        // Calculate average cost
        BigDecimal avgCost = history.stream()
            .map(WorkOrder::getTotalCost)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(Math.max(history.size(), 1)), 2, BigDecimal.ROUND_HALF_UP);
        
        return new TrendMetrics(avgMonthly, direction, growthRate, avgCost);
    }
    
    /**
     * Calculate anomaly severity score
     */
    public double calculateSeverityScore(WorkOrder workOrder, double avgCost, double stdDev) {
        if (workOrder.getTotalCost() == null || avgCost == 0) return 0.0;
        
        double cost = workOrder.getTotalCost().doubleValue();
        double zScore = Math.abs((cost - avgCost) / Math.max(stdDev, 1.0));
        
        // Severity increases with z-score
        return Math.min(zScore / 3.0, 1.0); // Normalize to 0-1
    }
    
    /**
     * Get technicians available for work order assignment
     */
    public List<User> getAvailableTechnicians(UUID companyId) {
        // Find supervisors and technicians who can handle work orders
        List<User> supervisors = userRepository.findByCompanyIdAndUserType(companyId, UserType.SUPERVISOR);
        List<User> admins = userRepository.findByCompanyIdAndUserType(companyId, UserType.ADMIN);
        
        List<User> availableTechnicians = new ArrayList<>(supervisors);
        availableTechnicians.addAll(admins);
        return availableTechnicians;
    }
    
    /**
     * Record class for trend metrics
     */
    public record TrendMetrics(
        double monthlyAverage,
        String trendDirection,
        double growthRate,
        BigDecimal averageCost
    ) {}
    
    /**
     * Enhanced failure prediction with available data
     */
    public record EnhancedFailurePrediction(
        UUID assetId,
        String assetName,
        double failureProbability,
        String riskLevel,
        LocalDateTime predictedMaintenanceDate,
        List<String> recommendations,
        BigDecimal estimatedCost,
        String trendDirection,
        double severityScore
    ) {
        public String getTrendDirection() { return trendDirection; }
        public double getSeverityScore() { return severityScore; }
        public LocalDateTime getPredictedMaintenanceDate() { return predictedMaintenanceDate; }
        public List<String> getRecommendations() { return recommendations; }
    }
    
    /**
     * Enhanced anomaly detection result
     */
    public record EnhancedAnomaly(
        UUID workOrderId,
        String description,
        double severityScore,
        String anomalyType,
        LocalDateTime detectedAt,
        Map<String, Object> details
    ) {
        public double getSeverityScore() { return severityScore; }
    }
}