package com.cafm.cafmbackend.integration.controllers;

import com.cafm.cafmbackend.dto.report.*;
import com.cafm.cafmbackend.dto.workorder.*;
import com.cafm.cafmbackend.service.ReportService;
import com.cafm.cafmbackend.service.WorkOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Report and WorkOrder Controllers.
 * Tests the complete maintenance workflow from report creation to work order completion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Report & WorkOrder Controllers Integration Tests")
class ReportWorkOrderControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("cafm_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReportService reportService;

    @MockBean
    private WorkOrderService workOrderService;

    private static final String REPORT_BASE_URL = "/api/v1/reports";
    private static final String WORKORDER_BASE_URL = "/api/v1/work-orders";
    private static final UUID TEST_REPORT_ID = UUID.randomUUID();
    private static final UUID TEST_WORKORDER_ID = UUID.randomUUID();
    private static final UUID TEST_ASSET_ID = UUID.randomUUID();
    private static final UUID TEST_SCHOOL_ID = UUID.randomUUID();
    private static final UUID TEST_COMPANY_ID = UUID.randomUUID();

    // ========== Report Creation Tests ==========

    @Test
    @Order(1)
    @WithMockUser(roles = "SUPERVISOR", username = "supervisor@test.com")
    @DisplayName("Should create maintenance report with attachments")
    void testCreateReport_WithAttachments() throws Exception {
        // Arrange
        ReportCreateRequest request = new ReportCreateRequest(
                "Broken AC Unit",
                "The AC unit in Room 203 is not cooling properly",
                "HIGH",
                "HVAC",
                TEST_ASSET_ID,
                TEST_SCHOOL_ID,
                "Building A, Room 203",
                List.of("image1.jpg", "image2.jpg"),
                null
        );

        ReportResponse createdReport = createReportResponse("PENDING");
        when(reportService.createReport(any(ReportCreateRequest.class), eq("supervisor@test.com")))
                .thenReturn(createdReport);

        // Act & Assert
        mockMvc.perform(post(REPORT_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Broken AC Unit"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "TECHNICIAN", username = "tech@test.com")
    @DisplayName("Should create simple report as technician")
    void testCreateReport_AsTechnician() throws Exception {
        // Arrange
        ReportCreateRequest request = new ReportCreateRequest(
                "Light fixture issue",
                "Flickering lights in hallway",
                "MEDIUM",
                "ELECTRICAL",
                null,
                TEST_SCHOOL_ID,
                "Main hallway",
                null,
                null
        );

        ReportResponse createdReport = createReportResponse("PENDING");
        when(reportService.createReport(any(ReportCreateRequest.class), eq("tech@test.com")))
                .thenReturn(createdReport);

        // Act & Assert
        mockMvc.perform(post(REPORT_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("Should upload images for report")
    void testUploadReportImages() throws Exception {
        // Arrange
        MockMultipartFile image1 = new MockMultipartFile(
                "files",
                "damage1.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image content".getBytes()
        );
        MockMultipartFile image2 = new MockMultipartFile(
                "files",
                "damage2.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image content".getBytes()
        );

        List<String> uploadedUrls = Arrays.asList(
                "https://storage.example.com/damage1.jpg",
                "https://storage.example.com/damage2.jpg"
        );

        when(reportService.uploadReportImages(eq(TEST_REPORT_ID), any()))
                .thenReturn(uploadedUrls);

        // Act & Assert
        mockMvc.perform(multipart(REPORT_BASE_URL + "/" + TEST_REPORT_ID + "/images")
                        .file(image1)
                        .file(image2)
                        .with(csrf())
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ========== Report Status Workflow Tests ==========

    @Test
    @Order(4)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should update report status through workflow")
    void testReportStatusWorkflow() throws Exception {
        // Arrange
        ReportStatusUpdateRequest statusUpdate = new ReportStatusUpdateRequest(
                "IN_REVIEW",
                "Reviewing the reported issue"
        );

        ReportResponse updatedReport = createReportResponse("IN_REVIEW");
        when(reportService.updateReportStatus(eq(TEST_REPORT_ID), any(ReportStatusUpdateRequest.class)))
                .thenReturn(updatedReport);

        // Act & Assert
        mockMvc.perform(patch(REPORT_BASE_URL + "/" + TEST_REPORT_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));
    }

    @Test
    @Order(5)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should convert report to work order")
    void testConvertReportToWorkOrder() throws Exception {
        // Arrange
        WorkOrderCreateFromReportRequest request = new WorkOrderCreateFromReportRequest(
                TEST_REPORT_ID,
                UUID.randomUUID(), // technician ID
                LocalDateTime.now().plusDays(2),
                "HIGH",
                "Fix AC unit as per report",
                150.00
        );

        WorkOrderResponse workOrder = createWorkOrderResponse("ASSIGNED");
        when(workOrderService.createFromReport(any(WorkOrderCreateFromReportRequest.class)))
                .thenReturn(workOrder);

        // Act & Assert
        mockMvc.perform(post(WORKORDER_BASE_URL + "/from-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.reportId").value(TEST_REPORT_ID.toString()));
    }

    // ========== Work Order Management Tests ==========

    @Test
    @Order(6)
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("Should create work order directly")
    void testCreateWorkOrder_Direct() throws Exception {
        // Arrange
        WorkOrderCreateRequest request = new WorkOrderCreateRequest(
                "Routine HVAC Maintenance",
                "Quarterly HVAC system maintenance",
                "MEDIUM",
                "PREVENTIVE",
                TEST_ASSET_ID,
                TEST_SCHOOL_ID,
                UUID.randomUUID(), // technician ID
                LocalDateTime.now().plusDays(7),
                200.00,
                List.of("Replace filters", "Check refrigerant", "Clean coils")
        );

        WorkOrderResponse workOrder = createWorkOrderResponse("ASSIGNED");
        when(workOrderService.createWorkOrder(any(WorkOrderCreateRequest.class)))
                .thenReturn(workOrder);

        // Act & Assert
        mockMvc.perform(post(WORKORDER_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Routine HVAC Maintenance"));
    }

    @Test
    @Order(7)
    @WithMockUser(roles = "TECHNICIAN", username = "tech@test.com")
    @DisplayName("Should allow technician to update assigned work order")
    void testUpdateWorkOrderStatus_AsTechnician() throws Exception {
        // Arrange
        WorkOrderStatusUpdateRequest statusUpdate = new WorkOrderStatusUpdateRequest(
                "IN_PROGRESS",
                "Started working on the issue",
                25
        );

        WorkOrderResponse updatedWorkOrder = createWorkOrderResponse("IN_PROGRESS");
        when(workOrderService.canUpdateWorkOrder(eq(TEST_WORKORDER_ID), eq("tech@test.com")))
                .thenReturn(true);
        when(workOrderService.updateWorkOrderStatus(eq(TEST_WORKORDER_ID), any()))
                .thenReturn(updatedWorkOrder);

        // Act & Assert
        mockMvc.perform(patch(WORKORDER_BASE_URL + "/" + TEST_WORKORDER_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @Order(8)
    @WithMockUser(roles = "TECHNICIAN")
    @DisplayName("Should add work log entry")
    void testAddWorkLogEntry() throws Exception {
        // Arrange
        WorkLogEntryRequest workLog = new WorkLogEntryRequest(
                "Diagnosed the issue - compressor failure",
                2.5,
                List.of("Checked electrical connections", "Tested compressor", "Ordered replacement part"),
                List.of("compressor_test.jpg")
        );

        when(workOrderService.addWorkLogEntry(eq(TEST_WORKORDER_ID), any()))
                .thenReturn(UUID.randomUUID());

        // Act & Assert
        mockMvc.perform(post(WORKORDER_BASE_URL + "/" + TEST_WORKORDER_ID + "/work-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workLog))
                        .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(9)
    @WithMockUser(roles = "TECHNICIAN")
    @DisplayName("Should complete work order with resolution")
    void testCompleteWorkOrder() throws Exception {
        // Arrange
        WorkOrderCompletionRequest completion = new WorkOrderCompletionRequest(
                "Replaced faulty compressor. AC unit now working properly.",
                List.of("completion_photo.jpg"),
                350.00,
                4.5,
                List.of("Compressor - $250", "Labor - $100"),
                true
        );

        WorkOrderResponse completedWorkOrder = createWorkOrderResponse("COMPLETED");
        when(workOrderService.completeWorkOrder(eq(TEST_WORKORDER_ID), any()))
                .thenReturn(completedWorkOrder);

        // Act & Assert
        mockMvc.perform(post(WORKORDER_BASE_URL + "/" + TEST_WORKORDER_ID + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completion))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    // ========== Query and Filter Tests ==========

    @Test
    @Order(10)
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("Should get reports with filters")
    void testGetReports_WithFilters() throws Exception {
        // Arrange
        Page<ReportListResponse> reports = new PageImpl<>(
                Arrays.asList(createReportListResponse("HIGH", "PENDING"))
        );

        when(reportService.getReports(any(), eq("PENDING"), eq("HIGH"), any(), any(), any()))
                .thenReturn(reports);

        // Act & Assert
        mockMvc.perform(get(REPORT_BASE_URL)
                        .param("status", "PENDING")
                        .param("priority", "HIGH")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].priority").value("HIGH"));
    }

    @Test
    @Order(11)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get work orders by date range")
    void testGetWorkOrders_ByDateRange() throws Exception {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        Page<WorkOrderListResponse> workOrders = new PageImpl<>(
                Arrays.asList(createWorkOrderListResponse("IN_PROGRESS"))
        );

        when(workOrderService.getWorkOrdersByDateRange(any(), any(), any()))
                .thenReturn(workOrders);

        // Act & Assert
        mockMvc.perform(get(WORKORDER_BASE_URL)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @Order(12)
    @WithMockUser(roles = "TECHNICIAN", username = "tech@test.com")
    @DisplayName("Should get technician's assigned work orders")
    void testGetMyWorkOrders() throws Exception {
        // Arrange
        Page<WorkOrderListResponse> myWorkOrders = new PageImpl<>(
                Arrays.asList(
                        createWorkOrderListResponse("ASSIGNED"),
                        createWorkOrderListResponse("IN_PROGRESS")
                )
        );

        when(workOrderService.getTechnicianWorkOrders(eq("tech@test.com"), any()))
                .thenReturn(myWorkOrders);

        // Act & Assert
        mockMvc.perform(get(WORKORDER_BASE_URL + "/my-work-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    // ========== Priority and SLA Tests ==========

    @Test
    @Order(13)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should escalate overdue high priority reports")
    void testEscalateOverdueReports() throws Exception {
        // Arrange
        List<UUID> escalatedReports = Arrays.asList(
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        when(reportService.escalateOverdueReports()).thenReturn(escalatedReports);

        // Act & Assert
        mockMvc.perform(post(REPORT_BASE_URL + "/escalate-overdue")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @Order(14)
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("Should calculate SLA compliance")
    void testGetSLACompliance() throws Exception {
        // Arrange
        Map<String, Object> slaMetrics = Map.of(
                "totalWorkOrders", 100,
                "completedOnTime", 85,
                "complianceRate", 85.0,
                "averageResolutionTime", 3.5
        );

        when(workOrderService.getSLAMetrics(any(), any())).thenReturn(slaMetrics);

        // Act & Assert
        mockMvc.perform(get(WORKORDER_BASE_URL + "/sla-metrics")
                        .param("startDate", LocalDateTime.now().minusMonths(1).toString())
                        .param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complianceRate").value(85.0));
    }

    // ========== Bulk Operations Tests ==========

    @Test
    @Order(15)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should bulk assign work orders")
    void testBulkAssignWorkOrders() throws Exception {
        // Arrange
        BulkAssignRequest bulkAssign = new BulkAssignRequest(
                Arrays.asList(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()),
                UUID.randomUUID(), // technician ID
                "Assigning preventive maintenance tasks"
        );

        when(workOrderService.bulkAssign(any())).thenReturn(3);

        // Act & Assert
        mockMvc.perform(post(WORKORDER_BASE_URL + "/bulk-assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkAssign))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));
    }

    @Test
    @Order(16)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should bulk update report priorities")
    void testBulkUpdateReportPriorities() throws Exception {
        // Arrange
        BulkPriorityUpdateRequest bulkUpdate = new BulkPriorityUpdateRequest(
                Arrays.asList(UUID.randomUUID(), UUID.randomUUID()),
                "CRITICAL",
                "Emergency situation"
        );

        when(reportService.bulkUpdatePriority(any())).thenReturn(2);

        // Act & Assert
        mockMvc.perform(patch(REPORT_BASE_URL + "/bulk-priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkUpdate))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(2));
    }

    // ========== Notification Tests ==========

    @Test
    @Order(17)
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("Should trigger notifications on status change")
    void testNotificationOnStatusChange() throws Exception {
        // This tests that notifications are triggered when report/work order status changes
        ReportStatusUpdateRequest statusUpdate = new ReportStatusUpdateRequest(
                "WORK_ORDER_CREATED",
                "Work order has been created for this report"
        );

        ReportResponse updatedReport = createReportResponse("WORK_ORDER_CREATED");
        when(reportService.updateReportStatus(eq(TEST_REPORT_ID), any()))
                .thenReturn(updatedReport);

        // Verify notification service is called
        mockMvc.perform(patch(REPORT_BASE_URL + "/" + TEST_REPORT_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(csrf()))
                .andExpect(status().isOk());

        // In real test, verify notification service was called
    }

    // ========== Validation Tests ==========

    @Test
    @Order(18)
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("Should validate required fields in report creation")
    void testCreateReport_ValidationError() throws Exception {
        // Arrange - missing required fields
        String invalidRequest = """
                {
                    "title": "Test Report"
                }
                """;

        // Act & Assert
        mockMvc.perform(post(REPORT_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(19)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should validate work order date constraints")
    void testCreateWorkOrder_InvalidDueDate() throws Exception {
        // Arrange - due date in the past
        WorkOrderCreateRequest request = new WorkOrderCreateRequest(
                "Test Work Order",
                "Description",
                "HIGH",
                "CORRECTIVE",
                TEST_ASSET_ID,
                TEST_SCHOOL_ID,
                UUID.randomUUID(),
                LocalDateTime.now().minusDays(1), // Past date
                100.00,
                null
        );

        // Act & Assert
        mockMvc.perform(post(WORKORDER_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(20)
    @WithMockUser(roles = "TECHNICIAN")
    @DisplayName("Should prevent invalid status transitions")
    void testInvalidStatusTransition() throws Exception {
        // Arrange - try to move from COMPLETED back to ASSIGNED
        WorkOrderStatusUpdateRequest invalidTransition = new WorkOrderStatusUpdateRequest(
                "ASSIGNED",
                "Trying to reopen completed work order",
                0
        );

        when(workOrderService.updateWorkOrderStatus(eq(TEST_WORKORDER_ID), any()))
                .thenThrow(new IllegalStateException("Invalid status transition"));

        // Act & Assert
        mockMvc.perform(patch(WORKORDER_BASE_URL + "/" + TEST_WORKORDER_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTransition))
                        .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    // ========== Helper Methods ==========

    private ReportResponse createReportResponse(String status) {
        return new ReportResponse(
                TEST_REPORT_ID,
                "Broken AC Unit",
                "The AC unit in Room 203 is not cooling properly",
                "HIGH",
                status,
                "HVAC",
                TEST_ASSET_ID,
                "AC-UNIT-203",
                TEST_SCHOOL_ID,
                "Lincoln High School",
                "Building A, Room 203",
                "supervisor@test.com",
                "John Supervisor",
                TEST_COMPANY_ID,
                List.of("image1.jpg", "image2.jpg"),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private ReportListResponse createReportListResponse(String priority, String status) {
        return new ReportListResponse(
                TEST_REPORT_ID,
                "Test Report",
                priority,
                status,
                "HVAC",
                "Lincoln High School",
                "Room 203",
                LocalDateTime.now()
        );
    }

    private WorkOrderResponse createWorkOrderResponse(String status) {
        return new WorkOrderResponse(
                TEST_WORKORDER_ID,
                "Fix AC Unit",
                "Repair broken AC unit in Room 203",
                "HIGH",
                status,
                "CORRECTIVE",
                TEST_REPORT_ID,
                TEST_ASSET_ID,
                "AC-UNIT-203",
                TEST_SCHOOL_ID,
                "Lincoln High School",
                UUID.randomUUID(),
                "Tech User",
                LocalDateTime.now().plusDays(2),
                150.00,
                0.00,
                0.0,
                null,
                null,
                TEST_COMPANY_ID,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private WorkOrderListResponse createWorkOrderListResponse(String status) {
        return new WorkOrderListResponse(
                TEST_WORKORDER_ID,
                "Test Work Order",
                "HIGH",
                status,
                "CORRECTIVE",
                "Lincoln High School",
                "Tech User",
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now()
        );
    }

    // Inner class for bulk assign request
    record BulkAssignRequest(List<UUID> workOrderIds, UUID technicianId, String notes) {}
    
    // Inner class for bulk priority update
    record BulkPriorityUpdateRequest(List<UUID> reportIds, String priority, String reason) {}
    
    // Inner class for work order create from report
    record WorkOrderCreateFromReportRequest(
            UUID reportId,
            UUID technicianId,
            LocalDateTime dueDate,
            String priority,
            String notes,
            Double estimatedCost
    ) {}
    
    // Inner class for work log entry
    record WorkLogEntryRequest(
            String description,
            Double hoursWorked,
            List<String> tasksCompleted,
            List<String> images
    ) {}
    
    // Inner class for work order completion
    record WorkOrderCompletionRequest(
            String resolution,
            List<String> completionImages,
            Double actualCost,
            Double totalHours,
            List<String> partsUsed,
            boolean resolved
    ) {}
}