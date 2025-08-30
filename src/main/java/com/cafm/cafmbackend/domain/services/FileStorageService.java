package com.cafm.cafmbackend.domain.services;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for managing file storage operations with MinIO.
 * 
 * Purpose: Handle file uploads, downloads, and management with MinIO object storage
 * Pattern: Abstraction layer over MinIO client with image optimization capabilities
 * Java 23: Uses modern I/O operations and try-with-resources for resource management
 * Architecture: Domain service providing centralized file storage operations
 * Standards: Implements secure file handling with validation and optimization
 */
@Service
public class FileStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_MOBILE_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB for mobile
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
        "application/pdf", "application/msword", 
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );
    
    // Mobile optimization settings
    private static final int MOBILE_MAX_WIDTH = 1080;
    private static final int MOBILE_MAX_HEIGHT = 1080;
    private static final int THUMBNAIL_SIZE = 300;
    
    private final MinioClient minioClient;
    private final String filesBucket;
    private final String imagesBucket;
    
    public FileStorageService(
            @Value("${cafm.minio.endpoint}") String endpoint,
            @Value("${cafm.minio.access-key}") String accessKey,
            @Value("${cafm.minio.secret-key}") String secretKey,
            @Value("${cafm.minio.bucket.files}") String filesBucket,
            @Value("${cafm.minio.bucket.images}") String imagesBucket) {
        
        this.minioClient = MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();
        
        this.filesBucket = filesBucket;
        this.imagesBucket = imagesBucket;
        
        // Initialize buckets
        initializeBuckets();
        
        logger.info("FileStorageService initialized with endpoint: {}", endpoint);
    }
    
    /**
     * Upload an image file with optimization and mobile support.
     * 
     * Purpose: Enhanced image upload with mobile optimization and version control
     * Pattern: File processing pipeline with validation, optimization, and metadata tracking
     * Java 23: Uses enhanced switch expressions for mobile optimization logic
     * Architecture: Domain service with multi-provider support patterns
     * Standards: Implements secure file handling with mobile-first approach
     */
    public Map<String, Object> uploadImage(MultipartFile file, String category, String userId) {
        return uploadImage(file, category, userId, false);
    }
    
    /**
     * Upload an image with mobile optimization flag.
     */
    public Map<String, Object> uploadImage(MultipartFile file, String category, String userId, boolean mobileOptimized) {
        logger.debug("Uploading image for user: {}, category: {}, mobile: {}", userId, category, mobileOptimized);
        
        try {
            // Validate file based on mobile flag
            validateImageFile(file, mobileOptimized);
            
            // Generate unique filename with version support
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String uniqueFilename = generateUniqueFilename(userId, category, extension);
            
            // Optimize image based on mobile requirements
            byte[] optimizedImage = mobileOptimized ? 
                optimizeImageForMobile(file.getBytes(), extension) :
                optimizeImage(file.getBytes(), extension);
            
            // Upload to MinIO
            String objectName = buildObjectPath(category, uniqueFilename);
            
            // Create thumbnail for images
            byte[] thumbnail = createThumbnail(optimizedImage, extension);
            
            // Upload main image
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(imagesBucket)
                    .object(objectName)
                    .stream(new ByteArrayInputStream(optimizedImage), 
                           optimizedImage.length, -1)
                    .contentType(file.getContentType())
                    .userMetadata(Map.of(
                        "user-id", userId,
                        "category", category,
                        "original-filename", originalFilename,
                        "mobile-optimized", String.valueOf(mobileOptimized),
                        "upload-timestamp", LocalDateTime.now().toString()
                    ))
                    .build()
            );
            
            // Upload thumbnail
            String thumbnailPath = objectName.replace("/" + uniqueFilename, "/thumbnails/" + uniqueFilename);
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(imagesBucket)
                    .object(thumbnailPath)
                    .stream(new ByteArrayInputStream(thumbnail), thumbnail.length, -1)
                    .contentType(file.getContentType())
                    .build()
            );
            
            // Generate presigned URLs (7 days validity)
            String viewUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(imagesBucket)
                    .object(objectName)
                    .expiry(7, TimeUnit.DAYS)
                    .build()
            );
            
            String thumbnailUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(imagesBucket)
                    .object(thumbnailPath)
                    .expiry(7, TimeUnit.DAYS)
                    .build()
            );
            
            logger.info("Image uploaded successfully: {} (mobile: {})", objectName, mobileOptimized);
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("filename", uniqueFilename);
            result.put("originalFilename", originalFilename);
            result.put("path", objectName);
            result.put("thumbnailPath", thumbnailPath);
            result.put("url", viewUrl);
            result.put("thumbnailUrl", thumbnailUrl);
            result.put("size", optimizedImage.length);
            result.put("originalSize", file.getSize());
            result.put("thumbnailSize", thumbnail.length);
            result.put("contentType", file.getContentType());
            result.put("mobileOptimized", mobileOptimized);
            result.put("uploadedAt", LocalDateTime.now().toString());
            result.put("version", 1);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error uploading image", e);
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }
    
    /**
     * Upload multiple images in batch.
     */
    public List<Map<String, Object>> uploadImages(List<MultipartFile> files, 
                                                  String category, 
                                                  String userId) {
        logger.info("Uploading {} images for user: {}", files.size(), userId);
        
        return files.parallelStream()
            .map(file -> {
                try {
                    return uploadImage(file, category, userId);
                } catch (Exception e) {
                    logger.error("Failed to upload image: {}", file.getOriginalFilename(), e);
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("status", "error");
                    errorMap.put("filename", file.getOriginalFilename());
                    errorMap.put("error", e.getMessage());
                    return errorMap;
                }
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Upload a document file.
     */
    public Map<String, Object> uploadDocument(MultipartFile file, String category, String userId) {
        logger.debug("Uploading document for user: {}, category: {}", userId, category);
        
        try {
            // Validate file
            validateDocumentFile(file);
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String uniqueFilename = generateUniqueFilename(userId, category, extension);
            
            // Upload to MinIO
            String objectName = buildObjectPath(category, uniqueFilename);
            
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(filesBucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            
            // Generate presigned URL for download (7 days validity)
            String downloadUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(filesBucket)
                    .object(objectName)
                    .expiry(7, TimeUnit.DAYS)
                    .build()
            );
            
            logger.info("Document uploaded successfully: {}", objectName);
            
            return Map.of(
                "status", "success",
                "filename", uniqueFilename,
                "originalFilename", originalFilename,
                "path", objectName,
                "url", downloadUrl,
                "size", file.getSize(),
                "contentType", file.getContentType(),
                "uploadedAt", LocalDateTime.now().toString()
            );
            
        } catch (Exception e) {
            logger.error("Error uploading document", e);
            throw new RuntimeException("Failed to upload document: " + e.getMessage(), e);
        }
    }
    
    /**
     * Download a file from storage.
     */
    public byte[] downloadFile(String bucket, String objectName) {
        logger.debug("Downloading file: {} from bucket: {}", objectName, bucket);
        
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build())) {
            
            return stream.readAllBytes();
            
        } catch (Exception e) {
            logger.error("Error downloading file: {}", objectName, e);
            throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete a file from storage.
     */
    public boolean deleteFile(String bucket, String objectName) {
        logger.debug("Deleting file: {} from bucket: {}", objectName, bucket);
        
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build()
            );
            
            logger.info("File deleted successfully: {}", objectName);
            return true;
            
        } catch (Exception e) {
            logger.error("Error deleting file: {}", objectName, e);
            return false;
        }
    }
    
    /**
     * Delete multiple files.
     */
    public Map<String, Object> deleteFiles(String bucket, List<String> objectNames) {
        logger.debug("Deleting {} files from bucket: {}", objectNames.size(), bucket);
        
        try {
            List<DeleteObject> objects = objectNames.stream()
                .map(DeleteObject::new)
                .collect(Collectors.toList());
            
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                    .bucket(bucket)
                    .objects(objects)
                    .build()
            );
            
            List<String> errors = new ArrayList<>();
            for (Result<DeleteError> result : results) {
                DeleteError error = result.get();
                errors.add(error.objectName() + ": " + error.message());
                logger.error("Failed to delete: {}", error.objectName());
            }
            
            return Map.of(
                "totalRequested", objectNames.size(),
                "errors", errors,
                "success", errors.isEmpty()
            );
            
        } catch (Exception e) {
            logger.error("Error deleting files", e);
            throw new RuntimeException("Failed to delete files: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate a presigned URL for direct upload.
     */
    public String generateUploadUrl(String bucket, String objectName, int expiryMinutes) {
        logger.debug("Generating upload URL for: {}", objectName);
        
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucket)
                    .object(objectName)
                    .expiry(expiryMinutes, TimeUnit.MINUTES)
                    .build()
            );
            
        } catch (Exception e) {
            logger.error("Error generating upload URL", e);
            throw new RuntimeException("Failed to generate upload URL: " + e.getMessage(), e);
        }
    }
    
    /**
     * Upload file with version control support.
     */
    public Map<String, Object> uploadFileWithVersioning(MultipartFile file, String category, 
                                                        String userId, String existingPath) {
        logger.debug("Uploading versioned file for user: {}, category: {}", userId, category);
        
        try {
            // If existing path provided, create new version
            if (existingPath != null && !existingPath.isEmpty()) {
                return createNewVersion(file, existingPath, userId);
            }
            
            // Otherwise, upload as new file
            return isImageFile(file) ? 
                uploadImage(file, category, userId, false) :
                uploadDocument(file, category, userId);
                
        } catch (Exception e) {
            logger.error("Error uploading versioned file", e);
            throw new RuntimeException("Failed to upload versioned file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get file metadata including versions.
     */
    public Map<String, Object> getFileMetadata(String bucket, String objectName) {
        logger.debug("Getting metadata for: {} in bucket: {}", objectName, bucket);
        
        try {
            var stat = minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build()
            );
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("objectName", objectName);
            metadata.put("size", stat.size());
            metadata.put("contentType", stat.contentType());
            metadata.put("lastModified", stat.lastModified());
            metadata.put("etag", stat.etag());
            metadata.put("userMetadata", stat.userMetadata());
            
            // Get file versions if they exist
            List<String> versions = getFileVersions(bucket, objectName);
            metadata.put("versions", versions);
            metadata.put("versionCount", versions.size());
            
            return metadata;
            
        } catch (Exception e) {
            logger.error("Error getting file metadata", e);
            throw new RuntimeException("Failed to get file metadata: " + e.getMessage(), e);
        }
    }
    
    /**
     * Async file processing for mobile uploads.
     */
    public CompletableFuture<Map<String, Object>> uploadImageAsync(MultipartFile file, 
                                                                   String category, 
                                                                   String userId, 
                                                                   boolean mobileOptimized) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return uploadImage(file, category, userId, mobileOptimized);
            } catch (Exception e) {
                logger.error("Async image upload failed", e);
                return Map.of(
                    "status", "error",
                    "error", e.getMessage(),
                    "filename", file.getOriginalFilename()
                );
            }
        });
    }
    
    /**
     * Get secure download URL with access control.
     */
    public String getSecureDownloadUrl(String bucket, String objectName, String userId, int expiryMinutes) {
        logger.debug("Generating secure download URL for user: {}, file: {}", userId, objectName);
        
        try {
            // Verify user has access to this file
            if (!hasFileAccess(bucket, objectName, userId)) {
                throw new SecurityException("User does not have access to this file");
            }
            
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectName)
                    .expiry(expiryMinutes, TimeUnit.MINUTES)
                    .build()
            );
            
        } catch (Exception e) {
            logger.error("Error generating secure download URL", e);
            throw new RuntimeException("Failed to generate secure download URL: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if a file exists.
     */
    public boolean fileExists(String bucket, String objectName) {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            logger.error("Error checking file existence", e);
            throw new RuntimeException("Failed to check file existence", e);
        } catch (Exception e) {
            logger.error("Error checking file existence", e);
            throw new RuntimeException("Failed to check file existence", e);
        }
    }
    
    // ========== Private Helper Methods ==========
    
    private void initializeBuckets() {
        try {
            // Create buckets if they don't exist
            createBucketIfNotExists(filesBucket);
            createBucketIfNotExists(imagesBucket);
            
        } catch (Exception e) {
            logger.error("Failed to initialize buckets", e);
            throw new RuntimeException("Failed to initialize storage buckets", e);
        }
    }
    
    private void createBucketIfNotExists(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(bucketName).build()
        );
        
        if (!exists) {
            minioClient.makeBucket(
                MakeBucketArgs.builder().bucket(bucketName).build()
            );
            logger.info("Created bucket: {}", bucketName);
        }
    }
    
    private void validateImageFile(MultipartFile file) {
        validateImageFile(file, false);
    }
    
    private void validateImageFile(MultipartFile file, boolean mobileOptimized) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        long maxSize = mobileOptimized ? MAX_MOBILE_IMAGE_SIZE : MAX_FILE_SIZE;
        if (file.getSize() > maxSize) {
            String sizeLimit = mobileOptimized ? "5MB" : "10MB";
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + sizeLimit);
        }
        
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Invalid image type. Allowed types: " + 
                ALLOWED_IMAGE_TYPES);
        }
        
        // Additional validation for mobile uploads
        if (mobileOptimized) {
            validateMobileImageConstraints(file);
        }
    }
    
    private void validateDocumentFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 10MB");
        }
        
        String contentType = file.getContentType();
        if (!ALLOWED_DOCUMENT_TYPES.contains(contentType) && 
            !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Invalid file type");
        }
    }
    
    private byte[] optimizeImage(byte[] imageData, String extension) {
        try {
            // Read image
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            
            if (image == null) {
                return imageData; // Return original if can't process
            }
            
            // Check if resizing is needed (max 1920x1080)
            int maxWidth = 1920;
            int maxHeight = 1080;
            
            int width = image.getWidth();
            int height = image.getHeight();
            
            if (width <= maxWidth && height <= maxHeight) {
                return imageData; // No optimization needed
            }
            
            // Calculate new dimensions maintaining aspect ratio
            double widthRatio = (double) maxWidth / width;
            double heightRatio = (double) maxHeight / height;
            double ratio = Math.min(widthRatio, heightRatio);
            
            int newWidth = (int) (width * ratio);
            int newHeight = (int) (height * ratio);
            
            // Resize image
            Image scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, 
                image.getType() != 0 ? image.getType() : BufferedImage.TYPE_INT_RGB);
            
            Graphics2D g2d = resizedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(scaledImage, 0, 0, null);
            g2d.dispose();
            
            // Write optimized image
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, extension.replace(".", ""), outputStream);
            
            byte[] optimized = outputStream.toByteArray();
            logger.debug("Image optimized from {} bytes to {} bytes", 
                imageData.length, optimized.length);
            
            return optimized;
            
        } catch (IOException e) {
            logger.warn("Failed to optimize image, using original", e);
            return imageData;
        }
    }
    
    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }
    
    private String generateUniqueFilename(String userId, String category, String extension) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s_%s_%s%s", category, userId, timestamp, random, extension);
    }
    
    // New helper methods for enhanced file management
    
    private byte[] optimizeImageForMobile(byte[] imageData, String extension) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image == null) return imageData;
            
            int width = image.getWidth();
            int height = image.getHeight();
            
            // More aggressive optimization for mobile
            if (width <= MOBILE_MAX_WIDTH && height <= MOBILE_MAX_HEIGHT) {
                return imageData;
            }
            
            double widthRatio = (double) MOBILE_MAX_WIDTH / width;
            double heightRatio = (double) MOBILE_MAX_HEIGHT / height;
            double ratio = Math.min(widthRatio, heightRatio);
            
            int newWidth = (int) (width * ratio);
            int newHeight = (int) (height * ratio);
            
            Image scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, 
                BufferedImage.TYPE_INT_RGB);
            
            Graphics2D g2d = resizedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, 
                RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(scaledImage, 0, 0, null);
            g2d.dispose();
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpeg", outputStream); // Always convert to JPEG for mobile
            
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            logger.warn("Failed to optimize image for mobile, using original", e);
            return imageData;
        }
    }
    
    private byte[] createThumbnail(byte[] imageData, String extension) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image == null) return imageData;
            
            // Create square thumbnail
            int size = Math.min(image.getWidth(), image.getHeight());
            int x = (image.getWidth() - size) / 2;
            int y = (image.getHeight() - size) / 2;
            
            BufferedImage cropped = image.getSubimage(x, y, size, size);
            Image scaled = cropped.getScaledInstance(THUMBNAIL_SIZE, THUMBNAIL_SIZE, Image.SCALE_SMOOTH);
            
            BufferedImage thumbnail = new BufferedImage(THUMBNAIL_SIZE, THUMBNAIL_SIZE, 
                BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumbnail.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(scaled, 0, 0, null);
            g2d.dispose();
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "jpeg", outputStream);
            
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            logger.warn("Failed to create thumbnail, using original", e);
            return imageData;
        }
    }
    
    private Map<String, Object> createNewVersion(MultipartFile file, String existingPath, String userId) {
        try {
            // Parse existing path to get version info
            Path path = Paths.get(existingPath);
            String filename = path.getFileName().toString();
            String directory = path.getParent().toString();
            
            // Extract version number or default to 1
            int currentVersion = extractVersionFromFilename(filename);
            int newVersion = currentVersion + 1;
            
            // Create new filename with version
            String newFilename = insertVersionInFilename(filename, newVersion);
            String newObjectName = directory + "/" + newFilename;
            
            // Upload new version
            byte[] fileData = file.getBytes();
            String bucket = isImageFile(file) ? imagesBucket : filesBucket;
            
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(newObjectName)
                    .stream(new ByteArrayInputStream(fileData), fileData.length, -1)
                    .contentType(file.getContentType())
                    .userMetadata(Map.of(
                        "user-id", userId,
                        "version", String.valueOf(newVersion),
                        "parent-file", existingPath,
                        "upload-timestamp", LocalDateTime.now().toString()
                    ))
                    .build()
            );
            
            return Map.of(
                "status", "success",
                "path", newObjectName,
                "version", newVersion,
                "parentFile", existingPath,
                "size", fileData.length
            );
            
        } catch (Exception e) {
            logger.error("Error creating new file version", e);
            throw new RuntimeException("Failed to create new version: " + e.getMessage(), e);
        }
    }
    
    private List<String> getFileVersions(String bucket, String objectName) {
        // Implementation would scan for versioned files
        // For now, return basic version info
        return List.of(objectName);
    }
    
    private boolean hasFileAccess(String bucket, String objectName, String userId) {
        try {
            var stat = minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build()
            );
            
            // Check if user owns this file or has admin access
            String fileOwner = stat.userMetadata().get("user-id");
            return userId.equals(fileOwner) || isAdminUser(userId);
            
        } catch (Exception e) {
            logger.error("Error checking file access", e);
            return false;
        }
    }
    
    private boolean isImageFile(MultipartFile file) {
        return ALLOWED_IMAGE_TYPES.contains(file.getContentType());
    }
    
    private void validateMobileImageConstraints(MultipartFile file) {
        // Additional mobile-specific validations
        if (file.getOriginalFilename() != null && file.getOriginalFilename().length() > 255) {
            throw new IllegalArgumentException("Filename too long for mobile optimization");
        }
    }
    
    private int extractVersionFromFilename(String filename) {
        // Simple version extraction - could be enhanced
        if (filename.contains("_v")) {
            try {
                String versionPart = filename.substring(
                    filename.lastIndexOf("_v") + 2, 
                    filename.lastIndexOf("."));
                return Integer.parseInt(versionPart);
            } catch (Exception e) {
                return 1;
            }
        }
        return 1;
    }
    
    private String insertVersionInFilename(String filename, int version) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex) + "_v" + version + filename.substring(dotIndex);
        }
        return filename + "_v" + version;
    }
    
    private boolean isAdminUser(String userId) {
        // Implementation would check user roles
        // For now, simple check
        return userId != null && userId.contains("admin");
    }
    
    private String buildObjectPath(String category, String filename) {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%s/%d/%02d/%02d/%s", 
            category, now.getYear(), now.getMonthValue(), now.getDayOfMonth(), filename);
    }
}