package com.cafm.cafmbackend.dto.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class for pagination conversions.
 */
public final class PageUtils {
    
    private PageUtils() {
        // Utility class
    }
    
    /**
     * Convert Spring Data Page to PageResponse
     */
    public static <T> PageResponse<T> toPageResponse(Page<T> page) {
        List<PageResponse.PageMetadata.SortInfo> sortInfo = extractSortInfo(page.getSort());
        
        return PageResponse.<T>builder()
            .content(page.getContent())
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .sorted(sortInfo)
            .build();
    }
    
    /**
     * Convert Spring Data Page to PageResponse with mapping
     */
    public static <T, U> PageResponse<U> toPageResponse(Page<T> page, Function<T, U> mapper) {
        List<U> mappedContent = page.getContent().stream()
            .map(mapper)
            .collect(Collectors.toList());
        
        List<PageResponse.PageMetadata.SortInfo> sortInfo = extractSortInfo(page.getSort());
        
        return PageResponse.<U>builder()
            .content(mappedContent)
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .sorted(sortInfo)
            .build();
    }
    
    /**
     * Convert PageRequest to Spring Data Pageable
     */
    public static Pageable toPageable(PageRequest request) {
        Sort sort = createSort(request.sort());
        return org.springframework.data.domain.PageRequest.of(
            request.page(),
            request.size(),
            sort
        );
    }
    
    /**
     * Convert PageRequest with default sort
     */
    public static Pageable toPageable(PageRequest request, Sort defaultSort) {
        Sort sort = request.hasSorting() ? createSort(request.sort()) : defaultSort;
        return org.springframework.data.domain.PageRequest.of(
            request.page(),
            request.size(),
            sort
        );
    }
    
    /**
     * Create Spring Data Sort from PageRequest sort orders
     */
    private static Sort createSort(List<PageRequest.SortOrder> sortOrders) {
        if (sortOrders == null || sortOrders.isEmpty()) {
            return Sort.unsorted();
        }
        
        List<Sort.Order> orders = sortOrders.stream()
            .map(sortOrder -> {
                Sort.Direction direction = sortOrder.direction() == PageRequest.SortOrder.Direction.ASC
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
                return new Sort.Order(direction, sortOrder.field());
            })
            .collect(Collectors.toList());
        
        return Sort.by(orders);
    }
    
    /**
     * Extract sort information from Spring Data Sort
     */
    private static List<PageResponse.PageMetadata.SortInfo> extractSortInfo(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return List.of();
        }
        
        return sort.stream()
            .map(order -> new PageResponse.PageMetadata.SortInfo(
                order.getProperty(),
                order.getDirection().name()
            ))
            .collect(Collectors.toList());
    }
    
    /**
     * Create a default PageRequest
     */
    public static PageRequest defaultPageRequest() {
        return PageRequest.of(0, 20);
    }
    
    /**
     * Create a PageRequest with max size
     */
    public static PageRequest maxPageRequest() {
        return PageRequest.of(0, 100);
    }
    
    /**
     * Validate and sanitize PageRequest
     */
    public static PageRequest sanitize(PageRequest request) {
        int page = Math.max(0, request.page());
        int size = Math.min(Math.max(1, request.size()), 100);
        return new PageRequest(page, size, request.sort(), request.search(), request.filters());
    }
}