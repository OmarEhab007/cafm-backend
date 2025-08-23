package com.cafm.cafmbackend.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Test controller for file upload functionality.
 * 
 * Purpose: Simple test endpoint to verify multipart file upload
 * Pattern: REST controller for testing
 * Java 23: Uses modern patterns
 * Architecture: Controller layer for testing
 * Standards: Only available in dev/test profiles
 */
@RestController
@RequestMapping("/api/v1/test")
@Tag(name = "Test File Upload", description = "File upload testing endpoints (dev/test only)")
@Profile({"dev", "test", "local"})
public class TestFileUploadController {
    
    @Operation(summary = "Test file upload", description = "Simple endpoint to test multipart file upload")
    @PostMapping(value = "/upload-test", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testUpload(
            @RequestParam("testFile") MultipartFile file) {
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "File received",
            "fileName", file.getOriginalFilename(),
            "size", file.getSize(),
            "contentType", file.getContentType()
        ));
    }
    
    @Operation(summary = "Test simple upload", description = "Simplest possible file upload test")
    @PostMapping(value = "/simple-upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> simpleUpload(
            @RequestBody(required = false) MultipartFile simpleFile) {
        
        if (simpleFile == null) {
            return ResponseEntity.ok(Map.of(
                "status", "no-file",
                "message", "No file received in request body"
            ));
        }
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "File received via request body",
            "fileName", simpleFile.getOriginalFilename(),
            "size", simpleFile.getSize()
        ));
    }
}