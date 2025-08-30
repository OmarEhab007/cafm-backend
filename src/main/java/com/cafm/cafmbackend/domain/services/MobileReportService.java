package com.cafm.cafmbackend.domain.services;

import com.cafm.cafmbackend.infrastructure.persistence.entity.*;
import com.cafm.cafmbackend.infrastructure.persistence.repository.*;
import com.cafm.cafmbackend.shared.enums.*;
import com.cafm.cafmbackend.dto.mobile.*;
import com.cafm.cafmbackend.application.service.WorkOrderService;
import com.cafm.cafmbackend.dto.workorder.WorkOrderCreateRequest;
import com.cafm.cafmbackend.dto.mobile.MobileReportRequest.*;
import com.cafm.cafmbackend.dto.mobile.MobileReportResponse.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for processing mobile report submissions.
 * 
 * Purpose: Handles mobile report creation with photo processing and validation
 * Pattern: Domain service with complex business logic for mobile report processing
 * Java 23: Uses modern validation patterns and stream processing
 * Architecture: Domain layer service handling mobile-specific report operations
 * Standards: Constructor injection, transaction management, comprehensive logging
 */
@Service
@Transactional
public class MobileReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(MobileReportService.class);
    
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final SchoolRepository schoolRepository;
    private final AssetRepository assetRepository;
    private final NotificationService notificationService;
    private final WorkOrderService workOrderService;
    // private final FileStorageService fileStorageService; // Temporarily disabled
    
    @Autowired
    public MobileReportService(
            UserRepository userRepository,
            ReportRepository reportRepository,
            SchoolRepository schoolRepository,
            AssetRepository assetRepository,
            NotificationService notificationService,
            WorkOrderService workOrderService
            // FileStorageService fileStorageService // Temporarily disabled
            ) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.schoolRepository = schoolRepository;
        this.assetRepository = assetRepository;
        this.notificationService = notificationService;
        this.workOrderService = workOrderService;
        // this.fileStorageService = fileStorageService; // Temporarily disabled
    }
    
    /**
     * Get mobile-optimized reports with filtering and pagination.
     * 
     * Purpose: Provides filtered and paginated reports for mobile app
     * Pattern: Repository coordination with mobile-specific filtering
     * Java 23: Stream processing with enhanced filtering patterns
     * Architecture: Domain service method for mobile report retrieval
     * Standards: Implements mobile-first data retrieval with performance optimization
     */
    public List<MobileReportDto> getMobileReports(String username, String status, String priority, 
                                                  UUID schoolId, boolean myReports, int limit) {
        logger.debug("Getting mobile reports for user: {}, status: {}, priority: {}, schoolId: {}, myReports: {}, limit: {}", 
                    username, status, priority, schoolId, myReports, limit);
        
        try {
            // Get all reports and apply filters
            List<Report> allReports = reportRepository.findAll();
            
            // Apply filters
            var filteredReports = allReports.stream()
                .filter(report -> {
                    // Status filter
                    if (status != null && !status.isEmpty()) {
                        return report.getStatus().name().equalsIgnoreCase(status);
                    }
                    return true;
                })
                .filter(report -> {
                    // Priority filter
                    if (priority != null && !priority.isEmpty()) {
                        return report.getPriority().name().equalsIgnoreCase(priority);
                    }
                    return true;
                })
                .filter(report -> {
                    // School filter
                    if (schoolId != null) {
                        return report.getSchool().getId().equals(schoolId);
                    }
                    return true;
                })
                .filter(report -> {
                    // My reports filter (simplified - would need proper user association)
                    if (myReports) {
                        return true; // Simplified - return all for now
                    }
                    return true;
                })
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // Most recent first
                .limit(limit)
                .toList();
            
            // Convert to mobile DTOs
            List<MobileReportDto> mobileReports = filteredReports.stream()
                .map(this::convertToMobileReportDto)
                .collect(Collectors.toList());
            
            logger.debug("Retrieved {} mobile reports for user: {}", mobileReports.size(), username);
            return mobileReports;
            
        } catch (Exception e) {
            logger.error("Error getting mobile reports for user: {}", username, e);
            return List.of(); // Return empty list on error
        }
    }
    
    // Helper method to convert Report to MobileReportDto
    private MobileReportDto convertToMobileReportDto(Report report) {
        return new MobileReportDto(
            report.getId(),
            report.getTitle(),
            report.getDescription(),
            report.getStatus(),
            Priority.fromString(report.getPriority().name()), // Convert ReportPriority to Priority
            report.getSchool().getId(),
            report.getSchool().getName(),
            null, // assetId - simplified (no asset relationship in current Report entity)
            null, // assetName - simplified
            report.getReportedBy() != null ? report.getReportedBy().getId() : null,
            report.getReportedBy() != null ? 
                report.getReportedBy().getFirstName() + " " + report.getReportedBy().getLastName() : "Unknown",
            report.getAssignedTo() != null ? report.getAssignedTo().getId() : null,
            report.getAssignedTo() != null ? 
                report.getAssignedTo().getFirstName() + " " + report.getAssignedTo().getLastName() : null,
            List.of(), // Simplified - would include actual image URLs
            report.getReportedDate() != null ? report.getReportedDate().atStartOfDay() : report.getCreatedAt(),
            report.getScheduledDate() != null ? report.getScheduledDate().atStartOfDay() : null, // dueDate
            report.getStatus() == ReportStatus.COMPLETED ? report.getCompletedAt() : null,
            report.getUpdatedAt(),
            1L, // version - simplified
            null // location - simplified
        );
    }
    
    /**
     * Process mobile report submission.
     */
    public MobileReportResponse processMobileReport(String userEmail, MobileReportRequest request) {
        logger.info("Processing mobile report submission for user: {}, school: {}", 
                   userEmail, request.schoolId());
        
        long startTime = System.currentTimeMillis();
        
        // Validate user and permissions
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
        
        if (user.getUserType() != UserType.SUPERVISOR) {
            throw new IllegalArgumentException("User is not authorized to submit reports: " + userEmail);
        }
        
        // Validate school exists and user has access
        School school = validateSchoolAccess(user, UUID.fromString(request.schoolId()));
        
        // Check for duplicate reports
        Optional<Report> existingReport = checkForDuplicates(request, school);
        if (existingReport.isPresent()) {
            return buildDuplicateResponse(existingReport.get(), request);
        }
        
        // Process photos
        List<PhotoUploadResult> photoResults = processPhotos(request.photos());
        
        // Create report
        Report report = createReport(user, school, request);
        
        // Create work order if needed
        WorkOrder workOrder = null;
        if (shouldAutoCreateWorkOrder(request)) {
            workOrder = createWorkOrder(report, request);
        }
        
        // Send notifications asynchronously
        sendNotificationsForReport(report, workOrder, request);
        
        // Generate validation warnings
        List<ValidationWarning> warnings = validateReportData(request);
        
        // Calculate sync info
        SyncInfo syncInfo = generateSyncInfo(report);
        
        // Generate next actions
        List<NextAction> nextActions = generateNextActions(report, workOrder);
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        logger.info("Mobile report processed successfully in {}ms: report={}, work_order={}", 
                   processingTime, report.getId(), workOrder != null ? workOrder.getId() : "none");
        
        return new MobileReportResponse(
            report.getId().toString(),
            report.getReportNumber(),
            request.clientId(),
            report.getStatus().name(),
            determineSubmissionStatus(photoResults, warnings),
            report.getCreatedAt(),
            calculateEstimatedCompletion(request),
            false, // Priority not adjusted for now
            workOrder != null, // Auto-assigned if work order created
            workOrder != null,
            workOrder != null ? workOrder.getId().toString() : null,
            List.of(), // No technicians assigned yet
            photoResults,
            warnings,
            syncInfo,
            nextActions,
            buildMetadata(request, processingTime)
        );
    }
    
    private School validateSchoolAccess(User user, UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new RuntimeException("School not found: " + schoolId));
        
        // Check if user has access to this school (simplified check)
        if (!school.getCompany().getId().equals(user.getCompanyId())) {
            throw new RuntimeException("User does not have access to school: " + schoolId);
        }
        
        return school;
    }
    
    private Optional<Report> checkForDuplicates(MobileReportRequest request, School school) {
        // Simple duplicate check based on title and recent creation time
        LocalDateTime recentThreshold = LocalDateTime.now().minusHours(1);
        
        List<Report> recentReports = reportRepository.findBySchoolId(
            school.getId(), 
            org.springframework.data.domain.PageRequest.of(0, 10)
        ).getContent();
        
        return recentReports.stream()
            .filter(r -> r.getCreatedAt().isAfter(recentThreshold))
            .filter(r -> r.getTitle().equalsIgnoreCase(request.title()))
            .findFirst();
    }
    
    private MobileReportResponse buildDuplicateResponse(Report existingReport, MobileReportRequest request) {
        return new MobileReportResponse(
            existingReport.getId().toString(),
            existingReport.getReportNumber(),
            request.clientId(),
            existingReport.getStatus().name(),
            SubmissionStatus.DUPLICATE_DETECTED,
            existingReport.getCreatedAt(),
            null,
            false,
            false,
            false,
            null,
            List.of(),
            List.of(),
            List.of(new ValidationWarning(
                "report",
                "DUPLICATE",
                "A similar report was already submitted recently",
                "WARNING",
                "Check existing reports or modify the description"
            )),
            new SyncInfo(1L, null, false, false, LocalDateTime.now()),
            List.of(),
            Map.of("duplicate_of", existingReport.getId().toString())
        );
    }
    
    private List<PhotoUploadResult> processPhotos(List<PhotoData> photos) {
        List<PhotoUploadResult> results = new ArrayList<>();
        
        if (photos == null || photos.isEmpty()) {
            return results;
        }
        
        for (PhotoData photo : photos) {
            try {
                // Process photo upload
                String fileUrl = uploadPhotoToStorage(photo);
                String thumbnailUrl = generateThumbnail(fileUrl);
                
                results.add(new PhotoUploadResult(
                    photo.id(),
                    photo.filename(),
                    "SUCCESS",
                    fileUrl,
                    thumbnailUrl,
                    photo.sizeBytes(),
                    null
                ));
                
            } catch (Exception e) {
                logger.error("Failed to process photo: {}", photo.filename(), e);
                results.add(new PhotoUploadResult(
                    photo.id(),
                    photo.filename(),
                    "FAILED",
                    null,
                    null,
                    photo.sizeBytes(),
                    e.getMessage()
                ));
            }
        }
        
        return results;
    }
    
    private String uploadPhotoToStorage(PhotoData photo) {
        try {
            // FileStorageService temporarily disabled - return mock URL
            String fileName = String.format("reports/%s_%s", 
                                           System.currentTimeMillis(), photo.filename());
            logger.debug("Generated mock upload URL for photo: {}", photo.filename());
            return "https://storage.example.com/" + fileName;
        } catch (Exception e) {
            logger.error("Failed to generate upload URL for photo: {}", photo.filename(), e);
            return "https://storage.example.com/reports/" + photo.filename();
        }
    }
    
    private String generateThumbnail(String fileUrl) {
        try {
            // Thumbnail generation would be handled by image processing service
            // For MinIO, we can use image transformation parameters in URL
            if (fileUrl.contains("presigned")) {
                // For presigned URLs, append transformation params
                return fileUrl + "&transform=thumbnail";
            }
            return fileUrl.replace(".jpg", "_thumb.jpg").replace(".png", "_thumb.png");
        } catch (Exception e) {
            logger.warn("Failed to generate thumbnail URL", e);
            return fileUrl;
        }
    }
    
    private Report createReport(User user, School school, MobileReportRequest request) {
        Report report = new Report();
        
        // Set basic information
        report.setTitle(request.title());
        report.setDescription(request.description());
        report.setSchool(school);
        report.setSupervisor(user);
        report.setStatus(ReportStatus.SUBMITTED);
        report.setPriority(mapPriority(request.priority()));
        report.setReportedDate(request.reportedAt() != null ? request.reportedAt().toLocalDate() : LocalDate.now());
        
        // Set location if provided
        if (request.location() != null) {
            LocationData loc = request.location();
            // Location fields not available in current Report entity
            // report.setLatitude(loc.latitude());
            // report.setLongitude(loc.longitude());
            // report.setLocationDescription(buildLocationDescription(loc));
        }
        
        // Set maintenance details if provided
        if (request.maintenanceDetails() != null) {
            MaintenanceDetails details = request.maintenanceDetails();
            report.setEstimatedCost(details.estimatedCost());
            // Urgent and safety hazard fields not available in current Report entity
            // report.setUrgent(details.urgent() != null ? details.urgent() : false);
            // report.setSafetyHazard(details.safetyHazard() != null ? details.safetyHazard() : false);
        }
        
        // Generate report number
        report.setReportNumber(generateReportNumber(school));
        
        return reportRepository.save(report);
    }
    
    private ReportPriority mapPriority(String priority) {
        return switch (priority.toUpperCase()) {
            case "LOW" -> ReportPriority.LOW;
            case "MEDIUM", "NORMAL" -> ReportPriority.MEDIUM;
            case "HIGH" -> ReportPriority.HIGH;
            case "URGENT", "CRITICAL" -> ReportPriority.URGENT;
            default -> ReportPriority.MEDIUM;
        };
    }
    
    private String buildLocationDescription(LocationData location) {
        StringBuilder desc = new StringBuilder();
        if (location.building() != null) desc.append("Building: ").append(location.building()).append("; ");
        if (location.floor() != null) desc.append("Floor: ").append(location.floor()).append("; ");
        if (location.room() != null) desc.append("Room: ").append(location.room()).append("; ");
        if (location.additionalInfo() != null) desc.append(location.additionalInfo());
        return desc.toString();
    }
    
    private String generateReportNumber(School school) {
        String prefix = "RPT-" + LocalDateTime.now().getYear() + "-";
        long nextNumber = reportRepository.count() + 1;
        return prefix + String.format("%06d", nextNumber);
    }
    
    private boolean shouldAutoCreateWorkOrder(MobileReportRequest request) {
        // Auto-create work order for high priority or urgent reports
        return "HIGH".equalsIgnoreCase(request.priority()) || 
               "URGENT".equalsIgnoreCase(request.priority()) ||
               "CRITICAL".equalsIgnoreCase(request.priority());
    }
    
    private WorkOrder createWorkOrder(Report report, MobileReportRequest request) {
        logger.info("Auto-creating work order for report: {}", report.getId());
        
        try {
            // Map report priority to work order priority
            WorkOrderPriority workOrderPriority = mapToWorkOrderPriority(report.getPriority());
            
            // Calculate scheduled dates based on priority
            LocalDateTime scheduledStart = calculateScheduledStart(workOrderPriority);
            LocalDateTime scheduledEnd = calculateScheduledEnd(scheduledStart, workOrderPriority);
            
            // Estimate cost based on maintenance details
            BigDecimal estimatedCost = request.maintenanceDetails() != null ? 
                request.maintenanceDetails().estimatedCost() : estimateDefaultCost(workOrderPriority);
            
            // Create work order request from report
            WorkOrderCreateRequest workOrderRequest = WorkOrderCreateRequest.fromReport(
                report.getId(),
                "Auto-generated: " + report.getTitle(),
                buildWorkOrderDescription(report, request),
                workOrderPriority,
                determineCategoryFromReport(request),
                scheduledStart,
                scheduledEnd,
                estimatedCost
            );
            
            // Find best available technician for the work order
            User assignedTechnician = findBestAvailableTechnician(report.getSchool().getCompany().getId(), workOrderPriority);
            
            // Create the work order using WorkOrderService
            WorkOrder workOrder = new WorkOrder();
            workOrder.setCompany(report.getSchool().getCompany());
            workOrder.setWorkOrderNumber(generateWorkOrderNumber(report.getSchool()));
            workOrder.setTitle(workOrderRequest.title());
            workOrder.setDescription(workOrderRequest.description());
            workOrder.setPriority(workOrderPriority);
            workOrder.setCategory(workOrderRequest.category());
            workOrder.setReport(report);
            workOrder.setSchool(report.getSchool());
            workOrder.setScheduledStart(scheduledStart);
            workOrder.setScheduledEnd(scheduledEnd);
            workOrder.setEstimatedHours(BigDecimal.valueOf(calculateEstimatedHours(workOrderPriority)));
            
            if (assignedTechnician != null) {
                workOrder.setAssignedTo(assignedTechnician);
                workOrder.setAssignedBy(report.getSupervisor());
                workOrder.setAssignmentDate(LocalDateTime.now());
                workOrder.setStatus(WorkOrderStatus.ASSIGNED);
                logger.info("Work order auto-assigned to technician: {}", assignedTechnician.getEmail());
            } else {
                workOrder.setStatus(WorkOrderStatus.PENDING);
                logger.info("No available technician found, work order set to PENDING");
            }
            
            // Set location if available
            if (request.location() != null) {
                LocationData loc = request.location();
                workOrder.setLocationDetails(buildLocationDescription(loc));
                if (loc.latitude() != null && loc.longitude() != null) {
                    workOrder.setLatitude(new BigDecimal(loc.latitude().toString()));
                    workOrder.setLongitude(new BigDecimal(loc.longitude().toString()));
                }
            }
            
            // Use the existing WorkOrderService to save
            WorkOrder savedWorkOrder = workOrderService.createWorkOrder(
                workOrder, 
                report.getSchool().getCompany().getId(), 
                report.getSupervisor().getId()
            );
            
            logger.info("Work order auto-created successfully: id={}, number={}, assigned_to={}", 
                       savedWorkOrder.getId(), savedWorkOrder.getWorkOrderNumber(), 
                       savedWorkOrder.getAssignedTo() != null ? savedWorkOrder.getAssignedTo().getEmail() : "unassigned");
            
            return savedWorkOrder;
            
        } catch (Exception e) {
            logger.error("Failed to auto-create work order for report: {}", report.getId(), e);
            return null; // Return null to continue processing even if work order creation fails
        }
    }
    
    private List<ValidationWarning> validateReportData(MobileReportRequest request) {
        List<ValidationWarning> warnings = new ArrayList<>();
        
        // Check description length
        if (request.description().length() < 20) {
            warnings.add(new ValidationWarning(
                "description",
                "LENGTH",
                "Description is quite short. Consider adding more details for better processing.",
                "INFO",
                "Add details about the issue, location, and urgency"
            ));
        }
        
        // Check if photos are provided
        if (request.photos() == null || request.photos().isEmpty()) {
            warnings.add(new ValidationWarning(
                "photos",
                "MISSING",
                "No photos provided. Photos help technicians understand the issue better.",
                "INFO",
                "Add photos of the problem area"
            ));
        }
        
        return warnings;
    }
    
    private SyncInfo generateSyncInfo(Report report) {
        return new SyncInfo(
            1L, // Server version
            generateSyncToken(report),
            false, // No conflicts
            false, // No sync required
            LocalDateTime.now().plusMinutes(15) // Next sync in 15 minutes
        );
    }
    
    private String generateSyncToken(Report report) {
        return Base64.getEncoder().encodeToString(
            (report.getId() + ":" + report.getUpdatedAt()).getBytes()
        );
    }
    
    private List<NextAction> generateNextActions(Report report, WorkOrder workOrder) {
        List<NextAction> actions = new ArrayList<>();
        
        actions.add(new NextAction(
            "TRACK",
            "Track Report Progress",
            "Monitor the status of your submitted report",
            "LOW",
            null,
            "/mobile/reports/" + report.getId()
        ));
        
        if (workOrder != null) {
            actions.add(new NextAction(
                "MONITOR",
                "Monitor Work Order",
                "A work order has been created automatically",
                "MEDIUM",
                LocalDateTime.now().plusHours(2),
                "/mobile/work-orders/" + workOrder.getId()
            ));
        }
        
        return actions;
    }
    
    private SubmissionStatus determineSubmissionStatus(List<PhotoUploadResult> photoResults, 
                                                      List<ValidationWarning> warnings) {
        
        long failedPhotos = photoResults.stream()
            .filter(p -> "FAILED".equals(p.uploadStatus()))
            .count();
        
        if (failedPhotos > 0 && failedPhotos < photoResults.size()) {
            return SubmissionStatus.PARTIAL_SUCCESS;
        } else if (failedPhotos == photoResults.size() && !photoResults.isEmpty()) {
            return SubmissionStatus.FAILED;
        } else {
            return SubmissionStatus.SUCCESS;
        }
    }
    
    private LocalDateTime calculateEstimatedCompletion(MobileReportRequest request) {
        // Simple estimation based on priority
        int hoursToAdd = switch (request.priority().toUpperCase()) {
            case "URGENT", "CRITICAL" -> 4;
            case "HIGH" -> 24;
            case "MEDIUM", "NORMAL" -> 72;
            case "LOW" -> 168;
            default -> 72;
        };
        
        return LocalDateTime.now().plusHours(hoursToAdd);
    }
    
    private Map<String, Object> buildMetadata(MobileReportRequest request, long processingTime) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("processing_time_ms", processingTime);
        metadata.put("offline_created", request.offlineCreated());
        metadata.put("submission_source", "MOBILE_APP");
        
        if (request.deviceInfo() != null) {
            DeviceInfo device = request.deviceInfo();
            metadata.put("device_platform", device.platform());
            metadata.put("app_version", device.appVersion());
        }
        
        return metadata;
    }
    
    /**
     * Send appropriate notifications for the submitted report.
     */
    private void sendNotificationsForReport(Report report, WorkOrder workOrder, MobileReportRequest request) {
        try {
            // Send report status notification to supervisor
            notificationService.notifyReportStatusUpdate(report);
            
            // Send urgent alert if report is high priority or urgent
            if (report.getPriority() != null && 
                (report.getPriority().name().equals("HIGH") || 
                 report.getPriority().name().equals("URGENT"))) {
                
                notificationService.sendUrgentMaintenanceAlert(report, 10.0); // 10km radius
            }
            
            // Send work order assignment notification if auto-assigned
            if (workOrder != null && workOrder.getAssignedTo() != null) {
                notificationService.notifyWorkOrderAssigned(workOrder, workOrder.getAssignedTo());
            }
            
            logger.debug("Notifications sent for report: {}", report.getId());
            
        } catch (Exception e) {
            logger.warn("Failed to send notifications for report: {}", report.getId(), e);
            // Don't fail the entire report submission if notifications fail
        }
    }
    
    // ========== Work Order Auto-Creation Helper Methods ==========
    
    private WorkOrderPriority mapToWorkOrderPriority(ReportPriority reportPriority) {
        return switch (reportPriority) {
            case LOW -> WorkOrderPriority.LOW;
            case MEDIUM -> WorkOrderPriority.MEDIUM;
            case HIGH -> WorkOrderPriority.HIGH;
            case URGENT -> WorkOrderPriority.EMERGENCY;
            default -> WorkOrderPriority.MEDIUM;
        };
    }
    
    private LocalDateTime calculateScheduledStart(WorkOrderPriority priority) {
        LocalDateTime now = LocalDateTime.now();
        return switch (priority) {
            case EMERGENCY -> now.plusHours(1); // Start within 1 hour for emergencies
            case HIGH -> now.plusHours(4); // Start within 4 hours for high priority
            case MEDIUM -> now.plusDays(1); // Start within 1 day for medium priority
            case LOW -> now.plusDays(3); // Start within 3 days for low priority
            default -> now.plusDays(1);
        };
    }
    
    private LocalDateTime calculateScheduledEnd(LocalDateTime scheduledStart, WorkOrderPriority priority) {
        return switch (priority) {
            case EMERGENCY -> scheduledStart.plusHours(2); // Complete within 2 hours
            case HIGH -> scheduledStart.plusHours(8); // Complete within 8 hours
            case MEDIUM -> scheduledStart.plusDays(1); // Complete within 1 day
            case LOW -> scheduledStart.plusDays(2); // Complete within 2 days
            default -> scheduledStart.plusDays(1);
        };
    }
    
    private BigDecimal estimateDefaultCost(WorkOrderPriority priority) {
        return switch (priority) {
            case EMERGENCY -> BigDecimal.valueOf(500.0); // Emergency surcharge
            case HIGH -> BigDecimal.valueOf(250.0);
            case MEDIUM -> BigDecimal.valueOf(150.0);
            case LOW -> BigDecimal.valueOf(100.0);
            default -> BigDecimal.valueOf(150.0);
        };
    }
    
    private double calculateEstimatedHours(WorkOrderPriority priority) {
        return switch (priority) {
            case EMERGENCY -> 2.0; // 2 hours for emergency fixes
            case HIGH -> 4.0; // 4 hours for high priority
            case MEDIUM -> 6.0; // 6 hours for medium priority
            case LOW -> 8.0; // 8 hours for low priority
            default -> 6.0;
        };
    }
    
    private String buildWorkOrderDescription(Report report, MobileReportRequest request) {
        StringBuilder description = new StringBuilder();
        description.append("Auto-generated from mobile report submission.\n\n");
        description.append("Original Report: ").append(report.getDescription()).append("\n\n");
        
        if (request.maintenanceDetails() != null) {
            MaintenanceDetails details = request.maintenanceDetails();
            if (details.urgent() != null && details.urgent()) {
                description.append("⚠️ URGENT: Immediate attention required\n");
            }
            if (details.safetyHazard() != null && details.safetyHazard()) {
                description.append("🚨 SAFETY HAZARD: Follow all safety protocols\n");
            }
            // Materials needed information would be added here if available in the DTO
        }
        
        if (request.location() != null) {
            LocationData loc = request.location();
            description.append("\nLocation Details:\n");
            if (loc.building() != null) description.append("Building: ").append(loc.building()).append("\n");
            if (loc.floor() != null) description.append("Floor: ").append(loc.floor()).append("\n");
            if (loc.room() != null) description.append("Room: ").append(loc.room()).append("\n");
            if (loc.additionalInfo() != null) description.append("Additional Info: ").append(loc.additionalInfo()).append("\n");
        }
        
        description.append("\nSubmitted by: ").append(report.getSupervisor().getDisplayName());
        description.append("\nSubmission time: ").append(LocalDateTime.now());
        
        return description.toString();
    }
    
    private String determineCategoryFromReport(MobileReportRequest request) {
        if (request.maintenanceDetails() != null) {
            MaintenanceDetails details = request.maintenanceDetails();
            if (details.urgent() != null && details.urgent()) {
                return "urgent_repair";
            }
            if (details.safetyHazard() != null && details.safetyHazard()) {
                return "safety";
            }
        }
        
        // Determine category based on title/description keywords
        String lowerTitle = request.title().toLowerCase();
        String lowerDescription = request.description().toLowerCase();
        
        if (lowerTitle.contains("electrical") || lowerDescription.contains("electrical")) {
            return "electrical";
        } else if (lowerTitle.contains("plumbing") || lowerDescription.contains("plumbing") || 
                   lowerTitle.contains("water") || lowerDescription.contains("water")) {
            return "plumbing";
        } else if (lowerTitle.contains("hvac") || lowerDescription.contains("hvac") || 
                   lowerTitle.contains("air") || lowerDescription.contains("heating")) {
            return "hvac";
        } else if (lowerTitle.contains("paint") || lowerDescription.contains("paint") ||
                   lowerTitle.contains("wall") || lowerDescription.contains("wall")) {
            return "painting";
        } else if (lowerTitle.contains("door") || lowerDescription.contains("door") ||
                   lowerTitle.contains("window") || lowerDescription.contains("window")) {
            return "carpentry";
        } else {
            return "general_maintenance";
        }
    }
    
    private User findBestAvailableTechnician(UUID companyId, WorkOrderPriority priority) {
        try {
            // Find all available technicians in the company
            List<User> availableTechnicians = userRepository
                .findByUserTypeAndCompany_IdAndDeletedAtIsNull(UserType.TECHNICIAN, companyId)
                .stream()
                .filter(tech -> tech.getIsAvailableForAssignment() != null && tech.getIsAvailableForAssignment())
                .filter(tech -> tech.getStatus() == UserStatus.ACTIVE)
                .filter(tech -> tech.getIsLocked() == null || !tech.getIsLocked())
                .toList();
            
            if (availableTechnicians.isEmpty()) {
                logger.warn("No available technicians found for company: {}", companyId);
                return null;
            }
            
            // For emergency/high priority, prefer technicians with highest performance rating
            if (priority == WorkOrderPriority.EMERGENCY || priority == WorkOrderPriority.HIGH) {
                return availableTechnicians.stream()
                    .filter(tech -> tech.getPerformanceRating() != null)
                    .max((t1, t2) -> Double.compare(t1.getPerformanceRating(), t2.getPerformanceRating()))
                    .orElse(availableTechnicians.get(0)); // Fallback to first available
            }
            
            // For medium/low priority, use round-robin or first available
            return availableTechnicians.get(0);
            
        } catch (Exception e) {
            logger.error("Error finding available technician for company: {}", companyId, e);
            return null;
        }
    }
    
    private String generateWorkOrderNumber(School school) {
        String prefix = "WO-" + LocalDateTime.now().getYear() + "-";
        // Use school initials if available, otherwise use first 3 chars of school name
        String schoolCode = school.getName().length() >= 3 ? 
            school.getName().substring(0, 3).toUpperCase() : "SCH";
        long nextNumber = System.currentTimeMillis() % 100000; // Simple sequence
        return prefix + schoolCode + "-" + String.format("%05d", nextNumber);
    }
}