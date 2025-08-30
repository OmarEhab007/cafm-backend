package com.cafm.cafmbackend.dto.achievement;

import com.cafm.cafmbackend.infrastructure.persistence.entity.SchoolAchievement.AchievementType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for school achievement data.
 */
public record SchoolAchievementResponse(
    UUID id,
    
    UUID schoolId,
    String schoolName,
    
    UUID companyId,
    String companyName,
    
    AchievementType achievementType,
    
    String title,
    String description,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate achievementDate,
    
    List<String> achievementPhotoUrls,
    
    String achieverName,
    String category,
    String location,
    
    Integer participantCount,
    
    String notes,
    List<String> tags,
    
    Boolean isPublic,
    Boolean approved,
    
    UUID approvedBy,
    String approvedByName,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime approvedAt,
    
    String approvalNotes,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt,
    
    String createdBy,
    String updatedBy,
    
    AchievementMetrics metrics
) {
    /**
     * Metrics and insights for achievements.
     */
    public record AchievementMetrics(
        Integer totalPhotos,
        Integer daysSinceAchievement,
        String status,
        Boolean featured,
        Integer viewCount,
        List<String> relatedAchievements
    ) {
        public static AchievementMetrics calculate(
                SchoolAchievementResponse response) {
            
            int totalPhotos = response.achievementPhotoUrls() != null 
                ? response.achievementPhotoUrls().size() 
                : 0;
            
            int daysSinceAchievement = (int) java.time.temporal.ChronoUnit.DAYS.between(
                response.achievementDate(),
                LocalDate.now()
            );
            
            String status = determineStatus(response.approved(), response.isPublic());
            
            boolean featured = response.participantCount() != null && 
                             response.participantCount() > 50 &&
                             response.approved();
            
            return new AchievementMetrics(
                totalPhotos,
                daysSinceAchievement,
                status,
                featured,
                0, // View count would be tracked separately
                List.of() // Related achievements would be fetched separately
            );
        }
        
        private static String determineStatus(Boolean approved, Boolean isPublic) {
            if (!approved) return "PENDING_APPROVAL";
            if (!isPublic) return "PRIVATE";
            return "PUBLIC";
        }
    }
}