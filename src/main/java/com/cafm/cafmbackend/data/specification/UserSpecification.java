package com.cafm.cafmbackend.data.specification;

import com.cafm.cafmbackend.data.entity.User;
import com.cafm.cafmbackend.data.enums.UserStatus;
import com.cafm.cafmbackend.data.enums.UserType;
import com.cafm.cafmbackend.data.enums.TechnicianSpecialization;
import com.cafm.cafmbackend.data.enums.SkillLevel;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Specifications for User entity queries.
 * 
 * Architecture: Provides type-safe query building for complex filters
 * Pattern: Specification pattern for composable queries
 * Java 23: Uses pattern matching for cleaner predicate building
 */
public class UserSpecification {
    
    /**
     * Filter by user type
     */
    public static Specification<User> hasUserType(UserType userType) {
        return (root, query, criteriaBuilder) -> 
            userType == null ? null : criteriaBuilder.equal(root.get("userType"), userType);
    }
    
    /**
     * Filter by user status
     */
    public static Specification<User> hasStatus(UserStatus status) {
        return (root, query, criteriaBuilder) -> 
            status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }
    
    /**
     * Filter active users only
     */
    public static Specification<User> isActive() {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.and(
                criteriaBuilder.isTrue(root.get("isActive")),
                criteriaBuilder.isNull(root.get("deletedAt"))
            );
    }
    
    /**
     * Filter by email verification status
     */
    public static Specification<User> isEmailVerified(Boolean verified) {
        return (root, query, criteriaBuilder) -> 
            verified == null ? null : criteriaBuilder.equal(root.get("emailVerified"), verified);
    }
    
    /**
     * Filter by locked status
     */
    public static Specification<User> isLocked(Boolean locked) {
        return (root, query, criteriaBuilder) -> 
            locked == null ? null : criteriaBuilder.equal(root.get("isLocked"), locked);
    }
    
    /**
     * Filter by department
     */
    public static Specification<User> inDepartment(String department) {
        return (root, query, criteriaBuilder) -> 
            department == null || department.isEmpty() ? null : 
                criteriaBuilder.equal(root.get("department"), department);
    }
    
    /**
     * Filter by city
     */
    public static Specification<User> inCity(String city) {
        return (root, query, criteriaBuilder) -> 
            city == null || city.isEmpty() ? null : 
                criteriaBuilder.equal(root.get("city"), city);
    }
    
