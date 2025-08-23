package com.cafm.cafmbackend.data.entity;

import com.cafm.cafmbackend.data.entity.base.TenantAwareEntity;
import com.cafm.cafmbackend.data.enums.UserStatus;
import com.cafm.cafmbackend.data.enums.UserType;
import com.cafm.cafmbackend.data.enums.TechnicianSpecialization;
import com.cafm.cafmbackend.data.enums.SkillLevel;
import com.cafm.cafmbackend.data.type.UserStatusUserType;
import com.cafm.cafmbackend.data.type.UserTypeUserType;
import com.cafm.cafmbackend.validation.constraint.ValidIqamaId;
import com.cafm.cafmbackend.validation.constraint.ValidPlateNumber;
import com.cafm.cafmbackend.validation.constraint.ValidArabicText;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Type;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User entity representing the unified user table.
 * 
 * SECURITY ENHANCEMENT:
 * - Now extends TenantAwareEntity for CRITICAL tenant isolation
 * - Purpose: Prevents cross-tenant user access vulnerabilities  
 * - Pattern: Multi-tenant security through automatic tenant validation
 * - Java 23: Enhanced with modern JPA lifecycle callbacks
 * - Architecture: Core tenant-aware entity in data layer
 * - Standards: Implements Spring Security UserDetails with tenant boundaries
 */
@Entity
@Table(name = "users")
@NamedQueries({
    @NamedQuery(
        name = "User.findActiveByType", 
        query = "SELECT u FROM User u WHERE u.userType = :type AND u.status = :status AND u.deletedAt IS NULL AND u.company.id = :companyId"
    ),
    @NamedQuery(
        name = "User.findByEmailOrUsername",
        query = "SELECT u FROM User u WHERE (u.email = :identifier OR u.username = :identifier) AND u.deletedAt IS NULL AND u.company.id = :companyId"
    )
})
public class User extends TenantAwareEntity implements UserDetails {
    
    // ========== Core Fields ==========
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    private String email;
    
    @Column(name = "username", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;
    
    @Column(name = "password_hash", nullable = false, length = 255)
    @NotBlank(message = "Password is required")
    private String passwordHash;
    
    @Column(name = "first_name", length = 100)
    private String firstName;
    
    @Column(name = "last_name", length = 100)
    private String lastName;
    
    @Formula("(COALESCE(first_name, '') || ' ' || COALESCE(last_name, ''))")
    private String fullName;
    
    @Column(name = "phone", length = 20)
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone number must be valid")
    private String phone;
    
    // ========== Employee Information ==========
    
    @Column(name = "employee_id", unique = true, length = 50)
    private String employeeId;
    
    @Column(name = "iqama_id", length = 50)
    @ValidIqamaId
    private String iqamaId;
    
    @Column(name = "plate_number", length = 50)
    @ValidPlateNumber
    private String plateNumber;
    
    @Column(name = "plate_letters_en", length = 10)
    private String plateLettersEn;
    
    @Column(name = "plate_letters_ar", length = 10)
    @ValidArabicText(allowEmpty = true, allowMixed = false)
    private String plateLettersAr;
    
    // ========== User Type and Status ==========
    
    @Column(name = "user_type", nullable = false)
    @NotNull(message = "User type is required")
    @org.hibernate.annotations.Type(UserTypeUserType.class)
    private UserType userType = UserType.VIEWER;
    
    @Column(name = "status", nullable = false)
    @NotNull(message = "Status is required")
    @org.hibernate.annotations.Type(UserStatusUserType.class)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;
    
    // ========== Verification and Security ==========
    
    @Column(name = "email_verified")
    private Boolean emailVerified = false;
    
    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;
    
    @Column(name = "phone_verified")
    private Boolean phoneVerified = false;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "is_locked")
    private Boolean isLocked = false;
    
    // ========== Timestamps ==========
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;
    
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;
    
    @Column(name = "failed_login_attempts")
    @Min(value = 0, message = "Failed login attempts cannot be negative")
    private Integer failedLoginAttempts = 0;
    
    @Column(name = "password_change_required")
    private Boolean passwordChangeRequired = false;
    
    @Column(name = "verification_code", length = 10)
    private String verificationCode;
    
    @Column(name = "verification_code_expiry")
    private LocalDateTime verificationCodeExpiry;
    
    // ========== Extended Profile Fields ==========
    
    @Column(name = "department", length = 100)
    private String department;
    
