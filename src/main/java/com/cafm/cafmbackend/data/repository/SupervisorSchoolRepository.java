package com.cafm.cafmbackend.data.repository;

import com.cafm.cafmbackend.data.entity.SupervisorSchool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SupervisorSchool entity for managing supervisor-school assignments.
 */
@Repository
public interface SupervisorSchoolRepository extends JpaRepository<SupervisorSchool, UUID> {
    
    /**
     * Find all assignments for a supervisor
     */
    List<SupervisorSchool> findBySupervisorId(UUID supervisorId);
    
    /**
     * Find all assignments for a school
     */
    List<SupervisorSchool> findBySchoolId(UUID schoolId);
    
    /**
     * Find active assignments for a supervisor
     */
    List<SupervisorSchool> findBySupervisorIdAndIsActiveTrue(UUID supervisorId);
    
    /**
     * Find active assignments for a school
     */
    List<SupervisorSchool> findBySchoolIdAndIsActiveTrue(UUID schoolId);
    
    /**
     * Check if assignment exists
     */
    boolean existsBySupervisorIdAndSchoolId(UUID supervisorId, UUID schoolId);
    
    /**
     * Find specific assignment
     */
    Optional<SupervisorSchool> findBySupervisorIdAndSchoolId(UUID supervisorId, UUID schoolId);
    
    /**
     * Count active assignments for a school
     */
    long countBySchoolIdAndIsActiveTrue(UUID schoolId);
    
    /**
     * Count active assignments for a supervisor
     */
    long countBySupervisorIdAndIsActiveTrue(UUID supervisorId);
    
    /**
     * Find assignments by company
     */
    @Query("SELECT ss FROM SupervisorSchool ss WHERE ss.school.company.id = :companyId")
    Page<SupervisorSchool> findByCompanyId(@Param("companyId") UUID companyId, Pageable pageable);
    
    /**
     * Find active assignments by company
     */
    @Query("SELECT ss FROM SupervisorSchool ss WHERE ss.school.company.id = :companyId AND ss.isActive = true")
    List<SupervisorSchool> findActiveByCompanyId(@Param("companyId") UUID companyId);
}