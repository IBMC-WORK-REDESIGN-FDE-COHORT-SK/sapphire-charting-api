package com.health.charting.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a data series in a chart.
 * Contains the series name and all data points.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A data series containing multiple data points")
public class Series {
    
    @Schema(description = "Display name of the series", example = "Heart Rate")
    private String name;
    
    @Schema(description = "List of data points in chronological order")
    @Builder.Default
    private List<DataPoint> points = new ArrayList<>();
    
    /**
     * Add a data point to this series.
     * 
     * @param point the data point to add
     */
    public void addPoint(DataPoint point) {
        if (this.points == null) {
            this.points = new ArrayList<>();
        }
        this.points.add(point);
    }
    
    /**
     * Get the number of data points in this series.
     * 
     * @return the count of data points
     */
    public int getPointCount() {
        return points != null ? points.size() : 0;
    }
}

// Made with Bob
