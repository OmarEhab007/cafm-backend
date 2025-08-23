package com.cafm.cafmbackend.dto.user;

import com.cafm.cafmbackend.data.enums.UserType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * User creation request DTO.
 */
public record UserCreateRequest(
    // Basic Information
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
        message = "Password must contain at least one digit, one lowercase, one uppercase, one special character, and no whitespace"
    )
    String password,
    
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    String firstName,
    
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    String lastName,
    
    @Size(max = 100, message = "Arabic first name cannot exceed 100 characters")
    String firstNameAr,
    
    @Size(max = 100, message = "Arabic last name cannot exceed 100 characters")
    String lastNameAr,
    
    // User Type and Company
    @NotNull(message = "User type is required")
    UserType userType,
    
    @NotNull(message = "Company ID is required")
    UUID companyId,
    
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
    
    // Access Control
    Set<String> roles,
    Set<String> permissions,
    
    // Schools assignment for supervisors
    Set<UUID> schoolIds,
    
    // Preferences
    @Size(max = 10, message = "Language code cannot exceed 10 characters")
    String preferredLanguage,
    
    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    String timezone,
    
    // Flags
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
     * Constructor with required fields only
     */
    public UserCreateRequest(
        String email,
        String password,
        String firstName,
        String lastName,
        UserType userType,
        UUID companyId
    ) {
        this(
            email, password, firstName, lastName, null, null,
            userType, companyId,
            null, null, null, null, null,
            null, null, null, null, null, null,
            null, null, null,
            "en", "UTC",
            true, false, false, false,
            null,
            true, false, true
        );
    }
    
    /**
     * Factory method for creating admin user
     */
    public static UserCreateRequest createAdmin(
        String email,
        String password,
        String firstName,
        String lastName,
        UUID companyId
    ) {
        return new UserCreateRequest(
            email, password, firstName, lastName,
            UserType.ADMIN, companyId
        );
    }
    
    /**
     * Factory method for creating supervisor user
     */
    public static UserCreateRequest createSupervisor(
        String email,
        String password,
        String firstName,
        String lastName,
        UUID companyId,
        Set<UUID> schoolIds
    ) {
        return new UserCreateRequest(
            email, password, firstName, lastName, null, null,
            UserType.SUPERVISOR, companyId,
            null, null, null, null, null,
            null, null, null, null, null, null,
            null, null, schoolIds,
            "en", "UTC",
            true, false, false, false,
            null,
            true, false, true
        );
    }
    
    /**
     * Factory method for creating technician user
     */
    public static UserCreateRequest createTechnician(
        String email,
        String password,
        String firstName,
        String lastName,
        UUID companyId,
        String specialization,
        Double hourlyRate
    ) {
        return new UserCreateRequest(
            email, password, firstName, lastName, null, null,
            UserType.TECHNICIAN, companyId,
            null, null, null, null, null,
            null, null, null, null, specialization, hourlyRate,
            null, null, null,
            "en", "UTC",
            true, false, false, false,
            null,
            true, false, true
        );
    }
}