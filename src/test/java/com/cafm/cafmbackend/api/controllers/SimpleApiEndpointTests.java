package com.cafm.cafmbackend.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.cafm.cafmbackend.domain.services.MobileSyncService;
import com.cafm.cafmbackend.domain.services.MobileDashboardService;
import com.cafm.cafmbackend.domain.services.MobileReportService;
import com.cafm.cafmbackend.domain.services.SupervisorLocationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CAFM Backend API Controllers.
 * Tests controller logic without full Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CAFM Backend API Controller Unit Tests")
public class SimpleApiEndpointTests {

    @Mock private MobileSyncService mobileSyncService;
    @Mock private MobileDashboardService mobileDashboardService;
    @Mock private MobileReportService mobileReportService;
    @Mock private SupervisorLocationService locationService;

    private HealthController healthController;
    private MobileSupervisorController mobileController;
    private Authentication mockAuth;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        healthController = new HealthController();
        mobileController = new MobileSupervisorController(
                mobileSyncService, mobileDashboardService, mobileReportService, locationService);
        
        // Create mock authentication with SUPERVISOR role
        mockAuth = new UsernamePasswordAuthenticationToken(
                "supervisor@test.com", 
                null, 
                List.of(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))
        );
        
        objectMapper = new ObjectMapper();
    }

    // ========================================
    // HEALTH CONTROLLER TESTS
    // ========================================

    @Test
    @DisplayName("Health endpoint should return UP status with timestamp and message")
    void testHealthEndpoint() {
        // When
        ResponseEntity<Map<String, Object>> response = healthController.health();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> body = response.getBody();
        assertEquals("UP", body.get("status"));
        assertEquals("CAFM Backend is running", body.get("message"));
        assertNotNull(body.get("timestamp"));
        
        // Verify timestamp is a valid string
        assertTrue(body.get("timestamp") instanceof String);
    }

    @Test
    @DisplayName("Health status endpoint should return success message")
    void testHealthStatusEndpoint() {
        // When
        ResponseEntity<String> response = healthController.status();
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Application is running successfully!", response.getBody());
    }

    // ========================================
    // MOBILE SUPERVISOR CONTROLLER TESTS
    // ========================================

    @Test
    @DisplayName("Get mobile dashboard should return dashboard data successfully")
    void testGetMobileDashboard() {
        // Given
        Map<String, Object> mockDashboard = Map.of(
                "pending_reports", 5,
                "in_progress_reports", 3,
                "completed_today", 2,
                "schools_assigned", 4,
                "last_sync", LocalDateTime.now().toString()
        );
        when(mobileDashboardService.getSupervisorDashboard(anyString(), anyBoolean()))
                .thenReturn(mockDashboard);

        // When
        ResponseEntity<Map<String, Object>> response = mobileController.getMobileDashboard(true, mockAuth);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().get("pending_reports"));
        assertEquals(3, response.getBody().get("in_progress_reports"));
        assertEquals(2, response.getBody().get("completed_today"));
        assertEquals(4, response.getBody().get("schools_assigned"));

        verify(mobileDashboardService).getSupervisorDashboard("supervisor@test.com", true);
    }

    @Test
    @DisplayName("Get mobile dashboard should handle service exceptions")
    void testGetMobileDashboardWithException() {
        // Given
        when(mobileDashboardService.getSupervisorDashboard(anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<Map<String, Object>> response = mobileController.getMobileDashboard(false, mockAuth);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mobileDashboardService).getSupervisorDashboard("supervisor@test.com", false);
    }

    @Test
    @DisplayName("Get mobile reports should return report list")
    void testGetMobileReports() {
        // Given - using empty list since actual DTO creation might require more setup
        when(mobileReportService.getMobileReports(anyString(), anyString(), anyString(), 
                any(), anyBoolean(), anyInt()))
                .thenReturn(List.of());

        // When
        ResponseEntity<?> response = mobileController.getMobileReports(
                "PENDING", "HIGH", UUID.randomUUID(), true, 10, mockAuth);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);

        verify(mobileReportService).getMobileReports(
                eq("supervisor@test.com"), eq("PENDING"), eq("HIGH"), 
                any(UUID.class), eq(true), eq(10));
    }

    @Test
    @DisplayName("Get mobile config should return configuration")
    void testGetMobileConfig() {
        // Given
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

        // When
        ResponseEntity<Map<String, Object>> response = mobileController.getMobileConfig(mockAuth);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(15, response.getBody().get("sync_interval_minutes"));
        assertEquals(10, response.getBody().get("max_photo_size_mb"));
        assertEquals(true, response.getBody().get("offline_mode_enabled"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> features = (Map<String, Object>) response.getBody().get("features");
        assertEquals(true, features.get("location_tracking"));

        verify(mobileSyncService).getMobileConfig("supervisor@test.com");
    }

    @Test
    @DisplayName("Get sync status should return synchronization status")
    void testGetSyncStatus() {
        // Given
        Map<String, Object> mockStatus = Map.of(
                "last_sync_time", LocalDateTime.now().toString(),
                "sync_status", "SUCCESS",
                "pending_uploads", 0,
                "pending_downloads", 2,
                "conflicts", 0
        );
        when(mobileSyncService.getSyncStatus(anyString())).thenReturn(mockStatus);

        // When
        ResponseEntity<Map<String, Object>> response = mobileController.getSyncStatus(mockAuth);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().get("sync_status"));
        assertEquals(0, response.getBody().get("pending_uploads"));
        assertEquals(2, response.getBody().get("pending_downloads"));
        assertEquals(0, response.getBody().get("conflicts"));

        verify(mobileSyncService).getSyncStatus("supervisor@test.com");
    }

    @Test
    @DisplayName("Update location should process location update")
    void testUpdateLocation() {
        // Given
        var locationRequest = new MobileSupervisorController.SupervisorLocationUpdateRequest(
                40.7128, -74.0060, 5.0, UUID.randomUUID(), LocalDateTime.now()
        );
        
        Map<String, Object> mockResponse = Map.of(
                "status", "UPDATED",
                "message", "Location updated successfully",
                "timestamp", LocalDateTime.now().toString()
        );
        when(locationService.updateSupervisorLocation(anyString(), any()))
                .thenReturn(mockResponse);

        // When
        ResponseEntity<Map<String, Object>> response = mobileController.updateLocation(locationRequest, mockAuth);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UPDATED", response.getBody().get("status"));
        assertEquals("Location updated successfully", response.getBody().get("message"));

        verify(locationService).updateSupervisorLocation(eq("supervisor@test.com"), eq(locationRequest));
    }

    @Test
    @DisplayName("Register device should process device registration")
    void testRegisterDevice() {
        // Given
        var deviceRequest = new MobileSupervisorController.MobileDeviceRegistrationRequest(
                "device-123-abc",
                "Android",
                "1.0.0",
                "fcm-token-xyz",
                Map.of("model", "Pixel 6", "os_version", "13.0")
        );
        
        Map<String, Object> mockResponse = Map.of(
                "registration_id", UUID.randomUUID().toString(),
                "device_id", "device-123-abc",
                "status", "REGISTERED",
                "registered_at", LocalDateTime.now().toString()
        );
        when(mobileSyncService.registerDevice(anyString(), any())).thenReturn(mockResponse);

        // When
        ResponseEntity<Map<String, Object>> response = mobileController.registerDevice(deviceRequest, mockAuth);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("device-123-abc", response.getBody().get("device_id"));
        assertEquals("REGISTERED", response.getBody().get("status"));

        verify(mobileSyncService).registerDevice(eq("supervisor@test.com"), any(Map.class));
    }

    // ========================================
    // ERROR HANDLING TESTS
    // ========================================

    @Test
    @DisplayName("Mobile config should handle service exceptions")
    void testMobileConfigWithException() {
        // Given
        when(mobileSyncService.getMobileConfig(anyString()))
                .thenThrow(new RuntimeException("Config service error"));

        // When
        ResponseEntity<Map<String, Object>> response = mobileController.getMobileConfig(mockAuth);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mobileSyncService).getMobileConfig("supervisor@test.com");
    }

    @Test
    @DisplayName("Sync status should handle service exceptions")
    void testSyncStatusWithException() {
        // Given
        when(mobileSyncService.getSyncStatus(anyString()))
                .thenThrow(new RuntimeException("Sync service error"));

        // When
        ResponseEntity<Map<String, Object>> response = mobileController.getSyncStatus(mockAuth);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mobileSyncService).getSyncStatus("supervisor@test.com");
    }

    @Test
    @DisplayName("Location update should handle service exceptions")
    void testLocationUpdateWithException() {
        // Given
        var locationRequest = new MobileSupervisorController.SupervisorLocationUpdateRequest(
                40.7128, -74.0060, 5.0, UUID.randomUUID(), LocalDateTime.now()
        );
        when(locationService.updateSupervisorLocation(anyString(), any()))
                .thenThrow(new RuntimeException("Location service error"));

        // When
        ResponseEntity<Map<String, Object>> response = mobileController.updateLocation(locationRequest, mockAuth);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(locationService).updateSupervisorLocation(eq("supervisor@test.com"), eq(locationRequest));
    }

    @Test
    @DisplayName("Device registration should handle service exceptions")
    void testDeviceRegistrationWithException() {
        // Given
        var deviceRequest = new MobileSupervisorController.MobileDeviceRegistrationRequest(
                "device-123-abc", "Android", "1.0.0", "fcm-token-xyz", Map.of()
        );
        when(mobileSyncService.registerDevice(anyString(), any()))
                .thenThrow(new RuntimeException("Device service error"));

        // When
        ResponseEntity<Map<String, Object>> response = mobileController.registerDevice(deviceRequest, mockAuth);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(mobileSyncService).registerDevice(eq("supervisor@test.com"), any(Map.class));
    }

    // ========================================
    // VALIDATION TESTS
    // ========================================

    @Test
    @DisplayName("Mobile dashboard should handle different detail levels")
    void testMobileDashboardDetailLevels() {
        // Given
        Map<String, Object> basicDashboard = Map.of("basic", "data");
        Map<String, Object> detailedDashboard = Map.of("detailed", "data", "extra", "info");
        
        when(mobileDashboardService.getSupervisorDashboard("supervisor@test.com", false))
                .thenReturn(basicDashboard);
        when(mobileDashboardService.getSupervisorDashboard("supervisor@test.com", true))
                .thenReturn(detailedDashboard);

        // When & Then - Basic dashboard
        ResponseEntity<Map<String, Object>> basicResponse = mobileController.getMobileDashboard(false, mockAuth);
        assertEquals(HttpStatus.OK, basicResponse.getStatusCode());
        assertEquals("data", basicResponse.getBody().get("basic"));

        // When & Then - Detailed dashboard  
        ResponseEntity<Map<String, Object>> detailedResponse = mobileController.getMobileDashboard(true, mockAuth);
        assertEquals(HttpStatus.OK, detailedResponse.getStatusCode());
        assertEquals("data", detailedResponse.getBody().get("detailed"));
        assertEquals("info", detailedResponse.getBody().get("extra"));
    }

    @Test
    @DisplayName("All service methods should be called with correct parameters")
    void testServiceMethodCalls() {
        // Given - Set up all mocks to return valid responses
        when(mobileDashboardService.getSupervisorDashboard(anyString(), anyBoolean()))
                .thenReturn(Map.of("test", "data"));
        when(mobileReportService.getMobileReports(anyString(), anyString(), anyString(), 
                any(), anyBoolean(), anyInt()))
                .thenReturn(List.of());
        when(mobileSyncService.getMobileConfig(anyString()))
                .thenReturn(Map.of("config", "value"));
        when(mobileSyncService.getSyncStatus(anyString()))
                .thenReturn(Map.of("status", "ok"));
        when(locationService.updateSupervisorLocation(anyString(), any()))
                .thenReturn(Map.of("result", "success"));
        when(mobileSyncService.registerDevice(anyString(), any()))
                .thenReturn(Map.of("device", "registered"));

        // When - Call all endpoints
        mobileController.getMobileDashboard(true, mockAuth);
        mobileController.getMobileReports("PENDING", "HIGH", null, false, 20, mockAuth);
        mobileController.getMobileConfig(mockAuth);
        mobileController.getSyncStatus(mockAuth);
        
        var locationReq = new MobileSupervisorController.SupervisorLocationUpdateRequest(
                1.0, 2.0, 3.0, null, LocalDateTime.now());
        mobileController.updateLocation(locationReq, mockAuth);
        
        var deviceReq = new MobileSupervisorController.MobileDeviceRegistrationRequest(
                "device", "platform", "version", "token", Map.of());
        mobileController.registerDevice(deviceReq, mockAuth);

        // Then - Verify all services were called with correct parameters
        verify(mobileDashboardService).getSupervisorDashboard("supervisor@test.com", true);
        verify(mobileReportService).getMobileReports(
                eq("supervisor@test.com"), eq("PENDING"), eq("HIGH"), 
                isNull(), eq(false), eq(20));
        verify(mobileSyncService).getMobileConfig("supervisor@test.com");
        verify(mobileSyncService).getSyncStatus("supervisor@test.com");
        verify(locationService).updateSupervisorLocation(eq("supervisor@test.com"), eq(locationReq));
        verify(mobileSyncService).registerDevice(eq("supervisor@test.com"), any(Map.class));
    }
}