package com.cafm.cafmbackend.dto.attendance;

import com.cafm.cafmbackend.data.entity.SupervisorAttendance.AttendanceStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating or updating supervisor attendance records.
 */
public record SupervisorAttendanceRequest(
    @NotNull(message = "Supervisor ID is required")
    UUID supervisorId,
    
    @NotNull(message = "Company ID is required")
    UUID companyId,
    
    @NotNull(message = "Attendance date is required")
    LocalDate attendanceDate,
    
    LocalDateTime checkInTime,
    LocalDateTime checkOutTime,
    
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    BigDecimal checkInLatitude,
    
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    BigDecimal checkInLongitude,
    
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    BigDecimal checkOutLatitude,
    
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    BigDecimal checkOutLongitude,
    
    String checkInAddress,
    String checkOutAddress,
    
    String checkInPhotoUrl,
    String checkOutPhotoUrl,
    
    AttendanceStatus status,
    
    List<UUID> schoolsVisited,
    
    Integer totalVisits,
    
    BigDecimal totalDistance,
    
    Integer workingHours,
    
    String notes,
    
    String deviceId,
    String deviceInfo,
    
    Boolean verified,
    String verifiedBy
) {
    /**
     * Creates a check-in request.
     */
    public static SupervisorAttendanceRequest checkIn(
            UUID supervisorId,
            UUID companyId,
            BigDecimal latitude,
            BigDecimal longitude,
            String photoUrl,
            String deviceId) {
        return new SupervisorAttendanceRequest(
            supervisorId,
            companyId,
            LocalDate.now(),
            LocalDateTime.now(),
            null,
            latitude,
            longitude,
            null,
            null,
            null,
            null,
            photoUrl,
            null,
            AttendanceStatus.CHECKED_IN,
            List.of(),
            0,
            BigDecimal.ZERO,
            0,
            null,
            deviceId,
            null,
            false,
            null
        );
    }
    
    /**
     * Creates a check-out request.
     */
    public static SupervisorAttendanceRequest checkOut(
            UUID supervisorId,
            UUID companyId,
            BigDecimal latitude,
            BigDecimal longitude,
            String photoUrl,
            List<UUID> schoolsVisited) {
        return new SupervisorAttendanceRequest(
            supervisorId,
            companyId,
            LocalDate.now(),
            null,
            LocalDateTime.now(),
            null,
            null,
            latitude,
            longitude,
            null,
            null,
            null,
            photoUrl,
            AttendanceStatus.CHECKED_OUT,
            schoolsVisited,
            schoolsVisited.size(),
            null,
            null,
            null,
            null,
            null,
            false,
            null
        );
    }
    
    /**
     * Builder for complex attendance requests.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private UUID supervisorId;
        private UUID companyId;
        private LocalDate attendanceDate = LocalDate.now();
        private LocalDateTime checkInTime;
        private LocalDateTime checkOutTime;
        private BigDecimal checkInLatitude;
        private BigDecimal checkInLongitude;
        private BigDecimal checkOutLatitude;
        private BigDecimal checkOutLongitude;
        private String checkInAddress;
        private String checkOutAddress;
        private String checkInPhotoUrl;
        private String checkOutPhotoUrl;
        private AttendanceStatus status = AttendanceStatus.CHECKED_IN;
        private List<UUID> schoolsVisited = List.of();
        private Integer totalVisits = 0;
        private BigDecimal totalDistance = BigDecimal.ZERO;
        private Integer workingHours = 0;
        private String notes;
        private String deviceId;
        private String deviceInfo;
        private Boolean verified = false;
        private String verifiedBy;
        
        public Builder supervisorId(UUID supervisorId) {
            this.supervisorId = supervisorId;
            return this;
        }
        
        public Builder companyId(UUID companyId) {
            this.companyId = companyId;
            return this;
        }
        
        public Builder attendanceDate(LocalDate attendanceDate) {
            this.attendanceDate = attendanceDate;
            return this;
        }
        
        public Builder checkInTime(LocalDateTime checkInTime) {
            this.checkInTime = checkInTime;
            return this;
        }
        
        public Builder checkOutTime(LocalDateTime checkOutTime) {
            this.checkOutTime = checkOutTime;
            return this;
        }
        
        public Builder checkInLocation(BigDecimal latitude, BigDecimal longitude) {
            this.checkInLatitude = latitude;
            this.checkInLongitude = longitude;
            return this;
        }
        
        public Builder checkOutLocation(BigDecimal latitude, BigDecimal longitude) {
            this.checkOutLatitude = latitude;
            this.checkOutLongitude = longitude;
            return this;
        }
        
        public Builder checkInAddress(String checkInAddress) {
            this.checkInAddress = checkInAddress;
            return this;
        }
        
        public Builder checkOutAddress(String checkOutAddress) {
            this.checkOutAddress = checkOutAddress;
            return this;
        }
        
        public Builder checkInPhotoUrl(String checkInPhotoUrl) {
            this.checkInPhotoUrl = checkInPhotoUrl;
            return this;
        }
        
        public Builder checkOutPhotoUrl(String checkOutPhotoUrl) {
            this.checkOutPhotoUrl = checkOutPhotoUrl;
            return this;
        }
        
        public Builder status(AttendanceStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder schoolsVisited(List<UUID> schoolsVisited) {
            this.schoolsVisited = schoolsVisited;
            return this;
        }
        
        public Builder totalVisits(Integer totalVisits) {
            this.totalVisits = totalVisits;
            return this;
        }
        
        public Builder totalDistance(BigDecimal totalDistance) {
            this.totalDistance = totalDistance;
            return this;
        }
        
        public Builder workingHours(Integer workingHours) {
            this.workingHours = workingHours;
            return this;
        }
        
        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }
        
        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }
        
        public Builder deviceInfo(String deviceInfo) {
            this.deviceInfo = deviceInfo;
            return this;
        }
        
        public Builder verified(Boolean verified) {
            this.verified = verified;
            return this;
        }
        
        public Builder verifiedBy(String verifiedBy) {
            this.verifiedBy = verifiedBy;
            return this;
        }
        
        public SupervisorAttendanceRequest build() {
            return new SupervisorAttendanceRequest(
                supervisorId,
                companyId,
                attendanceDate,
                checkInTime,
                checkOutTime,
                checkInLatitude,
                checkInLongitude,
                checkOutLatitude,
                checkOutLongitude,
                checkInAddress,
                checkOutAddress,
                checkInPhotoUrl,
                checkOutPhotoUrl,
                status,
                schoolsVisited,
                totalVisits,
                totalDistance,
                workingHours,
                notes,
                deviceId,
                deviceInfo,
                verified,
                verifiedBy
            );
        }
    }
}