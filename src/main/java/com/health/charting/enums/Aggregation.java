package com.health.charting.enums;

import lombok.Getter;

/**
 * Enum representing different aggregation functions for metric values.
 * Each aggregation type maps to a specific SQL function.
 */
@Getter
public enum Aggregation {
    AVG("AVG(metric_value)", "Average"),
    MIN("MIN(metric_value)", "Minimum"),
    MAX("MAX(metric_value)", "Maximum"),
    SUM("SUM(metric_value)", "Sum"),
    COUNT("COUNT(*)", "Count"),
    P50("PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY metric_value)", "50th Percentile (Median)"),
    P95("PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY metric_value)", "95th Percentile"),
    P99("PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY metric_value)", "99th Percentile");

    private final String sqlFunction;
    private final String description;

    Aggregation(String sqlFunction, String description) {
        this.sqlFunction = sqlFunction;
        this.description = description;
    }

    /**
     * Get the SQL function string for this aggregation.
     * 
     * @return SQL function string
     */
    public String getSqlFunction() {
        return sqlFunction;
    }

    /**
     * Check if this aggregation is a percentile function.
     * 
     * @return true if percentile, false otherwise
     */
    public boolean isPercentile() {
        return this == P50 || this == P95 || this == P99;
    }
}

// Made with Bob
