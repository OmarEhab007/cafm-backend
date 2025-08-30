package com.cafm.cafmbackend.infrastructure.persistence.repository;

import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import com.cafm.cafmbackend.shared.enums.UserStatus;
import com.cafm.cafmbackend.shared.enums.UserType;
import com.cafm.cafmbackend.shared.enums.TechnicianSpecialization;
import com.cafm.cafmbackend.shared.enums.SkillLevel;
import com.cafm.cafmbackend.infrastructure.persistence.projection.UserListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity.
 * 
 * Architecture: Data access layer for user management
 * Pattern: Repository pattern with JPA Specifications
 * Java 23: Ready for virtual threads in query execution
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    
    // ========== Basic Queries ==========
    
    /**
     * Find user by email (case-insensitive)
     */
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByEmail(String email);
    /**
     * Find user by username (case-insensitive)
     */
    Optional<User> findByUsernameIgnoreCase(String username);
    
    /**
     * Find user by email or username
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:identifier) OR LOWER(u.username) = LOWER(:identifier)")
    Optional<User> findByEmailOrUsername(@Param("identifier") String identifier);
    
    /**
     * Find user by employee ID
     */
    Optional<User> findByEmployeeId(String employeeId);
    
    /**
     * Check if email exists
     */
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);
    
    /**
     * Find user by phone number
     */
    Optional<User> findByPhone(String phone);
    
    /**
     * Find user by Iqama ID
     */
    Optional<User> findByIqamaId(String iqamaId);
    
    /**
     * Check if username exists
     */
    boolean existsByUsernameIgnoreCase(String username);
    
    /**
     * Check if employee ID exists
     */
    boolean existsByEmployeeId(String employeeId);
    
    // ========== User Type Queries ==========
    
    /**
     * Find all users by type
     */
    List<User> findByUserType(UserType userType);
    
    /**
     * Find users by type and company with soft delete check
     */
    List<User> findByUserTypeAndCompany_IdAndDeletedAtIsNull(UserType userType, UUID companyId);
    
    /**
     * Find users by company with pagination and soft delete check
     */
    Page<User> findByCompany_IdAndDeletedAtIsNull(UUID companyId, Pageable pageable);
    
    /**
     * Find active users by type
     */
    @Query("SELECT u FROM User u WHERE u.userType = :type AND u.status = :status AND u.isActive = true AND u.deletedAt IS NULL")
    List<User> findActiveByUserType(@Param("type") UserType userType, @Param("status") UserStatus status);
    
    /**
     * Find all supervisors with pagination
     */
    @Query("SELECT u FROM User u WHERE u.userType = :userType AND u.deletedAt IS NULL")
    Page<User> findAllSupervisors(@Param("userType") UserType userType, Pageable pageable);
    
    /**
     * Find all admins
     */
    @Query("SELECT u FROM User u WHERE u.userType IN (:adminTypes) AND u.deletedAt IS NULL")
    List<User> findAllAdmins(@Param("adminTypes") List<UserType> adminTypes);
    
    // ========== Status Queries ==========
    
    /**
     * Find users by status
     */
    List<User> findByStatus(UserStatus status);
    
    /**
     * Find users pending verification
     */
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.deletedAt IS NULL")
    List<User> findPendingVerification(@Param("status") UserStatus status);
    
    /**
     * Find locked users
     */
    @Query("SELECT u FROM User u WHERE u.isLocked = true AND u.deletedAt IS NULL")
    List<User> findLockedUsers();
    
    // ========== Search Queries ==========
    
    /**
     * Search users by name, email, or username
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.deletedAt IS NULL 
        AND (
            LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
            LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
            LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
            LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
            LOWER(u.employeeId) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        )
        """)
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    /**
     * Full-text search using PostgreSQL
     */
    @Query(value = """
        SELECT * FROM users u 
        WHERE u.deleted_at IS NULL 
        AND u.search_vector @@ plainto_tsquery('english', :query)
        ORDER BY ts_rank(u.search_vector, plainto_tsquery('english', :query)) DESC
        """, nativeQuery = true)
    List<User> fullTextSearch(@Param("query") String query);
    
    // ========== Performance Metrics Queries ==========
    
    /**
     * Find top performers
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.userType = :userType 
        AND u.performanceRating IS NOT NULL 
        AND u.deletedAt IS NULL 
        ORDER BY u.performanceRating DESC, u.productivityScore DESC
        """)
    List<User> findTopPerformers(@Param("userType") UserType userType, Pageable pageable);
    
    /**
     * Find underperforming supervisors
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.userType = :userType 
        AND u.performanceRating < :threshold 
        AND u.deletedAt IS NULL
        """)
    List<User> findUnderperformers(@Param("userType") UserType userType, @Param("threshold") Double threshold);
    
    // ========== Soft Delete Queries ==========
    
    /**
     * Find soft-deleted users within recycle period
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.deletedAt IS NOT NULL 
        AND u.deletedAt > :thirtyDaysAgo
        """)
    List<User> findRecentlyDeleted(@Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);
    
    /**
     * Find users to be purged
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.deletedAt IS NOT NULL 
        AND u.deletedAt < :ninetyDaysAgo
        """)
    List<User> findUsersToPurge(@Param("ninetyDaysAgo") LocalDateTime ninetyDaysAgo);
    
    // ========== Projection Queries for Performance ==========
    
    /**
     * Find all users with projection
     */
    Page<UserListProjection> findAllProjectedBy(Pageable pageable);
    
    /**
     * Find users by company with projection
     */
    Page<UserListProjection> findByCompany_IdAndDeletedAtIsNull(UUID companyId, Pageable pageable, Class<UserListProjection> type);
    
    /**
     * Find users by status with projection
     */
    Page<UserListProjection> findByStatusAndDeletedAtIsNull(UserStatus status, Pageable pageable, Class<UserListProjection> type);
    
    /**
     * Find users by type with projection
     */
    Page<UserListProjection> findByUserTypeAndDeletedAtIsNull(UserType userType, Pageable pageable, Class<UserListProjection> type);
    
    // ========== Update Operations ==========
    
    /**
     * Update last login timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") UUID userId, @Param("loginTime") LocalDateTime loginTime);
    
    /**
     * Lock user account
     */
    @Modifying
    @Query("UPDATE User u SET u.isLocked = true, u.status = :status WHERE u.id = :userId")
    void lockUser(@Param("userId") UUID userId, @Param("status") UserStatus status);
    
    /**
     * Unlock user account
     */
    @Modifying
    @Query("UPDATE User u SET u.isLocked = false, u.status = :status WHERE u.id = :userId")
    void unlockUser(@Param("userId") UUID userId, @Param("status") UserStatus status);
    
    /**
     * Verify email
     */
    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true WHERE u.id = :userId")
    void verifyEmail(@Param("userId") UUID userId);
    
    /**
     * Update performance rating
     */
    @Modifying
    @Query("UPDATE User u SET u.performanceRating = :rating, u.productivityScore = :score WHERE u.id = :userId")
    void updatePerformanceMetrics(@Param("userId") UUID userId, 
                                  @Param("rating") Double rating, 
                                  @Param("score") Integer score);
    
    // ========== Technician Specialization Queries ==========
    
    /**
     * Find technicians by specialization
     */
    @Query("SELECT u FROM User u WHERE u.userType = :userType AND u.specialization = :specialization AND u.deletedAt IS NULL")
    List<User> findTechniciansBySpecialization(@Param("userType") UserType userType, @Param("specialization") TechnicianSpecialization specialization);
    
    /**
     * Find available technicians by specialization
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.userType = :userType 
        AND u.specialization = :specialization 
        AND u.status = :status 
        AND u.isActive = true 
        AND u.isLocked = false
        AND u.isAvailableForAssignment = true
        AND u.deletedAt IS NULL
        ORDER BY u.performanceRating DESC NULLS LAST, u.skillLevel DESC
        """)
    List<User> findAvailableTechniciansBySpecialization(@Param("userType") UserType userType, @Param("specialization") TechnicianSpecialization specialization, @Param("status") UserStatus status);
    
    /**
     * Find technicians by skill level
     */
    @Query("SELECT u FROM User u WHERE u.userType = :userType AND u.skillLevel = :skillLevel AND u.deletedAt IS NULL")
    List<User> findTechniciansBySkillLevel(@Param("userType") UserType userType, @Param("skillLevel") SkillLevel skillLevel);
    
    /**
     * Find technicians with minimum skill level
     */
    @Query(value = """
        SELECT * FROM users u 
        WHERE u.user_type = 'TECHNICIAN' 
        AND u.skill_level IS NOT NULL
        AND u.deleted_at IS NULL
        AND (
            (u.skill_level = 'MASTER' AND :minLevel IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT', 'MASTER')) OR
            (u.skill_level = 'EXPERT' AND :minLevel IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT')) OR
            (u.skill_level = 'ADVANCED' AND :minLevel IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')) OR
            (u.skill_level = 'INTERMEDIATE' AND :minLevel IN ('BEGINNER', 'INTERMEDIATE')) OR
            (u.skill_level = 'BEGINNER' AND :minLevel = 'BEGINNER')
        )
        ORDER BY 
            CASE u.skill_level
                WHEN 'MASTER' THEN 5
                WHEN 'EXPERT' THEN 4
                WHEN 'ADVANCED' THEN 3
                WHEN 'INTERMEDIATE' THEN 2
                WHEN 'BEGINNER' THEN 1
            END DESC
        """, nativeQuery = true)
    List<User> findTechniciansWithMinimumSkillLevel(@Param("minLevel") String minLevel);
    
    /**
     * Find technicians by specialization and minimum skill level
     */
    @Query(value = """
        SELECT * FROM users u 
        WHERE u.user_type = 'TECHNICIAN' 
        AND u.specialization = :specialization
        AND u.skill_level IS NOT NULL
        AND u.status = 'ACTIVE'
        AND u.is_active = true
        AND u.is_locked = false
        AND u.deleted_at IS NULL
        AND (
            (u.skill_level = 'MASTER' AND :minLevel IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT', 'MASTER')) OR
            (u.skill_level = 'EXPERT' AND :minLevel IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT')) OR
            (u.skill_level = 'ADVANCED' AND :minLevel IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')) OR
            (u.skill_level = 'INTERMEDIATE' AND :minLevel IN ('BEGINNER', 'INTERMEDIATE')) OR
            (u.skill_level = 'BEGINNER' AND :minLevel = 'BEGINNER')
        )
        ORDER BY u.performance_rating DESC NULLS LAST
        """, nativeQuery = true)
    List<User> findQualifiedTechnicians(@Param("specialization") String specialization, @Param("minLevel") String minLevel);
    
    /**
     * Find unassigned technicians
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.userType = :userType 
        AND u.status = :status
        AND u.isActive = true
        AND u.isAvailableForAssignment = true
        AND u.deletedAt IS NULL
        AND NOT EXISTS (
            SELECT 1 FROM SupervisorTechnician st 
            WHERE st.technician.id = u.id 
            AND st.isActive = true
        )
        """)
    List<User> findUnassignedTechnicians(@Param("userType") UserType userType, @Param("status") UserStatus status);
    
    /**
     * Find technicians assigned to a supervisor
     */
    @Query("""
        SELECT u FROM User u 
        JOIN SupervisorTechnician st ON u.id = st.technician.id
        WHERE st.supervisor.id = :supervisorId 
        AND st.isActive = true
        AND u.deletedAt IS NULL
        """)
    List<User> findTechniciansBySupervisor(@Param("supervisorId") UUID supervisorId);
    
    /**
     * Find technicians by experience range
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.userType = :userType 
        AND u.yearsOfExperience BETWEEN :minYears AND :maxYears
        AND u.deletedAt IS NULL
        ORDER BY u.yearsOfExperience DESC
        """)
    List<User> findTechniciansByExperience(@Param("userType") UserType userType, @Param("minYears") Integer minYears, @Param("maxYears") Integer maxYears);
    
    /**
     * Find technicians by hourly rate range
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.userType = :userType 
        AND u.hourlyRate BETWEEN :minRate AND :maxRate
        AND u.deletedAt IS NULL
        ORDER BY u.hourlyRate ASC
        """)
    List<User> findTechniciansByHourlyRate(@Param("userType") UserType userType, @Param("minRate") Double minRate, @Param("maxRate") Double maxRate);
    
    /**
     * Find top performing technicians by specialization
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.userType = :userType 
        AND u.specialization = :specialization
        AND u.performanceRating IS NOT NULL
        AND u.deletedAt IS NULL
        ORDER BY u.performanceRating DESC, u.productivityScore DESC
        """)
    List<User> findTopTechniciansBySpecialization(@Param("userType") UserType userType, @Param("specialization") TechnicianSpecialization specialization, Pageable pageable);
    
    // ========== Technician Availability Updates ==========
    
    /**
     * Set technician availability status
     */
    @Modifying
    @Query("UPDATE User u SET u.isAvailableForAssignment = :available WHERE u.id = :technicianId")
    void setTechnicianAvailability(@Param("technicianId") UUID technicianId, @Param("available") Boolean available);
    
    /**
     * Update technician specialization and skill level
     */
    @Modifying
    @Query("""
        UPDATE User u 
        SET u.specialization = :specialization, 
            u.skillLevel = :skillLevel 
        WHERE u.id = :technicianId
        """)
    void updateTechnicianSkills(@Param("technicianId") UUID technicianId, 
                               @Param("specialization") TechnicianSpecialization specialization,
                               @Param("skillLevel") SkillLevel skillLevel);
    
    // ========== Statistics Queries ==========
    
    /**
     * Count users by type
     */
    @Query("SELECT u.userType, COUNT(u) FROM User u WHERE u.deletedAt IS NULL GROUP BY u.userType")
    List<Object[]> countByUserType();
    
    /**
     * Count users by status
     */
    @Query("SELECT u.status, COUNT(u) FROM User u WHERE u.deletedAt IS NULL GROUP BY u.status")
    List<Object[]> countByStatus();
    
    /**
     * Get user statistics
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_users,
            COUNT(CASE WHEN is_active = true THEN 1 END) as active_users,
            COUNT(CASE WHEN is_locked = true THEN 1 END) as locked_users,
            COUNT(CASE WHEN email_verified = true THEN 1 END) as verified_users,
            COUNT(CASE WHEN deleted_at IS NOT NULL THEN 1 END) as deleted_users
        FROM users
        """, nativeQuery = true)
    Object getUserStatistics();
    
    /**
     * Count technicians by specialization
     */
    @Query("SELECT u.specialization, COUNT(u) FROM User u WHERE u.userType = :userType AND u.specialization IS NOT NULL AND u.deletedAt IS NULL GROUP BY u.specialization")
    List<Object[]> countTechniciansBySpecialization(@Param("userType") UserType userType);
    
    /**
     * Count technicians by skill level
     */
    @Query("SELECT u.skillLevel, COUNT(u) FROM User u WHERE u.userType = :userType AND u.skillLevel IS NOT NULL AND u.deletedAt IS NULL GROUP BY u.skillLevel")
    List<Object[]> countTechniciansBySkillLevel(@Param("userType") UserType userType);
    
    /**
     * Get technician statistics
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_technicians,
            COUNT(CASE WHEN is_available_for_assignment = true THEN 1 END) as available_technicians,
            COUNT(CASE WHEN specialization IS NOT NULL THEN 1 END) as technicians_with_specialization,
            COUNT(CASE WHEN skill_level IS NOT NULL THEN 1 END) as technicians_with_skill_level,
            AVG(years_of_experience) as avg_experience_years,
            AVG(hourly_rate) as avg_hourly_rate,
            AVG(performance_rating) as avg_performance_rating
        FROM users 
        WHERE user_type = 'TECHNICIAN' AND deleted_at IS NULL
        """, nativeQuery = true)
    Object getTechnicianStatistics();
    
    // ========== Count Queries ==========
    
    long countByCompany_IdAndDeletedAtIsNull(UUID companyId);
    
    long countByCompany_IdAndStatusAndDeletedAtIsNull(UUID companyId, UserStatus status);
    
    long countByCompany_IdAndUserTypeAndDeletedAtIsNull(UUID companyId, UserType userType);

    // ========== Tenant-Aware Queries ==========
    
    /**
     * Find all users by company ID with soft delete check
     */
    List<User> findAllByCompany_Id(UUID companyId);
    
    /**
     * Find all users by company ID with pagination and soft delete check
     */
    Page<User> findAllByCompany_IdAndDeletedAtIsNull(UUID companyId, Pageable pageable);
    
    /**
     * Find user by ID and company ID with soft delete check (tenant-aware)
     */
    Optional<User> findByIdAndCompany_Id(UUID id, UUID companyId);
    
    /**
     * Find user by email and company ID (tenant-aware)
     */
    Optional<User> findByEmailAndCompany_Id(String email, UUID companyId);
    
    /**
     * Check if user exists by ID and company ID (tenant-aware)
     */
    boolean existsByIdAndCompany_Id(UUID id, UUID companyId);
    
    /**
     * Check if email exists within company (tenant-aware)
     */
    boolean existsByEmailAndCompany_Id(String email, UUID companyId);
    
    /**
     * Get user statistics by company ID
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_users,
            COUNT(CASE WHEN is_active = true THEN 1 END) as active_users,
            COUNT(CASE WHEN is_locked = true THEN 1 END) as locked_users,
            COUNT(CASE WHEN email_verified = true THEN 1 END) as verified_users,
            COUNT(CASE WHEN user_type = 'ADMIN' THEN 1 END) as admin_users,
            COUNT(CASE WHEN user_type = 'SUPERVISOR' THEN 1 END) as supervisor_users,
            COUNT(CASE WHEN user_type = 'TECHNICIAN' THEN 1 END) as technician_users
        FROM users
        WHERE company_id = :companyId AND deleted_at IS NULL
        """, nativeQuery = true)
    Object getStatsByCompanyId(@Param("companyId") UUID companyId);
    
    /**
     * Delete user by ID and company ID (tenant-aware soft delete)
     */
    @Modifying
    @Query("UPDATE User u SET u.deletedAt = :deletedAt WHERE u.id = :id AND u.company.id = :companyId")
    int deleteByIdAndCompanyId(@Param("id") UUID id, @Param("companyId") UUID companyId, @Param("deletedAt") LocalDateTime deletedAt);
    
    /**
     * Count users by company ID (simple count method for tests)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.company.id = :companyId AND u.deletedAt IS NULL")
    long countByCompanyId(@Param("companyId") UUID companyId);
    
    /**
     * Find users by company ID and user type (simple method for tests)
     */
    @Query("SELECT u FROM User u WHERE u.company.id = :companyId AND u.userType = :userType AND u.deletedAt IS NULL")
    List<User> findByCompanyIdAndUserType(@Param("companyId") UUID companyId, @Param("userType") UserType userType);

}