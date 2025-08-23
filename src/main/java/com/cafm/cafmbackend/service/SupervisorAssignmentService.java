package com.cafm.cafmbackend.service;

import com.cafm.cafmbackend.data.entity.AdminSupervisor;
import com.cafm.cafmbackend.data.entity.User;
import com.cafm.cafmbackend.data.enums.UserStatus;
import com.cafm.cafmbackend.data.enums.UserType;
import com.cafm.cafmbackend.data.repository.AdminSupervisorRepository;
import com.cafm.cafmbackend.data.repository.UserRepository;
import com.cafm.cafmbackend.data.repository.SupervisorSchoolRepository;
import com.cafm.cafmbackend.data.repository.SupervisorTechnicianRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing supervisor assignments to admins.
 * 
 * Architecture: Business service layer for hierarchical admin-supervisor management
 * Pattern: Service layer with transactional operations and business validation
 * Java 23: Uses modern exception handling and validation patterns
 */
@Service
@Transactional
public class SupervisorAssignmentService {
    
    private static final Logger logger = LoggerFactory.getLogger(SupervisorAssignmentService.class);
    
    private final AdminSupervisorRepository assignmentRepository;
    private final UserRepository userRepository;
    private final SupervisorSchoolRepository supervisorSchoolRepository;
    private final SupervisorTechnicianRepository supervisorTechnicianRepository;
    
    @Autowired
    public SupervisorAssignmentService(AdminSupervisorRepository assignmentRepository, 
                                       UserRepository userRepository,
                                       SupervisorSchoolRepository supervisorSchoolRepository,
                                       SupervisorTechnicianRepository supervisorTechnicianRepository) {
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.supervisorSchoolRepository = supervisorSchoolRepository;
        this.supervisorTechnicianRepository = supervisorTechnicianRepository;
    }
    
    // ========== Assignment Management ==========
    
    /**
     * Assign a supervisor to an admin
     */
    public AdminSupervisor assignSupervisorToAdmin(UUID adminId, 
                                                   UUID supervisorId, 
                                                   String region,
                                                   Integer authorityLevel,
                                                   Integer maxSchoolsOversight,
                                                   Boolean isPrimaryAdmin,
                                                   String assignmentNotes,
                                                   UUID assignedByUserId) {
        logger.info("Assigning supervisor {} to admin {}", supervisorId, adminId);
        
        // Validate admin
        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new EntityNotFoundException("Admin not found with ID: " + adminId));
        
        if (!admin.isAdmin()) {
            throw new IllegalArgumentException("User is not an admin: " + adminId);
        }
        
        // Validate supervisor
        User supervisor = userRepository.findById(supervisorId)
            .orElseThrow(() -> new EntityNotFoundException("Supervisor not found with ID: " + supervisorId));
        
        if (!supervisor.isSupervisor()) {
            throw new IllegalArgumentException("User is not a supervisor: " + supervisorId);
        }
        
        if (!supervisor.isVerified()) {
            throw new IllegalStateException("Supervisor is not verified: " + supervisorId);
        }
        
        // Check if trying to assign as primary admin but supervisor already has one
        if (Boolean.TRUE.equals(isPrimaryAdmin)) {
            Optional<AdminSupervisor> existingPrimaryAssignment = 
                assignmentRepository.findPrimaryAdminAssignmentBySupervisor(supervisorId);
            
            if (existingPrimaryAssignment.isPresent()) {
                throw new IllegalStateException("Supervisor already has a primary admin assignment: " + 
                    existingPrimaryAssignment.get().getAdmin().getId());
            }
        }
        
        // Check if this specific admin-supervisor pair already exists
        if (assignmentRepository.existsActiveAssignment(adminId, supervisorId)) {
            throw new IllegalStateException("Active assignment already exists between admin " + 
                adminId + " and supervisor " + supervisorId);
        }
        
        // Create new assignment
        AdminSupervisor assignment = new AdminSupervisor(admin, supervisor, LocalDate.now());
        assignment.setRegion(region);
        assignment.setAuthorityLevel(authorityLevel != null ? authorityLevel : 3);
        assignment.setMaxSchoolsOversight(maxSchoolsOversight != null ? maxSchoolsOversight : 50);
        assignment.setIsPrimaryAdmin(isPrimaryAdmin != null ? isPrimaryAdmin : true);
        assignment.setAssignmentNotes(assignmentNotes);
        assignment.setAssignedByUserId(assignedByUserId);
        
