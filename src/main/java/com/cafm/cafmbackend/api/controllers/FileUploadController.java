package com.cafm.cafmbackend.api.controllers;

import com.cafm.cafmbackend.dto.file.FileUploadRequest;
import com.cafm.cafmbackend.dto.file.FileUploadResponse;
import com.cafm.cafmbackend.application.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for file upload and management operations.
 * 
 * Purpose: Provides secure file upload, download, and management endpoints
 * with proper authorization and validation.
 * 
 * Pattern: RESTful controller with comprehensive OpenAPI documentation
 * Java 23: Modern controller patterns with validation
 * Architecture: API layer with security annotations
 * Standards: Follows REST conventions and security best practices
 */
@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "File Management", description = "File upload, download, and management operations")
@SecurityRequirement(name = "Bearer Authentication")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @Operation(
        summary = "Upload a single file",
        description = "Upload a file with automatic optimization, virus scanning, and metadata tracking",
        responses = {
            @ApiResponse(responseCode = "201", description = "File uploaded successfully",
                content = @Content(schema = @Schema(implementation = FileUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or request"),
            @ApiResponse(responseCode = "413", description = "File too large"),
            @ApiResponse(responseCode = "415", description = "Unsupported file type"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @Parameter(description = "File to upload", required = true)
            @RequestParam("file") MultipartFile file,
            
            @Parameter(description = "File name")
            @RequestParam(value = "fileName", required = false) String fileName,
            
            @Parameter(description = "File type (image, document, video, audio, other)")
            @RequestParam(value = "fileType", required = false) String fileType,
            
            @Parameter(description = "Entity type for association")
            @RequestParam(value = "entityType", required = false) String entityType,
            
            @Parameter(description = "Entity ID for association")
            @RequestParam(value = "entityId", required = false) UUID entityId,
            
            @Parameter(description = "File category")
            @RequestParam(value = "category", required = false) String category,
            
            @Parameter(description = "File description")
            @RequestParam(value = "description", required = false) String description,
            
            @Parameter(description = "Is file public")
            @RequestParam(value = "isPublic", defaultValue = "false") Boolean isPublic,
            
            @Parameter(description = "Require optimization for images")
            @RequestParam(value = "requiresOptimization", defaultValue = "true") Boolean requiresOptimization,
            
            @Parameter(description = "Require virus scan")
            @RequestParam(value = "requiresVirusScan", defaultValue = "true") Boolean requiresVirusScan) {
        
        // Use provided fileName or fallback to original filename
        String finalFileName = fileName != null ? fileName : file.getOriginalFilename();
        
        // Determine file type if not provided
        String finalFileType = fileType;
        if (finalFileType == null) {
            String contentType = file.getContentType();
            if (contentType != null) {
                if (contentType.startsWith("image/")) finalFileType = "image";
                else if (contentType.startsWith("video/")) finalFileType = "video";
                else if (contentType.startsWith("audio/")) finalFileType = "audio";
                else if (contentType.contains("pdf") || contentType.contains("document")) finalFileType = "document";
                else finalFileType = "other";
            } else {
                finalFileType = "other";
            }
        }
        
        FileUploadRequest request = new FileUploadRequest(
            finalFileName,
            finalFileType,
            file.getContentType(),
            file.getSize(),
            entityType,
            entityId,
            category,
            description,
            Map.of(),
            isPublic,
            requiresOptimization,
            requiresVirusScan
        );
        
        FileUploadResponse response = fileUploadService.uploadFile(file, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Upload multiple files",
        description = "Upload multiple files in a single request with batch processing",
        responses = {
            @ApiResponse(responseCode = "201", description = "Files uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid files or request"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping(value = "/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<List<FileUploadResponse>> uploadFiles(
            @Parameter(description = "Files to upload", required = true)
            @RequestParam("files") List<MultipartFile> files,
            
            @Parameter(description = "Entity type for association")
            @RequestParam(value = "entityType", required = false) String entityType,
            
            @Parameter(description = "Entity ID for association")
            @RequestParam(value = "entityId", required = false) UUID entityId,
            
            @Parameter(description = "Category for all files")
            @RequestParam(value = "category", required = false) String category) {
        
        List<FileUploadRequest> requests = files.stream()
            .map(file -> FileUploadRequest.basic(
                file.getOriginalFilename(),
                file.getContentType()
            ))
            .toList();
        
        List<FileUploadResponse> responses = fileUploadService.uploadFiles(files, requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @Operation(
        summary = "Get file metadata",
        description = "Retrieve file metadata and information",
        responses = {
            @ApiResponse(responseCode = "200", description = "File metadata retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @GetMapping("/{fileId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<FileUploadResponse> getFile(
            @Parameter(description = "File ID", required = true)
            @PathVariable UUID fileId) {
        
        FileUploadResponse response = fileUploadService.getFile(fileId);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get files by entity",
        description = "Retrieve all files associated with a specific entity",
        responses = {
            @ApiResponse(responseCode = "200", description = "Files retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<Page<FileUploadResponse>> getFilesByEntity(
            @Parameter(description = "Entity type", required = true)
            @PathVariable String entityType,
            
            @Parameter(description = "Entity ID", required = true)
            @PathVariable UUID entityId,
            
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<FileUploadResponse> response = fileUploadService.getFilesByEntity(entityType, entityId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get file download URL",
        description = "Generate a secure temporary download URL for a file",
        responses = {
            @ApiResponse(responseCode = "200", description = "Download URL generated successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @GetMapping("/{fileId}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @Parameter(description = "File ID", required = true)
            @PathVariable UUID fileId,
            
            @Parameter(description = "URL expiration time in minutes (default: 60)")
            @RequestParam(value = "expirationMinutes", defaultValue = "60") int expirationMinutes) {
        
        String downloadUrl = fileUploadService.getDownloadUrl(fileId, expirationMinutes);
        return ResponseEntity.ok(Map.of(
            "downloadUrl", downloadUrl,
            "expiresInMinutes", String.valueOf(expirationMinutes)
        ));
    }

    @Operation(
        summary = "Delete a file",
        description = "Delete a file and its metadata",
        responses = {
            @ApiResponse(responseCode = "204", description = "File deleted successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @DeleteMapping("/{fileId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "File ID", required = true)
            @PathVariable UUID fileId) {
        
        fileUploadService.deleteFile(fileId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Get file statistics",
        description = "Get comprehensive file storage statistics for the current tenant",
        responses = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<Map<String, Object>> getFileStatistics() {
        Map<String, Object> statistics = fileUploadService.getFileStatistics();
        return ResponseEntity.ok(statistics);
    }
}