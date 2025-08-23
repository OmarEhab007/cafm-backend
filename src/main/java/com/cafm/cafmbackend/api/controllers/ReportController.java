package com.cafm.cafmbackend.controller;

import com.cafm.cafmbackend.data.entity.Report;
import com.cafm.cafmbackend.data.enums.ReportPriority;
import com.cafm.cafmbackend.data.enums.ReportStatus;
import com.cafm.cafmbackend.dto.report.*;
import com.cafm.cafmbackend.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for maintenance report operations.
 * 
 * Purpose: Manage maintenance reports from schools
 * Pattern: RESTful API with OpenAPI documentation
 * Java 23: Using records for DTOs and pattern matching
 * Architecture: Controller layer with service delegation
 * Standards: Comprehensive OpenAPI annotations for Swagger
 */
@RestController
@RequestMapping("/api/v1/reports")
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Reports", description = "Maintenance report operations")
public class ReportController {
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
    
    private final ReportService reportService;
    
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }
    
    // ========== CRUD Operations ==========
    
    /**
     * Get all reports with pagination.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Get all reports", description = "Get paginated list of maintenance reports")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reports retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Page<ReportSimplifiedResponse>> getAllReports(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        logger.debug("Get all reports with page: {}, size: {}", 
                    pageable.getPageNumber(), pageable.getPageSize());
        
        Page<ReportSimplifiedResponse> reports = reportService.getReports(pageable);
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get report by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    @Operation(summary = "Get report by ID", description = "Get detailed report information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Report retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Report not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized to view this report")
    })
    public ResponseEntity<ReportSimplifiedResponse> getReportById(
            @PathVariable @Parameter(description = "Report ID") UUID id) {
        
        logger.debug("Get report by ID: {}", id);
        
        ReportSimplifiedResponse report = reportService.getReportById(id);
        return ResponseEntity.ok(report);
    }
    
    /**
     * Create a new report.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Create report", description = "Create a new maintenance report")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Report created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid report data"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<ReportSimplifiedResponse> createReport(
            @Valid @RequestBody ReportCreateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Create report request from user: {} for school: {}", 
                   currentUser.getUsername(), request.schoolId());
        
        ReportSimplifiedResponse report = reportService.createReport(request);
        
        logger.info("Report created successfully with ID: {}", report.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }
    
    /**
     * Update report.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Update report", description = "Update an existing maintenance report")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Report updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid update data"),
        @ApiResponse(responseCode = "404", description = "Report not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized to update this report")
    })
    public ResponseEntity<ReportSimplifiedResponse> updateReport(
            @PathVariable @Parameter(description = "Report ID") UUID id,
            @Valid @RequestBody ReportUpdateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Update report request for ID: {} by user: {}", id, currentUser.getUsername());
        
        ReportSimplifiedResponse report = reportService.updateReportWithDto(id, request);
        
        logger.info("Report updated successfully: {}", id);
        return ResponseEntity.ok(report);
    }
    
    /**
     * Delete report (soft delete).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete report", description = "Soft delete a maintenance report")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Report deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Report not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Void> deleteReport(
            @PathVariable @Parameter(description = "Report ID") UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Delete report request for ID: {} by admin: {}", id, currentUser.getUsername());
        
        reportService.deleteReport(id);
        
        logger.info("Report deleted successfully: {}", id);
        return ResponseEntity.noContent().build();
    }
    
    // ========== Status Management ==========
    
    /**
     * Submit report for review.
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Transactional
    @Operation(summary = "Submit report", description = "Submit a report for review")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Report submitted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition"),
        @ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<Report> submitReport(
            @PathVariable @Parameter(description = "Report ID") UUID id) {
        
        logger.info("Submit report for review: {}", id);
        
        Report report = reportService.submitReport(id);
        
        logger.info("Report submitted successfully: {}", id);
        return ResponseEntity.ok(report);
    }
    
    /**
     * Review report (approve/reject).
     */
    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Review report", description = "Approve or reject a submitted report")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Report reviewed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid review action"),
        @ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<Report> reviewReport(
            @PathVariable @Parameter(description = "Report ID") UUID id,
            @RequestParam @Parameter(description = "Approval decision", required = true) boolean approved,
            @RequestParam(required = false) @Parameter(description = "Review comments") String comments) {
        
        logger.info("Review report {} - approved: {}", id, approved);
        
        Report report = reportService.reviewReport(id, approved, comments);
        
        logger.info("Report reviewed successfully: {} - status: {}", id, report.getStatus());
        return ResponseEntity.ok(report);
    }
    
    /**
     * Start work on report.
     */
    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    @Transactional
    @Operation(summary = "Start work", description = "Mark report work as started")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work started successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot start work on this report"),
        @ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<Report> startWork(
            @PathVariable @Parameter(description = "Report ID") UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Start work on report {} by user: {}", id, currentUser.getUsername());
        
        Report report = reportService.startWork(id);
        
        logger.info("Work started on report: {}", id);
        return ResponseEntity.ok(report);
    }
    
    /**
     * Complete report.
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    @Transactional
    @Operation(summary = "Complete report", description = "Mark report as completed")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Report completed successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot complete this report"),
        @ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<Report> completeReport(
            @PathVariable @Parameter(description = "Report ID") UUID id,
            @RequestParam(required = false) @Parameter(description = "Actual cost") BigDecimal actualCost,
            @RequestParam(required = false) @Parameter(description = "Completion notes") String completionNotes,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Complete report {} by user: {}", id, currentUser.getUsername());
        
        Report report = reportService.completeReport(id, actualCost, completionNotes);
        
        logger.info("Report completed successfully: {}", id);
        return ResponseEntity.ok(report);
    }
    
    /**
     * Cancel report.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Transactional
    @Operation(summary = "Cancel report", description = "Cancel a report")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Report cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot cancel this report"),
        @ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<Report> cancelReport(
            @PathVariable @Parameter(description = "Report ID") UUID id,
            @RequestParam @Parameter(description = "Cancellation reason", required = true) String reason,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Cancel report {} by user: {} with reason: {}", 
                   id, currentUser.getUsername(), reason);
        
        Report report = reportService.cancelReport(id, reason);
        
        logger.info("Report cancelled successfully: {}", id);
        return ResponseEntity.ok(report);
    }
    
    // ========== Assignment Operations ==========
    
    /**
     * Assign report to technician.
     */
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Transactional
    @Operation(summary = "Assign report", description = "Assign a report to a technician")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Report assigned successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid assignment"),
        @ApiResponse(responseCode = "404", description = "Report or technician not found")
    })
    public ResponseEntity<Report> assignReport(
            @PathVariable @Parameter(description = "Report ID") UUID id,
            @RequestParam @Parameter(description = "Technician ID", required = true) UUID technicianId) {
        
        logger.info("Assign report {} to technician: {}", id, technicianId);
        
        Report report = reportService.assignReport(id, technicianId);
        
        logger.info("Report assigned successfully: {} -> technician: {}", id, technicianId);
        return ResponseEntity.ok(report);
    }
    
    // ========== Query Operations ==========
    
    /**
     * Get reports by school.
     */
    @GetMapping("/school/{schoolId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Get reports by school", description = "Get all reports for a specific school")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reports retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "School not found")
    })
    public ResponseEntity<Page<ReportSimplifiedResponse>> getReportsBySchool(
            @PathVariable @Parameter(description = "School ID") UUID schoolId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        logger.debug("Get reports for school: {}", schoolId);
        
        Page<ReportSimplifiedResponse> reports = reportService.getReportsBySchool(schoolId, pageable);
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get reports by supervisor.
     */
    @GetMapping("/supervisor/{supervisorId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SUPERVISOR') and #supervisorId == authentication.principal.id)")
    @Operation(summary = "Get reports by supervisor", description = "Get all reports created by a supervisor")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reports retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Supervisor not found")
    })
    public ResponseEntity<Page<ReportSimplifiedResponse>> getReportsBySupervisor(
            @PathVariable @Parameter(description = "Supervisor ID") UUID supervisorId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        logger.debug("Get reports for supervisor: {}", supervisorId);
        
        Page<ReportSimplifiedResponse> reports = reportService.getReportsBySupervisor(supervisorId, pageable);
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get pending reports.
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get pending reports", description = "Get all pending reports")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reports retrieved successfully")
    })
    public ResponseEntity<List<Report>> getPendingReports() {
        
        logger.debug("Get pending reports");
        
        List<Report> reports = reportService.findPendingReports();
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get overdue reports.
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get overdue reports", description = "Get reports past their due date")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reports retrieved successfully")
    })
    public ResponseEntity<List<Report>> getOverdueReports() {
        
        logger.debug("Get overdue reports");
        
        List<Report> reports = reportService.findOverdueReports();
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get high priority reports.
     */
    @GetMapping("/high-priority")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get high priority reports", description = "Get reports with HIGH or CRITICAL priority")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reports retrieved successfully")
    })
    public ResponseEntity<List<Report>> getHighPriorityReports() {
        
        logger.debug("Get high priority reports");
        
        List<Report> reports = reportService.findHighPriorityReports();
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get reports by status.
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get reports by status", description = "Get reports with specific status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reports retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status")
    })
    public ResponseEntity<List<Report>> getReportsByStatus(
            @PathVariable @Parameter(description = "Report status") ReportStatus status) {
        
        logger.debug("Get reports with status: {}", status);
        
        List<Report> reports = reportService.findByStatus(status);
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get reports by priority.
     */
    @GetMapping("/priority/{priority}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get reports by priority", description = "Get reports with specific priority")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reports retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid priority")
    })
    public ResponseEntity<List<Report>> getReportsByPriority(
            @PathVariable @Parameter(description = "Report priority") ReportPriority priority) {
        
        logger.debug("Get reports with priority: {}", priority);
        
        List<Report> reports = reportService.findByPriority(priority);
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get reports between dates.
     */
    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get reports by date range", description = "Get reports created between two dates")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reports retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date range")
    })
    public ResponseEntity<List<Report>> getReportsBetweenDates(
            @RequestParam @Parameter(description = "Start date", required = true) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @Parameter(description = "End date", required = true) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.debug("Get reports between {} and {}", startDate, endDate);
        
        if (endDate.isBefore(startDate)) {
            return ResponseEntity.badRequest().build();
        }
        
        List<Report> reports = reportService.findReportsBetweenDates(startDate, endDate);
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get report statistics.
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get report statistics", description = "Get statistical summary of reports")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    public ResponseEntity<ReportService.ReportStatistics> getReportStatistics(
            @RequestParam(required = false) @Parameter(description = "Company ID") UUID companyId) {
        
        logger.debug("Get report statistics for company: {}", companyId);
        
        ReportService.ReportStatistics stats = reportService.getReportStatistics(companyId);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get available status transitions.
     */
    @GetMapping("/{id}/transitions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Get status transitions", description = "Get available status transitions for a report")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transitions retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<List<ReportStatus>> getAvailableTransitions(
            @PathVariable @Parameter(description = "Report ID") UUID id) {
        
        logger.debug("Get available transitions for report: {}", id);
        
        List<ReportStatus> transitions = reportService.getAvailableTransitions(id);
        return ResponseEntity.ok(transitions);
    }
}