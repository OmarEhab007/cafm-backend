package com.cafm.cafmbackend.application.service;

import com.cafm.cafmbackend.infrastructure.persistence.entity.Report;
import com.cafm.cafmbackend.infrastructure.persistence.entity.WorkOrder;
import com.cafm.cafmbackend.infrastructure.persistence.entity.Asset;
import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import com.cafm.cafmbackend.infrastructure.persistence.repository.ReportRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.WorkOrderRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.AssetRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.UserRepository;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for generating reports in various formats (Excel, PDF).
 * 
 * Purpose: Provide report generation functionality for maintenance data export
 * Pattern: Service layer with format-specific generation methods
 * Java 23: Uses modern exception handling and streaming operations
 * Architecture: Domain service for business logic with async processing
 * Standards: Excel via Apache POI, PDF via iText, async operations for performance
 */
@Service
@Transactional(readOnly = true)
public class ReportGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final ReportRepository reportRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public ReportGenerationService(
            ReportRepository reportRepository,
            WorkOrderRepository workOrderRepository,
            AssetRepository assetRepository,
            UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.workOrderRepository = workOrderRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Generate maintenance reports Excel file asynchronously.
     */
    public CompletableFuture<byte[]> generateMaintenanceReportsExcel(UUID companyId, 
                                                                      LocalDate startDate, 
                                                                      LocalDate endDate) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Generating maintenance reports Excel for company: {} from {} to {}", 
                       companyId, startDate, endDate);
            
            try (XSSFWorkbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                
                // Create main reports sheet
                Sheet reportsSheet = workbook.createSheet("Maintenance Reports");
                createMaintenanceReportsExcelSheet(reportsSheet, companyId, startDate, endDate);
                
                // Create summary sheet
                Sheet summarySheet = workbook.createSheet("Summary");
                createReportsSummarySheet(summarySheet, companyId, startDate, endDate);
                
                workbook.write(outputStream);
                logger.info("Successfully generated maintenance reports Excel for company: {}", companyId);
                return outputStream.toByteArray();
                
            } catch (IOException e) {
                logger.error("Error generating maintenance reports Excel", e);
                throw new RuntimeException("Failed to generate Excel report", e);
            }
        });
    }
    
    /**
     * Generate work orders Excel file asynchronously.
     */
    public CompletableFuture<byte[]> generateWorkOrdersExcel(UUID companyId, 
                                                             LocalDate startDate, 
                                                             LocalDate endDate) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Generating work orders Excel for company: {} from {} to {}", 
                       companyId, startDate, endDate);
            
            try (XSSFWorkbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                
                // Create main work orders sheet
                Sheet workOrdersSheet = workbook.createSheet("Work Orders");
                createWorkOrdersExcelSheet(workOrdersSheet, companyId, startDate, endDate);
                
                // Create status summary sheet
                Sheet statusSheet = workbook.createSheet("Status Summary");
                createWorkOrderStatusSummarySheet(statusSheet, companyId, startDate, endDate);
                
                workbook.write(outputStream);
                logger.info("Successfully generated work orders Excel for company: {}", companyId);
                return outputStream.toByteArray();
                
            } catch (IOException e) {
                logger.error("Error generating work orders Excel", e);
                throw new RuntimeException("Failed to generate Excel report", e);
            }
        });
    }
    
    /**
     * Generate assets inventory Excel file asynchronously.
     */
    public CompletableFuture<byte[]> generateAssetsInventoryExcel(UUID companyId) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Generating assets inventory Excel for company: {}", companyId);
            
            try (XSSFWorkbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                
                Sheet assetsSheet = workbook.createSheet("Assets Inventory");
                createAssetsInventoryExcelSheet(assetsSheet, companyId);
                
                workbook.write(outputStream);
                logger.info("Successfully generated assets inventory Excel for company: {}", companyId);
                return outputStream.toByteArray();
                
            } catch (IOException e) {
                logger.error("Error generating assets inventory Excel", e);
                throw new RuntimeException("Failed to generate Excel report", e);
            }
        });
    }
    
    /**
     * Generate maintenance reports PDF asynchronously.
     */
    public CompletableFuture<byte[]> generateMaintenanceReportsPDF(UUID companyId, 
                                                                    LocalDate startDate, 
                                                                    LocalDate endDate) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Generating maintenance reports PDF for company: {} from {} to {}", 
                       companyId, startDate, endDate);
            
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                PdfWriter writer = new PdfWriter(outputStream);
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc);
                
                // Generate HTML content and convert to PDF
                String htmlContent = generateMaintenanceReportsHTML(companyId, startDate, endDate);
                HtmlConverter.convertToPdf(htmlContent, outputStream);
                
                document.close();
                logger.info("Successfully generated maintenance reports PDF for company: {}", companyId);
                return outputStream.toByteArray();
                
            } catch (Exception e) {
                logger.error("Error generating maintenance reports PDF", e);
                throw new RuntimeException("Failed to generate PDF report", e);
            }
        });
    }
    
    /**
     * Generate work orders PDF asynchronously.
     */
    public CompletableFuture<byte[]> generateWorkOrdersPDF(UUID companyId, 
                                                           LocalDate startDate, 
                                                           LocalDate endDate) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Generating work orders PDF for company: {} from {} to {}", 
                       companyId, startDate, endDate);
            
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                PdfWriter writer = new PdfWriter(outputStream);
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc);
                
                // Generate HTML content and convert to PDF
                String htmlContent = generateWorkOrdersHTML(companyId, startDate, endDate);
                HtmlConverter.convertToPdf(htmlContent, outputStream);
                
                document.close();
                logger.info("Successfully generated work orders PDF for company: {}", companyId);
                return outputStream.toByteArray();
                
            } catch (Exception e) {
                logger.error("Error generating work orders PDF", e);
                throw new RuntimeException("Failed to generate PDF report", e);
            }
        });
    }
    
    // ========== Private Helper Methods for Excel Generation ==========
    
    private void createMaintenanceReportsExcelSheet(Sheet sheet, UUID companyId, 
                                                   LocalDate startDate, LocalDate endDate) {
        // Create header row with styling
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        
        String[] headers = {
            "Report ID", "Title", "Description", "Priority", "Status", 
            "Asset Name", "Asset Location", "Reported By", "Assigned To", 
            "Created Date", "Due Date", "Completed Date"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Fetch reports data
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        List<Report> reports = reportRepository.findReportsBetweenDates(
            startDateTime.toLocalDate(), endDateTime.toLocalDate());
        
        // Populate data rows
        int rowNum = 1;
        CellStyle dateStyle = createDateStyle(sheet.getWorkbook());
        
        for (Report report : reports) {
            Row dataRow = sheet.createRow(rowNum++);
            
            dataRow.createCell(0).setCellValue(report.getId().toString());
            dataRow.createCell(1).setCellValue(report.getTitle());
            dataRow.createCell(2).setCellValue(report.getDescription());
            dataRow.createCell(3).setCellValue(report.getPriority() != null ? 
                                             report.getPriority().toString() : "");
            dataRow.createCell(4).setCellValue(report.getStatus() != null ? 
                                             report.getStatus().toString() : "");
            dataRow.createCell(5).setCellValue(report.getSchool() != null ? 
                                             report.getSchool().getName() : "");
            dataRow.createCell(6).setCellValue(report.getSchool() != null ? 
                                             report.getSchool().getAddress() : "");
            dataRow.createCell(7).setCellValue(report.getSupervisor() != null ? 
                                             report.getSupervisor().getFullName() : "");
            dataRow.createCell(8).setCellValue(report.getAssignedTo() != null ? 
                                             report.getAssignedTo().getFullName() : "");
            
            // Date cells with formatting
            Cell createdCell = dataRow.createCell(9);
            if (report.getCreatedAt() != null) {
                createdCell.setCellValue(report.getCreatedAt().format(DATETIME_FORMATTER));
                createdCell.setCellStyle(dateStyle);
            }
            
            Cell dueCell = dataRow.createCell(10);
            if (report.getScheduledDate() != null) {
                dueCell.setCellValue(report.getScheduledDate().format(DATE_FORMATTER));
                dueCell.setCellStyle(dateStyle);
            }
            
            Cell completedCell = dataRow.createCell(11);
            if (report.getCompletedAt() != null) {
                completedCell.setCellValue(report.getCompletedAt().format(DATETIME_FORMATTER));
                completedCell.setCellStyle(dateStyle);
            }
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createWorkOrdersExcelSheet(Sheet sheet, UUID companyId, 
                                          LocalDate startDate, LocalDate endDate) {
        // Create header row
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        
        String[] headers = {
            "Work Order ID", "Title", "Description", "Status", "Priority",
            "Asset Name", "Asset Location", "Assigned To", "Supervisor",
            "Created Date", "Due Date", "Completed Date", "Estimated Hours", "Actual Hours"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Fetch work orders data
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        List<WorkOrder> workOrders = workOrderRepository.findByCompany_IdAndScheduledDateRange(
            companyId, startDateTime, endDateTime);
        
        // Populate data rows
        int rowNum = 1;
        CellStyle dateStyle = createDateStyle(sheet.getWorkbook());
        
        for (WorkOrder workOrder : workOrders) {
            Row dataRow = sheet.createRow(rowNum++);
            
            dataRow.createCell(0).setCellValue(workOrder.getId().toString());
            dataRow.createCell(1).setCellValue(workOrder.getTitle());
            dataRow.createCell(2).setCellValue(workOrder.getDescription());
            dataRow.createCell(3).setCellValue(workOrder.getStatus() != null ? 
                                             workOrder.getStatus().toString() : "");
            dataRow.createCell(4).setCellValue(workOrder.getPriority() != null ? 
                                             workOrder.getPriority().toString() : "");
            dataRow.createCell(5).setCellValue(workOrder.getSchool() != null ? 
                                             workOrder.getSchool().getName() : "");
            dataRow.createCell(6).setCellValue(workOrder.getSchool() != null ? 
                                             workOrder.getSchool().getAddress() : "");
            dataRow.createCell(7).setCellValue(workOrder.getAssignedTo() != null ? 
                                             workOrder.getAssignedTo().getFullName() : "");
            dataRow.createCell(8).setCellValue(workOrder.getAssignedBy() != null ? 
                                             workOrder.getAssignedBy().getFullName() : "");
            
            // Date cells
            Cell createdCell = dataRow.createCell(9);
            if (workOrder.getCreatedAt() != null) {
                createdCell.setCellValue(workOrder.getCreatedAt().format(DATETIME_FORMATTER));
                createdCell.setCellStyle(dateStyle);
            }
            
            Cell dueCell = dataRow.createCell(10);
            if (workOrder.getScheduledStart() != null) {
                dueCell.setCellValue(workOrder.getScheduledStart().format(DATE_FORMATTER));
                dueCell.setCellStyle(dateStyle);
            }
            
            Cell completedCell = dataRow.createCell(11);
            if (workOrder.getActualEnd() != null) {
                completedCell.setCellValue(workOrder.getActualEnd().format(DATETIME_FORMATTER));
                completedCell.setCellStyle(dateStyle);
            }
            
            // Numeric cells
            dataRow.createCell(12).setCellValue(workOrder.getEstimatedHours() != null ? 
                                              workOrder.getEstimatedHours().doubleValue() : 0);
            dataRow.createCell(13).setCellValue(workOrder.getActualHours() != null ? 
                                              workOrder.getActualHours().doubleValue() : 0);
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createAssetsInventoryExcelSheet(Sheet sheet, UUID companyId) {
        // Create header row
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        
        String[] headers = {
            "Asset ID", "Name", "Description", "Category", "Location", 
            "Status", "Purchase Date", "Purchase Cost", "Warranty Expiry",
            "Maintenance Schedule", "Last Maintenance", "Next Maintenance"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Fetch assets data
        List<Asset> assets = assetRepository.findByCompany_IdAndStatus(companyId, com.cafm.cafmbackend.shared.enums.AssetStatus.ACTIVE);
        
        // Populate data rows
        int rowNum = 1;
        CellStyle dateStyle = createDateStyle(sheet.getWorkbook());
        CellStyle currencyStyle = createCurrencyStyle(sheet.getWorkbook());
        
        for (Asset asset : assets) {
            Row dataRow = sheet.createRow(rowNum++);
            
            dataRow.createCell(0).setCellValue(asset.getId().toString());
            dataRow.createCell(1).setCellValue(asset.getName());
            dataRow.createCell(2).setCellValue(asset.getDescription());
            dataRow.createCell(3).setCellValue(asset.getCategory() != null ? 
                                             asset.getCategory().getName() : "");
            dataRow.createCell(4).setCellValue(asset.getLocation());
            dataRow.createCell(5).setCellValue(asset.getStatus() != null ? 
                                             asset.getStatus().toString() : "");
            
            // Date cells
            Cell purchaseDateCell = dataRow.createCell(6);
            if (asset.getPurchaseDate() != null) {
                purchaseDateCell.setCellValue(asset.getPurchaseDate().format(DATE_FORMATTER));
                purchaseDateCell.setCellStyle(dateStyle);
            }
            
            // Currency cell
            Cell costCell = dataRow.createCell(7);
            if (asset.getPurchaseCost() != null) {
                costCell.setCellValue(asset.getPurchaseCost().doubleValue());
                costCell.setCellStyle(currencyStyle);
            }
            
            Cell warrantyCell = dataRow.createCell(8);
            if (asset.getWarrantyEndDate() != null) {
                warrantyCell.setCellValue(asset.getWarrantyEndDate().format(DATE_FORMATTER));
                warrantyCell.setCellStyle(dateStyle);
            }
            
            dataRow.createCell(9).setCellValue(asset.getMaintenanceFrequencyDays() != null ? 
                                             asset.getMaintenanceFrequencyDays() + " days" : "");
            
            Cell lastMaintenanceCell = dataRow.createCell(10);
            if (asset.getLastMaintenanceDate() != null) {
                lastMaintenanceCell.setCellValue(asset.getLastMaintenanceDate().format(DATE_FORMATTER));
                lastMaintenanceCell.setCellStyle(dateStyle);
            }
            
            Cell nextMaintenanceCell = dataRow.createCell(11);
            if (asset.getNextMaintenanceDate() != null) {
                nextMaintenanceCell.setCellValue(asset.getNextMaintenanceDate().format(DATE_FORMATTER));
                nextMaintenanceCell.setCellStyle(dateStyle);
            }
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createReportsSummarySheet(Sheet sheet, UUID companyId, 
                                          LocalDate startDate, LocalDate endDate) {
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Maintenance Reports Summary - " + startDate + " to " + endDate);
        
        // Add summary statistics
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        List<Report> allReports = reportRepository.findReportsBetweenDates(
            startDate, endDate);
        long totalReports = allReports.size();
        long pendingReports = allReports.stream()
            .filter(r -> r.getStatus() == com.cafm.cafmbackend.shared.enums.ReportStatus.SUBMITTED)
            .count();
        long completedReports = allReports.stream()
            .filter(r -> r.getStatus() == com.cafm.cafmbackend.shared.enums.ReportStatus.COMPLETED)
            .count();
        
        int rowNum = 2;
        sheet.createRow(rowNum++).createCell(0).setCellValue("Total Reports: " + totalReports);
        sheet.createRow(rowNum++).createCell(0).setCellValue("Pending Reports: " + pendingReports);
        sheet.createRow(rowNum++).createCell(0).setCellValue("Completed Reports: " + completedReports);
        sheet.createRow(rowNum++).createCell(0).setCellValue("Completion Rate: " + 
            (totalReports > 0 ? String.format("%.1f%%", (completedReports * 100.0 / totalReports)) : "0%"));
    }
    
    private void createWorkOrderStatusSummarySheet(Sheet sheet, UUID companyId, 
                                                  LocalDate startDate, LocalDate endDate) {
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Work Orders Status Summary - " + startDate + " to " + endDate);
        
        // Add work order statistics
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        List<WorkOrder> allOrders = workOrderRepository.findByCompany_IdAndScheduledDateRange(
            companyId, startDateTime, endDateTime);
        long totalOrders = allOrders.size();
        long assignedOrders = allOrders.stream()
            .filter(wo -> wo.getStatus() == com.cafm.cafmbackend.shared.enums.WorkOrderStatus.ASSIGNED)
            .count();
        long inProgressOrders = allOrders.stream()
            .filter(wo -> wo.getStatus() == com.cafm.cafmbackend.shared.enums.WorkOrderStatus.IN_PROGRESS)
            .count();
        long completedOrders = allOrders.stream()
            .filter(wo -> wo.getStatus() == com.cafm.cafmbackend.shared.enums.WorkOrderStatus.COMPLETED)
            .count();
        
        int rowNum = 2;
        sheet.createRow(rowNum++).createCell(0).setCellValue("Total Work Orders: " + totalOrders);
        sheet.createRow(rowNum++).createCell(0).setCellValue("Assigned Orders: " + assignedOrders);
        sheet.createRow(rowNum++).createCell(0).setCellValue("In Progress Orders: " + inProgressOrders);
        sheet.createRow(rowNum++).createCell(0).setCellValue("Completed Orders: " + completedOrders);
        sheet.createRow(rowNum++).createCell(0).setCellValue("Completion Rate: " + 
            (totalOrders > 0 ? String.format("%.1f%%", (completedOrders * 100.0 / totalOrders)) : "0%"));
    }
    
    // ========== Private Helper Methods for PDF Generation ==========
    
    private String generateMaintenanceReportsHTML(UUID companyId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        List<Report> reports = reportRepository.findReportsBetweenDates(
            startDateTime.toLocalDate(), endDateTime.toLocalDate());
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append("h1 { color: #2c3e50; border-bottom: 2px solid #3498db; }");
        html.append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #f2f2f2; font-weight: bold; }");
        html.append("tr:nth-child(even) { background-color: #f9f9f9; }");
        html.append(".high-priority { color: #e74c3c; font-weight: bold; }");
        html.append(".medium-priority { color: #f39c12; }");
        html.append(".low-priority { color: #27ae60; }");
        html.append("</style>");
        html.append("</head><body>");
        
        html.append("<h1>Maintenance Reports - ").append(startDate).append(" to ").append(endDate).append("</h1>");
        
        html.append("<table>");
        html.append("<tr>");
        html.append("<th>Report ID</th><th>Title</th><th>Priority</th><th>Status</th>");
        html.append("<th>Asset</th><th>Reported By</th><th>Created Date</th><th>Due Date</th>");
        html.append("</tr>");
        
        for (Report report : reports) {
            html.append("<tr>");
            html.append("<td>").append(report.getId().toString().substring(0, 8)).append("...</td>");
            html.append("<td>").append(escapeHtml(report.getTitle())).append("</td>");
            
            String priorityClass = "";
            if (report.getPriority() != null) {
                switch (report.getPriority()) {
                    case HIGH -> priorityClass = "high-priority";
                    case MEDIUM -> priorityClass = "medium-priority";
                    case LOW -> priorityClass = "low-priority";
                }
            }
            html.append("<td class=\"").append(priorityClass).append("\">")
                .append(report.getPriority() != null ? report.getPriority().toString() : "").append("</td>");
            
            html.append("<td>").append(report.getStatus() != null ? report.getStatus().toString() : "").append("</td>");
            html.append("<td>").append(report.getSchool() != null ? escapeHtml(report.getSchool().getName()) : "").append("</td>");
            html.append("<td>").append(report.getSupervisor() != null ? escapeHtml(report.getSupervisor().getFullName()) : "").append("</td>");
            html.append("<td>").append(report.getCreatedAt() != null ? report.getCreatedAt().format(DATETIME_FORMATTER) : "").append("</td>");
            html.append("<td>").append(report.getScheduledDate() != null ? report.getScheduledDate().format(DATE_FORMATTER) : "").append("</td>");
            html.append("</tr>");
        }
        
        html.append("</table>");
        html.append("</body></html>");
        
        return html.toString();
    }
    
    private String generateWorkOrdersHTML(UUID companyId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        List<WorkOrder> workOrders = workOrderRepository.findByCompany_IdAndScheduledDateRange(
            companyId, startDateTime, endDateTime);
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append("h1 { color: #2c3e50; border-bottom: 2px solid #3498db; }");
        html.append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #f2f2f2; font-weight: bold; }");
        html.append("tr:nth-child(even) { background-color: #f9f9f9; }");
        html.append(".status-assigned { color: #f39c12; }");
        html.append(".status-progress { color: #3498db; }");
        html.append(".status-completed { color: #27ae60; }");
        html.append("</style>");
        html.append("</head><body>");
        
        html.append("<h1>Work Orders - ").append(startDate).append(" to ").append(endDate).append("</h1>");
        
        html.append("<table>");
        html.append("<tr>");
        html.append("<th>Work Order ID</th><th>Title</th><th>Status</th><th>Priority</th>");
        html.append("<th>Asset</th><th>Assigned To</th><th>Created Date</th><th>Due Date</th>");
        html.append("</tr>");
        
        for (WorkOrder workOrder : workOrders) {
            html.append("<tr>");
            html.append("<td>").append(workOrder.getId().toString().substring(0, 8)).append("...</td>");
            html.append("<td>").append(escapeHtml(workOrder.getTitle())).append("</td>");
            
            String statusClass = "";
            if (workOrder.getStatus() != null) {
                switch (workOrder.getStatus()) {
                    case ASSIGNED -> statusClass = "status-assigned";
                    case IN_PROGRESS -> statusClass = "status-progress";
                    case COMPLETED -> statusClass = "status-completed";
                }
            }
            html.append("<td class=\"").append(statusClass).append("\">")
                .append(workOrder.getStatus() != null ? workOrder.getStatus().toString() : "").append("</td>");
            
            html.append("<td>").append(workOrder.getPriority() != null ? workOrder.getPriority().toString() : "").append("</td>");
            html.append("<td>").append(workOrder.getSchool() != null ? escapeHtml(workOrder.getSchool().getName()) : "").append("</td>");
            html.append("<td>").append(workOrder.getAssignedTo() != null ? escapeHtml(workOrder.getAssignedTo().getFullName()) : "").append("</td>");
            html.append("<td>").append(workOrder.getCreatedAt() != null ? workOrder.getCreatedAt().format(DATETIME_FORMATTER) : "").append("</td>");
            html.append("<td>").append(workOrder.getScheduledStart() != null ? workOrder.getScheduledStart().format(DATE_FORMATTER) : "").append("</td>");
            html.append("</tr>");
        }
        
        html.append("</table>");
        html.append("</body></html>");
        
        return html.toString();
    }
    
    // ========== Style Helper Methods ==========
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
    
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        return style;
    }
    
    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("$#,##0.00"));
        return style;
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}