    @Column(name = "position", length = 100)
    private String position;
    
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    @Column(name = "bio")
    private String bio;
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "city", length = 100)
    private String city;

    
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    
    // ========== Performance Metrics ==========
    
    @Column(name = "performance_rating")
    @DecimalMin(value = "0.00", message = "Performance rating cannot be negative")
    @DecimalMax(value = "5.00", message = "Performance rating cannot exceed 5.00")
    private Double performanceRating;
    
    @Column(name = "productivity_score")
    @Min(value = 0, message = "Productivity score cannot be negative")
    @Max(value = 100, message = "Productivity score cannot exceed 100")
    private Integer productivityScore;
    
    // ========== Technician Specialization Fields ==========
    
    @Enumerated(EnumType.STRING)
    @Column(name = "specialization", length = 30)
    private TechnicianSpecialization specialization;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", length = 20)
    private SkillLevel skillLevel;
    
    @Column(name = "certifications", columnDefinition = "jsonb")
    @Type(JsonType.class)
    private String certifications; // JSON array of certification objects
    
    @Column(name = "years_of_experience")
    @Min(value = 0, message = "Years of experience cannot be negative")
    private Integer yearsOfExperience;
    
    @Column(name = "hourly_rate")
    @DecimalMin(value = "0.00", message = "Hourly rate cannot be negative")
    private Double hourlyRate;
    
    @Column(name = "is_available_for_assignment")
    private Boolean isAvailableForAssignment = true;
    
    // ========== Legacy Fields ==========
    
    @Column(name = "auth_user_id", length = 255)
    private String authUserId; // Legacy Supabase auth ID
    
    @Column(name = "admin_id")
    private UUID adminId; // Reference to admin who manages this user
    
    @Column(name = "technicians_detailed", columnDefinition = "jsonb")
    @Type(JsonType.class)
    private String techniciansDetailed; // JSON data for technician details
    
    
    // ========== Multi-Tenant Relationship ==========
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    @NotNull(message = "Company is required")
    private Company company;
    
    // ========== Relationships ==========
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
    
    @OneToMany(mappedBy = "supervisor", fetch = FetchType.LAZY)
    private Set<SupervisorSchool> supervisorSchools = new HashSet<>();
    
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<RefreshToken> refreshTokens = new HashSet<>();
    
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<Notification> notifications = new HashSet<>();
    
    @OneToMany(mappedBy = "supervisor", fetch = FetchType.LAZY)
    private Set<Report> createdReports = new HashSet<>();
    
    @OneToMany(mappedBy = "assignedTo", fetch = FetchType.LAZY)
    private Set<Report> assignedReports = new HashSet<>();
    
    // ========== Constructors ==========
    
    public User() {
        super();
    }
    
    public User(String email, String username, String passwordHash, UserType userType) {
        this();
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.userType = userType;
    }
    
    public User(String email, String username, String passwordHash, UserType userType, Company company) {
        this(email, username, passwordHash, userType);
        this.company = company;
    }
    
    
    // ========== UserDetails Implementation ==========
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        
        // Add user type as authority with ROLE_ prefix for Spring Security
        authorities.add(new SimpleGrantedAuthority("ROLE_" + userType.name()));
        
        // Add role-based authorities
        if (roles != null && !roles.isEmpty()) {
            authorities.addAll(roles.stream()
                .map(role -> {
                    String roleName = role.getName();
                    // Ensure ROLE_ prefix is present for Spring Security
                    if (!roleName.startsWith("ROLE_")) {
                        roleName = "ROLE_" + roleName;
                    }
                    return new SimpleGrantedAuthority(roleName);
                })
                .collect(Collectors.toSet()));
        }
        
        return authorities;
    }
    
    @Override
    public String getPassword() {
        return passwordHash;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return isActive != null && isActive;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return isLocked == null || !isLocked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        // Implement password expiry logic if needed
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return isActive != null && isActive && !isDeleted();
    }
    
    // ========== Business Methods ==========
    
    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String roleName) {
        return roles.stream()
            .anyMatch(role -> role.getName().equals(roleName));
    }
    
    /**
     * Check if user is of admin type (admin or super_admin)
     */
    public boolean isAdmin() {
        return userType == UserType.ADMIN || userType == UserType.SUPER_ADMIN;
    }
    
    /**
     * Check if user is a supervisor
     */
    public boolean isSupervisor() {
        return userType == UserType.SUPERVISOR;
    }
    
    /**
     * Check if user is a technician
     */
    public boolean isTechnician() {
        return userType == UserType.TECHNICIAN;
    }
    
    /**
     * Check if user is verified
     */
    public boolean isVerified() {
        return Boolean.TRUE.equals(emailVerified) && status == UserStatus.ACTIVE;
    }
    
    /**
     * Check if technician has the required specialization
     */
    public boolean hasSpecialization(TechnicianSpecialization requiredSpecialization) {
        return isTechnician() && specialization == requiredSpecialization;
    }
    
    /**
     * Check if technician meets minimum skill level
     */
    public boolean meetsSkillLevel(SkillLevel minimumLevel) {
        return isTechnician() && skillLevel != null && skillLevel.isAtLeast(minimumLevel);
    }
    
    /**
     * Check if technician is available for new assignments
     */
    public boolean isAvailableForAssignment() {
        return isTechnician() && 
               Boolean.TRUE.equals(isAvailableForAssignment) && 
               isVerified() && 
               !isLocked;
    }
    
    /**
     * Check if technician can handle complex tasks
     */
    public boolean canHandleComplexTasks() {
        return isTechnician() && 
               skillLevel != null && 
               skillLevel.isAtLeast(SkillLevel.getMinimumForComplexTasks());
    }
    
    /**
     * Get technician's experience category
     */
    public String getExperienceCategory() {
        if (!isTechnician() || yearsOfExperience == null) {
            return "N/A";
        }
        
        return switch (yearsOfExperience) {
            case 0, 1 -> "Entry Level";
            case 2, 3, 4 -> "Junior";
            case 5, 6, 7, 8, 9 -> "Mid-Level";
            case 10, 11, 12, 13, 14 -> "Senior";
            default -> "Expert";
        };
    }
    
    /**
     * Update last login timestamp
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
    
    /**
     * Lock the user account
     */
    public void lockAccount(String reason) {
        this.isLocked = true;
        this.status = UserStatus.SUSPENDED;
    }
    
    /**
     * Unlock the user account
     */
    public void unlockAccount() {
        this.isLocked = false;
        if (status == UserStatus.SUSPENDED) {
            this.status = UserStatus.ACTIVE;
        }
    }
    
    /**
     * Get display name (full name or username)
     */
    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName.trim();
        }
        return username;
    }
    
    /**
     * Check if user belongs to a specific company
     */
    public boolean belongsToCompany(UUID companyId) {
        return company != null && Objects.equals(company.getId(), companyId);
    }
    
    /**
     * Check if user's company is active
     */
    public boolean hasActiveCompany() {
        return company != null && company.isAccessible();
    }
    
    // ========== Getters and Setters ==========
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    @Override
    public String getUsername() {
        return email; // Use email as username for Spring Security (matches JWT subject)
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.passwordChangedAt = LocalDateTime.now();
    }
    
    /**
     * Alias method for compatibility with test fixtures
     */
    public void setPassword(String password) {
        this.passwordHash = password;
        this.passwordChangedAt = LocalDateTime.now();
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    
    public String getIqamaId() {
        return iqamaId;
    }
    
    public void setIqamaId(String iqamaId) {
        this.iqamaId = iqamaId;
    }
    
    public String getPlateNumber() {
        return plateNumber;
    }
    
    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }
    
    public String getPlateLettersEn() {
        return plateLettersEn;
    }
    
    public void setPlateLettersEn(String plateLettersEn) {
        this.plateLettersEn = plateLettersEn;
    }
    
    public String getPlateLettersAr() {
        return plateLettersAr;
    }
    
    public void setPlateLettersAr(String plateLettersAr) {
        this.plateLettersAr = plateLettersAr;
    }
    
    public UserType getUserType() {
        return userType;
    }
    
    public void setUserType(UserType userType) {
        this.userType = userType;
    }
    
    public UserStatus getStatus() {
        return status;
    }
    
    public void setStatus(UserStatus status) {
        this.status = status;
    }
    
    public Boolean getEmailVerified() {
        return emailVerified;
    }
    
    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
    
    public LocalDateTime getEmailVerifiedAt() {
        return emailVerifiedAt;
    }
    
    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }
    
    public Boolean getPhoneVerified() {
        return phoneVerified;
    }
    
    public void setPhoneVerified(Boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Boolean getIsLocked() {
        return isLocked;
    }
    
    public void setIsLocked(Boolean isLocked) {
        this.isLocked = isLocked;
    }
    
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
    
    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    
    public String getLastLoginIp() {
        return lastLoginIp;
    }
    
    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }
    
    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }
    
    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }
    
    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }
    
    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }
    
    public Boolean getPasswordChangeRequired() {
        return passwordChangeRequired;
    }
    
    public void setPasswordChangeRequired(Boolean passwordChangeRequired) {
        this.passwordChangeRequired = passwordChangeRequired;
    }
    
    public String getVerificationCode() {
        return verificationCode;
    }
    
    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }
    
    public LocalDateTime getVerificationCodeExpiry() {
        return verificationCodeExpiry;
    }
    
    public void setVerificationCodeExpiry(LocalDateTime verificationCodeExpiry) {
        this.verificationCodeExpiry = verificationCodeExpiry;
    }
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public String getPosition() {
        return position;
    }
    
    public void setPosition(String position) {
        this.position = position;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public String getBio() {
        return bio;
    }
    
    public void setBio(String bio) {
        this.bio = bio;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getPostalCode() {
        return postalCode;
    }
    
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }
    
    public Double getPerformanceRating() {
        return performanceRating;
    }
    
    public void setPerformanceRating(Double performanceRating) {
        this.performanceRating = performanceRating;
    }
    
    public Integer getProductivityScore() {
        return productivityScore;
    }
    
    public void setProductivityScore(Integer productivityScore) {
        this.productivityScore = productivityScore;
    }
    
    public TechnicianSpecialization getSpecialization() {
        return specialization;
    }
    
    public void setSpecialization(TechnicianSpecialization specialization) {
        this.specialization = specialization;
    }
    
    public SkillLevel getSkillLevel() {
        return skillLevel;
    }
    
    public void setSkillLevel(SkillLevel skillLevel) {
        this.skillLevel = skillLevel;
    }
    
    public String getCertifications() {
        return certifications;
    }
    
    public void setCertifications(String certifications) {
        this.certifications = certifications;
    }
    
    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }
    
    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }
    
    public Double getHourlyRate() {
        return hourlyRate;
    }
    
    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }
    
    public Boolean getIsAvailableForAssignment() {
        return isAvailableForAssignment;
    }
    
    public void setIsAvailableForAssignment(Boolean isAvailableForAssignment) {
        this.isAvailableForAssignment = isAvailableForAssignment;
    }
    
    public String getAuthUserId() {
        return authUserId;
    }
    
    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }
    
    public UUID getAdminId() {
        return adminId;
    }
    
    public void setAdminId(UUID adminId) {
        this.adminId = adminId;
    }
    
    public String getTechniciansDetailed() {
        return techniciansDetailed;
    }
    
    public void setTechniciansDetailed(String techniciansDetailed) {
        this.techniciansDetailed = techniciansDetailed;
    }
    
    public Set<Role> getRoles() {
        return roles;
    }
    
    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
    
    public Set<SupervisorSchool> getSupervisorSchools() {
        return supervisorSchools;
    }
    
    public void setSupervisorSchools(Set<SupervisorSchool> supervisorSchools) {
        this.supervisorSchools = supervisorSchools;
    }
    
    public Set<RefreshToken> getRefreshTokens() {
        return refreshTokens;
    }
    
    public void setRefreshTokens(Set<RefreshToken> refreshTokens) {
        this.refreshTokens = refreshTokens;
    }
    
    public Set<Notification> getNotifications() {
        return notifications;
    }
    
    public void setNotifications(Set<Notification> notifications) {
        this.notifications = notifications;
    }
    
    public Set<Report> getCreatedReports() {
        return createdReports;
    }
    
    public void setCreatedReports(Set<Report> createdReports) {
        this.createdReports = createdReports;
    }
    
    public Set<Report> getAssignedReports() {
        return assignedReports;
    }
    
    public void setAssignedReports(Set<Report> assignedReports) {
        this.assignedReports = assignedReports;
    }
    
    public Company getCompany() {
        return company;
    }
    
    public void setCompany(Company company) {
        this.company = company;
    }
    
    // ========== toString, equals, hashCode ==========
    
    @Override
    public String toString() {
        return String.format("User[id=%s, username=%s, email=%s, type=%s, status=%s]",
            getId(), username, email, userType, status);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(email, user.email) && 
               Objects.equals(username, user.username);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), email, username);
    }
}