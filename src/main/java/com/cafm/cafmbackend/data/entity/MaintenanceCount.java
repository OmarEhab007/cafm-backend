package com.cafm.cafmbackend.data.entity;

import com.cafm.cafmbackend.data.entity.base.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing maintenance count items for a school.
 * Contains complex maintenance inspection data with multiple JSONB fields for various data types.
 */
@Entity
@Table(name = "maintenance_counts")
public class MaintenanceCount extends BaseEntity {

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
    private MaintenanceCountStatus status = MaintenanceCountStatus.DRAFT;

    @Type(JsonType.class)
    @Column(name = "item_counts", columnDefinition = "jsonb")
    private Map<String, Integer> itemCounts = new HashMap<>();

    @Type(JsonType.class)
    @Column(name = "text_answers", columnDefinition = "jsonb")
    private Map<String, String> textAnswers = new HashMap<>();

    @Type(JsonType.class)
    @Column(name = "yes_no_answers", columnDefinition = "jsonb")
    private Map<String, Boolean> yesNoAnswers = new HashMap<>();

    @Type(JsonType.class)
    @Column(name = "yes_no_with_counts", columnDefinition = "jsonb")
    private Map<String, Integer> yesNoWithCounts = new HashMap<>();

    @Type(JsonType.class)
    @Column(name = "survey_answers", columnDefinition = "jsonb")
    private Map<String, String> surveyAnswers = new HashMap<>();

    @Type(JsonType.class)
    @Column(name = "maintenance_notes", columnDefinition = "jsonb")
    private Map<String, String> maintenanceNotes = new HashMap<>();

    @Type(JsonType.class)
    @Column(name = "fire_safety_alarm_panel_data", columnDefinition = "jsonb")
    private Map<String, String> fireSafetyAlarmPanelData = new HashMap<>();

    @Type(JsonType.class)
    @Column(name = "fire_safety_condition_only_data", columnDefinition = "jsonb")
    private Map<String, String> fireSafetyConditionOnlyData = new HashMap<>();

    @Type(JsonType.class)
    @Column(name = "fire_safety_expiry_dates", columnDefinition = "jsonb")
    private Map<String, String> fireSafetyExpiryDates = new HashMap<>();

    @Type(JsonType.class)
    @Column(name = "section_photos", columnDefinition = "jsonb")
    private Map<String, List<String>> sectionPhotos = new HashMap<>();

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public MaintenanceCount() {
        super();
    }

    private MaintenanceCount(Builder builder) {
        this.school = builder.school;
        this.supervisor = builder.supervisor;
        this.schoolName = builder.schoolName;
        this.status = builder.status;
        this.itemCounts = builder.itemCounts;
        this.textAnswers = builder.textAnswers;
        this.yesNoAnswers = builder.yesNoAnswers;
        this.yesNoWithCounts = builder.yesNoWithCounts;
        this.surveyAnswers = builder.surveyAnswers;
        this.maintenanceNotes = builder.maintenanceNotes;
        this.fireSafetyAlarmPanelData = builder.fireSafetyAlarmPanelData;
        this.fireSafetyConditionOnlyData = builder.fireSafetyConditionOnlyData;
        this.fireSafetyExpiryDates = builder.fireSafetyExpiryDates;
        this.sectionPhotos = builder.sectionPhotos;
        this.submittedAt = builder.submittedAt;
        this.company = builder.company;
    }

    // Commented out temporarily to avoid lazy loading issues
    // @PrePersist
    // @PreUpdate
    // private void updateSchoolName() {
    //     if (school != null && schoolName == null) {
    //         schoolName = school.getName();
    //     }
    // }

    public void submit() {
        if (this.status == MaintenanceCountStatus.DRAFT) {
            this.status = MaintenanceCountStatus.SUBMITTED;
            this.submittedAt = LocalDateTime.now();
        }
    }

    public void review() {
        if (this.status == MaintenanceCountStatus.SUBMITTED) {
            this.status = MaintenanceCountStatus.REVIEWED;
        }
    }

    public void complete() {
        if (this.status == MaintenanceCountStatus.REVIEWED) {
            this.status = MaintenanceCountStatus.COMPLETED;
        }
    }

    public void addItemCount(String itemKey, Integer count) {
        if (itemCounts == null) {
            itemCounts = new HashMap<>();
        }
        itemCounts.put(itemKey, count);
    }

    public void addTextAnswer(String questionKey, String answer) {
        if (textAnswers == null) {
            textAnswers = new HashMap<>();
        }
        textAnswers.put(questionKey, answer);
    }

    public void addYesNoAnswer(String questionKey, Boolean answer) {
        if (yesNoAnswers == null) {
            yesNoAnswers = new HashMap<>();
        }
        yesNoAnswers.put(questionKey, answer);
    }

    public void addSurveyAnswer(String questionKey, String answer) {
        if (surveyAnswers == null) {
            surveyAnswers = new HashMap<>();
        }
        surveyAnswers.put(questionKey, answer);
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

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public MaintenanceCountStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceCountStatus status) {
        this.status = status;
    }

    public Map<String, Integer> getItemCounts() {
        return itemCounts;
    }

    public void setItemCounts(Map<String, Integer> itemCounts) {
        this.itemCounts = itemCounts;
    }

    public Map<String, String> getTextAnswers() {
        return textAnswers;
    }

    public void setTextAnswers(Map<String, String> textAnswers) {
        this.textAnswers = textAnswers;
    }

