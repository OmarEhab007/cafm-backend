package com.cafm.cafmbackend.shared.enums;

import java.math.BigDecimal;

/**
 * Subscription plan enumeration for SaaS pricing tiers.
 * 
 * Architecture: SaaS subscription model with tiered pricing
 * Pattern: Strategy pattern for feature and limit management
 */
public enum SubscriptionPlan {
    
    /**
     * Free tier with basic features
     */
    FREE("FREE", "Free", "Basic CAFM features for small teams", 
         BigDecimal.ZERO, 10, 5, 3, 15, 1),
    
    /**
     * Basic paid plan for small organizations
     */
    BASIC("BASIC", "Basic", "Standard CAFM features for growing teams", 
          new BigDecimal("99.00"), 25, 10, 8, 40, 5),
    
    /**
     * Professional plan for medium organizations
     */
    PROFESSIONAL("PROFESSIONAL", "Professional", "Advanced CAFM features with analytics", 
                new BigDecimal("299.00"), 100, 50, 25, 150, 20),
    
    /**
     * Enterprise plan for large organizations
     */
    ENTERPRISE("ENTERPRISE", "Enterprise", "Full CAFM suite with custom integrations", 
               new BigDecimal("999.00"), 500, 200, 100, 1000, 100);
    
    private final String dbValue;
    private final String displayName;
    private final String description;
    private final BigDecimal monthlyPrice;
    private final int maxUsers;
    private final int maxSchools;
    private final int maxSupervisors;
    private final int maxTechnicians;
    private final int maxStorageGb;
    
    SubscriptionPlan(String dbValue, String displayName, String description, BigDecimal monthlyPrice,
                    int maxUsers, int maxSchools, int maxSupervisors, 
                    int maxTechnicians, int maxStorageGb) {
        this.dbValue = dbValue;
        this.displayName = displayName;
        this.description = description;
        this.monthlyPrice = monthlyPrice;
        this.maxUsers = maxUsers;
        this.maxSchools = maxSchools;
        this.maxSupervisors = maxSupervisors;
        this.maxTechnicians = maxTechnicians;
        this.maxStorageGb = maxStorageGb;
    }
    
    public String getDbValue() {
        return dbValue;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }
    
    public int getMaxUsers() {
        return maxUsers;
    }
    
    public int getMaxSchools() {
        return maxSchools;
    }
    
    public int getMaxSupervisors() {
        return maxSupervisors;
    }
    
    public int getMaxTechnicians() {
        return maxTechnicians;
    }
    
    public int getMaxStorageGb() {
        return maxStorageGb;
    }
    
    /**
     * Check if this is a free plan
     */
    public boolean isFree() {
        return this == FREE;
    }
    
    /**
     * Check if this is a paid plan
     */
    public boolean isPaid() {
        return !isFree();
    }
    
    /**
     * Check if plan supports advanced features
     */
    public boolean hasAdvancedFeatures() {
        return this == PROFESSIONAL || this == ENTERPRISE;
    }
    
    /**
     * Check if plan supports enterprise features
     */
    public boolean hasEnterpriseFeatures() {
        return this == ENTERPRISE;
    }
    
    /**
     * Get annual price with discount
     */
    public BigDecimal getAnnualPrice(double discountPercentage) {
        BigDecimal annualPrice = monthlyPrice.multiply(BigDecimal.valueOf(12));
        if (discountPercentage > 0) {
            BigDecimal discount = annualPrice.multiply(BigDecimal.valueOf(discountPercentage / 100));
            annualPrice = annualPrice.subtract(discount);
        }
        return annualPrice;
    }
    
    /**
     * Get annual price with 20% discount (standard)
     */
    public BigDecimal getAnnualPrice() {
        return getAnnualPrice(20.0);
    }
    
    /**
     * Check if upgrade is available to another plan
     */
    public boolean canUpgradeTo(SubscriptionPlan targetPlan) {
        return this.ordinal() < targetPlan.ordinal();
    }
    
    /**
     * Check if downgrade is available to another plan
     */
    public boolean canDowngradeTo(SubscriptionPlan targetPlan) {
        return this.ordinal() > targetPlan.ordinal();
    }
    
    /**
     * Get next higher plan for upgrades
     */
    public SubscriptionPlan getNextUpgrade() {
        return switch (this) {
            case FREE -> BASIC;
            case BASIC -> PROFESSIONAL;
            case PROFESSIONAL -> ENTERPRISE;
            case ENTERPRISE -> null; // Already at highest tier
        };
    }
    
    /**
     * Get available features for this plan
     */
    public String[] getFeatures() {
        return switch (this) {
            case FREE -> new String[]{
                "Basic maintenance tracking",
                "Up to " + maxUsers + " users",
                "Up to " + maxSchools + " schools",
                "Basic reporting",
                "Email support"
            };
            case BASIC -> new String[]{
                "All Free features",
                "Advanced maintenance workflows",
                "Up to " + maxUsers + " users", 
                "Up to " + maxSchools + " schools",
                "Technician assignments",
                "Mobile app access",
                "Priority email support"
            };
            case PROFESSIONAL -> new String[]{
                "All Basic features",
                "Analytics & insights",
                "Up to " + maxUsers + " users",
                "Up to " + maxSchools + " schools", 
                "Custom reporting",
                "API access",
                "Advanced user roles",
                "Phone & email support"
            };
            case ENTERPRISE -> new String[]{
                "All Professional features",
                "Unlimited custom fields",
                "Up to " + maxUsers + " users",
                "Up to " + maxSchools + " schools",
                "Custom integrations", 
                "SSO authentication",
                "Dedicated account manager",
                "24/7 priority support"
            };
        };
    }
    
    /**
     * Calculate overage cost for exceeding limits
     */
    public BigDecimal calculateOverageCost(int actualUsers, int actualSchools) {
        if (this == FREE) return BigDecimal.ZERO; // Free plan has hard limits
        
        BigDecimal overageCost = BigDecimal.ZERO;
        
        // User overage: $5 per additional user
        if (actualUsers > maxUsers) {
            int extraUsers = actualUsers - maxUsers;
            overageCost = overageCost.add(BigDecimal.valueOf(extraUsers * 5));
        }
        
        // School overage: $10 per additional school
        if (actualSchools > maxSchools) {
            int extraSchools = actualSchools - maxSchools;
            overageCost = overageCost.add(BigDecimal.valueOf(extraSchools * 10));
        }
        
        return overageCost;
    }
    
    /**
     * Get SubscriptionPlan from database value
     */
    public static SubscriptionPlan fromDbValue(String dbValue) {
        for (SubscriptionPlan plan : values()) {
            if (plan.dbValue.equals(dbValue)) {
                return plan;
            }
        }
        throw new IllegalArgumentException("Unknown subscription plan: " + dbValue);
    }
    
    @Override
    public String toString() {
        return displayName + " ($" + monthlyPrice + "/month)";
    }
}