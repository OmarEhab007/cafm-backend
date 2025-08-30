package com.cafm.cafmbackend.domain.services;

import com.cafm.cafmbackend.infrastructure.persistence.entity.WorkOrder;
import com.cafm.cafmbackend.infrastructure.persistence.entity.Report;
import com.cafm.cafmbackend.infrastructure.persistence.entity.School;
import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import com.cafm.cafmbackend.shared.enums.WorkOrderStatus;
import com.cafm.cafmbackend.shared.enums.UserType;
import com.cafm.cafmbackend.infrastructure.persistence.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for location-based spatial queries and proximity calculations.
 * 
 * Purpose: Provides spatial query capabilities for location-aware features
 * Pattern: Domain service with PostGIS-ready spatial query support
 * Java 23: Uses modern collection patterns and efficient distance calculations
 * Architecture: Domain service coordinating spatial operations across entities
 * Standards: Haversine formula for accurate distance calculations
 */
@Service
@Transactional(readOnly = true)
public class LocationQueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(LocationQueryService.class);
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final int MAX_RESULTS = 50;
    
    private final WorkOrderRepository workOrderRepository;
    private final ReportRepository reportRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public LocationQueryService(
            WorkOrderRepository workOrderRepository,
            ReportRepository reportRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository) {
        this.workOrderRepository = workOrderRepository;
        this.reportRepository = reportRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Find work orders near a given location within specified radius.
     */
    public List<Map<String, Object>> findNearbyWorkOrders(Double latitude, Double longitude, 
                                                          Double radiusKm, UUID companyId) {
        logger.debug("Finding work orders within {}km of {}, {}", radiusKm, latitude, longitude);
        
        try {
            // Get all active work orders for the company
            List<WorkOrder> activeWorkOrders = workOrderRepository
                .findAll(PageRequest.of(0, MAX_RESULTS))
                .getContent()
                .stream()
                .filter(wo -> wo.getCompanyId().equals(companyId))
                .filter(wo -> wo.getDeletedAt() == null)
                .filter(wo -> wo.getStatus() != WorkOrderStatus.COMPLETED && wo.getStatus() != WorkOrderStatus.CANCELLED)
                .toList();
            
            return activeWorkOrders.stream()
                .filter(wo -> wo.getLatitude() != null && wo.getLongitude() != null)
                .map(workOrder -> {
                    double woLat = workOrder.getLatitude().doubleValue();
                    double woLon = workOrder.getLongitude().doubleValue();
                    double distance = calculateDistance(latitude, longitude, woLat, woLon);
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", workOrder.getId());
                    result.put("workOrderNumber", workOrder.getWorkOrderNumber());
                    result.put("title", workOrder.getTitle());
                    result.put("priority", workOrder.getPriority().toString());
                    result.put("status", workOrder.getStatus().toString());
                    result.put("latitude", woLat);
                    result.put("longitude", woLon);
                    result.put("distance", distance);
                    result.put("unit", "km");
                    result.put("schoolName", workOrder.getSchool() != null ? 
                              workOrder.getSchool().getName() : "Unknown");
                    result.put("assignedTo", workOrder.getAssignedTo() != null ? 
                              workOrder.getAssignedTo().getDisplayName() : "Unassigned");
                    result.put("scheduledStart", workOrder.getScheduledStart());
                    result.put("estimatedHours", workOrder.getEstimatedHours());
                    
                    return result;
                })
                .filter(wo -> (Double) wo.get("distance") <= radiusKm)
                .sorted(Comparator.comparing(o -> (Double) o.get("distance")))
                .limit(MAX_RESULTS)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error finding nearby work orders", e);
            return List.of();
        }
    }
    
    /**
     * Find supervisors near a given location within specified radius.
     */
    public List<Map<String, Object>> findNearbySupervisors(Double latitude, Double longitude, 
                                                           Double radiusKm, UUID companyId) {
        logger.debug("Finding supervisors within {}km of {}, {}", radiusKm, latitude, longitude);
        
        try {
            // Get all active supervisors in the company with location data
            List<User> supervisors = userRepository
                .findByUserTypeAndCompany_IdAndDeletedAtIsNull(UserType.SUPERVISOR, companyId)
                .stream()
                .filter(user -> user.getLastKnownLatitude() != null && user.getLastKnownLongitude() != null)
                .filter(user -> user.getIsActive() != null && user.getIsActive())
                .toList();
            
            return supervisors.stream()
                .map(supervisor -> {
                    double supLat = supervisor.getLastKnownLatitude();
                    double supLon = supervisor.getLastKnownLongitude();
                    double distance = calculateDistance(latitude, longitude, supLat, supLon);
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", supervisor.getId());
                    result.put("displayName", supervisor.getDisplayName());
                    result.put("email", supervisor.getEmail());
                    result.put("phone", supervisor.getPhone());
                    result.put("latitude", supLat);
                    result.put("longitude", supLon);
                    result.put("distance", distance);
                    result.put("unit", "km");
                    result.put("lastLocationUpdate", supervisor.getLastLocationUpdate());
                    result.put("performanceRating", supervisor.getPerformanceRating());
                    result.put("isActive", supervisor.getIsActive());
                    
                    return result;
                })
                .filter(sup -> (Double) sup.get("distance") <= radiusKm)
                .sorted(Comparator.comparing(o -> (Double) o.get("distance")))
                .limit(MAX_RESULTS)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error finding nearby supervisors", e);
            return List.of();
        }
    }
    
    /**
     * Find schools near a given location within specified radius.
     */
    public List<Map<String, Object>> findNearbySchools(Double latitude, Double longitude, 
                                                       Double radiusKm, UUID companyId) {
        logger.debug("Finding schools within {}km of {}, {}", radiusKm, latitude, longitude);
        
        try {
            // Get all active schools for the company
            List<School> schools = schoolRepository
                .findAll(PageRequest.of(0, MAX_RESULTS))
                .getContent()
                .stream()
                .filter(school -> school.getCompany().getId().equals(companyId))
                .filter(school -> school.getDeletedAt() == null)
                .toList();
            
            return schools.stream()
                .filter(school -> school.getLatitude() != null && school.getLongitude() != null)
                .map(school -> {
                    double schoolLat = school.getLatitude().doubleValue();
                    double schoolLon = school.getLongitude().doubleValue();
                    double distance = calculateDistance(latitude, longitude, schoolLat, schoolLon);
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", school.getId());
                    result.put("name", school.getName());
                    result.put("address", school.getAddress());
                    result.put("city", school.getCity());
                    result.put("latitude", schoolLat);
                    result.put("longitude", schoolLon);
                    result.put("distance", distance);
                    result.put("unit", "km");
                    result.put("isActive", school.getDeletedAt() == null);
                    
                    return result;
                })
                .filter(school -> (Double) school.get("distance") <= radiusKm)
                .sorted(Comparator.comparing(o -> (Double) o.get("distance")))
                .limit(MAX_RESULTS)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error finding nearby schools", e);
            return List.of();
        }
    }
    
    /**
     * Find work orders assigned to supervisors in a specific area.
     */
    public List<Map<String, Object>> findWorkOrdersInArea(Double centerLat, Double centerLon, 
                                                          Double radiusKm, UUID companyId) {
        logger.debug("Finding work orders in area: center=({}, {}), radius={}km", 
                    centerLat, centerLon, radiusKm);
        
        try {
            List<WorkOrder> workOrders = workOrderRepository
                .findAll(PageRequest.of(0, MAX_RESULTS * 2))
                .getContent()
                .stream()
                .filter(wo -> wo.getCompanyId().equals(companyId))
                .filter(wo -> wo.getDeletedAt() == null)
                .toList();
            
            return workOrders.stream()
                .filter(wo -> wo.getLatitude() != null && wo.getLongitude() != null)
                .map(workOrder -> {
                    double woLat = workOrder.getLatitude().doubleValue();
                    double woLon = workOrder.getLongitude().doubleValue();
                    double distance = calculateDistance(centerLat, centerLon, woLat, woLon);
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", workOrder.getId());
                    result.put("workOrderNumber", workOrder.getWorkOrderNumber());
                    result.put("title", workOrder.getTitle());
                    result.put("status", workOrder.getStatus().toString());
                    result.put("priority", workOrder.getPriority().toString());
                    result.put("latitude", woLat);
                    result.put("longitude", woLon);
                    result.put("distance", distance);
                    result.put("unit", "km");
                    result.put("school", workOrder.getSchool() != null ? Map.of(
                        "id", workOrder.getSchool().getId(),
                        "name", workOrder.getSchool().getName()
                    ) : null);
                    result.put("assignedTo", workOrder.getAssignedTo() != null ? Map.of(
                        "id", workOrder.getAssignedTo().getId(),
                        "name", workOrder.getAssignedTo().getDisplayName()
                    ) : null);
                    
                    return result;
                })
                .filter(wo -> (Double) wo.get("distance") <= radiusKm)
                .sorted(Comparator.comparing(o -> (Double) o.get("distance")))
                .limit(MAX_RESULTS)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error finding work orders in area", e);
            return List.of();
        }
    }
    
    /**
     * Find reports near a specific location.
     */
    public List<Map<String, Object>> findNearbyReports(Double latitude, Double longitude, 
                                                       Double radiusKm, UUID companyId) {
        logger.debug("Finding reports within {}km of {}, {}", radiusKm, latitude, longitude);
        
        try {
            // Get recent reports for the company  
            List<Report> reports = reportRepository
                .findAll(PageRequest.of(0, MAX_RESULTS))
                .getContent()
                .stream()
                .filter(report -> report.getSchool().getCompany().getId().equals(companyId))
                .filter(report -> report.getDeletedAt() == null)
                .toList();
            
            return reports.stream()
                .filter(report -> report.getLatitude() != null && report.getLongitude() != null)
                .map(report -> {
                    double repLat = report.getLatitude();
                    double repLon = report.getLongitude();
                    double distance = calculateDistance(latitude, longitude, repLat, repLon);
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", report.getId());
                    result.put("reportNumber", report.getReportNumber());
                    result.put("title", report.getTitle());
                    result.put("status", report.getStatus().toString());
                    result.put("priority", report.getPriority().toString());
                    result.put("latitude", repLat);
                    result.put("longitude", repLon);
                    result.put("distance", distance);
                    result.put("unit", "km");
                    result.put("schoolName", report.getSchool() != null ? 
                              report.getSchool().getName() : "Unknown");
                    result.put("supervisorName", report.getSupervisor() != null ? 
                              report.getSupervisor().getDisplayName() : "Unknown");
                    result.put("reportedDate", report.getReportedDate());
                    result.put("locationAddress", report.getLocationAddress());
                    
                    return result;
                })
                .filter(rep -> (Double) rep.get("distance") <= radiusKm)
                .sorted(Comparator.comparing(o -> (Double) o.get("distance")))
                .limit(MAX_RESULTS)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error finding nearby reports", e);
            return List.of();
        }
    }
    
    /**
     * Get geographic statistics for a company's operations.
     */
    public Map<String, Object> getLocationStatistics(UUID companyId) {
        logger.debug("Calculating location statistics for company: {}", companyId);
        
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Count entities with location data
            long schoolsWithLocation = schoolRepository
                .findAll(PageRequest.of(0, 1000))
                .getContent()
                .stream()
                .filter(s -> s.getCompany().getId().equals(companyId))
                .filter(s -> s.getDeletedAt() == null)
                .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                .count();
            
            long workOrdersWithLocation = workOrderRepository
                .findAll(PageRequest.of(0, 1000))
                .getContent()
                .stream()
                .filter(wo -> wo.getCompanyId().equals(companyId))
                .filter(wo -> wo.getDeletedAt() == null)
                .filter(wo -> wo.getLatitude() != null && wo.getLongitude() != null)
                .count();
            
            long supervisorsWithLocation = userRepository
                .findByUserTypeAndCompany_IdAndDeletedAtIsNull(UserType.SUPERVISOR, companyId)
                .stream()
                .filter(u -> u.getLastKnownLatitude() != null && u.getLastKnownLongitude() != null)
                .count();
            
            long reportsWithLocation = reportRepository
                .findAll(PageRequest.of(0, 1000))
                .getContent()
                .stream()
                .filter(r -> r.getSchool().getCompany().getId().equals(companyId))
                .filter(r -> r.getDeletedAt() == null)
                .filter(r -> r.getLatitude() != null && r.getLongitude() != null)
                .count();
            
            stats.put("schoolsWithLocation", schoolsWithLocation);
            stats.put("workOrdersWithLocation", workOrdersWithLocation);
            stats.put("supervisorsWithLocation", supervisorsWithLocation);
            stats.put("reportsWithLocation", reportsWithLocation);
            stats.put("totalLocationDataPoints", 
                     schoolsWithLocation + workOrdersWithLocation + supervisorsWithLocation + reportsWithLocation);
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error calculating location statistics", e);
            return Map.of("error", "Failed to calculate statistics");
        }
    }
    
    /**
     * Calculate distance between two points using Haversine formula.
     * More accurate than simple coordinate distance for geographic points.
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        if (lat1 == lat2 && lon1 == lon2) {
            return 0.0;
        }
        
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);
        
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                  Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                  Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
    
    /**
     * Check if a point is within a given radius of a center point.
     */
    public boolean isWithinRadius(double centerLat, double centerLon, 
                                 double pointLat, double pointLon, double radiusKm) {
        double distance = calculateDistance(centerLat, centerLon, pointLat, pointLon);
        return distance <= radiusKm;
    }
    
    /**
     * Calculate the bounding box for a given center point and radius.
     * Useful for optimizing spatial queries.
     */
    public Map<String, Double> calculateBoundingBox(double centerLat, double centerLon, double radiusKm) {
        // Simple approximation - in production, use more precise geodetic calculations
        double latDelta = radiusKm / 111.0; // Approximately 111 km per degree latitude
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(centerLat))); // Longitude varies by latitude
        
        return Map.of(
            "minLat", centerLat - latDelta,
            "maxLat", centerLat + latDelta,
            "minLon", centerLon - lonDelta,
            "maxLon", centerLon + lonDelta
        );
    }
}