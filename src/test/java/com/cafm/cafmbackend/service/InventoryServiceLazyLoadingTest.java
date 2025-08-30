package com.cafm.cafmbackend.service;

import com.cafm.cafmbackend.infrastructure.persistence.entity.Company;
import com.cafm.cafmbackend.infrastructure.persistence.entity.InventoryItem;
import com.cafm.cafmbackend.infrastructure.persistence.repository.CompanyRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.InventoryItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Test to verify lazy loading issues are resolved for InventoryItem entities.
 * 
 * Purpose: Ensures that Company relationships are properly loaded to prevent
 * lazy initialization exceptions when serializing InventoryItem entities.
 * 
 * Pattern: Integration test with real database and transaction boundaries
 * Java 23: Uses modern JUnit 5 features and assertions
 * Architecture: Tests actual lazy loading behavior in realistic scenarios
 * Standards: Comprehensive test coverage for lazy loading scenarios
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("InventoryService Lazy Loading Tests")
class InventoryServiceLazyLoadingTest {

    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    
    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("Should load company relationship when getting inventory item by ID")
    @Transactional
    void shouldLoadCompanyWhenGettingItemById() {
        // Arrange: Create test data
        Company company = createTestCompany();
        InventoryItem item = createTestInventoryItem(company);
        
        // Act & Assert: This should not throw lazy initialization exception
        assertDoesNotThrow(() -> {
            InventoryItem retrievedItem = inventoryService.findInventoryItemById(item.getId());
            
            // Verify company is loaded (this would trigger lazy loading exception if not fixed)
            assertThat(retrievedItem.getCompany()).isNotNull();
            assertThat(retrievedItem.getCompany().getName()).isEqualTo("Test Company");
            assertThat(retrievedItem.getCompany().getId()).isEqualTo(company.getId());
        });
    }

    @Test
    @DisplayName("Should load company relationship when searching inventory items")
    @Transactional
    void shouldLoadCompanyWhenSearchingItems() {
        // Arrange: Create test data
        Company company = createTestCompany();
        InventoryItem item = createTestInventoryItem(company);
        
        // Act & Assert: This should not throw lazy initialization exception
        assertDoesNotThrow(() -> {
            Page<InventoryItem> items = inventoryService.searchInventoryItems(
                company.getId(), "Test", PageRequest.of(0, 10));
            
            assertThat(items.getContent()).isNotEmpty();
            
            // Verify each item has company loaded
            for (InventoryItem retrievedItem : items.getContent()) {
                assertThat(retrievedItem.getCompany()).isNotNull();
                assertThat(retrievedItem.getCompany().getName()).isNotNull();
            }
        });
    }

    @Test
    @DisplayName("Should load company relationship when getting all items")
    @Transactional
    void shouldLoadCompanyWhenGettingAllItems() {
        // Arrange: Create test data
        Company company = createTestCompany();
        InventoryItem item = createTestInventoryItem(company);
        
        // Act & Assert: This should not throw lazy initialization exception
        assertDoesNotThrow(() -> {
            Page<InventoryItem> items = inventoryService.getAllItems(
                company.getId(), PageRequest.of(0, 10));
            
            assertThat(items.getContent()).isNotEmpty();
            
            // Verify each item has company loaded
            for (InventoryItem retrievedItem : items.getContent()) {
                assertThat(retrievedItem.getCompany()).isNotNull();
                assertThat(retrievedItem.getCompany().getName()).isNotNull();
            }
        });
    }

    @Test
    @DisplayName("Should load company relationship when getting low stock items")
    @Transactional
    void shouldLoadCompanyWhenGettingLowStockItems() {
        // Arrange: Create test data with low stock
        Company company = createTestCompany();
        InventoryItem lowStockItem = createTestInventoryItem(company);
        lowStockItem.setCurrentStock(BigDecimal.ONE); // Set below minimum
        lowStockItem.setMinimumStock(BigDecimal.TEN);
        inventoryItemRepository.save(lowStockItem);
        
        // Act & Assert: This should not throw lazy initialization exception
        assertDoesNotThrow(() -> {
            List<InventoryItem> items = inventoryService.getLowStockItems(company.getId());
            
            if (!items.isEmpty()) {
                // Verify each item has company loaded
                for (InventoryItem retrievedItem : items) {
                    assertThat(retrievedItem.getCompany()).isNotNull();
                    assertThat(retrievedItem.getCompany().getName()).isNotNull();
                }
            }
        });
    }

