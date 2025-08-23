package com.cafm.cafmbackend.mappers.config;

/**
 * Base interface for all entity mappers.
 * Provides standard contract for entity-DTO conversions.
 * 
 * @param <E> Entity type
 * @param <REQ> Request DTO type
 * @param <RES> Response DTO type
 */
public interface BaseMapper<E, REQ, RES> {
    
    /**
     * Convert request DTO to entity.
     */
    E toEntity(REQ request);
    
    /**
     * Convert entity to response DTO.
     */
    RES toResponse(E entity);
}