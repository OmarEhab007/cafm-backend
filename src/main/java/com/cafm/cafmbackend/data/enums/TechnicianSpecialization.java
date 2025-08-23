package com.cafm.cafmbackend.data.enums;

/**
 * Enumeration of technician specializations.
 * 
 * Architecture: Domain-driven design for maintenance categories
 * Pattern: Enum for type safety and validation
 */
public enum TechnicianSpecialization {
    /**
     * Electrical systems and wiring
     */
    ELECTRICAL("Electrical Systems", "Electrical installations, repairs, and maintenance"),
    
    /**
     * Plumbing and water systems
     */
    PLUMBING("Plumbing", "Water supply, drainage, and plumbing fixtures"),
    
    /**
     * Heating, Ventilation, and Air Conditioning
     */
    HVAC("HVAC", "Heating, ventilation, and air conditioning systems"),
    
    /**
     * Carpentry and woodwork
     */
    CARPENTRY("Carpentry", "Wood construction, furniture repair, and carpentry work"),
    
    /**
     * Painting and decoration
     */
    PAINTING("Painting", "Interior and exterior painting, wall finishes"),
    
    /**
     * General maintenance tasks
     */
    GENERAL_MAINTENANCE("General Maintenance", "Basic repairs and general facility maintenance"),
    
    /**
     * Landscaping and grounds
     */
    LANDSCAPING("Landscaping", "Garden maintenance, grounds keeping, outdoor spaces"),
    
    /**
     * Cleaning and janitorial
     */
    CLEANING("Cleaning", "Janitorial services, deep cleaning, sanitation"),
    
    /**
     * IT support and technology
     */
    IT_SUPPORT("IT Support", "Computer systems, network equipment, AV systems"),
    
    /**
     * Security systems
     */
    SECURITY_SYSTEMS("Security Systems", "Alarm systems, cameras, access control"),
    
    /**
     * Roofing work
     */
    ROOFING("Roofing", "Roof repairs, waterproofing, gutter maintenance"),
    
    /**
     * Flooring installation and repair
     */
    FLOORING("Flooring", "Floor installation, repair, and maintenance"),
    
    /**
     * Masonry and concrete work
     */
    MASONRY("Masonry", "Brick work, concrete repairs, stonework"),
    
    /**
     * Fire safety systems
     */
    FIRE_SAFETY("Fire Safety", "Fire extinguishers, alarms, safety equipment"),
    
    /**
     * Pest control
     */
    PEST_CONTROL("Pest Control", "Pest management and prevention services");
    
    private final String displayName;
    private final String description;
    
    TechnicianSpecialization(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get specialization by display name
     */
    public static TechnicianSpecialization fromDisplayName(String displayName) {
        for (TechnicianSpecialization spec : values()) {
            if (spec.displayName.equalsIgnoreCase(displayName)) {
                return spec;
            }
        }
        throw new IllegalArgumentException("No specialization found with display name: " + displayName);
    }
    
    /**
     * Check if this specialization is technical (requires special skills)
     */
    public boolean isTechnical() {
        return switch (this) {
            case ELECTRICAL, PLUMBING, HVAC, IT_SUPPORT, SECURITY_SYSTEMS, 
                 FIRE_SAFETY, ROOFING, MASONRY -> true;
            case GENERAL_MAINTENANCE, CLEANING, LANDSCAPING, PAINTING, 
                 CARPENTRY, FLOORING, PEST_CONTROL -> false;
        };
    }
    
    /**
     * Check if this specialization requires certification
     */
    public boolean requiresCertification() {
        return switch (this) {
            case ELECTRICAL, PLUMBING, HVAC, FIRE_SAFETY, PEST_CONTROL -> true;
            case IT_SUPPORT, SECURITY_SYSTEMS, ROOFING, MASONRY, 
                 CARPENTRY, FLOORING, PAINTING, LANDSCAPING, CLEANING, 
                 GENERAL_MAINTENANCE -> false;
        };
    }
}