package com.cafm.cafmbackend.infrastructure.persistence.projection;

import com.cafm.cafmbackend.shared.enums.UserStatus;
import com.cafm.cafmbackend.shared.enums.UserType;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Projection for user list queries.
 * 
 * Purpose: Optimize user list queries by fetching only necessary fields
 * Pattern: Spring Data JPA projection interface
 * Java 23: Uses interface-based projections
 * Architecture: Data layer optimization
 * Standards: Reduces data transfer and memory usage
 */
public interface UserListProjection {
    UUID getId();
    String getEmail();
    String getFirstName();
    String getLastName();
    UserType getUserType();
    UserStatus getStatus();
    Boolean getIsActive();
    LocalDateTime getLastLoginAt();
    
    // Nested projection for company
    CompanyInfo getCompany();
    
    interface CompanyInfo {
        UUID getId();
        String getName();
    }
}