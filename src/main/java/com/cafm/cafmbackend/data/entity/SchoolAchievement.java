package com.cafm.cafmbackend.data.entity;

import com.cafm.cafmbackend.data.entity.base.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a school achievement submission with photos.
 * Tracks maintenance achievements, AC achievements, and checklists.
 */
@Entity
@Table(name = "school_achievements")
public class SchoolAchievement extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private User supervisor;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotNull
    @Column(name = "school_name", nullable = false)
    @Size(max = 255)
    private String schoolName;

    @NotNull
    @Column(name = "achievement_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AchievementType achievementType;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "title")
    @Size(max = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Type(JsonType.class)
    @Column(name = "photos", columnDefinition = "jsonb")
    private List<String> photos = new ArrayList<>();

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @NotNull
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AchievementStatus status = AchievementStatus.DRAFT;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public SchoolAchievement() {
        super();
    }

    private SchoolAchievement(Builder builder) {
        this.school = builder.school;
        this.supervisor = builder.supervisor;
        this.company = builder.company;
        this.schoolName = builder.schoolName;
        this.achievementType = builder.achievementType;
        this.category = builder.category;
        this.title = builder.title;
        this.description = builder.description;
        this.photos = builder.photos;
        this.notes = builder.notes;
        this.status = builder.status;
        this.submittedAt = builder.submittedAt;
    }

    public void submit() {
        if (this.status == AchievementStatus.DRAFT || this.status == AchievementStatus.PENDING) {
            this.status = AchievementStatus.SUBMITTED;
            this.submittedAt = LocalDateTime.now();
        }
    }

    public void approve(User approver, String approvalNotes) {
        if (this.status == AchievementStatus.SUBMITTED) {
            this.status = AchievementStatus.APPROVED;
            this.approvedAt = LocalDateTime.now();
            this.approvedBy = approver;
            this.approvalNotes = approvalNotes;
        }
    }

    public void reject(User approver, String rejectionReason) {
        if (this.status == AchievementStatus.SUBMITTED) {
            this.status = AchievementStatus.REJECTED;
            this.approvedAt = LocalDateTime.now();
            this.approvedBy = approver;
            this.approvalNotes = rejectionReason;
        }
    }

    public void addPhoto(String photoUrl) {
        if (photos == null) {
            photos = new ArrayList<>();
        }
        photos.add(photoUrl);
    }

    public void removePhoto(String photoUrl) {
        if (photos != null) {
            photos.remove(photoUrl);
        }
    }

    // Getters and Setters
    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    public User getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(User supervisor) {
        this.supervisor = supervisor;
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

    public AchievementType getAchievementType() {
        return achievementType;
    }

    public void setAchievementType(AchievementType achievementType) {
        this.achievementType = achievementType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public AchievementStatus getStatus() {
        return status;
    }

    public void setStatus(AchievementStatus status) {
        this.status = status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getApprovalNotes() {
        return approvalNotes;
    }

    public void setApprovalNotes(String approvalNotes) {
        this.approvalNotes = approvalNotes;
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
        private School school;
        private User supervisor;
        private Company company;
        private String schoolName;
        private AchievementType achievementType;
        private String category;
        private String title;
        private String description;
        private List<String> photos = new ArrayList<>();
        private String notes;
        private AchievementStatus status = AchievementStatus.DRAFT;
        private LocalDateTime submittedAt;

        public Builder school(School school) {
            this.school = school;
            return this;
        }

        public Builder supervisor(User supervisor) {
            this.supervisor = supervisor;
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

        public Builder achievementType(AchievementType achievementType) {
            this.achievementType = achievementType;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder photos(List<String> photos) {
            this.photos = photos;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder status(AchievementStatus status) {
            this.status = status;
            return this;
        }

        public Builder submittedAt(LocalDateTime submittedAt) {
            this.submittedAt = submittedAt;
            return this;
        }

        public SchoolAchievement build() {
            return new SchoolAchievement(this);
        }
    }

    public enum AchievementType {
        MAINTENANCE_ACHIEVEMENT,
        AC_ACHIEVEMENT,
        CHECKLIST
    }

    public enum AchievementStatus {
        DRAFT,
        PENDING,
        SUBMITTED,
        APPROVED,
        REJECTED
    }
}