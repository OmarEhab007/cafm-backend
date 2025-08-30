package com.cafm.cafmbackend.infrastructure.persistence.repository;

import com.cafm.cafmbackend.infrastructure.persistence.entity.SupervisorTechnician;
import com.cafm.cafmbackend.shared.enums.TechnicianSpecialization;
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
 * Repository interface for SupervisorTechnician entity.
 * 
 * Architecture: Data access layer for technician-supervisor assignments
 * Pattern: Repository pattern with assignment workflow support
 */
@Repository
public interface SupervisorTechnicianRepository extends JpaRepository<SupervisorTechnician, UUID>, JpaSpecificationExecutor<SupervisorTechnician> {
    
    // ========== Basic Assignment Queries ==========
    
    /**
     * Find active assignment for a technician
     */
    @Query("""
        SELECT st FROM SupervisorTechnician st 
        WHERE st.technician.id = :technicianId 
        AND st.isActive = true
        """)
    Optional<SupervisorTechnician> findActiveAssignmentByTechnician(@Param("technicianId") UUID technicianId);
    
    /**
     * Find all technicians assigned to a supervisor
     */
    @Query("""
        SELECT st FROM SupervisorTechnician st 
        WHERE st.supervisor.id = :supervisorId 
        AND st.isActive = true
        ORDER BY st.priorityLevel ASC, st.assignedDate DESC
        """)
    List<SupervisorTechnician> findTechniciansBySupervisor(@Param("supervisorId") UUID supervisorId);
    
    /**
     * Find all supervisors a technician has been assigned to (historical)
     */
    @Query("""
        SELECT st FROM SupervisorTechnician st 
        WHERE st.technician.id = :technicianId 
        ORDER BY st.assignedDate DESC
        """)
    List<SupervisorTechnician> findAssignmentHistoryByTechnician(@Param("technicianId") UUID technicianId);
    
    /**
     * Check if technician is already assigned to a supervisor
     */
    @Query("""
        SELECT CASE WHEN COUNT(st) > 0 THEN true ELSE false END 
        FROM SupervisorTechnician st 
        WHERE st.technician.id = :technicianId 
        AND st.supervisor.id = :supervisorId 
        AND st.isActive = true
        """)
    boolean existsActiveAssignment(@Param("supervisorId") UUID supervisorId, @Param("technicianId") UUID technicianId);
    
    /**
     * Check if technician has any active assignment
     */
    boolean existsByTechnicianIdAndIsActiveTrue(UUID technicianId);
    
    /**
     * Count active technicians assigned to a supervisor
     */
    long countBySupervisorIdAndIsActiveTrue(UUID supervisorId);
    
    // ========== Specialization-based Queries ==========
    
    /**
     * Find technicians by supervisor and specialization
     */
    @Query("""
        SELECT st FROM SupervisorTechnician st 
        WHERE st.supervisor.id = :supervisorId 
        AND st.primarySpecialization = :specialization
        AND st.isActive = true
        ORDER BY st.priorityLevel ASC
        """)
    List<SupervisorTechnician> findTechniciansBySupervisorAndSpecialization(
        @Param("supervisorId") UUID supervisorId, 
        @Param("specialization") TechnicianSpecialization specialization);
    
    /**
     * Find available technicians for a specialization
     */
    @Query("""
        SELECT st FROM SupervisorTechnician st 
        WHERE st.primarySpecialization = :specialization
        AND st.isActive = true
        AND st.technician.isAvailableForAssignment = true
        AND st.technician.status = :status
        AND st.technician.isActive = true
        AND st.technician.isLocked = false
        ORDER BY st.technician.performanceRating DESC NULLS LAST
        """)
    List<SupervisorTechnician> findAvailableBySpecialization(@Param("specialization") TechnicianSpecialization specialization, @Param("status") UserStatus status);
    
    // ========== Performance Queries ==========
    
