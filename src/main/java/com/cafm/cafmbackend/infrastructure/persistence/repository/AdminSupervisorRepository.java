package com.cafm.cafmbackend.infrastructure.persistence.repository;

import com.cafm.cafmbackend.infrastructure.persistence.entity.AdminSupervisor;
import com.cafm.cafmbackend.shared.enums.UserStatus;
import com.cafm.cafmbackend.shared.enums.UserType;
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
 * Repository interface for AdminSupervisor entity.
 * 
 * Architecture: Data access layer for admin-supervisor management relationships
 * Pattern: Repository pattern with hierarchical management support
 */
@Repository
public interface AdminSupervisorRepository extends JpaRepository<AdminSupervisor, UUID>, JpaSpecificationExecutor<AdminSupervisor> {
    
    // ========== Basic Assignment Queries ==========
    
    /**
     * Find active primary admin assignment for a supervisor
     */
    @Query("""
        SELECT aso FROM AdminSupervisor aso 
        WHERE aso.supervisor.id = :supervisorId 
        AND aso.isActive = true
        AND aso.isPrimaryAdmin = true
        """)
    Optional<AdminSupervisor> findPrimaryAdminAssignmentBySupervisor(@Param("supervisorId") UUID supervisorId);
    
    /**
     * Find all active assignments for a supervisor
     */
    @Query("""
        SELECT aso FROM AdminSupervisor aso 
        WHERE aso.supervisor.id = :supervisorId 
        AND aso.isActive = true
        ORDER BY aso.isPrimaryAdmin DESC, aso.assignedDate DESC
        """)
    List<AdminSupervisor> findActiveAssignmentsBySupervisor(@Param("supervisorId") UUID supervisorId);
    
    /**
     * Find all supervisors managed by an admin
     */
    @Query("""
        SELECT aso FROM AdminSupervisor aso 
        WHERE aso.admin.id = :adminId 
        AND aso.isActive = true
        ORDER BY aso.authorityLevel ASC, aso.assignedDate DESC
        """)
    List<AdminSupervisor> findSupervisorsByAdmin(@Param("adminId") UUID adminId);
    
    /**
     * Find assignment history for a supervisor
     */
    @Query("""
        SELECT aso FROM AdminSupervisor aso 
        WHERE aso.supervisor.id = :supervisorId 
        ORDER BY aso.assignedDate DESC
        """)
    List<AdminSupervisor> findAssignmentHistoryBySupervisor(@Param("supervisorId") UUID supervisorId);
    
    /**
     * Check if supervisor is already assigned to an admin
     */
    @Query("""
        SELECT CASE WHEN COUNT(aso) > 0 THEN true ELSE false END 
        FROM AdminSupervisor aso 
        WHERE aso.supervisor.id = :supervisorId 
        AND aso.admin.id = :adminId 
        AND aso.isActive = true
        """)
    boolean existsActiveAssignment(@Param("adminId") UUID adminId, @Param("supervisorId") UUID supervisorId);
    
    /**
     * Check if supervisor has any active assignment
     */
    boolean existsBySupervisorIdAndIsActiveTrue(UUID supervisorId);
    
    // ========== Region-based Queries ==========
    
    /**
     * Find supervisors by admin and region
     */
    @Query("""
        SELECT aso FROM AdminSupervisor aso 
        WHERE aso.admin.id = :adminId 
        AND aso.region = :region
        AND aso.isActive = true
        ORDER BY aso.authorityLevel ASC
        """)
    List<AdminSupervisor> findSupervisorsByAdminAndRegion(
        @Param("adminId") UUID adminId, 
        @Param("region") String region);
    
    /**
     * Find assignments by region
     */
    @Query("""
        SELECT aso FROM AdminSupervisor aso 
        WHERE aso.region = :region
        AND aso.isActive = true
        ORDER BY aso.authorityLevel ASC, aso.totalSchoolsCovered DESC
        """)
    List<AdminSupervisor> findAssignmentsByRegion(@Param("region") String region);
    
