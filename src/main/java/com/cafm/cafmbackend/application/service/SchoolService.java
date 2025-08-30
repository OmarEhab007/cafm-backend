package com.cafm.cafmbackend.application.service;

import com.cafm.cafmbackend.infrastructure.persistence.entity.Company;
import com.cafm.cafmbackend.infrastructure.persistence.entity.School;
import com.cafm.cafmbackend.infrastructure.persistence.entity.SupervisorSchool;
import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import com.cafm.cafmbackend.infrastructure.persistence.repository.CompanyRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.SchoolRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.UserRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.ReportRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.WorkOrderRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.AssetRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.SupervisorSchoolRepository;
import com.cafm.cafmbackend.shared.enums.AssetCondition;
import com.cafm.cafmbackend.shared.enums.ReportStatus;
import com.cafm.cafmbackend.shared.enums.WorkOrderStatus;
import com.cafm.cafmbackend.dto.school.*;
import com.cafm.cafmbackend.shared.exception.DuplicateResourceException;
import com.cafm.cafmbackend.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for managing schools.
 * Handles school CRUD operations, supervisor assignments, and maintenance tracking.
 */
@Service
@Transactional
public class SchoolService {
    
    private static final Logger logger = LoggerFactory.getLogger(SchoolService.class);
    
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final ReportRepository reportRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AssetRepository assetRepository;
    private final SupervisorSchoolRepository supervisorSchoolRepository;
    
    public SchoolService(SchoolRepository schoolRepository,
                        UserRepository userRepository,
                        CompanyRepository companyRepository,
                        ReportRepository reportRepository,
                        WorkOrderRepository workOrderRepository,
                        AssetRepository assetRepository,
                        SupervisorSchoolRepository supervisorSchoolRepository) {
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.reportRepository = reportRepository;
        this.workOrderRepository = workOrderRepository;
        this.assetRepository = assetRepository;
        this.supervisorSchoolRepository = supervisorSchoolRepository;
    }
    
    // ========== School Management Methods ==========
    
    /**
     * Create a new school.
     */
    public School createSchool(School school, UUID companyId) {
        logger.info("Creating new school with code: {}", school.getCode());
        
        // Validate code uniqueness
        if (schoolRepository.existsByCode(school.getCode())) {
            throw new DuplicateResourceException("School", "code", school.getCode());
        }
        
        // Set company
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
        school.setCompany(company);
        
        // Set defaults
        if (school.getIsActive() == null) {
            school.setIsActive(true);
        }
        
        // Calculate initial maintenance score if not provided
        if (school.getMaintenanceScore() == null) {
            school.setMaintenanceScore(100); // Start with perfect score
        }
        
        // Set activity level
        if (school.getActivityLevel() == null) {
            school.setActivityLevel("LOW");
        }
        
        School savedSchool = schoolRepository.save(school);
        logger.info("School created successfully with ID: {}", savedSchool.getId());
        
        return savedSchool;
    }
    
    /**
     * Update an existing school.
     */
    public School updateSchool(UUID schoolId, School updatedSchool) {
        logger.info("Updating school: {}", schoolId);
        
        School existingSchool = findById(schoolId);
        
        // Update allowed fields
        if (updatedSchool.getName() != null) {
            existingSchool.setName(updatedSchool.getName());
        }
        if (updatedSchool.getNameAr() != null) {
            existingSchool.setNameAr(updatedSchool.getNameAr());
        }
        if (updatedSchool.getType() != null) {
            existingSchool.setType(updatedSchool.getType());
        }
        if (updatedSchool.getGender() != null) {
            existingSchool.setGender(updatedSchool.getGender());
        }
        if (updatedSchool.getAddress() != null) {
            existingSchool.setAddress(updatedSchool.getAddress());
        }
        if (updatedSchool.getCity() != null) {
            existingSchool.setCity(updatedSchool.getCity());
        }
        if (updatedSchool.getLatitude() != null) {
            existingSchool.setLatitude(updatedSchool.getLatitude());
        }
        if (updatedSchool.getLongitude() != null) {
            existingSchool.setLongitude(updatedSchool.getLongitude());
        }
        
        return schoolRepository.save(existingSchool);
    }
    
