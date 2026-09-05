package com.health.charting.enums;

import lombok.Getter;

/**
 * Enum representing different types of health metrics.
 * Each metric type corresponds to a specific table in TimescaleDB.
 */
@Getter
public enum MetricType {
    HEARTRATE("heartrate"),
    GLUCOSE("glucose"),
    SPO2("spo2"),
    BLOODPRESSURE("bloodpressure"),
    SLEEP("sleep"),
    ACTIVITY("activity"),
    WORKOUT("workout");

    private final String tableName;

    MetricType(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Get MetricType from table name.
     * 
     * @param tableName the table name
     * @return the corresponding MetricType
     * @throws IllegalArgumentException if table name is invalid
     */
    public static MetricType fromTableName(String tableName) {
        for (MetricType type : values()) {
            if (type.tableName.equalsIgnoreCase(tableName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid table name: " + tableName);
    }
}

// Made with Bob
