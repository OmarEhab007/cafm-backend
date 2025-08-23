package com.cafm.cafmbackend.data.repository;

import com.cafm.cafmbackend.data.entity.FileMetadata;
import com.cafm.cafmbackend.data.repository.base.TenantAwareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for FileMetadata entities with tenant-aware operations.
 * 
 * Purpose: Provides data access methods for file metadata with 
 * automatic tenant isolation and performance optimizations.
 * 
 * Pattern: Extends TenantAwareRepository for automatic tenant filtering
 * Java 23: Uses modern query methods and optional handling
 * Architecture: Data layer repository with tenant isolation
 * Standards: Follows Spring Data naming conventions and security practices
 */
@Repository
public interface FileMetadataRepository extends TenantAwareRepository<FileMetadata, UUID> {

    /**
     * Find files by entity association.
     */
    Page<FileMetadata> findByEntityTypeAndEntityIdOrderByUploadedAtDesc(
        String entityType, UUID entityId, Pageable pageable);

    /**
     * Find files by category.
     */
    Page<FileMetadata> findByCategoryOrderByUploadedAtDesc(
        String category, Pageable pageable);

    /**
     * Find files by file type.
     */
    Page<FileMetadata> findByFileTypeOrderByUploadedAtDesc(
        String fileType, Pageable pageable);

    /**
     * Find public files.
     */
    Page<FileMetadata> findByIsPublicTrueOrderByUploadedAtDesc(Pageable pageable);

    /**
     * Find files uploaded by a specific user.
     */
    Page<FileMetadata> findByUploadedByOrderByUploadedAtDesc(
        UUID uploadedBy, Pageable pageable);

    /**
     * Find files that require virus scanning.
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.virusScanned = false " +
           "AND f.deletedAt IS NULL ORDER BY f.uploadedAt ASC")
    List<FileMetadata> findFilesRequiringVirusScan();

    /**
     * Find files that failed virus scanning.
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.virusScanned = true " +
           "AND f.virusScanPassed = false AND f.deletedAt IS NULL " +
           "ORDER BY f.uploadedAt DESC")
    List<FileMetadata> findVirusInfectedFiles();

    /**
     * Find files that require optimization.
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.fileType = 'image' " +
           "AND f.isOptimized = false AND f.deletedAt IS NULL " +
           "ORDER BY f.uploadedAt ASC")
    List<FileMetadata> findImagesRequiringOptimization();

    /**
     * Find files by MinIO object name for cleanup operations.
     */
    Optional<FileMetadata> findByMinioObjectNameAndBucketName(
        String minioObjectName, String bucketName);

    /**
     * Find orphaned files (no entity association).
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.entityType IS NULL " +
           "OR f.entityId IS NULL AND f.deletedAt IS NULL " +
           "ORDER BY f.uploadedAt ASC")
    List<FileMetadata> findOrphanedFiles();

    /**
     * Find old files for cleanup (older than specified date).
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.uploadedAt < :cutoffDate " +
           "AND f.deletedAt IS NULL ORDER BY f.uploadedAt ASC")
    List<FileMetadata> findOldFiles(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Count files by entity.
     */
    long countByEntityTypeAndEntityId(String entityType, UUID entityId);

    /**
     * Count files by type.
     */
    long countByFileType(String fileType);

    /**
     * Count files by user.
     */
    long countByUploadedBy(UUID uploadedBy);

    /**
     * Calculate total storage used by tenant.
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f " +
           "WHERE f.deletedAt IS NULL")
    long calculateTotalStorageUsed();

    /**
     * Calculate storage used by file type.
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f " +
           "WHERE f.fileType = :fileType AND f.deletedAt IS NULL")
    long calculateStorageUsedByType(@Param("fileType") String fileType);

    /**
     * Calculate storage used by entity.
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f " +
           "WHERE f.entityType = :entityType AND f.entityId = :entityId " +
           "AND f.deletedAt IS NULL")
    long calculateStorageUsedByEntity(
        @Param("entityType") String entityType, 
        @Param("entityId") UUID entityId);

    /**
     * Update access tracking for a file.
     */
    @Modifying
    @Query("UPDATE FileMetadata f SET f.lastAccessed = :accessTime, " +
           "f.accessCount = COALESCE(f.accessCount, 0) + 1 " +
           "WHERE f.id = :fileId")
    int updateAccessTracking(@Param("fileId") UUID fileId, 
                           @Param("accessTime") LocalDateTime accessTime);

    /**
     * Find most accessed files.
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.deletedAt IS NULL " +
           "ORDER BY f.accessCount DESC, f.uploadedAt DESC")
    Page<FileMetadata> findMostAccessedFiles(Pageable pageable);

    /**
     * Search files by filename.
     */
    @Query("SELECT f FROM FileMetadata f WHERE " +
           "LOWER(f.originalFileName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND f.deletedAt IS NULL ORDER BY f.uploadedAt DESC")
    Page<FileMetadata> searchByFileName(@Param("searchTerm") String searchTerm, 
                                       Pageable pageable);

    /**
     * Find files by content type pattern.
     */
    @Query("SELECT f FROM FileMetadata f WHERE " +
           "f.contentType LIKE :contentTypePattern AND f.deletedAt IS NULL " +
           "ORDER BY f.uploadedAt DESC")
    Page<FileMetadata> findByContentTypePattern(
        @Param("contentTypePattern") String contentTypePattern, 
        Pageable pageable);

    /**
     * Find recent uploads within date range.
     */
    @Query("SELECT f FROM FileMetadata f WHERE " +
           "f.uploadedAt >= :startDate AND f.uploadedAt <= :endDate " +
           "AND f.deletedAt IS NULL ORDER BY f.uploadedAt DESC")
    Page<FileMetadata> findRecentUploads(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable);

    /**
     * Get file statistics for dashboard.
     */
    @Query("SELECT new map(" +
           "COUNT(f) as totalFiles, " +
           "COALESCE(SUM(f.fileSize), 0) as totalSize, " +
           "COUNT(CASE WHEN f.fileType = 'image' THEN 1 END) as imageCount, " +
           "COUNT(CASE WHEN f.fileType = 'document' THEN 1 END) as documentCount, " +
           "COUNT(CASE WHEN f.virusScanned = true AND f.virusScanPassed = false THEN 1 END) as infectedCount" +
           ") FROM FileMetadata f WHERE f.deletedAt IS NULL")
    Object getFileStatistics();
}