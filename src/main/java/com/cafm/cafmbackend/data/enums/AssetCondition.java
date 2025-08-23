package com.cafm.cafmbackend.data.enums;

/**
 * Asset Condition enumeration for tracking physical state of assets.
 * Used for condition assessments and maintenance decisions.
 */
public enum AssetCondition {
    EXCELLENT("Excellent", "Like new, minimal wear", 100, "#28a745"),
    GOOD("Good", "Normal wear, fully functional", 80, "#8bc34a"),
    FAIR("Fair", "Moderate wear, functional with minor issues", 60, "#ffc107"),
    POOR("Poor", "Significant wear, needs repair", 40, "#ff9800"),
    UNUSABLE("Unusable", "Non-functional, requires major repair or replacement", 20, "#dc3545");
    
    private final String displayName;
    private final String description;
    private final int conditionScore;
    private final String colorCode;
    
    AssetCondition(String displayName, String description, int conditionScore, String colorCode) {
        this.displayName = displayName;
        this.description = description;
        this.conditionScore = conditionScore;
        this.colorCode = colorCode;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getConditionScore() {
        return conditionScore;
    }
    
    public String getColorCode() {
        return colorCode;
    }
    
    /**
     * Check if asset needs maintenance
     */
    public boolean needsMaintenance() {
        return this == FAIR || this == POOR || this == UNUSABLE;
    }
    
    /**
     * Check if asset needs replacement
     */
    public boolean needsReplacement() {
        return this == POOR || this == UNUSABLE;
    }
    
    /**
     * Check if asset is usable
     */
    public boolean isUsable() {
        return this != UNUSABLE;
    }
    
    /**
     * Get maintenance priority based on condition
     */
    public String getMaintenancePriority() {
        switch (this) {
            case UNUSABLE:
                return "EMERGENCY";
            case POOR:
                return "HIGH";
            case FAIR:
                return "MEDIUM";
            case GOOD:
                return "LOW";
            case EXCELLENT:
                return "NONE";
            default:
                return "LOW";
        }
    }
    
    /**
     * Get recommended action
     */
    public String getRecommendedAction() {
        switch (this) {
            case EXCELLENT:
                return "Regular inspection only";
            case GOOD:
                return "Preventive maintenance";
            case FAIR:
                return "Schedule maintenance";
            case POOR:
                return "Urgent repair needed";
            case UNUSABLE:
                return "Replace or major overhaul";
            default:
                return "Assess condition";
        }
    }
    
    /**
     * Calculate depreciation factor based on condition
     */
    public double getDepreciationFactor() {
        return conditionScore / 100.0;
    }
    
    /**
     * Get condition from score
     */
    public static AssetCondition fromScore(int score) {
        if (score >= 90) return EXCELLENT;
        if (score >= 70) return GOOD;
        if (score >= 50) return FAIR;
        if (score >= 30) return POOR;
        return UNUSABLE;
    }
    
    /**
     * Get icon name for UI
     */
    public String getIconName() {
        switch (this) {
            case EXCELLENT:
                return "star";
            case GOOD:
                return "thumbs-up";
            case FAIR:
                return "minus-circle";
            case POOR:
                return "thumbs-down";
            case UNUSABLE:
                return "x-circle";
            default:
                return "help-circle";
        }
    }
}