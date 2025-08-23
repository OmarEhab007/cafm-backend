package com.cafm.cafmbackend.mappers.user;

import com.cafm.cafmbackend.mappers.config.BaseMapper;
import com.cafm.cafmbackend.data.entity.Role;
import com.cafm.cafmbackend.data.entity.SupervisorSchool;
import com.cafm.cafmbackend.data.entity.User;
import com.cafm.cafmbackend.data.enums.TechnicianSpecialization;
import com.cafm.cafmbackend.data.enums.UserStatus;
import com.cafm.cafmbackend.dto.user.UserCreateRequest;
import com.cafm.cafmbackend.dto.user.UserResponse;
import com.cafm.cafmbackend.dto.user.UserUpdateRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper implementation for User entity and DTOs.
 * Note: Many DTO fields are not present in the User entity.
 */
@Component
public class UserMapperImpl implements BaseMapper<User, UserCreateRequest, UserResponse> {
    
    @Override
    public User toEntity(UserCreateRequest request) {
        if (request == null) return null;
        
        User user = new User();
        
        // Basic information
        user.setEmail(request.email());
        user.setUsername(request.email()); // Using email as username if not provided
        user.setPasswordHash(request.password()); // Will be hashed by service
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phoneNumber());
        user.setEmployeeId(request.employeeId());
        
        // User type and status
        user.setUserType(request.userType());
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        
        // Set defaults
        user.setEmailVerified(false);
        user.setIsActive(true);
        user.setIsLocked(false);
        
        // Technician-specific fields
        if (request.specialization() != null) {
            // Convert string to enum if possible
            try {
                user.setSpecialization(TechnicianSpecialization.valueOf(request.specialization().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Handle invalid specialization - log and skip
            }
        }
        if (request.hourlyRate() != null) {
            user.setHourlyRate(request.hourlyRate());
        }
        
        // Employment information
        if (request.department() != null) {
            user.setDepartment(request.department());
        }
        if (request.jobTitle() != null) {
            user.setPosition(request.jobTitle());
        }
        
        return user;
    }
    
    @Override
    public UserResponse toResponse(User entity) {
        if (entity == null) return null;
        
        // Calculate age and years of service
        Integer age = null;
        Integer yearsOfService = null;
        LocalDate hireDate = null; // Not in entity
        
        // Note: Many response fields are not available in the User entity
        return new UserResponse(
            // Identifiers
            entity.getId(),
            entity.getEmail(),
            entity.getUserType(),
            
            // Basic Information
            entity.getFirstName(),
            entity.getLastName(),
            null, // firstNameAr - not in entity
            null, // lastNameAr - not in entity
            entity.getFullName(),
            null, // fullNameAr - not in entity
            
            // Company Information
            entity.getCompany() != null ? entity.getCompany().getId() : null,
            entity.getCompany() != null ? entity.getCompany().getName() : null,
            
            // Contact Information
            entity.getPhone(),
            null, // mobileNumber - not in entity
            
            // Personal Information
            null, // dateOfBirth - not in entity
            null, // gender - not in entity
            null, // nationalId - not in entity
            age,
            
            // Employment Information
            entity.getEmployeeId(),
            entity.getDepartment(),
            entity.getPosition(), // Using position as jobTitle
            hireDate,
            yearsOfService,
            
            // Specialization (for technicians)
            entity.getSpecialization() != null ? entity.getSpecialization().toString() : null,
            entity.getHourlyRate(),
            
            // Access Control
            rolesToStringSet(entity.getRoles()),
            new HashSet<>(), // permissions - not in entity
            
            // Schools (for supervisors)
            supervisorSchoolsToSchoolInfoSet(entity.getSupervisorSchools()),
            
            // Status Information
            entity.getIsActive(),
            false, // isOnline - would need session tracking
            entity.getEmailVerified(),
            false, // twoFactorEnabled - not in entity
            false, // mustChangePassword - not in entity
            entity.getIsAvailableForAssignment(),
            
            // Timestamps
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getLastLoginAt(),
            null, // emailVerifiedAt - not in entity
            
            // Preferences
            "en", // preferredLanguage - not in entity, using default
            "UTC", // timezone - not in entity, using default
            "light", // theme - not in entity, using default
            
            // Avatar
            entity.getAvatarUrl(),
            
            // Notification preferences - not in entity
            true, // emailNotifications - default
            false, // smsNotifications - default
            true, // pushNotifications - default
            
            // Statistics
            calculateUserStatistics(entity)
        );
    }
    
    /**
     * Update entity from update request.
     */
    public void updateEntity(UserUpdateRequest request, User entity) {
        if (request == null || entity == null) return;
        
        // Basic information
        if (request.firstName() != null) {
            entity.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            entity.setLastName(request.lastName());
        }
        
        // Contact information
        if (request.phoneNumber() != null) {
            entity.setPhone(request.phoneNumber());
        }
        
        // Employment information
        if (request.employeeId() != null) {
            entity.setEmployeeId(request.employeeId());
        }
        if (request.department() != null) {
            entity.setDepartment(request.department());
        }
        if (request.jobTitle() != null) {
            entity.setPosition(request.jobTitle());
        }
        
        // Specialization for technicians
        if (request.specialization() != null) {
            try {
                entity.setSpecialization(TechnicianSpecialization.valueOf(request.specialization().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Handle invalid specialization
            }
        }
        if (request.hourlyRate() != null) {
            entity.setHourlyRate(request.hourlyRate());
        }
        
        // Flags (admin only)
        if (request.isActive() != null) {
            entity.setIsActive(request.isActive());
        }
        if (request.emailVerified() != null) {
            entity.setEmailVerified(request.emailVerified());
        }
        
        // Avatar
        if (request.avatarUrl() != null) {
            entity.setAvatarUrl(request.avatarUrl());
        }
        
        // Note: Many UserUpdateRequest fields don't exist in User entity
        // (firstNameAr, lastNameAr, mobileNumber, dateOfBirth, gender, nationalId,
        // hireDate, preferredLanguage, timezone, twoFactorEnabled, mustChangePassword,
        // emailNotifications, smsNotifications, pushNotifications)
        // These would need to be stored in a separate preferences table or JSONB column
    }
    
    private Set<String> rolesToStringSet(Set<Role> roles) {
        if (roles == null) return new HashSet<>();
        return roles.stream()
            .map(Role::getName)
            .collect(Collectors.toSet());
    }
    
    private Set<UserResponse.SchoolInfo> supervisorSchoolsToSchoolInfoSet(Set<SupervisorSchool> supervisorSchools) {
        if (supervisorSchools == null) return new HashSet<>();
        return supervisorSchools.stream()
            .filter(ss -> ss.getSchool() != null)
            .map(ss -> new UserResponse.SchoolInfo(
                ss.getSchool().getId(),
                ss.getSchool().getCode(),
                ss.getSchool().getName(),
                ss.getSchool().getNameAr(),
                ss.getSchool().getCity(), // Using city as district
                ss.getSchool().getIsActive()
            ))
            .collect(Collectors.toSet());
    }
    
    private UserResponse.UserStatistics calculateUserStatistics(User entity) {
        // Basic statistics - would need actual data from related tables
        return new UserResponse.UserStatistics(
            // For technicians
            0, // activeWorkOrders
            0, // completedWorkOrders
            0.0, // averageCompletionTime
            0.0, // completionRate
            
            // For supervisors
            entity.getSupervisorSchools() != null ? entity.getSupervisorSchools().size() : 0, // assignedSchools
            0, // activeReports
            0, // pendingReports
            
            // General
            0, // totalActivities
            entity.getLastLoginAt() // lastActivityAt
        );
    }
}