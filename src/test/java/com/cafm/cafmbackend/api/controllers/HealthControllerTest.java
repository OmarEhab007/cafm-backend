package com.cafm.cafmbackend.api.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HealthController.
 * Tests the health check endpoints that are essential for API monitoring.
 */
@DisplayName("Health Controller API Tests")
public class HealthControllerTest {

    private HealthController healthController;

    @BeforeEach
    void setUp() {
        healthController = new HealthController();
    }

    @Test
    @DisplayName("GET /api/v1/health - Health endpoint returns correct status and format")
    void testHealthEndpoint() {
        // When
        ResponseEntity<Map<String, Object>> response = healthController.health();
        
        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return HTTP 200 OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        
        Map<String, Object> body = response.getBody();
        
        // Verify required fields are present
        assertTrue(body.containsKey("status"), "Response should contain 'status' field");
        assertTrue(body.containsKey("timestamp"), "Response should contain 'timestamp' field");
        assertTrue(body.containsKey("message"), "Response should contain 'message' field");
        
        // Verify field values
        assertEquals("UP", body.get("status"), "Status should be 'UP'");
        assertEquals("CAFM Backend is running", body.get("message"), "Message should indicate backend is running");
        
        // Verify timestamp is valid
        String timestamp = (String) body.get("timestamp");
        assertNotNull(timestamp, "Timestamp should not be null");
        assertDoesNotThrow(() -> LocalDateTime.parse(timestamp), "Timestamp should be valid ISO format");
        
        // Verify response structure matches expected API contract
        assertEquals(3, body.size(), "Response should contain exactly 3 fields");
    }

    @Test
    @DisplayName("GET /api/v1/health/status - Status endpoint returns success message")
    void testHealthStatusEndpoint() {
        // When
        ResponseEntity<String> response = healthController.status();
        
        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Should return HTTP 200 OK");
        assertEquals("Application is running successfully!", response.getBody(), 
                    "Should return success message");
    }

    @Test
    @DisplayName("Health endpoint timestamp should be current")
    void testHealthEndpointTimestampIsCurrent() {
        // Given
        LocalDateTime beforeCall = LocalDateTime.now().minusSeconds(1);
        
        // When
        ResponseEntity<Map<String, Object>> response = healthController.health();
        
        // Then
        LocalDateTime afterCall = LocalDateTime.now().plusSeconds(1);
        String timestampString = (String) response.getBody().get("timestamp");
        LocalDateTime timestamp = LocalDateTime.parse(timestampString);
        
        assertTrue(timestamp.isAfter(beforeCall), 
                  "Timestamp should be after call start time");
        assertTrue(timestamp.isBefore(afterCall), 
                  "Timestamp should be before call end time");
    }

    @Test
    @DisplayName("Health endpoint should be consistent across multiple calls")
    void testHealthEndpointConsistency() {
        // When - Call endpoint multiple times
        ResponseEntity<Map<String, Object>> response1 = healthController.health();
        ResponseEntity<Map<String, Object>> response2 = healthController.health();
        ResponseEntity<Map<String, Object>> response3 = healthController.health();
        
        // Then - All responses should have same structure and non-timestamp fields
        assertEquals(response1.getStatusCode(), response2.getStatusCode(), 
                    "All responses should have same status code");
        assertEquals(response1.getStatusCode(), response3.getStatusCode(), 
                    "All responses should have same status code");
        
        assertEquals(response1.getBody().get("status"), response2.getBody().get("status"),
                    "Status field should be consistent");
        assertEquals(response1.getBody().get("message"), response2.getBody().get("message"),
                    "Message field should be consistent");
        
        // Timestamps should be different (or very close)
        assertNotNull(response1.getBody().get("timestamp"));
        assertNotNull(response2.getBody().get("timestamp"));
        assertNotNull(response3.getBody().get("timestamp"));
    }

    @Test
    @DisplayName("Status endpoint should be consistent across multiple calls")
    void testStatusEndpointConsistency() {
        // When - Call endpoint multiple times
        ResponseEntity<String> response1 = healthController.status();
        ResponseEntity<String> response2 = healthController.status();
        ResponseEntity<String> response3 = healthController.status();
        
        // Then - All responses should be identical
        assertEquals(response1.getStatusCode(), response2.getStatusCode());
        assertEquals(response1.getStatusCode(), response3.getStatusCode());
        assertEquals(response1.getBody(), response2.getBody());
        assertEquals(response1.getBody(), response3.getBody());
    }
}