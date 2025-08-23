package com.cafm.cafmbackend.dto.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * Pagination request DTO for API endpoints.
 * Supports sorting, filtering, and pagination parameters.
 */
public record PageRequest(
    @Min(value = 0, message = "Page number cannot be negative")
    Integer page,
    
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    Integer size,
    
    List<SortOrder> sort,
    
    String search,
    
    List<FilterCriteria> filters
) {
    /**
     * Default constructor with sensible defaults
     */
    public PageRequest {
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (sort == null) sort = List.of();
        if (filters == null) filters = List.of();
    }
    
    /**
     * Sort order specification
     */
    public record SortOrder(
        String field,
        Direction direction
    ) {
        public enum Direction {
            ASC, DESC
        }
        
        /**
         * Create ascending sort order
         */
        public static SortOrder asc(String field) {
            return new SortOrder(field, Direction.ASC);
        }
        
        /**
         * Create descending sort order
         */
        public static SortOrder desc(String field) {
            return new SortOrder(field, Direction.DESC);
        }
    }
    
    /**
     * Filter criteria for dynamic filtering
     */
    public record FilterCriteria(
        String field,
        FilterOperator operator,
        Object value,
        Object valueTo // For BETWEEN operator
    ) {
        public enum FilterOperator {
            EQUALS,
            NOT_EQUALS,
            CONTAINS,
            NOT_CONTAINS,
            STARTS_WITH,
            ENDS_WITH,
            GREATER_THAN,
            GREATER_THAN_OR_EQUAL,
            LESS_THAN,
            LESS_THAN_OR_EQUAL,
            BETWEEN,
            IN,
            NOT_IN,
            IS_NULL,
            IS_NOT_NULL
        }
        
        /**
         * Constructor for single value filters
         */
        public FilterCriteria(String field, FilterOperator operator, Object value) {
            this(field, operator, value, null);
        }
        
        /**
         * Factory methods for common filters
         */
        public static FilterCriteria equals(String field, Object value) {
            return new FilterCriteria(field, FilterOperator.EQUALS, value);
        }
        
        public static FilterCriteria contains(String field, String value) {
            return new FilterCriteria(field, FilterOperator.CONTAINS, value);
        }
        
        public static FilterCriteria between(String field, Object from, Object to) {
            return new FilterCriteria(field, FilterOperator.BETWEEN, from, to);
        }
        
        public static FilterCriteria in(String field, List<?> values) {
            return new FilterCriteria(field, FilterOperator.IN, values);
        }
        
        public static FilterCriteria isNull(String field) {
            return new FilterCriteria(field, FilterOperator.IS_NULL, null);
        }
        
        public static FilterCriteria greaterThan(String field, Object value) {
            return new FilterCriteria(field, FilterOperator.GREATER_THAN, value);
        }
    }
    
    /**
     * Create simple page request with just page and size
     */
    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, null, null, null);
    }
    
    /**
     * Create page request with single sort
     */
    public static PageRequest of(int page, int size, String sortField, SortOrder.Direction direction) {
        return new PageRequest(
            page, 
            size, 
            List.of(new SortOrder(sortField, direction)),
            null,
            null
        );
    }
    
    /**
     * Create page request with search
     */
    public static PageRequest ofSearch(int page, int size, String search) {
        return new PageRequest(page, size, null, search, null);
    }
    
    /**
     * Calculate offset for database queries
     */
    public int getOffset() {
        return page * size;
    }
    
    /**
     * Check if request has sorting
     */
    public boolean hasSorting() {
        return sort != null && !sort.isEmpty();
    }
    
    /**
     * Check if request has filters
     */
    public boolean hasFilters() {
        return filters != null && !filters.isEmpty();
    }
    
    /**
     * Check if request has search
     */
    public boolean hasSearch() {
        return search != null && !search.isBlank();
    }
}