package com.cafm.cafmbackend.domain.services;

import com.cafm.cafmbackend.infrastructure.persistence.entity.*;
import com.cafm.cafmbackend.infrastructure.persistence.repository.*;
import com.cafm.cafmbackend.shared.enums.UserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing supervisor attendance and check-in/check-out operations.
 * 
 * Purpose: Handles supervisor location tracking and attendance management
 * Pattern: Domain service with geolocation and time tracking capabilities
 * Java 23: Uses modern time APIs and pattern matching for status handling
 * Architecture: Domain layer service for attendance business logic
 * Standards: Constructor injection, transaction management, comprehensive logging
 */
@Service
@Transactional
public class SupervisorAttendanceService {
    
    private static final Logger logger = LoggerFactory.getLogger(SupervisorAttendanceService.class);
    private static final int MAX_CHECK_IN_DISTANCE_METERS = 100; // 100 meters radius
    
    private final UserRepository userRepository;
    private final SupervisorAttendanceRepository supervisorAttendanceRepository;
    private final SchoolRepository schoolRepository;
    private final SupervisorSchoolRepository supervisorSchoolRepository;
    
    @Autowired
    public SupervisorAttendanceService(
            UserRepository userRepository,
            SupervisorAttendanceRepository supervisorAttendanceRepository,
            SchoolRepository schoolRepository,
            SupervisorSchoolRepository supervisorSchoolRepository) {
        this.userRepository = userRepository;
        this.supervisorAttendanceRepository = supervisorAttendanceRepository;
        this.schoolRepository = schoolRepository;
        this.supervisorSchoolRepository = supervisorSchoolRepository;
    }
    
    /**
     * Process supervisor check-in at a school location.
     */
    public Map<String, Object> processCheckIn(String userEmail, Map<String, Object> checkInData) {
        logger.info("Processing check-in for supervisor: {}", userEmail);
        
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
        
        if (user.getUserType() != UserType.SUPERVISOR) {
            throw new IllegalArgumentException("User is not a supervisor: " + userEmail);
        }
        
        // Check if already checked in
        Optional<SupervisorAttendance> activeCheckIn = findActiveCheckIn(user.getId());
        if (activeCheckIn.isPresent()) {
            throw new IllegalStateException("Supervisor is already checked in at: " + 
                activeCheckIn.get().getSchool().getName());
        }
        
        // Validate school and location
        UUID schoolId = UUID.fromString((String) checkInData.get("school_id"));
        School school = validateSchoolAccess(user, schoolId);
        
        // Validate location if provided
        if (checkInData.containsKey("latitude") && checkInData.containsKey("longitude")) {
            validateLocation(school, checkInData);
        }
        
        // Create check-in record
        SupervisorAttendance attendance = new SupervisorAttendance();
        attendance.setSupervisor(user);
        attendance.setSchool(school);
        attendance.setAttendanceDate(LocalDate.now());
        attendance.setCheckInTime(LocalTime.now());
        
        // Set location data
        if (checkInData.containsKey("latitude")) {
            attendance.setCheckInLatitude(new BigDecimal(checkInData.get("latitude").toString()));
        }
        if (checkInData.containsKey("longitude")) {
            attendance.setCheckInLongitude(new BigDecimal(checkInData.get("longitude").toString()));
        }
        
        // Set additional metadata
        if (checkInData.containsKey("notes")) {
            attendance.setWorkSummary((String) checkInData.get("notes"));
        }
        
        attendance = supervisorAttendanceRepository.save(attendance);
        
        logger.info("Check-in recorded successfully: supervisor={}, school={}, attendance={}", 
                   user.getId(), school.getId(), attendance.getId());
        
        return Map.of(
            "status", "success",
            "message", "Check-in recorded successfully",
            "check_in_id", attendance.getId().toString(),
            "school_name", school.getName(),
            "check_in_time", attendance.getAttendanceDate().atTime(attendance.getCheckInTime()),
            "expected_duration", calculateExpectedDuration(user, school),
            "session_id", generateSessionId(attendance)
        );
    }
    
