package com.health.charting.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a single data point in a time series.
 * 
 * @param timestamp Unix timestamp in milliseconds
 * @param value The metric value at this timestamp
 */
@Schema(description = "A single data point in a time series")
public record DataPoint(
    @Schema(description = "Unix timestamp in milliseconds", example = "1707264000000")
    long timestamp,
    
    @Schema(description = "Metric value", example = "72.5")
    Double value
) {
    /**
     * Create a DataPoint with validation.
     */
    public DataPoint {
        if (timestamp < 0) {
            throw new IllegalArgumentException("Timestamp cannot be negative");
        }
    }
}

// Made with Bob
