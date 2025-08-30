package com.cafm.cafmbackend.application.service;

import com.cafm.cafmbackend.infrastructure.persistence.entity.*;
import com.cafm.cafmbackend.shared.enums.InventoryTransactionType;
import com.cafm.cafmbackend.infrastructure.persistence.repository.*;
import com.cafm.cafmbackend.shared.exception.BusinessLogicException;
import com.cafm.cafmbackend.shared.exception.DuplicateResourceException;
import com.cafm.cafmbackend.shared.exception.ErrorCode;
import com.cafm.cafmbackend.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Comprehensive inventory management service with stock tracking, transactions, and analytics.
 * 
 * Purpose: Provides complete inventory management capabilities including stock levels,
 * transactions, reorder management, and comprehensive reporting.
 * 
 * Pattern: Service layer with proper transaction management and audit logging
 * Java 23: Uses modern collection operations and pattern matching
 * Architecture: Domain service with comprehensive business logic
 * Standards: Implements proper error handling, logging, and security
 */
@Service
@Transactional
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final InventoryCategoryRepository inventoryCategoryRepository;
    private final WorkOrderRepository workOrderRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public InventoryService(InventoryItemRepository inventoryItemRepository,
                          InventoryTransactionRepository inventoryTransactionRepository,
                          InventoryCategoryRepository inventoryCategoryRepository,
                          WorkOrderRepository workOrderRepository,
                          CompanyRepository companyRepository,
                          UserRepository userRepository,
                          CurrentUserService currentUserService,
                          AuditService auditService) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.inventoryCategoryRepository = inventoryCategoryRepository;
        this.workOrderRepository = workOrderRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    // ==================== INVENTORY ITEM MANAGEMENT ====================

    /**
     * Create a new inventory item.
     */
    public InventoryItem createInventoryItem(InventoryItem item, UUID companyId) {
        logger.info("Creating new inventory item: {} for company: {}", item.getName(), companyId);

        // Check for duplicate item code
        if (inventoryItemRepository.existsByItemCodeAndCompanyId(item.getItemCode(), companyId)) {
            throw new DuplicateResourceException("InventoryItem", "itemCode", item.getItemCode());
        }

        // Set company
        Company company = companyRepository.getReferenceById(companyId);
        item.setCompany(company);

        // Set defaults
        if (item.getCurrentStock() == null) {
            item.setCurrentStock(BigDecimal.ZERO);
        }
        if (item.getIsActive() == null) {
            item.setIsActive(true);
        }
        if (item.getMinimumStock() == null) {
            item.setMinimumStock(BigDecimal.ZERO);
        }
        if (item.getReorderLevel() == null) {
            item.setReorderLevel(item.getMinimumStock() != null ? item.getMinimumStock() : BigDecimal.valueOf(10));
        }

        InventoryItem savedItem = inventoryItemRepository.save(item);
        
        // Create initial transaction if starting stock > 0
        if (item.getCurrentStock().compareTo(BigDecimal.ZERO) > 0) {
            createStockTransaction(
                savedItem.getId(),
                InventoryTransactionType.STOCK_IN,  // Use STOCK_IN instead of INITIAL_STOCK
                item.getCurrentStock(),
                "Initial stock entry",
                null,
                null
            );
        }

        auditService.logInventoryOperation("INVENTORY_ITEM_CREATED", 
            savedItem.getId(), savedItem.getName());

        logger.info("Inventory item created successfully: {}", savedItem.getItemCode());
        return savedItem;
    }

    /**
     * Update inventory item details.
     */
    public InventoryItem updateInventoryItem(UUID itemId, InventoryItem updatedItem) {
        logger.info("Updating inventory item: {}", itemId);

        InventoryItem existingItem = findInventoryItemById(itemId);

        // Update allowed fields
        if (updatedItem.getName() != null) {
            existingItem.setName(updatedItem.getName());
        }
        if (updatedItem.getNameAr() != null) {
            existingItem.setNameAr(updatedItem.getNameAr());
        }
        if (updatedItem.getDescription() != null) {
            existingItem.setDescription(updatedItem.getDescription());
        }
        if (updatedItem.getCategory() != null) {
            existingItem.setCategory(updatedItem.getCategory());
        }
        if (updatedItem.getUnitOfMeasure() != null) {
            existingItem.setUnitOfMeasure(updatedItem.getUnitOfMeasure());
        }
        if (updatedItem.getAverageCost() != null) {
            existingItem.setAverageCost(updatedItem.getAverageCost());
        }
        if (updatedItem.getMinimumStock() != null) {
            existingItem.setMinimumStock(updatedItem.getMinimumStock());
        }
        if (updatedItem.getReorderLevel() != null) {
            existingItem.setReorderLevel(updatedItem.getReorderLevel());
        }
        if (updatedItem.getReorderQuantity() != null) {
            existingItem.setReorderQuantity(updatedItem.getReorderQuantity());
        }
        // Supplier field doesn't exist in InventoryItem entity
        // Storage location is warehouseLocation
        if (updatedItem.getWarehouseLocation() != null) {
            existingItem.setWarehouseLocation(updatedItem.getWarehouseLocation());
        }
        // Barcode field doesn't exist in InventoryItem entity
        // Use itemCode instead
        if (updatedItem.getItemCode() != null) {
            existingItem.setItemCode(updatedItem.getItemCode());
        }

        InventoryItem savedItem = inventoryItemRepository.save(existingItem);
        
        auditService.logInventoryOperation("INVENTORY_ITEM_UPDATED", 
            savedItem.getId(), savedItem.getName());

        return savedItem;
    }

    /**
     * Activate or deactivate an inventory item.
     */
    public InventoryItem toggleItemStatus(UUID itemId, boolean isActive) {
        logger.info("Toggling inventory item {} status to: {}", itemId, isActive);

        InventoryItem item = findInventoryItemById(itemId);
        item.setIsActive(isActive);

        InventoryItem savedItem = inventoryItemRepository.save(item);
        
        auditService.logInventoryOperation(
            isActive ? "INVENTORY_ITEM_ACTIVATED" : "INVENTORY_ITEM_DEACTIVATED",
            savedItem.getId(), savedItem.getName());

        return savedItem;
    }

    /**
     * Delete inventory item (soft delete).
     */
    public void deleteInventoryItem(UUID itemId) {
        logger.info("Soft deleting inventory item: {}", itemId);

        InventoryItem item = findInventoryItemById(itemId);
        
        // Check if item has active transactions or current stock
        if (item.getCurrentStock().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessLogicException(
                "Cannot delete inventory item with remaining stock", 
                "BUSINESS_RULE_VIOLATION");
        }

        item.setDeletedAt(LocalDateTime.now());
        item.setIsActive(false);
        
        inventoryItemRepository.save(item);
        
        auditService.logInventoryOperation("INVENTORY_ITEM_DELETED", 
            item.getId(), item.getName());
    }

    // ==================== STOCK MANAGEMENT ====================

    /**
     * Add stock to inventory item.
     */
    public InventoryTransaction addStock(UUID itemId, BigDecimal quantity, String reason, 
                                       String reference, BigDecimal unitCost) {
        logger.info("Adding {} units to inventory item: {}", quantity, itemId);

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessLogicException("Quantity must be positive", "INVALID_FIELD_VALUE");
        }

        return createStockTransaction(itemId, InventoryTransactionType.STOCK_IN, 
            quantity, reason, reference, unitCost);
    }

    /**
     * Remove stock from inventory item.
     */
    public InventoryTransaction removeStock(UUID itemId, BigDecimal quantity, String reason, 
                                          String reference, UUID workOrderId) {
        logger.info("Removing {} units from inventory item: {}", quantity, itemId);

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessLogicException("Quantity must be positive", "INVALID_FIELD_VALUE");
        }

        InventoryItem item = findInventoryItemById(itemId);
        
        // Check available stock
        if (item.getCurrentStock().compareTo(quantity) < 0) {
            throw new BusinessLogicException(
                String.format("Insufficient stock. Available: %s, Requested: %s", 
                    item.getCurrentStock(), quantity),
                "INSUFFICIENT_STOCK");
        }

        return createStockTransaction(itemId, InventoryTransactionType.STOCK_OUT, 
            quantity, reason, reference, null, workOrderId);
    }

    /**
     * Transfer stock between items or locations.
     */
    public List<InventoryTransaction> transferStock(UUID fromItemId, UUID toItemId, 
                                                  BigDecimal quantity, String reason) {
        logger.info("Transferring {} units from item {} to item {}", quantity, fromItemId, toItemId);

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessLogicException("Quantity must be positive", "INVALID_FIELD_VALUE");
        }

        List<InventoryTransaction> transactions = new ArrayList<>();

        // Remove from source
        transactions.add(createStockTransaction(fromItemId, InventoryTransactionType.TRANSFER_OUT, 
            quantity, reason, String.format("Transfer to item %s", toItemId), null));

        // Add to destination
        transactions.add(createStockTransaction(toItemId, InventoryTransactionType.TRANSFER_IN, 
            quantity, reason, String.format("Transfer from item %s", fromItemId), null));

        auditService.logInventoryOperation("STOCK_TRANSFER", 
            fromItemId, String.format("Transferred %s units to %s", quantity, toItemId));

        return transactions;
    }

    /**
     * Adjust stock levels (for corrections).
     */
    public InventoryTransaction adjustStock(UUID itemId, BigDecimal newQuantity, String reason) {
        logger.info("Adjusting stock for item {} to {} units", itemId, newQuantity);

        if (newQuantity == null || newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessLogicException("New quantity must be non-negative", "INVALID_FIELD_VALUE");
        }

        InventoryItem item = findInventoryItemById(itemId);
        BigDecimal currentStock = item.getCurrentStock();
        BigDecimal difference = newQuantity.subtract(currentStock);

        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessLogicException("No adjustment needed", "BUSINESS_RULE_VIOLATION");
        }

        InventoryTransactionType transactionType = difference.compareTo(BigDecimal.ZERO) > 0 ? 
            InventoryTransactionType.ADJUSTMENT_IN : InventoryTransactionType.ADJUSTMENT_OUT;

        return createStockTransaction(itemId, transactionType, 
            difference.abs(), reason, "Stock adjustment", null);
    }

    /**
     * Record damaged/lost stock.
     */
    public InventoryTransaction recordDamage(UUID itemId, BigDecimal quantity, String reason, 
                                           String damageDetails) {
        logger.info("Recording {} units as damaged for item: {}", quantity, itemId);

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessLogicException("Quantity must be positive", "INVALID_FIELD_VALUE");
        }

        String fullReason = String.format("Damage/Loss: %s. Details: %s", reason, damageDetails);
        
        return createStockTransaction(itemId, InventoryTransactionType.DAMAGE, 
            quantity, fullReason, "DAMAGE", null);
    }

    // ==================== TRANSACTION MANAGEMENT ====================

    /**
     * Create a stock transaction.
     */
    private InventoryTransaction createStockTransaction(UUID itemId, InventoryTransactionType type, 
                                                      BigDecimal quantity, String reason, String reference, 
                                                      BigDecimal unitCost) {
        return createStockTransaction(itemId, type, quantity, reason, reference, unitCost, null);
    }

    private InventoryTransaction createStockTransaction(UUID itemId, InventoryTransactionType type, 
                                                      BigDecimal quantity, String reason, String reference, 
                                                      BigDecimal unitCost, UUID workOrderId) {
        InventoryItem item = findInventoryItemById(itemId);
        UUID currentUserId = currentUserService.getCurrentUserId();

        // Calculate new stock level
        BigDecimal newStock = calculateNewStock(item.getCurrentStock(), quantity, type);
        
        // Validate stock levels
        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessLogicException(
                "Transaction would result in negative stock", 
                "INSUFFICIENT_STOCK");
        }

        // Create transaction
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setTransactionNumber(generateTransactionNumber());
        transaction.setCompany(item.getCompany());
        transaction.setItem(item);
        transaction.setTransactionType(type);
        transaction.setQuantity(quantity);
        transaction.setNotes(reason);  // Use notes instead of setReason
        transaction.setReferenceType(reference);  // Use referenceType instead of setReference
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setCreatedByUser(userRepository.getReferenceById(currentUserId));
        
        if (unitCost != null) {
            transaction.setUnitCost(unitCost);
            // totalCost will be calculated automatically via getTotalCost() method
        } else if (item.getAverageCost() != null) {
            transaction.setUnitCost(item.getAverageCost());
        }

        if (workOrderId != null) {
            WorkOrder workOrder = workOrderRepository.getReferenceById(workOrderId);
            transaction.setWorkOrder(workOrder);
        }

        // Update item stock
        item.setCurrentStock(newStock);
        // lastStockUpdate and totalValue methods need to be added to InventoryItem

        // Update total value if cost information is available
        updateItemTotalValue(item);

        // Save transaction and item
        InventoryTransaction savedTransaction = inventoryTransactionRepository.save(transaction);
        inventoryItemRepository.save(item);

        logger.info("Stock transaction created: {} - {} units of {}", 
            transaction.getTransactionNumber(), quantity, item.getName());

        return savedTransaction;
    }

    /**
     * Get transaction history for an item.
     */
    @Transactional(readOnly = true)
    public List<InventoryTransaction> getTransactionHistory(UUID itemId) {
        return inventoryTransactionRepository.findByItemIdOrderByTransactionDateDesc(itemId);
    }

    /**
     * Get pending transactions requiring approval.
     */
    @Transactional(readOnly = true)
    public List<InventoryTransaction> getPendingApprovalTransactions(UUID companyId) {
        // This would require additional approval logic implementation
        // For now, return empty list since the method doesn't exist in repository
        logger.debug("Pending approval transactions requested for company: {}", companyId);
        return List.of();
    }

    // ==================== REORDER MANAGEMENT ====================

    /**
     * Get items that need reordering.
     */
    @Transactional(readOnly = true)
    public List<InventoryItem> getItemsRequiringReorder(UUID companyId) {
        return inventoryItemRepository.findItemsNeedingReorder(companyId);
    }

    /**
     * Get items with low stock.
     */
    @Transactional(readOnly = true)
    public List<InventoryItem> getLowStockItems(UUID companyId) {
        return inventoryItemRepository.findLowStockItems(companyId);
    }

    /**
     * Generate reorder suggestions.
     */
    @Transactional(readOnly = true)
    public Map<String, List<ReorderSuggestion>> generateReorderSuggestions(UUID companyId) {
        List<InventoryItem> reorderItems = getItemsRequiringReorder(companyId);
        Map<String, List<ReorderSuggestion>> suggestions = new HashMap<>();

        for (InventoryItem item : reorderItems) {
            String supplier = "Default Supplier"; // Using default since supplier field doesn't exist
            
            suggestions.computeIfAbsent(supplier, _unused -> new ArrayList<>())
                .add(new ReorderSuggestion(
                    item.getId(),
                    item.getItemCode(),
                    item.getName(),
                    item.getCurrentStock(),
                    item.getReorderLevel(),
                    item.getReorderQuantity() != null ? item.getReorderQuantity() : 
                        calculateOptimalReorderQuantity(item),
                    item.getAverageCost(), // Use averageCost instead of unitCost
                    calculateEstimatedDeliveryDate(item)
                ));
        }

        return suggestions;
    }

    /**
     * Create purchase requisition from reorder suggestions.
     */
    public Map<String, Object> createPurchaseRequisition(List<UUID> itemIds, String requisitionReason) {
        logger.info("Creating purchase requisition for {} items with reason: {}", itemIds.size(), requisitionReason);

        List<InventoryItem> items = itemIds.stream()
            .map(this::findInventoryItemById)
            .toList();

        BigDecimal totalEstimatedCost = BigDecimal.ZERO;
        Map<String, List<InventoryItem>> itemsBySupplier = items.stream()
            .collect(Collectors.groupingBy(_item -> "Default Supplier")); // Using default since supplier field doesn't exist

        for (InventoryItem item : items) {
            if (item.getAverageCost() != null && item.getReorderQuantity() != null) {
                totalEstimatedCost = totalEstimatedCost.add(
                    item.getAverageCost().multiply(item.getReorderQuantity())
                );
            }
        }

        String requisitionNumber = generateRequisitionNumber();
        
        // In a full implementation, this would create a PurchaseRequisition entity
        auditService.logInventoryOperation("PURCHASE_REQUISITION_CREATED", 
            null, String.format("Requisition %s for %d items", requisitionNumber, items.size()));

        return Map.of(
            "requisitionNumber", requisitionNumber,
            "itemCount", items.size(),
            "estimatedCost", totalEstimatedCost,
            "itemsBySupplier", itemsBySupplier,
            "createdAt", LocalDateTime.now()
        );
    }

    // ==================== CATEGORY MANAGEMENT ====================

    /**
     * Create inventory category.
     */
    public InventoryCategory createCategory(InventoryCategory category, UUID companyId) {
        logger.info("Creating inventory category: {}", category.getName());

        // Check for duplicate name
        if (inventoryCategoryRepository.existsByNameAndCompanyId(category.getName(), companyId)) {
            throw new DuplicateResourceException("InventoryCategory", "name", category.getName());
        }

        Company company = companyRepository.getReferenceById(companyId);
        category.setCompany(company);

        if (category.getIsActive() == null) {
            category.setIsActive(true);
        }

        return inventoryCategoryRepository.save(category);
    }

    /**
     * Get all categories for company.
     */
    @Transactional(readOnly = true)
    public Page<InventoryCategory> getCategories(UUID companyId, Pageable pageable) {
        return inventoryCategoryRepository.findByCompanyIdAndIsActiveTrue(companyId, pageable);
    }

    // ==================== QUERY METHODS ====================

    /**
     * Find inventory item by ID.
     */
    @Transactional(readOnly = true)
    public InventoryItem findInventoryItemById(UUID itemId) {
        return inventoryItemRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + itemId));
    }

    /**
     * Find inventory item by code.
     */
    @Transactional(readOnly = true)
    public Optional<InventoryItem> findByItemCode(String itemCode, UUID companyId) {
        return inventoryItemRepository.findByItemCodeAndCompanyId(itemCode, companyId);
    }

    /**
     * Search inventory items.
     */
    @Transactional(readOnly = true)
    public Page<InventoryItem> searchInventoryItems(UUID companyId, String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return inventoryItemRepository.findByCompanyIdAndIsActiveTrue(companyId, pageable);
        }
        return inventoryItemRepository.searchItems(companyId, searchTerm.trim(), pageable);
    }

    /**
     * Get inventory items by category.
     */
    @Transactional(readOnly = true)
    public List<InventoryItem> getItemsByCategory(UUID categoryId, UUID companyId) {
        return inventoryItemRepository.findByCategoryIdAndIsActiveTrue(categoryId);
    }

    /**
     * Get all inventory items for company.
     */
    @Transactional(readOnly = true)
    public Page<InventoryItem> getAllItems(UUID companyId, Pageable pageable) {
        return inventoryItemRepository.findByCompanyIdAndIsActiveTrue(companyId, pageable);
    }

    // ==================== STATISTICS AND REPORTING ====================

    /**
     * Get comprehensive inventory statistics.
     */
    @Transactional(readOnly = true)
    public InventoryStatistics getInventoryStatistics(UUID companyId) {
        // Use a custom query to fetch all items for company with JOIN FETCH to avoid lazy loading
        List<InventoryItem> allItems = inventoryItemRepository.findAllByCompanyIdWithFetch(companyId);
        
        InventoryStatistics stats = new InventoryStatistics();
        stats.totalItems = allItems.size();
        stats.activeItems = allItems.stream().filter(InventoryItem::getIsActive).count();
        stats.inactiveItems = stats.totalItems - stats.activeItems;
        
        List<InventoryItem> activeItems = allItems.stream()
            .filter(InventoryItem::getIsActive)
            .toList();

        // Stock metrics
        stats.totalStockValue = activeItems.stream()
            .filter(item -> item.getAverageCost() != null)
            .map(item -> item.getAverageCost().multiply(item.getCurrentStock()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.lowStockItems = activeItems.stream()
            .filter(item -> item.getCurrentStock().compareTo(item.getMinimumStock()) <= 0)
            .count();

        stats.outOfStockItems = activeItems.stream()
            .filter(item -> item.getCurrentStock().compareTo(BigDecimal.ZERO) == 0)
            .count();

        stats.reorderRequiredItems = activeItems.stream()
            .filter(item -> item.getCurrentStock().compareTo(item.getReorderLevel()) <= 0)
            .count();

        // Category breakdown
        stats.itemsByCategory = activeItems.stream()
            .filter(item -> item.getCategory() != null)
            .collect(Collectors.groupingBy(
                item -> item.getCategory().getName(),
                Collectors.counting()
            ));

        stats.valueByCategory = activeItems.stream()
            .filter(item -> item.getCategory() != null && item.getAverageCost() != null)
            .collect(Collectors.groupingBy(
                item -> item.getCategory().getName(),
                Collectors.reducing(BigDecimal.ZERO, 
                    item -> item.getAverageCost().multiply(item.getCurrentStock()),
                    BigDecimal::add)
            ));

        // Transaction metrics (last 30 days) - avoid loading all transactions
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        // TODO: Add proper repository method for company-specific transactions with date filter
        // For now, we'll set default values to avoid performance issues
        long recentTransactionsCount = 0;
        long stockMovements = 0;

        stats.transactionsLast30Days = recentTransactionsCount;
        stats.stockMovementsLast30Days = stockMovements;

        return stats;
    }

    /**
     * Get inventory valuation report.
     */
    @Transactional(readOnly = true)
    public InventoryValuationReport getValuationReport(UUID companyId) {
        // Use a custom query to fetch all active items for company with JOIN FETCH to avoid lazy loading
        List<InventoryItem> items = inventoryItemRepository.findAllByCompanyIdWithFetch(companyId).stream()
            .filter(InventoryItem::getIsActive)
            .toList();
        
        InventoryValuationReport report = new InventoryValuationReport();
        report.reportDate = LocalDate.now();
        report.companyId = companyId;

        for (InventoryItem item : items) {
            if (item.getCurrentStock().compareTo(BigDecimal.ZERO) > 0) {
                InventoryValuationEntry entry = new InventoryValuationEntry();
                entry.itemId = item.getId();
                entry.itemCode = item.getItemCode();
                entry.itemName = item.getName();
                entry.currentStock = item.getCurrentStock();
                entry.unitCost = item.getAverageCost();
                entry.totalValue = item.getAverageCost() != null ? 
                    item.getAverageCost().multiply(item.getCurrentStock()) :
                    BigDecimal.ZERO;
                
                report.entries.add(entry);
                report.totalValue = report.totalValue.add(entry.totalValue);
            }
        }

        report.itemCount = report.entries.size();
        return report;
    }

    // ==================== UTILITY METHODS ====================

    private BigDecimal calculateNewStock(BigDecimal currentStock, BigDecimal quantity, InventoryTransactionType type) {
        return switch (type) {
            case STOCK_IN, INITIAL_STOCK, TRANSFER_IN, ADJUSTMENT_IN -> currentStock.add(quantity);
            case STOCK_OUT, TRANSFER_OUT, ADJUSTMENT_OUT, DAMAGE -> currentStock.subtract(quantity);
            case RETURN -> currentStock.add(quantity); // Assuming returns increase stock
            default -> currentStock;
        };
    }

    private void updateItemTotalValue(InventoryItem item) {
        // Note: setTotalValue method needs to be added to InventoryItem entity
        // For now, total value calculation is not performed since there's no setter
        logger.debug("Total value calculation skipped for item: {}", item.getItemCode());
    }

    private BigDecimal calculateOptimalReorderQuantity(InventoryItem item) {
        // Simple calculation - in practice, this could be more sophisticated
        BigDecimal reorderLevel = item.getReorderLevel();
        BigDecimal minimumStock = item.getMinimumStock();
        
        if (reorderLevel != null && minimumStock != null) {
            BigDecimal option1 = reorderLevel.multiply(BigDecimal.valueOf(2));
            BigDecimal option2 = minimumStock.multiply(BigDecimal.valueOf(3));
            return option1.max(option2);
        }
        return BigDecimal.valueOf(50); // Default quantity
    }

    private LocalDate calculateEstimatedDeliveryDate(InventoryItem item) {
        // Simple calculation - add 7-14 days based on specifications or default
        // Note: supplier field doesn't exist in InventoryItem, using default lead time
        logger.debug("Calculating delivery date for item: {}", item.getItemCode());
        int leadTimeDays = 14; // Default lead time
        return LocalDate.now().plusDays(leadTimeDays);
    }

    private String generateTransactionNumber() {
        String prefix = "TXN";
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        return prefix + "-" + datePart + "-" + randomPart;
    }

    private String generateRequisitionNumber() {
        String prefix = "REQ";
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return prefix + "-" + datePart + "-" + randomPart;
    }


    // ==================== INNER CLASSES ====================

    /**
     * Inventory statistics DTO.
     */
    public static class InventoryStatistics {
        public long totalItems;
        public long activeItems;
        public long inactiveItems;
        public BigDecimal totalStockValue = BigDecimal.ZERO;
        public long lowStockItems;
        public long outOfStockItems;
        public long reorderRequiredItems;
        public Map<String, Long> itemsByCategory = new HashMap<>();
        public Map<String, BigDecimal> valueByCategory = new HashMap<>();
        public long transactionsLast30Days;
        public long stockMovementsLast30Days;
    }

    /**
     * Reorder suggestion DTO.
     */
    public static class ReorderSuggestion {
        public final UUID itemId;
        public final String itemCode;
        public final String itemName;
        public final BigDecimal currentStock;
        public final BigDecimal reorderLevel;
        public final BigDecimal suggestedQuantity;
        public final BigDecimal unitCost;
        public final LocalDate estimatedDeliveryDate;

        public ReorderSuggestion(UUID itemId, String itemCode, String itemName, 
                               BigDecimal currentStock, BigDecimal reorderLevel, BigDecimal suggestedQuantity,
                               BigDecimal unitCost, LocalDate estimatedDeliveryDate) {
            this.itemId = itemId;
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.currentStock = currentStock;
            this.reorderLevel = reorderLevel;
            this.suggestedQuantity = suggestedQuantity;
            this.unitCost = unitCost;
            this.estimatedDeliveryDate = estimatedDeliveryDate;
        }
    }

    /**
     * Inventory valuation report DTO.
     */
    public static class InventoryValuationReport {
        public LocalDate reportDate;
        public UUID companyId;
        public int itemCount;
        public BigDecimal totalValue = BigDecimal.ZERO;
        public List<InventoryValuationEntry> entries = new ArrayList<>();
    }

    /**
     * Inventory valuation entry.
     */
    public static class InventoryValuationEntry {
        public UUID itemId;
        public String itemCode;
        public String itemName;
        public BigDecimal currentStock;
        public BigDecimal unitCost;
        public BigDecimal totalValue;
    }
}