    /**
     * Get all active regions
     */
    @Query("""
        SELECT DISTINCT aso.region 
        FROM AdminSupervisor aso 
        WHERE aso.region IS NOT NULL 
        AND aso.isActive = true
        ORDER BY aso.region
        """)
    List<String> findActiveRegions();
    
    // ========== Authority Level Queries ==========
    
    /**
     * Find supervisors by authority level
     */
    @Query("""
        SELECT aso FROM AdminSupervisor aso 
        WHERE aso.authorityLevel = :level
        AND aso.isActive = true
        ORDER BY aso.totalSchoolsCovered DESC
        """)
    List<AdminSupervisor> findByAuthorityLevel(@Param("level") Integer level);
    
    /**
     * Find high authority assignments (level 1-2)
     */
    @Query("""
        SELECT aso FROM AdminSupervisor aso 
        WHERE aso.authorityLevel <= 2
        AND aso.isActive = true
        ORDER BY aso.authorityLevel ASC, aso.efficiencyRating DESC NULLS LAST
        """)
    List<AdminSupervisor> findHighAuthorityAssignments();
    
    /**
     * Find supervisors with maximum authority in a region
     */
    @Query(value = """
        SELECT * FROM admin_supervisors aso
        WHERE aso.region = :region
        AND aso.is_active = true
        AND aso.authority_level = (
            SELECT MIN(authority_level)
            FROM admin_supervisors
            WHERE region = :region AND is_active = true
        )
        ORDER BY total_schools_covered DESC
        """, nativeQuery = true)
    List<AdminSupervisor> findMaxAuthorityInRegion(@Param("region") String region);
    
    // ========== Capacity and Performance Queries ==========
    
    /**
     * Find supervisors at or near capacity
     */
    @Query(value = """
        SELECT * FROM admin_supervisors aso
        WHERE aso.is_active = true 
        AND aso.total_schools_covered >= (aso.max_schools_oversight * :capacityThreshold / 100.0)
        ORDER BY (aso.total_schools_covered::FLOAT / aso.max_schools_oversight::FLOAT) DESC
        """, nativeQuery = true)
    List<AdminSupervisor> findSupervisorsNearCapacity(@Param("capacityThreshold") double capacityThreshold);
    
    /**
     * Find supervisors with available capacity
     */
    @Query(value = """
        SELECT * FROM admin_supervisors aso
        WHERE aso.is_active = true 
        AND aso.total_schools_covered < aso.max_schools_oversight
        ORDER BY (aso.max_schools_oversight - aso.total_schools_covered) DESC
        """, nativeQuery = true)
    List<AdminSupervisor> findSupervisorsWithCapacity();
    
    /**
     * Find top performing assignments by efficiency
     */
    @Query("""
        SELECT aso FROM AdminSupervisor aso 
        WHERE aso.isActive = true 
        AND aso.efficiencyRating IS NOT NULL
        ORDER BY aso.efficiencyRating DESC
        """)
    List<AdminSupervisor> findTopPerformingAssignments(Pageable pageable);
    
    /**
     * Find underperforming assignments
     */
    @Query("""
        SELECT aso FROM AdminSupervisor aso 
        WHERE aso.isActive = true 
        AND aso.efficiencyRating IS NOT NULL
        AND aso.efficiencyRating < :threshold
        ORDER BY aso.efficiencyRating ASC
        """)
    List<AdminSupervisor> findUnderperformingAssignments(@Param("threshold") Double threshold);
    
    // ========== Assignment Management Operations ==========
    
    /**
     * End assignment (set inactive and end date)
     */
    @Modifying
    @Query("""
        UPDATE AdminSupervisor aso 
        SET aso.isActive = false, 
            aso.endDate = :endDate 
        WHERE aso.id = :assignmentId
        """)
    void endAssignment(@Param("assignmentId") UUID assignmentId, @Param("endDate") LocalDate endDate);
    
