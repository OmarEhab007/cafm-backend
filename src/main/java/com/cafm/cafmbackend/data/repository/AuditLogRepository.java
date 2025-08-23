package com.cafm.cafmbackend.data.repository;

import com.cafm.cafmbackend.data.entity.AuditLog;
import com.cafm.cafmbackend.data.entity.AuditLog.AuditAction;
import com.cafm.cafmbackend.data.entity.AuditLog.AuditStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for audit log queries.
 * 
 * Purpose: Data access for audit logs
 * Pattern: Spring Data JPA repository
 * Java 23: Efficient audit log queries
 * Architecture: Data layer repository
 * Standards: Read-only audit queries
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    /**
     * Find audit logs by user
     */
    Page<AuditLog> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);
    
    /**
     * Find audit logs by company
     */
    Page<AuditLog> findByCompanyIdOrderByTimestampDesc(UUID companyId, Pageable pageable);
    
    /**
     * Find audit logs by entity
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, UUID entityId);
    
    /**
     * Find audit logs by action
     */
    Page<AuditLog> findByActionOrderByTimestampDesc(AuditAction action, Pageable pageable);
    
    /**
     * Find audit logs by date range
     */
    @Query("""
        SELECT al FROM AuditLog al 
        WHERE al.timestamp BETWEEN :startDate AND :endDate 
        ORDER BY al.timestamp DESC
        """)
    Page<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   Pageable pageable);
    
    /**
     * Find failed operations
     */
    Page<AuditLog> findByStatusOrderByTimestampDesc(AuditStatus status, Pageable pageable);
    
    /**
     * Find authentication events
     */
    @Query("""
        SELECT al FROM AuditLog al 
        WHERE al.action IN ('LOGIN', 'LOGOUT', 'LOGIN_FAILED') 
        AND al.username = :username 
        ORDER BY al.timestamp DESC
        """)
    List<AuditLog> findAuthenticationEvents(@Param("username") String username);
    
    /**
     * Find security events
     */
    @Query("""
        SELECT al FROM AuditLog al 
        WHERE al.action IN ('UNAUTHORIZED_ACCESS', 'RATE_LIMIT_EXCEEDED', 'SUSPICIOUS_ACTIVITY') 
        AND al.timestamp > :since 
        ORDER BY al.timestamp DESC
        """)
    List<AuditLog> findSecurityEvents(@Param("since") LocalDateTime since);
    
    /**
     * Count actions by type
     */
    @Query("""
        SELECT al.action, COUNT(al) 
        FROM AuditLog al 
        WHERE al.companyId = :companyId 
        AND al.timestamp BETWEEN :startDate AND :endDate 
        GROUP BY al.action
        """)
    List<Object[]> countActionsByType(@Param("companyId") UUID companyId,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get user activity summary
     */
    @Query("""
        SELECT al.userId, al.username, COUNT(al) as actionCount, 
               MAX(al.timestamp) as lastActivity 
        FROM AuditLog al 
        WHERE al.companyId = :companyId 
        AND al.timestamp > :since 
        GROUP BY al.userId, al.username 
        ORDER BY actionCount DESC
        """)
    List<Object[]> getUserActivitySummary(@Param("companyId") UUID companyId,
                                          @Param("since") LocalDateTime since);
    
    /**
     * Find suspicious activities
     */
    @Query("""
        SELECT al FROM AuditLog al 
        WHERE al.status = 'FAILURE' 
        AND al.ipAddress = :ipAddress 
        AND al.timestamp > :since 
        ORDER BY al.timestamp DESC
        """)
    List<AuditLog> findFailedAttemptsByIp(@Param("ipAddress") String ipAddress,
                                          @Param("since") LocalDateTime since);
    
    /**
     * Get API usage statistics
     */
    @Query("""
        SELECT al.apiEndpoint, al.httpMethod, 
               COUNT(al) as requestCount,
               AVG(al.durationMs) as avgDuration,
               MAX(al.durationMs) as maxDuration 
        FROM AuditLog al 
        WHERE al.apiEndpoint IS NOT NULL 
        AND al.timestamp BETWEEN :startDate AND :endDate 
        GROUP BY al.apiEndpoint, al.httpMethod 
        ORDER BY requestCount DESC
        """)
    List<Object[]> getApiUsageStatistics(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
    
    /**
     * Clean old audit logs
     */
    @Modifying
    @Query("DELETE FROM AuditLog al WHERE al.timestamp < :cutoffDate")
    int deleteOldLogs(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Find changes to specific entity
     */
    @Query("""
        SELECT al FROM AuditLog al 
        WHERE al.entityType = :entityType 
        AND al.entityId = :entityId 
        AND al.action IN ('CREATE', 'UPDATE', 'DELETE') 
        ORDER BY al.timestamp DESC
        """)
    List<AuditLog> findEntityChanges(@Param("entityType") String entityType,
                                     @Param("entityId") UUID entityId);
    
    /**
     * Get compliance report data
     */
    @Query("""
        SELECT DATE(al.timestamp) as date, 
               al.action, 
               COUNT(al) as count 
        FROM AuditLog al 
        WHERE al.companyId = :companyId 
        AND al.timestamp BETWEEN :startDate AND :endDate 
        GROUP BY DATE(al.timestamp), al.action 
        ORDER BY date DESC, count DESC
        """)
    List<Object[]> getComplianceReportData(@Param("companyId") UUID companyId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find by action and company ID
     */
    Page<AuditLog> findByActionAndCompanyIdOrderByTimestampDesc(AuditAction action, UUID companyId, Pageable pageable);
    
    /**
     * Find by timestamp between and company ID
     */
    Page<AuditLog> findByTimestampBetweenAndCompanyIdOrderByTimestampDesc(LocalDateTime start, LocalDateTime end, UUID companyId, Pageable pageable);
    
    /**
     * Search audit logs with keyword
     */
    @Query("""
        SELECT al FROM AuditLog al 
        WHERE al.companyId = :companyId 
        AND (al.username LIKE %:keyword% 
             OR al.entityType LIKE %:keyword% 
             OR al.entityName LIKE %:keyword%
             OR al.changes LIKE %:keyword%) 
        ORDER BY al.timestamp DESC
        """)
    Page<AuditLog> searchAuditLogs(@Param("keyword") String keyword, @Param("companyId") UUID companyId, Pageable pageable);
    
    /**
     * Count by company ID and timestamp between
     */
    long countByCompanyIdAndTimestampBetween(UUID companyId, LocalDateTime start, LocalDateTime end);
    
    /**
     * Count by action and company ID and timestamp between
     */
    long countByActionAndCompanyIdAndTimestampBetween(AuditAction action, UUID companyId, LocalDateTime start, LocalDateTime end);
    
    /**
     * Count by entity type and company ID
     */
    @Query("""
        SELECT al.entityType, COUNT(al) 
        FROM AuditLog al 
        WHERE al.companyId = :companyId 
        AND al.timestamp BETWEEN :start AND :end 
        GROUP BY al.entityType
        """)
    List<Object[]> countByEntityTypeAndCompanyId(@Param("companyId") UUID companyId, 
                                                 @Param("start") LocalDateTime start, 
                                                 @Param("end") LocalDateTime end);
    
    /**
     * Count by user and company ID
     */
    @Query("""
        SELECT al.userId, COUNT(al) 
        FROM AuditLog al 
        WHERE al.companyId = :companyId 
        AND al.timestamp BETWEEN :start AND :end 
        GROUP BY al.userId
        """)
    List<Object[]> countByUserAndCompanyId(@Param("companyId") UUID companyId, 
                                           @Param("start") LocalDateTime start, 
                                           @Param("end") LocalDateTime end);
}