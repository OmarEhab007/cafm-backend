package com.cafm.cafmbackend.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pagination response wrapper for API responses.
 * 
 * @param <T> The type of content in the page
 */
public record PageResponse<T>(
    List<T> content,
    PageMetadata metadata
) {
    /**
     * Page metadata information
     */
    public record PageMetadata(
        @JsonProperty("page")
        int page,
        
        @JsonProperty("size")
        int size,
        
        @JsonProperty("totalElements")
        long totalElements,
        
        @JsonProperty("totalPages")
        int totalPages,
        
        @JsonProperty("isFirst")
        boolean isFirst,
        
        @JsonProperty("isLast")
        boolean isLast,
        
        @JsonProperty("hasNext")
        boolean hasNext,
        
        @JsonProperty("hasPrevious")
        boolean hasPrevious,
        
        @JsonProperty("numberOfElements")
        int numberOfElements,
        
        @JsonProperty("isEmpty")
        boolean isEmpty,
        
        @JsonProperty("sorted")
        List<SortInfo> sorted,
        
        @JsonProperty("filtered")
        List<FilterInfo> filtered
    ) {
        /**
         * Sort information
         */
        public record SortInfo(
            String field,
            String direction
        ) {}
        
        /**
         * Filter information
         */
        public record FilterInfo(
            String field,
            String operator,
            Object value
        ) {}
        
        /**
         * Create metadata from basic pagination info
         */
        public static PageMetadata of(
            int page,
            int size,
            long totalElements,
            int numberOfElements
        ) {
            int totalPages = (int) Math.ceil((double) totalElements / size);
            return new PageMetadata(
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                page >= totalPages - 1,
                page < totalPages - 1,
                page > 0,
                numberOfElements,
                numberOfElements == 0,
                List.of(),
                List.of()
            );
        }
    }
    
    /**
     * Create page response from Spring Data Page
     */
    public static <T> PageResponse<T> of(
        List<T> content,
        int page,
        int size,
        long totalElements
    ) {
        return new PageResponse<>(
            content,
            PageMetadata.of(page, size, totalElements, content.size())
        );
    }
    
    /**
     * Create empty page response
     */
    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(
            List.of(),
            PageMetadata.of(page, size, 0, 0)
        );
    }
    
    /**
     * Create page response with single element
     */
    public static <T> PageResponse<T> singleton(T element) {
        return new PageResponse<>(
            List.of(element),
            PageMetadata.of(0, 1, 1, 1)
        );
    }
    
    /**
     * Transform content using a mapper function
     */
    public <U> PageResponse<U> map(Function<T, U> mapper) {
        List<U> mappedContent = content.stream()
            .map(mapper)
            .collect(Collectors.toList());
        return new PageResponse<>(mappedContent, metadata);
    }
    
    /**
     * Check if page has content
     */
    public boolean hasContent() {
        return !content.isEmpty();
    }
    
    /**
     * Get total number of elements
     */
    public long getTotalElements() {
        return metadata.totalElements();
    }
    
    /**
     * Get total number of pages
     */
    public int getTotalPages() {
        return metadata.totalPages();
    }
    
    /**
     * Get current page number
     */
    public int getPage() {
        return metadata.page();
    }
    
    /**
     * Get page size
     */
    public int getSize() {
        return metadata.size();
    }
    
    /**
     * Check if this is the first page
     */
    public boolean isFirst() {
        return metadata.isFirst();
    }
    
    /**
     * Check if this is the last page
     */
    public boolean isLast() {
        return metadata.isLast();
    }
    
    /**
     * Builder for complex page responses
     */
    public static class Builder<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private List<PageMetadata.SortInfo> sorted = List.of();
        private List<PageMetadata.FilterInfo> filtered = List.of();
        
        public Builder<T> content(List<T> content) {
            this.content = content;
            return this;
        }
        
        public Builder<T> page(int page) {
            this.page = page;
            return this;
        }
        
        public Builder<T> size(int size) {
            this.size = size;
            return this;
        }
        
        public Builder<T> totalElements(long totalElements) {
            this.totalElements = totalElements;
            return this;
        }
        
        public Builder<T> sorted(List<PageMetadata.SortInfo> sorted) {
            this.sorted = sorted;
            return this;
        }
        
        public Builder<T> filtered(List<PageMetadata.FilterInfo> filtered) {
            this.filtered = filtered;
            return this;
        }
        
        public Builder<T> addSort(String field, String direction) {
            if (this.sorted.isEmpty()) {
                this.sorted = new java.util.ArrayList<>();
            }
            this.sorted.add(new PageMetadata.SortInfo(field, direction));
            return this;
        }
        
        public Builder<T> addFilter(String field, String operator, Object value) {
            if (this.filtered.isEmpty()) {
                this.filtered = new java.util.ArrayList<>();
            }
            this.filtered.add(new PageMetadata.FilterInfo(field, operator, value));
            return this;
        }
        
        public PageResponse<T> build() {
            int totalPages = (int) Math.ceil((double) totalElements / size);
            int numberOfElements = content != null ? content.size() : 0;
            
            PageMetadata metadata = new PageMetadata(
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                page >= totalPages - 1,
                page < totalPages - 1,
                page > 0,
                numberOfElements,
                numberOfElements == 0,
                sorted,
                filtered
            );
            
            return new PageResponse<>(
                content != null ? content : List.of(),
                metadata
            );
        }
    }
    
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
}