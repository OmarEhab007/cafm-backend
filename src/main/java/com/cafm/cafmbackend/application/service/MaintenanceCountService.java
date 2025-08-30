package com.cafm.cafmbackend.application.service;

import com.cafm.cafmbackend.infrastructure.persistence.entity.Company;
import com.cafm.cafmbackend.infrastructure.persistence.entity.MaintenanceCount;
import com.cafm.cafmbackend.infrastructure.persistence.entity.MaintenanceCount.MaintenanceCountStatus;
import com.cafm.cafmbackend.infrastructure.persistence.entity.School;
import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import com.cafm.cafmbackend.infrastructure.persistence.repository.CompanyRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.MaintenanceCountRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.SchoolRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.UserRepository;
import com.cafm.cafmbackend.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing maintenance counts with entity operations.
 * Handles complex maintenance inspection data for schools.
 */
@Service
@Transactional
public class MaintenanceCountService {
    
    private static final Logger logger = LoggerFactory.getLogger(MaintenanceCountService.class);
    
    private final MaintenanceCountRepository maintenanceCountRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    
    public MaintenanceCountService(
            MaintenanceCountRepository maintenanceCountRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            CompanyRepository companyRepository) {
        this.maintenanceCountRepository = maintenanceCountRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
    }
    
    // ========== CRUD Operations ==========
    
