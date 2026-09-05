package com.health.charting.dto.response;

import com.health.charting.enums.ChartType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO containing chart data.
 * Includes chart type, axis information, and all data series.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chart response containing all series data")
public class ChartResponse {
    
    @Schema(description = "Type of chart to render", example = "LINE")
    private ChartType chartType;
    
    @Schema(description = "X-axis label", example = "time")
    @Builder.Default
    private String xAxis = "time";
    
    @Schema(description = "List of data series")
    @Builder.Default
    private List<Series> series = new ArrayList<>();
    
    /**
     * Add a series to this chart response.
     * 
     * @param series the series to add
     */
    public void addSeries(Series series) {
        if (this.series == null) {
            this.series = new ArrayList<>();
        }
        this.series.add(series);
    }
    
    /**
     * Get the total number of series in this response.
     * 
     * @return the count of series
     */
    public int getSeriesCount() {
        return series != null ? series.size() : 0;
    }
    
    /**
     * Get the total number of data points across all series.
     * 
     * @return the total count of data points
     */
    public int getTotalDataPoints() {
        if (series == null) {
            return 0;
        }
        return series.stream()
                .mapToInt(Series::getPointCount)
                .sum();
    }
}

// Made with Bob
