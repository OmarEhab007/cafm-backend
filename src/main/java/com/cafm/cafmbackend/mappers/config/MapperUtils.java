package com.cafm.cafmbackend.mappers.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for common mapper conversions.
 * Provides reusable methods for type conversions and transformations.
 */
public final class MapperUtils {
    
    private MapperUtils() {
        // Utility class
    }
    
    // ========== UUID Conversions ==========
    
    public static String uuidToString(UUID uuid) {
        return uuid != null ? uuid.toString() : null;
    }
    
    public static UUID stringToUuid(String str) {
        return str != null && !str.isEmpty() ? UUID.fromString(str) : null;
    }
    
    public static List<String> uuidsToStrings(Collection<UUID> uuids) {
        if (uuids == null) return new ArrayList<>();
        return uuids.stream()
            .map(MapperUtils::uuidToString)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    public static List<UUID> stringsToUuids(Collection<String> strings) {
        if (strings == null) return new ArrayList<>();
        return strings.stream()
            .map(MapperUtils::stringToUuid)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    // ========== Date/Time Conversions ==========
    
    public static LocalDate dateTimeToDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate() : null;
    }
    
    public static LocalDateTime dateToDateTime(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }
    
    public static LocalTime dateTimeToTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalTime() : null;
    }
    
    public static LocalDateTime timeToDateTime(LocalTime time) {
        if (time == null) return null;
        return LocalDateTime.of(LocalDate.now(), time);
    }
    
    public static LocalDateTime timeToDateTime(LocalTime time, LocalDate date) {
        if (time == null || date == null) return null;
        return LocalDateTime.of(date, time);
    }
    
    // ========== Map Conversions for JSONB ==========
    
    public static Map<String, String> objectMapToStringMap(Map<String, Object> objectMap) {
        if (objectMap == null) return new HashMap<>();
        Map<String, String> stringMap = new HashMap<>();
        objectMap.forEach((key, value) -> 
            stringMap.put(key, value != null ? value.toString() : ""));
        return stringMap;
    }
    
    public static Map<String, Object> stringMapToObjectMap(Map<String, String> stringMap) {
        if (stringMap == null) return new HashMap<>();
        return new HashMap<>(stringMap);
    }
    
    public static <T> Map<String, T> copyMap(Map<String, T> source) {
        return source != null ? new HashMap<>(source) : new HashMap<>();
    }
    
    // ========== Collection Utilities ==========
    
    public static <T> List<T> copyList(List<T> source) {
        return source != null ? new ArrayList<>(source) : new ArrayList<>();
    }
    
    public static <T> Set<T> copySet(Set<T> source) {
        return source != null ? new HashSet<>(source) : new HashSet<>();
    }
    
    public static <T> List<T> safeList(List<T> list) {
        return list != null ? list : new ArrayList<>();
    }
    
    public static <T> Set<T> safeSet(Set<T> set) {
        return set != null ? set : new HashSet<>();
    }
    
    public static <K, V> Map<K, V> safeMap(Map<K, V> map) {
        return map != null ? map : new HashMap<>();
    }
    
    // ========== String Utilities ==========
    
    public static String defaultIfNull(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    public static String emptyIfNull(String value) {
        return value != null ? value : "";
    }
    
    public static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }
    
    // ========== Number Utilities ==========
    
    public static Integer defaultIfNull(Integer value, Integer defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    public static Long defaultIfNull(Long value, Long defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    public static Double defaultIfNull(Double value, Double defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    public static int safeInt(Integer value) {
        return value != null ? value : 0;
    }
    
    public static long safeLong(Long value) {
        return value != null ? value : 0L;
    }
    
    public static double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }
}