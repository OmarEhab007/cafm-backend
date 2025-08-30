package com.cafm.cafmbackend.application.service;

import com.cafm.cafmbackend.infrastructure.persistence.entity.SupervisorTechnician;
import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import com.cafm.cafmbackend.shared.enums.TechnicianSpecialization;
import com.cafm.cafmbackend.shared.enums.UserStatus;
import com.cafm.cafmbackend.shared.enums.UserType;
import com.cafm.cafmbackend.infrastructure.persistence.repository.SupervisorTechnicianRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing technician assignments to supervisors.
 * 
 * Architecture: Business service layer for assignment management
 * Pattern: Service layer with transactional operations
 * Java 23: Uses modern exception handling and validation
 */
@Service
@Transactional
public class TechnicianAssignmentService {
    
    private static final Logger logger = LoggerFactory.getLogger(TechnicianAssignmentService.class);
    
    private final SupervisorTechnicianRepository assignmentRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public TechnicianAssignmentService(SupervisorTechnicianRepository assignmentRepository, 
                                       UserRepository userRepository) {
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
    }
    
    // ========== Assignment Management ==========
    
    /**
     * Assign a technician to a supervisor
     */
    public SupervisorTechnician assignTechnicianToSupervisor(UUID supervisorId, 
                                                             UUID technicianId, 
                                                             TechnicianSpecialization primarySpecialization,
                                                             Integer priorityLevel,
                                                             String assignmentNotes,
                                                             UUID assignedByUserId) {
        logger.info("Assigning technician {} to supervisor {}", technicianId, supervisorId);
        
        // Validate supervisor
        User supervisor = userRepository.findById(supervisorId)
            .orElseThrow(() -> new EntityNotFoundException("Supervisor not found with ID: " + supervisorId));
        
        if (!supervisor.isSupervisor()) {
            throw new IllegalArgumentException("User is not a supervisor: " + supervisorId);
        }
        
        // Validate technician
        User technician = userRepository.findById(technicianId)
            .orElseThrow(() -> new EntityNotFoundException("Technician not found with ID: " + technicianId));
        
        if (!technician.isTechnician()) {
            throw new IllegalArgumentException("User is not a technician: " + technicianId);
        }
        
        if (!technician.isAvailableForAssignment()) {
            throw new IllegalStateException("Technician is not available for assignment: " + technicianId);
        }
        
        // Check if technician is already assigned
        Optional<SupervisorTechnician> existingAssignment = 
            assignmentRepository.findActiveAssignmentByTechnician(technicianId);
        
        if (existingAssignment.isPresent()) {
            throw new IllegalStateException("Technician is already assigned to supervisor: " + 
                existingAssignment.get().getSupervisor().getId());
        }
        
        // Create new assignment
        SupervisorTechnician assignment = new SupervisorTechnician(supervisor, technician, LocalDate.now());
        assignment.setPrimarySpecialization(primarySpecialization != null ? 
            primarySpecialization : technician.getSpecialization());
        assignment.setPriorityLevel(priorityLevel != null ? priorityLevel : 5);
        assignment.setAssignmentNotes(assignmentNotes);
        assignment.setAssignedByUserId(assignedByUserId);
        
        SupervisorTechnician savedAssignment = assignmentRepository.save(assignment);
        
        logger.info("Successfully assigned technician {} to supervisor {} with assignment ID {}", 
                   technicianId, supervisorId, savedAssignment.getId());
        
        return savedAssignment;
    }
    
    /**
     * Reassign a technician to a different supervisor
     */
    public SupervisorTechnician reassignTechnician(UUID technicianId, 
                                                   UUID newSupervisorId,
                                                   String reassignmentReason,
                                                   UUID reassignedByUserId) {
        logger.info("Reassigning technician {} to new supervisor {}", technicianId, newSupervisorId);
        
        // End current assignment
        SupervisorTechnician currentAssignment = assignmentRepository.findActiveAssignmentByTechnician(technicianId)
            .orElseThrow(() -> new EntityNotFoundException("No active assignment found for technician: " + technicianId));
        
        currentAssignment.endAssignment(LocalDate.now());
        assignmentRepository.save(currentAssignment);
        
        // Create new assignment with same specialization and priority
        return assignTechnicianToSupervisor(
            newSupervisorId,
            technicianId,
            currentAssignment.getPrimarySpecialization(),
            currentAssignment.getPriorityLevel(),
            reassignmentReason,
            reassignedByUserId
        );
    }
    
