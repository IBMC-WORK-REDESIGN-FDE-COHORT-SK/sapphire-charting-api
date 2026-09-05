package com.health.charting.service;

import com.health.charting.dto.response.MetricReadingDto;
import com.health.charting.dto.response.MetricReadingsResponse;
import com.health.charting.enums.MetricType;
import com.health.charting.repository.MetricReadingRepository;
import com.health.charting.service.evaluator.MetricStatusEvaluator;
import com.health.charting.service.evaluator.MetricStatusEvaluatorRegistry;
import com.health.charting.util.TimestampAggregator;
import com.health.charting.util.TimestampAggregator.AggregatedReading;
import com.health.charting.util.TimestampAggregator.RawMetricRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for fetching and processing metric readings.
 * Orchestrates repository calls, timestamp aggregation, and status computation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricReadingService {
    
    private final MetricReadingRepository repository;
    private final MetricStatusEvaluatorRegistry evaluatorRegistry;
    
    /**
     * Fetch paginated metric readings with status computation.
     * 
     * @param metricName Metric type name (e.g., "blood_pressure", "glucose")
     * @param userId User identifier
     * @param page Page number (0-based)
     * @param size Page size (max 100)
     * @param fromTime Optional start time filter (epoch milliseconds)
     * @param toTime Optional end time filter (epoch milliseconds)
     * @return MetricReadingsResponse with data and pagination info in headers
     */
    public MetricReadingsResponse fetchReadings(
            String metricName,
            String userId,
            int page,
            int size,
            Long fromTime,
            Long toTime) {
        
        log.info("Fetching readings for metric={}, user={}, page={}, size={}", 
                metricName, userId, page, size);
        
        // Validate and cap page size
        if (size > 50) {
            log.warn("Page size {} exceeds maximum, capping at 100", size);
            size = 50;
        }
        
        // Resolve metric type from name
        MetricType metricType = resolveMetricType(metricName);
        log.debug("Resolved metric type: {}", metricType);
        
        // Fetch raw rows from database
        List<RawMetricRow> rawRows = repository.fetchReadings(
                metricType, userId, page, size, fromTime, toTime);
        
        log.debug("Fetched {} raw rows from database", rawRows.size());
        
        // Aggregate rows by timestamp
        List<AggregatedReading> aggregatedReadings = TimestampAggregator.aggregate(rawRows);
        
        log.debug("Aggregated into {} readings", aggregatedReadings.size());
        
        // Get appropriate status evaluator
        MetricStatusEvaluator evaluator = evaluatorRegistry.getEvaluator(metricType);
        
        // Convert to DTOs with status computation
        List<MetricReadingDto> readings = aggregatedReadings.stream()
                .map(reading -> buildMetricReadingDto(reading, evaluator))
                .collect(Collectors.toList());
        
        log.info("Returning {} readings for metric={}, user={}", 
                readings.size(), metricName, userId);
        
        return MetricReadingsResponse.builder()
                .metric(metricName)
                .userId(userId)
                .data(readings)
                .build();
    }
    
    /**
     * Calculate pagination metadata for response headers.
     * 
     * @param metricName Metric type name
     * @param userId User identifier
     * @param page Current page number
     * @param size Page size
     * @param fromTime Optional start time filter
     * @param toTime Optional end time filter
     * @return PageInfo with pagination metadata
     */
    public PageInfo calculatePageInfo(
            String metricName,
            String userId,
            int page,
            int size,
            Long fromTime,
            Long toTime) {
        
        MetricType metricType = resolveMetricType(metricName);
        long totalElements = repository.countReadings(metricType, userId, fromTime, toTime);
        
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;
        
        return new PageInfo(page, size, totalElements, totalPages, hasNext, hasPrevious);
    }
    
    /**
     * Resolve MetricType from metric name string.
     */
    private MetricType resolveMetricType(String metricName) {
        try {
            // Try to match by table name
            return MetricType.fromTableName(metricName);
        } catch (IllegalArgumentException e) {
            // Try to match by enum name
            try {
                return MetricType.valueOf(metricName.toUpperCase());
            } catch (IllegalArgumentException ex) {
                log.error("Invalid metric name: {}", metricName);
                throw new IllegalArgumentException("Invalid metric name: " + metricName);
            }
        }
    }
    
    /**
     * Build MetricReadingDto from aggregated reading with status computation.
     */
    private MetricReadingDto buildMetricReadingDto(
            AggregatedReading reading,
            MetricStatusEvaluator evaluator) {
        
        // Compute status
        String status = evaluator.evaluate(reading.values)
                .orElse(null);
        
        // Convert timestamp from nanoseconds to milliseconds if needed
        long timestampMillis = reading.timestamp;
        
        // Check if timestamp is in nanoseconds (year > 3000 indicates nanoseconds)
        Instant instant = Instant.ofEpochMilli(timestampMillis);
        if (instant.getEpochSecond() > 32503680000L) { // Year 3000 in seconds
            // Convert from nanoseconds to milliseconds (divide by 1,000,000)
            timestampMillis = timestampMillis / 1000000;
            instant = Instant.ofEpochMilli(timestampMillis);
            log.debug("Converted timestamp from nanoseconds to milliseconds: {} -> {}",
                    reading.timestamp, timestampMillis);
        }
        
        return MetricReadingDto.builder()
                .timestamp(instant)
                .values(reading.values)
                .unit(reading.unit)
                .source(reading.source)
                .status(status)
                .build();
    }
    
    /**
     * Simple DTO for pagination metadata.
     */
    public static class PageInfo {
        public final int pageNumber;
        public final int pageSize;
        public final long totalElements;
        public final int totalPages;
        public final boolean hasNext;
        public final boolean hasPrevious;
        
        public PageInfo(int pageNumber, int pageSize, long totalElements, 
                       int totalPages, boolean hasNext, boolean hasPrevious) {
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.hasNext = hasNext;
            this.hasPrevious = hasPrevious;
        }
    }
}

// Made with Bob