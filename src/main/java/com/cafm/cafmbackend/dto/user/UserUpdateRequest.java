package com.cafm.cafmbackend.dto.user;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * User update request DTO.
 * All fields are optional - only provided fields will be updated.
 */
public record UserUpdateRequest(
    // Basic Information
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    String firstName,
    
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    String lastName,
    
    @Size(max = 100, message = "Arabic first name cannot exceed 100 characters")
    String firstNameAr,
    
    @Size(max = 100, message = "Arabic last name cannot exceed 100 characters")
    String lastNameAr,
    
    // Contact Information
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid")
    String phoneNumber,
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Mobile number must be valid")
    String mobileNumber,
    
    // Personal Information
    LocalDate dateOfBirth,
    
    @Size(max = 20, message = "Gender cannot exceed 20 characters")
    String gender,
    
    @Size(max = 50, message = "National ID cannot exceed 50 characters")
    String nationalId,
    
    // Employment Information
    @Size(max = 50, message = "Employee ID cannot exceed 50 characters")
    String employeeId,
    
    @Size(max = 100, message = "Department cannot exceed 100 characters")
    String department,
    
    @Size(max = 100, message = "Job title cannot exceed 100 characters")
    String jobTitle,
    
    LocalDate hireDate,
    
    // Specialization for technicians
    @Size(max = 100, message = "Specialization cannot exceed 100 characters")
    String specialization,
    
    @Min(value = 0, message = "Hourly rate cannot be negative")
    Double hourlyRate,
    
    // Access Control (admin only)
    Set<String> roles,
    Set<String> permissions,
    
    // Schools assignment for supervisors
    Set<UUID> schoolIds,
    
    // Preferences
    @Size(max = 10, message = "Language code cannot exceed 10 characters")
    String preferredLanguage,
    
    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    String timezone,
    
    // Flags (admin only)
    Boolean isActive,
    Boolean emailVerified,
    Boolean twoFactorEnabled,
    Boolean mustChangePassword,
    
    // Avatar
    String avatarUrl,
    
    // Notification preferences
    Boolean emailNotifications,
    Boolean smsNotifications,
    Boolean pushNotifications
) {
    /**
     * Create update request for basic info only
     */
    public static UserUpdateRequest updateBasicInfo(
        String firstName,
        String lastName,
        String phoneNumber,
        String mobileNumber
    ) {
        return new UserUpdateRequest(
            firstName, lastName, null, null,
            phoneNumber, mobileNumber,
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null
        );
    }
    
    /**
     * Create update request for preferences
     */
    public static UserUpdateRequest updatePreferences(
        String preferredLanguage,
        String timezone,
        Boolean emailNotifications,
        Boolean smsNotifications,
        Boolean pushNotifications
    ) {
        return new UserUpdateRequest(
            null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null,
            null, null, null,
            preferredLanguage, timezone,
            null, null, null, null,
            null,
            emailNotifications, smsNotifications, pushNotifications
        );
    }
    
    /**
     * Create update request for employment info
     */
    public static UserUpdateRequest updateEmploymentInfo(
        String department,
        String jobTitle,
        String specialization,
        Double hourlyRate
    ) {
        return new UserUpdateRequest(
            null, null, null, null, null, null,
            null, null, null, null,
            department, jobTitle, null, specialization, hourlyRate,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null
        );
    }
}