    public Map<String, Boolean> getYesNoAnswers() {
        return yesNoAnswers;
    }

    public void setYesNoAnswers(Map<String, Boolean> yesNoAnswers) {
        this.yesNoAnswers = yesNoAnswers;
    }

    public Map<String, Integer> getYesNoWithCounts() {
        return yesNoWithCounts;
    }

    public void setYesNoWithCounts(Map<String, Integer> yesNoWithCounts) {
        this.yesNoWithCounts = yesNoWithCounts;
    }

    public Map<String, String> getSurveyAnswers() {
        return surveyAnswers;
    }

    public void setSurveyAnswers(Map<String, String> surveyAnswers) {
        this.surveyAnswers = surveyAnswers;
    }

    public Map<String, String> getMaintenanceNotes() {
        return maintenanceNotes;
    }

    public void setMaintenanceNotes(Map<String, String> maintenanceNotes) {
        this.maintenanceNotes = maintenanceNotes;
    }

    public Map<String, String> getFireSafetyAlarmPanelData() {
        return fireSafetyAlarmPanelData;
    }

    public void setFireSafetyAlarmPanelData(Map<String, String> fireSafetyAlarmPanelData) {
        this.fireSafetyAlarmPanelData = fireSafetyAlarmPanelData;
    }

    public Map<String, String> getFireSafetyConditionOnlyData() {
        return fireSafetyConditionOnlyData;
    }

    public void setFireSafetyConditionOnlyData(Map<String, String> fireSafetyConditionOnlyData) {
        this.fireSafetyConditionOnlyData = fireSafetyConditionOnlyData;
    }

    public Map<String, String> getFireSafetyExpiryDates() {
        return fireSafetyExpiryDates;
    }

    public void setFireSafetyExpiryDates(Map<String, String> fireSafetyExpiryDates) {
        this.fireSafetyExpiryDates = fireSafetyExpiryDates;
    }

    public Map<String, List<String>> getSectionPhotos() {
        return sectionPhotos;
    }

    public void setSectionPhotos(Map<String, List<String>> sectionPhotos) {
        this.sectionPhotos = sectionPhotos;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
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
        private String schoolName;
        private MaintenanceCountStatus status = MaintenanceCountStatus.DRAFT;
        private Map<String, Integer> itemCounts = new HashMap<>();
        private Map<String, String> textAnswers = new HashMap<>();
        private Map<String, Boolean> yesNoAnswers = new HashMap<>();
        private Map<String, Integer> yesNoWithCounts = new HashMap<>();
        private Map<String, String> surveyAnswers = new HashMap<>();
        private Map<String, String> maintenanceNotes = new HashMap<>();
        private Map<String, String> fireSafetyAlarmPanelData = new HashMap<>();
        private Map<String, String> fireSafetyConditionOnlyData = new HashMap<>();
        private Map<String, String> fireSafetyExpiryDates = new HashMap<>();
        private Map<String, List<String>> sectionPhotos = new HashMap<>();
        private LocalDateTime submittedAt;
        private Company company;

        public Builder school(School school) {
            this.school = school;
            return this;
        }

        public Builder supervisor(User supervisor) {
            this.supervisor = supervisor;
            return this;
        }

        public Builder schoolName(String schoolName) {
            this.schoolName = schoolName;
            return this;
        }

        public Builder status(MaintenanceCountStatus status) {
            this.status = status;
            return this;
        }

        public Builder itemCounts(Map<String, Integer> itemCounts) {
            this.itemCounts = itemCounts;
            return this;
        }

        public Builder textAnswers(Map<String, String> textAnswers) {
            this.textAnswers = textAnswers;
            return this;
        }

        public Builder yesNoAnswers(Map<String, Boolean> yesNoAnswers) {
            this.yesNoAnswers = yesNoAnswers;
            return this;
        }

        public Builder yesNoWithCounts(Map<String, Integer> yesNoWithCounts) {
            this.yesNoWithCounts = yesNoWithCounts;
            return this;
        }

        public Builder surveyAnswers(Map<String, String> surveyAnswers) {
            this.surveyAnswers = surveyAnswers;
            return this;
        }

        public Builder maintenanceNotes(Map<String, String> maintenanceNotes) {
            this.maintenanceNotes = maintenanceNotes;
            return this;
        }

        public Builder fireSafetyAlarmPanelData(Map<String, String> fireSafetyAlarmPanelData) {
            this.fireSafetyAlarmPanelData = fireSafetyAlarmPanelData;
            return this;
        }

        public Builder fireSafetyConditionOnlyData(Map<String, String> fireSafetyConditionOnlyData) {
            this.fireSafetyConditionOnlyData = fireSafetyConditionOnlyData;
            return this;
        }

        public Builder fireSafetyExpiryDates(Map<String, String> fireSafetyExpiryDates) {
            this.fireSafetyExpiryDates = fireSafetyExpiryDates;
            return this;
        }

        public Builder sectionPhotos(Map<String, List<String>> sectionPhotos) {
            this.sectionPhotos = sectionPhotos;
            return this;
        }

        public Builder submittedAt(LocalDateTime submittedAt) {
            this.submittedAt = submittedAt;
            return this;
        }

        public Builder company(Company company) {
            this.company = company;
            return this;
        }

        public MaintenanceCount build() {
            return new MaintenanceCount(this);
        }
    }

    public enum MaintenanceCountStatus {
        DRAFT,
        SUBMITTED,
        REVIEWED,
        COMPLETED
    }
}