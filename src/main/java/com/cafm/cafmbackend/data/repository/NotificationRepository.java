package com.cafm.cafmbackend.data.repository;

import com.cafm.cafmbackend.data.entity.Notification;
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
 * Repository interface for Notification entity.
 * 
 * Purpose: Data access layer for notification management
 * Pattern: Spring Data JPA repository
 * Java 23: Leverages Spring Data's query derivation
 * Architecture: Data layer repository with tenant awareness
 * Standards: Follows repository pattern with proper query methods
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    /**
     * Find notifications by user
     */
    Page<Notification> findByUser_IdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    /**
     * Find unread notifications by user
     */
    List<Notification> findByUser_IdAndReadFalseOrderByCreatedAtDesc(UUID userId);
    
    /**
     * Count unread notifications by user
     */
    long countByUser_IdAndReadFalse(UUID userId);
    
    /**
     * Mark notification as read
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt WHERE n.id = :id")
    void markAsRead(@Param("id") UUID id, @Param("readAt") LocalDateTime readAt);
    
    /**
     * Mark all notifications as read for a user
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt WHERE n.user.id = :userId AND n.read = false")
    int markAllAsRead(@Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt);
    
    /**
     * Delete old notifications
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);
}