package com.health.charting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a time range for querying data.
 * 
 * @param from Start timestamp in milliseconds (inclusive)
 * @param to End timestamp in milliseconds (inclusive)
 */
@Schema(description = "Time range for data query")
public record TimeRange(
    @NotNull(message = "Start time is required")
    @Min(value = 0, message = "Start time must be positive")
    @Schema(description = "Start timestamp in milliseconds (inclusive)", example = "1707264000000", required = true)
    Long from,
    
    @NotNull(message = "End time is required")
    @Min(value = 0, message = "End time must be positive")
    @Schema(description = "End timestamp in milliseconds (inclusive)", example = "1707350399000", required = true)
    Long to
) {
    /**
     * Create a TimeRange with validation.
     */
    public TimeRange {
        if (from != null && to != null && from >= to) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
    }
    
    /**
     * Get the duration of this time range in milliseconds.
     * 
     * @return duration in milliseconds
     */
    public long getDurationMillis() {
        return to - from;
    }
    
    /**
     * Check if a timestamp falls within this time range.
     * 
     * @param timestamp the timestamp to check
     * @return true if within range, false otherwise
     */
    public boolean contains(long timestamp) {
        return timestamp >= from && timestamp <= to;
    }
}

// Made with Bob
