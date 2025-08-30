package com.cafm.cafmbackend.infrastructure.persistence.entity;

import com.cafm.cafmbackend.infrastructure.persistence.entity.base.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a maintenance report separate from general reports.
 * Tracks maintenance issues with photos and completion workflow.
 */
@Entity
@Table(name = "maintenance_reports")
public class MaintenanceReport extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private User supervisor;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "school_name")
    private String schoolName;

    @NotNull
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private MaintenanceReportStatus status = MaintenanceReportStatus.OPEN;

    @Column(name = "priority", length = 20)
    @Enumerated(EnumType.STRING)
    private PriorityLevel priority;

    @Type(JsonType.class)
    @Column(name = "images", columnDefinition = "jsonb")
    private List<String> images = new ArrayList<>();

    @Type(JsonType.class)
    @Column(name = "completion_photos", columnDefinition = "jsonb")
    private List<String> completionPhotos = new ArrayList<>();

    @Column(name = "completion_note", columnDefinition = "TEXT")
    private String completionNote;

    @Column(name = "category", length = 50)
    @Enumerated(EnumType.STRING)
    private MaintenanceCategory category;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "estimated_hours")
    private Integer estimatedHours;

    @Column(name = "actual_hours")
    private Integer actualHours;

    @Column(name = "verification_required")
    private Boolean verificationRequired = false;

    @Column(name = "verified")
    private Boolean verified = false;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;

    @Column(name = "reference_number", unique = true)
    private String referenceNumber;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public MaintenanceReport() {
        super();
    }

    private MaintenanceReport(Builder builder) {
        this.supervisor = builder.supervisor;
        this.school = builder.school;
        this.company = builder.company;
        this.schoolName = builder.schoolName;
        this.description = builder.description;
        this.status = builder.status;
        this.priority = builder.priority;
        this.images = builder.images;
        this.category = builder.category;
        this.location = builder.location;
        this.estimatedHours = builder.estimatedHours;
        this.verificationRequired = builder.verificationRequired;
        this.referenceNumber = builder.referenceNumber;
    }

    @PrePersist
    private void generateReferenceNumber() {
        if (referenceNumber == null) {
            // Generate format: MR-YYYYMM-XXXXX
            LocalDateTime now = LocalDateTime.now();
            String prefix = String.format("MR-%d%02d-", now.getYear(), now.getMonthValue());
            String random = String.format("%05d", (int) (Math.random() * 100000));
            this.referenceNumber = prefix + random;
        }
    }

    public void assign(UUID technicianId) {
        this.assignedTo = technicianId;
        this.assignedAt = LocalDateTime.now();
        this.status = MaintenanceReportStatus.ASSIGNED;
    }

    public void start() {
        if (this.status == MaintenanceReportStatus.ASSIGNED || this.status == MaintenanceReportStatus.OPEN) {
            this.startedAt = LocalDateTime.now();
            this.status = MaintenanceReportStatus.IN_PROGRESS;
        }
    }

    public void complete(String completionNote, List<String> completionPhotos) {
        if (this.status == MaintenanceReportStatus.IN_PROGRESS) {
            this.completionNote = completionNote;
            this.completionPhotos = completionPhotos;
            this.status = MaintenanceReportStatus.COMPLETED;
            this.closedAt = LocalDateTime.now();
            
            if (actualHours == null && startedAt != null) {
                long hours = java.time.Duration.between(startedAt, closedAt).toHours();
                this.actualHours = (int) hours;
            }
        }
    }

    public void close(UUID closedByUserId) {
        this.status = MaintenanceReportStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
        this.closedBy = closedByUserId;
    }

    public void verify(UUID verifierId, String notes) {
        if (this.verificationRequired && this.status == MaintenanceReportStatus.COMPLETED) {
            this.verified = true;
            this.verifiedBy = verifierId;
            this.verifiedAt = LocalDateTime.now();
            this.verificationNotes = notes;
            this.status = MaintenanceReportStatus.VERIFIED;
        }
    }

    public void reopen() {
        if (this.status == MaintenanceReportStatus.CLOSED || this.status == MaintenanceReportStatus.COMPLETED) {
            this.status = MaintenanceReportStatus.OPEN;
            this.closedAt = null;
            this.closedBy = null;
        }
    }

    public void cancel() {
        if (this.status != MaintenanceReportStatus.COMPLETED && this.status != MaintenanceReportStatus.CLOSED) {
            this.status = MaintenanceReportStatus.CANCELLED;
            this.closedAt = LocalDateTime.now();
        }
    }

    public void addImage(String imageUrl) {
        if (images == null) {
            images = new ArrayList<>();
        }
        images.add(imageUrl);
    }

    public void addCompletionPhoto(String photoUrl) {
        if (completionPhotos == null) {
            completionPhotos = new ArrayList<>();
        }
        completionPhotos.add(photoUrl);
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

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MaintenanceReportStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceReportStatus status) {
        this.status = status;
    }

    public PriorityLevel getPriority() {
        return priority;
    }

    public void setPriority(PriorityLevel priority) {
        this.priority = priority;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public List<String> getCompletionPhotos() {
        return completionPhotos;
    }

    public void setCompletionPhotos(List<String> completionPhotos) {
        this.completionPhotos = completionPhotos;
    }

    public String getCompletionNote() {
        return completionNote;
    }

    public void setCompletionNote(String completionNote) {
        this.completionNote = completionNote;
    }

    public MaintenanceCategory getCategory() {
        return category;
    }

    public void setCategory(MaintenanceCategory category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public UUID getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(UUID assignedTo) {
        this.assignedTo = assignedTo;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public UUID getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(UUID closedBy) {
        this.closedBy = closedBy;
    }

    public Integer getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(Integer estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public Integer getActualHours() {
        return actualHours;
    }

    public void setActualHours(Integer actualHours) {
        this.actualHours = actualHours;
    }

    public Boolean getVerificationRequired() {
        return verificationRequired;
    }

    public void setVerificationRequired(Boolean verificationRequired) {
        this.verificationRequired = verificationRequired;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public UUID getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(UUID verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getVerificationNotes() {
        return verificationNotes;
    }

    public void setVerificationNotes(String verificationNotes) {
        this.verificationNotes = verificationNotes;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
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
        private String schoolName;
        private String description;
        private MaintenanceReportStatus status = MaintenanceReportStatus.OPEN;
        private PriorityLevel priority;
        private List<String> images = new ArrayList<>();
        private MaintenanceCategory category;
        private String location;
        private Integer estimatedHours;
        private Boolean verificationRequired = false;
        private String referenceNumber;

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

        public Builder schoolName(String schoolName) {
            this.schoolName = schoolName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder status(MaintenanceReportStatus status) {
            this.status = status;
            return this;
        }

        public Builder priority(PriorityLevel priority) {
            this.priority = priority;
            return this;
        }

        public Builder images(List<String> images) {
            this.images = images;
            return this;
        }

        public Builder category(MaintenanceCategory category) {
            this.category = category;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder estimatedHours(Integer estimatedHours) {
            this.estimatedHours = estimatedHours;
            return this;
        }

        public Builder verificationRequired(Boolean verificationRequired) {
            this.verificationRequired = verificationRequired;
            return this;
        }

        public Builder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }

        public MaintenanceReport build() {
            return new MaintenanceReport(this);
        }
    }

    public enum MaintenanceReportStatus {
        OPEN,
        ASSIGNED,
        IN_PROGRESS,
        COMPLETED,
        VERIFIED,
        CLOSED,
        CANCELLED,
        ON_HOLD
    }

    public enum MaintenanceCategory {
        ELECTRICAL,
        PLUMBING,
        HVAC,
        CIVIL,
        CARPENTRY,
        PAINTING,
        CLEANING,
        SAFETY,
        IT_EQUIPMENT,
        GENERAL,
        OTHER
    }

    public enum PriorityLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL,
        URGENT
    }
}