package com.cafm.cafmbackend.infrastructure.persistence.repository;

import com.cafm.cafmbackend.infrastructure.persistence.entity.Company;
import com.cafm.cafmbackend.shared.enums.CompanyStatus;
import com.cafm.cafmbackend.shared.enums.SubscriptionPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Company entity - multi-tenant foundation.
 * 
 * Architecture: Data access layer for company/tenant management
 * Pattern: Repository pattern with tenant-aware operations
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID>, JpaSpecificationExecutor<Company> {
    
    // ========== Basic Tenant Queries ==========
    
    /**
     * Find company by domain
     */
    Optional<Company> findByDomainAndDeletedAtIsNull(String domain);
    
    /**
     * Find company by subdomain
     */
    Optional<Company> findBySubdomainAndDeletedAtIsNull(String subdomain);
    
    /**
     * Find all active companies
     */
    @Query("""
        SELECT c FROM Company c 
        WHERE c.status = com.cafm.cafmbackend.shared.enums.CompanyStatus.ACTIVE 
        AND c.isActive = true 
        AND c.deletedAt IS NULL
        ORDER BY c.createdAt DESC
        """)
    List<Company> findActiveCompanies();
    
    /**
     * Find companies by status
     */
    List<Company> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(CompanyStatus status);
    
    /**
     * Find companies by subscription plan
     */
    List<Company> findBySubscriptionPlanAndDeletedAtIsNullOrderByCreatedAtDesc(SubscriptionPlan plan);
    
    /**
     * Check if domain is available
     */
    boolean existsByDomainAndDeletedAtIsNull(String domain);
    
    /**
     * Check if subdomain is available
     */
    boolean existsBySubdomainAndDeletedAtIsNull(String subdomain);
    
    // ========== Subscription Management ==========
    
    /**
     * Find companies with expiring subscriptions
     */
    @Query("""
        SELECT c FROM Company c 
        WHERE c.subscriptionEndDate BETWEEN :startDate AND :endDate
        AND c.deletedAt IS NULL
        ORDER BY c.subscriptionEndDate ASC
        """)
    List<Company> findCompaniesWithExpiringSubscriptions(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find companies with expired subscriptions
     */
    @Query("""
        SELECT c FROM Company c 
        WHERE c.subscriptionEndDate < :currentDate
        AND c.status = com.cafm.cafmbackend.shared.enums.CompanyStatus.ACTIVE
        AND c.deletedAt IS NULL
        ORDER BY c.subscriptionEndDate ASC
        """)
    List<Company> findCompaniesWithExpiredSubscriptions(@Param("currentDate") LocalDate currentDate);
    
    /**
     * Find companies in trial status
     */
    @Query("""
        SELECT c FROM Company c 
        WHERE c.status = com.cafm.cafmbackend.shared.enums.CompanyStatus.TRIAL
        AND c.deletedAt IS NULL
        ORDER BY c.subscriptionEndDate ASC NULLS FIRST
        """)
    List<Company> findTrialCompanies();
    
    /**
     * Find companies by subscription plan and status
     */
    List<Company> findBySubscriptionPlanAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
        SubscriptionPlan plan, CompanyStatus status);
    
    // ========== Resource Usage Queries ==========
    
    /**
     * Find companies approaching user limits
     */
    @Query(value = """
        SELECT c.*, 
               COALESCE(user_counts.user_count, 0) as current_users,
               (COALESCE(user_counts.user_count, 0)::FLOAT / c.max_users::FLOAT * 100) as usage_percentage
        FROM companies c
        LEFT JOIN (
            SELECT company_id, COUNT(*) as user_count 
            FROM users 
            WHERE deleted_at IS NULL 
            GROUP BY company_id
        ) user_counts ON c.id = user_counts.company_id
        WHERE c.deleted_at IS NULL
        AND c.is_active = true
        AND (COALESCE(user_counts.user_count, 0)::FLOAT / c.max_users::FLOAT) >= :usageThreshold
        ORDER BY usage_percentage DESC
        """, nativeQuery = true)
    List<Company> findCompaniesApproachingUserLimits(@Param("usageThreshold") double usageThreshold);
    
    /**
     * Find companies approaching school limits
     */
    @Query(value = """
        SELECT c.*, 
               COALESCE(school_counts.school_count, 0) as current_schools,
               (COALESCE(school_counts.school_count, 0)::FLOAT / c.max_schools::FLOAT * 100) as usage_percentage
        FROM companies c
        LEFT JOIN (
            SELECT company_id, COUNT(*) as school_count 
            FROM schools 
            WHERE deleted_at IS NULL 
            GROUP BY company_id
        ) school_counts ON c.id = school_counts.company_id
        WHERE c.deleted_at IS NULL
        AND c.is_active = true
        AND (COALESCE(school_counts.school_count, 0)::FLOAT / c.max_schools::FLOAT) >= :usageThreshold
        ORDER BY usage_percentage DESC
        """, nativeQuery = true)
    List<Company> findCompaniesApproachingSchoolLimits(@Param("usageThreshold") double usageThreshold);
    
    // ========== Admin Operations ==========
    
    /**
     * Update company status
     */
    @Modifying
    @Query("""
        UPDATE Company c 
        SET c.status = :status, 
            c.isActive = :isActive 
        WHERE c.id = :companyId
        """)
    void updateCompanyStatus(@Param("companyId") UUID companyId, 
                           @Param("status") CompanyStatus status, 
                           @Param("isActive") Boolean isActive);
    
    /**
     * Update subscription plan
     */
    @Modifying
    @Query("""
        UPDATE Company c 
        SET c.subscriptionPlan = :plan,
            c.subscriptionStartDate = :startDate,
            c.subscriptionEndDate = :endDate,
            c.maxUsers = :maxUsers,
            c.maxSchools = :maxSchools,
            c.maxSupervisors = :maxSupervisors,
            c.maxTechnicians = :maxTechnicians,
            c.maxStorageGb = :maxStorageGb
        WHERE c.id = :companyId
        """)
    void updateSubscriptionPlan(@Param("companyId") UUID companyId,
                              @Param("plan") SubscriptionPlan plan,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              @Param("maxUsers") Integer maxUsers,
                              @Param("maxSchools") Integer maxSchools,
                              @Param("maxSupervisors") Integer maxSupervisors,
                              @Param("maxTechnicians") Integer maxTechnicians,
                              @Param("maxStorageGb") Integer maxStorageGb);
    
    /**
     * Soft delete company and all related data
     */
    @Modifying
    @Query("""
        UPDATE Company c 
        SET c.deletedAt = CURRENT_TIMESTAMP,
            c.deletedBy = :deletedBy,
            c.isActive = false,
            c.status = com.cafm.cafmbackend.shared.enums.CompanyStatus.INACTIVE
        WHERE c.id = :companyId
        """)
    void softDeleteCompany(@Param("companyId") UUID companyId, @Param("deletedBy") UUID deletedBy);
    
    // ========== Search and Filtering ==========
    
    /**
     * Search companies by name or domain
     */
    @Query("""
        SELECT c FROM Company c 
        WHERE (LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(c.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(c.domain) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        AND c.deletedAt IS NULL
        ORDER BY c.name ASC
        """)
    Page<Company> searchCompanies(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    /**
     * Find companies by country
     */
    List<Company> findByCountryAndDeletedAtIsNullOrderByNameAsc(String country);
    
    /**
     * Find companies by industry
     */
    List<Company> findByIndustryAndDeletedAtIsNullOrderByNameAsc(String industry);
    
    /**
     * Find recently created companies
     */
    @Query("""
        SELECT c FROM Company c 
        WHERE c.createdAt >= :sinceDate
        AND c.deletedAt IS NULL
        ORDER BY c.createdAt DESC
        """)
    List<Company> findRecentlyCreatedCompanies(@Param("sinceDate") LocalDate sinceDate);
    
    // ========== Statistics Queries ==========
    
    /**
     * Count companies by status
     */
    @Query("""
        SELECT c.status, COUNT(c) 
        FROM Company c 
        WHERE c.deletedAt IS NULL
        GROUP BY c.status 
        ORDER BY COUNT(c) DESC
        """)
    List<Object[]> countCompaniesByStatus();
    
    /**
     * Count companies by subscription plan
     */
    @Query("""
        SELECT c.subscriptionPlan, COUNT(c) 
        FROM Company c 
        WHERE c.deletedAt IS NULL
        GROUP BY c.subscriptionPlan 
        ORDER BY COUNT(c) DESC
        """)
    List<Object[]> countCompaniesBySubscriptionPlan();
    
    /**
     * Get company statistics
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_companies,
            COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active_companies,
            COUNT(CASE WHEN status = 'TRIAL' THEN 1 END) as trial_companies,
            COUNT(CASE WHEN status = 'SUSPENDED' THEN 1 END) as suspended_companies,
            COUNT(CASE WHEN subscription_plan = 'FREE' THEN 1 END) as free_companies,
            COUNT(CASE WHEN subscription_plan IN ('BASIC', 'PROFESSIONAL', 'ENTERPRISE') THEN 1 END) as paid_companies,
            AVG(max_users) as avg_user_limit,
            AVG(max_schools) as avg_school_limit
        FROM companies
        WHERE deleted_at IS NULL
        """, nativeQuery = true)
    Object getCompanyStatistics();
    
    /**
     * Get monthly company registrations
     */
    @Query(value = """
        SELECT 
            DATE_TRUNC('month', created_at) as month,
            COUNT(*) as registrations
        FROM companies 
        WHERE deleted_at IS NULL
        AND created_at >= :startDate
        GROUP BY DATE_TRUNC('month', created_at)
        ORDER BY month DESC
        """, nativeQuery = true)
    List<Object[]> getMonthlyRegistrations(@Param("startDate") LocalDate startDate);
    
    /**
     * Find top companies by user count
     */
    @Query(value = """
        SELECT c.*, 
               COALESCE(user_counts.user_count, 0) as current_users
        FROM companies c
        LEFT JOIN (
            SELECT company_id, COUNT(*) as user_count 
            FROM users 
            WHERE deleted_at IS NULL 
            GROUP BY company_id
        ) user_counts ON c.id = user_counts.company_id
        WHERE c.deleted_at IS NULL
        AND c.is_active = true
        ORDER BY current_users DESC
        """, nativeQuery = true)
    List<Company> findTopCompaniesByUserCount(Pageable pageable);
    
    /**
     * Check if company has available capacity
     */
    @Query(value = """
        SELECT 
            c.id,
            c.max_users - COALESCE(u.user_count, 0) as available_user_slots,
            c.max_schools - COALESCE(s.school_count, 0) as available_school_slots
        FROM companies c
        LEFT JOIN (
            SELECT company_id, COUNT(*) as user_count 
            FROM users 
            WHERE deleted_at IS NULL 
            GROUP BY company_id
        ) u ON c.id = u.company_id
        LEFT JOIN (
            SELECT company_id, COUNT(*) as school_count 
            FROM schools 
            WHERE deleted_at IS NULL 
            GROUP BY company_id
        ) s ON c.id = s.company_id
        WHERE c.id = :companyId
        AND c.deleted_at IS NULL
        """, nativeQuery = true)
    Object getCompanyCapacity(@Param("companyId") UUID companyId);
}