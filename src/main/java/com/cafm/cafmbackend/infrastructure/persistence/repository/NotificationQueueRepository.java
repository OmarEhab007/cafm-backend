package com.cafm.cafmbackend.infrastructure.persistence.repository;

import com.cafm.cafmbackend.infrastructure.persistence.entity.NotificationQueue;
import com.cafm.cafmbackend.infrastructure.persistence.entity.NotificationQueue.NotificationStatus;
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
 * Repository interface for NotificationQueue entity.
 * 
 * Purpose: Data access layer for queued notification management
 * Pattern: Spring Data JPA repository
 * Java 23: Leverages Spring Data's query derivation
 * Architecture: Data layer repository for notification queue
 * Standards: Follows repository pattern with retry logic support
 */
@Repository
public interface NotificationQueueRepository extends JpaRepository<NotificationQueue, UUID> {
    
    /**
     * Find pending notifications ready to be sent
     */
    @Query("""
        SELECT nq FROM NotificationQueue nq 
        WHERE nq.status = :status 
        AND (nq.scheduledFor IS NULL OR nq.scheduledFor <= :now) 
        AND nq.retryCount < nq.maxRetries 
        ORDER BY nq.priority DESC, nq.scheduledFor ASC
        """)
    List<NotificationQueue> findPendingNotifications(@Param("status") NotificationStatus status, @Param("now") LocalDateTime now, Pageable pageable);
    
    /**
     * Find failed notifications for retry
     */
    @Query("""
        SELECT nq FROM NotificationQueue nq 
        WHERE nq.status = :status 
        AND nq.retryCount < nq.maxRetries 
        AND nq.nextRetryAt <= :now 
        ORDER BY nq.priority DESC, nq.nextRetryAt ASC
        """)
    List<NotificationQueue> findRetriableNotifications(@Param("status") NotificationStatus status, @Param("now") LocalDateTime now, Pageable pageable);
    
    /**
     * Update notification status
     */
    @Modifying
    @Query("""
        UPDATE NotificationQueue nq 
        SET nq.status = :status, 
            nq.processed = true, 
            nq.errorMessage = :errorMessage 
        WHERE nq.id = :id
        """)
    void updateStatus(@Param("id") UUID id, 
                     @Param("status") NotificationStatus status,
                     @Param("errorMessage") String errorMessage);
    
    /**
     * Increment retry count and set next retry time
     */
    @Modifying
    @Query("""
        UPDATE NotificationQueue nq 
        SET nq.retryCount = nq.retryCount + 1, 
            nq.nextRetryAt = :nextRetryAt,
            nq.status = :status 
        WHERE nq.id = :id
        """)
    void incrementRetryCount(@Param("id") UUID id, @Param("nextRetryAt") LocalDateTime nextRetryAt, @Param("status") NotificationStatus status);
    
    /**
     * Find notifications by user and status
     */
    Page<NotificationQueue> findByUser_IdAndStatusOrderByCreatedAtDesc(UUID userId, 
                                                                      NotificationStatus status, 
                                                                      Pageable pageable);
    
    /**
     * Count notifications by status
     */
    long countByStatus(NotificationStatus status);
    
    /**
     * Delete old processed notifications
     */
    @Modifying
    @Query("""
        DELETE FROM NotificationQueue nq 
        WHERE nq.status IN :processedStatuses 
        AND (nq.sentAt < :cutoffDate OR nq.failedAt < :cutoffDate)
        """)
    int deleteOldProcessedNotifications(@Param("processedStatuses") List<NotificationStatus> processedStatuses, @Param("cutoffDate") LocalDateTime cutoffDate);
}