package com.cafm.cafmbackend.infrastructure.persistence.repository;

import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import com.cafm.cafmbackend.shared.enums.UserStatus;
import com.cafm.cafmbackend.shared.enums.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SECURITY ENHANCEMENT: Tenant-aware User Repository
 * 
 * Purpose: Provides tenant-safe user operations preventing cross-tenant access
 * Pattern: Repository pattern with mandatory tenant isolation
 * Java 23: Enhanced with modern query patterns and security validation
 * Architecture: Security-first data access layer for user management
 * Standards: All methods require tenant context to prevent data leakage
 * 
 * CRITICAL SECURITY FEATURES:
 * - All user lookup methods require companyId parameter
 * - Prevents accidental cross-tenant user access
 * - Replaces dangerous non-tenant-aware methods
 * - Includes validation for proper tenant isolation
 */
@Repository
public interface TenantAwareUserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    
    // ========== SECURE USER LOOKUP METHODS ==========
    
    /**
     * SECURITY: Find user by email with mandatory tenant isolation (case-insensitive)
     * Purpose: Prevents cross-tenant user access via email lookup
     * Pattern: Always includes company filter for tenant isolation
     */
    @Query("""
        SELECT u FROM User u 
        WHERE LOWER(u.email) = LOWER(:email) 
        AND u.company.id = :companyId 
        AND u.deletedAt IS NULL
        """)
    Optional<User> findByEmailIgnoreCaseAndCompanyId(@Param("email") String email, @Param("companyId") UUID companyId);
    
    /**
     * SECURITY: Find user by username with mandatory tenant isolation (case-insensitive)
     */
    @Query("""
        SELECT u FROM User u 
        WHERE LOWER(u.username) = LOWER(:username) 
        AND u.company.id = :companyId 
        AND u.deletedAt IS NULL
        """)
    Optional<User> findByUsernameIgnoreCaseAndCompanyId(@Param("username") String username, @Param("companyId") UUID companyId);
    
    /**
     * SECURITY: Find user by email OR username with tenant isolation
     * Critical for authentication - prevents cross-tenant login attempts
     */
    @Query("""
        SELECT u FROM User u 
        WHERE (LOWER(u.email) = LOWER(:identifier) OR LOWER(u.username) = LOWER(:identifier)) 
        AND u.company.id = :companyId 
        AND u.deletedAt IS NULL
        """)
    Optional<User> findByEmailOrUsernameAndCompanyId(@Param("identifier") String identifier, @Param("companyId") UUID companyId);
    
    /**
     * SECURITY: Find user by employee ID with tenant isolation
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.employeeId = :employeeId 
        AND u.company.id = :companyId 
        AND u.deletedAt IS NULL
        """)
    Optional<User> findByEmployeeIdAndCompanyId(@Param("employeeId") String employeeId, @Param("companyId") UUID companyId);
    
    // ========== TENANT-SAFE EXISTENCE CHECKS ==========
    
    /**
     * SECURITY: Check if email exists within tenant scope only
     */
    @Query("""
        SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END 
        FROM User u 
        WHERE LOWER(u.email) = LOWER(:email) 
        AND u.company.id = :companyId 
        AND u.deletedAt IS NULL
        """)
    boolean existsByEmailIgnoreCaseAndCompanyId(@Param("email") String email, @Param("companyId") UUID companyId);
    
    /**
     * SECURITY: Check if username exists within tenant scope only
     */
    @Query("""
        SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END 
        FROM User u 
        WHERE LOWER(u.username) = LOWER(:username) 
        AND u.company.id = :companyId 
        AND u.deletedAt IS NULL
        """)
    boolean existsByUsernameIgnoreCaseAndCompanyId(@Param("username") String username, @Param("companyId") UUID companyId);
    
    /**
     * SECURITY: Check if employee ID exists within tenant scope only
     */
    @Query("""
        SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END 
        FROM User u 
        WHERE u.employeeId = :employeeId 
        AND u.company.id = :companyId 
        AND u.deletedAt IS NULL
        """)
    boolean existsByEmployeeIdAndCompanyId(@Param("employeeId") String employeeId, @Param("companyId") UUID companyId);
    
    // ========== TENANT-SAFE USER TYPE QUERIES ==========
    
    /**
     * Find users by type within tenant boundary
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.userType = :userType 
        AND u.company.id = :companyId 
        AND u.deletedAt IS NULL
        """)
    List<User> findByUserTypeAndCompanyId(@Param("userType") UserType userType, @Param("companyId") UUID companyId);
    
    /**
     * Find active users by type within tenant boundary
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.userType = :userType 
        AND u.status = :status 
        AND u.isActive = true 
        AND u.company.id = :companyId 
        AND u.deletedAt IS NULL
        """)
    List<User> findActiveByUserTypeAndCompanyId(@Param("userType") UserType userType, @Param("status") UserStatus status, @Param("companyId") UUID companyId);
    
    // ========== VALIDATION METHODS ==========
    
    /**
     * Validate that a user ID belongs to the specified tenant
     * Critical security method for preventing cross-tenant access
     */
    @Query("""
        SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END 
        FROM User u 
        WHERE u.id = :userId 
        AND u.company.id = :companyId 
        AND u.deletedAt IS NULL
        """)
    boolean validateUserBelongsToTenant(@Param("userId") UUID userId, @Param("companyId") UUID companyId);
    
    /**
     * Validate multiple user IDs belong to the specified tenant
     * Bulk validation for batch operations
     */
    @Query("""
        SELECT COUNT(u) = :expectedCount
        FROM User u 
        WHERE u.id IN :userIds 
        AND u.company.id = :companyId 
        AND u.deletedAt IS NULL
        """)
    boolean validateAllUsersBelongToTenant(@Param("userIds") List<UUID> userIds, 
                                          @Param("companyId") UUID companyId, 
                                          @Param("expectedCount") long expectedCount);
    
    // ========== TENANT STATISTICS ==========
    
    /**
     * Count users by status within tenant
     */
    @Query("""
        SELECT COUNT(u) FROM User u 
        WHERE u.status = :status 
        AND u.company.id = :companyId 
        AND u.deletedAt IS NULL
        """)
    long countByStatusAndCompanyId(@Param("status") UserStatus status, @Param("companyId") UUID companyId);
    
    /**
     * Count users by type within tenant
     */
    @Query("""
        SELECT COUNT(u) FROM User u 
        WHERE u.userType = :userType 
        AND u.company.id = :companyId 
        AND u.deletedAt IS NULL
        """)
    long countByUserTypeAndCompanyId(@Param("userType") UserType userType, @Param("companyId") UUID companyId);
}