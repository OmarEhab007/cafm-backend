package com.cafm.cafmbackend.infrastructure.persistence.projection;

import java.util.UUID;

/**
 * Projection for school list queries.
 * 
 * Purpose: Optimize school list queries with essential fields only
 * Pattern: Spring Data JPA projection interface
 * Java 23: Efficient data transfer using projections
 * Architecture: Data layer optimization
 * Standards: Minimizes network and memory overhead
 */
public interface SchoolListProjection {
    UUID getId();
    String getCode();
    String getName();
    String getNameAr();
    String getType();
    String getCity();
    String getDistrict();
    Boolean getIsActive();
    Integer getStudentCount();
    Integer getStaffCount();
    
    // Calculated field - can be implemented via @Value in repository
    default String getDisplayName() {
        return getName() + " (" + getCode() + ")";
    }
}