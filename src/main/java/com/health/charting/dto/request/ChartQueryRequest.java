package com.health.charting.dto.request;

import com.health.charting.enums.Aggregation;
import com.health.charting.enums.ChartType;
import com.health.charting.enums.MetricType;
import com.health.charting.enums.Resolution;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for querying chart data.
 * Contains all parameters needed to query and aggregate health metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for querying chart data from TimescaleDB")
public class ChartQueryRequest {
    
    @NotBlank(message = "User ID is required")
    @Schema(description = "User identifier", example = "u1", required = true)
    private String userId;
    
    @NotNull(message = "Metric type is required")
    @Schema(description = "Type of health metric to query", example = "HEARTRATE", required = true)
    private MetricType metricType;
    
    @NotNull(message = "Time range is required")
    @Valid
    @Schema(description = "Time range for the query", required = true)
    private TimeRange timeRange;
    
    @Schema(description = "Time bucketing resolution (default: RAW)", example = "FIVE_MIN")
    @Builder.Default
    private Resolution resolution = Resolution.RAW;
    
    @Schema(description = "Default aggregation function for all series (can be overridden per series)",
            example = "AVG")
    private Aggregation aggregation;
    
    @Schema(description = "Chart type for visualization (default: LINE)", example = "BAR")
    @Builder.Default
    private ChartType chartType = ChartType.LINE;
    
    @NotEmpty(message = "At least one series specification is required")
    @Size(min = 1, max = 10, message = "Number of series must be between 1 and 10")
    @Valid
    @Schema(description = "List of series specifications", required = true)
    @Builder.Default
    private List<SeriesSpec> series = new ArrayList<>();
    
    /**
     * Get the effective aggregation for a series.
     * Returns the series-specific aggregation if set, otherwise the request-level aggregation.
     * 
     * @param seriesSpec the series specification
     * @return the effective aggregation, or AVG as default
     */
    public Aggregation getEffectiveAggregation(SeriesSpec seriesSpec) {
        if (seriesSpec.getAggregation() != null) {
            return seriesSpec.getAggregation();
        }
        return aggregation != null ? aggregation : Aggregation.AVG;
    }
    
    /**
     * Check if time bucketing is required based on resolution.
     * 
     * @return true if bucketing is needed, false otherwise
     */
    public boolean requiresTimeBucket() {
        return resolution != null && resolution.requiresTimeBucket();
    }
    
    /**
     * Add a series specification to this request.
     * 
     * @param seriesSpec the series specification to add
     */
    public void addSeries(SeriesSpec seriesSpec) {
        if (this.series == null) {
            this.series = new ArrayList<>();
        }
        this.series.add(seriesSpec);
    }
}

// Made with Bob
