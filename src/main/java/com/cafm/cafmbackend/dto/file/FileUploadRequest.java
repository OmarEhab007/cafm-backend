package com.cafm.cafmbackend.dto.file;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for file upload operations.
 */
public record FileUploadRequest(
    @NotNull(message = "File name is required")
    @Size(min = 1, max = 255, message = "File name must be between 1 and 255 characters")
    String fileName,
    
    @NotNull(message = "File type is required")
    @Pattern(regexp = "^(image|document|video|audio|other)$", 
             message = "File type must be one of: image, document, video, audio, other")
    String fileType,
    
    @NotNull(message = "Content type is required")
    @Pattern(regexp = "^[a-z]+/[a-z0-9\\-+.]+$", 
             message = "Invalid content type format")
    String contentType,
    
    Long fileSize,
    
    String entityType,
    UUID entityId,
    
    String category,
    String description,
    
    Map<String, String> metadata,
    
    Boolean isPublic,
    Boolean requiresOptimization,
    Boolean requiresVirusScan
) {
    /**
     * Creates a basic file upload request.
     */
    public static FileUploadRequest basic(
            String fileName,
            String contentType) {
        return new FileUploadRequest(
            fileName,
            determineFileType(contentType),
            contentType,
            null,
            null,
            null,
            null,
            null,
            Map.of(),
            false,
            shouldOptimize(contentType),
            true
        );
    }
    
    /**
     * Creates an image upload request with optimization.
     */
    public static FileUploadRequest image(
            String fileName,
            String entityType,
            UUID entityId) {
        return new FileUploadRequest(
            fileName,
            "image",
            determineImageContentType(fileName),
            null,
            entityType,
            entityId,
            "photo",
            null,
            Map.of(),
            false,
            true,
            true
        );
    }
    
    /**
     * Creates a document upload request.
     */
    public static FileUploadRequest document(
            String fileName,
            String contentType,
            String category) {
        return new FileUploadRequest(
            fileName,
            "document",
            contentType,
            null,
            null,
            null,
            category,
            null,
            Map.of(),
            false,
            false,
            true
        );
    }
    
    private static String determineFileType(String contentType) {
        if (contentType.startsWith("image/")) return "image";
        if (contentType.startsWith("video/")) return "video";
        if (contentType.startsWith("audio/")) return "audio";
        if (contentType.contains("pdf") || 
            contentType.contains("document") ||
            contentType.contains("msword") ||
            contentType.contains("spreadsheet") ||
            contentType.contains("presentation")) {
            return "document";
        }
        return "other";
    }
    
    private static String determineImageContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            default -> "image/jpeg";
        };
    }
    
    private static boolean shouldOptimize(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
}