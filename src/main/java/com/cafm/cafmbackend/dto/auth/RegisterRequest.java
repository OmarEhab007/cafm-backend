package com.cafm.cafmbackend.dto.auth;

import com.cafm.cafmbackend.data.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Minimal registration request DTO for admin to create new users.
 */
public record RegisterRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    String firstName,
    
    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    String lastName,
    
    @NotNull(message = "User type is required")
    UserType userType,
    
    @NotNull(message = "Company ID is required")
    UUID companyId,
    
    String phone,
    String employeeId
) {}