    /**
     * End all active assignments for a supervisor
     */
    @Modifying
    @Query("""
        UPDATE AdminSupervisor aso 
        SET aso.isActive = false, 
            aso.endDate = :endDate 
        WHERE aso.supervisor.id = :supervisorId 
        AND aso.isActive = true
        """)
    void endAllAssignmentsForSupervisor(@Param("supervisorId") UUID supervisorId, @Param("endDate") LocalDate endDate);
    
    /**
     * Update authority level for assignment
     */
    @Modifying
    @Query("""
        UPDATE AdminSupervisor aso 
        SET aso.authorityLevel = :level 
        WHERE aso.id = :assignmentId
        """)
    void updateAuthorityLevel(@Param("assignmentId") UUID assignmentId, @Param("level") Integer level);
    
    /**
     * Update coverage statistics
     */
    @Modifying
    @Query("""
        UPDATE AdminSupervisor aso 
        SET aso.totalSchoolsCovered = :schools,
            aso.techniciansCovered = :technicians
        WHERE aso.id = :assignmentId
        """)
    void updateCoverageStatistics(@Param("assignmentId") UUID assignmentId, 
                                 @Param("schools") Integer schools,
                                 @Param("technicians") Integer technicians);
    
    /**
     * Update efficiency rating
     */
    @Modifying
    @Query("""
        UPDATE AdminSupervisor aso 
        SET aso.efficiencyRating = :rating 
        WHERE aso.id = :assignmentId
        """)
    void updateEfficiencyRating(@Param("assignmentId") UUID assignmentId, @Param("rating") Double rating);
    
    /**
     * Set primary admin status
     */
    @Modifying
    @Query("""
        UPDATE AdminSupervisor aso 
        SET aso.isPrimaryAdmin = :isPrimary 
        WHERE aso.id = :assignmentId
        """)
    void setPrimaryAdminStatus(@Param("assignmentId") UUID assignmentId, @Param("isPrimary") Boolean isPrimary);
    
    // ========== Date-based Queries ==========
    
