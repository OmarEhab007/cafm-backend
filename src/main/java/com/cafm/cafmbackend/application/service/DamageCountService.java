package com.cafm.cafmbackend.application.service;

import com.cafm.cafmbackend.infrastructure.persistence.entity.Company;
import com.cafm.cafmbackend.infrastructure.persistence.entity.DamageCount;
import com.cafm.cafmbackend.infrastructure.persistence.entity.DamageCount.DamageCountStatus;
import com.cafm.cafmbackend.infrastructure.persistence.entity.School;
import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import com.cafm.cafmbackend.infrastructure.persistence.repository.CompanyRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.DamageCountRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.SchoolRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.UserRepository;
import com.cafm.cafmbackend.dto.damage.DamageCountSimplifiedRequest;
import com.cafm.cafmbackend.dto.damage.DamageCountSimplifiedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing damage counts with entity-DTO conversion.
 * This replaces the mapper layer with direct conversion in the service.
 */
@Service
@Transactional
public class DamageCountService {
    
    private static final Logger logger = LoggerFactory.getLogger(DamageCountService.class);
    
    private final DamageCountRepository damageCountRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    
    public DamageCountService(
            DamageCountRepository damageCountRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            CompanyRepository companyRepository) {
        this.damageCountRepository = damageCountRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
    }
    
    // ========== DTO Conversion Methods ==========
    
    /**
     * Convert DamageCount entity to simplified response DTO.
     * Only includes fields that actually exist in the entity.
     */
    private DamageCountSimplifiedResponse toResponse(DamageCount entity) {
        if (entity == null) return null;
        
        return DamageCountSimplifiedResponse.builder()
            .id(entity.getId())
            .schoolId(entity.getSchool() != null ? entity.getSchool().getId() : null)
            .schoolName(entity.getSchoolName())
            .supervisorId(entity.getSupervisor() != null ? entity.getSupervisor().getId() : null)
            .supervisorName(entity.getSupervisor() != null ? entity.getSupervisor().getFullName() : null)
            .companyId(entity.getCompany() != null ? entity.getCompany().getId() : null)
            .status(entity.getStatus())
            .priority(entity.getPriority())
            .itemCounts(entity.getItemCounts())
            .sectionPhotos(entity.getSectionPhotos())
            .totalItemsCount(entity.getTotalItemsCount())
            .estimatedRepairCost(entity.getEstimatedRepairCost())
            .repairNotes(entity.getRepairNotes())
            .submittedAt(entity.getSubmittedAt())
            .reviewedAt(entity.getReviewedAt())
            .reviewedBy(entity.getReviewedBy())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
    
    /**
     * Convert request DTO to DamageCount entity.
     * Sets up a new damage count with proper defaults.
     */
    private DamageCount fromRequest(DamageCountSimplifiedRequest request, 
                                   School school, User supervisor, Company company) {
        DamageCount entity = new DamageCount();
        
        // Set relationships
        entity.setSchool(school);
        entity.setSupervisor(supervisor);
        entity.setCompany(company);
        entity.setSchoolName(school.getName());
        
        // Set data fields
        entity.setItemCounts(request.itemCounts());
        entity.setSectionPhotos(request.sectionPhotos());
        entity.setPriority(request.priority() != null ? request.priority() : DamageCount.PriorityLevel.MEDIUM);
        entity.setEstimatedRepairCost(request.estimatedRepairCost());
        entity.setRepairNotes(request.repairNotes());
        
        // Set defaults
        entity.setStatus(DamageCountStatus.DRAFT);
        entity.calculateTotalItems();
        
        return entity;
    }
    
    // ========== CRUD Operations ==========
    
    /**
     * Create a new damage count.
     */
    @Transactional
    public DamageCountSimplifiedResponse createDamageCount(DamageCountSimplifiedRequest request) {
        logger.info("Creating new damage count for school: {}", request.schoolId());
        
        // Fetch related entities
        School school = schoolRepository.findById(request.schoolId())
            .orElseThrow(() -> new IllegalArgumentException("School not found: " + request.schoolId()));
        
        User supervisor = userRepository.findById(request.supervisorId())
            .orElseThrow(() -> new IllegalArgumentException("Supervisor not found: " + request.supervisorId()));
        
        Company company = companyRepository.findById(request.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + request.companyId()));
        
        // Convert and save
        DamageCount entity = fromRequest(request, school, supervisor, company);
        entity = damageCountRepository.save(entity);
        
        logger.info("Created damage count with ID: {}", entity.getId());
        return toResponse(entity);
    }
    
    /**
     * Get damage count by ID.
     */
    public DamageCountSimplifiedResponse getDamageCountById(UUID id) {
        DamageCount entity = damageCountRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Damage count not found: " + id));
        return toResponse(entity);
    }
    
