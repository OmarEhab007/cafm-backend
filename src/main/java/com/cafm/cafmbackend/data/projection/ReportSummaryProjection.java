package com.cafm.cafmbackend.data.projection;

import com.cafm.cafmbackend.data.enums.ReportPriority;
import com.cafm.cafmbackend.data.enums.ReportStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Projection for report summary queries.
 * 
 * Purpose: Optimize report list queries with minimal data
 * Pattern: Spring Data JPA projection interface
 * Java 23: Interface-based projections for performance
 * Architecture: Data layer optimization
 * Standards: Reduces query overhead for list operations
 */
public interface ReportSummaryProjection {
    UUID getId();
    String getReportNumber();
    String getTitle();
    ReportStatus getStatus();
    ReportPriority getPriority();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
    
    // Nested projection for school
    SchoolInfo getSchool();
    
    // Nested projection for supervisor
    UserInfo getSupervisor();
    
    interface SchoolInfo {
        UUID getId();
        String getName();
        String getCode();
    }
    
    interface UserInfo {
        UUID getId();
        String getFirstName();
        String getLastName();
        String getEmail();
    }
}