        AdminSupervisor savedAssignment = assignmentRepository.save(assignment);
        
        logger.info("Successfully assigned supervisor {} to admin {} with assignment ID {}", 
                   supervisorId, adminId, savedAssignment.getId());
        
        return savedAssignment;
    }
    
    /**
     * Reassign a supervisor to a different admin
     */
    public AdminSupervisor reassignSupervisor(UUID supervisorId, 
                                              UUID newAdminId,
                                              String reassignmentReason,
                                              UUID reassignedByUserId) {
        logger.info("Reassigning supervisor {} to new admin {}", supervisorId, newAdminId);
        
        // End current primary assignment
        AdminSupervisor currentAssignment = assignmentRepository.findPrimaryAdminAssignmentBySupervisor(supervisorId)
            .orElseThrow(() -> new EntityNotFoundException("No primary admin assignment found for supervisor: " + supervisorId));
        
        currentAssignment.endAssignment(LocalDate.now());
        assignmentRepository.save(currentAssignment);
        
        // Create new assignment with same settings
        return assignSupervisorToAdmin(
            newAdminId,
            supervisorId,
            currentAssignment.getRegion(),
            currentAssignment.getAuthorityLevel(),
            currentAssignment.getMaxSchoolsOversight(),
            true, // New assignment becomes primary
            reassignmentReason,
            reassignedByUserId
        );
    }
    
    /**
     * Add secondary admin assignment (supervisor can have multiple admins)
     */
    public AdminSupervisor addSecondaryAdminAssignment(UUID adminId,
                                                       UUID supervisorId,
                                                       String region,
                                                       Integer authorityLevel,
                                                       String assignmentNotes,
                                                       UUID assignedByUserId) {
        logger.info("Adding secondary admin assignment: admin {} for supervisor {}", adminId, supervisorId);
        
        return assignSupervisorToAdmin(
            adminId,
            supervisorId,
            region,
            authorityLevel,
            null, // No school oversight limit for secondary
            false, // Not primary admin
            assignmentNotes,
            assignedByUserId
        );
    }
    
    /**
     * End a supervisor assignment
     */
    public void endAssignment(UUID assignmentId, String endReason) {
        logger.info("Ending supervisor assignment {}", assignmentId);
        
        AdminSupervisor assignment = assignmentRepository.findById(assignmentId)
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
     * End all assignments for a supervisor (when supervisor leaves, etc.)
     */
    public void endAllAssignmentsForSupervisor(UUID supervisorId, String reason) {
        logger.info("Ending all assignments for supervisor {}", supervisorId);
        
        List<AdminSupervisor> assignments = 
            assignmentRepository.findActiveAssignmentsBySupervisor(supervisorId);
        
        int endedCount = 0;
        for (AdminSupervisor assignment : assignments) {
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
        
        logger.info("Ended {} active assignments for supervisor {}", endedCount, supervisorId);
    }
    
    // ========== Query Methods ==========
    
    /**
     * Find primary admin assignment for a supervisor
     */
    public Optional<AdminSupervisor> findPrimaryAdminAssignmentBySupervisor(UUID supervisorId) {
        return assignmentRepository.findPrimaryAdminAssignmentBySupervisor(supervisorId);
    }
    
    /**
     * Get all supervisors managed by an admin
     */
    public List<AdminSupervisor> getSupervisorsByAdmin(UUID adminId) {
        return assignmentRepository.findSupervisorsByAdmin(adminId);
    }
    
    /**
     * Get supervisors by admin and region
     */
    public List<AdminSupervisor> getSupervisorsByAdminAndRegion(UUID adminId, String region) {
        return assignmentRepository.findSupervisorsByAdminAndRegion(adminId, region);
    }
    
    /**
     * Get all assignments in a region
     */
    public List<AdminSupervisor> getAssignmentsByRegion(String region) {
        return assignmentRepository.findAssignmentsByRegion(region);
    }
    
    /**
     * Get assignment history for a supervisor
     */
    public List<AdminSupervisor> getAssignmentHistoryBySupervisor(UUID supervisorId) {
        return assignmentRepository.findAssignmentHistoryBySupervisor(supervisorId);
    }
    
    /**
     * Get all active assignments with pagination
     */
    public Page<AdminSupervisor> getActiveAssignments(Pageable pageable) {
        return assignmentRepository.findActiveAssignments(pageable);
    }
    
    /**
     * Search assignments by admin, supervisor name, or region
     */
    public Page<AdminSupervisor> searchAssignments(String searchTerm, Pageable pageable) {
        return assignmentRepository.searchAssignments(searchTerm, pageable);
    }
    
    // ========== Business Logic Methods ==========
    
    /**
     * Find unassigned supervisors
     */
    public List<User> findUnassignedSupervisors() {
        return userRepository.findActiveByUserType(UserType.SUPERVISOR, UserStatus.ACTIVE).stream()
            .filter(supervisor -> findPrimaryAdminAssignmentBySupervisor(supervisor.getId()).isEmpty())
            .toList();
    }
    
    /**
     * Find supervisors by authority level
     */
    public List<AdminSupervisor> findSupervisorsByAuthorityLevel(Integer level) {
        return assignmentRepository.findByAuthorityLevel(level);
    }
    
    /**
     * Find high authority assignments
     */
    public List<AdminSupervisor> findHighAuthorityAssignments() {
        return assignmentRepository.findHighAuthorityAssignments();
    }
    
    /**
     * Check if a supervisor can be assigned to an admin
     */
    public boolean canAssignSupervisor(UUID adminId, UUID supervisorId, boolean asPrimary) {
        try {
            // Check if admin exists and is valid
            User admin = userRepository.findById(adminId).orElse(null);
            if (admin == null || !admin.isAdmin()) {
                return false;
            }
            
            // Check if supervisor exists and is valid
            User supervisor = userRepository.findById(supervisorId).orElse(null);
            if (supervisor == null || !supervisor.isSupervisor()) {
                return false;
            }
            
            if (!supervisor.isVerified()) {
                return false;
            }
            
            // Check if this specific pair already exists
            if (assignmentRepository.existsActiveAssignment(adminId, supervisorId)) {
                return false;
            }
            
            // If trying to assign as primary, check supervisor doesn't already have primary admin
            if (asPrimary) {
                return assignmentRepository.findPrimaryAdminAssignmentBySupervisor(supervisorId).isEmpty();
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error checking if supervisor can be assigned", e);
            return false;
        }
    }
    
    /**
     * Get admin's workload (number of assigned supervisors)
     */
    public int getAdminWorkload(UUID adminId) {
        return assignmentRepository.findSupervisorsByAdmin(adminId).size();
    }
    
    /**
     * Find admins with capacity for more supervisors
     */
    public List<User> findAdminsWithCapacity(int maxSupervisorsPerAdmin) {
        List<User> allAdmins = userRepository.findAllAdmins(Arrays.asList(UserType.ADMIN, UserType.SUPER_ADMIN));
        
        return allAdmins.stream()
            .filter(admin -> getAdminWorkload(admin.getId()) < maxSupervisorsPerAdmin)
            .toList();
    }
    
    /**
     * Find supervisors at or near capacity
     */
    public List<AdminSupervisor> findSupervisorsNearCapacity(double capacityThreshold) {
        return assignmentRepository.findSupervisorsNearCapacity(capacityThreshold);
    }
    
    /**
     * Find supervisors with available capacity
     */
    public List<AdminSupervisor> findSupervisorsWithCapacity() {
        return assignmentRepository.findSupervisorsWithCapacity();
    }
    
    /**
     * Update assignment authority level
     */
    public AdminSupervisor updateAuthorityLevel(UUID assignmentId, Integer newLevel) {
        logger.info("Updating assignment {} authority level to {}", assignmentId, newLevel);
        
        AdminSupervisor assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new EntityNotFoundException("Assignment not found: " + assignmentId));
        
        assignment.setAuthorityLevel(newLevel);
        return assignmentRepository.save(assignment);
    }
    
    /**
     * Update coverage statistics for an assignment
     */
    public AdminSupervisor updateCoverageStatistics(UUID assignmentId, int schools, int technicians) {
        logger.info("Updating assignment {} coverage: {} schools, {} technicians", 
                   assignmentId, schools, technicians);
        
        AdminSupervisor assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new EntityNotFoundException("Assignment not found: " + assignmentId));
        
        assignment.updateCoverageStats(schools, technicians);
        return assignmentRepository.save(assignment);
    }
    
    /**
     * Update efficiency rating
     */
    public AdminSupervisor updateEfficiencyRating(UUID assignmentId, Double rating) {
        logger.info("Updating assignment {} efficiency rating to {}", assignmentId, rating);
        
        AdminSupervisor assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new EntityNotFoundException("Assignment not found: " + assignmentId));
        
        assignment.setEfficiencyRating(rating);
        return assignmentRepository.save(assignment);
    }
    
    /**
     * Update assignment notes
     */
    public AdminSupervisor updateAssignmentNotes(UUID assignmentId, String notes) {
        logger.info("Updating assignment {} notes", assignmentId);
        
        AdminSupervisor assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new EntityNotFoundException("Assignment not found: " + assignmentId));
        
        assignment.setAssignmentNotes(notes);
        return assignmentRepository.save(assignment);
    }
    
    /**
     * Get all active regions
     */
    public List<String> getActiveRegions() {
        return assignmentRepository.findActiveRegions();
    }
    
    /**
     * Find maximum authority supervisors in a region
     */
    public List<AdminSupervisor> findMaxAuthorityInRegion(String region) {
        return assignmentRepository.findMaxAuthorityInRegion(region);
    }
    
    // ========== Statistics and Reporting ==========
    
    /**
     * Get assignment statistics
     */
    public Object getAssignmentStatistics() {
        return assignmentRepository.getAssignmentStatistics();
    }
    
    /**
     * Get admin performance metrics
     */
    public Object getAdminPerformanceMetrics(UUID adminId) {
        return assignmentRepository.getAdminPerformanceMetrics(adminId);
    }
    
    /**
     * Find top performing assignments
     */
    public List<AdminSupervisor> getTopPerformingAssignments(int limit) {
        return assignmentRepository.findTopPerformingAssignments(
            org.springframework.data.domain.PageRequest.of(0, limit));
    }
    
    /**
     * Find underperforming assignments
     */
    public List<AdminSupervisor> getUnderperformingAssignments(double threshold) {
        return assignmentRepository.findUnderperformingAssignments(threshold);
    }
    
    /**
     * Count assignments by admin
     */
    public List<Object[]> countAssignmentsByAdmin() {
        return assignmentRepository.countAssignmentsByAdmin(Arrays.asList(UserType.ADMIN, UserType.SUPER_ADMIN));
    }
    
    /**
     * Count assignments by region
     */
    public List<Object[]> countAssignmentsByRegion() {
        return assignmentRepository.countAssignmentsByRegion();
    }
    
    /**
     * Count assignments by authority level
     */
    public List<Object[]> countAssignmentsByAuthorityLevel() {
        return assignmentRepository.countAssignmentsByAuthorityLevel();
    }
    
    /**
     * Calculate total management span for an admin
     */
    public Object calculateManagementSpan(UUID adminId) {
        return assignmentRepository.calculateManagementSpan(adminId);
    }
    
    /**
     * Bulk update coverage statistics for all assignments
     * This would typically be called by a scheduled job
     */
    @Transactional
    public void updateAllCoverageStatistics() {
        logger.info("Starting bulk update of coverage statistics");
        
        List<AdminSupervisor> activeAssignments = assignmentRepository.findActiveAssignments(
            org.springframework.data.domain.Pageable.unpaged()).getContent();
        
        for (AdminSupervisor assignment : activeAssignments) {
            try {
                // Calculate actual counts from repositories
                UUID supervisorId = assignment.getSupervisor().getId();
                long schoolCount = supervisorSchoolRepository.countBySupervisorIdAndIsActiveTrue(supervisorId);
                long technicianCount = supervisorTechnicianRepository.countBySupervisorIdAndIsActiveTrue(supervisorId);
                
                assignment.updateCoverageStats((int) schoolCount, (int) technicianCount);
                assignmentRepository.save(assignment);
                
            } catch (Exception e) {
                logger.error("Error updating coverage stats for assignment {}", assignment.getId(), e);
            }
        }
        
        logger.info("Completed bulk update of coverage statistics for {} assignments", 
                   activeAssignments.size());
    }
}