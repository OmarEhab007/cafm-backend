package com.cafm.cafmbackend.data.entity;

import com.cafm.cafmbackend.data.entity.base.TenantAwareEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing file metadata in the CAFM system.
 * 
 * Purpose: Store comprehensive metadata about uploaded files including
 * MinIO storage paths, optimization results, and virus scan status.
 * 
 * Pattern: Uses tenant-aware base entity for multi-tenancy isolation
 * Java 23: No Lombok on entities per coding standards
 * Architecture: Data layer entity with proper validation
 * Standards: Follows JPA best practices with explicit column definitions
 */
@Entity
@Table(name = "file_metadata", indexes = {
    @Index(name = "idx_file_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_file_category", columnList = "category"),
    @Index(name = "idx_file_uploaded_at", columnList = "uploaded_at"),
    @Index(name = "idx_file_virus_scan", columnList = "virus_scanned, virus_scan_passed")
})
public class FileMetadata extends TenantAwareEntity {

    @Id
    @GeneratedValue
    @Column(name = "file_id")
    private UUID id;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFileName;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "stored_filename", nullable = false, length = 255)
    private String storedFileName;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "minio_object_name", nullable = false, length = 255)
    private String minioObjectName;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "bucket_name", nullable = false, length = 100)
    private String bucketName;

    @NotNull
    @Size(min = 1, max = 127)
    @Column(name = "content_type", nullable = false, length = 127)
    private String contentType;

    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType;

    @NotNull
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "original_file_size")
    private Long originalFileSize;

    @Size(max = 100)
    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Size(max = 100)
    @Column(name = "category", length = 100)
    private String category;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @NotNull
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @NotNull
    @Column(name = "is_optimized", nullable = false)
    private Boolean isOptimized = false;

    @NotNull
    @Column(name = "virus_scanned", nullable = false)
    private Boolean virusScanned = false;

    @Column(name = "virus_scan_passed")
    private Boolean virusScanPassed;

    @Column(name = "virus_scan_details")
    private String virusScanDetails;

    @Size(max = 500)
    @Column(name = "public_url", length = 500)
    private String publicUrl;

    @Size(max = 500)
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    // Image-specific metadata
    @Column(name = "image_width")
    private Integer imageWidth;

    @Column(name = "image_height")
    private Integer imageHeight;

    @Size(max = 20)
    @Column(name = "image_format", length = 20)
    private String imageFormat;

    @Column(name = "compression_ratio")
    private Integer compressionRatio;

    @NotNull
    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @NotNull
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    @Column(name = "access_count")
    private Long accessCount = 0L;

    /**
     * Protected no-args constructor for JPA.
     */
    protected FileMetadata() {
        // Required by JPA
    }

    /**
     * Constructor for creating file metadata.
     */
    public FileMetadata(String originalFileName, 
                       String storedFileName, 
                       String minioObjectName,
                       String bucketName,
                       String contentType, 
                       String fileType, 
                       Long fileSize,
                       UUID uploadedBy) {
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.minioObjectName = minioObjectName;
        this.bucketName = bucketName;
        this.contentType = contentType;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = LocalDateTime.now();
        this.isPublic = false;
        this.isOptimized = false;
        this.virusScanned = false;
        this.accessCount = 0L;
    }

    /**
     * Update image metadata after optimization.
     */
    public void updateImageMetadata(Integer width, Integer height, String format, 
                                  Long originalSize, Integer compressionRatio) {
        this.imageWidth = width;
        this.imageHeight = height;
        this.imageFormat = format;
        this.originalFileSize = originalSize;
        this.compressionRatio = compressionRatio;
        this.isOptimized = true;
    }

    /**
     * Update virus scan results.
     */
    public void updateVirusScanResults(boolean passed, String details) {
        this.virusScanned = true;
        this.virusScanPassed = passed;
        this.virusScanDetails = details;
    }

    /**
     * Record file access for analytics.
     */
    public void recordAccess() {
        this.lastAccessed = LocalDateTime.now();
        this.accessCount = this.accessCount != null ? this.accessCount + 1 : 1L;
    }

    /**
     * Set entity association for this file.
     */
    public void setEntityAssociation(String entityType, UUID entityId) {
        this.entityType = entityType;
        this.entityId = entityId;
    }

    /**
     * Check if file is an image.
     */
    public boolean isImage() {
        return "image".equals(this.fileType) || 
               (this.contentType != null && this.contentType.startsWith("image/"));
    }

    /**
     * Check if file is a document.
     */
    public boolean isDocument() {
        return "document".equals(this.fileType);
    }

    /**
     * Get human-readable file size.
     */
    public String getHumanReadableSize() {
        if (fileSize == null) return "Unknown";
        
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.2f KB", fileSize / 1024.0);
        if (fileSize < 1024 * 1024 * 1024) return String.format("%.2f MB", fileSize / (1024.0 * 1024));
        return String.format("%.2f GB", fileSize / (1024.0 * 1024 * 1024));
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getStoredFileName() { return storedFileName; }
    public void setStoredFileName(String storedFileName) { this.storedFileName = storedFileName; }

    public String getMinioObjectName() { return minioObjectName; }
    public void setMinioObjectName(String minioObjectName) { this.minioObjectName = minioObjectName; }

    public String getBucketName() { return bucketName; }
    public void setBucketName(String bucketName) { this.bucketName = bucketName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Long getOriginalFileSize() { return originalFileSize; }
    public void setOriginalFileSize(Long originalFileSize) { this.originalFileSize = originalFileSize; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

    public Boolean getIsOptimized() { return isOptimized; }
    public void setIsOptimized(Boolean isOptimized) { this.isOptimized = isOptimized; }

    public Boolean getVirusScanned() { return virusScanned; }
    public void setVirusScanned(Boolean virusScanned) { this.virusScanned = virusScanned; }

    public Boolean getVirusScanPassed() { return virusScanPassed; }
    public void setVirusScanPassed(Boolean virusScanPassed) { this.virusScanPassed = virusScanPassed; }

    public String getVirusScanDetails() { return virusScanDetails; }
    public void setVirusScanDetails(String virusScanDetails) { this.virusScanDetails = virusScanDetails; }

    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public Integer getImageWidth() { return imageWidth; }
    public void setImageWidth(Integer imageWidth) { this.imageWidth = imageWidth; }

    public Integer getImageHeight() { return imageHeight; }
    public void setImageHeight(Integer imageHeight) { this.imageHeight = imageHeight; }

    public String getImageFormat() { return imageFormat; }
    public void setImageFormat(String imageFormat) { this.imageFormat = imageFormat; }

    public Integer getCompressionRatio() { return compressionRatio; }
    public void setCompressionRatio(Integer compressionRatio) { this.compressionRatio = compressionRatio; }

    public UUID getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(UUID uploadedBy) { this.uploadedBy = uploadedBy; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public LocalDateTime getLastAccessed() { return lastAccessed; }
    public void setLastAccessed(LocalDateTime lastAccessed) { this.lastAccessed = lastAccessed; }

    public Long getAccessCount() { return accessCount; }
    public void setAccessCount(Long accessCount) { this.accessCount = accessCount; }
}