    /**
     * Update existing damage count.
     */
    @Transactional
    public DamageCountSimplifiedResponse updateDamageCount(UUID id, DamageCountSimplifiedRequest request) {
        logger.info("Updating damage count with ID: {}", id);
        
        DamageCount entity = damageCountRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Damage count not found: " + id));
        
        // Only update if in DRAFT status
        if (entity.getStatus() != DamageCountStatus.DRAFT) {
            throw new IllegalStateException("Can only update damage counts in DRAFT status");
        }
        
        // Update fields
        if (request.itemCounts() != null) {
            entity.setItemCounts(request.itemCounts());
            entity.calculateTotalItems();
        }
        if (request.sectionPhotos() != null) {
            entity.setSectionPhotos(request.sectionPhotos());
        }
        if (request.priority() != null) {
            entity.setPriority(request.priority());
        }
        if (request.estimatedRepairCost() != null) {
            entity.setEstimatedRepairCost(request.estimatedRepairCost());
        }
        if (request.repairNotes() != null) {
            entity.setRepairNotes(request.repairNotes());
        }
        
        entity = damageCountRepository.save(entity);
        
        logger.info("Updated damage count with ID: {}", id);
        return toResponse(entity);
    }
    
    /**
     * Get paginated list of damage counts.
     */
    public Page<DamageCountSimplifiedResponse> getDamageCounts(Pageable pageable) {
        return damageCountRepository.findAll(pageable)
            .map(this::toResponse);
    }
    
    /**
     * Get damage counts by school.
     */
    public Page<DamageCountSimplifiedResponse> getDamageCountsBySchool(UUID schoolId, Pageable pageable) {
        return damageCountRepository.findBySchoolIdAndDeletedAtIsNull(schoolId, pageable)
            .map(this::toResponse);
    }
    
    /**
     * Get damage counts by supervisor.
     */
    public Page<DamageCountSimplifiedResponse> getDamageCountsBySupervisor(UUID supervisorId, Pageable pageable) {
        return damageCountRepository.findBySupervisorIdAndDeletedAtIsNull(supervisorId, pageable)
            .map(this::toResponse);
    }
    
    // ========== Status Management ==========
    
    /**
     * Submit a damage count for review.
     */
    @Transactional
    public DamageCountSimplifiedResponse submitDamageCount(UUID id) {
        logger.info("Submitting damage count with ID: {}", id);
        
        DamageCount entity = damageCountRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Damage count not found: " + id));
        
        entity.submit();
        entity = damageCountRepository.save(entity);
        
        logger.info("Submitted damage count with ID: {}", id);
        return toResponse(entity);
    }
    
    /**
     * Review a damage count.
     */
    @Transactional
    public DamageCountSimplifiedResponse reviewDamageCount(UUID id, UUID reviewerId) {
        logger.info("Reviewing damage count with ID: {} by reviewer: {}", id, reviewerId);
        
        DamageCount entity = damageCountRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Damage count not found: " + id));
        
        entity.review(reviewerId);
        entity = damageCountRepository.save(entity);
        
        logger.info("Reviewed damage count with ID: {}", id);
        return toResponse(entity);
    }
    
    /**
     * Complete a damage count.
     */
    @Transactional
    public DamageCountSimplifiedResponse completeDamageCount(UUID id) {
        logger.info("Completing damage count with ID: {}", id);
        
        DamageCount entity = damageCountRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Damage count not found: " + id));
        
        entity.complete();
        entity = damageCountRepository.save(entity);
        
        logger.info("Completed damage count with ID: {}", id);
        return toResponse(entity);
    }
    
    /**
     * Delete damage count (soft delete).
     */
    @Transactional
    public void deleteDamageCount(UUID id) {
        logger.info("Deleting damage count with ID: {}", id);
        
        DamageCount entity = damageCountRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Damage count not found: " + id));
        
        // Only allow deletion of DRAFT status
        if (entity.getStatus() != DamageCountStatus.DRAFT) {
            throw new IllegalStateException("Can only delete damage counts in DRAFT status");
        }
        
        entity.setDeletedAt(LocalDateTime.now());
        damageCountRepository.save(entity);
        
        logger.info("Soft deleted damage count with ID: {}", id);
    }
    
    // ========== Photo Management ==========
    
    /**
     * Add photos to a section.
     */
    @Transactional
    public DamageCountSimplifiedResponse addSectionPhotos(UUID id, String section, List<String> photoUrls) {
        logger.info("Adding {} photos to section '{}' for damage count: {}", photoUrls.size(), section, id);
        
        DamageCount entity = damageCountRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Damage count not found: " + id));
        
        entity.addSectionPhotos(section, photoUrls);
        entity = damageCountRepository.save(entity);
        
        return toResponse(entity);
    }
    
    /**
     * Update item count for a specific item.
     */
    @Transactional
    public DamageCountSimplifiedResponse updateItemCount(UUID id, String itemKey, Integer count) {
        logger.info("Updating item count for '{}' to {} for damage count: {}", itemKey, count, id);
        
        DamageCount entity = damageCountRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Damage count not found: " + id));
        
        entity.addItemCount(itemKey, count);
        entity = damageCountRepository.save(entity);
        
        return toResponse(entity);
    }
}