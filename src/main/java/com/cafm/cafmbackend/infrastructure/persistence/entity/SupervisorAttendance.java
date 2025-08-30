package com.cafm.cafmbackend.infrastructure.persistence.entity;

import com.cafm.cafmbackend.infrastructure.persistence.entity.base.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing supervisor attendance at schools.
 * Tracks check-in/out times, location, and work performed.
 */
@Entity
@Table(name = "supervisor_attendance")
public class SupervisorAttendance extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private User supervisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @NotNull
    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    @Column(name = "check_in_latitude", precision = 10, scale = 8)
    private BigDecimal checkInLatitude;

    @Column(name = "check_in_longitude", precision = 11, scale = 8)
    private BigDecimal checkInLongitude;

    @Column(name = "check_out_latitude", precision = 10, scale = 8)
    private BigDecimal checkOutLatitude;

    @Column(name = "check_out_longitude", precision = 11, scale = 8)
    private BigDecimal checkOutLongitude;

    @Column(name = "work_summary", columnDefinition = "TEXT")
    private String workSummary;

    @Column(name = "issues_found", columnDefinition = "TEXT")
    private String issuesFound;

    @Column(name = "actions_taken", columnDefinition = "TEXT")
    private String actionsTaken;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "check_in_photo")
    private String checkInPhoto;

    @Column(name = "check_out_photo")
    private String checkOutPhoto;

    @Type(JsonType.class)
    @Column(name = "schools_visited", columnDefinition = "jsonb")
    private List<UUID> schoolsVisited = new ArrayList<>();

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private AttendanceStatus status = AttendanceStatus.CHECKED_IN;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verified")
    private Boolean verified = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public SupervisorAttendance() {
        super();
    }

    private SupervisorAttendance(Builder builder) {
        this.supervisor = builder.supervisor;
        this.school = builder.school;
        this.company = builder.company;
        this.attendanceDate = builder.attendanceDate;
        this.checkInTime = builder.checkInTime;
        this.checkInLatitude = builder.checkInLatitude;
        this.checkInLongitude = builder.checkInLongitude;
        this.workSummary = builder.workSummary;
        this.checkInPhoto = builder.checkInPhoto;
        this.status = builder.status;
        this.verificationCode = builder.verificationCode;
    }

    public void checkIn(BigDecimal latitude, BigDecimal longitude, String photoUrl) {
        this.checkInTime = LocalTime.now();
        this.checkInLatitude = latitude;
        this.checkInLongitude = longitude;
        this.checkInPhoto = photoUrl;
        this.status = AttendanceStatus.CHECKED_IN;
    }

    public void checkOut(BigDecimal latitude, BigDecimal longitude, String photoUrl) {
        this.checkOutTime = LocalTime.now();
        this.checkOutLatitude = latitude;
        this.checkOutLongitude = longitude;
        this.checkOutPhoto = photoUrl;
        this.status = AttendanceStatus.CHECKED_OUT;
    }

    public void addSchoolVisited(UUID schoolId) {
        if (schoolsVisited == null) {
            schoolsVisited = new ArrayList<>();
        }
        if (!schoolsVisited.contains(schoolId)) {
            schoolsVisited.add(schoolId);
        }
    }

    public void verify() {
        this.verified = true;
        this.status = AttendanceStatus.VERIFIED;
    }

    public boolean isComplete() {
        return checkInTime != null && checkOutTime != null;
    }

    // Getters and Setters
    public User getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(User supervisor) {
        this.supervisor = supervisor;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public BigDecimal getCheckInLatitude() {
        return checkInLatitude;
    }

    public void setCheckInLatitude(BigDecimal checkInLatitude) {
        this.checkInLatitude = checkInLatitude;
    }

    public BigDecimal getCheckInLongitude() {
        return checkInLongitude;
    }

    public void setCheckInLongitude(BigDecimal checkInLongitude) {
        this.checkInLongitude = checkInLongitude;
    }

    public BigDecimal getCheckOutLatitude() {
        return checkOutLatitude;
    }

    public void setCheckOutLatitude(BigDecimal checkOutLatitude) {
        this.checkOutLatitude = checkOutLatitude;
    }

    public BigDecimal getCheckOutLongitude() {
        return checkOutLongitude;
    }

    public void setCheckOutLongitude(BigDecimal checkOutLongitude) {
        this.checkOutLongitude = checkOutLongitude;
    }

    public String getWorkSummary() {
        return workSummary;
    }

    public void setWorkSummary(String workSummary) {
        this.workSummary = workSummary;
    }

    public String getIssuesFound() {
        return issuesFound;
    }

    public void setIssuesFound(String issuesFound) {
        this.issuesFound = issuesFound;
    }

    public String getActionsTaken() {
        return actionsTaken;
    }

    public void setActionsTaken(String actionsTaken) {
        this.actionsTaken = actionsTaken;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getCheckInPhoto() {
        return checkInPhoto;
    }

    public void setCheckInPhoto(String checkInPhoto) {
        this.checkInPhoto = checkInPhoto;
    }

    public String getCheckOutPhoto() {
        return checkOutPhoto;
    }

    public void setCheckOutPhoto(String checkOutPhoto) {
        this.checkOutPhoto = checkOutPhoto;
    }

    public List<UUID> getSchoolsVisited() {
        return schoolsVisited;
    }

    public void setSchoolsVisited(List<UUID> schoolsVisited) {
        this.schoolsVisited = schoolsVisited;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private User supervisor;
        private School school;
        private Company company;
        private LocalDate attendanceDate;
        private LocalTime checkInTime;
        private BigDecimal checkInLatitude;
        private BigDecimal checkInLongitude;
        private String workSummary;
        private String checkInPhoto;
        private AttendanceStatus status = AttendanceStatus.CHECKED_IN;
        private String verificationCode;

        public Builder supervisor(User supervisor) {
            this.supervisor = supervisor;
            return this;
        }

        public Builder school(School school) {
            this.school = school;
            return this;
        }

        public Builder company(Company company) {
            this.company = company;
            return this;
        }

        public Builder attendanceDate(LocalDate attendanceDate) {
            this.attendanceDate = attendanceDate;
            return this;
        }

        public Builder checkInTime(LocalTime checkInTime) {
            this.checkInTime = checkInTime;
            return this;
        }

        public Builder checkInLatitude(BigDecimal checkInLatitude) {
            this.checkInLatitude = checkInLatitude;
            return this;
        }

        public Builder checkInLongitude(BigDecimal checkInLongitude) {
            this.checkInLongitude = checkInLongitude;
            return this;
        }

        public Builder workSummary(String workSummary) {
            this.workSummary = workSummary;
            return this;
        }

        public Builder checkInPhoto(String checkInPhoto) {
            this.checkInPhoto = checkInPhoto;
            return this;
        }

        public Builder status(AttendanceStatus status) {
            this.status = status;
            return this;
        }

        public Builder verificationCode(String verificationCode) {
            this.verificationCode = verificationCode;
            return this;
        }

        public SupervisorAttendance build() {
            return new SupervisorAttendance(this);
        }
    }

    public enum AttendanceStatus {
        CHECKED_IN,
        CHECKED_OUT,
        VERIFIED,
        ABSENT,
        LATE
    }
}