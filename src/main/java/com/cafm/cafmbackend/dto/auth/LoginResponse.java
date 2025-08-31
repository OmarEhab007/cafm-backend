package com.cafm.cafmbackend.dto.auth;

import com.cafm.cafmbackend.shared.enums.UserType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Login response DTO containing authentication tokens and user information.
 */
@Schema(description = "Successful login response with tokens and user information")
public record LoginResponse(
    @Schema(description = "JWT access token for API authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String accessToken,
    
    @Schema(description = "JWT refresh token for token renewal", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String refreshToken,
    
    @Schema(description = "Token type", example = "Bearer", defaultValue = "Bearer")
    String tokenType,
    
    @Schema(description = "Access token expiration time in seconds", example = "3600")
    Long expiresIn,
    
    @Schema(description = "Unique user identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID userId,
    
    @Schema(description = "User's email address", example = "john.doe@example.com")
    String email,
    
    @Schema(description = "User's first name", example = "John")
    String firstName,
    
    @Schema(description = "User's last name", example = "Doe")
    String lastName,
    
    @Schema(description = "User's first name in Arabic", example = "جون")
    String firstNameAr,
    
    @Schema(description = "User's last name in Arabic", example = "دو")
    String lastNameAr,
    
    @Schema(description = "User type/role in the system", example = "ADMIN")
    UserType userType,
    
    @Schema(description = "User's assigned roles", example = "[\"ADMIN\", \"USER\"]")
    Set<String> roles,
    
    @Schema(description = "User's specific permissions", example = "[\"READ_USERS\", \"WRITE_REPORTS\"]")
    Set<String> permissions,
    
    @Schema(description = "Company/tenant identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID companyId,
    
    @Schema(description = "Company name", example = "ACME School District")
    String companyName,
    
    @Schema(description = "Company account status", example = "ACTIVE")
    String companyStatus,
    
    @Schema(description = "Company subscription plan", example = "PREMIUM")
    String subscriptionPlan,
    
    @Schema(description = "Unique session identifier", example = "sess_123456789")
    String sessionId,
    
    @Schema(description = "Current login timestamp", example = "2024-01-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime loginTime,
    
    @Schema(description = "Previous login timestamp", example = "2024-01-14T09:15:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime lastLoginTime,
    
    @Schema(description = "User's preferred language", example = "en", allowableValues = {"en", "ar"})
    String language,
    
    @Schema(description = "User's timezone", example = "Asia/Riyadh")
    String timezone,
    
    @Schema(description = "UI theme preference", example = "light", allowableValues = {"light", "dark"})
    String theme,
    
    @Schema(description = "Indicates if this is the user's first login", example = "false")
    Boolean isFirstLogin,
    
    @Schema(description = "Indicates if user must change password", example = "false")
    Boolean mustChangePassword,
    
    @Schema(description = "Indicates if two-factor authentication is enabled", example = "true")
    Boolean twoFactorEnabled,
    
    @Schema(description = "URL to user's profile picture", example = "https://api.example.com/avatars/user123.jpg")
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