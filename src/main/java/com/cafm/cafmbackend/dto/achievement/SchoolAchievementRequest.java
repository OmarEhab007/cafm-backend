package com.cafm.cafmbackend.dto.achievement;

import com.cafm.cafmbackend.infrastructure.persistence.entity.SchoolAchievement.AchievementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating or updating school achievements.
 */
public record SchoolAchievementRequest(
    @NotNull(message = "School ID is required")
    UUID schoolId,
    
    @NotNull(message = "Company ID is required")
    UUID companyId,
    
    @NotNull(message = "Achievement type is required")
    AchievementType achievementType,
    
    @NotNull(message = "Title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    String title,
    
    @NotNull(message = "Description is required")
    @Size(min = 10, max = 5000, message = "Description must be between 10 and 5000 characters")
    String description,
    
    @NotNull(message = "Achievement date is required")
    LocalDate achievementDate,
    
    @NotNull(message = "At least one photo is required")
    @Size(min = 1, max = 10, message = "Must provide between 1 and 10 photos")
    List<String> achievementPhotoUrls,
    
    String achieverName,
    
    String category,
    
    String location,
    
    Integer participantCount,
    
    String notes,
    
    List<String> tags,
    
    Boolean isPublic
) {
    /**
     * Creates a basic achievement request.
     */
    public static SchoolAchievementRequest basic(
            UUID schoolId,
            UUID companyId,
            AchievementType type,
            String title,
            String description,
            List<String> photos) {
        return new SchoolAchievementRequest(
            schoolId,
            companyId,
            type,
            title,
            description,
            LocalDate.now(),
            photos,
            null,
            null,
            null,
            null,
            null,
            List.of(),
            true
        );
    }
    
    /**
     * Builder for complex achievement requests.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private UUID schoolId;
        private UUID companyId;
        private AchievementType achievementType;
        private String title;
        private String description;
        private LocalDate achievementDate = LocalDate.now();
        private List<String> achievementPhotoUrls = List.of();
        private String achieverName;
        private String category;
        private String location;
        private Integer participantCount;
        private String notes;
        private List<String> tags = List.of();
        private Boolean isPublic = true;
        
        public Builder schoolId(UUID schoolId) {
            this.schoolId = schoolId;
            return this;
        }
        
        public Builder companyId(UUID companyId) {
            this.companyId = companyId;
            return this;
        }
        
        public Builder achievementType(AchievementType achievementType) {
            this.achievementType = achievementType;
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
        
        public Builder achievementDate(LocalDate achievementDate) {
            this.achievementDate = achievementDate;
            return this;
        }
        
        public Builder achievementPhotoUrls(List<String> achievementPhotoUrls) {
            this.achievementPhotoUrls = achievementPhotoUrls;
            return this;
        }
        
        public Builder achieverName(String achieverName) {
            this.achieverName = achieverName;
            return this;
        }
        
        public Builder category(String category) {
            this.category = category;
            return this;
        }
        
        public Builder location(String location) {
            this.location = location;
            return this;
        }
        
        public Builder participantCount(Integer participantCount) {
            this.participantCount = participantCount;
            return this;
        }
        
        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }
        
        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }
        
        public Builder isPublic(Boolean isPublic) {
            this.isPublic = isPublic;
            return this;
        }
        
        public SchoolAchievementRequest build() {
            return new SchoolAchievementRequest(
                schoolId,
                companyId,
                achievementType,
                title,
                description,
                achievementDate,
                achievementPhotoUrls,
                achieverName,
                category,
                location,
                participantCount,
                notes,
                tags,
                isPublic
            );
        }
    }
}