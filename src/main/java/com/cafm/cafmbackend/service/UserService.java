package com.cafm.cafmbackend.service;

import com.cafm.cafmbackend.data.entity.Company;
import com.cafm.cafmbackend.data.entity.RefreshToken;
import com.cafm.cafmbackend.data.entity.User;
import com.cafm.cafmbackend.data.enums.UserStatus;
import com.cafm.cafmbackend.data.enums.UserType;
import com.cafm.cafmbackend.data.repository.CompanyRepository;
import com.cafm.cafmbackend.data.repository.UserRepository;
import com.cafm.cafmbackend.data.specification.UserSpecification;
import com.cafm.cafmbackend.dto.user.UserCreateRequest;
import com.cafm.cafmbackend.dto.user.UserUpdateRequest;
import com.cafm.cafmbackend.dto.user.UserResponseSimplified;
import com.cafm.cafmbackend.dto.user.UserProfileUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for managing users and authentication.
 * Implements UserDetailsService for Spring Security integration.
 */
@Service
@Transactional
public class UserService implements UserDetailsService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;
    
    @Value("${app.security.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;
    
    public UserService(UserRepository userRepository,
                      CompanyRepository companyRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    // ========== UserDetailsService Implementation ==========
    
    @Override
    @Cacheable(value = "users", key = "#username", unless = "#result == null")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by username: {}", username);
        
        User user = userRepository.findByEmailIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        if (!user.isEnabled()) {
            throw new BadCredentialsException("User account is disabled");
        }
        
        return user;
    }
    
    // ========== DTO Conversion Methods ==========
    
    /**
     * Convert User entity to simplified response DTO.
     * This replaces the mapper layer with direct conversion.
     */
    public UserResponseSimplified toResponse(User user) {
        if (user == null) return null;
        
        return UserResponseSimplified.builder()
            .id(user.getId())
            .email(user.getEmail())
            .username(user.getUsername())
            .userType(user.getUserType())
            .status(user.getStatus())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .phone(user.getPhone())
            .employeeId(user.getEmployeeId())
            .iqamaId(user.getIqamaId())
            .plateNumber(user.getPlateNumber())
            .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
            .companyName(user.getCompany() != null ? user.getCompany().getName() : null)
            .department(user.getDepartment())
            .position(user.getPosition())
            .emailVerified(user.getEmailVerified())
            .phoneVerified(user.getPhoneVerified())
            .isActive(user.getIsActive())
            .isLocked(user.getIsLocked())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .roles(user.getRoles().stream()
                .map(role -> role.getName())
                .collect(java.util.stream.Collectors.toSet()))
            .build();
    }
    
    /**
     * Convert UserCreateRequest to User entity.
     * Sets up a new user with proper defaults.
     */
    public User fromCreateRequest(UserCreateRequest request, Company company) {
        User user = new User();
        
        // Required fields
        user.setEmail(request.email());
        user.setUsername(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUserType(request.userType());
        user.setCompany(company);
        
        // Optional fields
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phoneNumber());
        user.setEmployeeId(request.employeeId());
        user.setDepartment(request.department());
//        user.setPosition(request.position());
        
        // Set defaults
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setIsActive(true);
        user.setIsLocked(false);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        
        return user;
    }
    
    /**
     * Update User entity from UserUpdateRequest.
     * Only updates provided fields.
     */
    public void updateFromRequest(User user, UserUpdateRequest request) {
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.phoneNumber() != null) {
            user.setPhone(request.phoneNumber());
        }
        if (request.department() != null) {
            user.setDepartment(request.department());
        }
//        if (request.position() != null) {
//            user.setPosition(request.position());
//        }
        if (request.employeeId() != null) {
            user.setEmployeeId(request.employeeId());
        }
    }
    
    // ========== Authentication Methods ==========
    
    /**
     * Authenticate user with email and password.
     */
    public User authenticate(String email, String password) {
        logger.info("Authentication attempt for email: {}", email);
        
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        
        // Check if account is locked
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            logger.warn("Login attempt for locked account: {}", email);
            throw new BadCredentialsException("Account is locked");
        }
        
        // Verify password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new BadCredentialsException("Invalid credentials");
        }
        
        // Check account status
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException("Account is not active. Status: " + user.getStatus());
        }
        
        // Update last login
        user.updateLastLogin();
        
        return userRepository.save(user);
    }
    
    /**
     * Handle failed login attempt.
     */
    private void handleFailedLogin(User user) {
        // Log failed attempt
        logger.warn("Failed login attempt for user: {}", user.getEmail());
        // In a production system, you might want to track failed attempts in a separate table
        // or use a rate limiting service
    }
    
    /**
     * Verify user's password.
     */
    public boolean verifyPassword(UUID userId, String password) {
        User user = findById(userId);
        return passwordEncoder.matches(password, user.getPasswordHash());
    }
    
    /**
     * Change user's password.
     */
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        logger.info("Password change request for user: {}", userId);
        
        User user = findById(userId);
        
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        
        userRepository.save(user);
        logger.info("Password changed successfully for user: {}", userId);
    }
    
    /**
     * Reset user's password (admin action or forgot password).
     */
    public void resetPassword(UUID userId, String newPassword) {
        logger.info("Password reset for user: {}", userId);
        
        User user = findById(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        
        // Unlock account if it was locked
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            user.unlockAccount();
        }
        
        userRepository.save(user);
    }
    
    // ========== User Management Methods ==========
    
    /**
     * Create a new user.
     */
    public User createUser(User user, UUID companyId) {
        logger.info("Creating new user with email: {}", user.getEmail());
        
        // Validate email uniqueness
        if (userRepository.existsByEmailIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }
        
        // Validate phone uniqueness if provided
        if (user.getPhone() != null && userRepository.findByPhone(user.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Phone number already exists: " + user.getPhone());
        }
        
        // Validate Iqama ID uniqueness if provided
        if (user.getIqamaId() != null && userRepository.findByIqamaId(user.getIqamaId()).isPresent()) {
            throw new IllegalArgumentException("Iqama ID already exists: " + user.getIqamaId());
        }
        
        // Set company
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
        user.setCompany(company);
        
        // Check company user limit
        long currentUserCount = userRepository.countByCompany_IdAndDeletedAtIsNull(companyId);
        if (currentUserCount >= company.getMaxUsers()) {
            throw new IllegalStateException("Company has reached maximum user limit: " + company.getMaxUsers());
        }
        
        // Encode password if provided (password is stored in passwordHash field)
        if (user.getPasswordHash() != null) {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }
        
        // Set default values
        user.setStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        user.setIsLocked(false);
        
        User savedUser = userRepository.save(user);
        logger.info("User created successfully with ID: {}", savedUser.getId());
        
        return savedUser;
    }
    
    /**
     * Update an existing user.
     */
    public User updateUser(UUID userId, User updatedUser) {
        logger.info("Updating user: {}", userId);
        
        User existingUser = findById(userId);
        
        // Update allowed fields
        if (updatedUser.getFirstName() != null) {
            existingUser.setFirstName(updatedUser.getFirstName());
        }
        if (updatedUser.getLastName() != null) {
            existingUser.setLastName(updatedUser.getLastName());
        }
        if (updatedUser.getPhone() != null) {
            existingUser.setPhone(updatedUser.getPhone());
        }
        if (updatedUser.getAddress() != null) {
            existingUser.setAddress(updatedUser.getAddress());
        }
        if (updatedUser.getCity() != null) {
            existingUser.setCity(updatedUser.getCity());
        }
        if (updatedUser.getDepartment() != null) {
            existingUser.setDepartment(updatedUser.getDepartment());
        }
        if (updatedUser.getPosition() != null) {
            existingUser.setPosition(updatedUser.getPosition());
        }
        
        // Update role if changed and user has permission
        if (updatedUser.getUserType() != null) {
            existingUser.setUserType(updatedUser.getUserType());
        }
        
        return userRepository.save(existingUser);
    }
    

    
    /**
     * Activate a user account.
     */
    public UserResponseSimplified activateUser(UUID userId) {
        logger.info("Activating user: {}", userId);
        
        User user = findById(userId);
        user.setStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        user.setEmailVerified(true);
        
        User savedUser = userRepository.save(user);
        return toResponse(savedUser);
    }
    
    /**
     * Deactivate a user account.
     */
    public UserResponseSimplified deactivateUser(UUID userId) {
        logger.info("Deactivating user: {}", userId);
        
        User user = findById(userId);
        user.setStatus(UserStatus.INACTIVE);
        
        User savedUser = userRepository.save(user);
        return toResponse(savedUser);
    }
    
    /**
     * Suspend a user account.
     */
    public User suspendUser(UUID userId, String reason) {
        logger.info("Suspending user: {} for reason: {}", userId, reason);
        
        User user = findById(userId);
        user.setStatus(UserStatus.SUSPENDED);
        // Store suspension reason in metadata or audit log
        
        return userRepository.save(user);
    }
    
    // ========== User CRUD Operations with DTO Conversion ==========
    
    /**
     * Create a new user with proper validation and defaults.
     */
    @Transactional
    public UserResponseSimplified createUser(UserCreateRequest request) {
        logger.info("Creating new user with email: {}", request.email());
        
        // Validate email uniqueness
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email already exists: " + request.email());
        }
        
        // Validate username uniqueness
        if (userRepository.existsByUsernameIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Username already exists: " + request.email());
        }
        
        // Get company
        Company company = companyRepository.findById(request.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + request.companyId()));
        
        // Convert and save
        User user = fromCreateRequest(request, company);
        user = userRepository.save(user);
        
        logger.info("Created user with ID: {}", user.getId());
        return toResponse(user);
    }
    
    /**
     * Get user by ID with response conversion.
     */
    public UserResponseSimplified getUserById(UUID id) {
        User user = findById(id);
        return toResponse(user);
    }
    
    /**
     * Update existing user.
     */
    @Transactional
    public UserResponseSimplified updateUser(UUID id, UserUpdateRequest request) {
        logger.info("Updating user with ID: {}", id);
        
        User user = findById(id);
        updateFromRequest(user, request);
        user = userRepository.save(user);
        
        logger.info("Updated user with ID: {}", id);
        return toResponse(user);
    }
    
    /**
     * Get paginated list of users.
     */
    public Page<UserResponseSimplified> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
            .map(this::toResponse);
    }
    
    /**
     * Get all users (alias for getUsers).
     */
    public Page<UserResponseSimplified> getAllUsers(Pageable pageable) {
        return getUsers(pageable);
    }
    
    /**
     * Search users with simplified parameters.
     */
    public Page<UserResponseSimplified> searchUsers(String searchTerm, Pageable pageable) {
        // Use existing searchUsers method with default parameters
        return searchUsers(searchTerm, null, null, null, pageable)
            .map(this::toResponse);
    }
    
    /**
     * Get users by type.
     */
    public Page<UserResponseSimplified> getUsersByType(UserType userType, Pageable pageable) {
        // Use specification-based approach since exact method doesn't exist
        Specification<User> spec = UserSpecification.hasUserType(userType)
            .and(UserSpecification.isNotDeleted());
        return userRepository.findAll(spec, pageable)
            .map(this::toResponse);
    }
    
    /**
     * Get users by status.
     */
    public Page<UserResponseSimplified> getUsersByStatus(UserStatus status, Pageable pageable) {
        // Use specification-based approach since exact method doesn't exist
        Specification<User> spec = UserSpecification.hasStatus(status)
            .and(UserSpecification.isNotDeleted());
        return userRepository.findAll(spec, pageable)
            .map(this::toResponse);
    }
    
    /**
     * Lock user account.
     */
    public UserResponseSimplified lockUser(UUID userId, String reason) {
        User user = findById(userId);
        user.setStatus(UserStatus.LOCKED);
        user.setIsLocked(true);
        User savedUser = userRepository.save(user);
        logger.info("User {} locked. Reason: {}", userId, reason);
        return toResponse(savedUser);
    }
    
    /**
     * Unlock user account.
     */
    public UserResponseSimplified unlockUser(UUID userId) {
        User user = findById(userId);
        user.setStatus(UserStatus.ACTIVE);
        user.setIsLocked(false);
        user.setFailedLoginAttempts(0);
        User savedUser = userRepository.save(user);
        logger.info("User {} unlocked", userId);
        return toResponse(savedUser);
    }
    
    /**
     * Reset user password.
     */
    public String resetUserPassword(UUID userId) {
        User user = findById(userId);
        String newPassword = generateRandomPassword();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(true);
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        logger.info("Password reset for user {}", userId);
        return newPassword;
    }
    
    /**
     * Get users by company.
     */
    public Page<UserResponseSimplified> getUsersByCompany(UUID companyId, Pageable pageable) {
        return userRepository.findByCompany_IdAndDeletedAtIsNull(companyId, pageable)
            .map(this::toResponse);
    }
    
    /**
     * Get user by email.
     */
    public UserResponseSimplified getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        return toResponse(user);
    }
    
    /**
     * Update user profile.
     */
    public UserResponseSimplified updateUserProfile(String email, UserProfileUpdateRequest request) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        
        // Update profile fields
        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null) user.setLastName(request.lastName());
        if (request.phone() != null) user.setPhone(request.phone());
        
        User savedUser = userRepository.save(user);
        return toResponse(savedUser);
    }
    
    /**
     * Generate random password.
     */
    private String generateRandomPassword() {
        // Simple random password generation - should be improved
        return "TempPass" + System.currentTimeMillis() % 10000;
    }
    
    /**
     * Delete user (soft delete).
     */
    @Transactional
    public void deleteUser(UUID id) {
        logger.info("Deleting user with ID: {}", id);
        
        User user = findById(id);
        user.setDeletedAt(LocalDateTime.now());
        user.setIsActive(false);
        userRepository.save(user);
        
        logger.info("Soft deleted user with ID: {}", id);
    }
    
    // ========== Query Methods ==========
    
    /**
     * Find user by ID.
     */
    public User findById(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }
    
    /**
     * Find user by email.
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }
    
    /**
     * Find users by company with pagination.
     */
    public Page<User> findByCompany(UUID companyId, Pageable pageable) {
        return userRepository.findByCompany_IdAndDeletedAtIsNull(companyId, pageable);
    }
    
    /**
     * Find users by type.
     */
    public List<User> findByUserType(UserType userType, UUID companyId) {
        return userRepository.findByUserTypeAndCompany_IdAndDeletedAtIsNull(userType, companyId);
    }
    
    /**
     * Search users with multiple criteria.
     */
    public Page<User> searchUsers(String searchTerm, UserType userType, UserStatus status, 
                                 UUID companyId, Pageable pageable) {
        Specification<User> spec = Specification.where(UserSpecification.belongsToCompany(companyId))
            .and(UserSpecification.isNotDeleted());
        
        if (searchTerm != null && !searchTerm.isEmpty()) {
            spec = spec.and(UserSpecification.hasSearchTerm(searchTerm));
        }
        
        if (userType != null) {
            spec = spec.and(UserSpecification.hasUserType(userType));
        }
        
        if (status != null) {
            spec = spec.and(UserSpecification.hasStatus(status));
        }
        
        return userRepository.findAll(spec, pageable);
    }
    
    /**
     * Get user statistics for a company.
     */
    @Transactional(readOnly = true)
    public UserStatistics getUserStatistics(UUID companyId) {
        UserStatistics stats = new UserStatistics();
        
        stats.totalUsers = userRepository.countByCompany_IdAndDeletedAtIsNull(companyId);
        stats.activeUsers = userRepository.countByCompany_IdAndStatusAndDeletedAtIsNull(companyId, UserStatus.ACTIVE);
        stats.suspendedUsers = userRepository.countByCompany_IdAndStatusAndDeletedAtIsNull(companyId, UserStatus.SUSPENDED);
        
        // Count by user type
        for (UserType type : UserType.values()) {
            long count = userRepository.countByCompany_IdAndUserTypeAndDeletedAtIsNull(companyId, type);
            stats.usersByType.put(type, count);
        }
        
        return stats;
    }
    
    // ========== Verification Methods ==========
    
    /**
     * Generate and send verification code for email.
     */
    public String generateEmailVerificationCode(UUID userId) {
        logger.info("Generating email verification code for user: {}", userId);
        
        User user = findById(userId);
        String code = generateVerificationCode();
        user.setVerificationCode(code);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(30));
        
        userRepository.save(user);
        
        // Notification service would send the code via email
        logger.info("Verification code generated for user {}: {}", userId, code);
        return code;
    }
    
    /**
     * Verify user's email address.
     */
    public void verifyEmail(UUID userId, String verificationCode) {
        logger.info("Email verification for user: {}", userId);
        
        User user = findById(userId);
        
        // Check if verification code matches and is not expired
        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(verificationCode)) {
            throw new IllegalArgumentException("Invalid verification code");
        }
        
        if (user.getVerificationCodeExpiry() == null || user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code has expired");
        }
        
        // Mark email as verified
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        
        // Update user status if pending verification
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
        }
        
        userRepository.save(user);
        logger.info("Email verified successfully for user: {}", userId);
    }
    
    /**
     * Generate and send verification code for phone.
     */
    public String generatePhoneVerificationCode(UUID userId) {
        logger.info("Generating phone verification code for user: {}", userId);
        
        User user = findById(userId);
        String code = generateVerificationCode();
        user.setVerificationCode(code);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(10));
        
        userRepository.save(user);
        
        // SMS service would send the code
        logger.info("Phone verification code generated for user {}: {}", userId, code);
        return code;
    }
    
    /**
     * Verify user's phone number.
     */
    public void verifyPhone(UUID userId, String verificationCode) {
        logger.info("Phone verification for user: {}", userId);
        
        User user = findById(userId);
        
        // Check if verification code matches and is not expired
        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(verificationCode)) {
            throw new IllegalArgumentException("Invalid verification code");
        }
        
        if (user.getVerificationCodeExpiry() == null || user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code has expired");
        }
        
        // Mark phone as verified
        user.setPhoneVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        
        userRepository.save(user);
        logger.info("Phone verified successfully for user: {}", userId);
    }
    
    /**
     * Generate a random verification code.
     */
    private String generateVerificationCode() {
        // Generate 6-digit code
        return String.format("%06d", new java.util.Random().nextInt(999999));
    }
    
    // ========== Utility Methods ==========
    
    /**
     * Update user's last activity timestamp.
     */
    public void updateLastActivity(UUID userId) {
        User user = findById(userId);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    /**
     * Check if email is available.
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmailIgnoreCase(email);
    }
    
    /**
     * Check if phone is available.
     */
    public boolean isPhoneAvailable(String phone) {
        return !userRepository.findByPhone(phone).isPresent();
    }
    
    /**
     * Check if Iqama ID is available.
     */
    public boolean isIqamaIdAvailable(String iqamaId) {
        return !userRepository.findByIqamaId(iqamaId).isPresent();
    }
    
    /**
     * Get available technicians for a work order.
     */
    public List<User> getAvailableTechnicians(UUID companyId, com.cafm.cafmbackend.data.enums.TechnicianSpecialization specialization) {
        // Get technicians by specialization
        List<User> technicians = userRepository.findAvailableTechniciansBySpecialization(UserType.TECHNICIAN, specialization, UserStatus.ACTIVE);
        // Filter by company
        return technicians.stream()
            .filter(t -> t.getCompany() != null && t.getCompany().getId().equals(companyId))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get supervisors with capacity for new assignments.
     */
    public List<User> getSupervisorsWithCapacity(UUID companyId) {
        // Get all supervisors for the company
        List<User> supervisors = userRepository.findByUserTypeAndCompany_IdAndDeletedAtIsNull(UserType.SUPERVISOR, companyId);
        // TODO: Filter by capacity when supervisor assignment tracking is fully implemented
        return supervisors;
    }
    
    // ========== Security Methods for @PreAuthorize ==========
    
    /**
     * Check if the target user belongs to the same company as the current user.
     * 
     * Purpose: Security method for @PreAuthorize annotations in controllers
     * Pattern: Tenant isolation enforcement through company membership validation
     * Java 23: Modern exception handling with detailed logging
     * Architecture: Repository queries with null-safe operations
     * Standards: Security audit logging for compliance
     */
    public boolean isSameCompany(UUID targetUserId, String currentUserEmail) {
        logger.debug("Checking if user {} belongs to same company as target user {}", 
                    currentUserEmail, targetUserId);
        
        try {
            // Get current user
            Optional<User> currentUserOpt = userRepository.findByEmailIgnoreCase(currentUserEmail);
            if (currentUserOpt.isEmpty()) {
                logger.warn("Security check failed: Current user not found: {}", currentUserEmail);
                return false;
            }
            
            User currentUser = currentUserOpt.get();
            if (currentUser.getCompany() == null) {
                logger.warn("Security check failed: Current user has no company: {}", currentUserEmail);
                return false;
            }
            
            // Get target user
            Optional<User> targetUserOpt = userRepository.findById(targetUserId);
            if (targetUserOpt.isEmpty()) {
                logger.warn("Security check failed: Target user not found: {}", targetUserId);
                return false;
            }
            
            User targetUser = targetUserOpt.get();
            if (targetUser.getCompany() == null) {
                logger.warn("Security check failed: Target user has no company: {}", targetUserId);
                return false;
            }
            
            // Check if both users belong to the same company
            boolean sameCompany = currentUser.getCompany().getId().equals(targetUser.getCompany().getId());
            
            if (sameCompany) {
                logger.debug("Security check passed: Users {} and {} belong to same company {}", 
                           currentUserEmail, targetUserId, currentUser.getCompany().getId());
            } else {
                logger.warn("Security violation: User {} attempted to access user {} from different company. " +
                           "Current user company: {}, Target user company: {}", 
                           currentUserEmail, targetUserId, 
                           currentUser.getCompany().getId(), targetUser.getCompany().getId());
            }
            
            return sameCompany;
            
        } catch (Exception e) {
            logger.error("Error during security check for isSameCompany({}, {}): {}", 
                        targetUserId, currentUserEmail, e.getMessage(), e);
            // Fail secure - deny access on any error
            return false;
        }
    }
    
    /**
     * Check if the current user can manage the target user.
     * 
     * Purpose: Security method for management operations in @PreAuthorize
     * Pattern: Role-based access control with hierarchical permissions
     * Java 23: Pattern matching for role validation
     * Architecture: Business logic encapsulation for authorization rules
     * Standards: Comprehensive audit logging for security events
     */
    public boolean canManageUser(UUID targetUserId, String currentUserEmail) {
        logger.debug("Checking if user {} can manage target user {}", 
                    currentUserEmail, targetUserId);
        
        try {
            // Get current user
            Optional<User> currentUserOpt = userRepository.findByEmailIgnoreCase(currentUserEmail);
            if (currentUserOpt.isEmpty()) {
                logger.warn("Security check failed: Current user not found: {}", currentUserEmail);
                return false;
            }
            
            User currentUser = currentUserOpt.get();
            
            // Get target user
            Optional<User> targetUserOpt = userRepository.findById(targetUserId);
            if (targetUserOpt.isEmpty()) {
                logger.warn("Security check failed: Target user not found: {}", targetUserId);
                return false;
            }
            
            User targetUser = targetUserOpt.get();
            
            // First check if they belong to the same company
            if (!isSameCompany(targetUserId, currentUserEmail)) {
                logger.warn("Security violation: User {} cannot manage user {} - different companies", 
                           currentUserEmail, targetUserId);
                return false;
            }
            
            // Self-management is always allowed for profile updates
            if (currentUser.getId().equals(targetUserId)) {
                logger.debug("Security check passed: Self-management allowed for user {}", 
                           currentUserEmail);
                return true;
            }
            
            // Role-based management rules
            UserType currentUserType = currentUser.getUserType();
            UserType targetUserType = targetUser.getUserType();
            
            boolean canManage;
            switch (currentUserType) {
                case ADMIN, SUPER_ADMIN -> {
                    // Admins and super admins can manage all users in their company
                    logger.debug("Admin {} can manage user {} of type {}", 
                               currentUserEmail, targetUserId, targetUserType);
                    canManage = true;
                }
                case SUPERVISOR -> {
                    // Supervisors can only manage technicians and viewers
                    boolean allowed = targetUserType == UserType.TECHNICIAN || 
                                    targetUserType == UserType.VIEWER;
                    if (allowed) {
                        logger.debug("Supervisor {} can manage user {} of type {}", 
                                   currentUserEmail, targetUserId, targetUserType);
                    } else {
                        logger.warn("Security violation: Supervisor {} cannot manage user {} of type {}", 
                                  currentUserEmail, targetUserId, targetUserType);
                    }
                    canManage = allowed;
                }
                case TECHNICIAN, VIEWER -> {
                    // Technicians and viewers can only manage themselves (already handled above)
                    logger.warn("Security violation: User {} of type {} cannot manage other users", 
                              currentUserEmail, currentUserType);
                    canManage = false;
                }
                default -> {
                    // Default case for any unexpected user types
                    logger.error("Unknown user type: {}", currentUserType);
                    canManage = false;
                }
            }
            
            if (canManage) {
                logger.info("Security check passed: User {} can manage user {}", 
                           currentUserEmail, targetUserId);
            }
            
            return canManage;
            
        } catch (Exception e) {
            logger.error("Error during security check for canManageUser({}, {}): {}", 
                        targetUserId, currentUserEmail, e.getMessage(), e);
            // Fail secure - deny access on any error
            return false;
        }
    }
    
    // ========== Inner Classes ==========
    
    /**
     * User statistics DTO.
     */
    public static class UserStatistics {
        public long totalUsers;
        public long activeUsers;
        public long suspendedUsers;
        public java.util.Map<UserType, Long> usersByType = new java.util.HashMap<>();
    }
}