package com.health.charting.util;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility for aggregating metric rows by timestamp.
 * Groups multiple rows with the same timestamp into a single reading
 * with a combined values map.
 * 
 * This is necessary because multi-component metrics (like blood pressure)
 * are stored as separate rows in the database with the same timestamp.
 */
@Slf4j
public class TimestampAggregator {
    
    /**
     * Represents a raw database row before aggregation.
     */
    @AllArgsConstructor
    public static class RawMetricRow {
        public final long timestamp;
        public final String metricName;
        public final Number metricValue;
        public final String unit;
        public final String source;
    }
    
    /**
     * Represents an aggregated reading with all values for a timestamp.
     */
    @AllArgsConstructor
    public static class AggregatedReading {
        public final long timestamp;
        public final Map<String, Number> values;
        public final String unit;
        public final String source;
    }
    
    /**
     * Aggregate raw rows by timestamp.
     * Rows with the same timestamp are combined into a single reading.
     * 
     * @param rows List of raw metric rows from database
     * @return List of aggregated readings, sorted by timestamp descending
     */
    public static List<AggregatedReading> aggregate(List<RawMetricRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        
        log.debug("Aggregating {} raw rows by timestamp", rows.size());
        
        // Group by timestamp
        Map<Long, List<RawMetricRow>> groupedByTimestamp = rows.stream()
                .collect(Collectors.groupingBy(row -> row.timestamp));
        
        log.debug("Found {} unique timestamps", groupedByTimestamp.size());
        
        // Convert each group to an aggregated reading
        List<AggregatedReading> aggregated = groupedByTimestamp.entrySet().stream()
                .map(entry -> {
                    long timestamp = entry.getKey();
                    List<RawMetricRow> rowsForTimestamp = entry.getValue();
                    
                    // Build values map from all rows at this timestamp
                    Map<String, Number> values = new LinkedHashMap<>();
                    String unit = null;
                    String source = null;
                    
                    for (RawMetricRow row : rowsForTimestamp) {
                        values.put(row.metricName, row.metricValue);
                        if (unit == null) unit = row.unit;
                        if (source == null) source = row.source;
                    }
                    
                    return new AggregatedReading(timestamp, values, unit, source);
                })
                .sorted(Comparator.comparingLong(r -> -r.timestamp)) // Descending order
                .collect(Collectors.toList());
        
        log.debug("Aggregation complete: {} readings", aggregated.size());
        return aggregated;
    }
}

// Made with Bob