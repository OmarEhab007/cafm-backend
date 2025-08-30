package com.cafm.cafmbackend.application.service;

import com.cafm.cafmbackend.infrastructure.persistence.entity.FileMetadata;
import com.cafm.cafmbackend.infrastructure.persistence.repository.FileMetadataRepository;
import com.cafm.cafmbackend.dto.file.FileUploadRequest;
import com.cafm.cafmbackend.dto.file.FileUploadResponse;
import com.cafm.cafmbackend.shared.exception.BusinessLogicException;
import com.cafm.cafmbackend.shared.exception.ResourceNotFoundException;
import com.cafm.cafmbackend.application.service.cache.TenantCacheService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Comprehensive file upload service with MinIO integration, image optimization, and virus scanning.
 * 
 * Purpose: Provides complete file management capabilities including upload, optimization,
 * virus scanning, metadata tracking, and secure access control.
 * 
 * Pattern: Service layer with async processing and proper transaction management
 * Java 23: Uses virtual threads for I/O operations and modern exception handling
 * Architecture: Domain service with MinIO backend integration
 * Standards: Implements comprehensive security, logging, and error handling
 */
@Service
@Transactional
public class FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

    // File type validation
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
        "application/pdf", "application/msword", 
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    // Size limits (in bytes)
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    
    // Image optimization settings
    private static final int MAX_IMAGE_WIDTH = 2048;
    private static final int MAX_IMAGE_HEIGHT = 2048;
    private static final int THUMBNAIL_SIZE = 300;
    private static final float JPEG_QUALITY = 0.85f;

    private final MinioClient minioClient;
    private final FileMetadataRepository fileMetadataRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final TenantCacheService cacheService;
    
    // Async processing
    private final ExecutorService executorService;

    private final String filesBucket;
    private final String imagesBucket;
    private final String minioEndpoint;

    public FileUploadService(MinioClient minioClient,
                           FileMetadataRepository fileMetadataRepository,
                           CurrentUserService currentUserService,
                           AuditService auditService,
                           TenantCacheService cacheService,
                           @Value("${app.minio.bucket.files:cafm-files}") String filesBucket,
                           @Value("${app.minio.bucket.images:cafm-images}") String imagesBucket,
                           @Value("${app.minio.endpoint}") String minioEndpoint) {
        this.minioClient = minioClient;
        this.fileMetadataRepository = fileMetadataRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
        this.cacheService = cacheService;
        this.filesBucket = filesBucket;
        this.imagesBucket = imagesBucket;
        this.minioEndpoint = minioEndpoint;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        
        // Initialize buckets
        initializeBuckets();
    }

    /**
     * Upload a single file with comprehensive processing.
     */
    public FileUploadResponse uploadFile(MultipartFile file, FileUploadRequest request) {
        logger.info("Starting file upload: {} ({})", request.fileName(), request.contentType());
        
        try {
            // Validate file and request
            validateFileUpload(file, request);
            
            // Create file metadata
            FileMetadata metadata = createFileMetadata(file, request);
            
            // Determine bucket based on file type
            String bucket = determineTargetBucket(request.fileType());
            metadata.setBucketName(bucket);
            
            // Generate unique object name
            String objectName = generateObjectName(request.fileName(), metadata.getId());
            metadata.setMinioObjectName(objectName);
            
            // Upload to MinIO
            InputStream fileStream = file.getInputStream();
            long fileSize = file.getSize();
            
            if ("image".equals(request.fileType()) && 
                Boolean.TRUE.equals(request.requiresOptimization())) {
                // Process image with optimization
                fileStream = optimizeImage(fileStream, metadata);
                fileSize = metadata.getFileSize(); // Updated size after optimization
            }
            
            uploadToMinio(bucket, objectName, fileStream, fileSize, request.contentType());
            
            // Save metadata to database
            metadata = fileMetadataRepository.save(metadata);
            
            // Generate URLs
            String fileUrl = generateFileUrl(bucket, objectName);
            metadata.setPublicUrl(fileUrl);
            
            if (metadata.isImage()) {
                // Generate thumbnail asynchronously
                final FileMetadata finalMetadata = metadata;
                CompletableFuture.runAsync(() -> generateThumbnail(finalMetadata), executorService);
            }
            
            // Schedule virus scan asynchronously
            if (Boolean.TRUE.equals(request.requiresVirusScan())) {
                final FileMetadata finalMetadata = metadata;
                CompletableFuture.runAsync(() -> performVirusScan(finalMetadata), executorService);
            }
            
            // Update final metadata
            metadata = fileMetadataRepository.save(metadata);
            
            // Log successful upload
            auditService.logFileOperation("FILE_UPLOAD", metadata.getId(), 
                String.format("File uploaded: %s (%s)", request.fileName(), 
                formatFileSize(metadata.getFileSize())));
            
            return convertToResponse(metadata);
            
        } catch (Exception e) {
            logger.error("File upload failed for {}: {}", request.fileName(), e.getMessage(), e);
            throw new BusinessLogicException("File upload failed: " + e.getMessage(), "FILE_UPLOAD_FAILED");
        }
    }

    /**
     * Upload multiple files in batch.
     */
    public List<FileUploadResponse> uploadFiles(List<MultipartFile> files, List<FileUploadRequest> requests) {
        if (files.size() != requests.size()) {
            throw new BusinessLogicException("Files and requests count mismatch", "INVALID_REQUEST_FORMAT");
        }
        
        logger.info("Starting batch file upload: {} files", files.size());
        
        List<FileUploadResponse> responses = new ArrayList<>();
        
        for (int i = 0; i < files.size(); i++) {
            try {
                FileUploadResponse response = uploadFile(files.get(i), requests.get(i));
                responses.add(response);
            } catch (Exception e) {
                logger.error("Failed to upload file {}: {}", requests.get(i).fileName(), e.getMessage());
                // Continue with other files, but log the failure
                auditService.logFileOperation("FILE_UPLOAD_FAILED", null, 
                    String.format("File upload failed: %s - %s", requests.get(i).fileName(), e.getMessage()));
            }
        }
        
        return responses;
    }

    /**
     * Get file metadata by ID.
     */
    @Transactional(readOnly = true)
    public FileUploadResponse getFile(UUID fileId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
            .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
        
        // Record access
        metadata.recordAccess();
        fileMetadataRepository.save(metadata);
        
        return convertToResponse(metadata);
    }

    /**
     * Get files by entity association.
     */
    @Transactional(readOnly = true)
    public Page<FileUploadResponse> getFilesByEntity(String entityType, UUID entityId, Pageable pageable) {
        Page<FileMetadata> files = fileMetadataRepository
            .findByEntityTypeAndEntityIdOrderByUploadedAtDesc(entityType, entityId, pageable);
        return files.map(this::convertToResponse);
    }

    /**
     * Delete a file.
     */
    public void deleteFile(UUID fileId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
            .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
        
        try {
            // Delete from MinIO
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(metadata.getBucketName())
                    .object(metadata.getMinioObjectName())
                    .build()
            );
            
            // Delete thumbnail if exists
            if (metadata.getThumbnailUrl() != null) {
                String thumbnailObjectName = "thumbnails/" + metadata.getMinioObjectName();
                try {
                    minioClient.removeObject(
                        RemoveObjectArgs.builder()
                            .bucket(metadata.getBucketName())
                            .object(thumbnailObjectName)
                            .build()
                    );
                } catch (Exception e) {
                    logger.warn("Failed to delete thumbnail for file {}: {}", fileId, e.getMessage());
                }
            }
            
            // Soft delete from database
            fileMetadataRepository.delete(metadata);
            
            auditService.logFileOperation("FILE_DELETE", fileId, 
                String.format("File deleted: %s", metadata.getOriginalFileName()));
            
            logger.info("File deleted successfully: {} ({})", metadata.getOriginalFileName(), fileId);
            
        } catch (Exception e) {
            logger.error("Failed to delete file {}: {}", fileId, e.getMessage(), e);
            throw new BusinessLogicException("Failed to delete file: " + e.getMessage(), "FILE_DELETE_FAILED");
        }
    }

    /**
     * Get download URL for a file.
     */
    @Transactional(readOnly = true)
    public String getDownloadUrl(UUID fileId, int expirationMinutes) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
            .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
        
        try {
            String presignedUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(metadata.getBucketName())
                    .object(metadata.getMinioObjectName())
                    .expiry(expirationMinutes * 60)
                    .build()
            );
            
            // Record access
            metadata.recordAccess();
            fileMetadataRepository.save(metadata);
            
            return presignedUrl;
            
        } catch (Exception e) {
            logger.error("Failed to generate download URL for file {}: {}", fileId, e.getMessage());
            throw new BusinessLogicException("Failed to generate download URL", "FILE_ACCESS_FAILED");
        }
    }

    /**
     * Get file statistics.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFileStatistics() {
        String cacheKey = "file_stats:" + currentUserService.getCurrentTenantId();
        
        // Direct computation without cache for now
        Object stats = fileMetadataRepository.getFileStatistics();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) stats;
        
        // Add formatted sizes
        Long totalSize = (Long) result.get("totalSize");
        result.put("totalSizeFormatted", formatFileSize(totalSize));
            
        return result;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private void initializeBuckets() {
        try {
            ensureBucketExists(filesBucket);
            ensureBucketExists(imagesBucket);
            logger.info("MinIO buckets initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize MinIO buckets: {}", e.getMessage(), e);
            throw new BusinessLogicException("File storage initialization failed", "SYSTEM_ERROR");
        }
    }

    private void ensureBucketExists(String bucketName) throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            logger.info("Created MinIO bucket: {}", bucketName);
        }
    }

    private void validateFileUpload(MultipartFile file, FileUploadRequest request) {
        if (file.isEmpty()) {
            throw new BusinessLogicException("File is empty", "INVALID_FILE");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessLogicException(
                String.format("File too large: %s (max: %s)", 
                    formatFileSize(file.getSize()), formatFileSize(MAX_FILE_SIZE)),
                "FILE_TOO_LARGE");
        }
        
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessLogicException("Content type is required", "INVALID_FILE");
        }
        
        if ("image".equals(request.fileType())) {
            if (!ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
                throw new BusinessLogicException("Invalid image type: " + contentType, "INVALID_FILE_TYPE");
            }
            if (file.getSize() > MAX_IMAGE_SIZE) {
                throw new BusinessLogicException(
                    String.format("Image too large: %s (max: %s)", 
                        formatFileSize(file.getSize()), formatFileSize(MAX_IMAGE_SIZE)),
                    "FILE_TOO_LARGE");
            }
        } else if ("document".equals(request.fileType())) {
            if (!ALLOWED_DOCUMENT_TYPES.contains(contentType.toLowerCase())) {
                throw new BusinessLogicException("Invalid document type: " + contentType, "INVALID_FILE_TYPE");
            }
        }
    }

    private FileMetadata createFileMetadata(MultipartFile file, FileUploadRequest request) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        
        String storedFileName = generateStoredFileName(request.fileName());
        
        FileMetadata metadata = new FileMetadata(
            request.fileName(),
            storedFileName,
            "", // Will be set later
            "", // Will be set later
            request.contentType(),
            request.fileType(),
            file.getSize(),
            currentUserId
        );
        
        // Set optional fields
        if (request.entityType() != null && request.entityId() != null) {
            metadata.setEntityAssociation(request.entityType(), request.entityId());
        }
        
        if (request.category() != null) {
            metadata.setCategory(request.category());
        }
        
        if (request.description() != null) {
            metadata.setDescription(request.description());
        }
        
        if (request.metadata() != null) {
            metadata.setMetadata(new HashMap<>(request.metadata()));
        }
        
        metadata.setIsPublic(Boolean.TRUE.equals(request.isPublic()));
        
        return metadata;
    }

    private String determineTargetBucket(String fileType) {
        return "image".equals(fileType) ? imagesBucket : filesBucket;
    }

    private String generateObjectName(String originalFileName, UUID fileId) {
        String extension = getFileExtension(originalFileName);
        String timestamp = String.valueOf(System.currentTimeMillis());
        return String.format("%s/%s_%s%s", 
            LocalDateTime.now().toLocalDate(), 
            fileId.toString().replace("-", ""), 
            timestamp, 
            extension.isEmpty() ? "" : "." + extension);
    }

    private String generateStoredFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid + (extension.isEmpty() ? "" : "." + extension);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private void uploadToMinio(String bucket, String objectName, InputStream stream, 
                              long size, String contentType) throws Exception {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(stream, size, -1)
                .contentType(contentType)
                .build()
        );
    }

    private InputStream optimizeImage(InputStream inputStream, FileMetadata metadata) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputStream);
        if (originalImage == null) {
            throw new BusinessLogicException("Invalid image format", "INVALID_FILE");
        }
        
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        long originalSize = metadata.getFileSize();
        
        // Calculate new dimensions
        Dimension newDimension = calculateOptimalDimensions(originalWidth, originalHeight);
        
        // Resize image if necessary
        BufferedImage optimizedImage = originalImage;
        if (newDimension.width != originalWidth || newDimension.height != originalHeight) {
            optimizedImage = resizeImage(originalImage, newDimension.width, newDimension.height);
        }
        
        // Convert to bytes
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String format = metadata.getContentType().equals("image/png") ? "PNG" : "JPEG";
        
        if ("JPEG".equals(format)) {
            // Apply JPEG compression
            writeJPEGWithQuality(optimizedImage, outputStream, JPEG_QUALITY);
        } else {
            ImageIO.write(optimizedImage, format, outputStream);
        }
        
        byte[] optimizedBytes = outputStream.toByteArray();
        
        // Update metadata
        int compressionRatio = (int) ((1.0 - (double) optimizedBytes.length / originalSize) * 100);
        metadata.updateImageMetadata(newDimension.width, newDimension.height, format, originalSize, compressionRatio);
        metadata.setFileSize((long) optimizedBytes.length);
        
        logger.info("Image optimized: {}x{} -> {}x{}, {} -> {} ({}% reduction)",
            originalWidth, originalHeight, newDimension.width, newDimension.height,
            formatFileSize(originalSize), formatFileSize((long) optimizedBytes.length), compressionRatio);
        
        return new ByteArrayInputStream(optimizedBytes);
    }

    private Dimension calculateOptimalDimensions(int width, int height) {
        if (width <= MAX_IMAGE_WIDTH && height <= MAX_IMAGE_HEIGHT) {
            return new Dimension(width, height);
        }
        
        double widthRatio = (double) MAX_IMAGE_WIDTH / width;
        double heightRatio = (double) MAX_IMAGE_HEIGHT / height;
        double ratio = Math.min(widthRatio, heightRatio);
        
        return new Dimension((int) (width * ratio), (int) (height * ratio));
    }

    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(original, 0, 0, width, height, null);
        graphics.dispose();
        return resized;
    }

    private void writeJPEGWithQuality(BufferedImage image, OutputStream output, float quality) throws IOException {
        var writers = ImageIO.getImageWritersByFormatName("JPEG");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available");
        }
        
        var writer = writers.next();
        var params = writer.getDefaultWriteParam();
        params.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(quality);
        
        try (var imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            writer.write(null, new javax.imageio.IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
    }

    private void generateThumbnail(FileMetadata metadata) {
        try {
            // Download original image
            InputStream imageStream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(metadata.getBucketName())
                    .object(metadata.getMinioObjectName())
                    .build()
            );
            
            BufferedImage originalImage = ImageIO.read(imageStream);
            if (originalImage == null) {
                logger.warn("Could not read image for thumbnail generation: {}", metadata.getId());
                return;
            }
            
            // Create thumbnail
            BufferedImage thumbnail = createThumbnail(originalImage, THUMBNAIL_SIZE);
            
            // Upload thumbnail
            ByteArrayOutputStream thumbnailStream = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "JPEG", thumbnailStream);
            
            String thumbnailObjectName = "thumbnails/" + metadata.getMinioObjectName();
            
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(metadata.getBucketName())
                    .object(thumbnailObjectName)
                    .stream(new ByteArrayInputStream(thumbnailStream.toByteArray()), 
                           thumbnailStream.size(), -1)
                    .contentType("image/jpeg")
                    .build()
            );
            
            // Update metadata with thumbnail URL
            String thumbnailUrl = generateFileUrl(metadata.getBucketName(), thumbnailObjectName);
            metadata.setThumbnailUrl(thumbnailUrl);
            fileMetadataRepository.save(metadata);
            
            logger.info("Thumbnail generated for file: {}", metadata.getId());
            
        } catch (Exception e) {
            logger.error("Failed to generate thumbnail for file {}: {}", metadata.getId(), e.getMessage(), e);
        }
    }

    private BufferedImage createThumbnail(BufferedImage original, int thumbnailSize) {
        int width = original.getWidth();
        int height = original.getHeight();
        
        // Calculate thumbnail dimensions maintaining aspect ratio
        double ratio = Math.min((double) thumbnailSize / width, (double) thumbnailSize / height);
        int newWidth = (int) (width * ratio);
        int newHeight = (int) (height * ratio);
        
        return resizeImage(original, newWidth, newHeight);
    }

    private void performVirusScan(FileMetadata metadata) {
        try {
            // Simulate virus scan - In production, integrate with actual antivirus
            // For now, just mark as scanned and passed
            boolean scanPassed = simulateVirusScan(metadata);
            String details = scanPassed ? "Clean" : "Threat detected";
            
            metadata.updateVirusScanResults(scanPassed, details);
            fileMetadataRepository.save(metadata);
            
            if (!scanPassed) {
                logger.warn("Virus scan failed for file: {} - {}", metadata.getId(), details);
                auditService.logSecurityEvent("VIRUS_DETECTED", 
                    String.format("Virus detected in file: %s", metadata.getOriginalFileName()));
            } else {
                logger.info("Virus scan passed for file: {}", metadata.getId());
            }
            
        } catch (Exception e) {
            logger.error("Virus scan failed for file {}: {}", metadata.getId(), e.getMessage(), e);
            metadata.updateVirusScanResults(false, "Scan failed: " + e.getMessage());
            fileMetadataRepository.save(metadata);
        }
    }

    private boolean simulateVirusScan(FileMetadata metadata) {
        // In production, integrate with actual antivirus service
        // For demo purposes, assume all files are clean unless filename contains "virus"
        return !metadata.getOriginalFileName().toLowerCase().contains("virus");
    }

    private String generateFileUrl(String bucket, String objectName) {
        return String.format("%s/%s/%s", minioEndpoint, bucket, objectName);
    }

    private FileUploadResponse convertToResponse(FileMetadata metadata) {
        FileUploadResponse.FileMetrics metrics = metadata.isImage() ? 
            FileUploadResponse.FileMetrics.forImage(
                metadata.getFileSize(),
                metadata.getOriginalFileSize(),
                metadata.getImageWidth(),
                metadata.getImageHeight(),
                metadata.getImageFormat()
            ) :
            FileUploadResponse.FileMetrics.create(
                metadata.getFileSize(),
                metadata.getOriginalFileSize()
            );

        return new FileUploadResponse(
            metadata.getId(),
            metadata.getOriginalFileName(),
            metadata.getPublicUrl(),
            metadata.getThumbnailUrl(),
            metadata.getContentType(),
            metadata.getFileSize(),
            metadata.getFileType(),
            metadata.getEntityType(),
            metadata.getEntityId(),
            metadata.getCategory(),
            metadata.getDescription(),
            metadata.getMetadata() != null ? 
                metadata.getMetadata().entrySet().stream()
                    .collect(HashMap::new, (m, e) -> m.put(e.getKey(), String.valueOf(e.getValue())), HashMap::putAll) :
                Map.of(),
            metadata.getIsPublic(),
            metadata.getIsOptimized(),
            metadata.getVirusScanned(),
            metadata.getVirusScanPassed(),
            metadata.getUploadedBy().toString(),
            metadata.getUploadedAt(),
            metrics
        );
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null) return "Unknown";
        
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}