    /**
     * Find assignments created between dates
     */
    @Query("""
        SELECT aso FROM AdminSupervisor aso 
        WHERE aso.assignedDate BETWEEN :startDate AND :endDate
        ORDER BY aso.assignedDate DESC
        """)
    List<AdminSupervisor> findAssignmentsBetweenDates(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find assignments ending soon (within specified days)
     */
    @Query("""
        SELECT aso FROM AdminSupervisor aso 
        WHERE aso.isActive = true 
        AND aso.endDate IS NOT NULL 
        AND aso.endDate BETWEEN :today AND :futureDate
        ORDER BY aso.endDate ASC
        """)
    List<AdminSupervisor> findAssignmentsEndingSoon(
        @Param("today") LocalDate today, 
        @Param("futureDate") LocalDate futureDate);
    
    /**
     * Find long-running assignments (longer than specified days)
     */
    @Query("""
        SELECT aso FROM AdminSupervisor aso 
        WHERE aso.isActive = true 
        AND aso.assignedDate <= :thresholdDate
        ORDER BY aso.assignedDate ASC
        """)
    List<AdminSupervisor> findLongRunningAssignments(@Param("thresholdDate") LocalDate thresholdDate);
    
    // ========== Statistics Queries ==========
    
    /**
     * Count assignments by admin
     */
    @Query("""
        SELECT a.id, a.username, a.fullName, COUNT(aso) 
        FROM AdminSupervisor aso 
        RIGHT JOIN User a ON aso.admin.id = a.id
        WHERE a.userType IN :adminTypes
        GROUP BY a.id, a.username, a.fullName
        ORDER BY COUNT(aso) DESC
        """)
    List<Object[]> countAssignmentsByAdmin(@Param("adminTypes") List<UserType> adminTypes);
    
    /**
     * Count assignments by region
     */
    @Query("""
        SELECT aso.region, COUNT(aso) 
        FROM AdminSupervisor aso 
        WHERE aso.isActive = true 
        AND aso.region IS NOT NULL
        GROUP BY aso.region 
        ORDER BY COUNT(aso) DESC
        """)
    List<Object[]> countAssignmentsByRegion();
    
    /**
     * Count assignments by authority level
     */
    @Query("""
        SELECT aso.authorityLevel, COUNT(aso) 
        FROM AdminSupervisor aso 
        WHERE aso.isActive = true 
        GROUP BY aso.authorityLevel 
        ORDER BY aso.authorityLevel ASC
        """)
    List<Object[]> countAssignmentsByAuthorityLevel();
    
    /**
     * Get assignment statistics
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_assignments,
            COUNT(CASE WHEN is_active = true THEN 1 END) as active_assignments,
            COUNT(DISTINCT admin_id) as admins_with_assignments,
            COUNT(DISTINCT supervisor_id) as assigned_supervisors,
            COUNT(CASE WHEN is_primary_admin = true THEN 1 END) as primary_assignments,
            AVG(authority_level) as avg_authority_level,
            AVG(efficiency_rating) as avg_efficiency_rating,
            SUM(total_schools_covered) as total_schools_managed,
            SUM(technicians_covered) as total_technicians_managed
        FROM admin_supervisors
        WHERE is_active = true
        """, nativeQuery = true)
    Object getAssignmentStatistics();
    
    /**
     * Get admin performance metrics
     */
    @Query(value = """
        SELECT 
            COUNT(aso.id) as supervisors_managed,
            SUM(aso.total_schools_covered) as schools_overseen,
            SUM(aso.technicians_covered) as technicians_overseen,
            AVG(aso.efficiency_rating) as avg_efficiency,
            COUNT(CASE WHEN aso.is_primary_admin = true THEN 1 END) as primary_assignments,
            COUNT(CASE WHEN aso.authority_level <= 2 THEN 1 END) as high_authority_assignments
        FROM admin_supervisors aso
        WHERE aso.admin_id = :adminId 
        AND aso.is_active = true
        """, nativeQuery = true)
    Object getAdminPerformanceMetrics(@Param("adminId") UUID adminId);
    
    /**
     * Find assignments with pagination
     */
    @Query("""
        SELECT aso FROM AdminSupervisor aso 
        WHERE aso.isActive = true 
        ORDER BY aso.assignedDate DESC
        """)
    Page<AdminSupervisor> findActiveAssignments(Pageable pageable);
    
    /**
     * Search assignments by admin or supervisor name
     */
    @Query("""
        SELECT aso FROM AdminSupervisor aso 
        WHERE (LOWER(aso.admin.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(aso.admin.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(aso.admin.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(aso.supervisor.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(aso.supervisor.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(aso.supervisor.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(aso.region) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        AND aso.isActive = true
        """)
    Page<AdminSupervisor> searchAssignments(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    /**
     * Find supervisors available for assignment (no active primary admin)
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.userType = :userType 
        AND u.status = :status
        AND u.isActive = true
        AND u.deletedAt IS NULL
        AND NOT EXISTS (
            SELECT 1 FROM AdminSupervisor aso 
            WHERE aso.supervisor.id = u.id 
            AND aso.isActive = true
            AND aso.isPrimaryAdmin = true
        )
        """)
    List<AdminSupervisor> findUnassignedSupervisors(@Param("userType") UserType userType, @Param("status") UserStatus status);
    
    /**
     * Calculate total management span for an admin
     */
    @Query(value = """
        SELECT 
            COALESCE(SUM(total_schools_covered), 0) as total_schools,
            COALESCE(SUM(technicians_covered), 0) as total_technicians,
            COUNT(*) as supervisors_count
        FROM admin_supervisors 
        WHERE admin_id = :adminId 
        AND is_active = true
        """, nativeQuery = true)
    Object calculateManagementSpan(@Param("adminId") UUID adminId);
}