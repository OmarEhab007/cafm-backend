package com.cafm.cafmbackend.dto.file;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for file upload operations.
 */
public record FileUploadResponse(
    UUID fileId,
    String fileName,
    String fileUrl,
    String thumbnailUrl,
    String contentType,
    Long fileSize,
    String fileType,
    
    String entityType,
    UUID entityId,
    
    String category,
    String description,
    
    Map<String, String> metadata,
    
    Boolean isPublic,
    Boolean optimized,
    Boolean virusScanned,
    Boolean virusScanPassed,
    
    String uploadedBy,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime uploadedAt,
    
    FileMetrics metrics
) {
    /**
     * File metrics and information.
     */
    public record FileMetrics(
        String humanReadableSize,
        Integer width,
        Integer height,
        String format,
        Integer compressionRatio,
        Long originalSize,
        Long optimizedSize
    ) {
        public static FileMetrics create(Long fileSize, Long originalSize) {
            String humanReadableSize = formatFileSize(fileSize);
            
            Integer compressionRatio = null;
            if (originalSize != null && originalSize > 0) {
                compressionRatio = (int) ((1.0 - (double) fileSize / originalSize) * 100);
            }
            
            return new FileMetrics(
                humanReadableSize,
                null,
                null,
                null,
                compressionRatio,
                originalSize,
                fileSize
            );
        }
        
        public static FileMetrics forImage(
                Long fileSize,
                Long originalSize,
                Integer width,
                Integer height,
                String format) {
            
            String humanReadableSize = formatFileSize(fileSize);
            
            Integer compressionRatio = null;
            if (originalSize != null && originalSize > 0) {
                compressionRatio = (int) ((1.0 - (double) fileSize / originalSize) * 100);
            }
            
            return new FileMetrics(
                humanReadableSize,
                width,
                height,
                format,
                compressionRatio,
                originalSize,
                fileSize
            );
        }
        
        private static String formatFileSize(Long bytes) {
            if (bytes == null) return "Unknown";
            
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}