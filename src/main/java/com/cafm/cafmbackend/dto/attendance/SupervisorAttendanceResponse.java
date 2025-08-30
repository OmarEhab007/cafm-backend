package com.cafm.cafmbackend.dto.attendance;

import com.cafm.cafmbackend.infrastructure.persistence.entity.SupervisorAttendance.AttendanceStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for supervisor attendance data.
 */
public record SupervisorAttendanceResponse(
    UUID id,
    
    UUID supervisorId,
    String supervisorName,
    
    UUID companyId,
    String companyName,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate attendanceDate,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime checkInTime,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime checkOutTime,
    
    BigDecimal checkInLatitude,
    BigDecimal checkInLongitude,
    
    BigDecimal checkOutLatitude,
    BigDecimal checkOutLongitude,
    
    String checkInAddress,
    String checkOutAddress,
    
    String checkInPhotoUrl,
    String checkOutPhotoUrl,
    
    AttendanceStatus status,
    
    List<UUID> schoolsVisited,
    List<SchoolVisitInfo> schoolVisitDetails,
    
    Integer totalVisits,
    BigDecimal totalDistance,
    Integer workingHours,
    
    String notes,
    
    String deviceId,
    String deviceInfo,
    
    Boolean verified,
    String verifiedBy,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime verifiedAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt,
    
    AttendanceAnalytics analytics
) {
    /**
     * School visit information.
     */
    public record SchoolVisitInfo(
        UUID schoolId,
        String schoolName,
        LocalDateTime visitTime,
        Integer duration,
        BigDecimal distanceFromPrevious
    ) {}
    
    /**
     * Attendance analytics and insights.
     */
    public record AttendanceAnalytics(
        Double punctualityScore,
        Double productivityScore,
        Integer overtimeHours,
        Integer lateMinutes,
        Boolean isComplete,
        String performanceLevel,
        List<String> anomalies
    ) {
        public static AttendanceAnalytics calculate(
                SupervisorAttendanceResponse response) {
            
            boolean isComplete = response.checkInTime() != null && 
                               response.checkOutTime() != null;
            
            double punctualityScore = calculatePunctualityScore(
                response.checkInTime(),
                response.attendanceDate()
            );
            
            double productivityScore = calculateProductivityScore(
                response.totalVisits(),
                response.workingHours()
            );
            
            int overtimeHours = calculateOvertimeHours(response.workingHours());
            
            int lateMinutes = calculateLateMinutes(
                response.checkInTime(),
                response.attendanceDate()
            );
            
            String performanceLevel = determinePerformanceLevel(
                punctualityScore,
                productivityScore,
                response.totalVisits()
            );
            
            List<String> anomalies = detectAnomalies(response);
            
            return new AttendanceAnalytics(
                punctualityScore,
                productivityScore,
                overtimeHours,
                lateMinutes,
                isComplete,
                performanceLevel,
                anomalies
            );
        }
        
        private static double calculatePunctualityScore(
                LocalDateTime checkIn,
                LocalDate date) {
            if (checkIn == null) return 0.0;
            
            LocalDateTime expectedTime = date.atTime(8, 0);
            long minutesLate = java.time.Duration.between(expectedTime, checkIn).toMinutes();
            
            if (minutesLate <= 0) return 100.0;
            if (minutesLate <= 15) return 90.0;
            if (minutesLate <= 30) return 75.0;
            if (minutesLate <= 60) return 50.0;
            return 25.0;
        }
        
        private static double calculateProductivityScore(
                Integer visits,
                Integer workingHours) {
            if (visits == null || workingHours == null || workingHours == 0) {
                return 0.0;
            }
            
            double visitsPerHour = (double) visits / workingHours;
            double expectedVisitsPerHour = 0.5; // Expected 1 visit per 2 hours
            
            return Math.min(100.0, (visitsPerHour / expectedVisitsPerHour) * 100);
        }
        
        private static int calculateOvertimeHours(Integer workingHours) {
            if (workingHours == null) return 0;
            int standardHours = 8;
            return Math.max(0, workingHours - standardHours);
        }
        
        private static int calculateLateMinutes(
                LocalDateTime checkIn,
                LocalDate date) {
            if (checkIn == null) return 0;
            
            LocalDateTime expectedTime = date.atTime(8, 0);
            long minutesLate = java.time.Duration.between(expectedTime, checkIn).toMinutes();
            
            return Math.max(0, (int) minutesLate);
        }
        
        private static String determinePerformanceLevel(
                double punctualityScore,
                double productivityScore,
                Integer visits) {
            
            double avgScore = (punctualityScore + productivityScore) / 2;
            
            if (avgScore >= 90 && visits >= 3) return "EXCELLENT";
            if (avgScore >= 75 && visits >= 2) return "GOOD";
            if (avgScore >= 50) return "SATISFACTORY";
            return "NEEDS_IMPROVEMENT";
        }
        
        private static List<String> detectAnomalies(SupervisorAttendanceResponse response) {
            List<String> anomalies = new java.util.ArrayList<>();
            
            if (response.checkInTime() != null && response.checkOutTime() == null) {
                anomalies.add("CHECK_OUT_MISSING");
            }
            
            if (response.workingHours() != null && response.workingHours() > 12) {
                anomalies.add("EXCESSIVE_HOURS");
            }
            
            if (response.totalVisits() != null && response.totalVisits() == 0 && 
                response.workingHours() != null && response.workingHours() > 4) {
                anomalies.add("NO_VISITS_RECORDED");
            }
            
            if (response.checkInPhotoUrl() == null || response.checkInPhotoUrl().isEmpty()) {
                anomalies.add("CHECK_IN_PHOTO_MISSING");
            }
            
            return anomalies;
        }
    }
}