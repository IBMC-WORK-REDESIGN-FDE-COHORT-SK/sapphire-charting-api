package com.health.charting.enums;

import lombok.Getter;

/**
 * Enum representing different time bucket resolutions for data aggregation.
 * Each resolution maps to a TimescaleDB time_bucket interval.
 */
@Getter
public enum Resolution {
    RAW(null, "No bucketing - raw data points"),
    MINUTE("1 minute", "1-minute intervals"),
    FIVE_MIN("5 minutes", "5-minute intervals"),
    HOUR("1 hour", "1-hour intervals"),
    DAY("1 day", "1-day intervals");

    private final String timeBucketInterval;
    private final String description;

    Resolution(String timeBucketInterval, String description) {
        this.timeBucketInterval = timeBucketInterval;
        this.description = description;
    }

    /**
     * Check if this resolution requires time bucketing.
     * 
     * @return true if time bucketing is needed, false for RAW data
     */
    public boolean requiresTimeBucket() {
        return this != RAW;
    }

    /**
     * Get the time bucket SQL function for this resolution.
     * 
     * @param timeColumn the time column to bucket (e.g., "to_timestamp(time/1000)")
     * @return SQL time_bucket function or the original column for RAW
     */
    public String getTimeBucketSql(String timeColumn) {
        if (this == RAW) {
            return timeColumn;
        }
        return String.format("time_bucket('%s', %s)", timeBucketInterval, timeColumn);
    }
}

// Made with Bob
