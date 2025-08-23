package com.cafm.cafmbackend.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * User profile update request for users updating their own profile.
 */
public record UserProfileUpdateRequest(
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    String firstName,
    
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    String lastName,
    
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number")
    String phone,
    
    String firstNameAr,
    String lastNameAr,
    
    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    String bio,
    
    String language,
    String timezone,
    String avatarUrl
) {}