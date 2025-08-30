package com.cafm.cafmbackend.infrastructure.persistence.repository;

import com.cafm.cafmbackend.infrastructure.persistence.entity.School;
import com.cafm.cafmbackend.shared.enums.ReportStatus;
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
 * Repository interface for School entity.
 * 
 * Architecture: Data access layer for school management
 * Pattern: Repository pattern with spatial queries support
 */
@Repository
public interface SchoolRepository extends JpaRepository<School, UUID>, JpaSpecificationExecutor<School> {
    
    // ========== Basic Queries ==========
    
    /**
     * Find school by code
     */
    Optional<School> findByCode(String code);
    
    /**
     * Find school by name (case-insensitive)
     */
    Optional<School> findByNameIgnoreCase(String name);
    
    /**
     * Check if school code exists
     */
    boolean existsByCode(String code);
    
    /**
     * Find active schools
     */
    @Query("SELECT s FROM School s WHERE s.isActive = true AND s.deletedAt IS NULL")
    List<School> findActiveSchools();
    
    // ========== Filter Queries ==========
    
    /**
     * Find schools by type
     */
    List<School> findByType(String type);
    
    /**
     * Find schools by gender
     */
    List<School> findByGender(String gender);
    
    /**
     * Find schools by city
     */
    List<School> findByCity(String city);
    
    /**
     * Find schools by type and gender
     */
    @Query("SELECT s FROM School s WHERE s.type = :type AND s.gender = :gender AND s.deletedAt IS NULL")
    List<School> findByTypeAndGender(@Param("type") String type, @Param("gender") String gender);
    
    // ========== Search Queries ==========
    
