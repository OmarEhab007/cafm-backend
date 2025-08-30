package com.cafm.cafmbackend.dto.user;

import com.cafm.cafmbackend.shared.enums.UserType;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * User response DTO for API responses.
 */
public record UserResponse(
    // Identifiers
    UUID id,
    String email,
    UserType userType,
    
    // Basic Information
    String firstName,
    String lastName,
    String firstNameAr,
    String lastNameAr,
    String fullName,
    String fullNameAr,
    
    // Company Information
    UUID companyId,
    String companyName,
    
    // Contact Information
    String phoneNumber,
    String mobileNumber,
    
    // Personal Information
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate dateOfBirth,
    String gender,
    String nationalId,
    Integer age,
    
    // Employment Information
    String employeeId,
    String department,
    String jobTitle,
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate hireDate,
    Integer yearsOfService,
    
    // Specialization (for technicians)
    String specialization,
    Double hourlyRate,
    
    // Access Control
    Set<String> roles,
    Set<String> permissions,
    
    // Schools (for supervisors)
    Set<SchoolInfo> schools,
    
    // Status Information
    Boolean isActive,
    Boolean isOnline,
    Boolean emailVerified,
    Boolean twoFactorEnabled,
    Boolean mustChangePassword,
    Boolean isAvailableForAssignment,
    
    // Timestamps
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime lastLoginAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime emailVerifiedAt,
    
    // Preferences
    String preferredLanguage,
    String timezone,
    String theme,
    
    // Avatar
    String avatarUrl,
    
    // Notification preferences
    Boolean emailNotifications,
    Boolean smsNotifications,
    Boolean pushNotifications,
    
    // Statistics (optional, for detailed views)
    UserStatistics statistics
) {
    /**
     * Nested record for school information
     */
    public record SchoolInfo(
        UUID id,
        String code,
        String name,
        String nameAr,
        String district,
        Boolean isActive
    ) {}
    
    /**
     * Nested record for user statistics
     */
    public record UserStatistics(
        // For technicians
        Integer activeWorkOrders,
        Integer completedWorkOrders,
        Double averageCompletionTime,
        Double completionRate,
        
        // For supervisors
        Integer assignedSchools,
        Integer activeReports,
        Integer pendingReports,
        
        // General
        Integer totalActivities,
        LocalDateTime lastActivityAt
    ) {}
    
    /**
     * Create basic response without statistics
     */
    public static UserResponse basic(
        UUID id,
        String email,
        UserType userType,
        String firstName,
        String lastName,
        UUID companyId,
        String companyName,
        Boolean isActive
    ) {
        String fullName = firstName + " " + lastName;
        return new UserResponse(
            id, email, userType,
            firstName, lastName, null, null, fullName, null,
            companyId, companyName,
            null, null, null, null, null, null,
            null, null, null, null, null,
            null, null,
            null, null, null,
            isActive, false, false, false, false, true,
            LocalDateTime.now(), LocalDateTime.now(), null, null,
            "en", "UTC", "light",
            null,
            true, false, true,
            null
        );
    }
    
    /**
     * Builder pattern for complex response
     */
    public static class Builder {
        private UUID id;
        private String email;
        private UserType userType;
        private String firstName;
        private String lastName;
        private String firstNameAr;
        private String lastNameAr;
        private UUID companyId;
        private String companyName;
        private String phoneNumber;
        private String mobileNumber;
        private LocalDate dateOfBirth;
        private String gender;
        private String nationalId;
        private String employeeId;
        private String department;
        private String jobTitle;
        private LocalDate hireDate;
        private String specialization;
        private Double hourlyRate;
        private Set<String> roles;
        private Set<String> permissions;
        private Set<SchoolInfo> schools;
        private Boolean isActive = true;
        private Boolean isOnline = false;
        private Boolean emailVerified = false;
        private Boolean twoFactorEnabled = false;
        private Boolean mustChangePassword = false;
        private Boolean isAvailableForAssignment = true;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime lastLoginAt;
        private LocalDateTime emailVerifiedAt;
        private String preferredLanguage = "en";
        private String timezone = "UTC";
        private String theme = "light";
        private String avatarUrl;
        private Boolean emailNotifications = true;
        private Boolean smsNotifications = false;
        private Boolean pushNotifications = true;
        private UserStatistics statistics;
        
        public Builder id(UUID id) {
            this.id = id;
            return this;
        }
        
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        
        public Builder userType(UserType userType) {
            this.userType = userType;
            return this;
        }
        
        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }
        
        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }
        
        public Builder companyId(UUID companyId) {
            this.companyId = companyId;
            return this;
        }
        
        public Builder companyName(String companyName) {
            this.companyName = companyName;
            return this;
        }
        
        public Builder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }
        
        // Additional builder methods for all fields...
        
        public UserResponse build() {
            String fullName = (firstName != null && lastName != null) ? 
                firstName + " " + lastName : null;
            String fullNameAr = (firstNameAr != null && lastNameAr != null) ? 
                firstNameAr + " " + lastNameAr : null;
            
            Integer age = (dateOfBirth != null) ? 
                LocalDate.now().getYear() - dateOfBirth.getYear() : null;
            
            Integer yearsOfService = (hireDate != null) ? 
                LocalDate.now().getYear() - hireDate.getYear() : null;
            
            return new UserResponse(
                id, email, userType,
                firstName, lastName, firstNameAr, lastNameAr, fullName, fullNameAr,
                companyId, companyName,
                phoneNumber, mobileNumber,
                dateOfBirth, gender, nationalId, age,
                employeeId, department, jobTitle, hireDate, yearsOfService,
                specialization, hourlyRate,
                roles, permissions, schools,
                isActive, isOnline, emailVerified, twoFactorEnabled, 
                mustChangePassword, isAvailableForAssignment,
                createdAt, updatedAt, lastLoginAt, emailVerifiedAt,
                preferredLanguage, timezone, theme,
                avatarUrl,
                emailNotifications, smsNotifications, pushNotifications,
                statistics
            );
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}