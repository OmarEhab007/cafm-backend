package com.cafm.cafmbackend.api.controllers;

import com.cafm.cafmbackend.infrastructure.persistence.entity.WorkOrder;
import com.cafm.cafmbackend.infrastructure.persistence.entity.WorkOrderMaterial;
import com.cafm.cafmbackend.infrastructure.persistence.entity.WorkOrderTask;
import com.cafm.cafmbackend.shared.enums.WorkOrderStatus;
import com.cafm.cafmbackend.dto.workorder.*;
import com.cafm.cafmbackend.application.service.WorkOrderService;
import com.cafm.cafmbackend.application.service.ReportGenerationService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for work order management operations.
 * 
 * Purpose: Manage work orders for maintenance tasks
 * Pattern: RESTful API with comprehensive OpenAPI documentation
 * Java 23: Using records for DTOs and enhanced switch expressions
 * Architecture: Controller layer delegating to service layer
 * Standards: Full OpenAPI/Swagger documentation
 */
@RestController
@RequestMapping("/api/v1/work-orders")
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Work Orders", description = "Work order management operations")
public class WorkOrderController {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderController.class);
    
    private final WorkOrderService workOrderService;
    private final ReportGenerationService reportGenerationService;
    
    public WorkOrderController(WorkOrderService workOrderService, ReportGenerationService reportGenerationService) {
        this.workOrderService = workOrderService;
        this.reportGenerationService = reportGenerationService;
    }
    
    // ========== CRUD Operations ==========
    
    /**
     * Get all work orders with pagination.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    @Operation(summary = "Get all work orders", description = "Get paginated list of work orders")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work orders retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Page<WorkOrderSimplifiedResponse>> getAllWorkOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        logger.debug("Get all work orders with page: {}, size: {}", 
                    pageable.getPageNumber(), pageable.getPageSize());
        
        Page<WorkOrderSimplifiedResponse> workOrders = workOrderService.getWorkOrders(pageable);
        return ResponseEntity.ok(workOrders);
    }
    
    /**
     * Get work order by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    @Operation(summary = "Get work order by ID", description = "Get detailed work order information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work order retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Work order not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized to view this work order")
    })
    public ResponseEntity<WorkOrderSimplifiedResponse> getWorkOrderById(
            @PathVariable @Parameter(description = "Work order ID") UUID id) {
        
        logger.debug("Get work order by ID: {}", id);
        
        WorkOrderSimplifiedResponse workOrder = workOrderService.getWorkOrderById(id);
        return ResponseEntity.ok(workOrder);
    }
    
    /**
     * Create a new work order.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Create work order", description = "Create a new work order")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Work order created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid work order data"),
        @ApiResponse(responseCode = "404", description = "Report not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<WorkOrderSimplifiedResponse> createWorkOrder(
            @Valid @RequestBody WorkOrderCreateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Create work order request from user: {} for report: {}", 
                   currentUser.getUsername(), request.reportId());
        
        WorkOrderSimplifiedResponse workOrder = workOrderService.createWorkOrder(request);
        
        logger.info("Work order created successfully with ID: {}", workOrder.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(workOrder);
    }
    
    /**
     * Create work order from report.
     */
    @PostMapping("/from-report/{reportId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Transactional
    @Operation(summary = "Create from report", description = "Create work order from an existing report")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Work order created successfully"),
        @ApiResponse(responseCode = "404", description = "Report not found"),
        @ApiResponse(responseCode = "400", description = "Report already has work order")
    })
    public ResponseEntity<WorkOrder> createFromReport(
            @PathVariable @Parameter(description = "Report ID") UUID reportId,
            @RequestParam @Parameter(description = "Company ID", required = true) UUID companyId,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Create work order from report {} by user: {}", reportId, currentUser.getUsername());
        
        // Get user ID from authenticated user
        // This would normally get the actual user ID from the UserDetails
        UUID assignedById = UUID.randomUUID(); // Placeholder - should get from current user
        
        WorkOrder workOrder = workOrderService.createFromReport(reportId, companyId, assignedById);
        
        logger.info("Work order created from report: {} -> WO: {}", reportId, workOrder.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(workOrder);
    }
    
    /**
     * Delete work order (soft delete).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete work order", description = "Soft delete a work order")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Work order deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Work order not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Void> deleteWorkOrder(
            @PathVariable @Parameter(description = "Work order ID") UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Delete work order request for ID: {} by admin: {}", id, currentUser.getUsername());
        
        workOrderService.deleteWorkOrder(id);
        
        logger.info("Work order deleted successfully: {}", id);
        return ResponseEntity.noContent().build();
    }
    
    // ========== Assignment Operations ==========
    
    /**
     * Assign work order to technician.
     */
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Transactional
    @Operation(summary = "Assign work order", description = "Assign a work order to a technician")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work order assigned successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid assignment"),
        @ApiResponse(responseCode = "404", description = "Work order or technician not found")
    })
    public ResponseEntity<WorkOrder> assignWorkOrder(
            @PathVariable @Parameter(description = "Work order ID") UUID id,
            @RequestParam @Parameter(description = "Technician ID", required = true) UUID technicianId) {
        
        logger.info("Assign work order {} to technician: {}", id, technicianId);
        
        WorkOrder workOrder = workOrderService.assignToTechnician(id, technicianId);
        
        logger.info("Work order assigned successfully: {} -> technician: {}", id, technicianId);
        return ResponseEntity.ok(workOrder);
    }
    
    // ========== Work Progress Operations ==========
    
    /**
     * Start work on work order.
     */
    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    @Transactional
    @Operation(summary = "Start work order", description = "Mark a work order as in progress")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work order started successfully"),
        @ApiResponse(responseCode = "400", description = "Work order cannot be started"),
        @ApiResponse(responseCode = "404", description = "Work order not found")
    })
    public ResponseEntity<WorkOrder> startWorkOrder(
            @PathVariable @Parameter(description = "Work order ID") UUID id,
            @RequestParam @Parameter(description = "Technician ID", required = true) UUID technicianId) {
        
        logger.info("Start work order {} by technician: {}", id, technicianId);
        
        WorkOrder workOrder = workOrderService.startWork(id, technicianId);
        
        logger.info("Work order started successfully: {}", id);
        return ResponseEntity.ok(workOrder);
    }
    
    /**
     * Update work order progress.
     */
    @PatchMapping("/{id}/progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    @Operation(summary = "Update progress", description = "Update the progress percentage of a work order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Progress updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid progress value"),
        @ApiResponse(responseCode = "404", description = "Work order not found")
    })
    public ResponseEntity<WorkOrderSimplifiedResponse> updateProgress(
            @PathVariable @Parameter(description = "Work order ID") UUID id,
            @Valid @RequestBody WorkOrderProgressRequest request) {
        
        logger.info("Update work order {} progress to {}%", id, request.progressPercentage());
        
        WorkOrderSimplifiedResponse workOrder = workOrderService.updateWorkOrderProgress(id, request);
        
        logger.info("Work order progress updated successfully: {} -> {}%", id, request.progressPercentage());
        return ResponseEntity.ok(workOrder);
    }
    
    /**
     * Put work order on hold.
     */
    @PostMapping("/{id}/hold")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Transactional
    @Operation(summary = "Put on hold", description = "Put a work order on hold")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work order put on hold successfully"),
        @ApiResponse(responseCode = "400", description = "Work order cannot be put on hold"),
        @ApiResponse(responseCode = "404", description = "Work order not found")
    })
    public ResponseEntity<WorkOrder> putOnHold(
            @PathVariable @Parameter(description = "Work order ID") UUID id,
            @RequestParam @Parameter(description = "Hold reason", required = true) String reason) {
        
        logger.info("Put work order {} on hold: {}", id, reason);
        
        WorkOrder workOrder = workOrderService.putOnHold(id, reason);
        
        logger.info("Work order put on hold successfully: {}", id);
        return ResponseEntity.ok(workOrder);
    }
    
    /**
     * Resume work order from hold.
     */
    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Transactional
    @Operation(summary = "Resume work", description = "Resume a work order that was on hold")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work order resumed successfully"),
        @ApiResponse(responseCode = "400", description = "Work order is not on hold"),
        @ApiResponse(responseCode = "404", description = "Work order not found")
    })
    public ResponseEntity<WorkOrder> resumeWork(
            @PathVariable @Parameter(description = "Work order ID") UUID id) {
        
        logger.info("Resume work order: {}", id);
        
        WorkOrder workOrder = workOrderService.resumeWork(id);
        
        logger.info("Work order resumed successfully: {}", id);
        return ResponseEntity.ok(workOrder);
    }
    
    /**
     * Complete work order.
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    @Operation(summary = "Complete work order", description = "Mark a work order as completed")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work order completed successfully"),
        @ApiResponse(responseCode = "400", description = "Work order cannot be completed"),
        @ApiResponse(responseCode = "404", description = "Work order not found")
    })
    public ResponseEntity<WorkOrderSimplifiedResponse> completeWorkOrder(
            @PathVariable @Parameter(description = "Work order ID") UUID id,
            @RequestParam(required = false) @Parameter(description = "Completion notes") String completionNotes,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Complete work order {} by user: {}", id, currentUser.getUsername());
        
        WorkOrderSimplifiedResponse workOrder = workOrderService.completeWorkOrder(id, completionNotes);
        
        logger.info("Work order completed successfully: {}", id);
        return ResponseEntity.ok(workOrder);
    }
    
    /**
     * Verify completed work order.
     */
    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @Transactional
    @Operation(summary = "Verify work order", description = "Verify a completed work order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work order verified successfully"),
        @ApiResponse(responseCode = "400", description = "Work order is not completed"),
        @ApiResponse(responseCode = "404", description = "Work order not found")
    })
    public ResponseEntity<WorkOrder> verifyWorkOrder(
            @PathVariable @Parameter(description = "Work order ID") UUID id,
            @RequestParam @Parameter(description = "Supervisor ID", required = true) UUID supervisorId) {
        
        logger.info("Verify work order {} by supervisor: {}", id, supervisorId);
        
        WorkOrder workOrder = workOrderService.verifyWorkOrder(id, supervisorId);
        
        logger.info("Work order verified successfully: {}", id);
        return ResponseEntity.ok(workOrder);
    }
    
    /**
     * Cancel work order.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Cancel work order", description = "Cancel a work order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work order cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Work order cannot be cancelled"),
        @ApiResponse(responseCode = "404", description = "Work order not found")
    })
    public ResponseEntity<WorkOrderSimplifiedResponse> cancelWorkOrder(
            @PathVariable @Parameter(description = "Work order ID") UUID id,
            @RequestParam @Parameter(description = "Cancellation reason", required = true) String reason,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Cancel work order {} by user: {} with reason: {}", 
                   id, currentUser.getUsername(), reason);
        
        WorkOrderSimplifiedResponse workOrder = workOrderService.cancelWorkOrder(id, reason);
        
        logger.info("Work order cancelled successfully: {}", id);
        return ResponseEntity.ok(workOrder);
    }
    
    // ========== Task Management ==========
    
    /**
     * Add task to work order.
     */
    @PostMapping("/{id}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    @Transactional
    @Operation(summary = "Add task", description = "Add a task to a work order")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Task added successfully"),
        @ApiResponse(responseCode = "404", description = "Work order not found")
    })
    public ResponseEntity<WorkOrderTask> addTask(
            @PathVariable @Parameter(description = "Work order ID") UUID id,
            @Valid @RequestBody WorkOrderTask task) {
        
        logger.info("Add task to work order: {}", id);
        
        WorkOrderTask createdTask = workOrderService.addTask(id, task);
        
        logger.info("Task added successfully to work order: {}", id);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }
    
    /**
     * Update task status.
     */
    @PatchMapping("/tasks/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    @Transactional
    @Operation(summary = "Update task", description = "Update a task's completion status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task updated successfully"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<WorkOrderTask> updateTaskStatus(
            @PathVariable @Parameter(description = "Task ID") UUID taskId,
            @RequestParam @Parameter(description = "Completion status", required = true) boolean isCompleted) {
        
        logger.info("Update task {} - completed: {}", taskId, isCompleted);
        
        WorkOrderTask task = workOrderService.updateTaskStatus(taskId, isCompleted);
        
        logger.info("Task updated successfully: {}", taskId);
        return ResponseEntity.ok(task);
    }
    
    // ========== Material Management ==========
    
    /**
     * Add material to work order.
     */
    @PostMapping("/{id}/materials")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    @Transactional
    @Operation(summary = "Add material", description = "Add material usage to a work order")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Material added successfully"),
        @ApiResponse(responseCode = "404", description = "Work order not found")
    })
    public ResponseEntity<WorkOrderMaterial> addMaterial(
            @PathVariable @Parameter(description = "Work order ID") UUID id,
            @Valid @RequestBody WorkOrderMaterial material) {
        
        logger.info("Add material to work order: {}", id);
        
        WorkOrderMaterial createdMaterial = workOrderService.addMaterial(id, material);
        
        logger.info("Material added successfully to work order: {}", id);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMaterial);
    }
    
    // ========== Query Operations ==========
    
    /**
     * Get work orders by assignee.
     */
    @GetMapping("/assignee/{assigneeId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SUPERVISOR') and @workOrderService.isSameCompany(#assigneeId, authentication.principal.username)) or #assigneeId == authentication.principal.id")
    @Operation(summary = "Get by assignee", description = "Get work orders assigned to a user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work orders retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Page<WorkOrderSimplifiedResponse>> getWorkOrdersByAssignee(
            @PathVariable @Parameter(description = "Assignee ID") UUID assigneeId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        logger.debug("Get work orders for assignee: {}", assigneeId);
        
        Page<WorkOrderSimplifiedResponse> workOrders = workOrderService.getWorkOrdersByAssignee(assigneeId, pageable);
        return ResponseEntity.ok(workOrders);
    }
    
    /**
     * Get work orders by school.
     */
    @GetMapping("/school/{schoolId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Get by school", description = "Get work orders for a school")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work orders retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "School not found")
    })
    public ResponseEntity<Page<WorkOrderSimplifiedResponse>> getWorkOrdersBySchool(
            @PathVariable @Parameter(description = "School ID") UUID schoolId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        logger.debug("Get work orders for school: {}", schoolId);
        
        Page<WorkOrderSimplifiedResponse> workOrders = workOrderService.getWorkOrdersBySchool(schoolId, pageable);
        return ResponseEntity.ok(workOrders);
    }
    
    /**
     * Get overdue work orders.
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get overdue", description = "Get work orders past their due date")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work orders retrieved successfully")
    })
    public ResponseEntity<List<WorkOrder>> getOverdueWorkOrders(
            @RequestParam @Parameter(description = "Company ID", required = true) UUID companyId) {
        
        logger.debug("Get overdue work orders for company: {}", companyId);
        
        List<WorkOrder> workOrders = workOrderService.findOverdueWorkOrders(companyId);
        return ResponseEntity.ok(workOrders);
    }
    
    /**
     * Get high priority pending work orders.
     */
    @GetMapping("/high-priority")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get high priority", description = "Get high priority pending work orders")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work orders retrieved successfully")
    })
    public ResponseEntity<List<WorkOrder>> getHighPriorityPending(
            @RequestParam @Parameter(description = "Company ID", required = true) UUID companyId) {
        
        logger.debug("Get high priority pending work orders for company: {}", companyId);
        
        List<WorkOrder> workOrders = workOrderService.findHighPriorityPending(companyId);
        return ResponseEntity.ok(workOrders);
    }
    
    /**
     * Search work orders.
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Transactional(readOnly = true)
    @Operation(summary = "Search work orders", description = "Search work orders by term")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work orders retrieved successfully")
    })
    public ResponseEntity<Page<WorkOrder>> searchWorkOrders(
            @RequestParam @Parameter(description = "Company ID", required = true) UUID companyId,
            @RequestParam @Parameter(description = "Search term", required = true) String searchTerm,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        logger.debug("Search work orders for company: {} with term: {}", companyId, searchTerm);
        
        Page<WorkOrder> workOrders = workOrderService.searchWorkOrders(companyId, searchTerm, pageable);
        return ResponseEntity.ok(workOrders);
    }
    
    /**
     * Get work order statistics.
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get statistics", description = "Get work order statistics")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    public ResponseEntity<WorkOrderService.WorkOrderStatistics> getStatistics(
            @RequestParam @Parameter(description = "Company ID", required = true) UUID companyId) {
        
        logger.debug("Get work order statistics for company: {}", companyId);
        
        WorkOrderService.WorkOrderStatistics stats = workOrderService.getStatistics(companyId);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get technician performance.
     */
    @GetMapping("/technician/{technicianId}/performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Get technician performance", description = "Get performance metrics for a technician")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Performance metrics retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Technician not found")
    })
    public ResponseEntity<WorkOrderService.TechnicianPerformance> getTechnicianPerformance(
            @PathVariable @Parameter(description = "Technician ID") UUID technicianId,
            @RequestParam @Parameter(description = "Start date", required = true) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @Parameter(description = "End date", required = true) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        logger.debug("Get performance for technician: {} from {} to {}", technicianId, startDate, endDate);
        
        WorkOrderService.TechnicianPerformance performance = 
            workOrderService.getTechnicianPerformance(technicianId, startDate, endDate);
        return ResponseEntity.ok(performance);
    }
    
    /**
     * Auto-schedule work orders.
     */
    @PostMapping("/auto-schedule")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Auto-schedule", description = "Automatically schedule work orders")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Work orders scheduled successfully")
    })
    public ResponseEntity<Void> autoScheduleWorkOrders(
            @RequestParam @Parameter(description = "Company ID", required = true) UUID companyId) {
        
        logger.info("Auto-schedule work orders for company: {}", companyId);
        
        workOrderService.autoScheduleWorkOrders(companyId);
        
        logger.info("Work orders scheduled successfully for company: {}", companyId);
        return ResponseEntity.ok().build();
    }
    
    // ========== Export Operations ==========
    
    /**
     * Export work orders to Excel.
     */
    @GetMapping("/export/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Export work orders to Excel", description = "Export work orders to Excel format")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Excel file generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date range"),
        @ApiResponse(responseCode = "500", description = "Export generation failed")
    })
    public CompletableFuture<ResponseEntity<byte[]>> exportWorkOrdersExcel(
            @RequestParam @Parameter(description = "Start date", required = true) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @Parameter(description = "End date", required = true) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Export work orders Excel from {} to {} by user: {}", 
                   startDate, endDate, currentUser.getUsername());
        
        if (endDate.isBefore(startDate)) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().build());
        }
        
        // Get company ID from user context - simplified for now
        UUID companyId = UUID.randomUUID(); // This should come from user's company
        
        return reportGenerationService.generateWorkOrdersExcel(companyId, startDate, endDate)
            .thenApply(excelData -> {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentDispositionFormData("attachment", 
                    "work-orders-" + startDate + "-to-" + endDate + ".xlsx");
                
                logger.info("Successfully generated work orders Excel export for {} bytes", excelData.length);
                return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
            })
            .exceptionally(throwable -> {
                logger.error("Error generating work orders Excel export", throwable);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            });
    }
    
    /**
     * Export work orders to PDF.
     */
    @GetMapping("/export/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Export work orders to PDF", description = "Export work orders to PDF format")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PDF file generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date range"),
        @ApiResponse(responseCode = "500", description = "Export generation failed")
    })
    public CompletableFuture<ResponseEntity<byte[]>> exportWorkOrdersPDF(
            @RequestParam @Parameter(description = "Start date", required = true) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @Parameter(description = "End date", required = true) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        logger.info("Export work orders PDF from {} to {} by user: {}", 
                   startDate, endDate, currentUser.getUsername());
        
        if (endDate.isBefore(startDate)) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().build());
        }
        
        // Get company ID from user context - simplified for now
        UUID companyId = UUID.randomUUID(); // This should come from user's company
        
        return reportGenerationService.generateWorkOrdersPDF(companyId, startDate, endDate)
            .thenApply(pdfData -> {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("attachment", 
                    "work-orders-" + startDate + "-to-" + endDate + ".pdf");
                
                logger.info("Successfully generated work orders PDF export for {} bytes", pdfData.length);
                return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
            })
            .exceptionally(throwable -> {
                logger.error("Error generating work orders PDF export", throwable);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            });
    }
}