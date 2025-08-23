package com.cafm.cafmbackend.data.entity;

import com.cafm.cafmbackend.data.entity.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Work Order Attachment entity for storing files related to work orders.
 * Supports before/after photos, documents, invoices, and reports.
 */
@Entity
@Table(name = "work_order_attachments")
public class WorkOrderAttachment extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    @NotNull(message = "Work order is required")
    private WorkOrder workOrder;
    
    @Column(name = "file_name", nullable = false, length = 255)
    @NotBlank(message = "File name is required")
    @Size(max = 255, message = "File name cannot exceed 255 characters")
    private String fileName;
    
    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "File URL is required")
    private String fileUrl;
    
    @Column(name = "file_type", length = 50)
    @Size(max = 50, message = "File type cannot exceed 50 characters")
    private String fileType;
    
    @Column(name = "file_size")
    @Min(value = 0, message = "File size cannot be negative")
    private Long fileSize;
    
    @Column(name = "attachment_type", length = 50)
    @Size(max = 50, message = "Attachment type cannot exceed 50 characters")
    private String attachmentType; // 'before', 'during', 'after', 'invoice', 'report', 'other'
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;
    
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
    
    // ========== Constructors ==========
    
    public WorkOrderAttachment() {
        super();
        this.uploadedAt = LocalDateTime.now();
    }
    
    public WorkOrderAttachment(WorkOrder workOrder, String fileName, String fileUrl, String attachmentType) {
        this();
        this.workOrder = workOrder;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.attachmentType = attachmentType;
    }
    
    public WorkOrderAttachment(WorkOrder workOrder, String fileName, String fileUrl, 
                              String attachmentType, User uploadedBy) {
        this(workOrder, fileName, fileUrl, attachmentType);
        this.uploadedBy = uploadedBy;
    }
    
    // ========== Business Methods ==========
    
    /**
     * Check if attachment is an image
     */
    public boolean isImage() {
        if (fileType == null) {
            return fileName != null && (
                fileName.toLowerCase().endsWith(".jpg") ||
                fileName.toLowerCase().endsWith(".jpeg") ||
                fileName.toLowerCase().endsWith(".png") ||
                fileName.toLowerCase().endsWith(".gif") ||
                fileName.toLowerCase().endsWith(".webp")
            );
        }
        return fileType.toLowerCase().startsWith("image/");
    }
    
    /**
     * Check if attachment is a PDF
     */
    public boolean isPDF() {
        if (fileType == null) {
            return fileName != null && fileName.toLowerCase().endsWith(".pdf");
        }
        return "application/pdf".equals(fileType.toLowerCase());
    }
    
    /**
     * Check if attachment is a document
     */
    public boolean isDocument() {
        if (fileType == null) {
            return fileName != null && (
                fileName.toLowerCase().endsWith(".pdf") ||
                fileName.toLowerCase().endsWith(".doc") ||
                fileName.toLowerCase().endsWith(".docx") ||
                fileName.toLowerCase().endsWith(".xls") ||
                fileName.toLowerCase().endsWith(".xlsx")
            );
        }
        return fileType.contains("document") || 
               fileType.contains("pdf") || 
               fileType.contains("sheet") ||
               fileType.contains("word");
    }
    
    /**
     * Check if this is a before photo
     */
    public boolean isBeforePhoto() {
        return "before".equalsIgnoreCase(attachmentType) && isImage();
    }
    
    /**
     * Check if this is an after photo
     */
    public boolean isAfterPhoto() {
        return "after".equalsIgnoreCase(attachmentType) && isImage();
    }
    
    /**
     * Check if this is an invoice
     */
    public boolean isInvoice() {
        return "invoice".equalsIgnoreCase(attachmentType);
    }
    
    /**
     * Check if this is a report
     */
    public boolean isReport() {
        return "report".equalsIgnoreCase(attachmentType);
    }
    
    /**
     * Get file extension
     */
    public String getFileExtension() {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * Get formatted file size
     */
    public String getFormattedFileSize() {
        if (fileSize == null) {
            return "Unknown";
        }
        
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Validate file size (max 50MB)
     */
    public boolean isValidFileSize() {
        return fileSize == null || fileSize <= 50 * 1024 * 1024; // 50MB max
    }
    
    /**
     * Get attachment type display name
     */
    public String getAttachmentTypeDisplayName() {
        if (attachmentType == null) {
            return "Other";
        }
        
        switch (attachmentType.toLowerCase()) {
            case "before":
                return "Before Photo";
            case "during":
                return "During Work";
            case "after":
                return "After Photo";
            case "invoice":
                return "Invoice";
            case "report":
                return "Report";
            default:
                return attachmentType.substring(0, 1).toUpperCase() + attachmentType.substring(1);
        }
    }
    
    /**
     * Set file type from file name if not provided
     */
    public void inferFileType() {
        if (fileType != null && !fileType.trim().isEmpty()) {
            return;
        }
        
        String ext = getFileExtension();
        switch (ext) {
            case "jpg":
            case "jpeg":
                fileType = "image/jpeg";
                break;
            case "png":
                fileType = "image/png";
                break;
            case "gif":
                fileType = "image/gif";
                break;
            case "pdf":
                fileType = "application/pdf";
                break;
            case "doc":
                fileType = "application/msword";
                break;
            case "docx":
                fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                break;
            case "xls":
                fileType = "application/vnd.ms-excel";
                break;
            case "xlsx":
                fileType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                break;
            default:
                fileType = "application/octet-stream";
        }
    }
    
    // ========== Getters and Setters ==========
    
    public WorkOrder getWorkOrder() {
        return workOrder;
    }
    
    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFileUrl() {
        return fileUrl;
    }
    
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getAttachmentType() {
        return attachmentType;
    }
    
    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public User getUploadedBy() {
        return uploadedBy;
    }
    
    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    // ========== toString, equals, hashCode ==========
    
    @Override
    public String toString() {
        return String.format("WorkOrderAttachment[id=%s, workOrder=%s, fileName=%s, type=%s, size=%s]",
            getId(),
            workOrder != null ? workOrder.getWorkOrderNumber() : "null",
            fileName,
            attachmentType,
            getFormattedFileSize());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkOrderAttachment)) return false;
        if (!super.equals(o)) return false;
        WorkOrderAttachment that = (WorkOrderAttachment) o;
        return Objects.equals(workOrder, that.workOrder) &&
               Objects.equals(fileName, that.fileName) &&
               Objects.equals(fileUrl, that.fileUrl);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), workOrder, fileName, fileUrl);
    }
}