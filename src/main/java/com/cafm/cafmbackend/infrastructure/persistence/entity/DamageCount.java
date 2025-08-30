package com.cafm.cafmbackend.infrastructure.persistence.entity;

import com.cafm.cafmbackend.infrastructure.persistence.entity.base.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing damage count items for a school.
 * Tracks damaged equipment and items inventory with photos.
 */
@Entity
@Table(name = "damage_counts")
public class DamageCount extends BaseEntity {

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
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DamageCountStatus status = DamageCountStatus.DRAFT;

    @Column(name = "priority")
    @Enumerated(EnumType.STRING)
    private PriorityLevel priority;

    @Type(JsonType.class)
    @Column(name = "item_counts", columnDefinition = "jsonb")
    private Map<String, Integer> itemCounts = new HashMap<>();

    @Type(JsonType.class)
    @Column(name = "section_photos", columnDefinition = "jsonb")
    private Map<String, List<String>> sectionPhotos = new HashMap<>();

    @Column(name = "total_items_count")
    private Integer totalItemsCount = 0;

    @Column(name = "estimated_repair_cost", precision = 10, scale = 2)
    private BigDecimal estimatedRepairCost;

    @Column(name = "repair_notes", columnDefinition = "TEXT")
    private String repairNotes;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public DamageCount() {
        super();
    }

    private DamageCount(Builder builder) {
        this.school = builder.school;
        this.supervisor = builder.supervisor;
        this.company = builder.company;
        this.schoolName = builder.schoolName;
        this.status = builder.status;
        this.priority = builder.priority;
        this.itemCounts = builder.itemCounts;
        this.sectionPhotos = builder.sectionPhotos;
        this.totalItemsCount = builder.totalItemsCount;
        this.estimatedRepairCost = builder.estimatedRepairCost;
        this.repairNotes = builder.repairNotes;
        this.submittedAt = builder.submittedAt;
    }

    public void submit() {
        if (this.status == DamageCountStatus.DRAFT) {
            this.status = DamageCountStatus.SUBMITTED;
            this.submittedAt = LocalDateTime.now();
            calculateTotalItems();
        }
    }

    public void review(UUID reviewerId) {
        if (this.status == DamageCountStatus.SUBMITTED) {
            this.status = DamageCountStatus.REVIEWED;
            this.reviewedAt = LocalDateTime.now();
            this.reviewedBy = reviewerId;
        }
    }

    public void complete() {
        if (this.status == DamageCountStatus.REVIEWED) {
            this.status = DamageCountStatus.COMPLETED;
        }
    }

    public void calculateTotalItems() {
        if (itemCounts != null) {
            this.totalItemsCount = itemCounts.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
        }
    }

    public void addItemCount(String itemKey, Integer count) {
        if (itemCounts == null) {
            itemCounts = new HashMap<>();
        }
        itemCounts.put(itemKey, count);
        calculateTotalItems();
    }

    public void addSectionPhotos(String section, List<String> photoUrls) {
        if (sectionPhotos == null) {
            sectionPhotos = new HashMap<>();
        }
        sectionPhotos.put(section, photoUrls);
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

    public DamageCountStatus getStatus() {
        return status;
    }

    public void setStatus(DamageCountStatus status) {
        this.status = status;
    }

    public PriorityLevel getPriority() {
        return priority;
    }

    public void setPriority(PriorityLevel priority) {
        this.priority = priority;
    }

    public Map<String, Integer> getItemCounts() {
        return itemCounts;
    }

    public void setItemCounts(Map<String, Integer> itemCounts) {
        this.itemCounts = itemCounts;
        calculateTotalItems();
    }

    public Map<String, List<String>> getSectionPhotos() {
        return sectionPhotos;
    }

    public void setSectionPhotos(Map<String, List<String>> sectionPhotos) {
        this.sectionPhotos = sectionPhotos;
    }

    public Integer getTotalItemsCount() {
        return totalItemsCount;
    }

    public void setTotalItemsCount(Integer totalItemsCount) {
        this.totalItemsCount = totalItemsCount;
    }

    public BigDecimal getEstimatedRepairCost() {
        return estimatedRepairCost;
    }

    public void setEstimatedRepairCost(BigDecimal estimatedRepairCost) {
        this.estimatedRepairCost = estimatedRepairCost;
    }

    public String getRepairNotes() {
        return repairNotes;
    }

    public void setRepairNotes(String repairNotes) {
        this.repairNotes = repairNotes;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(UUID reviewedBy) {
        this.reviewedBy = reviewedBy;
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
        private DamageCountStatus status = DamageCountStatus.DRAFT;
        private PriorityLevel priority;
        private Map<String, Integer> itemCounts = new HashMap<>();
        private Map<String, List<String>> sectionPhotos = new HashMap<>();
        private Integer totalItemsCount = 0;
        private BigDecimal estimatedRepairCost;
        private String repairNotes;
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

        public Builder status(DamageCountStatus status) {
            this.status = status;
            return this;
        }

        public Builder priority(PriorityLevel priority) {
            this.priority = priority;
            return this;
        }

        public Builder itemCounts(Map<String, Integer> itemCounts) {
            this.itemCounts = itemCounts;
            return this;
        }

        public Builder sectionPhotos(Map<String, List<String>> sectionPhotos) {
            this.sectionPhotos = sectionPhotos;
            return this;
        }

        public Builder totalItemsCount(Integer totalItemsCount) {
            this.totalItemsCount = totalItemsCount;
            return this;
        }

        public Builder estimatedRepairCost(BigDecimal estimatedRepairCost) {
            this.estimatedRepairCost = estimatedRepairCost;
            return this;
        }

        public Builder repairNotes(String repairNotes) {
            this.repairNotes = repairNotes;
            return this;
        }

        public Builder submittedAt(LocalDateTime submittedAt) {
            this.submittedAt = submittedAt;
            return this;
        }

        public DamageCount build() {
            return new DamageCount(this);
        }
    }

    public enum DamageCountStatus {
        DRAFT,
        SUBMITTED,
        REVIEWED,
        COMPLETED
    }

    public enum PriorityLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL,
        URGENT
    }
}