    /**
     * Process supervisor check-out.
     */
    public Map<String, Object> processCheckOut(String userEmail, Map<String, Object> checkOutData) {
        logger.info("Processing check-out for supervisor: {}", userEmail);
        
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
        
        // Find active check-in
        SupervisorAttendance attendance = findActiveCheckIn(user.getId())
            .orElseThrow(() -> new IllegalStateException("No active check-in found for supervisor"));
        
        // Process check-out
        LocalTime checkOutTime = LocalTime.now();
        attendance.setCheckOutTime(checkOutTime);
        
        // Set location data
        if (checkOutData.containsKey("latitude")) {
            attendance.setCheckOutLatitude(new BigDecimal(checkOutData.get("latitude").toString()));
        }
        if (checkOutData.containsKey("longitude")) {
            attendance.setCheckOutLongitude(new BigDecimal(checkOutData.get("longitude").toString()));
        }
        
        // Calculate work duration
        LocalDateTime checkInDateTime = attendance.getAttendanceDate().atTime(attendance.getCheckInTime());
        LocalDateTime checkOutDateTime = attendance.getAttendanceDate().atTime(checkOutTime);
        Duration workDuration = Duration.between(checkInDateTime, checkOutDateTime);
        
        // Set additional metadata
        if (checkOutData.containsKey("notes")) {
            String existingNotes = attendance.getWorkSummary() != null ? attendance.getWorkSummary() : "";
            attendance.setWorkSummary(existingNotes + "; Check-out: " + checkOutData.get("notes"));
        }
        
        attendance = supervisorAttendanceRepository.save(attendance);
        
        logger.info("Check-out recorded successfully: attendance={}, duration={}min", 
                   attendance.getId(), workDuration.toMinutes());
        
        return Map.of(
            "status", "success",
            "message", "Check-out recorded successfully",
            "attendance_id", attendance.getId().toString(),
            "school_name", attendance.getSchool().getName(),
            "check_in_time", attendance.getAttendanceDate().atTime(attendance.getCheckInTime()),
            "check_out_time", attendance.getAttendanceDate().atTime(checkOutTime),
            "work_duration_minutes", workDuration.toMinutes(),
            "work_duration_formatted", formatDuration(workDuration)
        );
    }
    
