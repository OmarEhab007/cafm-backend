package com.cafm.cafmbackend.shared.enums;

/**
 * Inventory Transaction Type enumeration for tracking inventory movements.
 * Maps to PostgreSQL enum type 'inventory_transaction_type'.
 */
public enum InventoryTransactionType {
    // Primary transaction types
    RECEIPT("Receipt", "Incoming inventory from purchase or supplier", 1),
    ISSUE("Issue", "Outgoing inventory for work order or usage", -1),
    ADJUSTMENT("Adjustment", "Manual inventory adjustment", 0),
    TRANSFER("Transfer", "Transfer between locations", 0),
    RETURN("Return", "Return from work order or to supplier", 1),
    DAMAGE("Damage", "Damaged or lost inventory", -1),
    INITIAL("Initial", "Initial stock entry", 1),
    
    // Additional specific transaction types for clearer business logic
    STOCK_IN("Stock In", "General incoming stock", 1),
    STOCK_OUT("Stock Out", "General outgoing stock", -1),
    INITIAL_STOCK("Initial Stock", "Initial stock entry", 1),
    TRANSFER_IN("Transfer In", "Incoming transfer from another location", 1),
    TRANSFER_OUT("Transfer Out", "Outgoing transfer to another location", -1),
    ADJUSTMENT_IN("Adjustment In", "Positive inventory adjustment", 1),
    ADJUSTMENT_OUT("Adjustment Out", "Negative inventory adjustment", -1);
    
    private final String displayName;
    private final String description;
    private final int stockImpact; // 1 = increase, -1 = decrease, 0 = neutral
    
    InventoryTransactionType(String displayName, String description, int stockImpact) {
        this.displayName = displayName;
        this.description = description;
        this.stockImpact = stockImpact;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getStockImpact() {
        return stockImpact;
    }
    
    /**
     * Check if this transaction increases stock
     */
    public boolean increasesStock() {
        return stockImpact > 0;
    }
    
    /**
     * Check if this transaction decreases stock
     */
    public boolean decreasesStock() {
        return stockImpact < 0;
    }
    
    /**
     * Check if this transaction is neutral (transfer)
     */
    public boolean isNeutral() {
        return stockImpact == 0;
    }
    
    /**
     * Check if this is a manual transaction
     */
    public boolean isManual() {
        return this == ADJUSTMENT || this == INITIAL;
    }
    
    /**
     * Check if this requires approval
     */
    public boolean requiresApproval() {
        return this == ADJUSTMENT || this == DAMAGE;
    }
    
    /**
     * Get icon name for UI display
     */
    public String getIconName() {
        switch (this) {
            case RECEIPT:
                return "arrow-down";
            case ISSUE:
                return "arrow-up";
            case ADJUSTMENT:
                return "edit";
            case TRANSFER:
                return "swap";
            case RETURN:
                return "undo";
            case DAMAGE:
                return "alert-triangle";
            case INITIAL:
                return "database";
            default:
                return "activity";
        }
    }
    
    /**
     * Get color code for UI display
     */
    public String getColorCode() {
        switch (this) {
            case RECEIPT:
            case RETURN:
            case INITIAL:
                return "#28a745"; // Green - incoming
            case ISSUE:
            case DAMAGE:
                return "#dc3545"; // Red - outgoing
            case ADJUSTMENT:
                return "#ffc107"; // Yellow - manual
            case TRANSFER:
                return "#17a2b8"; // Cyan - neutral
            default:
                return "#6c757d"; // Gray
        }
    }
    
    /**
     * Get database value for PostgreSQL enum
     */
    public String toDatabaseValue() {
        return name().toLowerCase();
    }
    
    /**
     * Parse from database value
     */
    public static InventoryTransactionType fromDatabaseValue(String value) {
        if (value == null) return null;
        return valueOf(value.toUpperCase());
    }
}