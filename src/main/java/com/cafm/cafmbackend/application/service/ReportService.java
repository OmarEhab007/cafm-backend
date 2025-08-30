package com.cafm.cafmbackend.application.service;

import com.cafm.cafmbackend.infrastructure.persistence.entity.Company;
import com.cafm.cafmbackend.infrastructure.persistence.entity.Report;
import com.cafm.cafmbackend.infrastructure.persistence.entity.School;
import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import com.cafm.cafmbackend.shared.enums.ReportPriority;
import com.cafm.cafmbackend.shared.enums.ReportStatus;
import com.cafm.cafmbackend.shared.enums.UserType;
import com.cafm.cafmbackend.infrastructure.persistence.repository.CompanyRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.ReportRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.SchoolRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.UserRepository;
import com.cafm.cafmbackend.dto.report.ReportCreateRequest;
import com.cafm.cafmbackend.dto.report.ReportUpdateRequest;
import com.cafm.cafmbackend.dto.report.ReportSimplifiedResponse;
import com.cafm.cafmbackend.shared.exception.DuplicateResourceException;
import com.cafm.cafmbackend.shared.exception.ResourceNotFoundException;
import com.cafm.cafmbackend.application.service.tenant.TenantContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Service class for managing maintenance reports and work orders.
 * Handles report lifecycle, assignment, and tracking.
 */