    /**
     * Soft delete a school.
     */
    public void deleteSchool(UUID schoolId) {
        logger.info("Soft deleting school: {}", schoolId);
        
        School school = findById(schoolId);
        school.setDeletedAt(LocalDateTime.now());
        school.setIsActive(false);
        
        schoolRepository.save(school);
    }
    
    /**
     * Activate a school.
     */
    public School activateSchool(UUID schoolId) {
        logger.info("Activating school: {}", schoolId);
        
        School school = findById(schoolId);
        school.setIsActive(true);
        
        return schoolRepository.save(school);
    }
    
    /**
     * Deactivate a school.
     */
    public School deactivateSchool(UUID schoolId) {
        logger.info("Deactivating school: {}", schoolId);
        
        School school = findById(schoolId);
        school.setIsActive(false);
        
        return schoolRepository.save(school);
    }
    
    // ========== Query Methods ==========
    
    /**
     * Find school by ID.
     */
    public School findById(UUID schoolId) {
        return schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));
    }
    
    /**
     * Find school by code.
     */
    public Optional<School> findByCode(String code) {
        return schoolRepository.findByCode(code);
    }
    
    /**
     * Find all schools with pagination.
     */
    public Page<School> findAll(Pageable pageable) {
        return schoolRepository.findAll(pageable);
    }
    
    /**
     * Find active schools.
     */
    public List<School> findActiveSchools() {
        return schoolRepository.findActiveSchools();
    }
    
    /**
     * Find schools by type.
     */
    public List<School> findByType(String type) {
        return schoolRepository.findByType(type);
    }
    
    /**
     * Find schools by gender.
     */
    public List<School> findByGender(String gender) {
        return schoolRepository.findByGender(gender);
    }
    
    /**
     * Find schools by city.
     */
    public List<School> findByCity(String city) {
        return schoolRepository.findByCity(city);
    }
    
    /**
     * Search schools by name or code.
     */
    public Page<School> searchSchools(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return schoolRepository.findAll(pageable);
        }
        return schoolRepository.searchSchools(searchTerm, pageable);
    }
    
    // ========== Spatial Queries ==========
    
    /**
     * Find schools within radius of a location.
     */
    public List<School> findSchoolsWithinRadius(Double latitude, Double longitude, Double radiusKm) {
        logger.debug("Finding schools within {}km of ({}, {})", radiusKm, latitude, longitude);
        return schoolRepository.findSchoolsWithinRadius(latitude, longitude, radiusKm);
    }
    
    /**
     * Find nearest schools to a location.
     */
    public List<School> findNearestSchools(Double latitude, Double longitude, int limit) {
        logger.debug("Finding {} nearest schools to ({}, {})", limit, latitude, longitude);
        return schoolRepository.findNearestSchools(latitude, longitude, limit);
    }
    
    // ========== Supervisor Assignment Methods ==========
    
    /**
     * Get schools assigned to a supervisor.
     */
    public List<School> getSchoolsBySupervisor(UUID supervisorId) {
        return schoolRepository.findSchoolsBySupervisor(supervisorId);
    }
    
    /**
     * Find unassigned schools.
     */
    public List<School> findUnassignedSchools() {
        return schoolRepository.findUnassignedSchools();
    }
    
    // ========== Maintenance Score Methods ==========
    
    /**
     * Update school maintenance score.
     */
    public void updateMaintenanceScore(UUID schoolId, Integer score) {
        logger.info("Updating maintenance score for school {} to {}", schoolId, score);
        
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Maintenance score must be between 0 and 100");
        }
        
        School school = findById(schoolId);
        school.setMaintenanceScore(score);
        
        // Update activity level based on score
        String activityLevel = calculateActivityLevel(score);
        school.setActivityLevel(activityLevel);
        
        schoolRepository.save(school);
    }
    
    /**
     * Calculate activity level based on maintenance score.
     */
    private String calculateActivityLevel(Integer score) {
        if (score >= 80) return "LOW";
        else if (score >= 60) return "MEDIUM";
        else if (score >= 40) return "HIGH";
        else return "CRITICAL";
    }
    
    /**
     * Get schools with critical maintenance needs.
     */
    public List<School> getSchoolsWithCriticalMaintenance() {
        return schoolRepository.findAll().stream()
            .filter(s -> s.getMaintenanceScore() != null && s.getMaintenanceScore() < 40)
            .collect(Collectors.toList());
    }
    
    // ========== Statistics Methods ==========
    
    /**
     * Get school statistics for a company.
     */
    @Transactional(readOnly = true)
    public SchoolStatistics getSchoolStatistics(UUID companyId) {
        SchoolStatistics stats = new SchoolStatistics();
        
        List<School> schools = schoolRepository.findAll().stream()
            .filter(s -> s.belongsToCompany(companyId))
            .collect(Collectors.toList());
        
        stats.totalSchools = schools.size();
        stats.activeSchools = schools.stream()
            .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
            .count();
        
        // Count by type
        stats.schoolsByType = schools.stream()
            .filter(s -> s.getType() != null)
            .collect(Collectors.groupingBy(School::getType, Collectors.counting()));
        
        // Count by gender
        stats.schoolsByGender = schools.stream()
            .filter(s -> s.getGender() != null)
            .collect(Collectors.groupingBy(School::getGender, Collectors.counting()));
        
        // Count by city
        stats.schoolsByCity = schools.stream()
            .filter(s -> s.getCity() != null)
            .collect(Collectors.groupingBy(School::getCity, Collectors.counting()));
        
        // Calculate average maintenance score
        stats.averageMaintenanceScore = schools.stream()
            .filter(s -> s.getMaintenanceScore() != null)
            .mapToInt(School::getMaintenanceScore)
            .average()
            .orElse(0.0);
        
        // Count by activity level
        stats.schoolsByActivityLevel = schools.stream()
            .filter(s -> s.getActivityLevel() != null)
            .collect(Collectors.groupingBy(School::getActivityLevel, Collectors.counting()));
        
        return stats;
    }
    
    /**
     * Get schools with pending reports.
     */
    public List<School> getSchoolsWithPendingReports() {
        List<ReportStatus> pendingStatuses = Arrays.asList(
            ReportStatus.SUBMITTED, 
            ReportStatus.IN_REVIEW, 
            ReportStatus.APPROVED, 
            ReportStatus.IN_PROGRESS, 
            ReportStatus.PENDING
        );
        return schoolRepository.findSchoolsWithPendingReports(pendingStatuses);
    }
    
    /**
     * Count reports by school.
     */
    public Map<UUID, Long> countReportsBySchool() {
        List<Object[]> results = schoolRepository.countReportsBySchool();
        Map<UUID, Long> counts = new HashMap<>();
        
        for (Object[] result : results) {
            UUID schoolId = (UUID) result[0];
            Long count = (Long) result[2];
            counts.put(schoolId, count);
        }
        
        return counts;
    }
    
    // ========== Utility Methods ==========
    
    /**
     * Check if school code is available.
     */
    public boolean isCodeAvailable(String code) {
        return !schoolRepository.existsByCode(code);
    }
    
    /**
     * Import schools from CSV or external source.
     */
    @Transactional
    public List<School> importSchools(List<School> schools, UUID companyId) {
        logger.info("Importing {} schools for company {}", schools.size(), companyId);
        
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
        
        List<School> savedSchools = new ArrayList<>();
        
        for (School school : schools) {
            // Skip if code already exists
            if (schoolRepository.existsByCode(school.getCode())) {
                logger.warn("Skipping school with duplicate code: {}", school.getCode());
                continue;
            }
            
            school.setCompany(company);
            school.setIsActive(true);
            
            if (school.getMaintenanceScore() == null) {
                school.setMaintenanceScore(100);
            }
            if (school.getActivityLevel() == null) {
                school.setActivityLevel("LOW");
            }
            
            savedSchools.add(schoolRepository.save(school));
        }
        
        logger.info("Successfully imported {} schools", savedSchools.size());
        return savedSchools;
    }
    
    /**
     * Bulk update school locations.
     */
    @Transactional
    public void updateSchoolLocations(Map<UUID, Location> locations) {
        logger.info("Updating locations for {} schools", locations.size());
        
        for (Map.Entry<UUID, Location> entry : locations.entrySet()) {
            School school = findById(entry.getKey());
            Location location = entry.getValue();
            
            school.setLatitude(location.latitude);
            school.setLongitude(location.longitude);
            
            if (location.address != null) {
                school.setAddress(location.address);
            }
            if (location.city != null) {
                school.setCity(location.city);
            }
            
            schoolRepository.save(school);
        }
    }
    
    // ========== DTO-Based Methods ==========
    
    /**
     * Create a new school from DTO.
     */
    public SchoolResponse createSchoolFromDto(SchoolCreateRequest request, UUID companyId) {
        logger.info("Creating new school with code: {}", request.code());
        
        // Validate code uniqueness
        if (schoolRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("School", "code", request.code());
        }
        
        // Get company
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
        
        // Create school entity from DTO
        School school = mapCreateRequestToEntity(request, company);
        
        School savedSchool = schoolRepository.save(school);
        logger.info("School created successfully with ID: {}", savedSchool.getId());
        
        return mapToResponse(savedSchool);
    }
    
    /**
     * Update an existing school from DTO.
     */
    public SchoolResponse updateSchoolFromDto(UUID schoolId, SchoolUpdateRequest request) {
        logger.info("Updating school: {}", schoolId);
        
        School existingSchool = findById(schoolId);
        
        // Apply updates from DTO
        mapUpdateRequestToEntity(request, existingSchool);
        
        School savedSchool = schoolRepository.save(existingSchool);
        return mapToResponse(savedSchool);
    }
    
    /**
     * Get school by ID as DTO.
     */
    public SchoolResponse getSchoolByIdAsDto(UUID schoolId) {
        School school = findById(schoolId);
        return mapToResponse(school);
    }
    
    /**
     * Get all schools with filtering and pagination.
     */
    public Page<SchoolListResponse> getAllSchoolsAsDto(Pageable pageable, String type, String gender, 
                                                      String city, String search, Boolean isActive) {
        
        Specification<School> spec = Specification.where(null);
        
        if (type != null && !type.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        
        if (gender != null && !gender.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("gender"), gender));
        }
        
        if (city != null && !city.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("city"), city));
        }
        
        if (isActive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), isActive));
        }
        
        if (search != null && !search.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("nameAr")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("code")), "%" + search.toLowerCase() + "%")
                )
            );
        }
        
        // Add soft delete filter
        spec = spec.and((root, query, cb) -> cb.isNull(root.get("deletedAt")));
        
        Page<School> schools = schoolRepository.findAll(spec, pageable);
        return schools.map(this::mapToListResponse);
    }
    
    /**
     * Get schools statistics as DTO.
     */
    public SchoolStatsResponse getSchoolsStatsAsDto(UUID companyId) {
        List<School> schools = schoolRepository.findAll().stream()
            .filter(s -> s.getCompany() != null && s.getCompany().getId().equals(companyId))
            .filter(s -> s.getDeletedAt() == null)
            .collect(Collectors.toList());
        
        return buildSchoolStatsResponse(schools);
    }
    
    // ========== DTO Mapping Methods ==========
    
    /**
     * Map SchoolCreateRequest to School entity.
     */
    private School mapCreateRequestToEntity(SchoolCreateRequest request, Company company) {
        School school = new School();
        school.setCode(request.code());
        school.setName(request.name());
        school.setNameAr(request.nameAr());
        school.setType(request.type());
        school.setGender(request.gender());
        school.setAddress(request.address());
        school.setCity(request.city());
        school.setLatitude(request.latitude());
        school.setLongitude(request.longitude());
        school.setMaintenanceScore(request.getEffectiveMaintenanceScore());
        school.setActivityLevel(request.getEffectiveActivityLevel());
        school.setCompany(company);
        school.setIsActive(true);
        
        return school;
    }
    
    /**
     * Map SchoolUpdateRequest to existing School entity.
     */
    private void mapUpdateRequestToEntity(SchoolUpdateRequest request, School school) {
        if (request.name() != null) {
            school.setName(request.name());
        }
        if (request.nameAr() != null) {
            school.setNameAr(request.nameAr());
        }
        if (request.type() != null) {
            school.setType(request.type());
        }
        if (request.gender() != null) {
            school.setGender(request.gender());
        }
        if (request.address() != null) {
            school.setAddress(request.address());
        }
        if (request.city() != null) {
            school.setCity(request.city());
        }
        if (request.latitude() != null) {
            school.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            school.setLongitude(request.longitude());
        }
        if (request.maintenanceScore() != null) {
            school.setMaintenanceScore(request.maintenanceScore());
        }
        if (request.activityLevel() != null) {
            school.setActivityLevel(request.activityLevel());
        }
        if (request.isActive() != null) {
            school.setIsActive(request.isActive());
        }
    }
    
    /**
     * Map School entity to SchoolResponse DTO.
     */
    private SchoolResponse mapToResponse(School school) {
        // Calculate actual counts from repositories
        long totalReports = reportRepository.countBySchoolIdAndDeletedAtIsNull(school.getId());
        long pendingReports = reportRepository.countBySchoolIdAndStatusAndDeletedAtIsNull(
            school.getId(), ReportStatus.PENDING);
        
        long activeWorkOrders = workOrderRepository.countByCompanyIdAndStatus(
            school.getCompany().getId(), WorkOrderStatus.IN_PROGRESS);
        
        long totalAssets = assetRepository.countBySchoolIdAndIsActiveTrue(school.getId());
        List<AssetCondition> poorConditions = Arrays.asList(
            AssetCondition.POOR, 
            AssetCondition.UNUSABLE
        );
        long assetsNeedingMaintenance = assetRepository.countAssetsNeedingMaintenanceBySchoolId(school.getId(), poorConditions);
        
        long assignedSupervisors = supervisorSchoolRepository.countBySchoolIdAndIsActiveTrue(school.getId());
        
        return new SchoolResponse(
            school.getId(),
            school.getCode(),
            school.getName(),
            school.getNameAr(),
            school.getType(),
            school.getGender(),
            school.getAddress(),
            school.getCity(),
            school.getLatitude(),
            school.getLongitude(),
            school.getIsActive(),
            school.getMaintenanceScore(),
            school.getActivityLevel(),
            school.getCompany() != null ? school.getCompany().getId() : null,
            school.getCreatedAt(),
            school.getUpdatedAt(),
            totalReports,
            pendingReports,
            activeWorkOrders,
            totalAssets,
            assetsNeedingMaintenance,
            assignedSupervisors
        );
    }
    
    /**
     * Map School entity to SchoolListResponse DTO.
     */
    private SchoolListResponse mapToListResponse(School school) {
        return new SchoolListResponse(
            school.getId(),
            school.getCode(),
            school.getName(),
            school.getNameAr(),
            school.getType(),
            school.getGender(),
            school.getCity(),
            school.getIsActive(),
            school.getMaintenanceScore(),
            school.getActivityLevel(),
            school.getCreatedAt(),
            0L, // totalReports
            0L, // pendingReports
            0L, // activeWorkOrders
            0L, // assignedSupervisors
            school.getLatitude() != null && school.getLongitude() != null
        );
    }
    
    /**
     * Build comprehensive school statistics response.
     */
    private SchoolStatsResponse buildSchoolStatsResponse(List<School> schools) {
        long totalSchools = schools.size();
        long activeSchools = schools.stream()
            .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
            .count();
        long inactiveSchools = totalSchools - activeSchools;
        
        // Count by type
        Map<String, Long> typeStats = schools.stream()
            .filter(s -> s.getType() != null)
            .collect(Collectors.groupingBy(School::getType, Collectors.counting()));
        
        long primarySchools = typeStats.getOrDefault("PRIMARY", 0L);
        long intermediateSchools = typeStats.getOrDefault("INTERMEDIATE", 0L);
        long secondarySchools = typeStats.getOrDefault("SECONDARY", 0L);
        long highSchools = typeStats.getOrDefault("HIGH_SCHOOL", 0L);
        long kindergartens = typeStats.getOrDefault("KINDERGARTEN", 0L);
        long universities = typeStats.getOrDefault("UNIVERSITY", 0L);
        
        // Count by gender
        Map<String, Long> genderStats = schools.stream()
            .filter(s -> s.getGender() != null)
            .collect(Collectors.groupingBy(School::getGender, Collectors.counting()));
        
        long boysSchools = genderStats.getOrDefault("BOYS", 0L);
        long girlsSchools = genderStats.getOrDefault("GIRLS", 0L);
        long mixedSchools = genderStats.getOrDefault("MIXED", 0L);
        
        // Calculate maintenance statistics
        List<Integer> scores = schools.stream()
            .filter(s -> s.getMaintenanceScore() != null)
            .map(School::getMaintenanceScore)
            .collect(Collectors.toList());
        
        BigDecimal averageScore = scores.isEmpty() ? BigDecimal.ZERO :
            BigDecimal.valueOf(scores.stream().mapToInt(Integer::intValue).average().orElse(0.0));
        
        Integer minScore = scores.isEmpty() ? null : scores.stream().min(Integer::compareTo).orElse(0);
        Integer maxScore = scores.isEmpty() ? null : scores.stream().max(Integer::compareTo).orElse(0);
        
        // Count by maintenance level
        long excellentMaintenance = scores.stream().filter(s -> s >= 90).count();
        long goodMaintenance = scores.stream().filter(s -> s >= 75 && s < 90).count();
        long fairMaintenance = scores.stream().filter(s -> s >= 60 && s < 75).count();
        long poorMaintenance = scores.stream().filter(s -> s >= 40 && s < 60).count();
        long criticalMaintenance = scores.stream().filter(s -> s < 40).count();
        
        // Count by activity level
        Map<String, Long> activityStats = schools.stream()
            .filter(s -> s.getActivityLevel() != null)
            .collect(Collectors.groupingBy(School::getActivityLevel, Collectors.counting()));
        
        long highActivitySchools = activityStats.getOrDefault("HIGH", 0L);
        long mediumActivitySchools = activityStats.getOrDefault("MEDIUM", 0L);
        long lowActivitySchools = activityStats.getOrDefault("LOW", 0L);
        
        // Geographic distribution
        Map<String, Long> cityStats = schools.stream()
            .filter(s -> s.getCity() != null)
            .collect(Collectors.groupingBy(School::getCity, Collectors.counting()));
        
        long schoolsWithCoordinates = schools.stream()
            .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
            .count();
        long schoolsWithoutCoordinates = totalSchools - schoolsWithCoordinates;
        
        return new SchoolStatsResponse(
            totalSchools, activeSchools, inactiveSchools,
            primarySchools, intermediateSchools, secondarySchools, highSchools, kindergartens, universities,
            boysSchools, girlsSchools, mixedSchools,
            averageScore, minScore, maxScore,
            excellentMaintenance, goodMaintenance, fairMaintenance, poorMaintenance, criticalMaintenance,
            highActivitySchools, mediumActivitySchools, lowActivitySchools,
            cityStats, schoolsWithCoordinates, schoolsWithoutCoordinates,
            0L, // totalPendingReports - would come from ReportService
            0L, // totalActiveWorkOrders - would come from WorkOrderService
            BigDecimal.ZERO, // averageReportsPerSchool - would be calculated
            0L, // schoolsWithHeavyWorkload - would be calculated
            0L, // schoolsNeedingAttention - would be calculated
            0L, // schoolsWithSupervisors - would come from assignment service
            0L, // unassignedSchools - would come from assignment service
            BigDecimal.ZERO, // averageSupervisorsPerSchool - would be calculated
            BigDecimal.ZERO, // schoolCountTrend - would require historical data
            BigDecimal.ZERO, // maintenanceScoreTrend - would require historical data
            "stable" // overallTrend - would be calculated from trends
        );
    }
    
    // ========== Inner Classes ==========
    
    /**
     * School statistics DTO.
     */
    public static class SchoolStatistics {
        public long totalSchools;
        public long activeSchools;
        public Map<String, Long> schoolsByType = new HashMap<>();
        public Map<String, Long> schoolsByGender = new HashMap<>();
        public Map<String, Long> schoolsByCity = new HashMap<>();
        public Map<String, Long> schoolsByActivityLevel = new HashMap<>();
        public double averageMaintenanceScore;
    }
    
    /**
     * Location DTO for bulk updates.
     */
    public static class Location {
        public BigDecimal latitude;
        public BigDecimal longitude;
        public String address;
        public String city;
        
        public Location(BigDecimal latitude, BigDecimal longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
        
        public Location(BigDecimal latitude, BigDecimal longitude, String address, String city) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
            this.city = city;
        }
    }
}