    /**
     * Filter by performance rating range
     */
    public static Specification<User> hasPerformanceRatingBetween(Double min, Double max) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (min != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("performanceRating"), min));
            }
            
            if (max != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("performanceRating"), max));
            }
            
            return predicates.isEmpty() ? null : 
                criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Filter by productivity score range
     */
    public static Specification<User> hasProductivityScoreBetween(Integer min, Integer max) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (min != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("productivityScore"), min));
            }
            
            if (max != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("productivityScore"), max));
            }
            
            return predicates.isEmpty() ? null : 
                criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Filter by last login date range
     */
    public static Specification<User> lastLoginBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("lastLoginAt"), from));
            }
            
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("lastLoginAt"), to));
            }
            
            return predicates.isEmpty() ? null : 
                criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Filter users not logged in for specified days
     */
    public static Specification<User> notLoggedInForDays(int days) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.or(
                criteriaBuilder.isNull(root.get("lastLoginAt")),
                criteriaBuilder.lessThan(root.get("lastLoginAt"), threshold)
            );
    }
    
    /**
     * Filter by creation date range
     */
    public static Specification<User> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("createdAt"), from));
            }
            
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("createdAt"), to));
            }
            
            return predicates.isEmpty() ? null : 
                criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Search by text in multiple fields
     */
    public static Specification<User> containsText(String searchText) {
        return (root, query, criteriaBuilder) -> {
            if (searchText == null || searchText.trim().isEmpty()) {
                return null;
            }
            
            String likePattern = "%" + searchText.toLowerCase() + "%";
            
            return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("employeeId")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), likePattern)
            );
        };
    }
    
    /**
     * Filter soft-deleted users
     */
    public static Specification<User> isDeleted() {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.isNotNull(root.get("deletedAt"));
    }
    
    /**
     * Filter non-deleted users
     */
    public static Specification<User> isNotDeleted() {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.isNull(root.get("deletedAt"));
    }
    
    /**
     * Filter users belonging to a specific company
     */
    public static Specification<User> belongsToCompany(UUID companyId) {
        return (root, query, criteriaBuilder) -> {
            if (companyId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("company").get("id"), companyId);
        };
    }
    
    /**
     * Search users by a search term (name, email, phone, employeeId)
     */
    public static Specification<User> hasSearchTerm(String searchTerm) {
        return (root, query, criteriaBuilder) -> {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            String likePattern = "%" + searchTerm.toLowerCase() + "%";
            return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("employeeId")), likePattern)
            );
        };
    }
    
    /**
     * Filter users deleted by specific user
     */
    public static Specification<User> deletedBy(UUID deletedBy) {
        return (root, query, criteriaBuilder) -> 
            deletedBy == null ? null : criteriaBuilder.equal(root.get("deletedBy"), deletedBy);
    }
    
    /**
     * Filter users in recycle bin (deleted within 30 days)
     */
    public static Specification<User> inRecycleBin() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.and(
                criteriaBuilder.isNotNull(root.get("deletedAt")),
                criteriaBuilder.greaterThan(root.get("deletedAt"), thirtyDaysAgo)
            );
    }
    
    /**
     * Filter users ready for purge (deleted more than 90 days ago)
     */
    public static Specification<User> readyForPurge() {
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.and(
                criteriaBuilder.isNotNull(root.get("deletedAt")),
                criteriaBuilder.lessThan(root.get("deletedAt"), ninetyDaysAgo)
            );
    }
    
    // ========== Technician Specialization Filters ==========
    
    /**
     * Filter by technician specialization
     */
    public static Specification<User> hasSpecialization(TechnicianSpecialization specialization) {
        return (root, query, criteriaBuilder) -> 
            specialization == null ? null : criteriaBuilder.equal(root.get("specialization"), specialization);
    }
    
    /**
     * Filter by skill level
     */
    public static Specification<User> hasSkillLevel(SkillLevel skillLevel) {
        return (root, query, criteriaBuilder) -> 
            skillLevel == null ? null : criteriaBuilder.equal(root.get("skillLevel"), skillLevel);
    }
    
    /**
     * Filter by minimum skill level (inclusive)
     */
    public static Specification<User> hasMinimumSkillLevel(SkillLevel minimumLevel) {
        return (root, query, criteriaBuilder) -> {
            if (minimumLevel == null) return null;
            
            return criteriaBuilder.or(
                criteriaBuilder.equal(root.get("skillLevel"), minimumLevel),
                minimumLevel == SkillLevel.BEGINNER ? 
                    criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("skillLevel"), SkillLevel.INTERMEDIATE),
                        criteriaBuilder.equal(root.get("skillLevel"), SkillLevel.ADVANCED),
                        criteriaBuilder.equal(root.get("skillLevel"), SkillLevel.EXPERT),
                        criteriaBuilder.equal(root.get("skillLevel"), SkillLevel.MASTER)
                    ) :
                minimumLevel == SkillLevel.INTERMEDIATE ?
                    criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("skillLevel"), SkillLevel.ADVANCED),
                        criteriaBuilder.equal(root.get("skillLevel"), SkillLevel.EXPERT),
                        criteriaBuilder.equal(root.get("skillLevel"), SkillLevel.MASTER)
                    ) :
                minimumLevel == SkillLevel.ADVANCED ?
                    criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("skillLevel"), SkillLevel.EXPERT),
                        criteriaBuilder.equal(root.get("skillLevel"), SkillLevel.MASTER)
                    ) :
                minimumLevel == SkillLevel.EXPERT ?
                    criteriaBuilder.equal(root.get("skillLevel"), SkillLevel.MASTER) :
                    null // MASTER is the highest, no higher levels
            );
        };
    }
    
    /**
     * Filter technicians available for assignment
     */
    public static Specification<User> isAvailableForAssignment() {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get("userType"), UserType.TECHNICIAN),
                criteriaBuilder.isTrue(root.get("isAvailableForAssignment")),
                criteriaBuilder.equal(root.get("status"), UserStatus.ACTIVE),
                criteriaBuilder.isTrue(root.get("isActive")),
                criteriaBuilder.isFalse(root.get("isLocked")),
                criteriaBuilder.isNull(root.get("deletedAt"))
            );
    }
    
    /**
     * Filter by years of experience range
     */
    public static Specification<User> hasExperienceBetween(Integer minYears, Integer maxYears) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (minYears != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("yearsOfExperience"), minYears));
            }
            
            if (maxYears != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("yearsOfExperience"), maxYears));
            }
            
            return predicates.isEmpty() ? null : 
                criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Filter by hourly rate range
     */
    public static Specification<User> hasHourlyRateBetween(Double minRate, Double maxRate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (minRate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("hourlyRate"), minRate));
            }
            
            if (maxRate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("hourlyRate"), maxRate));
            }
            
            return predicates.isEmpty() ? null : 
                criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Filter technicians with certifications
     */
    public static Specification<User> hasCertifications() {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get("userType"), UserType.TECHNICIAN),
                criteriaBuilder.isNotNull(root.get("certifications"))
            );
    }
    
    /**
     * Filter technicians by specialization and availability
     */
    public static Specification<User> isAvailableTechnicianWithSpecialization(TechnicianSpecialization specialization) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Must be technician
            predicates.add(criteriaBuilder.equal(root.get("userType"), UserType.TECHNICIAN));
            
            // Must have the required specialization
            if (specialization != null) {
                predicates.add(criteriaBuilder.equal(root.get("specialization"), specialization));
            }
            
            // Must be available
            predicates.add(criteriaBuilder.isTrue(root.get("isAvailableForAssignment")));
            predicates.add(criteriaBuilder.equal(root.get("status"), UserStatus.ACTIVE));
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));
            predicates.add(criteriaBuilder.isFalse(root.get("isLocked")));
            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Complex filter builder for advanced search
     */
    public static class Builder {
        private Specification<User> specification = Specification.where(null);
        
        public Builder withUserType(UserType userType) {
            if (userType != null) {
                specification = specification.and(hasUserType(userType));
            }
            return this;
        }
        
        public Builder withStatus(UserStatus status) {
            if (status != null) {
                specification = specification.and(hasStatus(status));
            }
            return this;
        }
        
        public Builder onlyActive() {
            specification = specification.and(isActive());
            return this;
        }
        
        public Builder withEmailVerified(Boolean verified) {
            if (verified != null) {
                specification = specification.and(isEmailVerified(verified));
            }
            return this;
        }
        
        public Builder withLocked(Boolean locked) {
            if (locked != null) {
                specification = specification.and(isLocked(locked));
            }
            return this;
        }
        
        public Builder inDepartment(String department) {
            if (department != null && !department.isEmpty()) {
                specification = specification.and(UserSpecification.inDepartment(department));
            }
            return this;
        }
        
        public Builder searchText(String text) {
            if (text != null && !text.trim().isEmpty()) {
                specification = specification.and(containsText(text));
            }
            return this;
        }
        
        public Builder excludeDeleted() {
            specification = specification.and(isNotDeleted());
            return this;
        }
        
        public Builder withSpecialization(TechnicianSpecialization specialization) {
            if (specialization != null) {
                specification = specification.and(hasSpecialization(specialization));
            }
            return this;
        }
        
        public Builder withSkillLevel(SkillLevel skillLevel) {
            if (skillLevel != null) {
                specification = specification.and(hasSkillLevel(skillLevel));
            }
            return this;
        }
        
        public Builder withMinimumSkillLevel(SkillLevel minimumLevel) {
            if (minimumLevel != null) {
                specification = specification.and(hasMinimumSkillLevel(minimumLevel));
            }
            return this;
        }
        
        public Builder availableForAssignment() {
            specification = specification.and(isAvailableForAssignment());
            return this;
        }
        
        public Builder withExperienceRange(Integer minYears, Integer maxYears) {
            if (minYears != null || maxYears != null) {
                specification = specification.and(hasExperienceBetween(minYears, maxYears));
            }
            return this;
        }
        
        public Builder withHourlyRateRange(Double minRate, Double maxRate) {
            if (minRate != null || maxRate != null) {
                specification = specification.and(hasHourlyRateBetween(minRate, maxRate));
            }
            return this;
        }
        
        public Builder withCertifications() {
            specification = specification.and(hasCertifications());
            return this;
        }
        
        public Builder availableTechnicianWithSpecialization(TechnicianSpecialization specialization) {
            specification = specification.and(isAvailableTechnicianWithSpecialization(specialization));
            return this;
        }
        
        public Specification<User> build() {
            return specification;
        }
    }
}