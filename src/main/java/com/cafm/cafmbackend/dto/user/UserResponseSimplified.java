package com.cafm.cafmbackend.dto.user;

import com.cafm.cafmbackend.data.enums.UserType;
import com.cafm.cafmbackend.data.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Simplified User response DTO that matches actual User entity fields.
 * This replaces the overly complex UserResponse with realistic data.
 */
public record UserResponseSimplified(
    // Core identifiers
    UUID id,
    String email,
    String username,
    UserType userType,
    UserStatus status,
    
    // Basic Information
    String firstName,
    String lastName,
    String fullName,
    String phone,
    
    // Employee Information
    String employeeId,
    String iqamaId,
    String plateNumber,
    
    // Company Information
    UUID companyId,
    String companyName,
    
    // Department and Position
    String department,
    String position,
    
    // Verification Status
    Boolean emailVerified,
    Boolean phoneVerified,
    Boolean isActive,
    Boolean isLocked,
    
    // Timestamps
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime lastLoginAt,
    
    // Roles (simple string set for now)
    Set<String> roles
) {
    /**
     * Create a basic response for list views
     */
    public static UserResponseSimplified forList(
        UUID id,
        String email,
        String username,
        UserType userType,
        String firstName,
        String lastName,
        Boolean isActive
    ) {
        String fullName = (firstName != null && lastName != null) 
            ? firstName + " " + lastName 
            : username;
            
        return new UserResponseSimplified(
            id, email, username, userType, UserStatus.ACTIVE,
            firstName, lastName, fullName, null,
            null, null, null,
            null, null,
            null, null,
            false, false, isActive, false,
            null, null, null,
            Set.of()
        );
    }
    
    /**
     * Builder for creating responses from entities
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private UUID id;
        private String email;
        private String username;
        private UserType userType;
        private UserStatus status;
        private String firstName;
        private String lastName;
        private String phone;
        private String employeeId;
        private String iqamaId;
        private String plateNumber;
        private UUID companyId;
        private String companyName;
        private String department;
        private String position;
        private Boolean emailVerified;
        private Boolean phoneVerified;
        private Boolean isActive;
        private Boolean isLocked;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime lastLoginAt;
        private Set<String> roles = Set.of();
        
        public Builder id(UUID id) {
            this.id = id;
            return this;
        }
        
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        
        public Builder username(String username) {
            this.username = username;
            return this;
        }
        
        public Builder userType(UserType userType) {
            this.userType = userType;
            return this;
        }
        
        public Builder status(UserStatus status) {
            this.status = status;
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
        
        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }
        
        public Builder employeeId(String employeeId) {
            this.employeeId = employeeId;
            return this;
        }
        
        public Builder iqamaId(String iqamaId) {
            this.iqamaId = iqamaId;
            return this;
        }
        
        public Builder plateNumber(String plateNumber) {
            this.plateNumber = plateNumber;
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
        
        public Builder department(String department) {
            this.department = department;
            return this;
        }
        
        public Builder position(String position) {
            this.position = position;
            return this;
        }
        
        public Builder emailVerified(Boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }
        
        public Builder phoneVerified(Boolean phoneVerified) {
            this.phoneVerified = phoneVerified;
            return this;
        }
        
        public Builder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }
        
        public Builder isLocked(Boolean isLocked) {
            this.isLocked = isLocked;
            return this;
        }
        
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public Builder lastLoginAt(LocalDateTime lastLoginAt) {
            this.lastLoginAt = lastLoginAt;
            return this;
        }
        
        public Builder roles(Set<String> roles) {
            this.roles = roles;
            return this;
        }
        
        public UserResponseSimplified build() {
            String fullName = (firstName != null && lastName != null) 
                ? firstName + " " + lastName 
                : username;
                
            return new UserResponseSimplified(
                id, email, username, userType, status,
                firstName, lastName, fullName, phone,
                employeeId, iqamaId, plateNumber,
                companyId, companyName,
                department, position,
                emailVerified, phoneVerified, isActive, isLocked,
                createdAt, updatedAt, lastLoginAt,
                roles
            );
        }
    }
}