package com.cafm.cafmbackend.service;

import com.cafm.cafmbackend.data.entity.*;
import com.cafm.cafmbackend.data.enums.UserStatus;
import com.cafm.cafmbackend.data.enums.UserType;
import com.cafm.cafmbackend.data.enums.WorkOrderPriority;
import com.cafm.cafmbackend.data.enums.WorkOrderStatus;
import com.cafm.cafmbackend.data.repository.*;
import com.cafm.cafmbackend.dto.workorder.WorkOrderCreateRequest;
import com.cafm.cafmbackend.dto.workorder.WorkOrderProgressRequest;
import com.cafm.cafmbackend.dto.workorder.WorkOrderSimplifiedResponse;
import com.cafm.cafmbackend.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Service class for managing work orders.
 * Handles work order lifecycle, assignment, tracking, and completion.
 */
@Service
@Transactional
public class WorkOrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderService.class);
    
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderTaskRepository workOrderTaskRepository;
    private final WorkOrderMaterialRepository workOrderMaterialRepository;
    private final WorkOrderAttachmentRepository workOrderAttachmentRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final ReportRepository reportRepository;
    private final CompanyRepository companyRepository;
    
    public WorkOrderService(WorkOrderRepository workOrderRepository,
                           WorkOrderTaskRepository workOrderTaskRepository,
                           WorkOrderMaterialRepository workOrderMaterialRepository,
                           WorkOrderAttachmentRepository workOrderAttachmentRepository,
                           UserRepository userRepository,
                           SchoolRepository schoolRepository,
                           ReportRepository reportRepository,
                           CompanyRepository companyRepository) {
        this.workOrderRepository = workOrderRepository;
        this.workOrderTaskRepository = workOrderTaskRepository;
        this.workOrderMaterialRepository = workOrderMaterialRepository;
        this.workOrderAttachmentRepository = workOrderAttachmentRepository;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.reportRepository = reportRepository;
        this.companyRepository = companyRepository;
    }
    
    // ========== DTO Conversion Methods ==========
    
    /**
     * Convert WorkOrder entity to simplified response DTO.
     */
    private WorkOrderSimplifiedResponse toResponse(WorkOrder entity) {
        if (entity == null) return null;
        
        return WorkOrderSimplifiedResponse.builder()
            .id(entity.getId())
            .workOrderNumber(entity.getWorkOrderNumber())
            .title(entity.getTitle())
            .description(entity.getDescription())
            .status(entity.getStatus())
            .priority(entity.getPriority())
            .reportId(entity.getReport() != null ? entity.getReport().getId() : null)
            .schoolId(entity.getSchool() != null ? entity.getSchool().getId() : null)
            .schoolName(entity.getSchool() != null ? entity.getSchool().getName() : null)
            .assignedToId(entity.getAssignedTo() != null ? entity.getAssignedTo().getId() : null)
            .assignedToName(entity.getAssignedTo() != null ? entity.getAssignedTo().getFullName() : null)
            .companyId(entity.getCompany() != null ? entity.getCompany().getId() : null)
            .scheduledStart(entity.getScheduledStart())
            .scheduledEnd(entity.getScheduledEnd())
            .actualStart(entity.getActualStart())
            .actualEnd(entity.getActualEnd())
            .completionPercentage(entity.getCompletionPercentage())
            .actualHours(entity.getActualHours() != null ? entity.getActualHours().doubleValue() : null)
            .verifiedAt(entity.getVerifiedAt())
            .verifiedBy(entity.getVerifiedBy() != null ? entity.getVerifiedBy().getId() : null)
            .instructions(null) // field doesn't exist in entity
            .completionNotes(entity.getCompletionNotes())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
    
    /**
     * Convert WorkOrderCreateRequest to WorkOrder entity.
     */
    private WorkOrder fromCreateRequest(WorkOrderCreateRequest request, Company company) {
        WorkOrder entity = new WorkOrder();
        
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setPriority(request.priority() != null ? request.priority() : WorkOrderPriority.MEDIUM);
        entity.setScheduledStart(request.scheduledStart());
        entity.setScheduledEnd(request.scheduledEnd());
        // entity.setInstructions(request.instructions()); // field doesn't exist
        entity.setCompany(company);
        
        // Set defaults
        entity.setStatus(WorkOrderStatus.PENDING);
        entity.setCompletionPercentage(0);
        entity.setActualHours(BigDecimal.ZERO);
        
        return entity;
    }
    
    /**
     * Update work order progress from request.
     */
    private void updateProgress(WorkOrder entity, WorkOrderProgressRequest request) {
        if (request.progressPercentage() != null) {
            entity.setCompletionPercentage(request.progressPercentage());
        }
        if (request.progressNotes() != null) {
            entity.setCompletionNotes(request.progressNotes());
        }
        if (request.actualHours() != null) {
            entity.setActualHours(request.actualHours());
        }
        
        // Update status based on completion
        if (entity.getCompletionPercentage() >= 100) {
            entity.setStatus(WorkOrderStatus.COMPLETED);
            entity.setActualEnd(LocalDateTime.now());
        } else if (entity.getCompletionPercentage() > 0) {
            entity.setStatus(WorkOrderStatus.IN_PROGRESS);
            if (entity.getActualStart() == null) {
                entity.setActualStart(LocalDateTime.now());
            }
        }
    }
    
    // ========== Work Order Management ==========
    
    /**
     * Create a new work order using DTO.
     */
    @Transactional
    public WorkOrderSimplifiedResponse createWorkOrder(WorkOrderCreateRequest request) {
        logger.info("Creating new work order: {}", request.title());
        
        // Get school if provided
        School school = null;
        if (request.schoolId() != null) {
            school = schoolRepository.findById(request.schoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found: " + request.schoolId()));
        }
        
        // Get company from school or report
        Company company = null;
        if (school != null) {
            company = school.getCompany();
        }
        if (company == null && request.reportId() != null) {
            Report report = reportRepository.findById(request.reportId())
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + request.reportId()));
            company = report.getCompany();
        }
        if (company == null) {
            throw new IllegalArgumentException("Unable to determine company for work order");
        }
        
        // Convert from request
        WorkOrder workOrder = fromCreateRequest(request, company);
        
        // Generate unique work order number
        String workOrderNumber = generateWorkOrderNumber();
        while (workOrderRepository.existsByWorkOrderNumberAndCompanyId(workOrderNumber, company.getId())) {
            workOrderNumber = generateWorkOrderNumber();
        }
        workOrder.setWorkOrderNumber(workOrderNumber);
        
        // Set report if provided
        if (request.reportId() != null) {
            Report report = reportRepository.findById(request.reportId())
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + request.reportId()));
            workOrder.setReport(report);
            workOrder.setSchool(report.getSchool());
            
            // Update report status
            report.setStatus(com.cafm.cafmbackend.data.enums.ReportStatus.IN_PROGRESS);
            reportRepository.save(report);
        } else if (request.schoolId() != null) {
            // School already fetched above
            if (school != null) {
                workOrder.setSchool(school);
            }
        }
        
        // Set assigned user if provided
        if (request.assignedToId() != null) {
            User assignee = userRepository.findById(request.assignedToId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.assignedToId()));
            
            if (!assignee.isTechnician()) {
                throw new IllegalArgumentException("User is not a technician: " + request.assignedToId());
            }
            
            workOrder.setAssignedTo(assignee);
            workOrder.setStatus(WorkOrderStatus.ASSIGNED);
        }
        
        // Save and return
        workOrder = workOrderRepository.save(workOrder);
        logger.info("Created work order with ID: {}", workOrder.getId());
        return toResponse(workOrder);
    }
    
    /**
     * Create a new work order (legacy method).
     */
    public WorkOrder createWorkOrder(WorkOrder workOrder, UUID companyId, UUID assignedById) {
        logger.info("Creating new work order for company: {}", companyId);
        
        // Generate unique work order number
        String workOrderNumber = generateWorkOrderNumber();
        while (workOrderRepository.existsByWorkOrderNumberAndCompanyId(workOrderNumber, companyId)) {
            workOrderNumber = generateWorkOrderNumber();
        }
        workOrder.setWorkOrderNumber(workOrderNumber);
        
        // Set company
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
        workOrder.setCompany(company);
        
        // Set assigned by
        User assignedBy = userRepository.findById(assignedById)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + assignedById));
        workOrder.setAssignedBy(assignedBy);
        workOrder.setAssignmentDate(LocalDateTime.now());
        
        // Set defaults
        if (workOrder.getStatus() == null) {
            workOrder.setStatus(WorkOrderStatus.PENDING);
        }
        if (workOrder.getPriority() == null) {
            workOrder.setPriority(WorkOrderPriority.MEDIUM);
        }
        if (workOrder.getCompletionPercentage() == null) {
            workOrder.setCompletionPercentage(0);
        }
        
        WorkOrder savedWorkOrder = workOrderRepository.save(workOrder);
        logger.info("Work order created successfully: {}", savedWorkOrder.getWorkOrderNumber());
        
        return savedWorkOrder;
    }
    
    /**
     * Get work order by ID with DTO conversion.
     */
    public WorkOrderSimplifiedResponse getWorkOrderById(UUID id) {
        WorkOrder workOrder = findById(id);
        return toResponse(workOrder);
    }
    
    /**
     * Update work order progress using DTO.
     */
    @Transactional
    public WorkOrderSimplifiedResponse updateWorkOrderProgress(UUID id, WorkOrderProgressRequest request) {
        logger.info("Updating work order progress: {}", id);
        
        WorkOrder workOrder = findById(id);
        
        // Validate status for updating
        if (workOrder.getStatus() == WorkOrderStatus.COMPLETED || 
            workOrder.getStatus() == WorkOrderStatus.CANCELLED ||
            workOrder.getStatus() == WorkOrderStatus.VERIFIED) {
            throw new IllegalStateException("Cannot update progress for work order in status: " + workOrder.getStatus());
        }
        
        updateProgress(workOrder, request);
        workOrder = workOrderRepository.save(workOrder);
        
        logger.info("Updated work order progress: {} - {}%", id, workOrder.getCompletionPercentage());
        return toResponse(workOrder);
    }
    
    /**
     * Get paginated list of work orders.
     */
    public Page<WorkOrderSimplifiedResponse> getWorkOrders(Pageable pageable) {
        return workOrderRepository.findAll(pageable)
            .map(this::toResponse);
    }
    
    /**
     * Get work orders by assignee.
     */
    public Page<WorkOrderSimplifiedResponse> getWorkOrdersByAssignee(UUID assigneeId, Pageable pageable) {
        return workOrderRepository.findByAssignedToIdAndDeletedAtIsNull(assigneeId, pageable)
            .map(this::toResponse);
    }
    
    /**
     * Get work orders by school.
     */
    public Page<WorkOrderSimplifiedResponse> getWorkOrdersBySchool(UUID schoolId, Pageable pageable) {
        return workOrderRepository.findBySchoolIdAndDeletedAtIsNull(schoolId, pageable)
            .map(this::toResponse);
    }
    
    /**
     * Cancel work order.
     */
    @Transactional
    public WorkOrderSimplifiedResponse cancelWorkOrder(UUID id, String reason) {
        logger.info("Cancelling work order: {}", id);
        
        WorkOrder workOrder = findById(id);
        
        // Validate status
        if (workOrder.getStatus() == WorkOrderStatus.COMPLETED || 
            workOrder.getStatus() == WorkOrderStatus.VERIFIED) {
            throw new IllegalStateException("Cannot cancel completed or verified work order");
        }
        
        workOrder.setStatus(WorkOrderStatus.CANCELLED);
        workOrder.setCompletionNotes(reason);
        workOrder.setActualEnd(LocalDateTime.now());
        
        workOrder = workOrderRepository.save(workOrder);
        logger.info("Cancelled work order: {}", id);
        return toResponse(workOrder);
    }
    
    /**
     * Complete work order.
     */
    @Transactional
    public WorkOrderSimplifiedResponse completeWorkOrder(UUID id, String completionNotes) {
        logger.info("Completing work order: {}", id);
        
        WorkOrder workOrder = findById(id);
        
        // Validate status
        if (workOrder.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new IllegalStateException("Work order must be in progress to complete");
        }
        
        workOrder.setStatus(WorkOrderStatus.COMPLETED);
        workOrder.setCompletionPercentage(100);
        workOrder.setCompletionNotes(completionNotes);
        workOrder.setActualEnd(LocalDateTime.now());
        
        // Calculate actual hours if start time is set
        if (workOrder.getActualStart() != null) {
            long hours = ChronoUnit.HOURS.between(workOrder.getActualStart(), workOrder.getActualEnd());
            workOrder.setActualHours(BigDecimal.valueOf(hours));
        }
        
        workOrder = workOrderRepository.save(workOrder);
        
        // Update related report if exists
        if (workOrder.getReport() != null) {
            Report report = workOrder.getReport();
            report.setStatus(com.cafm.cafmbackend.data.enums.ReportStatus.COMPLETED);
            report.setCompletedDate(LocalDate.now());
            reportRepository.save(report);
        }
        
        logger.info("Completed work order: {}", id);
        return toResponse(workOrder);
    }
    
    /**
     * Create work order from report.
     */
    public WorkOrder createFromReport(UUID reportId, UUID companyId, UUID assignedById) {
        logger.info("Creating work order from report: {}", reportId);
        
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));
        
        WorkOrder workOrder = new WorkOrder();
        workOrder.setReport(report);
        workOrder.setTitle(report.getTitle());
        workOrder.setDescription(report.getDescription());
        workOrder.setSchool(report.getSchool());
        
        // Map report priority to work order priority
        if (report.getPriority() != null) {
            workOrder.setPriority(mapReportPriority(report.getPriority()));
        }
        
        // Set scheduled dates based on report
        if (report.getScheduledDate() != null) {
            workOrder.setScheduledStart(report.getScheduledDate().atStartOfDay());
            workOrder.setScheduledEnd(report.getScheduledDate().atTime(17, 0)); // Default end of day
        }
        
        return createWorkOrder(workOrder, companyId, assignedById);
    }
    
    /**
     * Update work order details.
     */
    public WorkOrder updateWorkOrder(UUID workOrderId, WorkOrder updatedWorkOrder) {
        logger.info("Updating work order: {}", workOrderId);
        
        WorkOrder existingWorkOrder = findById(workOrderId);
        
        // Check if work order can be edited
        if (existingWorkOrder.getStatus().isFinal()) {
            throw new IllegalStateException("Cannot update finalized work order");
        }
        
        // Update allowed fields
        if (updatedWorkOrder.getTitle() != null) {
            existingWorkOrder.setTitle(updatedWorkOrder.getTitle());
        }
        if (updatedWorkOrder.getDescription() != null) {
            existingWorkOrder.setDescription(updatedWorkOrder.getDescription());
        }
        if (updatedWorkOrder.getPriority() != null) {
            existingWorkOrder.setPriority(updatedWorkOrder.getPriority());
        }
        if (updatedWorkOrder.getCategory() != null) {
            existingWorkOrder.setCategory(updatedWorkOrder.getCategory());
        }
        if (updatedWorkOrder.getScheduledStart() != null) {
            existingWorkOrder.setScheduledStart(updatedWorkOrder.getScheduledStart());
        }
        if (updatedWorkOrder.getScheduledEnd() != null) {
            existingWorkOrder.setScheduledEnd(updatedWorkOrder.getScheduledEnd());
        }
        if (updatedWorkOrder.getEstimatedHours() != null) {
            existingWorkOrder.setEstimatedHours(updatedWorkOrder.getEstimatedHours());
        }
        if (updatedWorkOrder.getLocationDetails() != null) {
            existingWorkOrder.setLocationDetails(updatedWorkOrder.getLocationDetails());
        }
        
        return workOrderRepository.save(existingWorkOrder);
    }
    
    /**
     * Assign work order to technician.
     */
    public WorkOrder assignToTechnician(UUID workOrderId, UUID technicianId) {
        logger.info("Assigning work order {} to technician {}", workOrderId, technicianId);
        
        WorkOrder workOrder = findById(workOrderId);
        
        // Validate status
        if (workOrder.getStatus() != WorkOrderStatus.PENDING && 
            workOrder.getStatus() != WorkOrderStatus.ASSIGNED) {
            throw new IllegalStateException("Work order must be PENDING or ASSIGNED to reassign");
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
        
        workOrder.setAssignedTo(technician);
        workOrder.setAssignmentDate(LocalDateTime.now());
        workOrder.setStatus(WorkOrderStatus.ASSIGNED);
        
        return workOrderRepository.save(workOrder);
    }
    
    /**
     * Start work on order.
     */
    public WorkOrder startWork(UUID workOrderId, UUID technicianId) {
        logger.info("Starting work on order: {}", workOrderId);
        
        WorkOrder workOrder = findById(workOrderId);
        
        User technician = userRepository.findById(technicianId)
            .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + technicianId));
        
        workOrder.startWork(technician);
        
        return workOrderRepository.save(workOrder);
    }
    
    /**
     * Update work progress.
     */
    public WorkOrder updateProgress(UUID workOrderId, Integer completionPercentage, String notes) {
        logger.info("Updating progress for work order {}: {}%", workOrderId, completionPercentage);
        
        WorkOrder workOrder = findById(workOrderId);
        
        if (workOrder.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only update progress for in-progress work orders");
        }
        
        workOrder.setCompletionPercentage(completionPercentage);
        
        if (notes != null) {
            String existingNotes = workOrder.getCompletionNotes();
            if (existingNotes != null) {
                workOrder.setCompletionNotes(existingNotes + "\n[" + LocalDateTime.now() + "] " + notes);
            } else {
                workOrder.setCompletionNotes("[" + LocalDateTime.now() + "] " + notes);
            }
        }
        
        return workOrderRepository.save(workOrder);
    }
    
    /**
     * Put work order on hold.
     */
    public WorkOrder putOnHold(UUID workOrderId, String reason) {
        logger.info("Putting work order {} on hold: {}", workOrderId, reason);
        
        WorkOrder workOrder = findById(workOrderId);
        workOrder.putOnHold(reason);
        
        return workOrderRepository.save(workOrder);
    }
    
    /**
     * Resume work order from hold.
     */
    public WorkOrder resumeWork(UUID workOrderId) {
        logger.info("Resuming work order: {}", workOrderId);
        
        WorkOrder workOrder = findById(workOrderId);
        
        if (workOrder.getStatus() != WorkOrderStatus.ON_HOLD) {
            throw new IllegalStateException("Can only resume work orders that are on hold");
        }
        
        workOrder.setStatus(WorkOrderStatus.IN_PROGRESS);
        
        return workOrderRepository.save(workOrder);
    }
    
    /**
     * Complete work order.
     */
    public WorkOrder completeWorkOrder(UUID workOrderId, String completionNotes, 
                                       BigDecimal actualHours, String signatureUrl) {
        logger.info("Completing work order: {}", workOrderId);
        
        WorkOrder workOrder = findById(workOrderId);
        workOrder.complete(completionNotes);
        
        if (actualHours != null) {
            workOrder.setActualHours(actualHours);
            
            // Calculate labor cost if hourly rate is available
            if (workOrder.getAssignedTo() != null && workOrder.getAssignedTo().getHourlyRate() != null) {
                BigDecimal laborCost = actualHours.multiply(
                    BigDecimal.valueOf(workOrder.getAssignedTo().getHourlyRate())
                );
                workOrder.setLaborCost(laborCost);
            }
        }
        
        if (signatureUrl != null) {
            workOrder.setSignatureUrl(signatureUrl);
        }
        
        // Total cost is calculated automatically in the entity
        
        return workOrderRepository.save(workOrder);
    }
    
    /**
     * Verify completed work order.
     */
    public WorkOrder verifyWorkOrder(UUID workOrderId, UUID supervisorId) {
        logger.info("Verifying work order {} by supervisor {}", workOrderId, supervisorId);
        
        WorkOrder workOrder = findById(workOrderId);
        
        User supervisor = userRepository.findById(supervisorId)
            .orElseThrow(() -> new ResourceNotFoundException("Supervisor not found: " + supervisorId));
        
        if (!supervisor.isSupervisor() && !supervisor.isAdmin()) {
            throw new IllegalArgumentException("User must be supervisor or admin to verify work orders");
        }
        
        workOrder.verify(supervisor);
        
        return workOrderRepository.save(workOrder);
    }
    
    /**
     * Cancel work order.
     */
    public WorkOrder cancelWorkOrderLegacy(UUID workOrderId, String reason) {
        logger.info("Cancelling work order {}: {}", workOrderId, reason);
        
        WorkOrder workOrder = findById(workOrderId);
        workOrder.cancel(reason);
        
        return workOrderRepository.save(workOrder);
    }
    
    /**
     * Soft delete work order.
     */
    public void deleteWorkOrder(UUID workOrderId) {
        logger.info("Soft deleting work order: {}", workOrderId);
        
        WorkOrder workOrder = findById(workOrderId);
        workOrder.setDeletedAt(LocalDateTime.now());
        
        workOrderRepository.save(workOrder);
    }
    
    // ========== Task Management ==========
    
    /**
     * Add task to work order.
     */
    public WorkOrderTask addTask(UUID workOrderId, WorkOrderTask task) {
        logger.info("Adding task to work order: {}", workOrderId);
        
        WorkOrder workOrder = findById(workOrderId);
        
        if (workOrder.getStatus().isFinal()) {
            throw new IllegalStateException("Cannot add tasks to finalized work order");
        }
        
        task.setWorkOrder(workOrder);
        WorkOrderTask savedTask = workOrderTaskRepository.save(task);
        
        // Update work order completion percentage based on tasks
        updateCompletionFromTasks(workOrder);
        
        return savedTask;
    }
    
    /**
     * Update task status.
     */
    public WorkOrderTask updateTaskStatus(UUID taskId, boolean isCompleted) {
        WorkOrderTask task = workOrderTaskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        
        if (isCompleted) {
            task.setStatus("completed");
            task.setCompletedAt(LocalDateTime.now());
        } else {
            task.setStatus("pending");
            task.setCompletedAt(null);
        }
        
        WorkOrderTask savedTask = workOrderTaskRepository.save(task);
        
        // Update work order completion percentage
        updateCompletionFromTasks(task.getWorkOrder());
        
        return savedTask;
    }
    
    /**
     * Update work order completion based on tasks.
     */
    private void updateCompletionFromTasks(WorkOrder workOrder) {
        List<WorkOrderTask> tasks = workOrderTaskRepository.findByWorkOrderId(workOrder.getId());
        
        if (tasks.isEmpty()) {
            return;
        }
        
        long completedTasks = tasks.stream().filter(WorkOrderTask::isCompleted).count();
        int percentage = (int) ((completedTasks * 100) / tasks.size());
        
        workOrder.setCompletionPercentage(percentage);
        workOrderRepository.save(workOrder);
    }
    
    // ========== Material Management ==========
    
    /**
     * Add material to work order.
     */
    public WorkOrderMaterial addMaterial(UUID workOrderId, WorkOrderMaterial material) {
        logger.info("Adding material to work order: {}", workOrderId);
        
        WorkOrder workOrder = findById(workOrderId);
        
        if (workOrder.getStatus().isFinal()) {
            throw new IllegalStateException("Cannot add materials to finalized work order");
        }
        
        material.setWorkOrder(workOrder);
        WorkOrderMaterial savedMaterial = workOrderMaterialRepository.save(material);
        
        // Update work order material cost
        updateMaterialCost(workOrder);
        
        return savedMaterial;
    }
    
    /**
     * Update work order material cost.
     */
    private void updateMaterialCost(WorkOrder workOrder) {
        List<WorkOrderMaterial> materials = workOrderMaterialRepository.findByWorkOrderId(workOrder.getId());
        
        BigDecimal totalMaterialCost = materials.stream()
            .map(WorkOrderMaterial::getTotalCost)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        workOrder.setMaterialCost(totalMaterialCost);
        // Total cost is calculated automatically in the entity
        workOrderRepository.save(workOrder);
    }
    
    // ========== Query Methods ==========
    
    /**
     * Find work order by ID.
     */
    public WorkOrder findById(UUID workOrderId) {
        return workOrderRepository.findById(workOrderId)
            .orElseThrow(() -> new ResourceNotFoundException("Work order not found: " + workOrderId));
    }
    
    /**
     * Find work order by number.
     */
    public Optional<WorkOrder> findByWorkOrderNumber(String workOrderNumber, UUID companyId) {
        return workOrderRepository.findByWorkOrderNumberAndCompanyId(workOrderNumber, companyId);
    }
    
    /**
     * Find work orders by company.
     */
    public Page<WorkOrder> findByCompany(UUID companyId, Pageable pageable) {
        return workOrderRepository.findByCompanyIdAndDeletedAtIsNull(companyId, pageable);
    }
    
    /**
     * Find work orders assigned to user.
     */
    public Page<WorkOrder> findAssignedToUser(UUID userId, List<WorkOrderStatus> statuses, Pageable pageable) {
        return workOrderRepository.findByAssignedToAndStatuses(userId, statuses, pageable);
    }
    
    /**
     * Find work orders by school.
     */
    public Page<WorkOrder> findBySchool(UUID schoolId, Pageable pageable) {
        return workOrderRepository.findBySchoolIdAndDeletedAtIsNull(schoolId, pageable);
    }
    
    /**
     * Find overdue work orders.
     */
    public List<WorkOrder> findOverdueWorkOrders(UUID companyId) {
        List<WorkOrderStatus> excludeStatuses = Arrays.asList(
            WorkOrderStatus.COMPLETED, 
            WorkOrderStatus.CANCELLED, 
            WorkOrderStatus.VERIFIED
        );
        return workOrderRepository.findOverdueWorkOrders(companyId, LocalDateTime.now(), excludeStatuses);
    }
    
    /**
     * Find high priority pending work orders.
     */
    public List<WorkOrder> findHighPriorityPending(UUID companyId) {
        List<WorkOrderPriority> highPriorities = Arrays.asList(
            WorkOrderPriority.EMERGENCY, 
            WorkOrderPriority.HIGH
        );
        List<WorkOrderStatus> excludeStatuses = Arrays.asList(
            WorkOrderStatus.COMPLETED, 
            WorkOrderStatus.CANCELLED, 
            WorkOrderStatus.VERIFIED
        );
        return workOrderRepository.findHighPriorityPending(companyId, highPriorities, excludeStatuses);
    }
    
    /**
     * Search work orders.
     */
    public Page<WorkOrder> searchWorkOrders(UUID companyId, String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findByCompany(companyId, pageable);
        }
        return workOrderRepository.searchWorkOrders(companyId, searchTerm, pageable);
    }
    
    // ========== Statistics Methods ==========
    
    /**
     * Get work order statistics for company.
     */
    @Transactional(readOnly = true)
    public WorkOrderStatistics getStatistics(UUID companyId) {
        WorkOrderStatistics stats = new WorkOrderStatistics();
        
        // Count by status
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            long count = workOrderRepository.countByCompanyIdAndStatus(companyId, status);
            stats.countByStatus.put(status, count);
            stats.totalWorkOrders += count;
        }
        
        // Calculate metrics
        stats.pendingCount = stats.countByStatus.getOrDefault(WorkOrderStatus.PENDING, 0L);
        stats.inProgressCount = stats.countByStatus.getOrDefault(WorkOrderStatus.IN_PROGRESS, 0L);
        stats.completedCount = stats.countByStatus.getOrDefault(WorkOrderStatus.COMPLETED, 0L);
        stats.verifiedCount = stats.countByStatus.getOrDefault(WorkOrderStatus.VERIFIED, 0L);
        
        // Overdue count
        stats.overdueCount = findOverdueWorkOrders(companyId).size();
        
        // Average completion rate
        List<WorkOrderStatus> includeStatuses = Arrays.asList(
            WorkOrderStatus.IN_PROGRESS, 
            WorkOrderStatus.COMPLETED, 
            WorkOrderStatus.VERIFIED
        );
        Double avgCompletion = workOrderRepository.getAverageCompletionRate(companyId, includeStatuses);
        stats.averageCompletionRate = avgCompletion != null ? avgCompletion : 0.0;
        
        // Calculate this month's costs
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime monthEnd = monthStart.plusMonths(1);
        
        BigDecimal monthlyTotal = workOrderRepository.getTotalCostInPeriod(companyId, monthStart, monthEnd, WorkOrderStatus.COMPLETED);
        stats.totalCostThisMonth = monthlyTotal != null ? monthlyTotal : BigDecimal.ZERO;
        
        // Recent activity
        stats.createdThisWeek = workOrderRepository.countRecentWorkOrders(
            companyId, LocalDateTime.now().minusDays(7)
        );
        
        return stats;
    }
    
    /**
     * Get technician performance metrics.
     */
    @Transactional(readOnly = true)
    public TechnicianPerformance getTechnicianPerformance(UUID technicianId, LocalDateTime startDate, LocalDateTime endDate) {
        TechnicianPerformance performance = new TechnicianPerformance();
        
        performance.technicianId = technicianId;
        
        // Get all work orders for technician in period
        List<WorkOrderStatus> allStatuses = Arrays.asList(WorkOrderStatus.values());
        List<WorkOrder> workOrders = workOrderRepository.findByAssignedToAndStatuses(
            technicianId, allStatuses, Pageable.unpaged()
        ).getContent();
        
        // Filter by date range
        workOrders = workOrders.stream()
            .filter(wo -> wo.getCreatedAt().isAfter(startDate) && wo.getCreatedAt().isBefore(endDate))
            .collect(Collectors.toList());
        
        performance.totalAssigned = workOrders.size();
        performance.completed = workOrders.stream()
            .filter(wo -> wo.getStatus() == WorkOrderStatus.COMPLETED || wo.getStatus() == WorkOrderStatus.VERIFIED)
            .count();
        performance.inProgress = workOrders.stream()
            .filter(wo -> wo.getStatus() == WorkOrderStatus.IN_PROGRESS)
            .count();
        
        // Calculate average completion time
        List<WorkOrder> completedOrders = workOrders.stream()
            .filter(wo -> wo.getStatus() == WorkOrderStatus.COMPLETED || wo.getStatus() == WorkOrderStatus.VERIFIED)
            .filter(wo -> wo.getActualStart() != null && wo.getActualEnd() != null)
            .collect(Collectors.toList());
        
        if (!completedOrders.isEmpty()) {
            double avgHours = completedOrders.stream()
                .mapToLong(wo -> ChronoUnit.HOURS.between(wo.getActualStart(), wo.getActualEnd()))
                .average()
                .orElse(0);
            performance.averageCompletionHours = avgHours;
        }
        
        // Calculate total hours worked
        performance.totalHoursWorked = workOrders.stream()
            .map(WorkOrder::getActualHours)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate completion rate
        if (performance.totalAssigned > 0) {
            performance.completionRate = (performance.completed * 100.0) / performance.totalAssigned;
        }
        
        return performance;
    }
    
    // ========== Utility Methods ==========
    
    /**
     * Generate unique work order number.
     */
    private String generateWorkOrderNumber() {
        String prefix = "WO";
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return prefix + "-" + datePart + "-" + randomPart;
    }
    
    /**
     * Map report priority to work order priority.
     */
    private WorkOrderPriority mapReportPriority(com.cafm.cafmbackend.data.enums.ReportPriority reportPriority) {
        return switch (reportPriority) {
            case CRITICAL -> WorkOrderPriority.EMERGENCY;
            case URGENT -> WorkOrderPriority.HIGH;
            case HIGH -> WorkOrderPriority.HIGH;
            case MEDIUM -> WorkOrderPriority.MEDIUM;
            case LOW -> WorkOrderPriority.LOW;
        };
    }
    
    /**
     * Schedule work orders based on priority and availability.
     */
    @Transactional
    public void autoScheduleWorkOrders(UUID companyId) {
        logger.info("Auto-scheduling work orders for company: {}", companyId);
        
        // Get pending work orders sorted by priority
        List<WorkOrder> pendingOrders = workOrderRepository.findByCompanyIdAndStatus(companyId, WorkOrderStatus.PENDING);
        pendingOrders.sort(Comparator.comparing(WorkOrder::getPriority));
        
        // Get available technicians
        List<User> availableTechnicians = userRepository.findAvailableTechniciansBySpecialization(UserType.TECHNICIAN, null, UserStatus.ACTIVE);
        
        if (availableTechnicians.isEmpty()) {
            logger.warn("No available technicians for auto-scheduling");
            return;
        }
        
        // Simple round-robin assignment
        int technicianIndex = 0;
        for (WorkOrder order : pendingOrders) {
            if (order.getAssignedTo() == null) {
                User technician = availableTechnicians.get(technicianIndex % availableTechnicians.size());
                order.setAssignedTo(technician);
                order.setAssignmentDate(LocalDateTime.now());
                order.setStatus(WorkOrderStatus.ASSIGNED);
                
                // Set scheduled dates if not set
                if (order.getScheduledStart() == null) {
                    LocalDateTime scheduledStart = calculateNextAvailableSlot(technician.getId());
                    order.setScheduledStart(scheduledStart);
                    order.setScheduledEnd(scheduledStart.plusHours(
                        order.getEstimatedHours() != null ? order.getEstimatedHours().intValue() : 2
                    ));
                }
                
                workOrderRepository.save(order);
                technicianIndex++;
            }
        }
        
        logger.info("Auto-scheduled {} work orders", pendingOrders.size());
    }
    
    /**
     * Calculate next available time slot for technician.
     */
    private LocalDateTime calculateNextAvailableSlot(UUID technicianId) {
        // Get technician's current work orders
        List<WorkOrderStatus> activeStatuses = Arrays.asList(
            WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS
        );
        
        List<WorkOrder> currentOrders = workOrderRepository.findByAssignedToAndStatuses(
            technicianId, activeStatuses, Pageable.unpaged()
        ).getContent();
        
        // Find the latest scheduled end time
        LocalDateTime latestEnd = currentOrders.stream()
            .map(WorkOrder::getScheduledEnd)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());
        
        // Add buffer time (30 minutes) and ensure it's within working hours
        LocalDateTime nextSlot = latestEnd.plusMinutes(30);
        
        // If it's after 5 PM, schedule for next day 8 AM
        if (nextSlot.getHour() >= 17) {
            nextSlot = nextSlot.plusDays(1).withHour(8).withMinute(0);
        }
        
        // Skip weekends
        while (nextSlot.getDayOfWeek().getValue() > 5) {
            nextSlot = nextSlot.plusDays(1);
        }
        
        return nextSlot;
    }
    
    // ========== Inner Classes ==========
    
    /**
     * Work order statistics DTO.
     */
    public static class WorkOrderStatistics {
        public long totalWorkOrders;
        public long pendingCount;
        public long inProgressCount;
        public long completedCount;
        public long verifiedCount;
        public long overdueCount;
        public long createdThisWeek;
        public double averageCompletionRate;
        public BigDecimal totalCostThisMonth;
        public Map<WorkOrderStatus, Long> countByStatus = new HashMap<>();
    }
    
    /**
     * Technician performance DTO.
     */
    public static class TechnicianPerformance {
        public UUID technicianId;
        public long totalAssigned;
        public long completed;
        public long inProgress;
        public double completionRate;
        public double averageCompletionHours;
        public BigDecimal totalHoursWorked;
    }
}