    /**
     * Get attendance history for a supervisor.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAttendanceHistory(String userEmail, Integer days, Integer page, Integer size) {
        logger.debug("Getting attendance history for supervisor: {}, days: {}", userEmail, days);
        
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
        
        LocalDate fromDate = LocalDate.now().minusDays(days);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "attendanceDate"));
        
        // Use date range query
        LocalDate toDate = LocalDate.now();
        List<SupervisorAttendance> allRecords = supervisorAttendanceRepository
            .findBySupervisorAndDateRange(user.getId(), fromDate, toDate);
        
        // Manual pagination for now (would be better to add a repository method)
        int start = page * size;
        int end = Math.min(start + size, allRecords.size());
        List<SupervisorAttendance> pageContent = start < allRecords.size() ? 
            allRecords.subList(start, end) : List.of();
        
        int totalElements = allRecords.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        Map<String, Object> statistics = calculateAttendanceStatistics(allRecords);
        
        // Format attendance records
        List<Map<String, Object>> attendanceRecords = pageContent
            .stream()
            .map(this::formatAttendanceRecord)
            .collect(Collectors.toList());
        
        return Map.of(
            "attendance_records", attendanceRecords,
            "statistics", statistics,
            "pagination", Map.of(
                "current_page", page,
                "total_pages", totalPages,
                "total_elements", totalElements,
                "page_size", size
            ),
            "period_days", days,
            "generated_at", LocalDateTime.now()
        );
    }
    
    /**
     * Get current check-in status.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentCheckInStatus(String userEmail) {
        logger.debug("Getting check-in status for supervisor: {}", userEmail);
        
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
        
        Optional<SupervisorAttendance> activeCheckIn = findActiveCheckIn(user.getId());
        
        if (activeCheckIn.isEmpty()) {
            return Map.of(
                "is_checked_in", false,
                "status", "NOT_CHECKED_IN",
                "message", "No active check-in session"
            );
        }
        
        SupervisorAttendance attendance = activeCheckIn.get();
        LocalDateTime checkInDateTime = attendance.getAttendanceDate().atTime(attendance.getCheckInTime());
        Duration currentDuration = Duration.between(checkInDateTime, LocalDateTime.now());
        
        return Map.of(
            "is_checked_in", true,
            "status", "CHECKED_IN",
            "attendance_id", attendance.getId().toString(),
            "school_id", attendance.getSchool().getId().toString(),
            "school_name", attendance.getSchool().getName(),
            "check_in_time", checkInDateTime,
            "current_duration_minutes", currentDuration.toMinutes(),
            "current_duration_formatted", formatDuration(currentDuration),
            "check_in_location", Map.of(
                "latitude", attendance.getCheckInLatitude(),
                "longitude", attendance.getCheckInLongitude()
            )
        );
    }
    
    private Optional<SupervisorAttendance> findActiveCheckIn(UUID supervisorId) {
        // Find attendance for today where check-out time is null
        return supervisorAttendanceRepository.findBySupervisorIdAndAttendanceDateAndDeletedAtIsNull(
            supervisorId, LocalDate.now())
            .filter(attendance -> attendance.getCheckOutTime() == null);
    }
    
    private School validateSchoolAccess(User user, UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new RuntimeException("School not found: " + schoolId));
        
        // Check if user has access to this school
        if (!school.getCompany().getId().equals(user.getCompanyId())) {
            throw new RuntimeException("User does not have access to school: " + schoolId);
        }
        
        // Check if supervisor is assigned to this school
        boolean isAssigned = supervisorSchoolRepository
            .existsBySupervisorIdAndSchoolId(user.getId(), schoolId);
        
        if (!isAssigned) {
            throw new RuntimeException("Supervisor is not assigned to school: " + schoolId);
        }
        
        return school;
    }
    
    private void validateLocation(School school, Map<String, Object> checkInData) {
        if (school.getLatitude() == null || school.getLongitude() == null) {
            // School location not set, skip validation
            return;
        }
        
        double userLat = Double.parseDouble(checkInData.get("latitude").toString());
        double userLng = Double.parseDouble(checkInData.get("longitude").toString());
        double schoolLat = school.getLatitude().doubleValue();
        double schoolLng = school.getLongitude().doubleValue();
        
        double distance = calculateDistance(userLat, userLng, schoolLat, schoolLng);
        
        if (distance > MAX_CHECK_IN_DISTANCE_METERS) {
            logger.warn("Check-in attempt from distance: {}m (max: {}m)", distance, MAX_CHECK_IN_DISTANCE_METERS);
            throw new IllegalArgumentException(
                String.format("Check-in location is too far from school (%.0fm). Please move closer.", distance));
        }
    }
    
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        // Haversine formula for calculating distance between two points
        final int R = 6371000; // Earth's radius in meters
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    private int calculateExpectedDuration(User supervisor, School school) {
        // Simple calculation based on school size or historical data
        // This would be more sophisticated in a real implementation
        return 480; // 8 hours default
    }
    
    private String generateSessionId(SupervisorAttendance attendance) {
        return String.format("SESSION-%s-%s", 
                           attendance.getId().toString().substring(0, 8),
                           System.currentTimeMillis());
    }
    
    private Map<String, Object> calculateAttendanceStatistics(List<SupervisorAttendance> records) {
        if (records.isEmpty()) {
            return Map.of(
                "total_sessions", 0,
                "total_hours", 0,
                "average_hours_per_day", 0,
                "schools_visited", 0
            );
        }
        
        int totalSessions = records.size();
        int completedSessions = (int) records.stream()
            .filter(r -> r.getCheckOutTime() != null)
            .count();
        
        int totalMinutes = records.stream()
            .filter(r -> r.getCheckInTime() != null && r.getCheckOutTime() != null)
            .mapToInt(r -> {
                LocalDateTime checkIn = r.getAttendanceDate().atTime(r.getCheckInTime());
                LocalDateTime checkOut = r.getAttendanceDate().atTime(r.getCheckOutTime());
                return (int) Duration.between(checkIn, checkOut).toMinutes();
            })
            .sum();
        
        Set<UUID> uniqueSchools = records.stream()
            .map(r -> r.getSchool().getId())
            .collect(Collectors.toSet());
        
        return Map.of(
            "total_sessions", totalSessions,
            "completed_sessions", completedSessions,
            "active_sessions", totalSessions - completedSessions,
            "total_hours", totalMinutes / 60.0,
            "average_hours_per_session", completedSessions > 0 ? (totalMinutes / 60.0) / completedSessions : 0,
            "schools_visited", uniqueSchools.size()
        );
    }
    
    private Map<String, Object> formatAttendanceRecord(SupervisorAttendance attendance) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", attendance.getId().toString());
        record.put("school_id", attendance.getSchool().getId().toString());
        record.put("school_name", attendance.getSchool().getName());
        record.put("check_in_time", attendance.getAttendanceDate().atTime(attendance.getCheckInTime()));
        record.put("check_out_time", attendance.getCheckOutTime() != null ? 
                  attendance.getAttendanceDate().atTime(attendance.getCheckOutTime()) : null);
        
        if (attendance.getCheckInTime() != null && attendance.getCheckOutTime() != null) {
            LocalDateTime checkIn = attendance.getAttendanceDate().atTime(attendance.getCheckInTime());
            LocalDateTime checkOut = attendance.getAttendanceDate().atTime(attendance.getCheckOutTime());
            Duration duration = Duration.between(checkIn, checkOut);
            record.put("work_duration_minutes", duration.toMinutes());
            record.put("work_duration_formatted", formatDuration(duration));
        }
        
        record.put("status", attendance.getCheckOutTime() != null ? "COMPLETED" : "ACTIVE");
        
        return record;
    }
    
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        return String.format("%dh %02dm", hours, minutes);
    }
}