    /**
     * Find top performing assignments by completion rate
     */
    @Query(value = """
        SELECT * FROM supervisor_technicians st
        WHERE st.is_active = true 
        AND st.tasks_assigned > 0
        ORDER BY (CAST(st.tasks_completed AS FLOAT) / st.tasks_assigned) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<SupervisorTechnician> findTopPerformingAssignments(@Param("limit") int limit);
    
    /**
     * Find assignments with low completion rates (underperforming)
     */
    @Query(value = """
        SELECT * FROM supervisor_technicians st
        WHERE st.is_active = true 
        AND st.tasks_assigned >= :minTasks
        AND (CAST(st.tasks_completed AS FLOAT) / st.tasks_assigned) < :threshold
        ORDER BY (CAST(st.tasks_completed AS FLOAT) / st.tasks_assigned) ASC
        """, nativeQuery = true)
    List<SupervisorTechnician> findUnderperformingAssignments(
        @Param("minTasks") int minTasks, 
        @Param("threshold") double threshold);
    
    /**
     * Get average performance metrics for a supervisor
     */
    @Query(value = """
        SELECT 
            AVG(CASE WHEN st.tasks_assigned > 0 
                THEN CAST(st.tasks_completed AS FLOAT) / st.tasks_assigned 
                ELSE 0 END) as avg_completion_rate,
            AVG(st.avg_completion_time_hours) as avg_completion_time,
            COUNT(*) as total_assignments,
            SUM(st.tasks_assigned) as total_tasks_assigned,
            SUM(st.tasks_completed) as total_tasks_completed
        FROM supervisor_technicians st
        WHERE st.supervisor_id = :supervisorId 
        AND st.is_active = true
        """, nativeQuery = true)
    Object getSupervisorPerformanceMetrics(@Param("supervisorId") UUID supervisorId);
    
    // ========== Assignment Management ==========
    
    /**
     * End assignment (set inactive and end date)
     */
    @Modifying
    @Query("""
        UPDATE SupervisorTechnician st 
        SET st.isActive = false, 
            st.endDate = :endDate 
        WHERE st.id = :assignmentId
        """)
    void endAssignment(@Param("assignmentId") UUID assignmentId, @Param("endDate") LocalDate endDate);
    
    /**
     * End all active assignments for a technician
     */
    @Modifying
    @Query("""
        UPDATE SupervisorTechnician st 
        SET st.isActive = false, 
            st.endDate = :endDate 
        WHERE st.technician.id = :technicianId 
        AND st.isActive = true
        """)
    void endAllAssignmentsForTechnician(@Param("technicianId") UUID technicianId, @Param("endDate") LocalDate endDate);
    
    /**
     * Update priority level for assignment
     */
    @Modifying
    @Query("""
        UPDATE SupervisorTechnician st 
        SET st.priorityLevel = :priority 
        WHERE st.id = :assignmentId
        """)
    void updatePriority(@Param("assignmentId") UUID assignmentId, @Param("priority") Integer priority);
    
    /**
     * Update task counters
     */
    @Modifying
    @Query("""
        UPDATE SupervisorTechnician st 
        SET st.tasksAssigned = st.tasksAssigned + 1 
        WHERE st.id = :assignmentId
        """)
    void incrementTasksAssigned(@Param("assignmentId") UUID assignmentId);
    
    @Modifying
    @Query("""
        UPDATE SupervisorTechnician st 
        SET st.tasksCompleted = st.tasksCompleted + 1,
            st.avgCompletionTimeHours = 
                CASE WHEN st.avgCompletionTimeHours IS NULL 
                THEN :completionTimeHours
                ELSE (st.avgCompletionTimeHours * (st.tasksCompleted) + :completionTimeHours) / (st.tasksCompleted + 1)
                END
        WHERE st.id = :assignmentId
        """)
    void incrementTasksCompleted(@Param("assignmentId") UUID assignmentId, @Param("completionTimeHours") Double completionTimeHours);
    
    // ========== Date-based Queries ==========
    
    /**
     * Find assignments created between dates
     */
    @Query("""
        SELECT st FROM SupervisorTechnician st 
        WHERE st.assignedDate BETWEEN :startDate AND :endDate
        ORDER BY st.assignedDate DESC
        """)
    List<SupervisorTechnician> findAssignmentsBetweenDates(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find assignments ending soon (within specified days)
     */
    @Query("""
        SELECT st FROM SupervisorTechnician st 
        WHERE st.isActive = true 
        AND st.endDate IS NOT NULL 
        AND st.endDate BETWEEN :today AND :futureDate
        ORDER BY st.endDate ASC
        """)
    List<SupervisorTechnician> findAssignmentsEndingSoon(
        @Param("today") LocalDate today, 
        @Param("futureDate") LocalDate futureDate);
    
    /**
     * Find long-running assignments (longer than specified days)
     */
    @Query("""
        SELECT st FROM SupervisorTechnician st 
        WHERE st.isActive = true 
        AND st.assignedDate <= :thresholdDate
        ORDER BY st.assignedDate ASC
        """)
    List<SupervisorTechnician> findLongRunningAssignments(@Param("thresholdDate") LocalDate thresholdDate);
    
    // ========== Statistics Queries ==========
    
    /**
     * Count assignments by supervisor
     */
    @Query("""
        SELECT s.id, s.username, s.fullName, COUNT(st) 
        FROM SupervisorTechnician st 
        RIGHT JOIN User s ON st.supervisor.id = s.id
        WHERE s.userType = :userType
        GROUP BY s.id, s.username, s.fullName
        ORDER BY COUNT(st) DESC
        """)
    List<Object[]> countAssignmentsBySupervisor(@Param("userType") UserType userType);
    
    /**
     * Count assignments by specialization
     */
    @Query("""
        SELECT st.primarySpecialization, COUNT(st) 
        FROM SupervisorTechnician st 
        WHERE st.isActive = true 
        GROUP BY st.primarySpecialization 
        ORDER BY COUNT(st) DESC
        """)
    List<Object[]> countAssignmentsBySpecialization();
    
    /**
     * Get assignment statistics
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_assignments,
            COUNT(CASE WHEN is_active = true THEN 1 END) as active_assignments,
            COUNT(DISTINCT supervisor_id) as supervisors_with_assignments,
            COUNT(DISTINCT technician_id) as assigned_technicians,
            AVG(priority_level) as avg_priority_level,
            AVG(CASE WHEN tasks_assigned > 0 
                THEN CAST(tasks_completed AS FLOAT) / tasks_assigned 
                ELSE NULL END) as overall_completion_rate
        FROM supervisor_technicians
        """, nativeQuery = true)
    Object getAssignmentStatistics();
    
    /**
     * Find assignments with pagination
     */
    @Query("""
        SELECT st FROM SupervisorTechnician st 
        WHERE st.isActive = true 
        ORDER BY st.assignedDate DESC
        """)
    Page<SupervisorTechnician> findActiveAssignments(Pageable pageable);
    
    /**
     * Search assignments by supervisor or technician name
     */
    @Query("""
        SELECT st FROM SupervisorTechnician st 
        WHERE (LOWER(st.supervisor.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(st.supervisor.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(st.supervisor.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(st.technician.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(st.technician.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(st.technician.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        AND st.isActive = true
        """)
    Page<SupervisorTechnician> searchAssignments(@Param("searchTerm") String searchTerm, Pageable pageable);
}