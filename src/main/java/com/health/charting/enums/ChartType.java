package com.health.charting.enums;

import lombok.Getter;

/**
 * Enum representing different types of charts for data visualization.
 */
@Getter
public enum ChartType {
    LINE("Line Chart", "Displays data as a continuous line over time"),
    BAR("Bar Chart", "Displays data as vertical bars");

    private final String displayName;
    private final String description;

    ChartType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}

// Made with Bob