    /**
     * End a technician assignment
     */
    public void endAssignment(UUID assignmentId, String endReason) {
        logger.info("Ending assignment {}", assignmentId);
        
        SupervisorTechnician assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new EntityNotFoundException("Assignment not found: " + assignmentId));
        
        if (!assignment.isCurrentlyActive()) {
            throw new IllegalStateException("Assignment is not active: " + assignmentId);
        }
        
        assignment.endAssignment(LocalDate.now());
        if (endReason != null) {
            assignment.setAssignmentNotes(
                (assignment.getAssignmentNotes() != null ? assignment.getAssignmentNotes() + "\n" : "") +
                "Ended: " + endReason
            );
        }
        
        assignmentRepository.save(assignment);
        logger.info("Successfully ended assignment {}", assignmentId);
    }
    
    /**
     * End all assignments for a technician (when technician leaves, etc.)
     */
    public void endAllAssignmentsForTechnician(UUID technicianId, String reason) {
        logger.info("Ending all assignments for technician {}", technicianId);
        
        List<SupervisorTechnician> assignments = 
            assignmentRepository.findAssignmentHistoryByTechnician(technicianId);
        
        int endedCount = 0;
        for (SupervisorTechnician assignment : assignments) {
            if (assignment.isCurrentlyActive()) {
                assignment.endAssignment(LocalDate.now());
                if (reason != null) {
                    assignment.setAssignmentNotes(
                        (assignment.getAssignmentNotes() != null ? assignment.getAssignmentNotes() + "\n" : "") +
                        "Bulk ended: " + reason
                    );
                }
                assignmentRepository.save(assignment);
                endedCount++;
            }
        }
        
        logger.info("Ended {} active assignments for technician {}", endedCount, technicianId);
    }
    
    // ========== Query Methods ==========
    
    /**
     * Find active assignment for a technician
     */
    public Optional<SupervisorTechnician> findActiveAssignmentByTechnician(UUID technicianId) {
        return assignmentRepository.findActiveAssignmentByTechnician(technicianId);
    }
    
    /**
     * Get all technicians assigned to a supervisor
     */
    public List<SupervisorTechnician> getTechniciansBySupervisor(UUID supervisorId) {
        return assignmentRepository.findTechniciansBySupervisor(supervisorId);
    }
    
    /**
     * Get technicians by supervisor and specialization
     */
    public List<SupervisorTechnician> getTechniciansBySupervisorAndSpecialization(UUID supervisorId, 
                                                                                  TechnicianSpecialization specialization) {
        return assignmentRepository.findTechniciansBySupervisorAndSpecialization(supervisorId, specialization);
    }
    
    /**
     * Find available technicians for a specialization
     */
    public List<SupervisorTechnician> findAvailableTechniciansBySpecialization(TechnicianSpecialization specialization) {
        return assignmentRepository.findAvailableBySpecialization(specialization, UserStatus.ACTIVE);
    }
    
    /**
     * Get assignment history for a technician
     */
    public List<SupervisorTechnician> getAssignmentHistoryByTechnician(UUID technicianId) {
        return assignmentRepository.findAssignmentHistoryByTechnician(technicianId);
    }
    
    /**
     * Get all active assignments with pagination
     */
    public Page<SupervisorTechnician> getActiveAssignments(Pageable pageable) {
        return assignmentRepository.findActiveAssignments(pageable);
    }
    
    /**
     * Search assignments by supervisor or technician name
     */
    public Page<SupervisorTechnician> searchAssignments(String searchTerm, Pageable pageable) {
        return assignmentRepository.searchAssignments(searchTerm, pageable);
    }
    
    // ========== Business Logic Methods ==========
    
    /**
     * Find unassigned technicians available for assignment
     */
    public List<User> findUnassignedTechnicians() {
        return userRepository.findUnassignedTechnicians(UserType.TECHNICIAN, UserStatus.ACTIVE);
    }
    
    /**
     * Find unassigned technicians by specialization
     */
    public List<User> findUnassignedTechniciansBySpecialization(TechnicianSpecialization specialization) {
        return userRepository.findAvailableTechniciansBySpecialization(UserType.TECHNICIAN, specialization, UserStatus.ACTIVE);
    }
    
    /**
     * Check if a technician can be assigned to a supervisor
     */
    public boolean canAssignTechnician(UUID supervisorId, UUID technicianId) {
        try {
            // Check if supervisor exists and is valid
            User supervisor = userRepository.findById(supervisorId).orElse(null);
            if (supervisor == null || !supervisor.isSupervisor()) {
                return false;
            }
            
            // Check if technician exists and is available
            User technician = userRepository.findById(technicianId).orElse(null);
            if (technician == null || !technician.isTechnician()) {
                return false;
            }
            
            if (!technician.isAvailableForAssignment()) {
                return false;
            }
            
            // Check if technician is not already assigned
            return assignmentRepository.findActiveAssignmentByTechnician(technicianId).isEmpty();
            
        } catch (Exception e) {
            logger.error("Error checking if technician can be assigned", e);
            return false;
        }
    }
    
    /**
     * Get supervisor's workload (number of assigned technicians)
     */
    public int getSupervisorWorkload(UUID supervisorId) {
        return assignmentRepository.findTechniciansBySupervisor(supervisorId).size();
    }
    
    /**
     * Find supervisors with capacity for more technicians
     */
    public List<User> findSupervisorsWithCapacity(int maxTechniciansPerSupervisor) {
        List<User> allSupervisors = userRepository.findActiveByUserType(UserType.SUPERVISOR, UserStatus.ACTIVE);
        
        return allSupervisors.stream()
            .filter(supervisor -> getSupervisorWorkload(supervisor.getId()) < maxTechniciansPerSupervisor)
            .toList();
    }
    
    /**
     * Update assignment priority
     */
    public SupervisorTechnician updateAssignmentPriority(UUID assignmentId, Integer newPriority) {
        logger.info("Updating assignment {} priority to {}", assignmentId, newPriority);
        
        SupervisorTechnician assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new EntityNotFoundException("Assignment not found: " + assignmentId));
        
        assignment.setPriorityLevel(newPriority);
        return assignmentRepository.save(assignment);
    }
    
    /**
     * Update assignment notes
     */
    public SupervisorTechnician updateAssignmentNotes(UUID assignmentId, String notes) {
        logger.info("Updating assignment {} notes", assignmentId);
        
        SupervisorTechnician assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new EntityNotFoundException("Assignment not found: " + assignmentId));
        
        assignment.setAssignmentNotes(notes);
        return assignmentRepository.save(assignment);
    }
    
    // ========== Task Management ==========
    
    /**
     * Record task assignment to a technician
     */
    public void recordTaskAssignment(UUID assignmentId) {
        logger.debug("Recording task assignment for assignment {}", assignmentId);
        
        SupervisorTechnician assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new EntityNotFoundException("Assignment not found: " + assignmentId));
        
        assignment.assignTask();
        assignmentRepository.save(assignment);
    }
    
    /**
     * Record task completion by a technician
     */
    public void recordTaskCompletion(UUID assignmentId, double completionTimeHours) {
        logger.debug("Recording task completion for assignment {} with time {}", assignmentId, completionTimeHours);
        
        SupervisorTechnician assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new EntityNotFoundException("Assignment not found: " + assignmentId));
        
        assignment.completeTask(completionTimeHours);
        assignmentRepository.save(assignment);
    }
    
    // ========== Statistics and Reporting ==========
    
    /**
     * Get assignment statistics
     */
    public Object getAssignmentStatistics() {
        return assignmentRepository.getAssignmentStatistics();
    }
    
    /**
     * Get supervisor performance metrics
     */
    public Object getSupervisorPerformanceMetrics(UUID supervisorId) {
        return assignmentRepository.getSupervisorPerformanceMetrics(supervisorId);
    }
    
    /**
     * Find top performing assignments
     */
    public List<SupervisorTechnician> getTopPerformingAssignments(int limit) {
        return assignmentRepository.findTopPerformingAssignments(limit);
    }
    
    /**
     * Find underperforming assignments
     */
    public List<SupervisorTechnician> getUnderperformingAssignments(int minTasks, double threshold) {
        return assignmentRepository.findUnderperformingAssignments(minTasks, threshold);
    }
    
    /**
     * Count assignments by supervisor
     */
    public List<Object[]> countAssignmentsBySupervisor() {
        return assignmentRepository.countAssignmentsBySupervisor(UserType.SUPERVISOR);
    }
    
    /**
     * Count assignments by specialization
     */
    public List<Object[]> countAssignmentsBySpecialization() {
        return assignmentRepository.countAssignmentsBySpecialization();
    }
}