@Service
@Transactional
public class ReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    
    private final ReportRepository reportRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final TenantContextService tenantContextService;
    
    public ReportService(ReportRepository reportRepository,
                        SchoolRepository schoolRepository,
                        UserRepository userRepository,
                        CompanyRepository companyRepository,
                        TenantContextService tenantContextService) {
        this.reportRepository = reportRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.tenantContextService = tenantContextService;
    }
    
    // ========== DTO Conversion Methods ==========
    
    /**
     * Convert Report entity to simplified response DTO.
     * This replaces the mapper layer with direct conversion.
     */
    private ReportSimplifiedResponse toResponse(Report entity) {
        if (entity == null) return null;
        
        return ReportSimplifiedResponse.builder()
            .id(entity.getId())
            .reportNumber(entity.getReportNumber())
            .title(entity.getTitle())
            .description(entity.getDescription())
            .status(entity.getStatus())
            .priority(entity.getPriority())
            .schoolId(entity.getSchool() != null ? entity.getSchool().getId() : null)
            .schoolName(entity.getSchool() != null ? entity.getSchool().getName() : null)
            .supervisorId(entity.getSupervisor() != null ? entity.getSupervisor().getId() : null)
            .supervisorName(entity.getSupervisor() != null ? entity.getSupervisor().getFullName() : null)
            .assignedToId(entity.getAssignedTo() != null ? entity.getAssignedTo().getId() : null)
            .assignedToName(entity.getAssignedTo() != null ? entity.getAssignedTo().getFullName() : null)
            .companyId(entity.getCompany() != null ? entity.getCompany().getId() : null)
            .reportedDate(entity.getReportedDate())
            .scheduledDate(entity.getScheduledDate())
            .completedDate(entity.getCompletedDate())
            .estimatedCost(entity.getEstimatedCost())
            .actualCost(entity.getActualCost())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
    
    /**
     * Convert ReportCreateRequest to Report entity.
     * Sets up a new report with proper defaults.
     */
    private Report fromCreateRequest(ReportCreateRequest request, School school, User supervisor, Company company) {
        Report entity = new Report();
        
        // Required fields
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setSchool(school);
        entity.setSupervisor(supervisor);
        entity.setCompany(company);
        // Note: category and location fields not in Report entity
        
        // Optional fields
        entity.setPriority(request.priority() != null ? request.priority() : ReportPriority.MEDIUM);
        // Note: estimatedCost and scheduledDate not in ReportCreateRequest
        entity.setEstimatedCost(null);
        entity.setScheduledDate(null);
        
        // Set defaults
        entity.setStatus(ReportStatus.DRAFT);
        entity.setReportedDate(LocalDate.now());
        
        return entity;
    }
    
    /**
     * Update Report entity from ReportUpdateRequest.
     * Only updates provided fields.
     */
    private void updateFromRequest(Report entity, ReportUpdateRequest request) {
        if (request.title() != null) {
            entity.setTitle(request.title());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.priority() != null) {
            entity.setPriority(request.priority());
        }
        // Note: estimatedCost and scheduledDate not in ReportUpdateRequest
        // These fields would need to be handled separately if needed
    }
    
    // ========== Report Management Methods ==========
    
    /**
     * Create a new report using DTO.
     */
    @Transactional
    public ReportSimplifiedResponse createReport(ReportCreateRequest request) {
        logger.info("Creating new report: {}", request.title());
        
        // Fetch related entities
        School school = schoolRepository.findById(request.schoolId())
            .orElseThrow(() -> new ResourceNotFoundException("School not found: " + request.schoolId()));
        
        User supervisor = userRepository.findById(request.supervisorId())
            .orElseThrow(() -> new ResourceNotFoundException("Supervisor not found: " + request.supervisorId()));
        
        Company company = companyRepository.findById(request.companyId())
            .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + request.companyId()));
        
        // Validate supervisor
        if (!supervisor.isSupervisor()) {
            throw new IllegalArgumentException("User is not a supervisor: " + request.supervisorId());
        }
        
        // Convert and set report number
        Report report = fromCreateRequest(request, school, supervisor, company);
        String reportNumber = generateReportNumber();
        while (reportRepository.existsByReportNumber(reportNumber)) {
            reportNumber = generateReportNumber();
        }
        report.setReportNumber(reportNumber);
        
        // Save and return
        report = reportRepository.save(report);
        logger.info("Created report with ID: {}", report.getId());
        return toResponse(report);
    }
    
    /**
     * Create a new report (legacy method for compatibility).
     */
    public Report createReport(Report report, UUID supervisorId, UUID schoolId, UUID companyId) {
        logger.info("Creating new report for school: {}", schoolId);
        
        // Generate unique report number
        String reportNumber = generateReportNumber();
        while (reportRepository.existsByReportNumber(reportNumber)) {
            reportNumber = generateReportNumber();
        }
        report.setReportNumber(reportNumber);
        
        // Set relationships
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
        report.setCompany(company);
        
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));
        report.setSchool(school);
        
        User supervisor = userRepository.findById(supervisorId)
            .orElseThrow(() -> new ResourceNotFoundException("Supervisor not found: " + supervisorId));
        
        // Validate supervisor
        if (!supervisor.isSupervisor()) {
            throw new IllegalArgumentException("User is not a supervisor: " + supervisorId);
        }
        report.setSupervisor(supervisor);
        
        // Set defaults
        if (report.getStatus() == null) {
            report.setStatus(ReportStatus.DRAFT);
        }
        if (report.getPriority() == null) {
            report.setPriority(ReportPriority.MEDIUM);
        }
        if (report.getReportedDate() == null) {
            report.setReportedDate(LocalDate.now());
        }
        
        Report savedReport = reportRepository.save(report);
        logger.info("Report created successfully with number: {}", savedReport.getReportNumber());
        
        // Update school maintenance score if needed
        updateSchoolMaintenanceScore(schoolId);
        
        return savedReport;
    }
    
    /**
     * Get report by ID with DTO conversion.
     */
    public ReportSimplifiedResponse getReportById(UUID id) {
        Report report = findById(id);
        return toResponse(report);
    }
    
    /**
     * Update report using DTO.
     */
    @Transactional
    public ReportSimplifiedResponse updateReportWithDto(UUID id, ReportUpdateRequest request) {
        logger.info("Updating report with DTO: {}", id);
        
        Report report = findById(id);
        
        // Validate status for editing
        if (!report.getStatus().isEditable()) {
            throw new IllegalStateException("Report cannot be edited in status: " + report.getStatus());
        }
        
        updateFromRequest(report, request);
        report = reportRepository.save(report);
        
        logger.info("Updated report with ID: {}", id);
        return toResponse(report);
    }
    
    /**
     * Get paginated list of reports.
     */
    public Page<ReportSimplifiedResponse> getReports(Pageable pageable) {
        UUID companyId = tenantContextService.getCurrentTenant();
        
        // For now, get all reports and filter by company manually in memory
        // TODO: This should be replaced with a database query for better performance
        List<Report> allReports = reportRepository.findAll().stream()
            .filter(report -> report.getCompany() != null && 
                             report.getCompany().getId().equals(companyId) && 
                             report.getDeletedAt() == null)
            .toList();
        
        // Apply pagination manually for now
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allReports.size());
        
        List<Report> pageContent = allReports.subList(Math.min(start, allReports.size()), end);
        List<ReportSimplifiedResponse> responseContent = pageContent.stream()
            .map(this::toResponse)
            .toList();
        
        return new PageImpl<>(responseContent, pageable, allReports.size());
    }
    
    /**
     * Get reports by school.
     */
    public Page<ReportSimplifiedResponse> getReportsBySchool(UUID schoolId, Pageable pageable) {
        return reportRepository.findBySchoolId(schoolId, pageable)
            .map(this::toResponse);
    }
    
    /**
     * Get reports by supervisor.
     */
    public Page<ReportSimplifiedResponse> getReportsBySupervisor(UUID supervisorId, Pageable pageable) {
        return reportRepository.findBySupervisorId(supervisorId, pageable)
            .map(this::toResponse);
    }
    

    
    /**
     * Update an existing report.
     */
    public Report updateReport(UUID reportId, Report updatedReport) {
        logger.info("Updating report: {}", reportId);
        
        Report existingReport = findById(reportId);
        
        // Check if report is editable
        if (!existingReport.getStatus().isEditable()) {
            throw new IllegalStateException("Report cannot be edited in status: " + existingReport.getStatus());
        }
        
        // Update allowed fields
        if (updatedReport.getTitle() != null) {
            existingReport.setTitle(updatedReport.getTitle());
        }
        if (updatedReport.getDescription() != null) {
            existingReport.setDescription(updatedReport.getDescription());
        }
        if (updatedReport.getPriority() != null) {
            existingReport.setPriority(updatedReport.getPriority());
        }
        if (updatedReport.getScheduledDate() != null) {
            existingReport.setScheduledDate(updatedReport.getScheduledDate());
        }
        if (updatedReport.getEstimatedCost() != null) {
            existingReport.setEstimatedCost(updatedReport.getEstimatedCost());
        }
        
        return reportRepository.save(existingReport);
    }
    
    /**
     * Submit report for review.
     */
    public Report submitReport(UUID reportId) {
        logger.info("Submitting report: {}", reportId);
        
        Report report = findById(reportId);
        
        // Validate status transition
        if (report.getStatus() != ReportStatus.DRAFT && report.getStatus() != ReportStatus.REJECTED) {
            throw new IllegalStateException("Report can only be submitted from DRAFT or REJECTED status");
        }
        
        report.setStatus(ReportStatus.SUBMITTED);
        
        return reportRepository.save(report);
    }
    
    /**
     * Review report (approve or reject).
     */
    public Report reviewReport(UUID reportId, boolean approved, String comments) {
        logger.info("Reviewing report {}: {}", reportId, approved ? "APPROVED" : "REJECTED");
        
        Report report = findById(reportId);
        
        // Validate status
        if (report.getStatus() != ReportStatus.SUBMITTED && report.getStatus() != ReportStatus.IN_REVIEW) {
            throw new IllegalStateException("Report must be in SUBMITTED or IN_REVIEW status");
        }
        
        if (approved) {
            report.setStatus(ReportStatus.APPROVED);
        } else {
            report.setStatus(ReportStatus.REJECTED);
            // Store rejection reason in description or separate field
            if (comments != null) {
                report.setDescription(report.getDescription() + "\n[Rejection: " + comments + "]");
            }
        }
        
        return reportRepository.save(report);
    }
    
    /**
     * Assign report to technician.
     */
    public Report assignReport(UUID reportId, UUID technicianId) {
        logger.info("Assigning report {} to technician {}", reportId, technicianId);
        
        Report report = findById(reportId);
        
        // Validate status
        if (report.getStatus() != ReportStatus.APPROVED && report.getStatus() != ReportStatus.IN_PROGRESS) {
            throw new IllegalStateException("Report must be APPROVED to assign");
        }
        
        User technician = userRepository.findById(technicianId)
            .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + technicianId));
        
        // Validate technician
        if (!technician.isTechnician()) {
            throw new IllegalArgumentException("User is not a technician: " + technicianId);
        }
        
        if (!technician.isAvailableForAssignment()) {
            throw new IllegalStateException("Technician is not available for assignment");
        }
        
        report.setAssignedTo(technician);
        report.setStatus(ReportStatus.IN_PROGRESS);
        
        return reportRepository.save(report);
    }
    
    /**
     * Start work on report.
     */
    public Report startWork(UUID reportId) {
        logger.info("Starting work on report: {}", reportId);
        
        Report report = findById(reportId);
        
        // Validate status and assignment
        if (report.getStatus() != ReportStatus.APPROVED) {
            throw new IllegalStateException("Report must be APPROVED to start work");
        }
        
        if (report.getAssignedTo() == null) {
            throw new IllegalStateException("Report must be assigned before starting work");
        }
        
        report.setStatus(ReportStatus.IN_PROGRESS);
        
        return reportRepository.save(report);
    }
    
    /**
     * Complete report.
     */
    public Report completeReport(UUID reportId, BigDecimal actualCost, String completionNotes) {
        logger.info("Completing report: {}", reportId);
        
        Report report = findById(reportId);
        
        // Validate status
        if (report.getStatus() != ReportStatus.IN_PROGRESS) {
            throw new IllegalStateException("Report must be IN_PROGRESS to complete");
        }
        
        report.setStatus(ReportStatus.COMPLETED);
        report.setCompletedDate(LocalDate.now());
        
        if (actualCost != null) {
            report.setActualCost(actualCost);
        }
        
        if (completionNotes != null) {
            report.setDescription(report.getDescription() + "\n[Completion: " + completionNotes + "]");
        }
        
        Report completedReport = reportRepository.save(report);
        
        // Update school maintenance score
        updateSchoolMaintenanceScore(report.getSchool().getId());
        
        return completedReport;
    }
    
    /**
     * Cancel report.
     */
    public Report cancelReport(UUID reportId, String reason) {
        logger.info("Cancelling report: {}", reportId);
        
        Report report = findById(reportId);
        
        // Check if report can be cancelled
        if (report.getStatus().isFinal()) {
            throw new IllegalStateException("Cannot cancel report in final status: " + report.getStatus());
        }
        
        report.setStatus(ReportStatus.CANCELLED);
        
        if (reason != null) {
            report.setDescription(report.getDescription() + "\n[Cancelled: " + reason + "]");
        }
        
        return reportRepository.save(report);
    }
    
    /**
     * Soft delete a report.
     */
    public void deleteReport(UUID reportId) {
        logger.info("Soft deleting report: {}", reportId);
        
        Report report = findById(reportId);
        report.setDeletedAt(LocalDateTime.now());
        
        reportRepository.save(report);
    }
    
    // ========== Query Methods ==========
    
    /**
     * Find report by ID.
     */
    public Report findById(UUID reportId) {
        return reportRepository.findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));
    }
    
    /**
     * Find report by report number.
     */
    public Optional<Report> findByReportNumber(String reportNumber) {
        return reportRepository.findByReportNumber(reportNumber);
    }
    
    /**
     * Find all reports with pagination.
     */
    public Page<Report> findAll(Pageable pageable) {
        return reportRepository.findAll(pageable);
    }
    
    /**
     * Find reports by status.
     */
    public List<Report> findByStatus(ReportStatus status) {
        return reportRepository.findByStatus(status);
    }
    
    /**
     * Find reports by priority.
     */
    public List<Report> findByPriority(ReportPriority priority) {
        return reportRepository.findByPriority(priority);
    }
    
    /**
     * Find reports by school.
     */
    public Page<Report> findBySchool(UUID schoolId, Pageable pageable) {
        return reportRepository.findBySchoolId(schoolId, pageable);
    }
    
    /**
     * Find reports by supervisor.
     */
    public Page<Report> findBySupervisor(UUID supervisorId, Pageable pageable) {
        return reportRepository.findBySupervisorId(supervisorId, pageable);
    }
    
    /**
     * Find reports assigned to user.
     */
    public Page<Report> findAssignedReports(UUID userId, Pageable pageable) {
        return reportRepository.findByAssignedTo(userId, pageable);
    }
    
    /**
     * Find pending reports.
     */
    public List<Report> findPendingReports() {
        List<ReportStatus> pendingStatuses = Arrays.asList(
            ReportStatus.SUBMITTED, 
            ReportStatus.IN_REVIEW, 
            ReportStatus.APPROVED
        );
        return reportRepository.findPendingReports(pendingStatuses);
    }
    
    /**
     * Find overdue reports.
     */
    public List<Report> findOverdueReports() {
        List<ReportStatus> excludeStatuses = Arrays.asList(
            ReportStatus.COMPLETED, 
            ReportStatus.CANCELLED
        );
        return reportRepository.findOverdueReports(LocalDate.now(), excludeStatuses);
    }
    
    /**
     * Find high priority reports.
     */
    public List<Report> findHighPriorityReports() {
        List<ReportPriority> highPriorities = Arrays.asList(
            ReportPriority.CRITICAL, 
            ReportPriority.URGENT
        );
        List<ReportStatus> excludeStatuses = Arrays.asList(
            ReportStatus.COMPLETED, 
            ReportStatus.CANCELLED
        );
        return reportRepository.findHighPriorityReports(highPriorities, excludeStatuses);
    }
    
    /**
     * Find reports between dates.
     */
    public List<Report> findReportsBetweenDates(LocalDate startDate, LocalDate endDate) {
        return reportRepository.findReportsBetweenDates(startDate, endDate);
    }
    
    // ========== Workflow Methods ==========
    
    /**
     * Get available status transitions for a report.
     */
    public List<ReportStatus> getAvailableTransitions(UUID reportId) {
        Report report = findById(reportId);
        ReportStatus currentStatus = report.getStatus();
        
        List<ReportStatus> transitions = new ArrayList<>();
        
        switch (currentStatus) {
            case DRAFT -> {
                transitions.add(ReportStatus.SUBMITTED);
                transitions.add(ReportStatus.CANCELLED);
            }
            case SUBMITTED -> {
                transitions.add(ReportStatus.IN_REVIEW);
                transitions.add(ReportStatus.APPROVED);
                transitions.add(ReportStatus.REJECTED);
                transitions.add(ReportStatus.CANCELLED);
            }
            case IN_REVIEW -> {
                transitions.add(ReportStatus.APPROVED);
                transitions.add(ReportStatus.REJECTED);
                transitions.add(ReportStatus.CANCELLED);
            }
            case APPROVED -> {
                transitions.add(ReportStatus.IN_PROGRESS);
                transitions.add(ReportStatus.CANCELLED);
            }
            case REJECTED -> {
                transitions.add(ReportStatus.DRAFT);
                transitions.add(ReportStatus.SUBMITTED);
                transitions.add(ReportStatus.CANCELLED);
            }
            case IN_PROGRESS -> {
                transitions.add(ReportStatus.PENDING);
                transitions.add(ReportStatus.COMPLETED);
                transitions.add(ReportStatus.CANCELLED);
            }
            case PENDING -> {
                transitions.add(ReportStatus.IN_PROGRESS);
                transitions.add(ReportStatus.CANCELLED);
            }
            case COMPLETED, CANCELLED -> {
                // Final states - no transitions
            }
        }
        
        return transitions;
    }
    
    /**
     * Transition report to new status.
     */
    public Report transitionStatus(UUID reportId, ReportStatus newStatus) {
        logger.info("Transitioning report {} to status: {}", reportId, newStatus);
        
        Report report = findById(reportId);
        List<ReportStatus> availableTransitions = getAvailableTransitions(reportId);
        
        if (!availableTransitions.contains(newStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", report.getStatus(), newStatus)
            );
        }
        
        report.setStatus(newStatus);
        
        // Handle status-specific logic
        if (newStatus == ReportStatus.COMPLETED) {
            report.setCompletedDate(LocalDate.now());
        }
        
        return reportRepository.save(report);
    }
    
    // ========== Statistics Methods ==========
    
    /**
     * Get report statistics for a company.
     */
    @Transactional(readOnly = true)
    public ReportStatistics getReportStatistics(UUID companyId) {
        ReportStatistics stats = new ReportStatistics();
        
        List<Report> reports = reportRepository.findAll().stream()
            .filter(r -> r.getCompany() != null && r.getCompany().getId().equals(companyId))
            .collect(Collectors.toList());
        
        stats.totalReports = reports.size();
        
        // Count by status
        stats.reportsByStatus = reports.stream()
            .collect(Collectors.groupingBy(Report::getStatus, Collectors.counting()));
        
        // Count by priority
        stats.reportsByPriority = reports.stream()
            .filter(r -> r.getPriority() != null)
            .collect(Collectors.groupingBy(Report::getPriority, Collectors.counting()));
        
        // Calculate metrics
        stats.completedReports = reports.stream()
            .filter(r -> r.getStatus() == ReportStatus.COMPLETED)
            .count();
        
        stats.inProgressReports = reports.stream()
            .filter(r -> r.getStatus() == ReportStatus.IN_PROGRESS)
            .count();
        
        stats.overdueReports = reports.stream()
            .filter(r -> r.getScheduledDate() != null && 
                        r.getScheduledDate().isBefore(LocalDate.now()) &&
                        !r.getStatus().isFinal())
            .count();
        
        // Calculate costs
        stats.totalEstimatedCost = reports.stream()
            .filter(r -> r.getEstimatedCost() != null)
            .map(Report::getEstimatedCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        stats.totalActualCost = reports.stream()
            .filter(r -> r.getActualCost() != null)
            .map(Report::getActualCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate average completion time
        stats.averageCompletionDays = reports.stream()
            .filter(r -> r.getStatus() == ReportStatus.COMPLETED && 
                        r.getReportedDate() != null && 
                        r.getCompletedDate() != null)
            .mapToLong(r -> r.getCompletedDate().toEpochDay() - r.getReportedDate().toEpochDay())
            .average()
            .orElse(0.0);
        
        return stats;
    }
    
    /**
     * Get report metrics by school.
     */
    public Map<UUID, SchoolReportMetrics> getReportMetricsBySchool(UUID companyId) {
        Map<UUID, SchoolReportMetrics> metricsMap = new HashMap<>();
        
        List<School> schools = schoolRepository.findAll().stream()
            .filter(s -> s.getCompany() != null && s.getCompany().getId().equals(companyId))
            .collect(Collectors.toList());
        
        for (School school : schools) {
            SchoolReportMetrics metrics = new SchoolReportMetrics();
            metrics.schoolId = school.getId();
            metrics.schoolName = school.getName();
            
            List<Report> schoolReports = reportRepository.findBySchoolId(school.getId(), Pageable.unpaged()).getContent();
            
            metrics.totalReports = schoolReports.size();
            metrics.openReports = schoolReports.stream()
                .filter(r -> !r.getStatus().isFinal())
                .count();
            metrics.completedReports = schoolReports.stream()
                .filter(r -> r.getStatus() == ReportStatus.COMPLETED)
                .count();
            
            metricsMap.put(school.getId(), metrics);
        }
        
        return metricsMap;
    }
    
    // ========== Utility Methods ==========
    
    /**
     * Generate unique report number.
     */
    private String generateReportNumber() {
        String prefix = "RPT";
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return prefix + "-" + datePart + "-" + randomPart;
    }
    
    /**
     * Update school maintenance score based on reports.
     */
    private void updateSchoolMaintenanceScore(UUID schoolId) {
        List<ReportStatus> excludeStatuses = Arrays.asList(
            ReportStatus.COMPLETED, 
            ReportStatus.CANCELLED, 
            ReportStatus.REJECTED
        );
        List<Report> activeReports = reportRepository.findActiveReportsBySchool(schoolId, excludeStatuses);
        
        // Calculate score based on active reports
        int baseScore = 100;
        int deduction = 0;
        
        for (Report report : activeReports) {
            if (report.getPriority() == ReportPriority.CRITICAL) {
                deduction += 20;
            } else if (report.getPriority() == ReportPriority.URGENT) {
                deduction += 15;
            } else if (report.getPriority() == ReportPriority.HIGH) {
                deduction += 10;
            } else if (report.getPriority() == ReportPriority.MEDIUM) {
                deduction += 5;
            } else {
                deduction += 2;
            }
            
            // Additional deduction for overdue reports
            if (report.getScheduledDate() != null && report.getScheduledDate().isBefore(LocalDate.now())) {
                deduction += 5;
            }
        }
        
        int newScore = Math.max(0, baseScore - deduction);
        
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found: " + schoolId));
        school.setMaintenanceScore(newScore);
        schoolRepository.save(school);
        
        logger.debug("Updated maintenance score for school {} to {}", schoolId, newScore);
    }
    
    /**
     * Bulk import reports from external source.
     */
    @Transactional
    public List<Report> importReports(List<Report> reports, UUID companyId) {
        logger.info("Importing {} reports for company {}", reports.size(), companyId);
        
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
        
        List<Report> savedReports = new ArrayList<>();
        
        for (Report report : reports) {
            // Generate unique report number if not provided
            if (report.getReportNumber() == null || reportRepository.existsByReportNumber(report.getReportNumber())) {
                report.setReportNumber(generateReportNumber());
            }
            
            report.setCompany(company);
            
            // Set defaults
            if (report.getStatus() == null) {
                report.setStatus(ReportStatus.DRAFT);
            }
            if (report.getPriority() == null) {
                report.setPriority(ReportPriority.MEDIUM);
            }
            if (report.getReportedDate() == null) {
                report.setReportedDate(LocalDate.now());
            }
            
            savedReports.add(reportRepository.save(report));
        }
        
        logger.info("Successfully imported {} reports", savedReports.size());
        return savedReports;
    }
    
    // ========== Inner Classes ==========
    
    /**
     * Report statistics DTO.
     */
    public static class ReportStatistics {
        public long totalReports;
        public long completedReports;
        public long inProgressReports;
        public long overdueReports;
        public Map<ReportStatus, Long> reportsByStatus = new HashMap<>();
        public Map<ReportPriority, Long> reportsByPriority = new HashMap<>();
        public BigDecimal totalEstimatedCost = BigDecimal.ZERO;
        public BigDecimal totalActualCost = BigDecimal.ZERO;
        public double averageCompletionDays;
    }
    
    /**
     * School report metrics DTO.
     */
    public static class SchoolReportMetrics {
        public UUID schoolId;
        public String schoolName;
        public long totalReports;
        public long openReports;
        public long completedReports;
    }
}