    /**
     * Create a new maintenance count.
     */
    @Transactional
    public MaintenanceCount createMaintenanceCount(UUID schoolId, UUID supervisorId, UUID companyId) {
        logger.info("Creating new maintenance count for school: {}", schoolId);
        
        // Fetch related entities
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));
        
        User supervisor = userRepository.findById(supervisorId)
            .orElseThrow(() -> new ResourceNotFoundException("Supervisor not found: " + supervisorId));
        
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
        
        // Validate supervisor
        if (!supervisor.isSupervisor()) {
            throw new IllegalArgumentException("User is not a supervisor: " + supervisorId);
        }
        
        // Create entity using builder
        MaintenanceCount entity = MaintenanceCount.builder()
            .school(school)
            .supervisor(supervisor)
            .company(company)
            .schoolName(school.getName())
            .status(MaintenanceCountStatus.DRAFT)
            .build();
        
        entity = maintenanceCountRepository.save(entity);
        logger.info("Created maintenance count with ID: {}", entity.getId());
        
        return entity;
    }
    
    /**
     * Get maintenance count by ID.
     */
    public MaintenanceCount getMaintenanceCountById(UUID id) {
        return maintenanceCountRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Maintenance count not found: " + id));
    }
    
    /**
     * Update maintenance count item data.
     */
    @Transactional
    public MaintenanceCount updateItemCounts(UUID id, Map<String, Integer> itemCounts) {
        logger.info("Updating item counts for maintenance count: {}", id);
        
        MaintenanceCount entity = getMaintenanceCountById(id);
        
        // Only allow updates in DRAFT status
        if (entity.getStatus() != MaintenanceCountStatus.DRAFT) {
            throw new IllegalStateException("Can only update maintenance counts in DRAFT status");
        }
        
        entity.setItemCounts(itemCounts);
        entity = maintenanceCountRepository.save(entity);
        
        logger.info("Updated item counts for maintenance count: {}", id);
        return entity;
    }
    
    /**
     * Update text answers.
     */
    @Transactional
    public MaintenanceCount updateTextAnswers(UUID id, Map<String, String> textAnswers) {
        logger.info("Updating text answers for maintenance count: {}", id);
        
        MaintenanceCount entity = getMaintenanceCountById(id);
        
        if (entity.getStatus() != MaintenanceCountStatus.DRAFT) {
            throw new IllegalStateException("Can only update maintenance counts in DRAFT status");
        }
        
        entity.setTextAnswers(textAnswers);
        entity = maintenanceCountRepository.save(entity);
        
        return entity;
    }
    
    /**
     * Update yes/no answers.
     */
    @Transactional
    public MaintenanceCount updateYesNoAnswers(UUID id, Map<String, Boolean> yesNoAnswers) {
        logger.info("Updating yes/no answers for maintenance count: {}", id);
        
        MaintenanceCount entity = getMaintenanceCountById(id);
        
        if (entity.getStatus() != MaintenanceCountStatus.DRAFT) {
            throw new IllegalStateException("Can only update maintenance counts in DRAFT status");
        }
        
        entity.setYesNoAnswers(yesNoAnswers);
        entity = maintenanceCountRepository.save(entity);
        
        return entity;
    }
    
    /**
     * Update fire safety data.
     */
    @Transactional
    public MaintenanceCount updateFireSafetyData(UUID id, 
                                                Map<String, String> alarmPanelData,
                                                Map<String, String> conditionData,
                                                Map<String, String> expiryDates) {
        logger.info("Updating fire safety data for maintenance count: {}", id);
        
        MaintenanceCount entity = getMaintenanceCountById(id);
        
        if (entity.getStatus() != MaintenanceCountStatus.DRAFT) {
            throw new IllegalStateException("Can only update maintenance counts in DRAFT status");
        }
        
        if (alarmPanelData != null) {
            entity.setFireSafetyAlarmPanelData(alarmPanelData);
        }
        if (conditionData != null) {
            entity.setFireSafetyConditionOnlyData(conditionData);
        }
        if (expiryDates != null) {
            entity.setFireSafetyExpiryDates(expiryDates);
        }
        
        entity = maintenanceCountRepository.save(entity);
        
        return entity;
    }
    
    /**
     * Add photos to a section.
     */
    @Transactional
    public MaintenanceCount addSectionPhotos(UUID id, String section, List<String> photoUrls) {
        logger.info("Adding {} photos to section '{}' for maintenance count: {}", 
                   photoUrls.size(), section, id);
        
        MaintenanceCount entity = getMaintenanceCountById(id);
        
        if (entity.getStatus() != MaintenanceCountStatus.DRAFT) {
            throw new IllegalStateException("Can only update maintenance counts in DRAFT status");
        }
        
        Map<String, List<String>> currentPhotos = entity.getSectionPhotos();
        currentPhotos.put(section, photoUrls);
        entity.setSectionPhotos(currentPhotos);
        
        entity = maintenanceCountRepository.save(entity);
        
        return entity;
    }
    
    /**
     * Submit maintenance count for review.
     */
    @Transactional
    public MaintenanceCount submitMaintenanceCount(UUID id) {
        logger.info("Submitting maintenance count: {}", id);
        
        MaintenanceCount entity = getMaintenanceCountById(id);
        
        if (entity.getStatus() != MaintenanceCountStatus.DRAFT) {
            throw new IllegalStateException("Can only submit maintenance counts in DRAFT status");
        }
        
        entity.setStatus(MaintenanceCountStatus.SUBMITTED);
        entity.setSubmittedAt(LocalDateTime.now());
        entity = maintenanceCountRepository.save(entity);
        
        logger.info("Submitted maintenance count: {}", id);
        return entity;
    }
    
    /**
     * Review maintenance count.
     */
    @Transactional
    public MaintenanceCount reviewMaintenanceCount(UUID id) {
        logger.info("Reviewing maintenance count: {}", id);
        
        MaintenanceCount entity = getMaintenanceCountById(id);
        
        if (entity.getStatus() != MaintenanceCountStatus.SUBMITTED) {
            throw new IllegalStateException("Can only review maintenance counts in SUBMITTED status");
        }
        
        entity.setStatus(MaintenanceCountStatus.REVIEWED);
        entity = maintenanceCountRepository.save(entity);
        
        logger.info("Reviewed maintenance count: {}", id);
        return entity;
    }
    
    /**
     * Complete maintenance count.
     */
    @Transactional
    public MaintenanceCount completeMaintenanceCount(UUID id) {
        logger.info("Completing maintenance count: {}", id);
        
        MaintenanceCount entity = getMaintenanceCountById(id);
        
        if (entity.getStatus() != MaintenanceCountStatus.REVIEWED) {
            throw new IllegalStateException("Can only complete maintenance counts in REVIEWED status");
        }
        
        entity.setStatus(MaintenanceCountStatus.COMPLETED);
        entity = maintenanceCountRepository.save(entity);
        
        logger.info("Completed maintenance count: {}", id);
        return entity;
    }
    
    /**
     * Delete maintenance count (soft delete).
     */
    @Transactional
    public void deleteMaintenanceCount(UUID id) {
        logger.info("Deleting maintenance count: {}", id);
        
        MaintenanceCount entity = getMaintenanceCountById(id);
        
        // Only allow deletion of DRAFT status
        if (entity.getStatus() != MaintenanceCountStatus.DRAFT) {
            throw new IllegalStateException("Can only delete maintenance counts in DRAFT status");
        }
        
        entity.setDeletedAt(LocalDateTime.now());
        maintenanceCountRepository.save(entity);
        
        logger.info("Soft deleted maintenance count: {}", id);
    }
    
    // ========== Query Methods ==========
    
    /**
     * Get all maintenance counts with pagination.
     */
    public Page<MaintenanceCount> getMaintenanceCounts(Pageable pageable) {
        return maintenanceCountRepository.findAll(pageable);
    }
    
    /**
     * Get maintenance counts by school.
     */
    public Page<MaintenanceCount> getMaintenanceCountsBySchool(UUID schoolId, Pageable pageable) {
        return maintenanceCountRepository.findBySchoolIdAndDeletedAtIsNull(schoolId, pageable);
    }
    
    /**
     * Get maintenance counts by supervisor.
     */
    public Page<MaintenanceCount> getMaintenanceCountsBySupervisor(UUID supervisorId, Pageable pageable) {
        return maintenanceCountRepository.findBySupervisorIdAndDeletedAtIsNull(supervisorId, pageable);
    }
    
    /**
     * Get maintenance counts by status.
     */
    public Page<MaintenanceCount> getMaintenanceCountsByStatus(MaintenanceCountStatus status, Pageable pageable) {
        return maintenanceCountRepository.findByStatusAndDeletedAtIsNull(status, pageable);
    }
    
    /**
     * Get latest maintenance count for a school.
     */
    public MaintenanceCount getLatestMaintenanceCountForSchool(UUID schoolId) {
        return maintenanceCountRepository.findFirstBySchoolIdAndDeletedAtIsNullOrderByCreatedAtDesc(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No maintenance count found for school: " + schoolId));
    }
    
    // ========== Statistics Methods ==========
    
    /**
     * Count maintenance counts by status.
     */
    public List<Object[]> countByStatus(UUID companyId) {
        return maintenanceCountRepository.countByStatusForCompany(companyId);
    }
    
    /**
     * Get maintenance count statistics for a company.
     */
    @Transactional(readOnly = true)
    public MaintenanceCountStatistics getStatistics(UUID companyId) {
        MaintenanceCountStatistics stats = new MaintenanceCountStatistics();
        
        Page<MaintenanceCount> countsPage = maintenanceCountRepository.findByCompanyIdAndDeletedAtIsNull(companyId, Pageable.unpaged());
        List<MaintenanceCount> counts = countsPage.getContent();
        
        stats.totalCounts = counts.size();
        stats.draftCounts = counts.stream()
            .filter(mc -> mc.getStatus() == MaintenanceCountStatus.DRAFT)
            .count();
        stats.submittedCounts = counts.stream()
            .filter(mc -> mc.getStatus() == MaintenanceCountStatus.SUBMITTED)
            .count();
        stats.reviewedCounts = counts.stream()
            .filter(mc -> mc.getStatus() == MaintenanceCountStatus.REVIEWED)
            .count();
        stats.completedCounts = counts.stream()
            .filter(mc -> mc.getStatus() == MaintenanceCountStatus.COMPLETED)
            .count();
        
        return stats;
    }
    
    // ========== Utility Methods ==========
    
    /**
     * Check if a maintenance count exists for a school within a time period.
     */
    public boolean existsForSchoolInPeriod(UUID schoolId, LocalDateTime startDate, LocalDateTime endDate) {
        // TODO: Add custom query method to repository when needed
        Page<MaintenanceCount> countsPage = maintenanceCountRepository.findBySchoolIdAndDeletedAtIsNull(schoolId, Pageable.unpaged());
        List<MaintenanceCount> counts = countsPage.getContent();
        return counts.stream()
            .anyMatch(mc -> mc.getCreatedAt() != null && 
                          mc.getCreatedAt().isAfter(startDate) && 
                          mc.getCreatedAt().isBefore(endDate));
    }
    
    /**
     * Clone a maintenance count (for creating new inspection based on previous).
     */
    @Transactional
    public MaintenanceCount cloneMaintenanceCount(UUID sourceId, UUID supervisorId) {
        logger.info("Cloning maintenance count: {}", sourceId);
        
        MaintenanceCount source = getMaintenanceCountById(sourceId);
        User supervisor = userRepository.findById(supervisorId)
            .orElseThrow(() -> new ResourceNotFoundException("Supervisor not found: " + supervisorId));
        
        MaintenanceCount clone = MaintenanceCount.builder()
            .school(source.getSchool())
            .supervisor(supervisor)
            .company(source.getCompany())
            .schoolName(source.getSchoolName())
            .status(MaintenanceCountStatus.DRAFT)
            .itemCounts(source.getItemCounts())
            .textAnswers(source.getTextAnswers())
            .yesNoAnswers(source.getYesNoAnswers())
            .yesNoWithCounts(source.getYesNoWithCounts())
            .surveyAnswers(source.getSurveyAnswers())
            .maintenanceNotes(source.getMaintenanceNotes())
            .build();
        
        clone = maintenanceCountRepository.save(clone);
        logger.info("Created clone with ID: {}", clone.getId());
        
        return clone;
    }
    
    // ========== Inner Classes ==========
    
    /**
     * Maintenance count statistics DTO.
     */
    public static class MaintenanceCountStatistics {
        public long totalCounts;
        public long draftCounts;
        public long submittedCounts;
        public long reviewedCounts;
        public long completedCounts;
    }
}