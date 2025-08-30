package com.cafm.cafmbackend.dto.auth;

import com.cafm.cafmbackend.shared.enums.UserType;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Login response DTO containing authentication tokens and user information.
 */
public record LoginResponse(
    // Authentication tokens
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn, // seconds until access token expires
    
    // User information
    UUID userId,
    String email,
    String firstName,
    String lastName,
    String firstNameAr,
    String lastNameAr,
    UserType userType,
    Set<String> roles,
    Set<String> permissions,
    
    // Company information
    UUID companyId,
    String companyName,
    String companyStatus,
    String subscriptionPlan,
    
    // Session information
    String sessionId,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime loginTime,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime lastLoginTime,
    
    // User preferences
    String language,
    String timezone,
    String theme,
    
    // Feature flags
    Boolean isFirstLogin,
    Boolean mustChangePassword,
    Boolean twoFactorEnabled,
    
    // Avatar URL
    String avatarUrl
) {
    /**
     * Builder pattern for complex object creation
     */
    public static class Builder {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private Long expiresIn;
        private UUID userId;
        private String email;
        private String firstName;
        private String lastName;
        private String firstNameAr;
        private String lastNameAr;
        private UserType userType;
        private Set<String> roles;
        private Set<String> permissions;
        private UUID companyId;
        private String companyName;
        private String companyStatus;
        private String subscriptionPlan;
        private String sessionId;
        private LocalDateTime loginTime = LocalDateTime.now();
        private LocalDateTime lastLoginTime;
        private String language = "en";
        private String timezone = "UTC";
        private String theme = "light";
        private Boolean isFirstLogin = false;
        private Boolean mustChangePassword = false;
        private Boolean twoFactorEnabled = false;
        private String avatarUrl;
        
        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }
        
        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }
        
        public Builder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }
        
        public Builder expiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }
        
        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder email(String email) {
            this.email = email;
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
        
        public Builder firstNameAr(String firstNameAr) {
            this.firstNameAr = firstNameAr;
            return this;
        }
        
        public Builder lastNameAr(String lastNameAr) {
            this.lastNameAr = lastNameAr;
            return this;
        }
        
        public Builder userType(UserType userType) {
            this.userType = userType;
            return this;
        }
        
        public Builder roles(Set<String> roles) {
            this.roles = roles;
            return this;
        }
        
        public Builder permissions(Set<String> permissions) {
            this.permissions = permissions;
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
        
        public Builder companyStatus(String companyStatus) {
            this.companyStatus = companyStatus;
            return this;
        }
        
        public Builder subscriptionPlan(String subscriptionPlan) {
            this.subscriptionPlan = subscriptionPlan;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder loginTime(LocalDateTime loginTime) {
            this.loginTime = loginTime;
            return this;
        }
        
        public Builder lastLoginTime(LocalDateTime lastLoginTime) {
            this.lastLoginTime = lastLoginTime;
            return this;
        }
        
        public Builder language(String language) {
            this.language = language;
            return this;
        }
        
        public Builder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }
        
        public Builder theme(String theme) {
            this.theme = theme;
            return this;
        }
        
        public Builder isFirstLogin(Boolean isFirstLogin) {
            this.isFirstLogin = isFirstLogin;
            return this;
        }
        
        public Builder mustChangePassword(Boolean mustChangePassword) {
            this.mustChangePassword = mustChangePassword;
            return this;
        }
        
        public Builder twoFactorEnabled(Boolean twoFactorEnabled) {
            this.twoFactorEnabled = twoFactorEnabled;
            return this;
        }
        
        public Builder avatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }
        
        public LoginResponse build() {
            return new LoginResponse(
                accessToken, refreshToken, tokenType, expiresIn,
                userId, email, firstName, lastName, firstNameAr, lastNameAr,
                userType, roles, permissions,
                companyId, companyName, companyStatus, subscriptionPlan,
                sessionId, loginTime, lastLoginTime,
                language, timezone, theme,
                isFirstLogin, mustChangePassword, twoFactorEnabled,
                avatarUrl
            );
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}