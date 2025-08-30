package com.cafm.cafmbackend.api.controllers;

import com.cafm.cafmbackend.infrastructure.persistence.entity.InventoryCategory;
import com.cafm.cafmbackend.infrastructure.persistence.entity.InventoryItem;
import com.cafm.cafmbackend.infrastructure.persistence.entity.InventoryTransaction;
import com.cafm.cafmbackend.application.service.InventoryService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for inventory management operations.
 * 
 * Purpose: Provides comprehensive inventory management endpoints including
 * item management, stock transactions, reorder management, and reporting.
 * 
 * Pattern: RESTful controller with comprehensive OpenAPI documentation
 * Java 23: Modern controller patterns with validation
 * Architecture: API layer with proper security annotations
 * Standards: Follows REST conventions and security best practices
 */
@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory Management", description = "Inventory and stock management operations")
@SecurityRequirement(name = "Bearer Authentication")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // ==================== INVENTORY ITEM MANAGEMENT ====================

    @Operation(
        summary = "Create inventory item",
        description = "Create a new inventory item with stock tracking",
        responses = {
            @ApiResponse(responseCode = "201", description = "Inventory item created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Item code already exists"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<InventoryItem> createInventoryItem(
            @Parameter(description = "Company ID", required = true)
            @RequestParam UUID companyId,
            
            @Parameter(description = "Inventory item details", required = true)
            @Valid @RequestBody InventoryItem inventoryItem) {
        
        InventoryItem created = inventoryService.createInventoryItem(inventoryItem, companyId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
        summary = "Get inventory item",
        description = "Retrieve a specific inventory item by ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "Inventory item retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory item not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @GetMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<InventoryItem> getInventoryItem(
            @Parameter(description = "Item ID", required = true)
            @PathVariable UUID itemId) {
        
        InventoryItem item = inventoryService.findInventoryItemById(itemId);
        return ResponseEntity.ok(item);
    }

    @Operation(
        summary = "Update inventory item",
        description = "Update inventory item details (not stock levels)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Inventory item updated successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory item not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<InventoryItem> updateInventoryItem(
            @Parameter(description = "Item ID", required = true)
            @PathVariable UUID itemId,
            
            @Parameter(description = "Updated inventory item details", required = true)
            @Valid @RequestBody InventoryItem inventoryItem) {
        
        InventoryItem updated = inventoryService.updateInventoryItem(itemId, inventoryItem);
        return ResponseEntity.ok(updated);
    }

    @Operation(
        summary = "Toggle item status",
        description = "Activate or deactivate an inventory item",
        responses = {
            @ApiResponse(responseCode = "200", description = "Item status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory item not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PatchMapping("/items/{itemId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<InventoryItem> toggleItemStatus(
            @Parameter(description = "Item ID", required = true)
            @PathVariable UUID itemId,
            
            @Parameter(description = "Active status", required = true)
            @RequestParam boolean isActive) {
        
        InventoryItem updated = inventoryService.toggleItemStatus(itemId, isActive);
        return ResponseEntity.ok(updated);
    }

    @Operation(
        summary = "Delete inventory item",
        description = "Soft delete an inventory item (must have zero stock)",
        responses = {
            @ApiResponse(responseCode = "204", description = "Inventory item deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory item not found"),
            @ApiResponse(responseCode = "400", description = "Cannot delete item with remaining stock"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteInventoryItem(
            @Parameter(description = "Item ID", required = true)
            @PathVariable UUID itemId) {
        
        inventoryService.deleteInventoryItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Search inventory items",
        description = "Search inventory items with pagination and filtering",
        responses = {
            @ApiResponse(responseCode = "200", description = "Items retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<Page<InventoryItem>> searchInventoryItems(
            @Parameter(description = "Company ID", required = true)
            @RequestParam UUID companyId,
            
            @Parameter(description = "Search term")
            @RequestParam(required = false) String search,
            
            @Parameter(description = "Category ID")
            @RequestParam(required = false) UUID categoryId,
            
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<InventoryItem> items;
        
        if (categoryId != null) {
            List<InventoryItem> categoryItems = inventoryService.getItemsByCategory(categoryId, companyId);
            // Convert List to Page for compatibility
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), categoryItems.size());
            items = new org.springframework.data.domain.PageImpl<>(
                categoryItems.subList(start, end), pageable, categoryItems.size());
        } else {
            items = inventoryService.searchInventoryItems(companyId, search, pageable);
        }
        
        return ResponseEntity.ok(items);
    }

    // ==================== STOCK MANAGEMENT ====================

    @Operation(
        summary = "Add stock",
        description = "Add stock to an inventory item",
        responses = {
            @ApiResponse(responseCode = "201", description = "Stock added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Inventory item not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping("/items/{itemId}/stock/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<InventoryTransaction> addStock(
            @Parameter(description = "Item ID", required = true)
            @PathVariable UUID itemId,
            
            @Parameter(description = "Quantity to add", required = true)
            @RequestParam BigDecimal quantity,
            
            @Parameter(description = "Reason for stock addition", required = true)
            @RequestParam String reason,
            
            @Parameter(description = "Reference (PO number, etc.)")
            @RequestParam(required = false) String reference,
            
            @Parameter(description = "Unit cost")
            @RequestParam(required = false) BigDecimal unitCost) {
        
        InventoryTransaction transaction = inventoryService.addStock(
            itemId, quantity, reason, reference, unitCost);
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    @Operation(
        summary = "Remove stock",
        description = "Remove stock from an inventory item",
        responses = {
            @ApiResponse(responseCode = "201", description = "Stock removed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or insufficient stock"),
            @ApiResponse(responseCode = "404", description = "Inventory item not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping("/items/{itemId}/stock/remove")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<InventoryTransaction> removeStock(
            @Parameter(description = "Item ID", required = true)
            @PathVariable UUID itemId,
            
            @Parameter(description = "Quantity to remove", required = true)
            @RequestParam BigDecimal quantity,
            
            @Parameter(description = "Reason for stock removal", required = true)
            @RequestParam String reason,
            
            @Parameter(description = "Reference")
            @RequestParam(required = false) String reference,
            
            @Parameter(description = "Work order ID (if related to work order)")
            @RequestParam(required = false) UUID workOrderId) {
        
        InventoryTransaction transaction = inventoryService.removeStock(
            itemId, quantity, reason, reference, workOrderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    @Operation(
        summary = "Transfer stock",
        description = "Transfer stock between inventory items",
        responses = {
            @ApiResponse(responseCode = "201", description = "Stock transferred successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or insufficient stock"),
            @ApiResponse(responseCode = "404", description = "Inventory item not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping("/items/{fromItemId}/transfer/{toItemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<List<InventoryTransaction>> transferStock(
            @Parameter(description = "Source item ID", required = true)
            @PathVariable UUID fromItemId,
            
            @Parameter(description = "Destination item ID", required = true)
            @PathVariable UUID toItemId,
            
            @Parameter(description = "Quantity to transfer", required = true)
            @RequestParam BigDecimal quantity,
            
            @Parameter(description = "Reason for transfer", required = true)
            @RequestParam String reason) {
        
        List<InventoryTransaction> transactions = inventoryService.transferStock(
            fromItemId, toItemId, quantity, reason);
        return ResponseEntity.status(HttpStatus.CREATED).body(transactions);
    }

    @Operation(
        summary = "Adjust stock",
        description = "Adjust stock levels for corrections",
        responses = {
            @ApiResponse(responseCode = "201", description = "Stock adjusted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Inventory item not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping("/items/{itemId}/stock/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<InventoryTransaction> adjustStock(
            @Parameter(description = "Item ID", required = true)
            @PathVariable UUID itemId,
            
            @Parameter(description = "New stock quantity", required = true)
            @RequestParam BigDecimal newQuantity,
            
            @Parameter(description = "Reason for adjustment", required = true)
            @RequestParam String reason) {
        
        InventoryTransaction transaction = inventoryService.adjustStock(itemId, newQuantity, reason);
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    @Operation(
        summary = "Record damage",
        description = "Record damaged or lost inventory",
        responses = {
            @ApiResponse(responseCode = "201", description = "Damage recorded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Inventory item not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping("/items/{itemId}/damage")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<InventoryTransaction> recordDamage(
            @Parameter(description = "Item ID", required = true)
            @PathVariable UUID itemId,
            
            @Parameter(description = "Damaged quantity", required = true)
            @RequestParam BigDecimal quantity,
            
            @Parameter(description = "Damage reason", required = true)
            @RequestParam String reason,
            
            @Parameter(description = "Damage details", required = true)
            @RequestParam String damageDetails) {
        
        InventoryTransaction transaction = inventoryService.recordDamage(
            itemId, quantity, reason, damageDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    // ==================== TRANSACTION HISTORY ====================

    @Operation(
        summary = "Get transaction history",
        description = "Get transaction history for an inventory item",
        responses = {
            @ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory item not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @GetMapping("/items/{itemId}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<List<InventoryTransaction>> getTransactionHistory(
            @Parameter(description = "Item ID", required = true)
            @PathVariable UUID itemId) {
        
        List<InventoryTransaction> transactions = inventoryService.getTransactionHistory(itemId);
        return ResponseEntity.ok(transactions);
    }

    @Operation(
        summary = "Get pending approval transactions",
        description = "Get transactions pending approval (admin only)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Pending transactions retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @GetMapping("/transactions/pending-approval")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InventoryTransaction>> getPendingApprovalTransactions(
            @Parameter(description = "Company ID", required = true)
            @RequestParam UUID companyId) {
        
        List<InventoryTransaction> transactions = inventoryService.getPendingApprovalTransactions(companyId);
        return ResponseEntity.ok(transactions);
    }

    // ==================== REORDER MANAGEMENT ====================

    @Operation(
        summary = "Get low stock items",
        description = "Get items with stock below minimum levels",
        responses = {
            @ApiResponse(responseCode = "200", description = "Low stock items retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @GetMapping("/items/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<List<InventoryItem>> getLowStockItems(
            @Parameter(description = "Company ID", required = true)
            @RequestParam UUID companyId) {
        
        List<InventoryItem> items = inventoryService.getLowStockItems(companyId);
        return ResponseEntity.ok(items);
    }

    @Operation(
        summary = "Get items requiring reorder",
        description = "Get items that need to be reordered",
        responses = {
            @ApiResponse(responseCode = "200", description = "Reorder items retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @GetMapping("/items/reorder-required")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<List<InventoryItem>> getItemsRequiringReorder(
            @Parameter(description = "Company ID", required = true)
            @RequestParam UUID companyId) {
        
        List<InventoryItem> items = inventoryService.getItemsRequiringReorder(companyId);
        return ResponseEntity.ok(items);
    }

    @Operation(
        summary = "Generate reorder suggestions",
        description = "Generate reorder suggestions grouped by supplier",
        responses = {
            @ApiResponse(responseCode = "200", description = "Reorder suggestions generated successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @GetMapping("/reorder-suggestions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<Map<String, List<InventoryService.ReorderSuggestion>>> generateReorderSuggestions(
            @Parameter(description = "Company ID", required = true)
            @RequestParam UUID companyId) {
        
        Map<String, List<InventoryService.ReorderSuggestion>> suggestions = 
            inventoryService.generateReorderSuggestions(companyId);
        return ResponseEntity.ok(suggestions);
    }

    @Operation(
        summary = "Create purchase requisition",
        description = "Create purchase requisition from selected items",
        responses = {
            @ApiResponse(responseCode = "201", description = "Purchase requisition created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping("/purchase-requisition")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<Map<String, Object>> createPurchaseRequisition(
            @Parameter(description = "Item IDs to include in requisition", required = true)
            @RequestParam List<UUID> itemIds,
            
            @Parameter(description = "Requisition reason", required = true)
            @RequestParam String reason) {
        
        Map<String, Object> requisition = inventoryService.createPurchaseRequisition(itemIds, reason);
        return ResponseEntity.status(HttpStatus.CREATED).body(requisition);
    }

    // ==================== CATEGORY MANAGEMENT ====================

    @Operation(
        summary = "Create inventory category",
        description = "Create a new inventory category",
        responses = {
            @ApiResponse(responseCode = "201", description = "Category created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Category name already exists"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<InventoryCategory> createCategory(
            @Parameter(description = "Company ID", required = true)
            @RequestParam UUID companyId,
            
            @Parameter(description = "Category details", required = true)
            @Valid @RequestBody InventoryCategory category) {
        
        InventoryCategory created = inventoryService.createCategory(category, companyId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
        summary = "Get inventory categories",
        description = "Get all active inventory categories for company",
        responses = {
            @ApiResponse(responseCode = "200", description = "Categories retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<Page<InventoryCategory>> getCategories(
            @Parameter(description = "Company ID", required = true)
            @RequestParam UUID companyId,
            
            @PageableDefault(size = 50) Pageable pageable) {
        
        Page<InventoryCategory> categories = inventoryService.getCategories(companyId, pageable);
        return ResponseEntity.ok(categories);
    }

    // ==================== STATISTICS AND REPORTING ====================

    @Operation(
        summary = "Get inventory statistics",
        description = "Get comprehensive inventory statistics for company",
        responses = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<InventoryService.InventoryStatistics> getInventoryStatistics(
            @Parameter(description = "Company ID", required = true)
            @RequestParam UUID companyId) {
        
        InventoryService.InventoryStatistics statistics = inventoryService.getInventoryStatistics(companyId);
        return ResponseEntity.ok(statistics);
    }

    @Operation(
        summary = "Get inventory valuation report",
        description = "Get detailed inventory valuation report",
        responses = {
            @ApiResponse(responseCode = "200", description = "Valuation report generated successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
        }
    )
    @GetMapping("/valuation-report")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<InventoryService.InventoryValuationReport> getValuationReport(
            @Parameter(description = "Company ID", required = true)
            @RequestParam UUID companyId) {
        
        InventoryService.InventoryValuationReport report = inventoryService.getValuationReport(companyId);
        return ResponseEntity.ok(report);
    }
}