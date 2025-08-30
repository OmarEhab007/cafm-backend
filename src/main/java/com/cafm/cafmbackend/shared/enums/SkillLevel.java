package com.cafm.cafmbackend.shared.enums;

/**
 * Enumeration of skill levels for technicians.
 * 
 * Architecture: Defines competency levels for task assignment
 * Pattern: Enum with ordinal-based comparison support
 */
public enum SkillLevel {
    /**
     * Beginner level - basic tasks under supervision
     */
    BEGINNER("Beginner", "Entry level, basic tasks with supervision", 1),
    
    /**
     * Intermediate level - moderate complexity tasks
     */
    INTERMEDIATE("Intermediate", "Moderate complexity tasks with minimal supervision", 2),
    
    /**
     * Advanced level - complex tasks independently
     */
    ADVANCED("Advanced", "Complex tasks performed independently", 3),
    
    /**
     * Expert level - most complex tasks and mentoring
     */
    EXPERT("Expert", "Expert level, complex tasks and mentoring others", 4),
    
    /**
     * Master level - specialized expertise and leadership
     */
    MASTER("Master", "Master craftsman with specialized expertise", 5);
    
    private final String displayName;
    private final String description;
    private final int level;
    
    SkillLevel(String displayName, String description, int level) {
        this.displayName = displayName;
        this.description = description;
        this.level = level;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getLevel() {
        return level;
    }
    
    /**
     * Check if this skill level is higher than or equal to another
     */
    public boolean isAtLeast(SkillLevel other) {
        return this.level >= other.level;
    }
    
    /**
     * Check if this skill level is higher than another
     */
    public boolean isHigherThan(SkillLevel other) {
        return this.level > other.level;
    }
    
    /**
     * Get skill level by display name
     */
    public static SkillLevel fromDisplayName(String displayName) {
        for (SkillLevel skill : values()) {
            if (skill.displayName.equalsIgnoreCase(displayName)) {
                return skill;
            }
        }
        throw new IllegalArgumentException("No skill level found with display name: " + displayName);
    }
    
    /**
     * Get minimum skill level required for complex tasks
     */
    public static SkillLevel getMinimumForComplexTasks() {
        return ADVANCED;
    }
    
    /**
     * Get minimum skill level required for supervision
     */
    public static SkillLevel getMinimumForSupervision() {
        return EXPERT;
    }
}