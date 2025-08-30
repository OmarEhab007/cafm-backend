package com.cafm.cafmbackend.dto.report;

import com.cafm.cafmbackend.shared.enums.ReportStatus;

import java.math.BigDecimal;

/**
 * Request DTO for updating report status.
 * 
 * Purpose: Provides structured data for report status transitions
 * Pattern: Immutable record DTO for status update operations
 * Java 23: Leverages record pattern for data transfer
 * Architecture: DTO for report workflow management
 * Standards: Immutable design with validation support
 */
public record ReportStatusUpdateRequest(
    ReportStatus newStatus,
    String notes,
    BigDecimal actualCost
) {
    
    /**
     * Create status update request with notes only.
     */
    public static ReportStatusUpdateRequest withNotes(ReportStatus newStatus, String notes) {
        return new ReportStatusUpdateRequest(newStatus, notes, null);
    }
    
    /**
     * Create status update request with cost.
     */
    public static ReportStatusUpdateRequest withCost(ReportStatus newStatus, String notes, BigDecimal actualCost) {
        return new ReportStatusUpdateRequest(newStatus, notes, actualCost);
    }
    
    /**
     * Create simple status update without additional data.
     */
    public static ReportStatusUpdateRequest simple(ReportStatus newStatus) {
        return new ReportStatusUpdateRequest(newStatus, null, null);
    }
}