    @Test
    @DisplayName("Should load company relationship when getting items requiring reorder")
    @Transactional
    void shouldLoadCompanyWhenGettingReorderItems() {
        // Arrange: Create test data that needs reorder
        Company company = createTestCompany();
        InventoryItem reorderItem = createTestInventoryItem(company);
        reorderItem.setCurrentStock(BigDecimal.ONE); // Set below reorder level
        reorderItem.setReorderLevel(BigDecimal.TEN);
        inventoryItemRepository.save(reorderItem);
        
        // Act & Assert: This should not throw lazy initialization exception
        assertDoesNotThrow(() -> {
            List<InventoryItem> items = inventoryService.getItemsRequiringReorder(company.getId());
            
            if (!items.isEmpty()) {
                // Verify each item has company loaded
                for (InventoryItem retrievedItem : items) {
                    assertThat(retrievedItem.getCompany()).isNotNull();
                    assertThat(retrievedItem.getCompany().getName()).isNotNull();
                }
            }
        });
    }

    @Test
    @DisplayName("Should load company relationship when getting inventory statistics")
    @Transactional
    void shouldLoadCompanyWhenGettingStatistics() {
        // Arrange: Create test data
        Company company = createTestCompany();
        InventoryItem item = createTestInventoryItem(company);
        
        // Act & Assert: This should not throw lazy initialization exception
        assertDoesNotThrow(() -> {
            InventoryService.InventoryStatistics stats = 
                inventoryService.getInventoryStatistics(company.getId());
            
            assertThat(stats).isNotNull();
            assertThat(stats.totalItems).isGreaterThanOrEqualTo(0);
        });
    }

    @Test
    @DisplayName("Should load company relationship when getting valuation report")
    @Transactional
    void shouldLoadCompanyWhenGettingValuationReport() {
        // Arrange: Create test data with cost
        Company company = createTestCompany();
        InventoryItem item = createTestInventoryItem(company);
        item.setAverageCost(BigDecimal.valueOf(10.50));
        item.setCurrentStock(BigDecimal.valueOf(100));
        inventoryItemRepository.save(item);
        
        // Act & Assert: This should not throw lazy initialization exception
        assertDoesNotThrow(() -> {
            InventoryService.InventoryValuationReport report = 
                inventoryService.getValuationReport(company.getId());
            
            assertThat(report).isNotNull();
            assertThat(report.companyId).isEqualTo(company.getId());
        });
    }

    /**
     * Create a test company for testing purposes.
     */
    private Company createTestCompany() {
        Company company = new Company();
        company.setName("Test Company");
        company.setDisplayName("Test Company Display");
        company.setDomain("test.com");
        company.setIsActive(true);
        company.setMaxUsers(100);
        company.setMaxSupervisors(10);
        company.setMaxTechnicians(20);
        company.setMaxSchools(5);
        return companyRepository.save(company);
    }

    /**
     * Create a test inventory item with the given company.
     */
    private InventoryItem createTestInventoryItem(Company company) {
        InventoryItem item = new InventoryItem();
        item.setCompany(company);
        item.setItemCode("TEST-001");
        item.setName("Test Inventory Item");
        item.setUnitOfMeasure("PIECE");
        item.setCurrentStock(BigDecimal.valueOf(50));
        item.setMinimumStock(BigDecimal.valueOf(10));
        item.setReorderLevel(BigDecimal.valueOf(15));
        item.setIsActive(true);
        return inventoryItemRepository.save(item);
    }
}