    /**
     * Search schools by name or code
     */
    @Query("""
        SELECT s FROM School s 
        WHERE s.deletedAt IS NULL 
        AND (
            LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
            LOWER(s.nameAr) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
            LOWER(s.code) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        )
        """)
    Page<School> searchSchools(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // ========== Spatial Queries ==========
    
    /**
     * Find schools within radius (in kilometers) using PostgreSQL
     */
    @Query(value = """
        SELECT * FROM schools s 
        WHERE s.deleted_at IS NULL 
        AND s.latitude IS NOT NULL 
        AND s.longitude IS NOT NULL
        AND (
            6371 * acos(
                cos(radians(:lat)) * cos(radians(s.latitude)) * 
                cos(radians(s.longitude) - radians(:lng)) + 
                sin(radians(:lat)) * sin(radians(s.latitude))
            )
        ) <= :radius
        ORDER BY (
            6371 * acos(
                cos(radians(:lat)) * cos(radians(s.latitude)) * 
                cos(radians(s.longitude) - radians(:lng)) + 
                sin(radians(:lat)) * sin(radians(s.latitude))
            )
        )
        """, nativeQuery = true)
    List<School> findSchoolsWithinRadius(@Param("lat") Double latitude, 
                                         @Param("lng") Double longitude, 
                                         @Param("radius") Double radius);
    
    /**
     * Find nearest schools to a location
     */
    @Query(value = """
        SELECT * FROM schools s 
        WHERE s.deleted_at IS NULL 
        AND s.latitude IS NOT NULL 
        AND s.longitude IS NOT NULL
        ORDER BY (
            6371 * acos(
                cos(radians(:lat)) * cos(radians(s.latitude)) * 
                cos(radians(s.longitude) - radians(:lng)) + 
                sin(radians(:lat)) * sin(radians(s.latitude))
            )
        )
        LIMIT :limit
        """, nativeQuery = true)
    List<School> findNearestSchools(@Param("lat") Double latitude, 
                                    @Param("lng") Double longitude, 
                                    @Param("limit") int limit);
    
    // ========== Supervisor Assignment Queries ==========
    
    /**
     * Find schools assigned to a supervisor
     */
    @Query("""
        SELECT s FROM School s 
        JOIN SupervisorSchool ss ON s.id = ss.school.id 
        WHERE ss.supervisor.id = :supervisorId 
        AND ss.isActive = true 
        AND s.deletedAt IS NULL
        """)
    List<School> findSchoolsBySupervisor(@Param("supervisorId") UUID supervisorId);
    
    /**
     * Find unassigned schools
     */
    @Query("""
        SELECT s FROM School s 
        WHERE s.deletedAt IS NULL 
        AND NOT EXISTS (
            SELECT 1 FROM SupervisorSchool ss 
            WHERE ss.school.id = s.id 
            AND ss.isActive = true
        )
        """)
    List<School> findUnassignedSchools();
    
    // ========== Maintenance Statistics ==========
    
    /**
     * Find schools with pending reports
     */
    @Query("""
        SELECT DISTINCT s FROM School s 
        JOIN Report r ON s.id = r.school.id 
        WHERE r.status IN :pendingStatuses 
        AND s.deletedAt IS NULL 
        AND r.deletedAt IS NULL
        """)
    List<School> findSchoolsWithPendingReports(@Param("pendingStatuses") List<ReportStatus> pendingStatuses);
    
    /**
     * Count reports by school
     */
    @Query("""
        SELECT s.id, s.name, COUNT(r) 
        FROM School s 
        LEFT JOIN Report r ON s.id = r.school.id AND r.deletedAt IS NULL
        WHERE s.deletedAt IS NULL 
        GROUP BY s.id, s.name
        """)
    List<Object[]> countReportsBySchool();
    
    /**
     * Get school maintenance score statistics
     */
    @Query(value = """
        SELECT 
            AVG(maintenance_score) as avg_score,
            MIN(maintenance_score) as min_score,
            MAX(maintenance_score) as max_score,
            COUNT(*) as school_count
        FROM schools 
        WHERE deleted_at IS NULL 
        AND maintenance_score IS NOT NULL
        """, nativeQuery = true)
    Object getMaintenanceScoreStatistics();
    
    // ========== Update Operations ==========
    
    /**
     * Update school maintenance score
     */
    @Modifying
    @Query("UPDATE School s SET s.maintenanceScore = :score WHERE s.id = :schoolId")
    void updateMaintenanceScore(@Param("schoolId") UUID schoolId, @Param("score") BigDecimal score);

    /**
     * Update school activity level
     */
    @Modifying
    @Query("UPDATE School s SET s.activityLevel = :level WHERE s.id = :schoolId")
    void updateActivityLevel(@Param("schoolId") UUID schoolId, @Param("level") String level);

    /**
     * Activate/Deactivate school
     */
    @Modifying
    @Query("UPDATE School s SET s.isActive = :active WHERE s.id = :schoolId")
    void setActiveStatus(@Param("schoolId") UUID schoolId, @Param("active") Boolean active);
    
    // ========== Statistics Queries ==========
    
    /**
     * Count schools by type
     */
    @Query("SELECT s.type, COUNT(s) FROM School s WHERE s.deletedAt IS NULL GROUP BY s.type")
    List<Object[]> countByType();
    
    /**
     * Count schools by city
     */
    @Query("SELECT s.city, COUNT(s) FROM School s WHERE s.deletedAt IS NULL GROUP BY s.city")
    List<Object[]> countByCity();
    
    /**
     * Get school statistics
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_schools,
            COUNT(CASE WHEN is_active = true THEN 1 END) as active_schools,
            COUNT(CASE WHEN type = 'PRIMARY' THEN 1 END) as primary_schools,
            COUNT(CASE WHEN type = 'SECONDARY' THEN 1 END) as secondary_schools,
            COUNT(CASE WHEN type = 'HIGH_SCHOOL' THEN 1 END) as high_schools,
            COUNT(CASE WHEN gender = 'BOYS' THEN 1 END) as boys_schools,
            COUNT(CASE WHEN gender = 'GIRLS' THEN 1 END) as girls_schools,
            COUNT(CASE WHEN gender = 'MIXED' THEN 1 END) as mixed_schools
        FROM schools 
        WHERE deleted_at IS NULL
        """, nativeQuery = true)
    Object getSchoolStatistics();
    
    // ========== Company-specific Queries ==========
    
    /**
     * Count schools by company ID
     */
    long countByCompany_IdAndDeletedAtIsNull(UUID companyId);
}