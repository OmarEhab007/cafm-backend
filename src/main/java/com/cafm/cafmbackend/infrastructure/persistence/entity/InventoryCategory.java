package com.cafm.cafmbackend.infrastructure.persistence.entity;

import com.cafm.cafmbackend.infrastructure.persistence.entity.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Inventory Category entity for organizing inventory items.
 * Supports hierarchical category structure.
 */
@Entity
@Table(name = "inventory_categories")
@NamedQueries({
    @NamedQuery(
        name = "InventoryCategory.findByCompany",
        query = "SELECT ic FROM InventoryCategory ic WHERE ic.company.id = :companyId AND ic.isActive = true ORDER BY ic.name"
    ),
    @NamedQuery(
        name = "InventoryCategory.findRootCategories",
        query = "SELECT ic FROM InventoryCategory ic WHERE ic.company.id = :companyId AND ic.parentCategory IS NULL AND ic.isActive = true"
    )
})
public class InventoryCategory extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @NotNull(message = "Company is required")
    private Company company;
    
    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name cannot exceed 100 characters")
    private String name;
    
    @Column(name = "name_ar", length = 100)
    @Size(max = 100, message = "Arabic name cannot exceed 100 characters")
    private String nameAr;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private InventoryCategory parentCategory;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // ========== Relationships ==========
    
    @OneToMany(mappedBy = "parentCategory", fetch = FetchType.LAZY)
    private Set<InventoryCategory> subCategories = new HashSet<>();
    
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private Set<InventoryItem> items = new HashSet<>();
    
    // ========== Constructors ==========
    
    public InventoryCategory() {
        super();
    }
    
    public InventoryCategory(Company company, String name) {
        this();
        this.company = company;
        this.name = name;
    }
    
    public InventoryCategory(Company company, String name, String nameAr, String description) {
        this(company, name);
        this.nameAr = nameAr;
        this.description = description;
    }
    
    // ========== Business Methods ==========
    
    /**
     * Add a subcategory
     */
    public void addSubCategory(InventoryCategory subCategory) {
        subCategories.add(subCategory);
        subCategory.setParentCategory(this);
    }
    
    /**
     * Remove a subcategory
     */
    public void removeSubCategory(InventoryCategory subCategory) {
        subCategories.remove(subCategory);
        subCategory.setParentCategory(null);
    }
    
    /**
     * Check if this is a root category
     */
    public boolean isRootCategory() {
        return parentCategory == null;
    }
    
    /**
     * Check if this is a leaf category (no subcategories)
     */
    public boolean isLeafCategory() {
        return subCategories == null || subCategories.isEmpty();
    }
    
    /**
     * Get the full category path (e.g., "Electronics > Computers > Laptops")
     */
    public String getFullPath() {
        if (parentCategory == null) {
            return name;
        }
        return parentCategory.getFullPath() + " > " + name;
    }
    
    /**
     * Get the category depth level (0 for root)
     */
    public int getDepthLevel() {
        if (parentCategory == null) {
            return 0;
        }
        return parentCategory.getDepthLevel() + 1;
    }
    
    /**
     * Check if this category is ancestor of another
     */
    public boolean isAncestorOf(InventoryCategory other) {
        if (other == null || other.parentCategory == null) {
            return false;
        }
        if (other.parentCategory.equals(this)) {
            return true;
        }
        return isAncestorOf(other.parentCategory);
    }
    
    /**
     * Check if this category is descendant of another
     */
    public boolean isDescendantOf(InventoryCategory other) {
        if (other == null || parentCategory == null) {
            return false;
        }
        if (parentCategory.equals(other)) {
            return true;
        }
        return parentCategory.isDescendantOf(other);
    }
    
    /**
     * Get total item count including subcategories
     */
    public int getTotalItemCount() {
        int count = items != null ? items.size() : 0;
        if (subCategories != null) {
            for (InventoryCategory subCategory : subCategories) {
                count += subCategory.getTotalItemCount();
            }
        }
        return count;
    }
    
    /**
     * Get all items including from subcategories
     */
    public Set<InventoryItem> getAllItems() {
        Set<InventoryItem> allItems = new HashSet<>();
        if (items != null) {
            allItems.addAll(items);
        }
        if (subCategories != null) {
            for (InventoryCategory subCategory : subCategories) {
                allItems.addAll(subCategory.getAllItems());
            }
        }
        return allItems;
    }
    
    /**
     * Activate this category and optionally its subcategories
     */
    public void activate(boolean includeSubCategories) {
        this.isActive = true;
        if (includeSubCategories && subCategories != null) {
            for (InventoryCategory subCategory : subCategories) {
                subCategory.activate(true);
            }
        }
    }
    
    /**
     * Deactivate this category and optionally its subcategories
     */
    public void deactivate(boolean includeSubCategories) {
        this.isActive = false;
        if (includeSubCategories && subCategories != null) {
            for (InventoryCategory subCategory : subCategories) {
                subCategory.deactivate(true);
            }
        }
    }
    
    /**
     * Get display name (uses Arabic if available based on locale)
     */
    public String getDisplayName(String locale) {
        if ("ar".equals(locale) && nameAr != null && !nameAr.trim().isEmpty()) {
            return nameAr;
        }
        return name;
    }
    
    // ========== Getters and Setters ==========
    
    public Company getCompany() {
        return company;
    }
    
    public void setCompany(Company company) {
        this.company = company;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getNameAr() {
        return nameAr;
    }
    
    public void setNameAr(String nameAr) {
        this.nameAr = nameAr;
    }
    
    public InventoryCategory getParentCategory() {
        return parentCategory;
    }
    
    public void setParentCategory(InventoryCategory parentCategory) {
        this.parentCategory = parentCategory;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Set<InventoryCategory> getSubCategories() {
        return subCategories;
    }
    
    public void setSubCategories(Set<InventoryCategory> subCategories) {
        this.subCategories = subCategories;
    }
    
    public Set<InventoryItem> getItems() {
        return items;
    }
    
    public void setItems(Set<InventoryItem> items) {
        this.items = items;
    }
    
    // ========== toString, equals, hashCode ==========
    
    @Override
    public String toString() {
        return String.format("InventoryCategory[id=%s, name=%s, parent=%s, active=%s]",
            getId(), name, 
            parentCategory != null ? parentCategory.getName() : "null",
            isActive);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryCategory)) return false;
        if (!super.equals(o)) return false;
        InventoryCategory that = (InventoryCategory) o;
        return Objects.equals(company, that.company) &&
               Objects.equals(name, that.name) &&
               Objects.equals(parentCategory, that.parentCategory);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), company, name, parentCategory);
    }
}