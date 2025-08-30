package com.cafm.cafmbackend.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.cafm.cafmbackend.domain.services.MobileSyncService;
import com.cafm.cafmbackend.domain.services.MobileDashboardService;
import com.cafm.cafmbackend.domain.services.MobileReportService;
import com.cafm.cafmbackend.domain.services.SupervisorLocationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

/**
 * Comprehensive API endpoint tests for CAFM Backend.
 * Tests all available REST endpoints without requiring database connectivity.
 */
@WebMvcTest(controllers = {HealthController.class, MobileSupervisorController.class})
@DisplayName("CAFM Backend API Endpoint Tests")
public class ApiEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MobileSyncService mobileSyncService;

    @MockBean
    private MobileDashboardService mobileDashboardService;

    @MockBean
    private MobileReportService mobileReportService;

    @MockBean
    private SupervisorLocationService locationService;

    // ========================================
    // HEALTH CONTROLLER TESTS
    // ========================================

    @Test
    @DisplayName("GET /api/v1/health - should return health status")
    public void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("CAFM Backend is running"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /api/v1/health/status - should return status message")
    public void testHealthStatusEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health/status"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Application is running successfully!"));
    }

    // ========================================
    // MOBILE SUPERVISOR CONTROLLER TESTS
    // ========================================

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("GET /api/v1/mobile/supervisor/dashboard - should return dashboard data")
    public void testGetMobileDashboard() throws Exception {
        // Mock service response
        Map<String, Object> mockDashboard = Map.of(
            "pending_reports", 5,
            "in_progress_reports", 3,
            "completed_today", 2,
            "schools_assigned", 4,
            "last_sync", LocalDateTime.now().toString()
        );
        
        when(mobileDashboardService.getSupervisorDashboard(anyString(), anyBoolean()))
            .thenReturn(mockDashboard);

        mockMvc.perform(get("/api/v1/mobile/supervisor/dashboard")
                .param("detailed", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.pending_reports").value(5))
                .andExpect(jsonPath("$.in_progress_reports").value(3))
                .andExpect(jsonPath("$.completed_today").value(2))
                .andExpect(jsonPath("$.schools_assigned").value(4));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("GET /api/v1/mobile/supervisor/reports - should return mobile reports")
    public void testGetMobileReports() throws Exception {
        // Mock service response with empty list for now since DTOs might need creation
        when(mobileReportService.getMobileReports(anyString(), anyString(), anyString(), 
                any(UUID.class), anyBoolean(), anyInt()))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/mobile/supervisor/reports")
                .param("status", "PENDING")
                .param("priority", "HIGH")
                .param("myReports", "true")
                .param("limit", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("GET /api/v1/mobile/supervisor/config - should return mobile config")
    public void testGetMobileConfig() throws Exception {
        // Mock service response
        Map<String, Object> mockConfig = Map.of(
            "sync_interval_minutes", 15,
            "max_photo_size_mb", 10,
            "offline_mode_enabled", true,
            "features", Map.of(
                "location_tracking", true,
                "photo_upload", true,
                "offline_reports", true
            )
        );
        
        when(mobileSyncService.getMobileConfig(anyString())).thenReturn(mockConfig);

        mockMvc.perform(get("/api/v1/mobile/supervisor/config"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sync_interval_minutes").value(15))
                .andExpect(jsonPath("$.max_photo_size_mb").value(10))
                .andExpect(jsonPath("$.offline_mode_enabled").value(true))
                .andExpect(jsonPath("$.features.location_tracking").value(true));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("GET /api/v1/mobile/supervisor/sync/status - should return sync status")
    public void testGetSyncStatus() throws Exception {
        // Mock service response
        Map<String, Object> mockStatus = Map.of(
            "last_sync_time", LocalDateTime.now().toString(),
            "sync_status", "SUCCESS",
            "pending_uploads", 0,
            "pending_downloads", 2,
            "conflicts", 0
        );
        
        when(mobileSyncService.getSyncStatus(anyString())).thenReturn(mockStatus);

        mockMvc.perform(get("/api/v1/mobile/supervisor/sync/status"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sync_status").value("SUCCESS"))
                .andExpect(jsonPath("$.pending_uploads").value(0))
                .andExpect(jsonPath("$.pending_downloads").value(2))
                .andExpect(jsonPath("$.conflicts").value(0));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("POST /api/v1/mobile/supervisor/location - should update location")
    public void testUpdateLocation() throws Exception {
        // Mock location update request
        var locationRequest = Map.of(
            "latitude", 40.7128,
            "longitude", -74.0060,
            "accuracy", 5.0,
            "currentLocationId", UUID.randomUUID().toString(),
            "timestamp", LocalDateTime.now().toString()
        );
        
        // Mock service response
        Map<String, Object> mockResponse = Map.of(
            "status", "UPDATED",
            "message", "Location updated successfully",
            "timestamp", LocalDateTime.now().toString()
        );
        
        when(locationService.updateSupervisorLocation(anyString(), any()))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/mobile/supervisor/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(locationRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UPDATED"))
                .andExpect(jsonPath("$.message").value("Location updated successfully"));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("POST /api/v1/mobile/supervisor/device/register - should register device")
    public void testRegisterDevice() throws Exception {
        // Mock device registration request
        var deviceRequest = Map.of(
            "deviceId", "device-123-abc",
            "platform", "Android",
            "appVersion", "1.0.0",
            "fcmToken", "fcm-token-xyz",
            "deviceInfo", Map.of(
                "model", "Pixel 6",
                "os_version", "13.0",
                "app_build", "100"
            )
        );
        
        // Mock service response
        Map<String, Object> mockResponse = Map.of(
            "registration_id", UUID.randomUUID().toString(),
            "device_id", "device-123-abc",
            "status", "REGISTERED",
            "registered_at", LocalDateTime.now().toString()
        );
        
        when(mobileSyncService.registerDevice(anyString(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/mobile/supervisor/device/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deviceRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.device_id").value("device-123-abc"))
                .andExpect(jsonPath("$.status").value("REGISTERED"));
    }

    // ========================================
    // SECURITY TESTS
    // ========================================

    @Test
    @DisplayName("Mobile supervisor endpoints should require authentication")
    public void testMobileEndpointsRequireAuth() throws Exception {
        // Test that protected endpoints return 401/403 without authentication
        mockMvc.perform(get("/api/v1/mobile/supervisor/dashboard"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/mobile/supervisor/reports"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/mobile/supervisor/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Mobile supervisor endpoints should require SUPERVISOR role")
    public void testMobileEndpointsRequireSupervisorRole() throws Exception {
        // Test that endpoints require SUPERVISOR role
        mockMvc.perform(get("/api/v1/mobile/supervisor/dashboard"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    // ========================================
    // ERROR HANDLING TESTS  
    // ========================================

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("Should handle service exceptions gracefully")
    public void testServiceExceptionHandling() throws Exception {
        // Mock service to throw exception
        when(mobileDashboardService.getSupervisorDashboard(anyString(), anyBoolean()))
            .thenThrow(new RuntimeException("Database connection error"));

        mockMvc.perform(get("/api/v1/mobile/supervisor/dashboard"))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    // ========================================
    // CONTENT TYPE TESTS
    // ========================================

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("Should accept and return JSON content")
    public void testJsonContentTypes() throws Exception {
        when(mobileSyncService.getMobileConfig(anyString()))
            .thenReturn(Map.of("test", "value"));

        mockMvc.perform(get("/api/v1/mobile/supervisor/config")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}